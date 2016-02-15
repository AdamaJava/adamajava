/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MafAddGffBait {
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
//	private final int noOfBases = 5;
	
	Map<ChrPosition, String> positionsOfInterestMap = new HashMap<ChrPosition, String>();
//	List<ChrPosition> positionsOfInterestList = new ArrayList<ChrPosition>();
	SortedSet<ChrPosition> positionsOfInterestSet = new TreeSet<ChrPosition>();
	Map<String, Map<ChrPosition, String>> gffTypes = new HashMap<String, Map<ChrPosition, String>>();
	
	public int engage() throws Exception {
		// load mapping files
		logger.info("loading positions of interest from maf file: " + cmdLineInputFiles[0]);
		loadPositionsOfInterest(cmdLineInputFiles[0]);
		logger.info("loading positions of interest from maf file: " + cmdLineInputFiles[0] + " - DONE");
		
		// populate the positionsOfInterest map with reference data from the fasta file
		logger.info("populating positions of interest from gff file: " + cmdLineInputFiles[1]);
		populateGffTypes(cmdLineInputFiles[1]);
		logger.info("populating positions of interest from gff file: " + cmdLineInputFiles[1] + " - DONE");
		
		populatePositionsOfInterest();
		
		// output new maf file with additional column
		
		logger.info("write output: " + cmdLineOutputFiles[0]);
		writeMafOutput(cmdLineInputFiles[0], cmdLineOutputFiles[0]);
		
		return exitStatus;
	}
	
	private void populateGffTypes(String gff3File) throws Exception {
		GFF3FileReader reader = new GFF3FileReader(new File(gff3File));
		try {
			int  count = 0;
			for (GFF3Record rec : reader) {
				String chr = rec.getSeqId();
				Map<ChrPosition, String> thisMap = gffTypes.get(chr);
				if (null == thisMap) {
					thisMap = new HashMap<ChrPosition, String>();
					gffTypes.put(chr, thisMap);
				}
				thisMap.put(new ChrRangePosition(chr, rec.getStart(), rec.getEnd()), rec.getType());
				if (++count % 1000000 == 0) logger.info("hit " + count + " records");
			}
			logger.info("no of entries in gffTypes: " + gffTypes.size());
		} finally {
			reader.close();
		}
	}
	
	private void populatePositionsOfInterest() {
		for (ChrPosition cp : positionsOfInterestSet) {
				
			// get chromosome
			String chr = "chr" + cp.getChromosome();
			if ("chrM".equals(chr)) chr = "chrMT";
			
			// get map from gffTypes
			Map<ChrPosition, String> positionsAtChr = gffTypes.get(chr);
			if (null != positionsAtChr) {
				for (Entry<ChrPosition, String> entry : positionsAtChr.entrySet()) {
					if (ChrPositionUtils.doChrPositionsOverlap(entry.getKey(), new ChrRangePosition(chr, cp.getStartPosition(), cp.getEndPosition()))) {
						String type = positionsOfInterestMap.get(cp);
						if (null == type) {
							positionsOfInterestMap.put(cp, entry.getValue());
						} else {
							positionsOfInterestMap.put(cp, type + "," + entry.getValue());
						}
//						logger.info("matched!");
						// single position - won't have multiple gff3 regions
						if (cp.getStartPosition() == cp.getEndPosition()) break;
					}
				}
			}
		}
		logger.info("no of entries in map: " + positionsOfInterestMap.size());
	}
	
	private void loadPositionsOfInterest(String mafFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(mafFile));
		try {
			
			int count = 0;
			
			for (TabbedRecord rec : reader) {
				count++;
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
				ChrPosition cp = new ChrRangePosition(chr, startPos, endPos);
				positionsOfInterestSet.add(cp);
			}
			logger.info("for file: " + mafFile + " no of records: " + count + ", no of entries in chrpos set: " + positionsOfInterestSet.size());
			
		} finally {
			reader.close();
		}
	}
	
	private void writeMafOutput(String inputMafFile, String outputMafFile) throws Exception {
		if (positionsOfInterestMap.isEmpty()) return;
		
		TabbedFileReader reader = new TabbedFileReader(new File(inputMafFile));
		TabbedHeader header = reader.getHeader();
		FileWriter writer = new FileWriter(new File(outputMafFile), false);
		
		int count = 0;
		
		try {
			for (Iterator<String> iter = header.iterator() ; iter.hasNext() ;) {
				String headerLine = iter.next();
				if (headerLine.startsWith("#version")) {
					writer.write(headerLine + "\n");
				} else {
					writer.write(headerLine +  "\tGff3_Bait\n");
				}
			}
			
			for (TabbedRecord rec : reader) {
				count++;
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
//				String fullChr = "chr" + chr;
//				if ("chrM".equals(fullChr)) fullChr = "chrMT";
//				
//				ChrPosition chrCompliantCP = new ChrPosition(fullChr, startPos, endPos);
				
				
				ChrPosition cp = new ChrRangePosition(chr, startPos, endPos);
				String gff3Type = positionsOfInterestMap.get(cp);
				if (null != gff3Type) {
//					if ('-' != ref && ref != gff3Type.charAt(noOfBases)) {
//						logger.warn("reference base: " + ref + " does not equal base retrieved for cpg purposes: " 
//								+ gff3Type.charAt(noOfBases) + " at chrpos: " + cp.toString());
//					}
					writer.write(rec.getData() + "\t" + gff3Type + "\n");
				} else { 
					logger.warn("no type for chr pos: " + cp.toString());
				}
			}
			logger.info("written " + count + " maf records to file");
			
		} finally {
			try {
				writer.close();
			} finally {
				reader.close();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		MafAddGffBait sp = new MafAddGffBait();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running MafAddCPG:", e);
			else System.err.println("Exception caught whilst running MafAddCPG");
		}
		
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
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafAddGffBait.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafAddCPG", MafAddGffBait.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMafException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMafException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
//			logger.info("will collect region of interest + " + noOfBases + " bases on either side from the fasta file");
			
			return engage();
		}
		return returnStatus;
	}
}
