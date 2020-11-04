/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.pileup.PileupFileReader;

public class UniqueQSnps {
	
	private static final QLogger logger = QLoggerFactory.getLogger(UniqueQSnps.class);
	
	private static Map<ChrPosition,String> qSnpPileup = new HashMap<ChrPosition,String>(10000);
//	private static Map<ChrPosition,PileupRecord> qSnpPileup = new HashMap<ChrPosition,PileupRecord>(10000);
	private static Map<ChrPosition,String> gatkVcfs = new HashMap<ChrPosition,String>(10000);
//	private static Map<ChrPosition,PileupRecord> gatkVcfs = new HashMap<ChrPosition,PileupRecord>(10000);
	private static Map<ChrPosition,String> verifiedSNPs = new HashMap<ChrPosition,String>(500);
//	private static Map<ChrPosition,PileupRecord> verifiedSNPs = new HashMap<ChrPosition,PileupRecord>(500);
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	
	public static void main(String[] args) throws Exception {
		logger.info("hello...");
		
		String filename = args[0];
		boolean runQPileup = true;
		// filename type depends on whether to load qpileup or vcf
		if (FileUtils.isFileTypeValid(filename, "vcf")) {
			runQPileup = false;
		}
		loadVerifiedSnps(args[1]);
		logger.info("loaded "  + verifiedSNPs.size() + " entries into the verifiedSNPs map");
		
		
		if (runQPileup) {
			// load the existing pileup into memory
			logger.info("running in pileup mode");
			loadQPileup(args[0]);
			logger.info("loaded "  + qSnpPileup.size() + " entries into the pileup map");
			examine(args[2]);
		} else {
			logger.info("running in vcf mode");
			loadGatkData(args[0]);
			logger.info("loaded "  + gatkVcfs.size() + " entries into the vcf map");
			examineVCFs(args[2]);
		}
		
		
		// load the existing pileup into memory
		
		examine(args[2]);
		logger.info("goodbye...");
	}
	
	
	private static void examine(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			int totalCount = 0, uniqueQSnpClassACount = 0, uniqueQSnpClassBCount = 0;
			
			FileWriter writer = new FileWriter(new File(outputFile));
		
			// loop through the verified snps
			
			for (final Map.Entry<ChrPosition,String> entry : qSnpPileup.entrySet()) {
				++totalCount;
				String verifiedRecord = verifiedSNPs.get(entry.getKey());
//				PileupRecord verifiedRecord = verifiedSNPs.get(entry.getKey());
				String qSnpRecord = entry.getValue();
				
				if (null == verifiedRecord) {
					String [] params = TabTokenizer.tokenize(qSnpRecord);
//					String [] params = tabbedPattern.split(qSnpRecord.getPileup());
					String annotation = params[params.length-1];
					if ("--".equals(annotation)) {
						++uniqueQSnpClassACount;
						writer.write(qSnpRecord + "\n");
					} else if ("less than 12 reads coverage in normal".equals(annotation)) {
						++uniqueQSnpClassBCount;
						writer.write(qSnpRecord + "\n");
					}
				}
			}
			
			writer.close();
			logger.info("totalCount: " + totalCount + ", uniqueQSnpCount (class A): " + uniqueQSnpClassACount + ", uniqueQSnpCount (class B): " + uniqueQSnpClassBCount );
		}
	}
	
	private static void examineVCFs(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			int totalCount = 0, uniqueQSnpClassACount = 0, uniqueQSnpClassBCount = 0;
			
			FileWriter writer = new FileWriter(new File(outputFile));
			
			// loop through the verified snps
			
			for (final Map.Entry<ChrPosition,String> entry : qSnpPileup.entrySet()) {
				++totalCount;
				String verifiedRecord = verifiedSNPs.get(entry.getKey());
//				PileupRecord verifiedRecord = verifiedSNPs.get(entry.getKey());
				String qSnpRecord = entry.getValue();
//				PileupRecord qSnpRecord = entry.getValue();
				
				if (null == verifiedRecord) {
					String [] params = TabTokenizer.tokenize(qSnpRecord);
//					String [] params = tabbedPattern.split(qSnpRecord.getPileup());
					String annotation = params[params.length-1];
					if ("--".equals(annotation)) {
						++uniqueQSnpClassACount;
						writer.write(qSnpRecord + "\n");
					} else if ("less than 12 reads coverage in normal".equals(annotation)) {
						++uniqueQSnpClassBCount;
						writer.write(qSnpRecord + "\n");
					}
				}
			}
			
			writer.close();
			logger.info("totalCount: " + totalCount + ", uniqueQSnpCount (class A): " + uniqueQSnpClassACount + ", uniqueQSnpCount (class B): " + uniqueQSnpClassBCount );
		}
	}
	
	
	private static void loadQPileup(String pileupFile) throws Exception {
		if (FileUtils.canFileBeRead(pileupFile)) {
			PileupFileReader reader  = new PileupFileReader(new File(pileupFile));
			for (String pr : reader) {
//				for (PileupRecord pr : reader) {
				String [] params = TabTokenizer.tokenize(pr);
//				String [] params = tabbedPattern.split(pr.getPileup());
				String chrPosition = params[params.length-2];
//				logger.info("chrPosition: " + chrPosition);
				//TODO refactor to use StringUtils.getChrPositionFromString()
				int start = Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-")));
				ChrPosition chrPos = new ChrRangePosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), start, start);						 
				
				qSnpPileup.put(chrPos,pr);
			}
			reader.close();
		}
	}
	
	private static void loadGatkData(String pileupFile) throws Exception {
		if (FileUtils.canFileBeRead(pileupFile)) {
			PileupFileReader reader  = new PileupFileReader(new File(pileupFile));
			for (String pr : reader) {
//				for (PileupRecord pr : reader) {
				String [] params = TabTokenizer.tokenize(pr);
//				String [] params = tabbedPattern.split(pr.getPileup());
				String chrPosition = params[params.length-2];
//				logger.info("chrPosition: " + chrPosition);
				//TODO refactor to use StringUtils.getChrPositionFromString()
				int start = Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-")));
				ChrPosition chrPos = new ChrRangePosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), start, start);
				
				gatkVcfs.put(chrPos,pr);
			}
			reader.close();
		}
	}
	
	private static void loadVerifiedSnps(String verifiedSnpFile) throws Exception {
		if (FileUtils.canFileBeRead(verifiedSnpFile)) {
			
			PileupFileReader reader  = new PileupFileReader(new File(verifiedSnpFile));
			for (String pr : reader) {
//				for (PileupRecord pr : reader) {
				String [] params = TabTokenizer.tokenize(pr);
//				String [] params = tabbedPattern.split(pr.getPileup());
				String chrPosition = params[2];
//				logger.info("chrPosition: " + chrPosition);
				//TODO refactor to use StringUtils.getChrPositionFromString()
				int start = Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-")));
				ChrPosition chrPos = new ChrRangePosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), start, start);
				
				verifiedSNPs.put(chrPos,pr);
			}
			reader.close();
		}
	}
	
}
