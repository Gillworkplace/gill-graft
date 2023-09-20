package com.gill.graft.state;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.gill.graft.LogManager;
import com.gill.graft.Node;
import com.gill.graft.ProposeHelper;
import com.gill.graft.common.Utils;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.Reply;
import com.gill.graft.machine.RaftEventParams;
import com.gill.graft.service.InnerNodeService;

/**
 * Leader
 *
 * @author gill
 * @version 2023/09/05
 **/
public class Leader {

	/**
	 * 启动心跳定时任务
	 *
	 * @param self
	 *            节点
	 */
	public static void startHeartbeatSchedule(Node self, RaftEventParams params) {
		log.debug("starting heartbeat scheduler");
		ExecutorService heartbeatPool = self.getThreadPools().getClusterPool();
		int selfId = self.getID();
		long term = params.getTerm();
		self.getSchedulers().setHeartbeatScheduler(() -> {
			List<InnerNodeService> followers = self.getFollowers();
			log.debug("broadcast heartbeat");
			boolean success = Utils.majorityCall(followers,
					follower -> doHeartbeat(selfId, term, follower, self::unstable), Reply::isSuccess, heartbeatPool,
					"heartbeat");
			if (!success) {
				log.warn("broadcast heartbeat failed");
				self.stepDown();
			}
		}, self.getConfig(), selfId);
	}

	private static Reply doHeartbeat(int nodeId, long term, InnerNodeService follower, Runnable extraFunc) {
		AppendLogEntriesParam param = new AppendLogEntriesParam(nodeId, term);
		AppendLogReply reply = new AppendLogReply(false, -1);
		try {
			reply = follower.appendLogEntries(param);
		} catch (Exception e) {
			log.error("call heartbeat to {} failed, param: {}, e: {}", follower.getID(), param, e.getMessage());
		}
		if (!reply.isSuccess()) {
			if (reply.getTerm() > term) {
				extraFunc.run();
			}
			log.error("call heartbeat to {} failed, param: {}, reply: {}", follower.getID(), param, reply);
		}
		return reply;
	}

	/**
	 * 停止心跳定时任务
	 *
	 * @param self
	 *            节点
	 */
	public static void stopHeartbeatSchedule(Node self) {
		log.debug("stopping heartbeat scheduler");
		self.getSchedulers().clearHeartbeatScheduler();
	}

	/**
	 * 发送noOp指令
	 * 
	 * @param self
	 *            节点
	 */
	public static void noOp(Node self) {
		if (self.propose(Utils.NO_OP).getIdx() >= 0) {
			self.stable();
		}
	}

	/**
	 * 初始化leader
	 * 
	 * @param self
	 *            节点
	 */
	public static void init(Node self) {
		log.debug("init propose helper");
		ProposeHelper proposeHelper = self.getProposeHelper();
		LogManager logManager = self.getLogManager();
		int lastLogIdx = logManager.lastLog().getValue();
		proposeHelper.start(self, self.getFollowers(), lastLogIdx);
	}

	/**
	 * 停止leader的任务
	 * 
	 * @param self
	 *            节点
	 */
	public static void clear(Node self) {
		log.debug("clear propose helper");
		ProposeHelper proposeHelper = self.getProposeHelper();
		proposeHelper.clear();
	}
}
