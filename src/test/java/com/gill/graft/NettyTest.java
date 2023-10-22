package com.gill.graft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;
import com.gill.graft.mock.MockNettyNode;
import com.gill.graft.model.LogEntry;
import com.gill.graft.rpc.client.NettyRpcService;
import com.google.protobuf.Internal;

import cn.hutool.core.util.RandomUtil;

/**
 * NettyTest
 *
 * @author gill
 * @version 2023/10/16
 **/
public class NettyTest extends BaseTest {

	private static final int MOCK_NODE_ID = 999;

	private static final int MOCK_NODE_TERM = 999;

	private static final String HOST = "127.0.0.1";

	@Test
	public void testGetId() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		int id = service.getId();
		Assertions.assertEquals(0, id);
	}

	@Test
	public void testPreVote() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		PreVoteParam param = new PreVoteParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0);
		Reply reply = service.preVote(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(0, reply.getTerm(), reply.toString());
	}

	@Test
	public void testRequestVote() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		RequestVoteParam param = new RequestVoteParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0);
		Reply reply = service.requestVote(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
	}

	@Test
	public void testHeartbeat() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		AppendLogEntriesParam param = new AppendLogEntriesParam(MOCK_NODE_ID, MOCK_NODE_TERM);
		AppendLogReply reply = service.appendLogEntries(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
	}

	@Test
	public void testAppendLogEntries() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		LogEntry logEntry = new LogEntry(1, MOCK_NODE_TERM, "");
		AppendLogEntriesParam param = new AppendLogEntriesParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0, 0,
				Collections.singletonList(logEntry));
		AppendLogReply reply = service.appendLogEntries(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
	}

	@Test
	public void testReplicateSnapshot() {
		int port = genNode();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		ReplicateSnapshotParam param = new ReplicateSnapshotParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0,
				Internal.EMPTY_BYTE_ARRAY);
		Reply reply = service.replicateSnapshot(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
	}

	@RepeatedTest(10)
//	@Test
	public void testElection() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		System.out.println("============ TEST FINISHED =============");
//		sleep(999999);
		stopNodes(nodes);
	}

	@RepeatedTest(10)
	public void testRestart() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		MockNettyNode leader = findLeader(nodes);
		leader.stop();
		waitUtilStable(nodes);
		leader.start(getFollowers(nodes, leader));
		waitUtilStable(nodes);
		assertCluster(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	private int genNode() {
		Node node = new Node(0);
		int freePort = BaseTest.findFreePort();
		node.getConfig().setPort(freePort);
		node.start(Collections.emptyList(), true);
		return freePort;
	}

	private List<MockNettyNode> init(int num) {
		List<MockNettyNode> nodes = new ArrayList<>();
		int offset = RandomUtil.randomInt(1000) * 100;
		for (int i = 0; i < num; i++) {
			nodes.add(new MockNettyNode(offset + i));
		}
		System.out.println("offset: " + offset);
		return nodes;
	}

	private List<RaftRpcService> getFollowers(List<MockNettyNode> nodes, MockNettyNode self) {
		return nodes.stream().filter(node -> node != self)
				.map(node -> new NettyRpcService("127.0.0.1", node.getConfig().getPort(), node::getConfig))
				.collect(Collectors.toList());
	}

	private List<MockNettyNode> nodesInitUntilStable(int num) {
		return nodesInitUntilStable(num, null);
	}

	private List<MockNettyNode> nodesInitUntilStable(int num, Integer defaultPriority) {
		List<MockNettyNode> nodes = init(num);
		for (MockNettyNode node : nodes) {
			List<RaftRpcService> followers = getFollowers(nodes, node);
			if (defaultPriority == null) {
				node.start(followers, true);
			} else {
				node.start(followers, true, defaultPriority);
			}
		}
		waitUtilStable(nodes);
		assertCluster(nodes);
		return nodes;
	}
}
