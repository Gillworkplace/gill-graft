package com.gill.graft.example.intmap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.apis.CommandSerializer;
import com.gill.graft.apis.VersionDataStorage;
import com.gill.graft.model.Snapshot;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;

/**
 * Repository
 *
 * @author gill
 * @version 2023/09/07
 **/
public class IntMapDataStorage extends VersionDataStorage<IntMapCommand> {

	private static final Logger log = LoggerFactory.getLogger(IntMapDataStorage.class);

	private Map<String, Integer> map = new HashMap<>(1024);

	private final IntMapCommandSerializer serializer = new IntMapCommandSerializer();

	/**
	 * 获取
	 * 
	 * @param key
	 *            key
	 * @return value
	 */
	public Integer get(String key) {
		return map.get(key);
	}

	@Override
	public int getApplyIdx() {
		return 0;
	}

	@Override
	public int loadSnapshot() {
		map = new HashMap<>(1024);
		return 0;
	}

	@Override
	public byte[] getSnapshotData() {
		String mapStr = JSONUtil.toJsonStr(map);
		return mapStr.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public CommandSerializer<IntMapCommand> getCommandSerializer() {
		return serializer;
	}

	@Override
	public Object apply(IntMapCommand command) {
		return command.execute(map, command);
	}

	@Override
	public void saveSnapshotToFile(Snapshot snapshot) {
		log.debug("ignore saving snapshot to file");
	}

	@Override
	public void saveSnapshot(byte[] data) {
		map = JSONUtil.toBean(new String(data, StandardCharsets.UTF_8), new TypeReference<Map<String, Integer>>() {
		}, true);
	}

	@Override
	public String println() {
		return JSONUtil.toJsonStr(map);
	}
}
