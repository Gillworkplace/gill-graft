package com.gill.graft.mock;

import java.util.List;

import com.gill.consensus.raftplus.model.LogEntry;

/**
 * TestMethod
 *
 * @author gill
 * @version 2023/09/04
 **/
public interface TestMethod {

	/**
	 * 是否正在运行
	 *
	 * @return up
	 */
	boolean isUp();

	/**
	 * 是否为leader
	 * 
	 * @return 是否
	 */
	boolean isLeader();

	/**
	 * 是否为follower
	 * 
	 * @return 是否
	 */
	boolean isFollower();

	/**
	 * 获取存储集合
	 *
	 * @return 集合
	 */
	List<LogEntry> getLog();
}
