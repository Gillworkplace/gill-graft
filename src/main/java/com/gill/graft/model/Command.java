package com.gill.graft.model;

/**
 * Command
 *
 * @author gill
 * @version 2023/09/07
 **/
public class Command {

	public enum Type {

		/**
		 * 不会影响状态机状态的操作
		 */
		SELECT,

		/**
		 * 会印象状态机状态的操作
		 */
		UPDATE
	}

	private final Type type;

	private final String command;

	public Command(Type type, String command) {
		this.type = type;
		this.command = command;
	}

	@Override
	public String toString() {
		return "Command{" + "type=" + type + ", command='" + command + '\'' + '}';
	}
}
