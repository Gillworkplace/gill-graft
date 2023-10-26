package com.gill.graft.state;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.Node;

/**
 * Common
 *
 * @author gill
 * @version 2023/09/06
 **/
public class Common {

	private static final Logger log = LoggerFactory.getLogger(Common.class);

	/**
	 * 初始化follower
	 *
	 * @param self
	 *            节点
	 */
	public static void stop(Node self) {
		CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
			log.info("start to remove cluster pool");
			self.getThreadPools().setClusterPool(null);
			log.info("finished to remove cluster pool");
		});
		CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
			log.info("start to remove api pool");
			self.getThreadPools().setApiPool(null);
			log.info("finished to remove api pool");
		});
		CompletableFuture.allOf(f1, f2).join();

	}

	/**
	 * 打印debug日志
	 * 
	 * @param template
	 *            模板
	 * @param params
	 *            参数
	 */
	public static void debug(String template, Object... params) {
		log.debug(template, params);
	}
}
