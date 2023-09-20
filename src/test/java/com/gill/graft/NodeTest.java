package com.gill.graft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.graft.mock.MockNode;
import com.gill.graft.model.LogEntry;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;

/**
 * ClusterTest
 *
 * @author gill
 * @version 2023/09/04
 **/
public class NodeTest extends BaseTest {

	private List<MockNode> nodesInit(int num, long waitTime) {
		List<MockNode> nodes = init(num);
		for (MockNode node : nodes) {
			node.start(nodes);
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
			if (defaultPriority == null) {
				node.start(nodes);
			} else {
				node.start(nodes, defaultPriority);
			}
		}
		waitUtilStable(nodes);
		assertCluster(nodes);
		return nodes;
	}

	private static void assertCluster(List<MockNode> nodes) {
		int leaderCnt = 0;
		int followerCnt = 0;
		int availableCnt = 0;
		for (MockNode node : nodes) {
			if (node.isLeader()) {
				leaderCnt++;
			}
			if (node.isFollower()) {
				followerCnt++;
			}
			if (node.isUp()) {
				availableCnt++;
			}
		}
		try {
			Assertions.assertEquals(1, leaderCnt, "leader 数目异常");
			Assertions.assertEquals(availableCnt - 1, followerCnt, "follower 数目异常");
		} catch (Throwable e) {
			System.out.println("============ AFTER EXCEPTION ===============");
			stopNodes(nodes);
			throw e;
		}
	}

	private static void assertLogs(List<MockNode> nodes) {
		Optional<MockNode> leaderOpt = nodes.stream().filter(MockNode::isLeader).findFirst();
		if (!leaderOpt.isPresent()) {
			Assertions.fail("not find leader");
		}
		MockNode leader = leaderOpt.get();
		List<LogEntry> logs = leader.getLog();
		String expected = JSONUtil.toJsonStr(logs);
		int cnt = 0;
		try {
			for (MockNode node : nodes) {
				if (expected.equals(JSONUtil.toJsonStr(node.getLog()))) {
					cnt++;
				}
			}
			Assertions.assertTrue(cnt > nodes.size() / 2);
		} catch (Throwable e) {
			System.out.println("============ AFTER EXCEPTION ===============");
			stopNodes(nodes);
			throw e;
		}
	}

	private static void assertAllLogs(List<MockNode> nodes, MockNode... excludeNodes) {
		Optional<MockNode> leaderOpt = nodes.stream().filter(MockNode::isLeader).findFirst();
		if (!leaderOpt.isPresent()) {
			Assertions.fail("not find leader");
		}
		MockNode leader = leaderOpt.get();
		List<LogEntry> logs = leader.getLog();
		String expected = JSONUtil.toJsonStr(logs);
		Set<Integer> excludeSet = Arrays.stream(excludeNodes).map(Node::getId).collect(Collectors.toSet());
		try {
			for (MockNode node : nodes) {
				if (excludeSet.contains(node.getId())) {
					continue;
				}
				Assertions.assertEquals(expected, JSONUtil.toJsonStr(node.getLog()));
			}
		} catch (Throwable e) {
			System.out.println("============ AFTER EXCEPTION ===============");
			stopNodes(nodes);
			throw e;
		}
	}

	private static void waitUtilStable(List<MockNode> nodes) {
		while (true) {
			Optional<MockNode> leader = nodes.stream().filter(MockNode::isLeader).findFirst();
			if (leader.isPresent() && leader.get().isStable()) {
				break;
			}
			sleep(10);
		}
	}

	private static MockNode findLeader(List<MockNode> nodes) {
		Optional<MockNode> leaderOpt = nodes.stream().filter(MockNode::isLeader).findFirst();
		if (!leaderOpt.isPresent()) {
			Assertions.fail("not find leader");
		}
		return leaderOpt.get();
	}

	private static MockNode findFollower(List<MockNode> nodes) {
		Optional<MockNode> followerOpt = nodes.stream().filter(MockNode::isFollower).findFirst();
		if (!followerOpt.isPresent()) {
			Assertions.fail("not find follower");
		}
		return followerOpt.get();
	}

	private List<MockNode> init(int num) {
		List<MockNode> nodes = new ArrayList<>();
		int offset = RandomUtil.randomInt(1000) * 100;
		for (int i = 0; i < num; i++) {
			nodes.add(new MockNode(offset + i));
		}
		System.out.println("offset: " + offset);
		return nodes;
	}

	private static void stopNodes(List<MockNode> nodes) {
		for (MockNode node : nodes) {
			node.stop();
		}
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
		waitUtilStable(nodes);
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
			Assertions.assertNotEquals("-1", String.valueOf(futures[i].get()), "i: " + i);
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
		follower.start(nodes);
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
		MockNode follower = findFollower(nodes);
		follower.stop();
		System.out.println("============ FOLLOWER STOPPED =============");
		MockNode originLeader = findLeader(nodes);
		originLeader.propose("1");
		originLeader.stop();
		System.out.println("============ LEADER STOPPED =============");
		follower.start(nodes);
		waitUtilStable(nodes);
		MockNode newLeader = findLeader(nodes);
		System.out.println("============ FIND NEW LEADER =============");
		originLeader.start(nodes);
		System.out.println("============ ORIGIN LEADER STARTED =============");
		Assertions.assertEquals(4, (int) newLeader.propose("2").getIdx());
		Assertions.assertEquals(-1, originLeader.propose("123").getIdx());
		assertAllLogs(nodes);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	public void testRepairLogs_AfterLeaderDown() {
		List<MockNode> nodes = nodesInitUntilStable(5);
		MockNode originLeader = findLeader(nodes);
		originLeader.propose("1");
		originLeader.stop();
		System.out.println("============ LEADER STOPPED =============");
		waitUtilStable(nodes);
		MockNode newLeader = findLeader(nodes);
		System.out.println("============ FIND NEW LEADER =============");
		Assertions.assertEquals(4, (int) newLeader.propose("2").getIdx());
		Assertions.assertEquals(-1, originLeader.propose("123").getIdx());
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
		Assertions.assertEquals(3, (int) leader.propose("2").getIdx());
		assertAllLogs(nodes, follower);
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}

	@Test
	public void testRepairLogs_AfterMoreFollowersDown_Failed() {
		int n = 5;
		List<MockNode> nodes = nodesInitUntilStable(n);
		MockNode leader = findLeader(nodes);
		leader.propose("1");
		for (int i = 0; i < n / 2 + 1; i++) {
			MockNode follower = findFollower(nodes);
			follower.stop();
		}
		System.out.println("============ FOLLOWERS STOPPED =============");
		Assertions.assertEquals(-1, (int) leader.propose("2").getIdx());
		System.out.println("============ TEST FINISHED =============");
		stopNodes(nodes);
	}
}
