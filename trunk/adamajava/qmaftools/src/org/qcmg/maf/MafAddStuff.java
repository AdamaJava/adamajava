/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.sf.picard.PicardException;
import net.sf.picard.liftover.LiftOver;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.picard.util.Interval;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MafAddStuff {
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	private int noOfBases = 5;
	private String gffFile;
	private String fastaFile;
	private String chainFile;
	
	private final SortedSet<ChrPosition> mafPositionsOfInterest = new TreeSet<ChrPosition>();
	private final Map<ChrPosition, ChrPosition> mafPositionsOfInterestLiftover = new HashMap<ChrPosition, ChrPosition>();
	private final Map<ChrPosition, String> fastaCPGDataMap = new HashMap<ChrPosition, String>();
	private final Map<ChrPosition, String> chrPosGffType = new HashMap<ChrPosition, String>();
	
	public int engage() throws Exception {
		// load mapping files
		for (String inputFile : cmdLineInputFiles) {
			logger.info("loading positions of interest from maf file: " + inputFile);
			MafUtils.loadPositionsOfInterest(inputFile, mafPositionsOfInterest);
		}
		logger.info("Loaded " + mafPositionsOfInterest.size() + " positions into set");
		
		populateLiftOverMap();
		
		// populate the positionsOfInterest map with reference data from the fasta file
		logger.info("populating fastaCPGDataMap data from file: " + fastaFile);
		getFastaData(fastaFile);
		logger.info("populating fastaCPGDataMap data from file: " + fastaFile + " - DONE");
		
		// load gff3 bait info
		logger.info("populating gff3 types from file: " + gffFile);
		getGffTypes(gffFile);
		logger.info("populating gff3 types from file: " + gffFile + " - DONE");
		// output new maf file with additional columns
		
		int i = 0;
		for (String inputFile : cmdLineInputFiles) {
			logger.info("write output: " + cmdLineOutputFiles[i]);
			writeMafOutput(inputFile, cmdLineOutputFiles[i++]);
		}
		
		return exitStatus;
	}
	
	private void populateLiftOverMap() {
		if (null != chainFile) {
			LiftOver picardLiftover = new LiftOver(new File(chainFile));
			for (ChrPosition cp : mafPositionsOfInterest) {
				Interval oldInt = new Interval(MafUtils.getFullChrFromMafChr(cp.getChromosome()), cp.getPosition(), cp.getEndPosition());
				Interval newInt =  picardLiftover.liftOver(oldInt);
				logger.info("oldInt: " + oldInt + ", new Int: " + newInt);
				
				mafPositionsOfInterestLiftover.put(cp, new ChrPosition(newInt.getSequence().substring(3), newInt.getStart(), newInt.getEnd()));
			}
		} else {
			for (ChrPosition cp : mafPositionsOfInterest) {
				mafPositionsOfInterestLiftover.put(cp, cp);
			}
		}
	}
	
	private void getGffTypes(String gff3File) throws Exception {
		Map<String, Map<ChrPosition, String>> gffTypes = new HashMap<String, Map<ChrPosition, String>>();
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
				thisMap.put(new ChrPosition(chr, rec.getStart(), rec.getEnd()), rec.getType());
				
				if (++count % 1000000 == 0) logger.info("hit " + count + " records");
			}
		} finally {
			reader.close();
		}
		logger.info("loaded all gff3 data, now populating map with relevent positions");
		
		// now populate chrpos map with gff type
		for (ChrPosition cp : mafPositionsOfInterestLiftover.values()) {
			// get chromosome
			String chr = MafUtils.getFullChrFromMafChr(cp.getChromosome());
			
			// get map from gffTypes
			Map<ChrPosition, String> positionsAtChr = gffTypes.get(chr);
			if (null != positionsAtChr) {
				for (Entry<ChrPosition, String> entry : positionsAtChr.entrySet()) {
					if (ChrPositionUtils.doChrPositionsOverlap(entry.getKey(), new ChrPosition(chr, cp.getPosition(), cp.getEndPosition()))) {
						String type = chrPosGffType.get(cp);
						if (null == type) {
							chrPosGffType.put(cp, entry.getValue());
						} else {
							chrPosGffType.put(cp, type + "," + entry.getValue());
						}
						
						// single position - won't have multiple gff3 regions
						if (cp.getPosition() == cp.getEndPosition()) break;
					}
				}
			}
		}
		logger.info("no of entries in chrPosGffType: " + chrPosGffType.size());
	}
	
	private void getFastaData(String refFile) throws FileNotFoundException {
		IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(new File(refFile));
		
		for (ChrPosition cp : mafPositionsOfInterestLiftover.values()) {
			String chr = MafUtils.getFullChrFromMafChr(cp.getChromosome());
			
			logger.info("retrieveing info for ChrPos: " + chr + ", " + (cp.getPosition() - noOfBases) + "-" + (cp.getEndPosition() + noOfBases));
			ReferenceSequence seq = null;
			try {
				seq = fasta.getSubsequenceAt(chr, cp.getPosition() - noOfBases, cp.getEndPosition() + noOfBases);
			} catch (PicardException pe) {
				logger.error("Exception caught in getFastaData",pe);
			}
			if (null != seq)
				fastaCPGDataMap.put(cp, new String(seq.getBases()));
		}
		logger.info("no of entries in CPG map: " + fastaCPGDataMap.size());
	}
	
	private void writeMafOutput(String inputMafFile, String outputMafFile) throws Exception {
		if (fastaCPGDataMap.isEmpty() && chrPosGffType.isEmpty()) return;
		
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
					writer.write(headerLine +  "\tCPG\tGff3_Bait\n");
				}
			}
			
			for (TabbedRecord rec : reader) {
				// first line is part of header
				if (count++ == 0 && (rec.getData().startsWith("Hugo_Symbol"))) {
					writer.write(rec.getData() +  "\tCPG\tGff3_Bait\n");
					continue;
				}
				
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
				// sanity check on ref base
				char ref = params[10].charAt(0);
				
				ChrPosition cp = new ChrPosition(chr, startPos, endPos);
				logger.debug("cp: " + cp.getChromosome() + ", start: " + cp.getPosition() + ", end: " + cp.getEndPosition());
				if (null != chainFile) {
					cp = mafPositionsOfInterestLiftover.get(cp);
					if (null == cp)
						logger.warn("null entry in mafPositionsOfInterestLiftover map ");
					
					//FIXME - upping the version number by 1
					if (params[3].startsWith("hg") || params[3].startsWith("GRCh")) {
						
					} else {
						int version = Integer.parseInt(params[3]);
						params[3] = "" + ++version;
					}
					params[4] = cp.getChromosome();
					params[5] = "" +cp.getPosition();
					params[6] = "" +cp.getEndPosition();
				}
				String cpgBases = fastaCPGDataMap.get(cp);
				if (null != cpgBases) {
					if ('-' != ref && ref != cpgBases.charAt(noOfBases)) {
						logger.warn("reference base: " + ref + " does not equal base retrieved for cpg purposes: " 
								+ cpgBases.charAt(noOfBases) + " at chrpos: " + cp.toString());
					}
				} else { 
					logger.warn("no reference bases for chr pos: " + cp.toString());
				}
				
				String gff3Type = chrPosGffType.get(cp);
				if (null == gff3Type) {
					logger.warn("no gff type for chr pos: " + cp.toString());
				}
				
				StringBuilder sb = new StringBuilder();
				for (String param : params) {
					if (sb.length() > 0) sb.append("\t");
					sb.append(param);
				}
				sb.append("\t");
				sb.append(cpgBases);
				sb.append("\t");
				sb.append(gff3Type);
				sb.append("\n");
				
				writer.write(sb.toString());
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
		MafAddStuff sp = new MafAddStuff();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running MafAddStuff:", e);
			else System.err.println("Exception caught whilst running MafAddStuff");
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
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
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMafException("MISSING_INPUT_OPTIONS");
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
			
			if (options.getNoOfBases() > 0)
				noOfBases = options.getNoOfBases();
			
			if ((fastaFile = options.getFastaFile()) == null || ! FileUtils.canFileBeRead(fastaFile))
				throw new QMafException("NO_FASTA_FILE_ERROR");
			if ((gffFile = options.getGffFile()) == null || ! FileUtils.canFileBeRead(gffFile))
				throw new QMafException("NO_GFF_FILE_ERROR");
			
			// check that no of input files is equal to the no of output files
			if (cmdLineInputFiles.length != cmdLineOutputFiles.length)
				throw new QMafException("INPUT_OUTPUT_FILE_NUMBERS_ARE_NOT_EQUAL");
			
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafAddStuff.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafAddStuff", MafAddStuff.class.getPackage().getImplementationVersion(), args);
			// only supply chain file if Liftover is required
			chainFile = options.getChainFile();
			if (null != chainFile && ! FileUtils.canFileBeRead(chainFile))
				throw new QMafException("NONEXISTENT_INPUT_FILE", chainFile);
			else logger.tool("will Liftover using chain file: " + chainFile);
			
			logger.tool("will collect region of interest + " + noOfBases + " bases on either side from the fasta file");
			
			return engage();
		}
		return returnStatus;
	}
}
