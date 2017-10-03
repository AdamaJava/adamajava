/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.PositionRange;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.pileup.PileupFileReader;

public class WiggleFromPileupTakeTwo {
	
	private static QLogger logger;
//	private final static Pattern tabbedPattern = Pattern.compile("[\\t]");
//	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private boolean compressOutput; 
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	private String pileupFormat;
	private int normalCoverage, tumourCoverage;
	private int noOfNormalFiles, noOfTumourFiles;
	private long covGood, covBad, totalCov;
	private int[] normalStartPositions, tumourStartPositions;
	private String currentChromosome;
	private String[] gffRegions;
	private PriorityQueue<PositionRange> currentQueue;
	private PositionRange currentRange;
	private int lastPosition;
	private final Map<String,PriorityQueue<PositionRange>> regionsOfInterest = new HashMap<String, PriorityQueue<PositionRange>>();
	
	
	public int engage() throws Exception {
		// setup
		initialise();
		
		loadGffFile();
		
		logger.info("no of entries in regionsOfInterest: " + regionsOfInterest.size());
		
		long baseCount = 0;
		for (PriorityQueue<PositionRange> ranges : regionsOfInterest.values()) {
			for  (PositionRange pr : ranges) {
				baseCount += (pr.getEnd() - pr.getStart());
			}
		}
		logger.info("total no of bases covered by gff regions of interest: " + baseCount);
		
		
		if (regionsOfInterest.isEmpty()) throw new IllegalArgumentException("No positions loaded from gff3 file");
		
		// parse pileup file
		parsePileup();
		
		logger.info("bases with enough coverage: " + covGood + ", those with not enough coverage: " + covBad + ", total: " + totalCov);
		
		return exitStatus;
	}
	
	private void loadGffFile() throws Exception {
		GFF3FileReader reader =  new GFF3FileReader(new File(cmdLineInputFiles[1]));
		try {
			int totalNoOfbaits = 0, ignoredBaits = 0;
			for (GFF3Record record : reader) {
				totalNoOfbaits++;
				if (isGff3RecordCorrectType(record.getType())) {
					populateRegionsOfInterest(record);
				} else ignoredBaits++;
			}
			
			logger.info("loaded gff3 file, total no of baits: " + totalNoOfbaits + ", entries in collection: " + (totalNoOfbaits - ignoredBaits) + ", entries that didn't make it: " + ignoredBaits);
		} finally {
			reader.close();
		}
	}
	
	private void populateRegionsOfInterest(GFF3Record record) {
		// get collection corresponding to chromosome
		PriorityQueue<PositionRange> ranges = regionsOfInterest.get(record.getSeqId());
		if (null == ranges) {
			ranges = new PriorityQueue<PositionRange>();
			ranges.add(new PositionRange(record.getStart(), record.getEnd()));
			regionsOfInterest.put(record.getSeqId(), ranges);
		} else {
			// loop through PositionRanges and see if any are adjacent
			// not very efficient, but will do for now
			boolean rangeExtended = false;
			for (PositionRange pr : ranges) {
				if (pr.isAdjacentToEnd(record.getStart())) {
					pr.extendRange(record.getEnd());
					rangeExtended = true;
					break;
				}
			}
			if ( ! rangeExtended) {
				// add new PositionRange
				ranges.add(new PositionRange(record.getStart(), record.getEnd()));
			}
		}
	}

	protected boolean isGff3RecordCorrectType(String type) {
		for (String regionName : gffRegions) {
			if (type.equals(regionName)) return true;
		}
		return false;
	}
	
