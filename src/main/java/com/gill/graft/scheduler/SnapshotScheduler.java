package com.gill.graft.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gill.graft.apis.DataStorage;
import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;

/**
 * SnapshotScheduler
 *
 * @author gill
 * @version 2023/09/27
 **/
public class SnapshotScheduler {

	private static ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1,
			r -> new Thread(r, "snapshot-scheduler"));

	public synchronized static void start(DataStorage dataStorage, RaftConfig config) {
		scheduler.scheduleAtFixedRate(dataStorage::saveSnapshotToFile, 0, config.getSnapshotPersistedInterval(),
				TimeUnit.MILLISECONDS);
	}

	public synchronized static void stop() {
		ExecutorService tmp = scheduler;
		scheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "snapshot-scheduler"));
		tmp.shutdown();
		Utils.awaitTermination(tmp, "snapshot-scheduler");
	}

}
