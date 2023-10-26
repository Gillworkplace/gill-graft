package com.gill.graft.apis;

/**
 * Server
 *
 * @author gill
 * @version 2023/10/26
 **/
public interface Server {

	/**
	 * 是否可以对外提供服务
	 * 
	 * @return 是否
	 */
	boolean isReady();

	/**
	 * 启动服务器
	 */
	void start();

	/**
	 * 停止服务器
	 */
	void stop();
}
