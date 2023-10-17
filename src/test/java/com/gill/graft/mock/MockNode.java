package com.gill.graft.mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import com.gill.graft.BaseTest;
import com.gill.graft.Node;
import com.gill.graft.TestUtils;
import com.gill.graft.apis.RaftRpcService;
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
import com.gill.graft.scheduler.SnapshotScheduler;

/**
 * MockNode
 *
 * @author gill
 * @version 2023/09/04
 **/
public class MockNode extends Node implements TestMethod, RaftRpcService {

	public MockNode(int id) {
		super(id);
	}

	public RaftMachine getRaftMachine() {
		return TestUtils.getField(this, "machine");
	}

	@Override
	public boolean isUp() {
		return getRaftMachine().getState() != RaftState.STRANGER;
	}

	@Override
	public boolean isLeader() {
		return getRaftMachine().getState() == RaftState.LEADER;
	}

	@Override
	public boolean isFollower() {
		return getRaftMachine().getState() == RaftState.FOLLOWER;
	}

	@Override
	public List<LogEntry> getLog() {
		return getLogManager().getLogs(0, Integer.MAX_VALUE);
	}

	@Override
	public Reply preVote(PreVoteParam param) {
		return super.preVote(param);
	}

	@Override
	public Reply requestVote(RequestVoteParam param) {
		return super.requestVote(param);
	}

	@Override
	public AppendLogReply appendLogEntries(AppendLogEntriesParam param) {
		return super.appendLogEntries(param);
	}

	@Override
	public Reply replicateSnapshot(ReplicateSnapshotParam param) {
		return super.replicateSnapshot(param);
	}

	/**
	 * 关闭资源
	 */
	@Override
	public void shutdown() {

	}
}
