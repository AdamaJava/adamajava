/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.BAMFileReader;
import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.QueryInterval;
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
	/**
	 * The coverage results, keyed by coverage type.
	 */
	private final Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> perIdPerCoverageBaseCounts = new HashMap<>();
	/**
	 * The low read depth results, keyed by coverage type.
	 */
	private final Map<Object, HashMap<String, List<LowReadDepthRegion>>> lowReadDepthResultsFinalMap = new HashMap<>();
	private final int numberThreads;
	private int numberFeatures = 0;
	private final File gff3File;
	private final HashSet<String> refNames = new HashSet<>();
	private final LinkedHashSet<String> refNamesOrdered = new LinkedHashSet<>();
	private final HashMap<String, HashSet<Gff3Record>> perRefnameFeatures = new HashMap<>();
	private final HashMap<String, Integer> perRefnameLengths = new HashMap<>();
	private final HashMap<Integer, HashSet<String>> perLengthRefnames = new HashMap<>();
	private final HashSet<Pair<File, File>> filePairs;
	private final HashMap<String, HashSet<Pair<File, File>>> refnameFilePairs = new HashMap<>();
	/**
	 * The BAM indexes, keyed by BAM file. Parsing a BAM index can be expensive for large
	 * BAM files, so the index is parsed once and shared by all the jobs that require it.
	 */
	private final HashMap<File, BamIndex> bamIndexes = new HashMap<>();
	private final Vector<String> refnameExecutionOrder = new Vector<>();
	private final HashSet<Map<Object, HashMap<String, TreeMap<Integer, AtomicLong>>>> perRefnameResults = new HashSet<>();
	private final HashSet<Map<Object, HashMap<String, List<LowReadDepthRegion>>>> lowReadDepthResultsSet = new HashSet<>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
	private final LoggerInfo loggerInfo;
	private final QLogger logger;
	private final QueryExecutor filter;
	/**
	 * The algorithm to be used for each of the coverage types.
	 */
	private final Map<Object, Algorithm> algorithms;
	private final String validation;
	private final ReadsNumberCounter countIn;
	private final ReadsNumberCounter countOut;
	private final Options options;

	public JobQueue(final Configuration invariants) throws Exception {
		gff3File = invariants.getInputGFF3File();
		filter = invariants.getFilter();
		algorithms = invariants.getAlgorithms();
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

	private void execute() throws Exception {
		try {
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
		} finally {
			closeBamIndexes();
		}
	}

	/**
	 * Closes the readers that own the shared BAM indexes.
	 */
	private void closeBamIndexes() {
		for (final BamIndex bamIndex : bamIndexes.values()) {
			bamIndex.close();
		}
		bamIndexes.clear();
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
					refLength, perRefnameFeatures, filePairs, getBamFileInfo(refname),
					filter, options.hasPerFeatureOption(), algorithms, countIn, countOut, validation);
			jobQueue.add(job);
		}
		logger.info("Number of queued coverage jobs: " + jobQueue.size());
		logger.info("Queued jobs are: " + jobQueue);
	}

	/**
	 * Returns the information about the BAM files that contain the given reference sequence.
	 * <p>
	 * The file pointers of the reads, and the index of the reference sequence within each
	 * of the BAM headers, are calculated once, using the shared BAM index.
	 */
	private HashMap<File, BamFileInfo> getBamFileInfo(final String refname) {
		final HashMap<File, BamFileInfo> result = new HashMap<>();
		final HashSet<Pair<File, File>> filePairs = refnameFilePairs.get(refname);
		if (null == filePairs) {
			return result;
		}
		for (final Pair<File, File> pair : filePairs) {
			final BamIndex bamIndex = bamIndexes.get(pair.left());
			if (null != bamIndex && bamIndex.hasFilePointers(refname)) {
				result.put(pair.left(), new BamFileInfo(bamIndex.getFilePointers(refname),
						bamIndex.getRefIndex(refname)));
			}
		}
		return result;
	}

	private void reduceResults() {
		for (Map<Object, HashMap<String, TreeMap<Integer, AtomicLong>>> mappedResult : perRefnameResults) {
			for (Map.Entry<Object, HashMap<String, TreeMap<Integer, AtomicLong>>> typeEntry : mappedResult.entrySet()) {
				HashMap<String, HashMap<Integer, AtomicLong>> perIdMap = perIdPerCoverageBaseCounts
						.computeIfAbsent(typeEntry.getKey(), k -> new HashMap<>());
				for (Map.Entry<String, TreeMap<Integer, AtomicLong>> idEntry : typeEntry.getValue().entrySet()) {
					HashMap<Integer, AtomicLong> covToBaseCountMap = perIdMap
							.computeIfAbsent(idEntry.getKey(), k -> new HashMap<>());
					for (Map.Entry<Integer, AtomicLong> covEntry : idEntry.getValue().entrySet()) {
						AtomicLong reducedBaseCount = covToBaseCountMap.computeIfAbsent(covEntry.getKey(), k -> new AtomicLong());
						reducedBaseCount.addAndGet(covEntry.getValue().get());
					}
				}
			}
		}

		for (Map<Object, HashMap<String, List<LowReadDepthRegion>>> mappedLowRDepthResult : lowReadDepthResultsSet) {
			for (Map.Entry<Object, HashMap<String, List<LowReadDepthRegion>>> typeEntry : mappedLowRDepthResult.entrySet()) {
				HashMap<String, List<LowReadDepthRegion>> lowReadDepthResults = lowReadDepthResultsFinalMap
						.computeIfAbsent(typeEntry.getKey(), k -> new HashMap<>());
				for (Map.Entry<String, List<LowReadDepthRegion>> refEntry : typeEntry.getValue().entrySet()) {
					List<LowReadDepthRegion> lowReadDepthRegions = lowReadDepthResults.computeIfAbsent(refEntry.getKey(), k -> new ArrayList<>());
					lowReadDepthRegions.addAll(refEntry.getValue());
				}
			}
		}

	}

	/**
	 * Loads the features from the GFF3 file and determines the reference sequences that are
	 * common to the GFF3 file and the BAM file(s).
	 * <p>
	 * The GFF3 file is read only once, with the features being collected as they are read.
	 */
	private void loadFeatures() throws Exception {
		LinkedHashSet<String> bamRefNames = identifyBamRefNames();
		Collection<String> gff3RefNames = readGff3Features(bamRefNames);

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

	/**
	 * Reads the GFF3 file, validating each record and collecting the features for the
	 * reference sequences that are present in the BAM file(s).
	 *
	 * @param bamRefNames the reference names found in the BAM file(s)
	 * @return the reference names found in the GFF3 file
	 */
	private Collection<String> readGff3Features(final Collection<String> bamRefNames) throws Exception {

		LinkedHashMap<String, Integer> gff3RefNames = new LinkedHashMap<>();
		final StringBuilder gffErrors = new StringBuilder();
		try (Gff3FileReader gff3Reader = new Gff3FileReader(gff3File)) {
			for (Gff3Record record : gff3Reader) {
				if (isGff3RecordValid(record)) {
					numberFeatures++;
					String refName = record.getSeqId();
					int position = record.getEnd();
					Integer existingPosition = gff3RefNames.get(refName);
					if (null == existingPosition || existingPosition.longValue() < position) {
						gff3RefNames.put(refName, position);
					}
					// only the features for reference sequences that are in the BAM file(s) are of interest
					if (bamRefNames.contains(refName)) {
						HashSet<Gff3Record> features = perRefnameFeatures.computeIfAbsent(refName, k -> new HashSet<>());
						features.add(record);
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
		
		if (!gffErrors.isEmpty()) {
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
			SamReader samReader = SAMFileReaderFactory.createSAMFileReader(bamFile, baiFile);
			SAMFileHeader header = samReader.getFileHeader();
			// the reader owns the BAM index, so it is kept open until the end of the run
			// and the index is shared by all the jobs that need it
			BamIndex bamIndex = null;
			final BAMFileReader bamFileReader = SAMFileReaderFactory.getBAMFileReader(samReader);
			if (null != bamFileReader) {
				bamIndex = new BamIndex(bamFileReader);
				bamIndexes.put(bamFile, bamIndex);
			} else {
				samReader.close();
			}
			for (SAMSequenceRecord seqRecord : header.getSequenceDictionary()
					.getSequences()) {
				String seqName = seqRecord.getSequenceName();
				bamRefNames.add(seqName);
				Integer seqLength = seqRecord.getSequenceLength();
				perRefnameLengths.put(seqName, seqLength);
                HashSet<Pair<File, File>> filePairs = refnameFilePairs.computeIfAbsent(seqName, k -> new HashSet<>());
                filePairs.add(pair);
                if (null != bamIndex) {
                	bamIndex.addFilePointers(seqName, header.getSequenceIndex(seqName));
                }
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
		return bamRefNames;
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
			perRefnameResults.add(thread.getReducedResults());
			lowReadDepthResultsSet.add(thread.getReducedLowReadDepthResults());
		}
		logger.debug("Results from threads gathered: " + perRefnameResults);
		logger.debug("Low Read Depth Results size from threads gathered: " + lowReadDepthResultsSet.size());
	}

	// Prioritise thread execution based on decreasing sequence length
	private void identifyRefNameExecutionOrder() {
		for (Integer length : perRefnameLengths.values()) {
			perLengthRefnames.put(length, new HashSet<>());
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
            refnameExecutionOrder.addAll(perLengthRefnames.get(lengths[i]));
		}
		logger.debug("Refname execution order (first-to-last): "
				+ refnameExecutionOrder);
	}

	public Map<CoverageType, List<CoverageReport>> getCoverageReport() {
		final Map<CoverageType, List<CoverageReport>> results = new LinkedHashMap<>();
		for (final Map.Entry<Object, HashMap<String, HashMap<Integer, AtomicLong>>> typeEntry : perIdPerCoverageBaseCounts.entrySet()) {
			CoverageType ct = extractType(typeEntry.getKey());
			List<CoverageReport> reports = new Vector<>();
			for (final Map.Entry<String, HashMap<Integer, AtomicLong>> featureEntry : typeEntry.getValue().entrySet()) {
				CoverageReport report = new CoverageReport();
				report.setFeature(featureEntry.getKey());
				report.setType(ct);
				for (final Map.Entry<Integer, AtomicLong> coverageEntry : featureEntry.getValue().entrySet()) {
					AtomicLong bases = coverageEntry.getValue();
					CoverageModel element = new CoverageModel();
					element.setBases(new BigInteger("" + bases.get()));
					element.setAt(coverageEntry.getKey().toString());
					report.getCoverage().add(element);
				}
				reports.add(report);
			}
			results.put(ct, reports);
		}
		return results;
	}

	public Map<Object, HashMap<String, List<LowReadDepthRegion>>> getLowReadDepthResultsFinalMap() {
		return lowReadDepthResultsFinalMap;
	}

	public Map<CoverageType, HashMap<String, List<LowReadDepthRegion>>> getLowReadDepthResultsFinalMapLegacy() {
		Map<CoverageType, HashMap<String, List<LowReadDepthRegion>>> result = new LinkedHashMap<>();
		for (Map.Entry<Object, HashMap<String, List<LowReadDepthRegion>>> e : lowReadDepthResultsFinalMap.entrySet()) {
			result.put(extractType(e.getKey()), e.getValue());
		}
		return result;
	}

	private static CoverageType extractType(Object k) {
		if (k instanceof Configuration.CoverageTypeKey) {
			return ((Configuration.CoverageTypeKey) k).type;
		}
		if (k instanceof CoverageType) {
			return (CoverageType) k;
		}
		return null;
	}

	public LinkedHashSet<String> getRefNamesOrdered() {
		return refNamesOrdered;
	}

	/**
	 * Holds the BAM index of a single BAM file, along with the file pointers of the
	 * reference sequences within that file.
	 * <p>
	 * The file pointers identify the chunks of the BAM file that contain the reads of a
	 * reference sequence. They are calculated once, using the shared BAM index, so that
	 * each job can read its reads without having to parse the index itself.
	 */
	static final class BamIndex {
		private final BAMFileReader reader;
		private final HashMap<String, long[]> filePointers = new HashMap<>();

		BamIndex(final BAMFileReader reader) {
			this.reader = reader;
		}

		/**
		 * Calculates and stores the file pointers of the given reference sequence.
		 */
		void addFilePointers(final String refName, final int refIndex) {
			if (!reader.hasIndex()) {
				return;
			}
			final BAMFileSpan span = BAMFileReader.getFileSpan(
					new QueryInterval[] { new QueryInterval(refIndex, 0, 0) }, reader.getIndex());
			// an empty span indicates that there are no reads for this reference sequence.
			// BAMFileSpan.toCoordinateArray() returns null for an empty span, so an empty
			// array is used instead, to distinguish it from an unknown set of file pointers
			final long[] coordinates = (null == span) ? null : span.toCoordinateArray();
			filePointers.put(refName, (null == coordinates) ? new long[0] : coordinates);
		}

		/**
		 * @return the file pointers of the given reference sequence, or null if they are unknown
		 */
		long[] getFilePointers(final String refName) {
			return filePointers.get(refName);
		}

		/**
		 * @return the index of the given reference sequence within the BAM header, or -1 if
		 *         the reference sequence is not present in the BAM file
		 */
		int getRefIndex(final String refName) {
			return reader.getFileHeader().getSequenceIndex(refName);
		}

		/**
		 * @return true if the file pointers of the given reference sequence are known
		 */
		boolean hasFilePointers(final String refName) {
			return filePointers.containsKey(refName);
		}

		void close() {
			reader.close();
		}
	}

}
