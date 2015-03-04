/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.motif.util.RegionCounter;

class WorkerThread extends Thread {
	private final BlockingQueue<Job> inputQueue;
	private final Map<ChrPosition, RegionCounter> reducedResults = new HashMap<>();
	private final QLogger logger;
	private final Thread mainThread;

	WorkerThread(final BlockingQueue<Job> jobQueue,
			final LoggerInfo loggerInfo, Thread mainThread) {
		this.inputQueue = jobQueue;
		logger = QLoggerFactory.getLogger(WorkerThread.class);
		this.mainThread = mainThread;
	}

	public Map<ChrPosition, RegionCounter> getReducedResults() {
		return reducedResults;
	}

	@Override
	public void run() {
		while (true) {
			try {
				logger.debug(getName() + " taking job from queue");
				final Job job = inputQueue.take();
				if (TerminationJob.INSTANCE == job) {
					logger.debug(getName() + " obtained job [" + job + "]");
					logger.info(getName() + " completing due to end of job queue");
					break; // Exit loop
				}
				logger.info(getName() + " obtained job [" + job + "]");
				logger.info(getName() + " running job [" + job + "]");
				job.run();
				logger.info(getName() + " completed job [" + job + "]");
				reducedResults.putAll(job.getResults());
				for (Entry<ChrPosition, RegionCounter> entry : job.getResults().entrySet()) {
					logger.debug(getName() + " added job results " + entry.getKey() + ":" + entry.getValue());
				}
//				logger.debug(getName() + " added job results "
//						+ job.getResults());
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage());
			} catch (Exception e) {
				mainThread.interrupt();
			}
		}
		logger.info(getName() + " reducing results from handled jobs");
	}

}
