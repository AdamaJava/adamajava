/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
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
	
	
	public JobQueue(final Configuration invariants) throws Exception {
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
		Set<String> bamRefNames = identifyBamRefNames();
		Collection<String> gff3RefNames = identifyGff3RefNames();
		
		refNames.addAll(bamRefNames);
		refNames.retainAll(gff3RefNames);
		
		logger.debug("Common reference names: " + refNames);
		for (String s : refNames) {
			logger.debug(s);
		}
	}

	private Collection<String> identifyGff3RefNames() throws Exception, IOException {
		
		Map<String, Integer> gff3RefNames = new HashMap<>();
		final StringBuilder gffErrors = new StringBuilder();
		try (GFF3FileReader gff3Reader = new GFF3FileReader(gff3File);) {
			for (GFF3Record record : gff3Reader) {
				numberFeatures++;
				String refName = record.getSeqId();
				int position = record.getEnd();
				Integer existingPosition = gff3RefNames.get(refName);
				if (null == existingPosition || existingPosition.longValue() < position) {
					gff3RefNames.put(refName, position);
				}
				if ( ! isGff3RecordValid(record)) {
					gffErrors.append("Start position > end position at line no: ")
						.append(numberFeatures).append(", rec: ")
						.append(null == record ? "null" : record.getRawData()).append(Constants.NL);
				}
			}
		}
		logger.debug("Number of GFF3 features: " + numberFeatures);
		logger.debug("GFF3 reference names: " + gff3RefNames);
		
		// check that we don't have any features that go beyond the lengths of the sequences as defined in the bam header
		for (Entry<String, Integer> entry : gff3RefNames.entrySet()) {
			// find corresponding entry in bam header collection
			Integer bamLength = perRefnameLengths.get(entry.getKey());
			if (null != bamLength) {
				if (bamLength < entry.getValue()) {
					gffErrors.append("Gff lengths are greater than bam header lengths for: ")
						.append(entry.getKey()).append(", gff length: ").append(entry.getValue())
						.append(", bam header length: ").append(bamLength).append(Constants.NL);
				}
			} else {
//				gffErrors.add("Chromosome appears in gff but not in bam file: " + entry.getKey());
			}
		}
		
		if (gffErrors.length() > 0) {
			logger.error(gffErrors.toString());
			throw new IllegalArgumentException("Errors in gff file: " + gff3File);
		}
		return gff3RefNames.keySet();
	}
	
	/**
	 * Assumes that the record is not null.
	 * If the record is null, will return false
	 * If the record has a start > end, returns false
	 * true otherwise
	 * @param record
	 * @return
	 */
	public static boolean isGff3RecordValid(GFF3Record record) {
		return null != record && record.getStart() <= record.getEnd();
	}
	
	private Set<String> identifyBamRefNames() throws IOException {
		Set<String> bamRefNames = new HashSet<>();
		for (final Pair<File, File> pair : filePairs) {
			File bamFile = pair.getLeft();
			File baiFile = pair.getRight();
			try (SamReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile, baiFile);) {
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
			}
			if ( ! bamRefNames.isEmpty()) {
				logger.debug("BAM reference names:");
				for(String s : bamRefNames) {
					logger.debug(s);
				}
			} else {
				logger.info("No bam reference names found!");
			}
		}
		return bamRefNames;
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
