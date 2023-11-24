package com.gill.graft.apis;

import com.gill.graft.model.Snapshot;
import com.gill.graft.service.PrintService;

/**
 * Apply
 *
 * @author gill
 * @version 2023/09/07
 **/
public interface DataStorage extends PrintService {

	/**
	 * 获取applyIdx
	 *
	 * @return applyIdx
	 */
	int getApplyIdx();

	/**
	 * 获取磁盘快照的applyIdx
	 * 
	 * @return 快照的applyIdx
	 */
	int getSnapshotApplyIdx();

	/**
	 * 加载数据
	 *
	 * @return applyIdx
	 */
	int loadSnapshot();

	/**
	 * 获取当前快照
	 * 
	 * @return 快照
	 */
	Snapshot getSnapshot();

	/**
	 * 保存数据
	 */
	void saveSnapshotToFile();

	/**
	 * 保存快照
	 * 
	 * @param applyTerm
	 *            日志任期
	 * @param applyIdx
	 *            日志idx
	 * @param data
	 *            快照数据
	 */
	void saveSnapshot(long applyTerm, int applyIdx, byte[] data);

	/**
	 * 应用命令
	 * 
	 * @param logTerm
	 *            日志任期
	 * @param logIdx
	 *            日志索引
	 * @param command
	 *            命令
	 * @return 返回值
	 */
	Object apply(long logTerm, int logIdx, String command);

	/**
	 * 校验命令
	 * 
	 * @param command
	 *            命令
	 * @return String
	 */
	String validateCommand(String command);
}
