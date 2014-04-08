/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class JobQueue {
	private final HashMap<String, HashMap<Integer, AtomicLong>> perIdPerCoverageBaseCounts = new HashMap<String, HashMap<Integer, AtomicLong>>();
	private final boolean perFeatureFlag;
	private final int numberThreads;
	private int numberFeatures = 0;
	private final File gff3File;
	private final HashSet<String> refNames = new HashSet<String>();
	private final HashSet<String> bamRefNames = new HashSet<String>();
	private final HashMap<String, Long> gff3RefNames = new HashMap<String, Long>();
//	private final HashSet<String> gff3RefNames = new HashSet<String>();
	private final HashMap<String, HashSet<GFF3Record>> perRefnameFeatures = new HashMap<String, HashSet<GFF3Record>>();
	private final HashMap<String, Integer> perRefnameLengths = new HashMap<String, Integer>();
	private final HashMap<Integer, HashSet<String>> perLengthRefnames = new HashMap<Integer, HashSet<String>>();
	private final HashSet<Pair<File, File>> filePairs;
	private final HashMap<String, HashSet<Pair<File, File>>> refnameFilePairs = new HashMap<String, HashSet<Pair<File, File>>>();
	private final Vector<String> refnameExecutionOrder = new Vector<String>();
	private final HashSet<HashMap<String, TreeMap<Integer, AtomicLong>>> perRefnameResults = new HashSet<HashMap<String, TreeMap<Integer, AtomicLong>>>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();
	private final LoggerInfo loggerInfo;
	private final QLogger logger;
	private final QueryExecutor filter;
	private final org.qcmg.coverage.CoverageType coverageType;
	private final Algorithm algorithm;
	private final String validation;
	private final ReadsNumberCounter countIn;
	private final ReadsNumberCounter countOut;
	
	private final List<String> gffErrors = new ArrayList<String>();

	public JobQueue(final Configuration invariants)
			throws Exception {
		perFeatureFlag = invariants.isPerFeatureFlag();
		gff3File = invariants.getInputGFF3File();
		filter = invariants.getFilter();
		coverageType = invariants.getCoverageType();
		algorithm = invariants.getAlgorithm();
		numberThreads = invariants.getNumberThreads();
		filePairs = invariants.getFilePairs();
		loggerInfo = invariants.getLoggerInfo();
		validation = invariants.getValidation();
		logger = QLoggerFactory.getLogger(JobQueue.class);
		countIn = invariants.getInputReadsCount();
		countOut = invariants.getCoverageReadsCount();
		
		execute();
	}

	private void execute() throws Exception, IOException {
		logger.info("Loading features from GFF file");
		loadFeatures();
		logger.info("Queueing jobs");
		queueJobs();
		logger.info("Commencing processing of jobs...");
		processJobs();
		logger.info("All jobs completed");
		
		logger.info("total number from input is " + countIn.getNumber());
		logger.info("total number of reads (satisfied by the query if query provided) count to coverage is " + countOut.getNumber());
		
		logger.info("Performing final reduce step on results");
		reduceResults();
		logger.info("Final reduce step complete");
		logger.debug("Final reduced results: " + perIdPerCoverageBaseCounts);
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
		identifyRefNameExecutionOrder();
		for (String refname : refnameExecutionOrder) {
			int refLength = perRefnameLengths.get(refname);
			HashSet<Pair<File, File>> filePairs = refnameFilePairs.get(refname);
			
			CoverageJob job = new CoverageJob(refname,
					refLength, perRefnameFeatures, filePairs, filter,
					perFeatureFlag, algorithm, countIn, countOut, validation);
			jobQueue.add(job);
		}
		logger.info("Number of queued coverage jobs: " + jobQueue.size());
		logger.info("Queued jobs are: " + jobQueue);
	}

	private void reduceResults() throws Exception {
		for (HashMap<String, TreeMap<Integer, AtomicLong>> mappedResult : perRefnameResults) {
			for (String id : mappedResult.keySet()) {
				HashMap<Integer, AtomicLong> covToBaseCountMap = perIdPerCoverageBaseCounts
						.get(id);
				if (null == covToBaseCountMap) {
					covToBaseCountMap = new HashMap<Integer, AtomicLong>();
					perIdPerCoverageBaseCounts.put(id, covToBaseCountMap);
				}
				for (Integer cov : mappedResult.get(id).keySet()) {
					AtomicLong reducedBaseCount = covToBaseCountMap.get(cov);
					if (null == reducedBaseCount) {
						reducedBaseCount = new AtomicLong();
						covToBaseCountMap.put(cov, reducedBaseCount);
					}
					AtomicLong mappedBaseCount = mappedResult.get(id).get(cov);
					assert (null != mappedBaseCount); // Implicit to above logic
					reducedBaseCount.addAndGet(mappedBaseCount.get());
				}
			}
		}
	}

	private void loadFeatures() throws Exception, IOException {
		identifyRefNames();
		GFF3FileReader featureReader = new GFF3FileReader(gff3File);
		for (final GFF3Record feature : featureReader) {
			String ref = feature.getSeqId();
			if (refNames.contains(ref)) {
				HashSet<GFF3Record> features = perRefnameFeatures.get(ref);
				if (null == features) {
					features = new HashSet<GFF3Record>();
					perRefnameFeatures.put(ref, features);
				}
				features.add(feature);
			}
		}
		featureReader.close();
	}

	private void identifyRefNames() throws Exception {
		identifyBamRefNames();
		identifyGff3RefNames();
		refNames.addAll(bamRefNames);
		refNames.retainAll(gff3RefNames.keySet());
		
		// free up some space
		bamRefNames.clear();
		gff3RefNames.clear();
		
		logger.debug("Common reference names: " + refNames);
	}

	private void identifyGff3RefNames() throws Exception, IOException {
		GFF3FileReader gff3Reader = new GFF3FileReader(gff3File);
		String chr = null;
		long maxPosition = 0;
		for (GFF3Record record : gff3Reader) {
			numberFeatures++;
			String refName = record.getSeqId();
			if ( ! refName.equals(chr)) {
				if (chr != null)
					gff3RefNames.put(chr, maxPosition);
				chr = refName;	// update chr
				maxPosition = 0;	// reset max position
			}
			maxPosition = Math.max(record.getEnd(), maxPosition);
			
//			gff3RefNames.add(refName);
			checkGff3Record(record);
		}
		gff3Reader.close();
		logger.debug("Number of GFF3 features: " + numberFeatures);
		logger.debug("GFF3 reference names: " + gff3RefNames);
		
		// check that we don't have any features that go beyond the lengths of the sequences as defined in the bam header
		for (Entry<String, Long> entry : gff3RefNames.entrySet()) {
			// find corresponding entry in bam header collection
			Integer bamLength = perRefnameLengths.get(entry.getKey());
			if (null != bamLength) {
				if (bamLength < entry.getValue()) {
					gffErrors.add("Gff lengths are greater than bam header lengths for: " 
							+ entry.getKey() + ", gff length: " + entry.getValue() + ", bam header length: " + bamLength);
				}
			} else {
//				gffErrors.add("Chromosome appears in gff but not in bam file: " + entry.getKey());
			}
		}
		
		if ( ! gffErrors.isEmpty()) {
			for (String error : gffErrors) {
				logger.error(error);
			}
			throw new IllegalArgumentException("Errors in gff file: " + gff3File);
		}
	}
	
	private void checkGff3Record(GFF3Record record) {
		// check that start position is before or equal to end position
		if (record.getStart() > record.getEnd()) {
			gffErrors.add("Start position > end position at line no: " + numberFeatures + ", rec: " + record.getRawData());
		}
		
	}
	
	private void identifyBamRefNames() {
		for (final Pair<File, File> pair : filePairs) {
			File bamFile = pair.getLeft();
			File baiFile = pair.getRight();
			SAMFileReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile, baiFile);
			SAMFileHeader header = samReader.getFileHeader();
			for (SAMSequenceRecord seqRecord : header.getSequenceDictionary()
					.getSequences()) {
				String seqName = seqRecord.getSequenceName();
				bamRefNames.add(seqName);
				Integer seqLength = seqRecord.getSequenceLength();
				perRefnameLengths.put(seqName, seqLength);
				HashSet<Pair<File, File>> filePairs = refnameFilePairs.get(seqName);
				if (null == filePairs) {
					filePairs = new HashSet<Pair<File, File>>();
					refnameFilePairs.put(seqName, filePairs);
				}
				filePairs.add(pair);
			}
			logger.debug("BAM reference names: " + bamRefNames);
			samReader.close();
		}
	}

	private void processJobs() throws Exception {
		assert (null != refNames);
		HashSet<WorkerThread> workerThreads = new HashSet<WorkerThread>();
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
			perRefnameResults.add(thread.getReducedResults());
		}
		logger.debug("Results from threads gathered: " + perRefnameResults);
	}

	// Prioritise thread execution based on decreasing sequence length
	private void identifyRefNameExecutionOrder() {
		for (Integer length : perRefnameLengths.values()) {
			perLengthRefnames.put(length, new HashSet<String>());
		}
		// Identify refnames for each length
		for (String refName : refNames) {
			Integer length = perRefnameLengths.get(refName);
			assert (perLengthRefnames.containsKey(length));
			perLengthRefnames.get(length).add(refName);
		}
		// Identify smallest to largest length order
		Integer[] lengths = new Integer[perLengthRefnames.keySet().size()];
		perLengthRefnames.keySet().toArray(lengths);
		Arrays.sort(lengths);
		// Determine refName execution order from largest-to-smallest length
		for (int i = lengths.length - 1; i >= 0; i--) {
			assert (perLengthRefnames.containsKey(lengths[i]));
			for (String refName : perLengthRefnames.get(lengths[i])) {
				refnameExecutionOrder.add(refName);
			}
		}
		logger.debug("Refname execution order (first-to-last): "
				+ refnameExecutionOrder);
	}

	public List<CoverageReport> getCoverageReport() {
		List<CoverageReport> results = new Vector<CoverageReport>();
		for (final String type : perIdPerCoverageBaseCounts.keySet()) {
			HashMap<Integer, AtomicLong> value = perIdPerCoverageBaseCounts
					.get(type);
			CoverageReport report = new CoverageReport();
			report.setFeature(type);
			report.setType(coverageType);
			for (Integer coverage : value.keySet()) {
				AtomicLong bases = value.get(coverage);
				CoverageModel element = new CoverageModel();
				element.setBases(new BigInteger(""+bases.get()));
				element.setAt(coverage.toString());
				report.getCoverage().add(element);
			}
			results.add(report);
		}
		return results;
	}

}
