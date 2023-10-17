package com.gill.graft.mock;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gill.graft.LogManager;
import com.gill.graft.Node;
import com.gill.graft.TestUtils;
import com.gill.graft.example.intmap.IntMapDataStorage;
import com.gill.graft.example.intmap.IntMapServer;
import com.gill.graft.machine.RaftMachine;
import com.gill.graft.machine.RaftState;
import com.gill.graft.model.LogEntry;

/**
 * MockIntMapServer
 *
 * @author gill
 * @version 2023/09/18
 **/
public class MockIntMapServer extends IntMapServer implements TestMethod {

	public MockIntMapServer(int id) {
		super(id);
	}

	public Node getNode() {
		return TestUtils.getField(this, "node");
	}

	public RaftMachine getRaftMachine() {
		return TestUtils.getField(getNode(), "machine");
	}

	public void updateCommittedIdx(int committedIdx) {
		Node node = getNode();
		node.setCommittedIdx(committedIdx);
	}

	public void clearLogsAndData() {
		Node node = getNode();
		LogManager logManager = node.getLogManager();
		TreeMap<Integer, LogEntry> logs = TestUtils.getField(logManager, "logs");
		logs.clear();
		logs.put(0, new LogEntry(0, 0, ""));
		IntMapDataStorage dataStorage = (IntMapDataStorage) node.getDataStorage();
		Map<String, Integer> map = TestUtils.getField(dataStorage, "map");
		map.clear();
	}

	/**
	 * id
	 *
	 * @return id
	 */
	@Override
	public int getId() {
		return getNode().getId();
	}

	@Override
	public boolean isUp() {
		return getRaftMachine().getState() != RaftState.STRANGER;
	}

	/**
	 * 是否稳定
	 *
	 * @return ret
	 */
	@Override
	public boolean isStable() {
		return getNode().isStable();
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
		return getNode().getLogManager().getLogs(0, Integer.MAX_VALUE);
	}
}
