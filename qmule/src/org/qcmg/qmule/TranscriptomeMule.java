/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.pileup.PileupFileReader;
import org.qcmg.pileup.QSnpRecord;

public class TranscriptomeMule {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private final static int MIN_COVERAGE = 3;
	// assuming all the tumours have been merged together, and we only have a single entry
//	private static int[] tumourStartPositions = null;
	private int[] tumourStartPositions = null;
	
	private final List<QSnpRecord> positions = new ArrayList<QSnpRecord>(100000);
	
	private static QLogger logger;
	
	public int engage() throws Exception {
		logger.info("loading samtools mpileup data");
		walkPileup(cmdLineInputFiles[0]);
		logger.info("loading samtools mpileup data - DONE [" + positions.size() + "]");
		
		logger.info("outputting data");
		writeOutput(cmdLineOutputFiles[0]);
		logger.info("outputting data - DONE");
		
		return exitStatus;
	}
	
	private void writeOutput(String outputFile) throws IOException {
		FileWriter writer = new FileWriter(outputFile);
		String header = "chr\tposition\tref\tpileup";
		
		try {
			writer.write(header + "\n");
			for (QSnpRecord record : positions)
				writer.write(record.getChromosome() + "\t"
						+ record.getPosition() + "\t"
						+ record.getRef() + "\t"
						+ record.getTumourNucleotides() + "\n");
		} finally {
			writer.close();
		}
	}
	
	private void parsePileup(String record) {
//		private void parsePileup(PileupRecord record) {
		String[] params = TabTokenizer.tokenize(record);
//		String[] params = tabbedPattern.split(record.getPileup(), -1);
		if (null == tumourStartPositions) {
			// set up the number of tumour start positions
			// dependent on the number of columns in the input
			// HUGE assumption that the mpileup data only contains tumour data here...
			
			//TODO is this right?
			// first 3 columns are chr	pos	ref
			int noOfSamples = (params.length -3) /3;
			tumourStartPositions = new int[noOfSamples];
			for (int i = 0 ; i < noOfSamples ; i++) {
				tumourStartPositions[i] = (i+1) * 3;
			}
		}
		
		// get coverage for both normal and tumour
		int tumourCoverage = PileupUtils.getCoverageCount(params, tumourStartPositions);
		if (tumourCoverage < MIN_COVERAGE) return;

		String tumourBases = PileupUtils.getBases(params, tumourStartPositions);
		
		// means there is an indel at this position - ignore
		if (tumourBases.contains("+") || tumourBases.contains("-")) return;
		String tumourBaseQualities = PileupUtils.getQualities(params, tumourStartPositions);
		
		// get bases as PileupElement collections
		List<PileupElement> tumourBaseCounts = PileupElementUtil.getPileupCounts(tumourBases, tumourBaseQualities);

		// get variant count for both
		int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourBaseCounts);
		
		if (tumourVariantCount >= 3) {
			// keeper
			QSnpRecord rec = new QSnpRecord();
			rec.setChromosome(params[0]);
			rec.setPosition(Integer.parseInt(params[1]));
			rec.setRef(params[2].charAt(0));
			rec.setTumourNucleotides(PileupElementUtil.getPileupElementString(tumourBaseCounts, rec.getRef()));
			positions.add(rec);
		}

	}
	
	private void walkPileup(String pileupFileName) throws Exception {
		PileupFileReader reader = new PileupFileReader(new File(pileupFileName));
		int count = 0;
		try {
			for (String record : reader) {
//				for (PileupRecord record : reader) {
				parsePileup(record);
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " pileup records, with " + positions.size() + " keepers.");
			}
		} finally {
			reader.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		TranscriptomeMule sp = new TranscriptomeMule();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = -1;
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
			logger = QLoggerFactory.getLogger(TranscriptomeMule.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("Example", TranscriptomeMule.class.getPackage().getImplementationVersion(), args);
			
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
			
			return engage();
		}
		return returnStatus;
	}
}
