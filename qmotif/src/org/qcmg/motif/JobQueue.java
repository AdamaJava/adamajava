/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Pair;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.motif.util.SummaryStats;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMWriterFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

public final class JobQueue {
	
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	
	private final int numberThreads;
	private final HashSet<Pair<File, File>> filePairs;
	private final Map<ChrPosition, RegionCounter> results = new HashMap<>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
	private final LoggerInfo loggerInfo;
	private final QLogger logger;
	private final QueryExecutor filter;
	private final Algorithm algorithm;
	private final String validation;
	private final LongAdder countIn;
	private final LongAdder countOut;
	private final List<ChrPosition> includes;
	private final List<ChrPosition> excludes;
	private final Integer windowSize;
	private final AbstractQueue<SAMRecord> outputQueue = new ConcurrentLinkedQueue<>();
	private final SAMWriterFactory bamWriterFactory;
	private final MotifsAndRegexes motifsAndRegexes;
	private final SummaryStats ss = new SummaryStats();
	private final boolean includesOnly;
	
	private final List<ChrPosition> contigs;
	
	public JobQueue(final Configuration invariants) throws Exception {
		filter = invariants.getFilter();
		algorithm = invariants.getAlgorithm();
		numberThreads = invariants.getNumberThreads();
		filePairs = invariants.getFilePairs();
		loggerInfo = invariants.getLoggerInfo();
		validation = invariants.getValidation();
		logger = QLoggerFactory.getLogger(JobQueue.class);
		countIn = invariants.getInputReadsCount();
		countOut = invariants.getCoverageReadsCount();
		motifsAndRegexes = invariants.getRegex();
		windowSize = invariants.getWindowSize();
		includes = invariants.getIncludes();
		excludes = invariants.getExcludes();
		includesOnly = invariants.isIncludesOnlyMode();
				
		// get header from bam file
		Pair<File, File> firstPair = filePairs.iterator().next();
		File bamFile = firstPair.left();
		
		// set bam filename in SummaryStats
		ss.setBamFileName(bamFile.getAbsolutePath());
		ss.setIncludesOnly(includesOnly);

		try (SamReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile)) {

			SAMFileHeader header = samReader.getFileHeader();

			// set sort order to coordinate
			header.setSortOrder(SortOrder.coordinate);
			bamWriterFactory = new SAMWriterFactory(header, false, new File(invariants.getOutputBam()));

			contigs = new ArrayList<>();

			logger.info("running with invariants.isIncludesOnlyMode: " + includesOnly);
			if (includesOnly) {
				contigs.addAll(includes);
			} else {

				List<SAMSequenceRecord> bamFileContigs = header.getSequenceDictionary().getSequences();
				// create a copy of this as we can't modify the original
				// add in unmapped as this is not usually in the bam header
				boolean containsUnmapped = false;
				for (SAMSequenceRecord ssr : bamFileContigs) {
					if (ssr.getSequenceName().equalsIgnoreCase(MotifConstants.UNMAPPED)) {
						containsUnmapped = true;
						break;
					}
					contigs.add(new ChrRangePosition(ssr.getSequenceName(), 1, ssr.getSequenceLength()));
				}
				if (!containsUnmapped) {
					contigs.add(new ChrRangePosition(MotifConstants.UNMAPPED, 1, 1000 * 1000 * 128));
				}
			}
		}

