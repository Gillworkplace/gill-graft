package com.gill.graft.logging;

/**
 * LogException
 *
 * @author gill
 * @version 2023/09/20
 **/
public class LogException extends RuntimeException {
	private static final long serialVersionUID = 1022924004852350942L;

	public LogException() {
	}

	public LogException(String message) {
		super(message);
	}

	public LogException(String message, Throwable cause) {
		super(message, cause);
	}

	public LogException(Throwable cause) {
		super(cause);
	}
}
