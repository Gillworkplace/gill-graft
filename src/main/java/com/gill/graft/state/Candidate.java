package com.gill.graft.state;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.gill.graft.config.RaftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.LogManager;
import com.gill.graft.Node;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.common.Utils;
import com.gill.graft.entity.Reply;
import com.gill.graft.entity.RequestVoteParam;
import com.gill.graft.machine.RaftEvent;
import com.gill.graft.machine.RaftEventParams;

import cn.hutool.core.util.RandomUtil;
import javafx.util.Pair;

/**
 * Candidate
 *
 * @author gill
 * @version 2023/09/05
 **/
public class Candidate {

	private static final Logger log = LoggerFactory.getLogger(Candidate.class);

	/**
	 * 投票
	 *
	 * @param self
	 *            节点
	 * @param params
	 *            params
	 */
	public static void vote(Node self, RaftEventParams params) {

		// 再次检验是否超时，降低在vote之前更新了超时时间导致本次选举失败的情况
		if (isTimeout(self)) {
			self.stepDown();
			return;
		}

		long pTerm = params.getTerm();
		long nextTerm = self.electSelf(pTerm);

		// 该节点已投票，直接返回失败
		if (nextTerm == -1) {
			log.debug("vote for self failed when pTerm: {}, cTerm: {}", pTerm, self.getTerm());
			self.stepDown();
			return;
		}
		log.info("vote for self when term: {}", nextTerm);
		ExecutorService clusterPool = self.getThreadPools().getClusterPool();
		List<RaftRpcService> followers = self.getFollowers();
		boolean success = Utils.majorityCall(followers, follower -> doVote(self, follower, nextTerm), Reply::isSuccess,
				clusterPool, "vote");
		if (success) {
			self.publishEvent(RaftEvent.TO_LEADER, new RaftEventParams(nextTerm));
		} else {
			log.debug("request vote failed when term: {}", nextTerm);

			// 增加随机时间
			Utils.sleepQuietly(RandomUtil.randomInt(10));
			self.stepDown();
		}
	}

	private static boolean isTimeout(Node self) {
		RaftConfig newConfig = self.getConfig();
		Pair<Long, Long> pair = self.getHeartbeatState().get();
		long lastHeartbeatTimestamp = pair.getValue();
		long now = System.currentTimeMillis();
		long diff = now - lastHeartbeatTimestamp;
		return diff <= newConfig.getBaseTimeoutInterval();
	}

	private static Reply doVote(Node self, RaftRpcService follower, long term) {
		LogManager logManager = self.getLogManager();
		Pair<Long, Integer> lastLog = logManager.lastLog();
		int nodeId = self.getId();
		long lastLogTerm = lastLog.getKey();
		int lastLogIdx = lastLog.getValue();
		return follower.requestVote(new RequestVoteParam(nodeId, term, lastLogTerm, lastLogIdx));
	}
}
