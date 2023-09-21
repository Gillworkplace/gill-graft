package com.gill.graft.apis.empty;

import com.gill.graft.apis.CommandSerializer;
import com.gill.graft.apis.VersionDataStorage;
import com.gill.graft.model.Snapshot;

/**
 * EmptyRepository
 *
 * @author gill
 * @version 2023/09/07
 **/
public class EmptyDataStorage extends VersionDataStorage<String> {

	private CommandSerializer<String> commandSerializer = new EmptyCommandSerializer();

	@Override
	public int getApplyIdx() {
		return 0;
	}

	@Override
	public byte[] getSnapshotData() {
		return new byte[0];
	}

	@Override
	public CommandSerializer<String> getCommandSerializer() {
		return commandSerializer;
	}

	@Override
	public int loadSnapshot() {
		return 0;
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