	private void initialise() {
		noOfNormalFiles = PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'N');
		noOfTumourFiles = PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'T');
		normalStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, true);
		tumourStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, false);
	}
	
	private void parsePileup() throws Exception {
		Writer writer = getWriter(cmdLineOutputFiles[0]);
		
		PileupFileReader reader = new PileupFileReader(new File(cmdLineInputFiles[0]));
		StringBuilder sb = new StringBuilder();
		try {
			for (String pr : reader) {
//				for (PileupRecord pr : reader) {
				addWiggleData(pr, sb);
//				addWiggleData(tabbedPattern.split(pr.getPileup(), -1), sb);
				if (++totalCov % 100000 == 0 && sb.length() > 0) {
					writer.write(sb.toString());
					sb = new StringBuilder();
					
					if (totalCov % 10000000 == 0) 
						logger.info("hit " + totalCov + " pileup records");
				}
			}
			
			// empty contents of StringBuilder to writer
			if (sb.length() > 0) writer.write(sb.toString());
			
		} finally {
			writer.close();
			reader.close();
		}
	}
	
	private Writer getWriter(String fileName) throws IOException {
		Writer writer = null;
		if (compressOutput) {
			writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName)));
		} else {
			writer = new FileWriter(new File(fileName));
		}
		return writer;
	}
	
	protected boolean isPositionInRegionOfInterest(int position, PriorityQueue<PositionRange> ranges) {
		if (null == currentRange) return false;
		
		if (position < currentRange.getStart()) {
			return false;
		} else if (position <= currentRange.getEnd()) {
			return true;
		} else {
			// advance queue
			currentRange = ranges.poll();
			return isPositionInRegionOfInterest(position, ranges);
		}
	}

	private void addWiggleData(String paramString, StringBuilder sb) {
		int firstTabIndex = paramString.indexOf('\t');
		String chromosome = paramString.substring(0, firstTabIndex);
		int position = Integer.parseInt(paramString.substring(firstTabIndex+1, paramString.indexOf('\t', firstTabIndex+1)));
		boolean chromosomeUpdated = false;
		if ( ! chromosome.equalsIgnoreCase(currentChromosome)) {
			// update last position and current chromosome
			currentChromosome = chromosome;
			chromosomeUpdated = true;
			currentQueue = regionsOfInterest.get(chromosome);
			if (null == currentQueue) {
				logger.warn("no ranges found for chr: " + chromosome);
				currentRange = null;
			} else {
				currentRange = currentQueue.poll();
			}
		}
		
		if ( ! isPositionInRegionOfInterest(position, currentQueue)) return;
		
		if (position != lastPosition +1 || chromosomeUpdated) {
			String wiggleHeader = "fixedStep chrom=" + chromosome + " start=" + position + " step=1\n";
			sb.append(wiggleHeader);
		}
		lastPosition = position;
		String [] params = TabTokenizer.tokenize(paramString);
//		String [] params = tabbedPattern.split(paramString, -1);
		
		if (PileupUtils.getCoverageCount(params, normalStartPositions) < normalCoverage) {
			sb.append("0\n");
			++covBad;
		} else {
			if (PileupUtils.getCoverageCount(params, tumourStartPositions) >= tumourCoverage) {
				sb.append("1\n");
				++covGood;
			} else {
				sb.append("0\n");
				++covBad;
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(WiggleFromPileupTakeTwo.class);
		WiggleFromPileupTakeTwo sp = new WiggleFromPileupTakeTwo();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(WiggleFromPileupTakeTwo.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("WiggleFromPileup", WiggleFromPileupTakeTwo.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			// get app specific options
			pileupFormat = options.getPileupFormat();
			normalCoverage = options.getNormalCoverage();
			tumourCoverage = options.getTumourCoverage();
			compressOutput = FileUtils.isFileNameGZip(new File(cmdLineOutputFiles[0]));
			gffRegions = options.getGffRegions();
			
			
			if (null == pileupFormat) throw new IllegalArgumentException("Please specify a pileupFormat relating to the pileup file (eg. NNTT)");
			if (normalCoverage == 0) throw new IllegalArgumentException("Please specify a normal coverage value (eg. 20)");
			if (tumourCoverage == 0) throw new IllegalArgumentException("Please specify a tumour coverage value (eg. 20)");
			if (gffRegions.length == 0) throw new IllegalArgumentException("Please specify the region names within the gff3 file you are interested in");
			
			logger.tool("about to run with pileupFormat: " + pileupFormat + ", normal cov: " + normalCoverage + ", tumour cov: " + tumourCoverage + ", compressOutput: " + compressOutput + ", gff regions: " + Arrays.deepToString(gffRegions));
			
			return engage();
		}
		return returnStatus;
	}

}
