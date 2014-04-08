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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.gff3.GFF3RecordChromosomeAndPositionComparator;
import org.qcmg.pileup.PileupFileReader;

public class WiggleFromPileup {
	
	private final static Pattern tabbedPattern = Pattern.compile("[\\t]");
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
	private String currentChromosome = "chr1";
	
	private int lastPosition;
	
	private final List<GFF3Record> gffs = new ArrayList<GFF3Record>();
	
	private static GFF3Record gffRecord;
	private static Iterator<GFF3Record> iter;
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	private final static GFF3RecordChromosomeAndPositionComparator CHR_POS_COMP = new GFF3RecordChromosomeAndPositionComparator();
	
	
	private static QLogger logger;
	
	public int engage() throws Exception {
		
		// setup
		initialise();
		
		loadGffFile();
		
		Collections.sort(gffs, CHR_POS_COMP);
		
		if (gffs.isEmpty()) throw new IllegalArgumentException("No positions loaded from gff3 file");
		
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
				if (isGff3RecordBait(record.getType())) {
					gffs.add(record);
				} else ignoredBaits++;
			}
			
			logger.info("loaded gff3 file, total no of baits: " + totalNoOfbaits + ", entries in collection: " + gffs.size() + ", entries that didn't make it: " + ignoredBaits);
		} finally {
			reader.close();
		}
	}
	
	protected static boolean isGff3RecordBait(String type) {
		return "exon".equals(type);
	}
//	protected static boolean isGff3RecordBait(String type) {
//		return "bait_1_100".equals(type)
//		|| "bait".equals(type)
//		|| "highbait".equals(type)
//		|| "lowbait".equals(type);
//	}
	
	private void initialise() {
		noOfNormalFiles = PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'N');
		noOfTumourFiles = PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'T');
		normalStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, true);
		tumourStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, false);
		
//		logger.info("start positions: " + Arrays.deepToString(normalStartPositions) + ", " + Arrays.deepToString(tumourStartPositions));
	}
	
	private void parsePileup() throws Exception {
		Writer writer = getWriter(cmdLineOutputFiles[0]);
		
		iter = gffs.iterator();
		if (iter.hasNext()) {
			setGffRecord(iter.next()); 
		} else {
			throw new RuntimeException("Unable to set next Gff record");
		}
		
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
	
	protected static boolean isPositionInBait(String chromosome, int position, Iterator<GFF3Record> iter, GFF3Record currentRecord) {
		
		if (chromosome.equals(currentRecord.getSeqId())) {
		
			if (position < currentRecord.getStart()) {
				return false;
			} else if (position <= currentRecord.getEnd()) {
				return true;
			} else {
				return advanceGff3Record(chromosome, position, iter);
			}
		} else if (COMPARATOR.compare(chromosome, currentRecord.getSeqId()) < 0) {
			// pileup position is in lower chromosome than gffRecord
			return false;
		} else {
			// pileup position is in higher chromosome than gffRecord
			// advance iterator
			return advanceGff3Record(chromosome, position, iter);
		}
	}

	private static boolean advanceGff3Record(String chromosome, int position,
			Iterator<GFF3Record> iter) {
		if ( ! iter.hasNext()) {
			// no more entries in gffs
			return false;
		} else {
			setGffRecord(iter.next());
			return isPositionInBait(chromosome, position, iter, getGffRecord());
		}
	}
	
	private void addWiggleData(String paramString, StringBuilder sb) {
		int firstTabIndex = paramString.indexOf('\t');
		String chromosome = paramString.substring(0, firstTabIndex);
		int position = Integer.parseInt(paramString.substring(firstTabIndex+1, paramString.indexOf('\t', firstTabIndex+1)));
		
		if ( ! isPositionInBait(chromosome, position, iter, getGffRecord())) return;
//		if ( ! isPositionInBait(chromosome, position)) return;
		
		if (position != lastPosition +1 ||  ! currentChromosome.equalsIgnoreCase(chromosome)) {
			// add new header to the StringBuilder
			String wiggleHeader = "fixedStep chrom=" + chromosome + " start=" + position + " step=1\n";
			sb.append(wiggleHeader);
			
			// update last position and current chromosome
			currentChromosome = chromosome;
		}
		lastPosition = position;
		String [] params = tabbedPattern.split(paramString, -1);
		
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
		WiggleFromPileup sp = new WiggleFromPileup();
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
			logger = QLoggerFactory.getLogger(WiggleFromPileup.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("WiggleFromPileup", WiggleFromPileup.class.getPackage().getImplementationVersion(), args);
			
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
			compressOutput = FileUtils.isFileGZip(new File(cmdLineOutputFiles[0]));
			
			if (null == pileupFormat) throw new IllegalArgumentException("Please specify a pileupFormat relating to the pileup file (eg. NNTT)");
			if (normalCoverage == 0) throw new IllegalArgumentException("Please specify a normal coverage value (eg. 20)");
			if (tumourCoverage == 0) throw new IllegalArgumentException("Please specify a tumour coverage value (eg. 20)");
			
			logger.tool("about to run with pileupFormat: " + pileupFormat + ", normal cov: " + normalCoverage + ", tumour cov: " + tumourCoverage + ", compressOutput: " + compressOutput);
			
			return engage();
		}
		return returnStatus;
	}

	protected static void setGffRecord(GFF3Record gffRecord) {
		WiggleFromPileup.gffRecord = gffRecord;
	}

	protected static GFF3Record getGffRecord() {
		return gffRecord;
	}
}
