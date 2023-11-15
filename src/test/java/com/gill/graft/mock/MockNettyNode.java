package com.gill.graft.mock;

import java.util.List;

import com.gill.graft.BaseTest;
import com.gill.graft.Node;
import com.gill.graft.TestUtils;
import com.gill.graft.apis.Server;
import com.gill.graft.machine.RaftMachine;
import com.gill.graft.machine.RaftState;
import com.gill.graft.model.LogEntry;
import com.gill.graft.rpc.server.NettyServer;

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

	public static MockNettyNode newNode(int id) {
		MockNettyNode node = new MockNettyNode(id);
		TestUtils.setField(node, "server", new NettyServer(node));
		return node;
	}

	public boolean isReadiness() {
		return isMachineReady() && isServerReady();
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
