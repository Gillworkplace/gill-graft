package com.gill.graft.mock;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gill.graft.model.LogEntry;
import org.springframework.test.util.ReflectionTestUtils;

import com.gill.consensus.raftplus.LogManager;
import com.gill.consensus.raftplus.Node;
import com.gill.consensus.raftplus.example.intmap.IntMapDataStorage;
import com.gill.consensus.raftplus.example.intmap.IntMapServer;
import com.gill.consensus.raftplus.machine.RaftMachine;
import com.gill.consensus.raftplus.machine.RaftState;
import com.gill.consensus.raftplus.model.LogEntry;

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
        return (Node) ReflectionTestUtils.getField(this, "node");
    }

    public RaftMachine getRaftMachine() {
        return (RaftMachine) ReflectionTestUtils.getField(getNode(), "machine");
    }

    public void updateCommittedIdx(int committedIdx) {
        Node node = getNode();
        node.setCommittedIdx(committedIdx);
    }

    @SuppressWarnings("unchecked")
    public void clearLogsAndData() {
        Node node = getNode();
        LogManager logManager = node.getLogManager();
        TreeMap<Integer, LogEntry> logs = (TreeMap<Integer, LogEntry>) ReflectionTestUtils.getField(logManager, "logs");
        logs.clear();
        logs.put(0, new LogEntry(0, 0, ""));
        IntMapDataStorage dataStorage = (IntMapDataStorage) node.getDataStorage();
        Map<String, Integer> map = (Map<String, Integer>) ReflectionTestUtils.getField(dataStorage, "map");
        map.clear();
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
        return getNode().getLogManager().getLogs(0, Integer.MAX_VALUE);
    }
}
