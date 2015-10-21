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
import java.util.regex.Pattern;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MafAddCPG {
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	private int noOfBases = 5;
	
	Map<ChrPosition, String> positionsOfInterestMap = new HashMap<ChrPosition, String>();
//	List<ChrPosition> positionsOfInterestList = new ArrayList<ChrPosition>();
	SortedSet<ChrPosition> positionsOfInterestSet = new TreeSet<ChrPosition>();
	
	public int engage() throws Exception {
		// load mapping files
		logger.info("loading positions of interest from maf file: " + cmdLineInputFiles[0]);
		loadPositionsOfInterest(cmdLineInputFiles[0]);
		logger.info("loading positions of interest from maf file: " + cmdLineInputFiles[0] + " - DONE");
		
		// populate the positionsOfInterest map with reference data from the fasta file
		logger.info("populating positions of interest from fasta file: " + cmdLineInputFiles[1]);
		populatePositionsOfInterest(cmdLineInputFiles[1]);
		logger.info("populating positions of interest from fasta file: " + cmdLineInputFiles[1] + " - DONE");
		
		// output new maf file with additional column
		
		logger.info("write output: " + cmdLineOutputFiles[0]);
		writeMafOutput(cmdLineInputFiles[0], cmdLineOutputFiles[0]);
		
		return exitStatus;
	}
	
	private void populatePositionsOfInterest(String refFile) throws FileNotFoundException {
		IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(new File(refFile));
		
		for (ChrPosition cp : positionsOfInterestSet) {
			String chr = "chr" + cp.getChromosome();
			if ("chrM".equals(chr)) chr = "chrMT";
			
			ReferenceSequence seq = fasta.getSubsequenceAt(chr, cp.getPosition(), cp.getEndPosition());
			positionsOfInterestMap.put(cp, new String(seq.getBases()));
		}
		logger.info("no of entries in map: " + positionsOfInterestMap.size());
			
	}
	
	private void loadPositionsOfInterest(String mafFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(mafFile));
		try {
			
			int count = 0;
			
			for (TabbedRecord rec : reader) {
				if (count++ == 0) continue;	// first line is header
				
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
				ChrPosition cp = new ChrPosition(chr, startPos - noOfBases, endPos + noOfBases);
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
					// add CPG column header to end of line
//					if (headerLine.indexOf("\n") != -1)
//						writer.write(headerLine.replace("\n", "\tCPG\n"));
//					else 
						writer.write(headerLine +  "\tCPG\n");
				}
			}
//			writer.write(MafUtils.HEADER_WITH_CPG);
			
			for (TabbedRecord rec : reader) {
				// first line is part of header
				if (count++ == 0 && (rec.getData().startsWith("Hugo_Symbol"))) {
					writer.write(rec.getData() +  "\tCPG\n");
					continue;
				}
				
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
				// sanity check on ref base
				char ref = params[10].charAt(0);
				
				ChrPosition cp = new ChrPosition(chr, startPos - noOfBases, endPos + noOfBases);
				String bases = positionsOfInterestMap.get(cp);
				if (null != bases) {
					if ('-' != ref && ref != bases.charAt(noOfBases)) {
						logger.warn("reference base: " + ref + " does not equal base retrieved for cpg purposes: " 
								+ bases.charAt(noOfBases) + " at chrpos: " + cp.toString());
					}
					writer.write(rec.getData() + "\t" + bases + "\n");
				} else { 
					logger.warn("no reference bases for chr pos: " + cp.toString());
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
		MafAddCPG sp = new MafAddCPG();
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
			logger = QLoggerFactory.getLogger(MafAddCPG.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafAddCPG", MafAddCPG.class.getPackage().getImplementationVersion(), args);
			
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
			
			if (options.getNoOfBases() > 0)
				noOfBases = options.getNoOfBases();
			
			logger.info("will collect region of interest + " + noOfBases + " bases on either side from the fasta file");
			
			return engage();
		}
		return returnStatus;
	}
}
