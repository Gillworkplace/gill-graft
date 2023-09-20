package com.gill.graft.mock;

import com.gill.graft.Node;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;

/**
 * NodeRpcWrapper
 *
 * @author gill
 * @version 2023/09/20
 **/
public class NodeRpcWrapper implements RaftRpcService {

	private final Node node;

	public NodeRpcWrapper(Node node) {
		this.node = node;
	}

	@Override
	public int getId() {
		return node.getId();
	}

	@Override
	public Reply preVote(PreVoteParam param) {
		return node.preVote(param);
	}

	@Override
	public Reply requestVote(RequestVoteParam param) {
		return node.requestVote(param);
	}

	@Override
	public AppendLogReply appendLogEntries(AppendLogEntriesParam param) {
		return node.appendLogEntries(param);
	}

	@Override
	public Reply replicateSnapshot(ReplicateSnapshotParam param) {
		return node.replicateSnapshot(param);
	}
}
