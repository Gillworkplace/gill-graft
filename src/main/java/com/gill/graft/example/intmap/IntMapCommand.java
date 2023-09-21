package com.gill.graft.example.intmap;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * MapCommand
 *
 * @author gill
 * @version 2023/09/07
 **/
public class IntMapCommand {

	public enum Type {

		/**
		 * 查询操作
		 */
		GET((map, command) -> map.get(command.getKey())),

		/**
		 * 设置操作
		 */
		PUT((map, command) -> {
			map.put(command.getKey(), command.getValue());
			return 1;
		});

		private final BiFunction<Map<String, Integer>, IntMapCommand, Integer> handler;

		Type(BiFunction<Map<String, Integer>, IntMapCommand, Integer> handler) {
			this.handler = handler;
		}
	}

	private Type type;

	private String key;

	private Integer value;

	/**
	 * 执行命令
	 *
	 * @param map
	 *            map
	 * @param command
	 *            命令
	 * @return 结果
	 */
	public Integer execute(Map<String, Integer> map, IntMapCommand command) {
		return this.type.handler.apply(map, command);
	}

	public Type getType() {
		return type;
	}

	public String getKey() {
		return key;
	}

	public Integer getValue() {
		return value;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public IntMapCommand(Type type, String key) {
		this.type = type;
		this.key = key;
	}

	public IntMapCommand(Type type, String key, Integer value) {
		this.type = type;
		this.key = key;
		this.value = value;
	}

	@Override
	public String toString() {
		return "IntMapCommand{" + "type=" + type + ", key='" + key + '\'' + ", value=" + value + '}';
	}
}
