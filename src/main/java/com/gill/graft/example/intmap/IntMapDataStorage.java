package com.gill.graft.example.intmap;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.util.Pair;
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

	private Map<String, Integer> map = new ConcurrentHashMap<>(1024);

	private final Map<String, AtomicInteger> mapCnt = new ConcurrentHashMap<>(1024);

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

	public Integer cnt(String key) {
		return mapCnt.getOrDefault(key, new AtomicInteger(0)).get();
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
		this.map = new ConcurrentHashMap<>(1024);
	}

	@Override
	public byte[] deepCopySnapshotData() {
		String mapStr = JSONUtil.toJsonStr(map);
		return mapStr.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public CommandSerializer<IntMapCommand> getCommandSerializer() {
		return serializer;
	}

	@Override
	public Object apply(IntMapCommand command) {
		AtomicInteger updateCnt = mapCnt.computeIfAbsent(command.getKey(), key -> new AtomicInteger(0));
		updateCnt.incrementAndGet();
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
