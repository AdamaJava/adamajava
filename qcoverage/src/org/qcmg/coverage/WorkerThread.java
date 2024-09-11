/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

class WorkerThread extends Thread {
	private final BlockingQueue<Job> inputQueue;
	private final HashMap<String, TreeMap<Integer, AtomicLong>> reducedResults = new HashMap<String, TreeMap<Integer, AtomicLong>>();
	private final HashMap<String, List<LowCoverageRegion>> reducedLowCoverageResults = new HashMap<String, List<LowCoverageRegion>>();

	private final HashSet<HashMap<String, HashMap<Integer, AtomicLong>>> perJobResults = new HashSet<HashMap<String, HashMap<Integer, AtomicLong>>>();
	private final HashSet<HashMap<String, List<LowCoverageRegion>>> perJobLowCoverageResults = new HashSet<HashMap<String, List<LowCoverageRegion>>>();

	private final QLogger logger;
	private final Thread mainThread;

	WorkerThread(final BlockingQueue<Job> jobQueue,
			final LoggerInfo loggerInfo, Thread mainThread) {
		this.inputQueue = jobQueue;
		logger = QLoggerFactory.getLogger(WorkerThread.class);
		this.mainThread = mainThread;
	}

	public HashMap<String, TreeMap<Integer, AtomicLong>> getReducedResults() {
		return reducedResults;
	}

	public HashMap<String, List<LowCoverageRegion>> getReducedLowCoverageResults() {
		return reducedLowCoverageResults;
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
				perJobResults.add(job.getResults());
				perJobLowCoverageResults.add(job.getLowCoverageResults());
				logger.debug(getName() + " added job results. Results size: "
						+ job.getResults().size());
				logger.debug(getName() + " added job low coverage results. Results size: "
						+ job.getLowCoverageResults().size());
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage());
			} catch (Exception e) {
				mainThread.interrupt();
			}
		}
		logger.info(getName() + " reducing results from handled jobs");
		reduceResults();
		logger.debug(getName() + " reducing results: " + reducedResults);
	}

	private void reduceResults() {
		for (HashMap<String, HashMap<Integer, AtomicLong>> mappedResult : perJobResults) {
			for (String id : mappedResult.keySet()) {
                TreeMap<Integer, AtomicLong> covToBaseCountMap = reducedResults.computeIfAbsent(id, k -> new TreeMap<Integer, AtomicLong>());
                for (Integer cov : mappedResult.get(id).keySet()) {
                    AtomicLong reducedBaseCount = covToBaseCountMap.computeIfAbsent(cov, k -> new AtomicLong());
                    AtomicLong mappedBaseCount = mappedResult.get(id).get(cov);
					assert (null != mappedBaseCount); // Implicit to above logic
					reducedBaseCount.addAndGet(mappedBaseCount.get());
				}
			}
		}
		//Reduce results for low coverage if run
		for (HashMap<String, List<LowCoverageRegion>> mappedLowCovResult : perJobLowCoverageResults) {
			for (String key : mappedLowCovResult.keySet()) {

                List<LowCoverageRegion> lowCoverageRegions = reducedLowCoverageResults.computeIfAbsent(key, k -> new ArrayList<LowCoverageRegion>());
                lowCoverageRegions.addAll(mappedLowCovResult.get(key));
			}
		}
	}
}
