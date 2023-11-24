package com.gill.graft;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.common.Utils;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.Reply;
import com.gill.graft.model.LogEntry;
import com.gill.graft.model.ProposeReply;
import com.gill.graft.service.PrintService;

/**
 * ProposeHelper
 *
 * @author gill
 * @version 2023/09/11
 **/
public class ProposeHelper implements PrintService {

	private static final Logger log = LoggerFactory.getLogger(ProposeHelper.class);

	private List<NodeProxy> followerProxies = Collections.emptyList();

	private final Supplier<ExecutorService> apiPoolSupplier;

	private final ApplyWorker applyWorker;

	public ProposeHelper(Supplier<ExecutorService> apiPoolSupplier, ApplyWorker applyWorker) {
		this.apiPoolSupplier = apiPoolSupplier;
		this.applyWorker = applyWorker;
	}

	/**
	 * 启动
	 *
	 * @param node
	 *            节点
	 * @param followers
	 *            从者
	 * @param preLogIdx
	 *            leader节点启动时最后的日志索引位置
	 */
	public void start(Node node, List<RaftRpcService> followers, int preLogIdx) {
		List<NodeProxy> proxies = followers.stream().map(follower -> new NodeProxy(node, follower, preLogIdx))
				.collect(Collectors.toList());
		proxies.forEach(NodeProxy::start);
		followerProxies = proxies;
	}

	/**
	 * 停止
	 */
	public void clear() {
		List<NodeProxy> proxies = followerProxies;
		followerProxies = Collections.emptyList();
		log.info("start to clear propose helper's proxies");
		CompletableFuture<?>[] futures = proxies.stream().map(proxy -> CompletableFuture.runAsync(proxy::stop))
				.toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(futures).join();
		log.info("finish clearing propose helper's proxies");
	}

	/**
	 * propose
	 * 
	 * @param logEntry
	 *            日志
	 * @return 结果
	 */
	public ProposeReply propose(LogEntry logEntry, Consumer<Integer> commitIdx) {

		// 注册apply的同步器
		int logIdx = logEntry.getIndex();
		CompletableFuture<Object> cf = applyWorker.registerApplyCallback(logIdx);

		// 完成集群共识
		boolean success = Utils.majorityCall(followerProxies, proxy -> {
			AppendLogReply reply = new AppendLogReply(false, -1);
			try {
				reply = proxy.appendLog(logEntry);
				return reply;
			} catch (Exception e) {
				log.error("call propose to {} failed, logEntry: {}, e: {}", proxy.getId(), logEntry, e.getMessage());
			}
			log.error("call propose to {} failed, logEntry: {}, reply: {}", proxy.getId(), logEntry, reply);
			return reply;
		}, Reply::isSuccess, apiPoolSupplier.get(), "propose");

		// 正常情况下都为success，只有网络分区或者超时导致失败
		ProposeReply reply = new ProposeReply(logIdx);
		if (success) {

			// 更新commitIdx并等待apply结果
			commitIdx.accept(logIdx);
			reply.setSuccess(true);
			try {
				reply.setData(cf.get());
			} catch (InterruptedException | ExecutionException e) {
				log.warn("the propose that waiting apply is Interrupted, e: {}", e.getMessage());
			}
		} else {
			log.warn("propose failed, logEntry: {}", logEntry);
			reply.setSuccess(false);
			reply.setException("cluster propose failed");
		}

		// propose响应失败不代表真的失败
		return reply;
	}

	@Override
	public String println() {
		StringBuilder sb = new StringBuilder();
		sb.append("followerProxies: ").append(System.lineSeparator());
		for (NodeProxy proxy : followerProxies) {
			sb.append(proxy.println());
		}
		return sb.toString();
	}
}
