/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.illumina.IlluminaFileReader;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.record.Record;

public class CompareIlluminaData {
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private final Map<ChrPosition, IlluminaRecord> normalIlluminaMap = new HashMap<ChrPosition, IlluminaRecord>();
	private final Map<ChrPosition, IlluminaRecord> tumourIlluminaMap = new HashMap<ChrPosition, IlluminaRecord>();
	
	public int engage() throws Exception {
		
		loadIlluminaData(new File(cmdLineInputFiles[0]), normalIlluminaMap);
		logger.info("Normal illumina data loaded: " + normalIlluminaMap.size());
		loadIlluminaData(new File(cmdLineInputFiles[1]), tumourIlluminaMap);
		logger.info("Tumour illumina data loaded: " + normalIlluminaMap.size());
		
		compareIlluminaData();
		
		return exitStatus;
	}
	
	private void compareIlluminaData() {
		
		int sameCallInBoth = 0, diffCallInBoth = 0, count = 0, diffStrand = 0;
		// will track 1, 5, and 10 percent
		int [] callsInBothArray = new int[3]; 
		
		
		for (Entry<ChrPosition, IlluminaRecord> entry : normalIlluminaMap.entrySet()) {
			
			ChrPosition cp = entry.getKey();
			IlluminaRecord normalRec = entry.getValue();
			
			// get tumour equivalent
			IlluminaRecord tumourRec = tumourIlluminaMap.get(cp);
			
			if (null == tumourRec) {
				// only care about positions that have matching N&T illumina records
				continue;
			}
			count++;
			
//			if ( ! "TOP".equals(normalRec.getStrand()) && ! "BOT".equals(normalRec.getStrand())) {
//				logger.warn("wrong strand field! " + normalRec.getStrand());
//			}
			// check bases and strand
			if (normalRec.getStrand().equals(tumourRec.getStrand())) {
			
				double normalAAllelePercentage = (double)normalRec.getRawX() / (normalRec.getRawX() + normalRec.getRawY());
				double tumourAAllelePercentage = (double)tumourRec.getRawX() / (tumourRec.getRawX() + tumourRec.getRawY());
				
				double diff = Math.abs(normalAAllelePercentage - tumourAAllelePercentage);
				
				if (diff < 0.01) {
					//within 1% - first array entry
					callsInBothArray[0] ++; 
				} else if (diff < 0.05) {
					//within 5% - second array entry
					callsInBothArray[1] ++; 
				} else if (diff < 0.1) {
					// within 10% - third array entry
					callsInBothArray[2] ++; 
				}
			} else {
				diffStrand++;
			}
			
//			char nC1 = normalRec.getFirstAllele();
//			char nC2 = normalRec.getSecondAllele();
//			char tC1 = tumourRec.getFirstAllele();
//			char tC2 = tumourRec.getSecondAllele();
//			
//			if (isHeterozygous(nC1, nC2) && isHeterozygous(tC1, tC2)) {
//				// both are het
//				bothHetCount ++ ;
//			} else if (isHeterozygous(nC1, nC2)) {
//				// normal is het, tumour is hom
//				nHetTHomCount++;
//			} else if (isHeterozygous(tC1, tC2)) {
//				// normal is hom, tumour is het
//				nHomTHetCount++;
//			} else {
//				// both hom
//				bothHomCount++;
//			}
		}
		int percentageTotal = 0;
		for (int i : callsInBothArray) {
			percentageTotal+= i;
		}
		logger.info("STATS:");
		logger.info("sameCallInBoth (<1%): " + callsInBothArray[0] + ", (between 1% and 5%): " + callsInBothArray[1] 
		                +  ", (between 5% and 10%): " + callsInBothArray[2] +", >= 10% diff: " +  (count - percentageTotal) + ", totalCount: " + count + ", diffStrand: " + diffStrand);
		
	}
//	private void compareIlluminaData() {
//		
//		int bothHetCount = 0, bothHomCount = 0, nHetTHomCount = 0, nHomTHetCount = 0, count = 0;
//		
//		
//		for (Entry<ChrPosition, IlluminaRecord> entry : normalIlluminaMap.entrySet()) {
//			
//			ChrPosition cp = entry.getKey();
//			IlluminaRecord normalRec = entry.getValue();
//			
//			// get tumour equivalent
//			IlluminaRecord tumourRec = tumourIlluminaMap.get(cp);
//			
//			if (null == tumourRec) {
//				// only care about positions that have matching N&T illumina records
//				continue;
//			}
//			count++;
//			
//			char nC1 = normalRec.getFirstAllele();
//			char nC2 = normalRec.getSecondAllele();
//			char tC1 = tumourRec.getFirstAllele();
//			char tC2 = tumourRec.getSecondAllele();
//			
//			if (isHeterozygous(nC1, nC2) && isHeterozygous(tC1, tC2)) {
//				// both are het
//				bothHetCount ++ ;
//			} else if (isHeterozygous(nC1, nC2)) {
//				// normal is het, tumour is hom
//				nHetTHomCount++;
//			} else if (isHeterozygous(tC1, tC2)) {
//				// normal is hom, tumour is het
//				nHomTHetCount++;
//			} else {
//				// both hom
//				bothHomCount++;
//			}
//		}
//		
//		logger.info("STATS:");
//		logger.info("bothHetCount: " + bothHetCount + ", bothHomCount: " + bothHomCount + ", nHetTHomCount: " + nHetTHomCount
//				+ ", nHomTHetCount: " + nHomTHetCount + ", totalCount: " + count);
//		
//	}
	
	public static final boolean isHeterozygous(char c1, char c2) {
		return c1 != c2;
	}
	
	
	static void loadIlluminaData(File illuminaFile, Map<ChrPosition, IlluminaRecord> illuminaMap) throws IOException {
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);
		IlluminaRecord tempRec;
		try {
			for (Record rec : reader) {
				tempRec = (IlluminaRecord) rec;
				
				// only interested in illumina data if it has a gc score above 0.7, and a valid chromosome
//				if (tempRec.getGCScore() >= 0.7f ) {
					
					//get XY, 0 for chromosome
					// ignore chromosome 0, and for XY, create 2 records, one for each!
					if (null != tempRec.getChr() && ! "0".equals(tempRec.getChr()) && tempRec.getGCScore() > 0.6999 ) {
						
						if ("XY".equals(tempRec.getChr())) {
							// add both X and Y to map
							illuminaMap.put(new ChrPosition("chrX", tempRec.getStart()), tempRec);
							illuminaMap.put(new ChrPosition("chrY", tempRec.getStart()), tempRec);
							continue;
						}
						
						// Illumina record chromosome does not contain "chr", whereas the positionRecordMap does - add
						illuminaMap.put(new ChrPosition("chr" + tempRec.getChr(), tempRec.getStart()), tempRec);
					}
//				}
			}
		} finally{
			reader.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		CompareIlluminaData sp = new CompareIlluminaData();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running CompareIlluminaData:", e);
			else System.err.println("Exception caught whilst running CompareIlluminaData");
			e.printStackTrace();
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
			logger = QLoggerFactory.getLogger(CompareIlluminaData.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareIlluminaData", CompareIlluminaData.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QSignatureException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			return engage();
		}
		return returnStatus;
	}
	
}
