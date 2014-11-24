/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import net.sf.samtools.SAMRecord;

import org.qcmg.chrconv.ChrConvFileReader;
import org.qcmg.chrconv.ChromosomeConversionRecord;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.QSnpGATKRecord;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.germlinedb.GermlineDBFileReader;
import org.qcmg.germlinedb.GermlineDBRecord;
import org.qcmg.picard.QJumper;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.Options.Ids;
import org.qcmg.qmule.QMuleException;
import org.qcmg.vcf.VCFFileReader;

public class GatkUniqueSnps {
	
//	private static final QLogger logger = QLoggerFactory.getLogger(GatkUniqueSnps.class);
	private static QLogger logger;
	
	private static Map<ChrPosition,QSnpGATKRecord> tumourRecords = new HashMap<ChrPosition,QSnpGATKRecord>(100000);
	private static Map<ChrPosition,QSnpGATKRecord> normalRecords = new HashMap<ChrPosition,QSnpGATKRecord>(100000);
	
//	private static Map<ChrPosition,VCFRecord> classABRecords = new HashMap<ChrPosition,VCFRecord>(100000);
	private static List<QSnpRecord> qPileupRecords = new ArrayList<QSnpRecord>(15000);
	
	// map to hold chromosome conversion data
	private static final Map<String, String> ensembleToQCMG = new HashMap<String, String>(110);
	
	
	// constants
	private String mutationIdPrefix;
	private String tumourSampleId;
	private String normalSampleId;
	private String patientId;
	private String somaticAnalysisId;
	private String germlineAnalysisId;
//	private String analysisId;
//	private static final String mutationIdPrefix = "APGI_1992_";
//	private static final String analysisId = "qcmg_ssm_20110524_1";
//	private static final String tumourSampleId = "ICGC-ABMP-20091203-06-TD";
	
	
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private Properties ids;
	
	private int exitStatus;
	
	
	private static String bamFile1;
	private static String bamFile1Index;
//	private static String bamFile2;
//	private static String bamFile2Index;
	
	private static QJumper jumper1;
//	private static QJumper jumper2;
	
	public int engage() throws Exception {
		
		setupIds();
		
		logger.info("loading normal vcf file");
		loadGATKData(cmdLineInputFiles[0], normalRecords);
		logger.info("loaded " + normalRecords.size() + " normal vcf's");
		
		logger.info("loading tumour vcf file");
		loadGATKData(cmdLineInputFiles[1], tumourRecords);
		logger.info("loaded " + tumourRecords.size() + " tumour vcf's");
		
		bamFile1 = cmdLineInputFiles[2];
		bamFile1Index = cmdLineInputFiles[3];
//		bamFile2 = args[4];
//		bamFile2Index = args[5];
		
		
		jumper1 = new QJumper();
		jumper1.setupReader(bamFile1, bamFile1Index);
//		jumper2 = new QJumper();
//		jumper2.setupReader(bamFile2, bamFile2Index);
		
		
		logger.info("about to call examine");
		examine();
		logger.info("about to call examine - DONE");
		
		// close the qjumper
		jumper1.closeReader();
		
		logger.info("about to load chromosome conversion data");
		loadChromosomeConversionData(cmdLineInputFiles[4]);
		logger.info("about to load chromosome conversion data - DONE");
		
		logger.info("about to add germlineDB info");
		addGermlineDBData(cmdLineInputFiles[5]);
		
		int noAnnotation = 0;
		for (QSnpRecord qpr : qPileupRecords) if (null == qpr.getAnnotation()) noAnnotation++;
		logger.info("class A after addition of germlinedb data: " + noAnnotation );
		
		
		logger.info("writing output");
		writeOutputForDCC(cmdLineOutputFiles[0]);
		logger.info("DONE");
		
		return exitStatus;
	}
	
