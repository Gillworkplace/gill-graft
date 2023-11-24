package com.gill.graft;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.apis.DataStorage;
import com.gill.graft.apis.LogStorage;
import com.gill.graft.apis.MetaStorage;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.apis.Server;
import com.gill.graft.apis.empty.EmptyDataStorage;
import com.gill.graft.apis.empty.EmptyLogStorage;
import com.gill.graft.apis.empty.EmptyMetaStorage;
import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;
import com.gill.graft.machine.RaftEvent;
import com.gill.graft.machine.RaftEventParams;
import com.gill.graft.machine.RaftMachine;
import com.gill.graft.machine.RaftState;
import com.gill.graft.model.LogEntry;
import com.gill.graft.model.PersistentProperties;
import com.gill.graft.model.ProposeReply;
import com.gill.graft.proto.RaftConverter;
import com.gill.graft.rpc.MetricsRegistry;
import com.gill.graft.rpc.ServiceRegistry;
import com.gill.graft.rpc.server.NettyServer;
import com.gill.graft.service.InnerNodeService;
import com.gill.graft.service.PrintService;
import com.gill.graft.service.RaftService;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import javafx.util.Pair;

/**
 * Node single
 *
 * @author gill
 * @version 2023/08/02
 **/
public class Node implements InnerNodeService, RaftService, PrintService {

	private static final Logger log = LoggerFactory.getLogger(Node.class);

	/**
	 * 节点属性
	 */
	private final int id;

	private int priority = 0;

	private final AtomicInteger commitIdx = new AtomicInteger(0);

	/**
	 * 状态机状态是否稳定 稳定：收到更高版本的心跳包; leader的op请求过半数成功响应 不稳定：心跳检测超时; leader的心跳包过半数发送失败
	 */
	private final AtomicBoolean stable = new AtomicBoolean(false);

	/**
	 * 节点是否启动完成
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private final HeartbeatState heartbeatState = new HeartbeatState(0, 0);

	private final Lock lock = new ReentrantLock();

	/**
	 * 节点组件
	 */
	private final Schedulers schedulers = new Schedulers();

	private final ThreadPools threadPools = new ThreadPools();

	private final MetaDataManager metaDataManager;

	private RaftConfig config = new RaftConfig();

	private transient final RaftMachine machine = new RaftMachine(this);

	private transient final DataStorage dataStorage;

	private transient final LogManager logManager;

	private transient final ProposeHelper proposeHelper;

	private transient final ApplyWorker applyWorker;

	private final ServiceRegistry serviceRegistry = new ServiceRegistry();

	{
		serviceRegistry.register(1, bytes -> RaftConverter.intEncode(getId()));
		serviceRegistry.register(2, bytes -> preVote(PreVoteParam.decode(bytes)).encode());
		serviceRegistry.register(3, bytes -> requestVote(RequestVoteParam.decode(bytes)).encode());
		serviceRegistry.register(4, bytes -> appendLogEntries(AppendLogEntriesParam.decode(bytes)).encode());
		serviceRegistry.register(5, bytes -> replicateSnapshot(ReplicateSnapshotParam.decode(bytes)).encode());
	}

	private final MetricsRegistry metricsRegistry;

	private Server server;

	/**
	 * 集群属性
	 */
	private List<RaftRpcService> followers = Collections.emptyList();

	protected Node() {
		this(RandomUtil.randomInt(100, 200));
	}

	protected Node(MetaStorage metaStorage, DataStorage dataStorage, LogStorage logStorage) {
		this(RandomUtil.randomInt(100, 200), metaStorage, dataStorage, logStorage);
	}

	protected Node(int id) {
		this(id, EmptyMetaStorage.INSTANCE, EmptyDataStorage.INSTANCE, EmptyLogStorage.INSTANCE);
	}

	protected Node(int id, MetaStorage metaStorage, DataStorage dataStorage, LogStorage logStorage) {
		this.id = id;
		this.metaDataManager = new MetaDataManager(metaStorage);
		this.dataStorage = dataStorage;
		this.logManager = new LogManager(logStorage, this.config.getLogConfig());
		this.metricsRegistry = new MetricsRegistry(this.id);
		this.applyWorker = new ApplyWorker(this.id, this.dataStorage, this.logManager, this.dataStorage::getApplyIdx,
				this::getCommitIdx);
		this.proposeHelper = new ProposeHelper(threadPools::getApiPool, applyWorker);
	}

