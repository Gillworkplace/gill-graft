package com.gill.graft.state;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.LogManager;
import com.gill.graft.Node;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.common.Utils;
import com.gill.graft.entity.PreVoteParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.machine.RaftEvent;
import com.gill.graft.machine.RaftEventParams;

import cn.hutool.core.util.RandomUtil;
import javafx.util.Pair;

/**
 * PreCandidate
 *
 * @author gill
 * @version 2023/09/05
 **/
public class PreCandidate {

	private static final Logger log = LoggerFactory.getLogger(PreCandidate.class);

	/**
	 * 预投票
	 *
	 * @param self
	 *            发起节点
	 * @param params
	 *            params
	 */
	public static void preVote(Node self, RaftEventParams params) {
		long pTerm = params.getTerm();
		log.info("start to pre-vote when term: {}", pTerm);
		ExecutorService clusterPool = self.getThreadPools().getClusterPool();
		List<RaftRpcService> followers = self.getFollowers();
		boolean success = Utils.majorityCall(followers, follower -> doPreVote(self, follower, params), Reply::isSuccess,
				clusterPool, "pre-vote");
		if (success) {
			self.publishEvent(RaftEvent.TO_CANDIDATE, new RaftEventParams(pTerm));
		} else {
			log.debug("pre-vote failed when term: {}", pTerm);

			// 增加随机时间
			Utils.sleepQuietly(RandomUtil.randomInt(10));
			self.stepDown();
		}
	}

	private static Reply doPreVote(Node self, RaftRpcService follower, RaftEventParams params) {
		LogManager logManager = self.getLogManager();
		Pair<Long, Integer> lastLog = logManager.lastLog();
		int nodeId = self.getId();
		long term = params.getTerm();
		long lastLogTerm = lastLog.getKey();
		int lastLogIdx = lastLog.getValue();
		return follower.preVote(new PreVoteParam(nodeId, term, lastLogTerm, lastLogIdx));
	}
}
