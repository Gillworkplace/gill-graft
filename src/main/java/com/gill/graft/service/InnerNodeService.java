package com.gill.graft.service;

import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;
import com.gill.graft.statistic.CostStatistic;

/**
 * NodeService
 *
 * @author gill
 * @version 2023/08/18
 **/
public interface InnerNodeService extends NodeService {

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
	default Reply preVote(PreVoteParam param) {
		if (!isMachineReady()) {
			return new Reply(false, -1);
		}
		return CostStatistic.cost(() -> doPreVote(param), "pre-vote");
	}

	/**
	 * raft 预投票（防止term膨胀）
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	Reply doPreVote(PreVoteParam param);

	/**
	 * raft 投票
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	default Reply requestVote(RequestVoteParam param) {
		if (!isMachineReady()) {
			return new Reply(false, -1);
		}
		return CostStatistic.cost(() -> doRequestVote(param), "request-vote");
	}

	/**
	 * raft 投票
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	Reply doRequestVote(RequestVoteParam param);

	/**
	 * ping 和 日志同步
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	default AppendLogReply appendLogEntries(AppendLogEntriesParam param) {
		if (!isMachineReady()) {
			return new AppendLogReply(false, -1);
		}
		return CostStatistic.cost(() -> doAppendLogEntries(param), "append-log-entries");
	}

	/**
	 * ping 和 日志同步
	 *
	 * @param param
	 *            param
	 * @return Reply
	 */
	AppendLogReply doAppendLogEntries(AppendLogEntriesParam param);

	/**
	 * 同步快照数据
	 * 
	 * @param param
	 *            参数
	 * @return 响应
	 */
	default Reply replicateSnapshot(ReplicateSnapshotParam param) {
		if (!isMachineReady()) {
			return new AppendLogReply(false, -1);
		}
		return CostStatistic.cost(() -> doReplicateSnapshot(param), "replicate-snapshot");
	}

	/**
	 * 同步快照数据
	 *
	 * @param param
	 *            参数
	 * @return 响应
	 */
	Reply doReplicateSnapshot(ReplicateSnapshotParam param);
}
