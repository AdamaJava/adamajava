/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Pair;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

class CoverageJob implements Job {
	
	private final int refLength;
	private final int startPosition;
	private final int stopPosition;
	private final String refName;
	private final QLogger logger;
	private final QueryExecutor filter;
	private final HashSet<SAMFileReader> fileReaders = new HashSet<SAMFileReader>();
	private final Algorithm alg;
	private final AtomicLong counterIn;
	private final AtomicLong counterOut;
	private final List<ChrPosition> includes;
	private final List<ChrPosition> excludes;
	private Map<ChrPosition, RegionCounter> chrSpecificRegions;
	private int windowSize = 1000000;		// default to a mill
	private final AbstractQueue<SAMRecord> outputQueue;

	CoverageJob(final String refName,int startPosition, int stopPosition,  final int refLength,
			final HashSet<Pair<File, File>> filePairs, final QueryExecutor filter,
			final Algorithm algorithm, final AtomicLong counterIn,final AtomicLong counterOut, 
			Integer windowSize, Pattern regex, AbstractQueue<SAMRecord> outputQueue) throws Exception {
		this(refName, startPosition, stopPosition, refLength, filePairs, filter, algorithm, counterIn, counterOut, null, windowSize, outputQueue, null, null);
	}
	CoverageJob(final String refName, int startPosition, int stopPosition, final int refLength,
			final HashSet<Pair<File, File>> filePairs, final QueryExecutor filter,
			final Algorithm algorithm, final AtomicLong counterIn, final AtomicLong counterOut, final String validation,
			 Integer windowSize, AbstractQueue<SAMRecord> outputQueue,
			List<ChrPosition> includes, List<ChrPosition> excludes) throws Exception {
		assert (refLength > -1);
		this.refLength = refLength;
		this.startPosition = startPosition;
		this.stopPosition = stopPosition;
		this.alg = algorithm;
		this.counterIn = counterIn;
		this.counterOut = counterOut;
		this.filter = filter;
		this.logger = QLoggerFactory.getLogger(CoverageJob.class);
		this.refName = refName;
		this.includes = includes;
		this.excludes = excludes;
		if (null != windowSize) {
			this.windowSize = windowSize.intValue();
		}
		this.outputQueue = outputQueue;
		for (final Pair<File, File> pair : filePairs) {
			File bamFile = pair.getLeft();
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, validation);
			fileReaders.add(reader);
		}
		logger.debug("Length of sequence to be processed by job '" + toString() + "':" + refLength);
	}

	@Override
	synchronized public Map<ChrPosition, RegionCounter> getResults() {
		return chrSpecificRegions;
	}

	@Override
	public String toString() {
		return refName + " coverage";
	}

	@Override
	synchronized public void run() throws Exception{
		try {
			logger.info("Starting job for: " + refName);
			logger.debug("Constructing storage for coverage: " + refName);
			constructCoverageMap();
			logger.info("Performing coverage for: " + refName);
			performCoverage();
			logger.info("Ending job for: " + refName);
		} catch (Exception ex) {
			logger.error("Exception caught in run method of CoverageJob", ex);
			throw ex;
		}
	}

	void constructCoverageMap() {
		
		List<ChrPosition> chrIncludes = MotifUtils.getPositionsForChromosome(refName, includes);
		List<ChrPosition> chrExcludes = MotifUtils.getPositionsForChromosome(refName, excludes);
		
		chrSpecificRegions = MotifUtils.getRegionMap(refName, refLength, windowSize, chrIncludes, chrExcludes, "unmapped".equals(refName) );
		logger.info("created " + chrSpecificRegions.size() + " regions for " + refName + " with " + chrIncludes.size() + " includes and " + chrExcludes.size() + " excludes");
	}

	private void performCoverage() throws Exception {
 		for (final SAMFileReader fileReader : fileReaders) {
 			
			Iterator<SAMRecord> iter = "unmapped".equals(refName) ? fileReader.queryUnmapped() : fileReader.query(refName, startPosition, stopPosition, false);
			long recordCounterIn = 0;
			long recordCounterOut = 0; 
			while (iter.hasNext()) {
				SAMRecord read = iter.next();
				
				counterIn.incrementAndGet();   //count input read number
				
				if (++recordCounterIn % 10000000 == 0) {
					logger.debug("Hit " + recordCounterIn + " record for " + refName);
				}

				if (JobQueue.UNMAPPED.equals(refName) || read.getReferenceName().equals(refName)) {
					if (null == filter) {
						recordCounterOut ++;
						counterOut.incrementAndGet();    //count output read number
						if (alg.applyTo(read, chrSpecificRegions)) outputQueue.add(read);
					} else if (filter.Execute(read)) {
						recordCounterOut ++;
						counterOut.incrementAndGet(); //count output read number
						if (alg.applyTo(read, chrSpecificRegions)) outputQueue.add(read);
					}
				} else {
					logger.info("ref names did not match!");
				}
			}
			fileReader.close();
			
			StringBuilder sb = new StringBuilder();
			sb.append(refName).append(":\n");
			sb.append("read ").append(recordCounterIn).append(" records, of which ").append(recordCounterOut).append(" satisfied the query\n");
			sb.append("number in counterIn instance is: ").append(counterIn.get()).append("\n");
			sb.append("number in counterOut instance is: ").append(counterOut.get()).append("\n");
			
			logger.info(sb.toString());
		}
	}

}
