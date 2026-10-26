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
	private final Map<Object, HashMap<String, TreeMap<Integer, AtomicLong>>> reducedResults = new LinkedHashMap<>();
	private final Map<Object, HashMap<String, List<LowReadDepthRegion>>> reducedLowReadDepthResults = new LinkedHashMap<>();

	private final HashSet<Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>>> perJobResults = new HashSet<>();
	private final HashSet<Map<Object, HashMap<String, List<LowReadDepthRegion>>>> perJobLowReadDepthResults = new HashSet<>();

	private final QLogger logger;
	private final Thread mainThread;

	WorkerThread(final BlockingQueue<Job> jobQueue,
			final LoggerInfo loggerInfo, Thread mainThread) {
		this.inputQueue = jobQueue;
		logger = QLoggerFactory.getLogger(WorkerThread.class);
		this.mainThread = mainThread;
	}

	public Map<Object, HashMap<String, TreeMap<Integer, AtomicLong>>> getReducedResults() {
		return reducedResults;
	}

	public Map<Object, HashMap<String, List<LowReadDepthRegion>>> getReducedLowReadDepthResults() {
		return reducedLowReadDepthResults;
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
				perJobLowReadDepthResults.add(job.getLowReadDepthResults());
				logger.debug(getName() + " added job results. Results size: "
						+ totalResultsSize(job.getResults()));
				logger.debug(getName() + " added job low read depth results. Results size: "
						+ totalLowReadDepthResultsSize(job.getLowReadDepthResults()));
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
		for (Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> mappedResult : perJobResults) {
			for (Map.Entry<Object, HashMap<String, HashMap<Integer, AtomicLong>>> typeEntry : mappedResult.entrySet()) {
				HashMap<String, TreeMap<Integer, AtomicLong>> perIdMap = reducedResults
						.computeIfAbsent(typeEntry.getKey(), k -> new HashMap<>());
				for (Map.Entry<String, HashMap<Integer, AtomicLong>> idEntry : typeEntry.getValue().entrySet()) {
					TreeMap<Integer, AtomicLong> covToBaseCountMap = perIdMap
							.computeIfAbsent(idEntry.getKey(), k -> new TreeMap<>());
					for (Map.Entry<Integer, AtomicLong> covEntry : idEntry.getValue().entrySet()) {
						AtomicLong reducedBaseCount = covToBaseCountMap.computeIfAbsent(covEntry.getKey(), k -> new AtomicLong());
						reducedBaseCount.addAndGet(covEntry.getValue().get());
					}
				}
			}
		}
		//Reduce results for low read depth if run
		for (Map<Object, HashMap<String, List<LowReadDepthRegion>>> mappedLowReadDepthResult : perJobLowReadDepthResults) {
			for (Map.Entry<Object, HashMap<String, List<LowReadDepthRegion>>> typeEntry : mappedLowReadDepthResult.entrySet()) {
				HashMap<String, List<LowReadDepthRegion>> lowReadDepthResults = reducedLowReadDepthResults
						.computeIfAbsent(typeEntry.getKey(), k -> new HashMap<>());
				for (Map.Entry<String, List<LowReadDepthRegion>> refEntry : typeEntry.getValue().entrySet()) {
					List<LowReadDepthRegion> lowReadDepthRegions = lowReadDepthResults.computeIfAbsent(refEntry.getKey(), k -> new ArrayList<>());
					lowReadDepthRegions.addAll(refEntry.getValue());
				}
			}
		}
	}

	private int totalResultsSize(final Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> results) {
		int count = 0;
		for (final HashMap<String, HashMap<Integer, AtomicLong>> map : results.values()) {
			count += map.size();
		}
		return count;
	}

	private int totalLowReadDepthResultsSize(final Map<Object, HashMap<String, List<LowReadDepthRegion>>> results) {
		int count = 0;
		for (final HashMap<String, List<LowReadDepthRegion>> map : results.values()) {
			count += map.size();
		}
		return count;
	}
}
