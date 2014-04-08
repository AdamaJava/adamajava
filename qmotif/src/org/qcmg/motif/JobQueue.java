/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Pair;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
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
	private int numberFeatures = 0;
	private final File gff3File;
	private final HashSet<String> refNames = new HashSet<String>();
	private HashSet<String> bamRefNames = new HashSet<String>();
	private HashMap<String, Long> gff3RefNames = new HashMap<String, Long>();
//	private final HashMap<String, HashSet<GFF3Record>> perRefnameFeatures = new HashMap<String, HashSet<GFF3Record>>();
	private final HashMap<String, Integer> perRefnameLengths = new HashMap<String, Integer>();
	private final HashMap<Integer, HashSet<String>> perLengthRefnames = new HashMap<Integer, HashSet<String>>();
	private final HashSet<Pair<File, File>> filePairs;
	private final HashMap<String, HashSet<Pair<File, File>>> refnameFilePairs = new HashMap<String, HashSet<Pair<File, File>>>();
	private final Vector<String> refnameExecutionOrder = new Vector<String>();
	private final Map<ChrPosition, RegionCounter> results = new HashMap<>();
	private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();
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
	private int cutoff = 10;
	private final AbstractQueue<SAMRecord> outputQueue = new ConcurrentLinkedQueue<>();
	private final SAMOrBAMWriterFactory bamWriterFactory;
	private final MotifsAndRegexes motifsAndRegexes;
	private final SummaryStats ss = new SummaryStats();
	
	private final List<String> gffErrors = new ArrayList<String>();

	public JobQueue(final Configuration invariants)
			throws Exception {
//		perFeatureFlag = invariants.isPerFeatureFlag();
		gff3File = invariants.getInputGFF3File();
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
//		motifRefPositions = invariants.getExcludes();
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
		bamWriterFactory = new SAMOrBAMWriterFactory(header, false, new File(invariants.getOutputBam()));
		
		if (null != invariants.getCutoff()) cutoff = invariants.getCutoff(); 
//		if (null != invariants.getRegex()) regex =Pattern.compile(invariants.getRegex()); 
		
		execute();
	}

	private void execute() throws Exception, IOException {
//		logger.info("Loading features from GFF file");
//		loadFeatures();
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
		identifyRefNameExecutionOrder();
		for (String refname : refnameExecutionOrder) {
			int refLength = perRefnameLengths.get(refname);
			HashSet<Pair<File, File>> filePairs = refnameFilePairs.get(refname);
			
			Job job = new CoverageJob(refname,
					refLength, filePairs, filter,
					algorithm, countIn, countOut, validation, windowSize, outputQueue, includes, excludes);
			jobQueue.add(job);
		}
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
				
				// overwrite motifs for now - may want ot put big list of bumf back in there soon...
				
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
			logger.info("SUMMARY: (window size: " + windowSize + ", cutoff: " + cutoff + ")");
			for (String s : resultsList) logger.info(s);
			
			logger.info("motif details:");
			logger.info("No of unique motifs: " + allMotifs.size());
			
		} else {
			logger.info("no evidence of motifs were found!!!");
		}
		
		long factor = 1000000000;
		long totalReadCount = countIn.get();
		
		ss.setWindowSize(windowSize);
		ss.setCutoff(cutoff);
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

//	private void loadFeatures() throws Exception, IOException {
//		identifyRefNames();
//		GFF3FileReader featureReader = new GFF3FileReader(gff3File);
//		for (final GFF3Record feature : featureReader) {
//			String ref = feature.getSeqId();
//			if (refNames.contains(ref)) {
//				HashSet<GFF3Record> features = perRefnameFeatures.get(ref);
//				if (null == features) {
//					features = new HashSet<GFF3Record>();
//					perRefnameFeatures.put(ref, features);
//				}
//				features.add(feature);
//			}
//		}
//		featureReader.close();
//	}

	private void identifyRefNames() throws Exception {
		identifyBamRefNames();
		identifyGff3RefNames();
		refNames.addAll(bamRefNames);
		refNames.retainAll(gff3RefNames.keySet());
		
		// add in the unmaped
		refNames.add(UNMAPPED);
		
		// free up some space
		bamRefNames = null;
		gff3RefNames = null;
		
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
			// add in a bunch of crap for unmapped
			perRefnameLengths.put(UNMAPPED,1000 * 1000 * 128);
			bamRefNames.add(UNMAPPED);
			refnameFilePairs.put(UNMAPPED, filePairs);

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
			results.putAll(thread.getReducedResults());
		}
		logger.debug("Results from threads gathered, no of entries in results map: " + results.size());
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
	
	public SummaryStats getResults() {
		return ss;
	}

//	public List<CoverageReport> getCoverageReport() {
//		List<CoverageReport> results = new Vector<CoverageReport>();
//		for (final String type : perIdPerCoverageBaseCounts.keySet()) {
//			HashMap<Integer, AtomicLong> value = perIdPerCoverageBaseCounts
//					.get(type);
//			CoverageReport report = new CoverageReport();
//			report.setFeature(type);
//			report.setType(coverageType);
//			for (Integer coverage : value.keySet()) {
//				AtomicLong bases = value.get(coverage);
//				CoverageModel element = new CoverageModel();
//				element.setBases(new BigInteger(""+bases.get()));
//				element.setAt(coverage.toString());
//				report.getCoverage().add(element);
//			}
//			results.add(report);
//		}
//		return results;
//	}

}