	public static Node newNode() {
		Node node = new Node();
		node.setServer(new NettyServer(node));
		return node;
	}

	public static Node newNode(MetaStorage metaStorage, DataStorage dataStorage, LogStorage logStorage) {
		Node node = new Node(metaStorage, dataStorage, logStorage);
		node.setServer(new NettyServer(node));
		return node;
	}

	public static Node newNode(int id) {
		Node node = new Node(id);
		node.setServer(new NettyServer(node));
		return node;
	}

	public static Node newNode(int id, MetaStorage metaStorage, DataStorage dataStorage, LogStorage logStorage) {
		Node node = new Node(id, metaStorage, dataStorage, logStorage);
		node.setServer(new NettyServer(node));
		return node;
	}

	public static Node newNode(int id, MetaStorage metaStorage, DataStorage dataStorage, LogStorage logStorage,
			Function<Node, Server> serverFactory) {
		Node node = new Node(id, metaStorage, dataStorage, logStorage);
		Server server = serverFactory.apply(node);
		node.setServer(server);
		return node;
	}

	private void setServer(Server server) {
		this.server = server;
	}

	public long getTerm() {
		return this.metaDataManager.getTerm();
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public MetricsRegistry getMetricsRegistry() {
		return metricsRegistry;
	}

	/**
	 * 自增任期
	 * 
	 * @param originTerm
	 *            起始任期
	 * @return originTerm + 1, -1表示选举自己失败
	 */
	public long electSelf(long originTerm) {
		return metaDataManager.increaseTerm(originTerm, id);
	}

	public int getCommitIdx() {
		return commitIdx.get();
	}

	public void setCommitIdx(int commitIdx) {
		this.commitIdx.accumulateAndGet(commitIdx, Math::max);
		this.applyWorker.wakeup();
	}

	public void resetCommittedIdx(int committedIdx) {
		this.commitIdx.set(committedIdx);
		this.applyWorker.wakeup();
	}

	public boolean isStable() {
		return stable.get();
	}

	/**
	 * 将节点设为稳定状态
	 */
	public void stable() {
		stable.set(true);
	}

	/**
	 * 将节点设为不稳定状态
	 */
	public void unstable() {
		stable.set(false);
	}

	public boolean isInitialized() {
		return initialized.get();
	}

	private void initialized() {
		initialized.set(true);
	}

	private void uninitialized() {
		initialized.set(false);
	}

	private void loadData() {
		lock.lock();
		try {
			// 初始化元数据
			initMeta();

			// 初始化数据
			int applyIdx = initData();

			// 初始化日志
			initLog(applyIdx);
		} finally {
			lock.unlock();
		}
	}

	private void initMeta() {
		log.debug("initialize metadata...");
		metaDataManager.init();
		log.debug("finish initializing metadata.");
	}

	private int initData() {
		log.debug("loading snapshot...");
		int applyIdx = dataStorage.loadSnapshot();
		log.debug("finish loading snapshot.");
		return applyIdx;
	}

	private void initLog(int applyIdx) {
		log.debug("loading snapshot...");
		logManager.init(applyIdx);
		log.debug("finish loading snapshot...");
	}

	/**
	 * 发布事件，透传给状态机
	 * 
	 * @param event
	 *            事件
	 * @param params
	 *            参数
	 */
	public void publishEvent(RaftEvent event, RaftEventParams params) {
		log.debug("node: {} publishes event: {}", id, event.name());
		this.machine.publishEvent(event, params);
	}

	/**
	 * 降级为follower
	 */
	public void stepDown() {
		stepDown(getTerm());
	}

	/**
	 * 降级为follower
	 */
	public void stepDown(long newTerm) {
		stepDown(newTerm, false);
	}

	/**
	 * 降级为follower
	 */
	public void stepDown(long newTerm, boolean sync) {
		if (this.metaDataManager.acceptHigherOrSameTerm(newTerm)) {
			publishEvent(RaftEvent.FORCE_FOLLOWER, new RaftEventParams(Integer.MAX_VALUE, sync));
		}
	}

	private boolean voteFor(long newTerm, int nodeId) {
		if (this.metaDataManager.voteFor(newTerm, nodeId)) {
			refreshLastHeartbeatTimestamp();
			publishEvent(RaftEvent.FORCE_FOLLOWER, new RaftEventParams(Integer.MAX_VALUE, true));
			return true;
		}
		return false;
	}

	/**
	 * 刷新心跳时间
	 */
	private void refreshLastHeartbeatTimestamp() {
		log.debug("node: {} refresh heartbeat timestamp", id);
		heartbeatState.set(getTerm(), System.currentTimeMillis());
	}

	@Override
	public boolean isMachineReady() {
		return machine.isReady() && isInitialized();
	}

	/**
	 * server是否准备继续
	 *
	 * @return 是否
	 */
	@Override
	public boolean isServerReady() {
		return server.isReady();
	}

	private boolean unlatestLog(long lastLogTerm, long lastLogIdx) {
		Pair<Long, Integer> pair = logManager.lastLog();
		return lastLogTerm <= pair.getKey() && (lastLogTerm != pair.getKey() || lastLogIdx < pair.getValue());
	}

	@Override
	public Reply doPreVote(PreVoteParam param) {

		// 成功条件：
		// · 参数中的任期更大，或任期相同但日志索引更大
		// · 至少一次选举超时时间内没有收到领导者心跳
		log.debug("node: {} receives PRE_VOTE, param: {}", id, param);
		long pTerm = param.getTerm();
		PersistentProperties properties = metaDataManager.getProperties();
		long term = properties.getTerm();

		// 版本太低 丢弃
		if (pTerm < term) {
			log.debug("node: {} discards PRE_VOTE for the old vote's term, client id: {}", id, param.getNodeId());
			return new Reply(false, term);
		}

		// 日志不够新 丢弃
		if (pTerm == term && unlatestLog(param.getLastLogTerm(), param.getLastLogIdx())) {
			log.debug("node: {} discards PRE_VOTE for the old vote's log-index, client id: {}", id, param.getNodeId());
			return new Reply(false, term);
		}

		// 该节点还未超时
		Pair<Long, Long> pair = heartbeatState.get();
		if (System.currentTimeMillis() - pair.getValue() < config.getBaseTimeoutInterval()) {
			log.debug("node: {} discards PRE_VOTE, because the node is not timeout, client id: {}", id,
					param.getNodeId());
			return new Reply(false, term);
		}
		log.info("node: {} accepts PRE_VOTE, client id: {}", id, param.getNodeId());
		return new Reply(true, term);
	}

	@Override
	public Reply doRequestVote(RequestVoteParam param) {
		lock.lock();
		try {
			log.debug("node: {} receives REQUEST_VOTE, param: {}", id, param);
			int nodeId = param.getNodeId();
			long pTerm = param.getTerm();

			PersistentProperties properties = metaDataManager.getProperties();
			long cTerm = properties.getTerm();
			Integer cVotedFor = properties.getVotedFor();

			// 版本太低 丢弃
			if (pTerm < cTerm) {
				log.debug("node: {} discards REQUEST_VOTE for the old vote's cTerm, client id: {}", id, nodeId);
				return new Reply(false, cTerm);
			}

			// 当前任期已投票
			if (pTerm == cTerm && cVotedFor != null && cVotedFor != nodeId) {
				log.debug(
						"node: {} discards REQUEST_VOTE, because node was voted for {} when term was {}, client id: {}",
						id, cVotedFor, pTerm, nodeId);
				return new Reply(false, pTerm);
			}

			// 日志不够新 丢弃
			if (unlatestLog(param.getLastLogTerm(), param.getLastLogIdx())) {
				log.debug("node: {} discards REQUEST_VOTE for the old vote's log-index, client id: {}", id, nodeId);
				return new Reply(false, pTerm);
			}

			if (voteFor(pTerm, nodeId)) {
				log.info("node: {} REQUEST_VOTE has voted for {}, term: {}", id, nodeId, pTerm);
				return new Reply(true, pTerm);
			}
			log.debug("node: {} REQUEST_VOTE vote for term {} id {} failed ", id, pTerm, nodeId);
			return new Reply(false, pTerm);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public AppendLogReply doAppendLogEntries(AppendLogEntriesParam param) {
		lock.lock();
		try {
			long term = getTerm();
			long pTerm = param.getTerm();
			int pCommitIdx = param.getCommitIdx();
			if (term > pTerm) {
				return new AppendLogReply(false, term);
			}
			refreshLastHeartbeatTimestamp();
			stepDown(pTerm, true);
			stable();

			// 更新committedIdx
			setCommitIdx(pCommitIdx);

			// 没有logs属性的为ping请求
			if (param.getLogs() == null || param.getLogs().isEmpty()) {
				log.trace("node: {} receive heartbeat from {}", id, param.getNodeId());
				return new AppendLogReply(true, pTerm);
			}

			log.debug("node: {} receive appends log from {}", id, param.getNodeId());

			// 日志一致性检查
			int committedIdx = getCommitIdx();

			// 如果当前节点的committedIdx 大于 leader的 committedIdx
			// 说明当前节点的快照版本超前于 leader的版本，但一切以leader为准，因此需要重新同步快照信息
			if (committedIdx > param.getCommitIdx()) {
				return new AppendLogReply(false, pTerm, true);
			}

			Pair<Long, Integer> pair = logManager.lastLog();
			long lastLogTerm = pair.getKey();
			int lastLogIdx = pair.getValue();

			// 如果版本不一致，批量从前 repairLength 的索引开始修复
			if (lastLogTerm != param.getPreLogTerm()) {
				return new AppendLogReply(false, pTerm, false, lastLogIdx - config.getRepairLength());
			}

			// 同步日志的其实索引不是本节点的下一个索引位置时，返回本节点的最后的日志索引，从该索引开始修复
			if (lastLogIdx < param.getPreLogIdx()) {
				return new AppendLogReply(false, pTerm, false, lastLogIdx);
			}

			List<LogEntry> logs = param.getLogs();

			// 记录日志
			logs.forEach(logManager::appendLog);
			refreshLastHeartbeatTimestamp();
			return new AppendLogReply(true, pTerm);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Reply doReplicateSnapshot(ReplicateSnapshotParam param) {
		lock.lock();
		try {
			log.debug("node: {} receives snapshot from {}, term is {}, apply{idx={}, term={}}", id, param.getNodeId(),
					param.getTerm(), param.getApplyIdx(), param.getApplyTerm());
			long pTerm = param.getTerm();
			long term = getTerm();
			if (pTerm < term) {
				return new Reply(false, term);
			}
			refreshLastHeartbeatTimestamp();
			stepDown(pTerm, true);
			long applyLogTerm = param.getApplyTerm();
			int applyIdx = param.getApplyIdx();
			byte[] data = param.getData();
			dataStorage.saveSnapshot(applyLogTerm, applyIdx, data);
			logManager.rebuildLog(new LogEntry(applyIdx, applyLogTerm, Utils.SYNC));
			resetCommittedIdx(applyIdx);
			return new Reply(true, pTerm);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public synchronized void start(List<RaftRpcService> followers) {
		start(followers, null);
	}

	public synchronized void start(List<RaftRpcService> followers, Integer priority) {
		uninitialized();
		calcPriority(priority);
		this.followers = followers;

		// 启动netty server
		this.server.start();

		// 注册指标数据
		this.metricsRegistry.start();

		// 启动状态机
		this.machine.start();

		// 启动应用工作线程
		this.applyWorker.start();

		// 加载数据
		loadData();

		// 初始化节点状态
		publishEvent(RaftEvent.INIT, new RaftEventParams(getTerm(), true));

		// 启动快照定时器
		schedulers.getSnapshotScheduler().start(this.dataStorage, this.config);
		initialized();
	}

	private void releaseOldFollowers() {

		// 释放资源
		if (!CollectionUtil.isEmpty(followers)) {
			this.followers.parallelStream().forEach(RaftRpcService::shutdown);
		}
	}

	private void calcPriority(Integer priority) {
		if (priority != null) {
			this.priority = priority;
			return;
		}
		this.priority = RandomUtil.randomInt(10);
	}

	@Override
	public synchronized void stop() {
		uninitialized();
		this.publishEvent(RaftEvent.STOP, new RaftEventParams(Integer.MAX_VALUE, true));
		this.applyWorker.stop();
		this.machine.stop();
		schedulers.getSnapshotScheduler().stop();
		this.metricsRegistry.remove();
		releaseOldFollowers();
		this.server.stop();
	}

	@Override
	public synchronized void clear() {

	}

	@Override
	public ProposeReply propose(String command) {
		if (!isMachineReady() || machine.getState() != RaftState.LEADER) {
			return new ProposeReply("not ready");
		}
		log.debug("node: {} propose {}", id, command);
		String validateMessage = dataStorage.validateCommand(command);
		if (Utils.isNotEmpty(validateMessage)) {
			return new ProposeReply(validateMessage);
		}
		LogEntry logEntry = logManager.createLog(getTerm(), command);
		return proposeHelper.propose(logEntry, this::setCommitIdx);
	}

	@Override
	public boolean checkReadIndex(int readIdx) {
		return readIdx <= commitIdx.get();
	}

	@Override
	public int getId() {
		return id;
	}

	public int getPriority() {
		return priority;
	}

	public HeartbeatState getHeartbeatState() {
		return heartbeatState;
	}

	public Schedulers getSchedulers() {
		return schedulers;
	}

	public ThreadPools getThreadPools() {
		return threadPools;
	}

	public RaftConfig getConfig() {
		return config;
	}

	public DataStorage getDataStorage() {
		return dataStorage;
	}

	public LogManager getLogManager() {
		return logManager;
	}

	public ProposeHelper getProposeHelper() {
		return proposeHelper;
	}

	public List<RaftRpcService> getFollowers() {
		return followers;
	}

	@Override
	public String toString() {
		return String.format("id=%s,state=%s,term=%s;", id, machine.getState(), getTerm());
	}

	@Override
	public String println() {
		StringBuilder sb = new StringBuilder();
		sb.append("===================").append(System.lineSeparator());
		sb.append("node id: ").append(id).append("\t").append(machine.getState().name()).append(System.lineSeparator());
		sb.append("persistent properties: ").append(metaDataManager.println()).append(System.lineSeparator());
		sb.append("committed idx: ").append(getCommitIdx()).append(System.lineSeparator());
		sb.append("heartbeat state: ").append(heartbeatState).append(System.lineSeparator());
		sb.append("self: ").append(this).append(System.lineSeparator());
		sb.append("followers: ").append(followers).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("THREAD POOL").append(System.lineSeparator());
		sb.append("cluster pool: ")
				.append(Optional.ofNullable(threadPools.getClusterPool()).map(Object::toString).orElse("none"))
				.append(System.lineSeparator());
		sb.append("api pool: ")
				.append(Optional.ofNullable(threadPools.getApiPool()).map(Object::toString).orElse("none"))
				.append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("SCHEDULER").append(System.lineSeparator());
		sb.append("timeout scheduler: ")
				.append(Optional.ofNullable(schedulers.getTimeoutScheduler()).map(Object::toString).orElse("none"))
				.append(System.lineSeparator());
		sb.append("heartbeat scheduler: ")
				.append(Optional.ofNullable(schedulers.getHeartbeatScheduler()).map(Object::toString).orElse("none"))
				.append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("STATE MACHINE").append(System.lineSeparator());
		sb.append(machine.println()).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("CONFIG").append(System.lineSeparator());
		sb.append(config.toString()).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("PROPOSE HELPER").append(System.lineSeparator());
		sb.append(proposeHelper.println()).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("LOG MANAGER").append(System.lineSeparator());
		sb.append(logManager.println()).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("DATA STORAGE").append(System.lineSeparator());
		sb.append(dataStorage.println()).append(System.lineSeparator());
		sb.append("===================").append(System.lineSeparator());
		sb.append("APPLY WORKER").append(System.lineSeparator());
		sb.append(applyWorker.println()).append(System.lineSeparator());
		return sb.toString();
	}
}
