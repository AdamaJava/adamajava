/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.qio.illumina.IlluminaFileReader;
import org.qcmg.qio.illumina.IlluminaRecord;

/**
 * This class takes in 2 Illumina snp chip files as input 
 * and categorises the difference in A allele percentage values as being
 * either less then 1%, less than 5% or less than 10% concordant.
 * This information is presented to the user in the generated log file, the path of which must be supplied by the user.
 * 
 * NOTE that it does not compare qsig.vcf.gz files as other comparers in this package do.
 * 
 * @author oliverh
 *
 */
public class CompareIlluminaData {
	
	private static QLogger logger;
    private String[] cmdLineInputFiles;
	private int exitStatus;
	
	private final Map<ChrPosition, IlluminaRecord> normalIlluminaMap = new HashMap<>();
	private final Map<ChrPosition, IlluminaRecord> tumourIlluminaMap = new HashMap<>();
	
	public int engage() throws Exception {
		
		loadIlluminaData(new File(cmdLineInputFiles[0]), normalIlluminaMap);
		logger.info("Normal illumina data loaded: " + normalIlluminaMap.size());
		loadIlluminaData(new File(cmdLineInputFiles[1]), tumourIlluminaMap);
		logger.info("Tumour illumina data loaded: " + normalIlluminaMap.size());
		
		compareIlluminaData();
		
		return exitStatus;
	}
	
	private void compareIlluminaData() {
		
		int count = 0, diffStrand = 0;
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
		}
		
		int percentageTotal = 0;
		for (int i : callsInBothArray) {
			percentageTotal+= i;
		}
		
		logger.info("STATS:");
		logger.info("sameCallInBoth (<1%): " + callsInBothArray[0] + ", (between 1% and 5%): " + callsInBothArray[1] 
		                +  ", (between 5% and 10%): " + callsInBothArray[2] +", >= 10% diff: " +  (count - percentageTotal) + ", totalCount: " + count + ", diffStrand: " + diffStrand);
		
	}
	
	static void loadIlluminaData(File illuminaFile, Map<ChrPosition, IlluminaRecord> illuminaMap) throws IOException {
		
		try (IlluminaFileReader reader = new IlluminaFileReader(illuminaFile)) {
			 
			for (IlluminaRecord rec : reader) {
				 				
				// only interested in illumina data if it has a gc score above 0.7, and a valid chromosome
				// get XY, 0 for chromosome
				// ignore chromosome 0, and for XY, create 2 records, one for each!
				
				if (null != rec.getChr() && ! "0".equals(rec.getChr()) && rec.getGCScore() > 0.6999 ) {
					
					if ("XY".equals(rec.getChr())) {
						// add both X and Y to map
						illuminaMap.put(ChrPointPosition.valueOf("chrX", rec.getStart()), rec);
						illuminaMap.put(ChrPointPosition.valueOf("chrY", rec.getStart()), rec);
						continue;
					}
					
					// Illumina record chromosome does not contain "chr", whereas the positionRecordMap does - add
					illuminaMap.put(ChrPointPosition.valueOf("chr" + rec.getChr(), rec.getStart()), rec);
				}
			}
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
	
	protected int setup(String[] args) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.COMPARE_USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.COMPARE_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.COMPARE_USAGE);
			options.displayHelp();
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.COMPARE_USAGE);
			options.displayHelp();
		} else {
			// configure logging
            String logFile = options.getLog();
			logger = QLoggerFactory.getLogger(CompareIlluminaData.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareIlluminaData", CompareIlluminaData.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
                for (String cmdLineInputFile : cmdLineInputFiles) {
                    if (!FileUtils.canFileBeRead(cmdLineInputFile)) {
                        throw new QSignatureException("INPUT_FILE_READ_ERROR", cmdLineInputFile);
                    }
                }
			}
			
			return engage();
		}
		return returnStatus;
	}
	
}
