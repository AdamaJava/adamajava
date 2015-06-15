/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Pair;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.motif.util.SummaryStats;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class JobQueue {
	
	public static final String UNMAPPED = "unmapped";
	
	private final HashMap<String, HashMap<Integer, AtomicLong>> perIdPerCoverageBaseCounts = new HashMap<String, HashMap<Integer, AtomicLong>>();
	private final int numberThreads;
	private final HashSet<Pair<File, File>> filePairs;
	private final Map<ChrPosition, RegionCounter> results = new HashMap<>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
	private final LoggerInfo loggerInfo;
	private final QLogger logger;
	private final QueryExecutor filter;
	private final Algorithm algorithm;
	private final String validation;
	private final AtomicLong countIn;
	private final AtomicLong countOut;
	private final List<ChrPosition> includes;
	private final List<ChrPosition> excludes;
	private final Integer windowSize;
//	private int cutoff = 10;
	private final AbstractQueue<SAMRecord> outputQueue = new ConcurrentLinkedQueue<>();
	private final SAMOrBAMWriterFactory bamWriterFactory;
	private final MotifsAndRegexes motifsAndRegexes;
	private final SummaryStats ss = new SummaryStats();
	
	private final List<ChrPosition> contigs;
//	private List<SAMSequenceRecord> contigs;
	
	public JobQueue(final Configuration invariants)
			throws Exception {
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
		
		
		// get header from bam file
		Pair<File, File> firstPair = filePairs.iterator().next();
		File bamFile = firstPair.getLeft();
		// set bam filename in SummaryStats
		ss.setBamFileName(bamFile.getAbsolutePath());
		
		SAMFileReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		SAMFileHeader header = samReader.getFileHeader();
		
		// set sort order to coordinate
		header.setSortOrder(SortOrder.coordinate);
		bamWriterFactory = new SAMOrBAMWriterFactory(header, false, new File(invariants.getOutputBam()));
		
		List<SAMSequenceRecord> bamFileContigs = header.getSequenceDictionary().getSequences();
		// create a copy of this as we can't modify the original
		contigs = new ArrayList<>();
		// add in unmapped as this is not usually in the bam header
		boolean containsUnmapped = false;
		for (SAMSequenceRecord ssr : bamFileContigs) {
			if (ssr.getSequenceName().equalsIgnoreCase(UNMAPPED)) {
				containsUnmapped = true;
				break;
			}
			contigs.add(new ChrPosition(ssr.getSequenceName(), 0, ssr.getSequenceLength()));
		}
		if ( ! containsUnmapped) {
			contigs.add(new ChrPosition(UNMAPPED, 0, 1000 * 1000 * 128));
		}
		
		// and now sort so that the largest is first
		Collections.sort(contigs);
//		Collections.sort(contigs, new SAMSequenceRecodComparator());
	
//		if (null != invariants.getCutoff()) cutoff = invariants.getCutoff(); 
		
		execute();
	}

	private void execute() throws Exception, IOException {
		logger.info("Queueing jobs");
		queueJobs();
		logger.info("Commencing processing of jobs...");
		processJobs();
		logger.info("All jobs completed");
		
		logger.info("Writing output: " + outputQueue.size());
		writeOutput();
		logger.info("DONE - Writing output");
		
		logger.info("total number from input is " + countIn.get());
		logger.info("total number of reads (satisfied by the query if query provided) count to coverage is " + countOut.get());
		
		logger.info("Performing final reduce step on results");
		reduceResults();
		logger.info("Final reduce step complete");
		logger.debug("Final reduced results: " + perIdPerCoverageBaseCounts);
	}
	
	private void writeOutput() {
		if (outputQueue.isEmpty()) return;
		
		SAMFileWriter writer = null;
		try {
			writer = bamWriterFactory.getWriter();
			
			for (SAMRecord rec : outputQueue) {
				writer.addAlignment(rec);
			}
			
		} finally {
			if (null != writer) writer.close();
		}
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
		for (ChrPosition ssr : contigs) {
			String refName = ssr.getChromosome();
			int refLength = ssr.getLength();
			
			Job job = new CoverageJob(refName,
					refLength, filePairs, filter,
					algorithm, countIn, countOut, validation, windowSize, outputQueue, includes, excludes);
			jobQueue.add(job);
		}
//		for (SAMSequenceRecord ssr : contigs) {
//			String refName = ssr.getSequenceName();
//			int refLength = ssr.getSequenceLength();
//			
//			Job job = new CoverageJob(refName,
//					refLength, filePairs, filter,
//					algorithm, countIn, countOut, validation, windowSize, outputQueue, includes, excludes);
//			jobQueue.add(job);
//		}
		logger.info("Number of queued coverage jobs: " + jobQueue.size());
		logger.info("Queued jobs are: " + jobQueue);
	}
	
	private void addToMap(Map<String, AtomicInteger> map, String key, AtomicInteger value) {
		if (map.containsKey(key)) {
			AtomicInteger existingValue = map.get(key);
			existingValue.addAndGet(value.get());
		} else {
			map.put(key, value);
		}
	}

	private void reduceResults() throws Exception {
		
		// print out the motifs for users convenience
		int motifIndex = 1;
		
		if (motifsAndRegexes.getMotifMode().stageTwoString()) {
			for (String m : motifsAndRegexes.getStageTwoMotifs().getMotifs()) logger.info("motif " + motifIndex++ + ": " + m);
		}
		
		List<String> resultsList = new ArrayList<>();
		
		List<ChrPosition> orderedResultsList = new ArrayList<>(results.keySet());
		Collections.sort(orderedResultsList);
		
		Map<String, AtomicInteger> allMotifs = new HashMap<>();
		
		int rawUnmapped = 0;
		int rawIncludes = 0;
		int rawGenomic = 0;
		
		for (ChrPosition orderedCP : orderedResultsList) {
			
			RegionCounter rc = results.get(orderedCP);
			if (rc.hasMotifs()) {
				
				int noOfMotifsFS = 0;
				int noOfMotifsRS = 0;
				int noOfMotifHitsFS = 0;
				int noOfMotifHitsRS = 0;
				
				StringBuilder fs = null;
				if (null != rc.getMotifsForwardStrand()) {
//					Map<String, AtomicInteger> motifsFS = rc.getMotifsForwardStrand();
					Map<String, AtomicInteger> motifsFS = MotifUtils.convertStringArrayToMap(rc.getMotifsForwardStrand());
					noOfMotifsFS = motifsFS.size();
					fs = new StringBuilder("FS: ");
					int i = 0;
					for (Entry<String, AtomicInteger> entry : motifsFS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(allMotifs, entry.getKey(), entry.getValue());
						
						noOfMotifHitsFS += entry.getValue().get();
						if (i++ > 1) fs.append(",");
						fs.append(entry.getKey()).append("(").append(entry.getValue().get()).append(")");
					}
				}
				StringBuilder rs = null;
				if (null != rc.getMotifsReverseStrand()) {
//					Map<String, AtomicInteger> motifsRS = rc.getMotifsReverseStrand();
					Map<String, AtomicInteger> motifsRS = MotifUtils.convertStringArrayToMap(rc.getMotifsReverseStrand());
					noOfMotifsRS = motifsRS.size();
					rs = new StringBuilder("RS: ");
					int i = 0;
					for (Entry<String, AtomicInteger> entry : motifsRS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(allMotifs, entry.getKey(), entry.getValue());
						
						noOfMotifHitsRS += entry.getValue().get();
						if (i++ > 1) rs.append(",");
						rs.append(entry.getKey()).append("(").append(entry.getValue().get()).append(")");
					}
				}
				
				String motifs = (null != fs ? fs.toString() : "");
				
				if (null != rs) {
					if (motifs.length() > 0) motifs += " : ";
					motifs += rs.toString();
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
				motifs = "TotalCov: " + rc.getTotalCoverage() + ", stage 1 coverage: " + rc.getStage1Coverage() + ", stage 2 coverage: " + rc.getStage2Coverage();
				motifs += ", # FS motifs: " + noOfMotifsFS + "(" + noOfMotifHitsFS + "), # RS motifs: " + noOfMotifsRS + "(" + noOfMotifHitsRS + ")";
				
				resultsList.add(orderedCP.toIGVString() + " [" + rc.getType() + "] : " + motifs);
			}
		}
		
		if ( ! resultsList.isEmpty()) {
			logger.info("SUMMARY: (window size: " + windowSize + ")");
//			logger.info("SUMMARY: (window size: " + windowSize + ", cutoff: " + cutoff + ")");
			for (String s : resultsList) logger.info(s);
			
			logger.info("motif details:");
			logger.info("No of unique motifs: " + allMotifs.size());
			
		} else {
			logger.info("no evidence of motifs were found!!!");
		}
		
		long factor = 1000 * 1000 * 1000;
		long totalReadCount = countIn.get();
		
		ss.setWindowSize(windowSize);
//		ss.setCutoff(cutoff);
		ss.setResults(results);
		ss.setUniqueMotifCount(allMotifs.size());
		ss.setTotalReadCount(totalReadCount);
		ss.setRawGenomic(rawGenomic);
		ss.setRawIncludes(rawIncludes);
		ss.setRawUnmapped(rawUnmapped);
		
		ss.setScaledGenomic((rawGenomic * factor) / totalReadCount);
		ss.setScaledIncludes((rawIncludes * factor) / totalReadCount);
		ss.setScaledUnmapped((rawUnmapped * factor) / totalReadCount);
	}

	private void processJobs() throws Exception {
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
	}

	
	public SummaryStats getResults() {
		return ss;
	}

}
