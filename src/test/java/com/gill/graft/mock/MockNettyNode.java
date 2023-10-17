package com.gill.graft.mock;

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
import com.gill.graft.machine.RaftMachine;
import com.gill.graft.machine.RaftState;
import com.gill.graft.model.LogEntry;

/**
 * MockNode
 *
 * @author gill
 * @version 2023/09/04
 **/
public class MockNettyNode extends Node implements TestMethod {

	public MockNettyNode(int id) {
		super(id);
		this.getConfig().setPort(BaseTest.findFreePort());
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
}
