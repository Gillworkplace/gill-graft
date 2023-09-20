package com.gill.graft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.graft.mock.MockIntMapServer;
import com.gill.graft.mock.NodeRpcWrapper;

import cn.hutool.core.util.RandomUtil;

/**
 * IntMapTest
 *
 * @author gill
 * @version 2023/09/18
 **/
public class IntMapTest extends BaseTest {

	private static List<MockIntMapServer> init(int num) {
		List<MockIntMapServer> servers = new ArrayList<>();
		int offset = RandomUtil.randomInt(1000) * 100;
		for (int i = 0; i < num; i++) {
			servers.add(new MockIntMapServer(offset + i));
		}
		System.out.println("offset: " + offset);
		return servers;
	}

	private static MockIntMapServer findLeader(List<MockIntMapServer> servers) {
		Optional<MockIntMapServer> leaderOpt = servers.stream().filter(MockIntMapServer::isLeader).findFirst();
		if (!leaderOpt.isPresent()) {
			Assertions.fail("not find leader");
		}
		return leaderOpt.get();
	}

	private static MockIntMapServer findFollower(List<MockIntMapServer> servers) {
		Optional<MockIntMapServer> followerOpt = servers.stream().filter(MockIntMapServer::isFollower).findFirst();
		if (!followerOpt.isPresent()) {
			Assertions.fail("not find follower");
		}
		return followerOpt.get();
	}

	private static void waitUtilStable(List<MockIntMapServer> servers) {
		while (true) {
			Optional<MockIntMapServer> leader = servers.stream().filter(MockIntMapServer::isLeader).findFirst();
			if (leader.isPresent() && leader.get().getNode().isStable()) {
				break;
			}
			sleep(10);
		}
	}

	private static void stopServers(List<MockIntMapServer> servers) {
		for (MockIntMapServer server : servers) {
			server.stop();
		}
	}

	private static void assertCluster(List<MockIntMapServer> servers) {
		int leaderCnt = 0;
		int followerCnt = 0;
		int availableCnt = 0;
		for (MockIntMapServer server : servers) {
			if (server.isLeader()) {
				leaderCnt++;
			}
			if (server.isFollower()) {
				followerCnt++;
			}
			if (server.isUp()) {
				availableCnt++;
			}
		}
		try {
			Assertions.assertEquals(1, leaderCnt, "leader 数目异常");
			Assertions.assertEquals(availableCnt - 1, followerCnt, "follower 数目异常");
		} catch (Throwable e) {
			System.out.println("============ AFTER EXCEPTION ===============");
			stopServers(servers);
			throw e;
		}
	}

	private List<NodeRpcWrapper> getRpcNodes(List<MockIntMapServer> servers) {
		return servers.stream().map(MockIntMapServer::getNode).map(NodeRpcWrapper::new).collect(Collectors.toList());
	}

	private List<MockIntMapServer> nodesInit(int num) {
		List<MockIntMapServer> servers = init(num);
		List<NodeRpcWrapper> rpcNodes = getRpcNodes(servers);
		for (MockIntMapServer server : servers) {
			server.start(rpcNodes);
		}
		waitUtilStable(servers);
		assertCluster(servers);
		System.out.println("============ INIT NODES FINISHED ===============");
		return servers;
	}

	@Test
	public void testPutsGetCommand_Leader() {
		List<MockIntMapServer> servers = nodesInit(7);
		MockIntMapServer leader = findLeader(servers);
		leader.set("test", 123);
		leader.set("test", 321);
		System.out.println("============ TEST FINISHED ===============");
		Assertions.assertEquals(321, leader.get("test"));
		stopServers(servers);
	}

	@Test
	public void testPutsGetCommand_Follower() {
		List<MockIntMapServer> servers = nodesInit(5);
		MockIntMapServer leader = findLeader(servers);
		int readIdx1 = leader.set("test", 123);
		int readIdx2 = leader.set("test", 321);
		MockIntMapServer follower = findFollower(servers);
		Assertions.assertEquals(-1, follower.set("test", 1));
		Assertions.assertEquals(321, leader.get("test"));
		Assertions.assertEquals(123, follower.get("test", readIdx1));
		Assertions.assertEquals(-1, follower.get("test", readIdx2));
		System.out.println("============ TEST FINISHED ===============");
		stopServers(servers);
	}

	@Test
	public void testFollowerDownAndUp_FollowerGetCommand() {
		List<MockIntMapServer> servers = nodesInit(5);
		MockIntMapServer leader = findLeader(servers);
		int readIdx1 = leader.set("test", 111);
		int readIdx2 = leader.set("test", 222);
		MockIntMapServer follower = findFollower(servers);
		follower.stop();
		System.out.println("============ FOLLOWER STOPPED ===============");
		int readIdx3 = leader.set("test", 333);
		int readIdx4 = leader.set("test", 444);
		follower.start(getRpcNodes(servers));
		System.out.println("============ FOLLOWER STARTED ===============");
		int readIdx5 = leader.set("test", 555);
		int readIdx6 = leader.set("test", 666);
		Assertions.assertEquals(555, follower.get("test", readIdx1));
		Assertions.assertEquals(555, follower.get("test", readIdx2));
		Assertions.assertEquals(555, follower.get("test", readIdx3));
		Assertions.assertEquals(555, follower.get("test", readIdx4));
		Assertions.assertEquals(555, follower.get("test", readIdx5));
		Assertions.assertEquals(-1, follower.get("test", readIdx6));
		System.out.println("============ TEST FINISHED ===============");
		stopServers(servers);
	}

	@RepeatedTest(30)
	public void testSyncSnapshot() {
		List<MockIntMapServer> servers = nodesInit(5);
		MockIntMapServer leader = findLeader(servers);
		leader.set("test1", 111);
		leader.set("test2", 222);
		leader.set("test2", 333);
		MockIntMapServer follower = findFollower(servers);
		follower.updateCommittedIdx(100);
		follower.clearLogsAndData();
		System.out.println("============ CLEAR FOLLOWER ===============");
		Assertions.assertNull(follower.get("test1"));
		leader.set("test3", 444);
		System.out.println(follower.getNode().println());
		leader.set("test3", 555);
		Assertions.assertEquals(111, follower.get("test1"));
		Assertions.assertEquals(333, follower.get("test2"));
		Assertions.assertEquals(444, follower.get("test3"));
		stopServers(servers);
	}
}
