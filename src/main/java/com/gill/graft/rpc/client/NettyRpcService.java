package com.gill.graft.rpc.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.gill.graft.Node;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;

/**
 * NettyRpcService
 *
 * @author gill
 * @version 2023/10/11
 **/
public class NettyRpcService implements RaftRpcService {

	private final NettyClient client;

	private final AtomicInteger nodeId = new AtomicInteger(-1);

	public NettyRpcService(Node node, String host, int port) {
		this.client = new NettyClient(node.getId(), host, port, () -> node.getConfig().getNettyConfig(),
				() -> node.getConfig().getAuthConfig());
		this.client.connect();
	}

	public NettyRpcService(String host, int port, Supplier<RaftConfig> raftConfigSupplier) {
		this.client = new NettyClient(-1, host, port, raftConfigSupplier.get()::getNettyConfig,
				raftConfigSupplier.get()::getAuthConfig);
		this.client.connect();
	}

	/**
	 * 是否为就绪状态
	 *
	 * @return 就绪状态
	 */
	@Override
	public boolean isReady() {
		return client.isReady();
	}

	/**
	 * 获取id
	 *
	 * @return id
	 */
	@Override
	public int getId() {
		if (nodeId.get() == -1) {
			synchronized (nodeId) {
				if (nodeId.get() == -1) {
					this.nodeId.set(client.getRemoteId());
				}
			}
		}
		return nodeId.get();
	}

	/**
	 * raft 预投票（防止term膨胀）
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	@Override
	public Reply preVote(PreVoteParam param) {
		byte[] paramB = param.encode();
		byte[] retB = client.request(2, paramB);
		return Reply.decode(retB);
	}

	/**
	 * raft 投票
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	@Override
	public Reply requestVote(RequestVoteParam param) {
		byte[] paramB = param.encode();
		byte[] retB = client.request(3, paramB);
		return Reply.decode(retB);
	}

	/**
	 * ping 和 日志同步
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	@Override
	public AppendLogReply appendLogEntries(AppendLogEntriesParam param) {
		byte[] paramB = param.encode();
		byte[] retB = client.request(4, paramB);
		return AppendLogReply.decode(retB);
	}

	/**
	 * 同步快照数据
	 *
	 * @param param
	 *            参数
	 * @return 响应
	 */
	@Override
	public Reply replicateSnapshot(ReplicateSnapshotParam param) {
		byte[] paramB = param.encode();
		byte[] retB = client.request(5, paramB);
		return Reply.decode(retB);
	}

	/**
	 * 关闭资源
	 */
	@Override
	public void shutdown() {
		client.shutdownSync();
	}
}
