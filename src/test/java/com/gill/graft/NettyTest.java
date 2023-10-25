package com.gill.graft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.common.Utils;
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
import cn.hutool.core.util.RuntimeUtil;

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
	public void testAuthSuccess() {
		Node node = genNode(1, new byte[]{79, 23, 12});
		int port = node.getConfig().getPort();
		RaftConfig raftConfig = new RaftConfig();
		RaftConfig.AuthConfig authConfig = raftConfig.getAuthConfig();
		authConfig.setAuthKey(1);
		authConfig.setAuthValue(new byte[]{79, 23, 12});
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		int id = service.getId();
		Assertions.assertEquals(0, id);
		node.stop();
	}

	@Test
	public void testAuthFailed_keyNotEquals() {
		Node node = genNode(2, new byte[]{79, 23, 12});
		int port = node.getConfig().getPort();
		RaftConfig raftConfig = new RaftConfig();
		RaftConfig.AuthConfig authConfig = raftConfig.getAuthConfig();
		authConfig.setAuthKey(1);
		authConfig.setAuthValue(new byte[]{79, 23, 12});
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		int id = Utils.cost(service::getId, "getId");
		Assertions.assertEquals(-1, id);
		node.stop();
	}

	@Test
	public void testAuthFailed_valueNotEquals() {
		Node node = genNode(1, new byte[]{32, 43, 23});
		int port = node.getConfig().getPort();
		RaftConfig raftConfig = new RaftConfig();
		RaftConfig.AuthConfig authConfig = raftConfig.getAuthConfig();
		authConfig.setAuthKey(1);
		authConfig.setAuthValue(new byte[]{79, 23, 12});
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		int id = Utils.cost(service::getId, "getId");
		Assertions.assertEquals(-1, id);
		node.stop();
	}

	@Test
	public void testGetId() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		int id = service.getId();
		Assertions.assertEquals(0, id);
		node.stop();
	}

	@Test
	public void testPreVote() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(node, HOST, port);
		PreVoteParam param = new PreVoteParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0);
		Reply reply = service.preVote(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(0, reply.getTerm(), reply.toString());
		node.stop();
	}

	@Test
	public void testRequestVote() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		RequestVoteParam param = new RequestVoteParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0);
		Reply reply = service.requestVote(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
		node.stop();
	}

	@Test
	public void testHeartbeat() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		AppendLogEntriesParam param = new AppendLogEntriesParam(MOCK_NODE_ID, MOCK_NODE_TERM);
		AppendLogReply reply = service.appendLogEntries(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
		node.stop();
	}

	@Test
	public void testAppendLogEntries() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		LogEntry logEntry = new LogEntry(1, MOCK_NODE_TERM, "");
		AppendLogEntriesParam param = new AppendLogEntriesParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0, 0,
				Collections.singletonList(logEntry));
		AppendLogReply reply = service.appendLogEntries(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
		node.stop();
	}

	@Test
	public void testReplicateSnapshot() {
		Node node = genNode();
		int port = node.getConfig().getPort();
		NettyRpcService service = new NettyRpcService(HOST, port, RaftConfig::new);
		ReplicateSnapshotParam param = new ReplicateSnapshotParam(MOCK_NODE_ID, MOCK_NODE_TERM, 0, 0,
				Internal.EMPTY_BYTE_ARRAY);
		Reply reply = service.replicateSnapshot(param);
		Assertions.assertTrue(reply.isSuccess(), reply.toString());
		Assertions.assertEquals(MOCK_NODE_TERM, reply.getTerm(), reply.toString());
		node.stop();
	}

	@RepeatedTest(10)
	public void testElection() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	@Disabled
	public void debugElection() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		System.out.println("============ TEST FINISHED =============");
		sleep(999999);
		stopNodes(nodes);
	}

	// @RepeatedTest(10)
	@Test
	public void testRestart() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		sleep(10 * 1000);
		MockNettyNode leader = findLeader(nodes);
		leader.stop();
		waitUtilStable(nodes);
		sleep(10 * 1000);
		System.out.println("============ RE-ELECTION FINISHED =============");
		leader.start(getFollowers(nodes, leader));
		waitUtilStable(nodes);
		sleep(10 * 1000);
		assertCluster(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	public void testRecoverFromIdle() {
		List<MockNettyNode> nodes = nodesInitUntilStable(3);
		Set<String> ports = nodes.stream().map(node -> node.getConfig().getPort()).map(String::valueOf)
				.collect(Collectors.toSet());
		sleep(10 * 1000);
		printNetstat(ports);
		MockNettyNode leader = findLeader(nodes);
		leader.stop();
		printNetstat(ports);
		waitUtilStable(nodes, 10 * 1000);
		System.out.println("========= TEST FINISHED =========");
		stopNodes(nodes);
	}

	private static void printNetstat(Set<String> ports) {
		System.out.println("========= NETSTAT =========");
		RuntimeUtil.execForLines("netstat", "-ano").stream().filter(line -> {
			for (String port : ports) {
				if (line.contains(port)) {
					return true;
				}
			}
			return false;
		}).forEach(System.out::println);
	}

	private Node genNode() {
		Node node = new Node(0);
		int freePort = BaseTest.findFreePort();
		node.getConfig().setPort(freePort);
		node.start(Collections.emptyList(), true);
		return node;
	}

	private Node genNode(long authKey, byte[] authValue) {
		Node node = new Node(0);
		int freePort = BaseTest.findFreePort();
		node.getConfig().setPort(freePort);
		node.getConfig().getAuthConfig().setAuthKey(authKey);
		node.getConfig().getAuthConfig().setAuthValue(authValue);
		node.start(Collections.emptyList(), true);
		return node;
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
				.map(node -> new NettyRpcService(self, "127.0.0.1", node.getConfig().getPort()))
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
