package com.gill.graft.machine;

import java.util.function.BiConsumer;

import com.gill.graft.Node;
import com.gill.graft.logging.Log;
import com.gill.graft.logging.LogFactory;
import com.gill.graft.state.Candidate;
import com.gill.graft.state.Common;
import com.gill.graft.state.Follower;
import com.gill.graft.state.Leader;
import com.gill.graft.state.PreCandidate;

/**
 * RaftAction
 *
 * @author gill
 * @version 2023/09/04
 **/
public enum RaftAction {

	/**
	 * 空处理
	 */
	EMPTY((node, params) -> {
	}),

	/**
	 * 初始化
	 */
	INIT((node, params) -> Follower.init(node)),

	/**
	 * 停止
	 */
	PRE_STOP((node, params) -> {
		log.debug("pre-stop, state: {}", node.println());
	}),

	/**
	 * 清除请求线程池
	 */
	CLEAR_POOL((node, params) -> Common.stop(node)),

	/**
	 * 停止
	 */
	POST_STOP((node, params) -> {
		log.debug("post-stop, state: {}", node.println());
	}),

	/**
	 * 成为follower
	 */
	POST_FOLLOWER((node, params) -> {
		Follower.startTimeoutScheduler(node);
	}),

	/**
	 * 移除follower定时任务
	 */
	REMOVE_FOLLOWER_SCHEDULER((node, params) -> Follower.stopTimeoutScheduler(node)),

	/**
	 * 成为预候选者
	 */
	TO_PRE_CANDIDATE(PreCandidate::preVote),

	/**
	 * 成为候选者
	 */
	POST_CANDIDATE(Candidate::vote),

	/**
	 * leader准备
	 */
	INIT_LEADER((node, params) -> {
		Leader.init(node);
	}),

	/**
	 * 成为Leader
	 */
	POST_LEADER((node, params) -> {
		log.debug("become to leader when term is {}", params.getTerm());
		log.debug(node.println());
		Leader.startHeartbeatSchedule(node, params);
		Leader.noOp(node);
	}),

	/**
	 * 移除follower定时任务
	 */
	REMOVE_LEADER_SCHEDULER((node, params) -> {
		Leader.stopHeartbeatSchedule(node);
		Leader.clear(node);
	});

	private final BiConsumer<Node, RaftEventParams> func;

	private static final Log log = LogFactory.getLog(RaftAction.class);

	RaftAction(BiConsumer<Node, RaftEventParams> func) {
		this.func = func;
	}

	/**
	 * 动作
	 *
	 * @param node
	 *            节点
	 * @param params
	 *            参数
	 */
	public void action(Node node, RaftEventParams params) {
		func.accept(node, params);
	}
}
