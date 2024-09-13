/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qio.gff3.Gff3FileReader;
import org.qcmg.qio.gff3.Gff3Record;

public final class JobQueue {
	private final HashMap<String, HashMap<Integer, AtomicLong>> perIdPerCoverageBaseCounts = new HashMap<String, HashMap<Integer, AtomicLong>>();
	private final HashMap<String, List<LowReadDepthRegion>> lowReadDepthResultsFinalMap = new HashMap<String, List<LowReadDepthRegion>>();
	private final boolean perFeatureFlag;
	private final int numberThreads;
	private int numberFeatures = 0;
	private final File gff3File;
	private final HashSet<String> refNames = new HashSet<String>();
	private final LinkedHashSet<String> refNamesOrdered = new LinkedHashSet<String>();
	private final HashMap<String, HashSet<Gff3Record>> perRefnameFeatures = new HashMap<String, HashSet<Gff3Record>>();
	private final HashMap<String, Integer> perRefnameLengths = new HashMap<String, Integer>();
	private final HashMap<Integer, HashSet<String>> perLengthRefnames = new HashMap<Integer, HashSet<String>>();
	private final HashSet<Pair<File, File>> filePairs;
	private final HashMap<String, HashSet<Pair<File, File>>> refnameFilePairs = new HashMap<String, HashSet<Pair<File, File>>>();
	private final Vector<String> refnameExecutionOrder = new Vector<String>();
	private final HashSet<HashMap<String, TreeMap<Integer, AtomicLong>>> perRefnameResults = new HashSet<HashMap<String, TreeMap<Integer, AtomicLong>>>();
	private final HashSet<HashMap<String, List<LowReadDepthRegion>>> lowReadDepthResultsSet = new HashSet<HashMap<String, List<LowReadDepthRegion>>>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();
	private final LoggerInfo loggerInfo;
	private final QLogger logger;
	private final QueryExecutor filter;
	private final org.qcmg.coverage.CoverageType coverageType;
	private final Algorithm algorithm;
	private final String validation;
	private final ReadsNumberCounter countIn;
	private final ReadsNumberCounter countOut;
	private final Options options;
	
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
		options = invariants.getOptions();
		
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
		logger.debug("Final reduced low read depth results size: " + lowReadDepthResultsFinalMap.size());
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

		if (coverageType.equals(CoverageType.LOW_READDEPTH)) {
			//Reduce results for low read depth if run
			for (HashMap<String, List<LowReadDepthRegion>> mappedLowRDepthResult : lowReadDepthResultsSet) {
				for (String key : mappedLowRDepthResult.keySet()) {
                    List<LowReadDepthRegion> lowReadDepthRegions = lowReadDepthResultsFinalMap.computeIfAbsent(key, k -> new ArrayList<>());
                    lowReadDepthRegions.addAll(mappedLowRDepthResult.get(key));
				}
			}
		}

	}

	private void loadFeatures() throws Exception, IOException {
		identifyRefNames();
		Gff3FileReader featureReader = new Gff3FileReader(gff3File);
		for (final Gff3Record feature : featureReader) {
			String ref = feature.getSeqId();
			if (refNames.contains(ref)) {
				HashSet<Gff3Record> features = perRefnameFeatures.get(ref);
				if (null == features) {
					features = new HashSet<Gff3Record>();
					perRefnameFeatures.put(ref, features);
				}
				features.add(feature);
			}
		}
		featureReader.close();
	}

	private void identifyRefNames() throws Exception {
		LinkedHashSet<String> bamRefNames = identifyBamRefNames();
		Collection<String> gff3RefNames = identifyGff3RefNames();
		
		refNames.addAll(bamRefNames);
		refNames.retainAll(gff3RefNames);

		//create ordered
		refNamesOrdered.addAll(bamRefNames);
		refNamesOrdered.retainAll(gff3RefNames);
		
		logger.debug("Common reference names: " + refNames);
		for (String s : refNames) {
			logger.debug(s);
		}
	}

	private Collection<String> identifyGff3RefNames() throws Exception, IOException {

		LinkedHashMap<String, Integer> gff3RefNames = new LinkedHashMap<>();
		final StringBuilder gffErrors = new StringBuilder();
		try (Gff3FileReader gff3Reader = new Gff3FileReader(gff3File);) {
			for (Gff3Record record : gff3Reader) {
				if (isGff3RecordValid(record)) {
					numberFeatures++;
					String refName = record.getSeqId();
					int position = record.getEnd();
					Integer existingPosition = gff3RefNames.get(refName);
					if (null == existingPosition || existingPosition.longValue() < position) {
						gff3RefNames.put(refName, position);
					}
				} else {
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
	public static boolean isGff3RecordValid(Gff3Record record) {
		return null != record && record.getStart() <= record.getEnd();
	}

	private LinkedHashSet<String> identifyBamRefNames() throws IOException {
		LinkedHashSet<String> bamRefNames = new LinkedHashSet<>();
		for (final Pair<File, File> pair : filePairs) {
			File bamFile = pair.left();
			File baiFile = pair.right();
			try (SamReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile, baiFile)) {
				SAMFileHeader header = samReader.getFileHeader();
				for (SAMSequenceRecord seqRecord : header.getSequenceDictionary()
						.getSequences()) {
					String seqName = seqRecord.getSequenceName();
					bamRefNames.add(seqName);
					Integer seqLength = seqRecord.getSequenceLength();
					perRefnameLengths.put(seqName, seqLength);
                    HashSet<Pair<File, File>> filePairs = refnameFilePairs.computeIfAbsent(seqName, k -> new HashSet<Pair<File, File>>());
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
			if (coverageType.equals(CoverageType.LOW_READDEPTH)) {
				lowReadDepthResultsSet.add(thread.getReducedLowReadDepthResults());
			}
		}
		logger.debug("Results from threads gathered: " + perRefnameResults);
		if (coverageType.equals(CoverageType.LOW_READDEPTH)) {
			logger.debug("Low Read Depth Results size from threads gathered: " + lowReadDepthResultsSet.size());
		}
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

	public HashMap<String, List<LowReadDepthRegion>> getLowReadDepthResultsFinalMap() {
		return lowReadDepthResultsFinalMap;
	}

	public LinkedHashSet<String> getRefNamesOrdered() {
		return refNamesOrdered;
	}

}
