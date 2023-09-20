package com.gill.graft.service;

import com.gill.graft.model.ProposeReply;

/**
 * ClusterService
 *
 * @author gill
 * @version 2023/09/04
 **/
public interface RaftService extends NodeService {

	/**
	 * 提案
	 *
	 * @param command
	 *            更新操作
	 * @return 日志索引位置
	 */
	ProposeReply propose(String command);

	/**
	 * 校验读索引
	 * 
	 * @param readIdx
	 *            索引
	 * @return 是否成功
	 */
	boolean checkReadIndex(int readIdx);
}
