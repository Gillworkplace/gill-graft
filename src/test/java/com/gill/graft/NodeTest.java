package com.gill.graft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.mock.MockNode;
import com.gill.graft.model.ProposeReply;

import cn.hutool.core.util.RandomUtil;

/**
 * ClusterTest
 *
 * @author gill
 * @version 2023/09/04
 **/
public class NodeTest extends BaseTest {

	private List<RaftRpcService> getFollowers(List<MockNode> nodes, Node self) {
		return nodes.stream().filter(rpcNode -> self.getId() != rpcNode.getId()).collect(Collectors.toList());
	}

	private List<MockNode> nodesInit(int num, long waitTime) {
		List<MockNode> nodes = init(num);
		for (MockNode node : nodes) {
			node.start(getFollowers(nodes, node));
		}
		sleep(waitTime);
		assertCluster(nodes);
		return nodes;
	}

	private List<MockNode> nodesInitUntilStable(int num) {
		return nodesInitUntilStable(num, null);
	}

	private List<MockNode> nodesInitUntilStable(int num, Integer defaultPriority) {
		List<MockNode> nodes = init(num);
		for (MockNode node : nodes) {
			List<RaftRpcService> followers = getFollowers(nodes, node);
			if (defaultPriority == null) {
				node.start(followers);
			} else {
				node.start(followers, defaultPriority);
			}
		}
		waitUtilLeaderStable(nodes);
		assertCluster(nodes);
		return nodes;
	}

	private List<MockNode> init(int num) {
		List<MockNode> nodes = new ArrayList<>();
		int offset = RandomUtil.randomInt(1000) * 100;
		for (int i = 0; i < num; i++) {
			nodes.add(MockNode.newNode(offset + i));
		}
		System.out.println("offset: " + offset);
		return nodes;
	}

	@Test
	public void testNodeInit() {
		List<MockNode> nodes = nodesInit(1, 250);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	/**
	 * 节点初始化在500ms内能选出主节点
	 */
	@RepeatedTest(30)
	public void testNodesInit() {
		List<MockNode> nodes = nodesInit(5, 500);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@RepeatedTest(30)
	public void testNodesInitIfStable() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@RepeatedTest(30)
	public void testSamePriorityInitIfStable_Normal() {
		List<MockNode> nodes = nodesInitUntilStable(5, 0);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@RepeatedTest(30)
	public void testSamePriorityInitIfStable_Extra() {
		List<MockNode> nodes = nodesInitUntilStable(21, 0);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	/**
	 * 移除leader后能否重新选出节点
	 */
	@RepeatedTest(30)
	public void testRemoveLeader() {
		final int num = 5;
		List<MockNode> nodes = nodesInitUntilStable(num);
		MockNode leader = findLeader(nodes);
		System.out.println("remove leader " + leader.getId());
		leader.stop();
		long start = System.currentTimeMillis();
		waitUtilLeaderStable(nodes);
		System.out.println("cost: " + (System.currentTimeMillis() - start));
		assertCluster(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	/**
	 * 并发提交提案
	 */
	@Test
	public void testPropose() throws ExecutionException, InterruptedException {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode leader = findLeader(nodes);
		System.out.println("============ PROPOSE =============");
		int concurrency = 20;
		CompletableFuture<?>[] futures = IntStream.range(0, concurrency)
				.mapToObj(x -> CompletableFuture.supplyAsync(() -> leader.propose(String.valueOf(x % 10))))
				.toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(futures).join();
		for (int i = 0; i < futures.length; i++) {
			Assertions.assertTrue(((ProposeReply) futures[i].get()).isSuccess(), "i: " + i);
		}
		assertLogs(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	/**
	 * 正常提交多次后，其中1台follower宕机，继续提交多次，最终状态一直。
	 */
	@RepeatedTest(5)
	public void testRepairLogs_AfterFollowerDownAndUp() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode leader = findLeader(nodes);
		leader.propose("1");
		MockNode follower = findFollower(nodes);
		follower.stop();
		System.out.println("============ FOLLOWER STOPPED =============");
		leader.propose("2");
		follower.start(getFollowers(nodes, follower));
		System.out.println("============ FOLLOWER STARTED =============");
		leader.propose("3");
		assertAllLogs(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	/**
	 * 正常提交多次后，其中1台follower宕机，继续提交多次，最终状态一直。
	 */
	@RepeatedTest(5)
	public void testRepairLogs_AfterLeaderDownAndUp() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode originLeader = findLeader(nodes);
		originLeader.propose("1");
		originLeader.propose("2");
		MockNode follower = findFollower(nodes);
		follower.stop();
		System.out.println("============ FOLLOWER STOPPED =============");
		originLeader.propose("after follower stop 3");
		originLeader.propose("after follower stop 4");
		originLeader.stop();
		System.out.println("============ LEADER STOPPED =============");
		follower.start(getFollowers(nodes, follower));
		waitUtilLeaderStable(nodes);
		MockNode newLeader = findLeader(nodes);
		System.out.println("============ FIND NEW LEADER =============");
		originLeader.start(getFollowers(nodes, originLeader));
		System.out.println("============ ORIGIN LEADER STARTED =============");
		ProposeReply propose5 = newLeader.propose("all up 5");
		Assertions.assertTrue(propose5.isSuccess());
		ProposeReply proposeF = originLeader.propose("follower propose");
		Assertions.assertFalse(proposeF.isSuccess());
		assertAllLogs(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@RepeatedTest(5)
	public void testRepairLogs_AfterLeaderDown() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode originLeader = findLeader(nodes);
		originLeader.propose("1");
		originLeader.stop();
		System.out.println("============ LEADER STOPPED =============");
		waitUtilLeaderStable(nodes);
		MockNode newLeader = findLeader(nodes);
		System.out.println("============ FIND NEW LEADER =============");
		ProposeReply propose2 = newLeader.propose("2");
		Assertions.assertTrue(propose2.isSuccess());
		Assertions.assertEquals(4, (int) propose2.getIdx());
		ProposeReply propose123 = originLeader.propose("123");
		Assertions.assertFalse(propose123.isSuccess());
		assertAllLogs(nodes, originLeader);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	public void testRepairLogs_AfterFollowerDown() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode leader = findLeader(nodes);
		leader.propose("1");
		MockNode follower = findFollower(nodes);
		follower.stop();
		System.out.println("============ FOLLOWER STOPPED =============");
		ProposeReply propose = leader.propose("2");
		Assertions.assertTrue(propose.isSuccess());
		Assertions.assertEquals(3, (int) propose.getIdx());
		assertAllLogs(nodes, follower);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	public void testRepairLogs_AfterMoreThanHalfFollowersDown_Failed() {
		int n = 5;
		List<MockNode> nodes = nodesInitUntilStable(n);
		MockNode leader = findLeader(nodes);
		leader.propose("1");
		for (int i = 0; i < n / 2 + 1; i++) {
			MockNode follower = findFollower(nodes);
			follower.stop();
		}
		System.out.println("============ FOLLOWERS STOPPED =============");
		ProposeReply propose = leader.propose("2");
		Assertions.assertFalse(propose.isSuccess());
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}
}