	private void setupIds() throws Exception {
		if (null != ids) {
			
			somaticAnalysisId = (String) ids.get(Ids.SOMATIC_ANALYSIS);
			germlineAnalysisId = (String) ids.get(Ids.GEMLINE_ANALYSIS);
			tumourSampleId = (String) ids.get(Ids.TUMOUR_SAMPLE);
			normalSampleId = (String) ids.get(Ids.NORMAL_SAMPLE);
			patientId  = (String) ids.get(Ids.PATIENT);
			mutationIdPrefix = patientId + "_SNP_";
			
			logger.tool("somaticAnalysisId: " + somaticAnalysisId);
			logger.tool("germlineAnalysisId: " + germlineAnalysisId);
			logger.tool("normalSampleId: " + normalSampleId);
			logger.tool("tumourSampleId: " + tumourSampleId);
			logger.tool("patientId: " + patientId);
			logger.tool("mutationIdPrefix: " + mutationIdPrefix);
			
		} else {
			logger.error("No ids were passed into the program");
			throw new Exception("Invalid arguments to GatkUniqueSnps");
		}
	}
	
	private static void examine() throws Exception {
		
		int existsInNormalAndTumour = 0, sameGenotype = 0;
		// loop through the tumour map
		
		for (Entry<ChrPosition,QSnpGATKRecord> tumourEntry : tumourRecords.entrySet()) {
			
			// see if a position exists in the normal map
			QSnpGATKRecord normalRecord = normalRecords.get(tumourEntry.getKey());
			if (null != normalRecord) {
				existsInNormalAndTumour++;
				
				GenotypeEnum normalGenotype = normalRecord.getGenotypeEnum();
				GenotypeEnum tumourGenotype = tumourEntry.getValue().getGenotypeEnum();
				
				if (normalGenotype == tumourGenotype) {
					sameGenotype++;
				} else {
					if (tumourGenotype.containsAllele(normalRecord.getAlt().charAt(0))) {
						tumourEntry.getValue().getVCFRecord().addInfo("MIN");
					}
					if ( tumourGenotype.isHeterozygous() && ! tumourGenotype.containsAllele(tumourEntry.getValue().getRef().charAt(0)))
						tumourEntry.getValue().getVCFRecord().addInfo("tumour heterozygous for two non-reference alleles");
					
					if (null == tumourEntry.getValue().getAnnotation()) {
						qPileupRecords.add(getQPileupRecord(tumourEntry.getValue()));
					}
				}
			} else {
				// interested primarily in these fellas
				qPileupRecords.add(getQPileupRecord(tumourEntry.getValue()));
			}
		}
		
		logger.info("exists in both normal and tumour: " + existsInNormalAndTumour + ", same Genotype: " + sameGenotype);
		
		logger.info("potential number of class A&B's before pileup: " + qPileupRecords.size() );
		
		int noAnnotation = 0, count = 0;
		for (QSnpRecord qpr : qPileupRecords) {
			getPileup(jumper1, qpr);
			
			if (++count % 100 == 0)
				logger.info("hit " + count + " vcf records, " + qpr.getFormattedString());
			
			if (qpr.getAnnotation() == null)
				noAnnotation++;
		}
		
		logger.info("class A after pileup: " + noAnnotation );
		
	}
	
	private static void loadChromosomeConversionData(String chrConvFile) throws IOException  {
		ChrConvFileReader reader = new ChrConvFileReader(new File(chrConvFile));
		try {
			for (ChromosomeConversionRecord record : reader) {
				// add extra map inserts here as required
				ensembleToQCMG.put(record.getEnsembleV55(), record.getQcmg());
			}
		} finally {
			reader.close();
		}
	}
	
