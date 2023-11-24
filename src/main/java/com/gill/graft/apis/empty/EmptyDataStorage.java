package com.gill.graft.apis.empty;

import com.gill.graft.apis.CommandSerializer;
import com.gill.graft.apis.VersionDataStorage;
import com.gill.graft.model.Snapshot;

import javafx.util.Pair;

/**
 * EmptyRepository
 *
 * @author gill
 * @version 2023/09/07
 **/
public class EmptyDataStorage extends VersionDataStorage<String> {

	public static final EmptyDataStorage INSTANCE = new EmptyDataStorage();

	private final CommandSerializer<String> commandSerializer = new EmptyCommandSerializer();

	@Override
	public byte[] deepCopySnapshotData() {
		return new byte[0];
	}

	@Override
	public CommandSerializer<String> getCommandSerializer() {
		return commandSerializer;
	}

	/**
	 * 加载快照应用的索引位置与版本
	 *
	 * @return 版本, 索引位置
	 */
	@Override
	protected Pair<Long, Integer> loadApplyTermAndIdx() {
		return new Pair<>(0L, 0);
	}

	/**
	 * 加载数据
	 */
	@Override
	protected void loadData() {
	}

	/**
	 * 校验命令
	 *
	 * @param command
	 *            命令
	 * @return 校验是否通过
	 */
	@Override
	public String doValidateCommand(String command) {
		return "";
	}

	@Override
	public void saveSnapshotToFile(Snapshot snapshot) {

	}

	@Override
	public void saveSnapshot(byte[] data) {

	}

	@Override
	public Object apply(String command) {
		return "";
	}

	@Override
	public String println() {
		return "";
	}
}