		// and now sort so that the largest is first
		contigs.sort(COMPARATOR);
		execute();
	}

	private void execute() throws Exception {
		
		/*
		 * start the writing thread now
		 */
		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executor =  Executors.newSingleThreadExecutor();
		executor.execute(new Writing(outputQueue, Thread.currentThread(), latch));
		
		
		logger.info("Queueing jobs");
		queueJobs();
		logger.info("Commencing processing of jobs...");
		processJobs(latch);
		logger.info("All jobs completed");
		
		logger.info("total number from input is " + countIn.longValue());
		logger.info("total number of reads (satisfied by the query if query provided) count to coverage is " + countOut.longValue());
		
		logger.info("Performing final reduce step on results");
		reduceResults();
		logger.info("Final reduce step complete");
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.HOURS);
		
	}
	
	private void queueJobs() throws Exception {
		queueCoverageJobs();
		queueTerminationJobs();
		logger.debug("Total number of queued jobs (coverage jobs + termination jobs): "
						+ jobQueue.size());
	}

	private void queueTerminationJobs() {
		for (int i = 0; i < numberThreads; i++) {
			jobQueue.add(TerminationJob.INSTANCE);
		}
		logger.debug("Termination jobs queued");
	}

	private void queueCoverageJobs() throws Exception {
		for (ChrPosition cp : contigs) {
			
			Job job = new CoverageJob(cp, filePairs, filter,
					algorithm, countIn, countOut, validation, windowSize, outputQueue, includes, excludes);
			jobQueue.add(job);
		}
		logger.info("Number of queued coverage jobs: " + jobQueue.size());
		logger.info("Queued jobs are: " + jobQueue);
	}
	
	private void addToMap(Map<String, AtomicInteger> map, String key, AtomicInteger value) {
		map.computeIfAbsent(key, v -> new AtomicInteger()).addAndGet(value.get());
	}

	private void reduceResults() {
		
		// print out the motifs for users convenience
		int motifIndex = 1;
		
		if (motifsAndRegexes.getMotifMode().stageTwoString()) {
			for (String m : motifsAndRegexes.getStageTwoMotifs().getMotifs()) {
				logger.info("motif " + motifIndex++ + ": " + m);
			}
		}
		
		List<String> resultsList = new ArrayList<>();
		
		List<ChrPosition> orderedResultsList = new ArrayList<>(results.keySet());
		orderedResultsList.sort(COMPARATOR);
		
		Map<String, AtomicInteger> allMotifs = new HashMap<>();
		
		int rawUnmapped = 0;
		int rawIncludes = 0;
		int rawGenomic = 0;
		long basesCoveredByMotifs = 0;
		
		for (ChrPosition orderedCP : orderedResultsList) {
			
			RegionCounter rc = results.get(orderedCP);
			if (rc.hasMotifs()) {
				
				int noOfMotifsFS = 0;
				int noOfMotifsRS = 0;
				int noOfMotifHitsFS = 0;
				int noOfMotifHitsRS = 0;
				
				if ( ! rc.getMotifsForwardStrand().isEmpty()) {
					Map<String, AtomicInteger> motifsFS = rc.getMotifsForwardStrand();
					noOfMotifsFS = motifsFS.size();
					for (Entry<String, AtomicInteger> entry : motifsFS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(allMotifs, entry.getKey(), entry.getValue());
						noOfMotifHitsFS += entry.getValue().get();
						basesCoveredByMotifs += (long) entry.getKey().length() * entry.getValue().intValue();
					}
				}
				if ( ! rc.getMotifsReverseStrand().isEmpty()) {
					Map<String, AtomicInteger> motifsRS = rc.getMotifsReverseStrand();
					noOfMotifsRS = motifsRS.size();
					for (Entry<String, AtomicInteger> entry : motifsRS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(allMotifs, entry.getKey(), entry.getValue());
						noOfMotifHitsRS += entry.getValue().get();
						basesCoveredByMotifs += (long) entry.getKey().length() * entry.getValue().intValue();
					}
				}
				
				// overwrite motifs for now - may want to put big list of bumf back in there soon...
				
				switch (rc.getType()) {
				case INCLUDES: rawIncludes += rc.getStage2Coverage(); break;
				case GENOMIC: rawGenomic += rc.getStage2Coverage(); break;
				case UNMAPPED: rawUnmapped += rc.getStage2Coverage(); break;
				case EXCLUDES:
					break;
				default:
					break;
				}
				String motifs = "Stage 1 coverage: " + rc.getStage1Coverage() + ", stage 2 coverage: " + rc.getStage2Coverage();
				motifs += ", # FS motifs: " + noOfMotifsFS + "(" + noOfMotifHitsFS + "), # RS motifs: " + noOfMotifsRS + "(" + noOfMotifHitsRS + ")";
				
				resultsList.add(orderedCP.toIGVString() + " [" + rc.getType() + "] : " + motifs);
			}
		}
		
		if ( ! resultsList.isEmpty()) {
			logger.info("SUMMARY: (window size: " + windowSize + ")");
			for (String s : resultsList) {
				logger.info(s);
			}
			
			logger.info("motif details:");
			logger.info("No of unique motifs: " + allMotifs.size());
			
		} else {
			logger.info("no evidence of motifs were found!!!");
		}
		
		long factor = 1000 * 1000 * 1000;
		long totalReadCount = countIn.longValue();
		
		ss.setWindowSize(windowSize);
		ss.setResults(results);
		ss.setUniqueMotifCount(allMotifs.size());
		ss.setTotalReadCount(totalReadCount);
		ss.setRawGenomic(rawGenomic);
		ss.setRawIncludes(rawIncludes);
		ss.setRawUnmapped(rawUnmapped);
		
		ss.setCoveredBases(basesCoveredByMotifs);
		
		if (includesOnly) {
			/*
			 * Setting the scaled values when running in includes only mode is misleading - set to -1 instead
			 */
			ss.setScaledGenomic(-1);
			ss.setScaledIncludes(-1);
			ss.setScaledUnmapped(-1);
		} else {
			ss.setScaledGenomic((rawGenomic * factor) / totalReadCount);
			ss.setScaledIncludes((rawIncludes * factor) / totalReadCount);
			ss.setScaledUnmapped((rawUnmapped * factor) / totalReadCount);
		}
	}

	private void processJobs(CountDownLatch latch) throws Exception {
		HashSet<WorkerThread> workerThreads = new HashSet<>();
		for (int j = 0; j < numberThreads; j++) {
			WorkerThread thread = new WorkerThread(jobQueue, loggerInfo, Thread.currentThread());
			workerThreads.add(thread);
			thread.start();
		}
		for (WorkerThread thread : workerThreads) {
			thread.join();
		}
		logger.debug("All threads joined");
		for (WorkerThread thread : workerThreads) {
			results.putAll(thread.getReducedResults());
		}
		logger.debug("Results from threads gathered, no of entries in results map: " + results.size());
		latch.countDown();
	}

	
	public SummaryStats getResults() {
		return ss;
	}
	
	private class Writing implements Runnable {
		
		private final AbstractQueue<SAMRecord> queue;
        private final CountDownLatch latch;

		public Writing(AbstractQueue<SAMRecord> q,  Thread mainThread, CountDownLatch latch) {
			queue = q;
            this.latch = latch;
		}

		@Override
		public void run() {
			boolean run = true;
			long count = 0;
			SAMRecord record;
			try (SAMFileWriter writer = bamWriterFactory.getWriter()) {
				while (run) {
					if ((record = queue.poll()) == null) {
						if (latch.getCount() == 0) {
							/*
							 * the queue could have been added to since our last check (we did have a quick sleep after all
							 * Check size again before pulling the plug
							 */
							if (queue.isEmpty()) {
								run = false;
								logger.info("latch has counted down and queue is empty - exiting Writing thread, record written: " + count);
							}
						}
					} else {
						writer.addAlignment(record);
						count++;
						
						if (count % 1000000 == 0) {
							logger.info("have written " + count + " SAMRecords to file, queue size: " + queue.size());
						}
					}
				}
			}
		}
	}

}
