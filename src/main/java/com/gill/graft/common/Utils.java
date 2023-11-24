package com.gill.graft.common;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.RandomUtil;

/**
 * Utils
 *
 * @author gill
 * @version 2023/09/13
 **/
public class Utils {

	private static final Logger log = LoggerFactory.getLogger(Utils.class);

	public static final String NO_OP = "NOOP";

	public static final String SYNC = "SYNC";

	public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

	/**
	 * 睡眠
	 * 
	 * @param time
	 *            时间 （毫秒）
	 */
	public static void sleepQuietly(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			log.warn("thread is interrupted, e: {}", e.getMessage());
		}
	}

	/**
	 * 等待线程池关闭
	 * 
	 * @param executorService
	 *            线程池
	 * @param poolName
	 *            线程池名
	 */
	public static void awaitTermination(ExecutorService executorService, String poolName) {
		long start = System.currentTimeMillis();
		try {
			while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
				log.debug("waiting {} termination, duration: {}", poolName, System.currentTimeMillis() - start);
			}
			log.debug("{} is terminated, duration: {}", poolName, System.currentTimeMillis() - start);
		} catch (InterruptedException e) {
			log.warn("{} awaitTermination is interrupted, e: {}", poolName, e);
		}
	}

	/**
	 * majorityCall
	 *
	 * @param nodes
	 *            nodes
	 * @param call
	 *            call
	 * @param successFunc
	 *            successFunc
	 * @param pool
	 *            pool
	 * @param method
	 *            method
	 * @return boolean
	 * @param <T>
	 *            T
	 * @param <R>
	 *            R
	 */
	public static <T, R> boolean majorityCall(List<T> nodes, Function<T, R> call, Function<R, Boolean> successFunc,
			ExecutorService pool, String method) {
		if (nodes.size() == 0) {
			return true;
		}
		final int id = RandomUtil.randomInt();
		AtomicInteger cnt = new AtomicInteger(0);
		CompletableFuture<?>[] futures = nodes.stream().map(follower -> CompletableFuture.supplyAsync(() -> {
			R ret = call.apply(follower);
			log.trace("id: {}, call {} {}, ret: {}", id, method, follower, ret);
			return ret;
		}, pool).thenAccept(reply -> {
			if (successFunc.apply(reply)) {
				cnt.incrementAndGet();
			}
		})).toArray(CompletableFuture[]::new);
		try {
			CompletableFuture.allOf(futures).get(500, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error("majority call interrupted: {}", e.toString());
		}
		return cnt.get() >= nodes.size() / 2;
	}

	/**
	 * 是否为linux服务器
	 * 
	 * @return 是否
	 */
	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}

	/**
	 * 是否为空字符串
	 * 
	 * @param str
	 *            str
	 * @return boolean
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * 是否为非空字符串
	 * 
	 * @param str
	 *            str
	 * @return boolean
	 */
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}
}
