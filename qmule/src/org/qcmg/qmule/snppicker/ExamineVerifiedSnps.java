/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.pileup.QPileupFileReader;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.VerifiedSnpFileReader;
import org.qcmg.pileup.VerifiedSnpRecord;
import org.qcmg.vcf.VCFFileReader;

public class ExamineVerifiedSnps {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ExamineVerifiedSnps.class);
	
	private static Map<ChrPosition,QSnpRecord> pileup = new HashMap<ChrPosition,QSnpRecord>(80000);
	private static Map<ChrPosition,VcfRecord> vcfRecords = new HashMap<ChrPosition,VcfRecord>(80000);
	private static Map<ChrPosition,VerifiedSnpRecord> verifiedSNPs = new HashMap<ChrPosition,VerifiedSnpRecord>(250);

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
			logger.info("loaded "  + pileup.size() + " entries into the pileup map");
			examine(args[2]);
		} else {
			logger.info("running in vcf mode");
			loadGATKData(args[0]);
			logger.info("loaded "  + vcfRecords.size() + " entries into the vcf map");
			examineVCF(args[2]);
		}
		logger.info("goodbye...");
	}
	
	private static void examine(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			
			int verifiedYes = 0, qsnpVerifiedYes = 0;
			int verifiedNo = 0, qsnpVerifiedNo = 0;
			int verifiedNoGL = 0, qsnpVerifiedNoGL = 0;
			
			FileWriter writer = new FileWriter(new File(outputFile));
		
			// loop through the verified snps
			
			for (final Map.Entry<ChrPosition,VerifiedSnpRecord> entry : verifiedSNPs.entrySet()) {
				
				QSnpRecord qpr = pileup.get(entry.getKey());
				VerifiedSnpRecord vsr = entry.getValue();
				
				// only interested in exome data
				if ( ! "exome".equals(vsr.getAnalysis())) continue;
				
				
				if ("no".equals(vsr.getStatus())) {
					verifiedNo++;
					// if we don't have a matching qpr - good, otherwise, print details
					if (null == qpr) {
						qsnpVerifiedNo++;
						writer.write(vsr.getFormattedString() + "\tOK - no entry in qsnp\n");
					} else {
						writer.write(vsr.getFormattedString() + "\t???\t" + qpr.getClassification() + "\t" 
								+ qpr.getMutation() + getAnnotationAndNote(qpr) + "\n");
					}
					
				} else if ("yes".equals(vsr.getStatus())) {
					verifiedYes++;
					if (null != qpr) {
						qsnpVerifiedYes++;
						writer.write(vsr.getFormattedString() + "\tOK - entry in qsnp\t" + qpr.getClassification() + "\t" 
								+ qpr.getMutation() + getAnnotationAndNote(qpr) +"\n");
					} else {
						writer.write(vsr.getFormattedString() + "\t???\n");
					}
				} else if ("no -GL".equals(vsr.getStatus())) {
					verifiedNoGL++;
					if (null != qpr) {
						qsnpVerifiedNoGL++;
					
						writer.write(vsr.getFormattedString() + "\tentry in qsnp\t" + qpr.getClassification() + "\t" 
								+ qpr.getMutation() + getAnnotationAndNote(qpr) +"\n");
					} else {
						writer.write(vsr.getFormattedString() + "\tNo entry in qsnp\n");
					}
				}
			}
			
			writer.close();
			logger.info("verified yes: " + verifiedYes + ", in qsnp: " + qsnpVerifiedYes);
			logger.info("verified no: " + verifiedNo + ", in qsnp: " + (verifiedNo-qsnpVerifiedNo));
			logger.info("verified no -GL: " + verifiedNoGL + ", in qsnp: " + qsnpVerifiedNoGL);
		}
	}
	
	private static void examineVCF(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			
			int verifiedYes = 0, gatkVerifiedYes = 0;
			int verifiedNo = 0, gatkVerifiedNo = 0;
			int verifiedNoGL = 0, gatkVerifiedNoGL = 0;
			
			FileWriter writer = new FileWriter(new File(outputFile));
			
			// loop through the verified snps
			
			for (final Map.Entry<ChrPosition,VerifiedSnpRecord> entry : verifiedSNPs.entrySet()) {
				
				VcfRecord qpr = vcfRecords.get(entry.getKey());
				VerifiedSnpRecord vsr = entry.getValue();
				
				// only interested in exome data
				if ( ! "exome".equals(vsr.getAnalysis())) continue;
				
				if ("no".equals(vsr.getStatus())) {
					verifiedNo++;
					// if we don't have a matching qpr - good, otherwise, print details
					if (null == qpr) {
						gatkVerifiedNo++;
						writer.write(vsr.getFormattedString() + "\tOK - no entry in GATK\n");
					} else {
						writer.write(vsr.getFormattedString() + "\t???\t" + 
								VcfUtils.getGenotypeFromGATKVCFRecord(qpr) + "\t" + qpr.getAlt() + "\n");
//						writer.write(vsr.getFormattedString() + "\t???\t" + qpr.getGenotype() + "\t" + qpr.getAlt() + "\n");
					}
					
				} else if ("yes".equals(vsr.getStatus())) {
					verifiedYes++;
					if (null != qpr) {
						gatkVerifiedYes++;
						writer.write(vsr.getFormattedString() + "\tOK - entry in GATK\t" + 
								VcfUtils.getGenotypeFromGATKVCFRecord(qpr) + "\t" + qpr.getAlt() +"\n");
					} else {
						writer.write(vsr.getFormattedString() + "\t???\n");
					}
				} else if ("no -GL".equals(vsr.getStatus())) {
					verifiedNoGL++;
					if (null != qpr) {
						gatkVerifiedNoGL++;
						
						writer.write(vsr.getFormattedString() + "\tentry in GATK\t" + 
								VcfUtils.getGenotypeFromGATKVCFRecord(qpr) + "\t" + qpr.getAlt() +"\n");
					} else {
						writer.write(vsr.getFormattedString() + "\tNo entry in GATK\n");
					}
				}
			}
			
			writer.close();
			logger.info("verified yes: " + verifiedYes + ", in GATK: " + gatkVerifiedYes);
			logger.info("verified no: " + verifiedNo + ", in GATK: " + (verifiedNo-gatkVerifiedNo));
			logger.info("verified no -GL: " + verifiedNoGL + ", in GATK: " + gatkVerifiedNoGL);
		}
	}
	

	
	private static String getAnnotationAndNote(QSnpRecord record) {
		if ( isNull(record.getAnnotation()) && isNull(record.getNote())) return "\tClassA";
		else if (isNull(record.getAnnotation())) return "\tClassB\t" + record.getNote();
		else if (isNull(record.getNote())) return "\tClassB\t" + record.getAnnotation();
		else return "\tClassB\t" + record.getAnnotation() + "\t" + record.getNote();
	}
	
	private static boolean isNull(String string) {
		return null == string || "null".equals(string) || 0 == string.length();
	}
	
	private static void loadQPileup(String pileupFile) throws IOException {
		if (FileUtils.canFileBeRead(pileupFile)) {
			QPileupFileReader reader  = new QPileupFileReader(new File(pileupFile));
			try {
				for (QSnpRecord qpr : reader) {
					pileup.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
				}
			} finally {
				reader.close();
			}
		}
	}
	
	private static void loadGATKData(String pileupFile) throws Exception {
		if (FileUtils.canFileBeRead(pileupFile)) {
			
			VCFFileReader reader  = new VCFFileReader(new File(pileupFile));
			try {
				for (VcfRecord qpr : reader) {
					vcfRecords.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
				}
			} finally {
				reader.close();
			}
		}
	}
	private static void loadVerifiedSnps(String verifiedSnpFile) throws IOException {
		if (FileUtils.canFileBeRead(verifiedSnpFile)) {
			VerifiedSnpFileReader reader  = new VerifiedSnpFileReader(new File(verifiedSnpFile));
			try {
				for (VerifiedSnpRecord vsr : reader) {
					verifiedSNPs.put(new ChrPosition(vsr.getChromosome(), vsr.getPosition()),vsr);
				}
			} finally {
				reader.close();
			}
		}
	}
}
