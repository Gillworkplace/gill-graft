package com.gill.graft.service;

import java.util.List;

import com.gill.graft.apis.RaftRpcService;

/**
 * NodeService
 *
 * @author gill
 * @version 2023/09/04
 **/
public interface NodeService {

	/**
	 * 是否状态机是否准备就绪
	 * 
	 * @return 是否
	 */
	boolean isMachineReady();

	/**
	 * server是否准备继续
	 * 
	 * @return 是否
	 */
	boolean isServerReady();

	/**
	 * 启动节点
	 *
	 * @param nodes
	 *            集群节点
	 */
	void start(List<RaftRpcService> nodes);

	/**
	 * 停止节点
	 */
	void stop();

	/**
	 * 清除节点数据
	 */
	void clear();
}