	private void writeOutputForDCC(String dccSomaticFile) throws IOException {
		if (dccSomaticFile.contains("Germline_DB.txt")) throw new IOException("Wrong output file!!!");
		
		FileWriter somaticWriter = new FileWriter(new File(dccSomaticFile));
			
		String somaticHeader = "analysis_id\ttumour_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation\tquality_score\tprobability\tread_count\tis_annotated\tvalidation_status\tvalidation_platform\txref_ensembl_var_id\tnote\tQCMGflag\n";
		int counter = 1;
		try {
			
			somaticWriter.write(somaticHeader);
			for (QSnpRecord record : qPileupRecords) {
				
				String ensemblChr = null;
				// get ensembl chromosome
				for (Map.Entry<String, String> entry : ensembleToQCMG.entrySet()) {
					if (record.getChromosome().equals(entry.getValue())) {
						ensemblChr = entry.getKey();
						break;
					}
				}
				somaticWriter.write(somaticAnalysisId + "\t" + tumourSampleId + "\t" 
						+ record.getDCCData(mutationIdPrefix, ensemblChr) + "\n");
			}
		} finally {
			somaticWriter.close();
		}
	}
	
	private static QSnpRecord getQPileupRecord(QSnpGATKRecord vcfRec) {
		QSnpRecord qpr = new QSnpRecord(vcfRec.getChromosome(), vcfRec.getPosition(), vcfRec.getRef());
		qpr.setTumourGenotype(vcfRec.getGenotypeEnum());
		qpr.setMutation(vcfRec.getRef() + Constants.MUT_DELIM + vcfRec.getAlt());
		qpr.getVcfRecord().setFilter(vcfRec.getAnnotation());
		qpr.setClassification(Classification.SOMATIC);
		return qpr;
	}
	
	
	public static void getPileup(QJumper jumper, QSnpRecord record) throws Exception {
		
		List<SAMRecord> firstSet = jumper.getRecordsAtPosition(record.getChromosome(), record.getPosition());
//		List<SAMRecord> secondSet = jumper2.getRecordsAtPosition(record.getChromosome(), record.getPosition());
		
		
		examinePileup(firstSet, record);
		
		
//		char mutation = record.getMutation().charAt(record.getMutation().length() -1);
//		boolean mutationFoundInNormal = false;
//		int normalCoverage = 0;
//		for (SAMRecord sam : firstSet ) {
//			if ( ! sam.getDuplicateReadFlag()) {
//				++normalCoverage;
//				
//				// need to get the base at the position
//				int offset = record.getPosition() - sam.getAlignmentStart();
//				if (offset < 0) throw new Exception("invalid start position!!!");
//				
//				if (sam.getReadBases()[offset] == mutation) {
//					mutationFoundInNormal = true;
//					break;
//				}
//			}
//		}
//		
//		if (mutationFoundInNormal) {
//			record.addAnnotation("mutation also found in pileup of normal");
//		}
//		
//		record.setNormalCount(normalCoverage);
//		
//		if (normalCoverage < 12)
//			record.addAnnotation("less than 12 reads coverage in normal");
		
	}
	
	
	public static void examinePileup(List<SAMRecord> sams, QSnpRecord record) throws Exception {
		
		char mutation = record.getMutation().charAt(record.getMutation().length() -1);
		boolean mutationFoundInNormal = false;
		int normalCoverage = 0;
		for (SAMRecord sam : sams ) {
			if ( ! sam.getDuplicateReadFlag()) {
				++normalCoverage;
				
				// need to get the base at the position
//				int offset = record.getPosition() - sam.getUnclippedStart();
				int offset = record.getPosition() - sam.getAlignmentStart();
				if (offset < 0) throw new Exception("invalid start position!!!: "+ sam.format());
				
				if (offset >= sam.getReadLength()) {
//					throw new Exception("offset [position: " + record.getPosition() + ", read start pos(unclipped): " + sam.getUnclippedStart() + ", read end pos(unclipped): " + sam.getUnclippedEnd()+ "] is larger than read length!!!: " + sam.format());
					// set to last entry in sequence
					offset = sam.getReadLength() -1; 
				}
				
				if (sam.getReadBases()[offset] == mutation) {
					mutationFoundInNormal = true;
//					break;
				}
			}
		}
		
		if (mutationFoundInNormal) {
			VcfUtils.updateFilter(record.getVcfRecord(), VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL);
		}
		
		record.setNormalCount(normalCoverage);
		
		if (normalCoverage < 12) {
			VcfUtils.updateFilter(record.getVcfRecord(), VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12);
		}
		
		
	}
	
	
//	private static void getPileup(VCFRecord record) {
//		
//		List<SAMRecord> firstSet = jumper1.getRecordsAtPosition(record.getChromosome(), record.getPosition());
////		List<SAMRecord> secondSet = jumper2.getRecordsAtPosition(record.getChromosome(), record.getPosition());
//		
//		int normalCoverage = 0;
//		for (SAMRecord sam : firstSet ) {
//			if ( ! sam.getDuplicateReadFlag())
//				++normalCoverage;
//		}
//		
//		
////		int normalCoverage = firstSet.size();
////		int normalCoverage = firstSet.size() + secondSet.size();
//		record.setNormalCoverage(normalCoverage);
//		
//		if (normalCoverage < 12)
//			record.addAnnotation("less than 12 reads coverage in normal");
//		
//	}
	
	
	private static void addGermlineDBData(String germlineDBFile) throws IOException {
		
		GermlineDBFileReader reader = new GermlineDBFileReader(new File(germlineDBFile));
		// create map of SOMATIC classified SNPs
		Map<ChrPosition, QSnpRecord> somaticPileupMap = new HashMap<ChrPosition, QSnpRecord>(qPileupRecords.size(), 1);
		for (QSnpRecord pileupRecord : qPileupRecords) {
			somaticPileupMap.put(new ChrPosition(pileupRecord.getChromosome(), pileupRecord.getPosition()), pileupRecord);
		}
		
		int updateCount = 0, count = 0;
		try {
			for (GermlineDBRecord rec : reader) {
				
				// get QCMG chromosome from map
				String chr = ensembleToQCMG.get(rec.getChromosome());
				ChrPosition id = new ChrPosition(chr, rec.getPosition());
				
				QSnpRecord qpr = somaticPileupMap.get(id);
				if (null != qpr && null != qpr.getMutation() && (null == qpr.getAnnotation() || ! qpr.getAnnotation().contains(VcfHeaderUtils.FILTER_GERMLINE))) {
					String mutation = qpr.getMutation();
					if (mutation.length() == 3) {
						char c = mutation.charAt(2);
						
						GenotypeEnum germlineDBGenotype = BaseUtils.getGenotypeEnum(rec.getNormalGenotype());
						if (germlineDBGenotype.containsAllele(c)) {
							updateCount++;
							VcfUtils.updateFilter(qpr.getVcfRecord(), VcfHeaderUtils.FILTER_GERMLINE);
						}
						
						
					} else {
						logger.info("mutation string length: " + mutation.length());
					}
				}
				
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " germline reords");
				
			}
		} finally {
			reader.close();
		} 
		logger.info("updated: " + updateCount + " somatic positions with germlineDB info");
	}
	
	private static void loadGATKData(String pileupFile, Map<ChrPosition,QSnpGATKRecord> map) throws Exception {
		if (FileUtils.canFileBeRead(pileupFile)) {
			
			VCFFileReader reader  = new VCFFileReader(new File(pileupFile));
			try {
				for (VCFRecord qpr : reader) {
					map.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()), new QSnpGATKRecord(qpr));
				}
			} finally {
				reader.close();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		GatkUniqueSnps gus = new GatkUniqueSnps();
		int exitStatus = gus.setup(args);
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
			logger = QLoggerFactory.getLogger(GatkUniqueSnps.class, options.getLogFile(), options.getLogLevel());
			logger.logInitialExecutionStats("GatkUniqueSnps", GatkUniqueSnps.class.getPackage().getImplementationVersion());
			
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
			
			ids = options.getIds();
			
			return engage();
		}
		return returnStatus;
	}

}
