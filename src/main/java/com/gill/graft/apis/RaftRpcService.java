package com.gill.graft.apis;

import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;

/**
 * RaftRpcService
 *
 * @author gill
 * @version 2023/09/20
 **/
public interface RaftRpcService {

	/**
	 * 获取id
	 *
	 * @return id
	 */
	int getId();

	/**
	 * raft 预投票（防止term膨胀）
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	Reply preVote(PreVoteParam param);

	/**
	 * raft 投票
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	Reply requestVote(RequestVoteParam param);

	/**
	 * ping 和 日志同步
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	AppendLogReply appendLogEntries(AppendLogEntriesParam param);

	/**
	 * 同步快照数据
	 *
	 * @param param
	 *            参数
	 * @return 响应
	 */
	Reply replicateSnapshot(ReplicateSnapshotParam param);
}
