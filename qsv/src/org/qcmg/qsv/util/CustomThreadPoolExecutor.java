/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;


public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
	private AtomicInteger exitStatus;
	private QLogger logger;

	public CustomThreadPoolExecutor(int threadNo, AtomicInteger exitStatus,
			QLogger logger) {
		super(threadNo, threadNo, threadNo, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(threadNo));
		this.logger = logger;
		this.exitStatus = exitStatus;
	}

	@Override
	public void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (t != null) {
			
			logger.info("Thread died due to " + QSVUtil.getStrackTrace(t));
			if (exitStatus.get() == 0) {
				exitStatus.incrementAndGet();
			}
		}

	}

	@Override
	public void terminated() {
		super.terminated();
		// ... Perform final clean-up actions
	}
}
