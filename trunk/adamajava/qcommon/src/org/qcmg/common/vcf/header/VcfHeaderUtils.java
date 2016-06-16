/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf.header;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.header.VcfHeader.QPGRecord;
import org.qcmg.common.vcf.header.VcfHeader.Record;
 

public class VcfHeaderUtils {
	
	public static final QLogger logger = QLoggerFactory.getLogger(VcfHeaderUtils.class);
	
	public static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
//	public static final VcfHeaderRecord BLANK_HEADER_LINE = new VcfHeaderRecord("##");
	public static final String BLANK_HEADER_LINE = Constants.DOUBLE_HASH;
	
	public static final String DESCRITPION_FILTER_GERMLINE = "Mutation is a germline variant in another patient";
	public static final String DESCRITPION_INFO_GERMLINE = "Counts of donor occurs this mutation, total recorded donor number";
	
	
	public static final String DESCRITPION_FORMAT_COMPOUND_SNP_ALLELE_COUNT = "Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand) ";
	public static final String DESCRITPION_INFO_DB = "dbSNP Membership";
	public static final String DESCRITPION_INFO_VAF = "Variant allele frequencies based on 1000Genomes from dbSNP as the CAF. CAF starting with the reference allele followed " +
			 "by alternate alleles as ordered in the ALT column.   Here we only take the related allel frequency.";
 	
	//FILTER FIELDS
	public static final String FILTER_PASS = "PASS";
	public static final String FILTER_COVERAGE_NORMAL_12 = SnpUtils.LESS_THAN_12_READS_NORMAL;
	public static final String FILTER_COVERAGE_NORMAL_8 = SnpUtils.LESS_THAN_8_READS_NORMAL;
	public static final String FILTER_COVERAGE_TUMOUR = SnpUtils.LESS_THAN_8_READS_TUMOUR;
	public static final String FILTER_GERMLINE = SnpUtils.MUTATION_GERMLINE_IN_ANOTHER_PATIENT;
	public static final String FILTER_MUTATION_IN_NORMAL = SnpUtils.MUTATION_IN_NORMAL;
	public static final String FILTER_MUTATION_IN_UNFILTERED_NORMAL = SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
	public static final String FILTER_SAME_ALLELE_NORMAL = SnpUtils.LESS_THAN_3_READS_NORMAL;
	public static final String FILTER_SAME_ALLELE_TUMOUR = SnpUtils.LESS_THAN_3_READS_TUMOUR;
	public static final String FILTER_NOVEL_STARTS = SnpUtils.NOVEL_STARTS;
	public static final String FILTER_MUTANT_READS = SnpUtils.MUTANT_READS;
	public static final String FILTER_MUTATION_EQUALS_REF = SnpUtils.MUTATION_EQUALS_REF;
	public static final String FILTER_LOW_QUAL = "LowQual";
	public static final String FILTER_NO_CALL_IN_TEST = SnpUtils.NO_CALL_IN_TEST;
	public static final String FILTER_STRAND_BIAS_ALT = SnpUtils.STRAND_BIAS_ALT;
	public static final String FILTER_STRAND_BIAS_COV = SnpUtils.STRAND_BIAS_COVERAGE;
	public static final String FILTER_TRF = "TRF";
	public static final String DESCRITPION_FILTER_TRF = "at least one of the repeat is with repeat sequence length less than six; "
			+ "and the repeat frequence is more than 10 (or more than six for homoplymers repeat), "
			+ ", or less than 20% of informative reads are strong supporting in case of indel variant";

	
//	public static final String FILTER_HOM = "HOM";
//	public static final String DESCRITPION_FILTER_HOM = "fallen in homoplymers region that is more than 5 repeat base";

	
	//INFO FIELDS
	public static final String INFO_MUTATION = "MU";
	public static final String INFO_FLANKING_SEQUENCE = "FLANK";
	public static final String INFO_DONOR = "DON";	
	public static final String INFO_EFFECT = "EFF";
	public static final String INFO_LOSS_FUNCTION = "LOF";
	public static final String INFO_NONSENSE_MEDIATED_DECAY = "NMD";	
	public static final String INFO_SOMATIC = SnpUtils.SOMATIC;
	public static final String INFO_SOMATIC_DESC = "Indicates that the record is a somatic mutation";
	public static final String INFO_CONFIDENT = "CONF";
	public static final String INFO_GMAF = "GMAF";
	public static final String INFO_CAF = "CAF";
	public static final String INFO_VAF = "VAF";
	public static final String INFO_DB = "DB";
	public static final String INFO_VLD = "VLD";	
	public static final String INFO_FS = "FS";    //previous qSNP used, now changed to FLANK
	public static final String INFO_FILLCOV =  "FULLCOV";
	public static final String INFO_GERMLINE = SnpUtils.MUTATION_GERMLINE_IN_ANOTHER_PATIENT;
	public static final String INFO_CADD = "CADD";	
	public static final String INFO_MERGE_IN = Constants.VCF_MERGE_INFO;
	public static final String DESCRITPION_MERGE_IN = "Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file";	
	public static final String INFO_TRF = FILTER_TRF;
	public static final String DESCRITPION_INFO_TRF = "List all repeat reported by TRFFinder,  crossing over the variant position.all repeat follow <repeat sequence Length>_<repeat frequency>, separated by ';'";

	public static final String INFO_HOM = "HOM";
	public static final String DESCRITPION_INFO_HOM = "nearby reference sequence fallen in a specified widow size,  leading by the number of homopolymers base.";
	
	
	//FORMAT FIELDS
	public static final String FORMAT_GENOTYPE = "GT";
	public static final String FORMAT_GENOTYPE_DETAILS = "GD";
	public static final String FORMAT_ALLELE_COUNT = "AC";
	public static final String FORMAT_ALLELE_COUNT_COMPOUND_SNP = "ACCS";
	public static final String FORMAT_MUTANT_READS = FILTER_MUTANT_READS;
	public static final String FORMAT_NOVEL_STARTS = FILTER_NOVEL_STARTS;
	//GATK specific format fields
	public static final String FORMAT_ALLELIC_DEPTHS = "AD";
	public static final String FORMAT_READ_DEPTH = "DP";
	public static final String FORMAT_GENOTYPE_QUALITY = "GQ";
	
	//Header lines
	public static final String CURRENT_FILE_VERSION = "##fileformat=VCFv4.2";
	public static final String STANDARD_FILE_VERSION = "##fileformat"; 
	public static final String STANDARD_FILE_DATE = "##fileDate";
	public static final String STANDARD_SOURCE_LINE = "##qSource";
	public static final String STANDARD_UUID_LINE = "##qUUID";
	public static final String STANDARD_ANALYSIS_ID = "##qAnalysisId";
	public static final String STANDARD_DONOR_ID = "##qDonorId";
	public static final String STANDARD_CONTROL_SAMPLE = "##qControlSample";
	public static final String STANDARD_TEST_SAMPLE = "##qTestSample";
	public static final String STANDARD_CONTROL_SAMPLE_1 = "##1:qControlSample";
	public static final String STANDARD_TEST_SAMPLE_1 = "##1:qTestSample";
	public static final String STANDARD_CONTROL_BAM = "##qControlBam";
	public static final String STANDARD_TEST_BAM = "##qTestBam";
	public static final String STANDARD_CONTROL_BAM_1 = "##1:qControlBam";
	public static final String STANDARD_TEST_BAM_1 = "##1:qTestBam";
	public static final String STANDARD_CONTROL_BAMID = "##qControlBamUUID";
	public static final String STANDARD_TEST_BAMID = "##qTestBamUUID";
	
	public static final String STANDARD_CONTROL_VCF = "##qControlVcf";
	public static final String STANDARD_TEST_VCF = "##qTestVcf";
	public static final String STANDARD_CONTROL_VCF_UUID = "##qControlVcfUUID";
	public static final String STANDARD_TEST_VCF_UUID = "##qTestVcfUUID";
	public static final String STANDARD_CONTROL_VCF_GATK_VER = "##qControlVcfGATKVersion";
	public static final String STANDARD_TEST_VCF_GATK_VER = "##qTestVcfGATKVersion";
	public static final String GERMDB_DONOR_NUMBER = "##dornorNumber";
	
  
//	public static final String PREVIOUS_UUID_LINE = "##preUuid";
	public static final String STANDARD_DBSNP_LINE = "##dbSNP_BUILD_ID";
	public static final String STANDARD_INPUT_LINE = "##qINPUT";
	public static final String HEADER_LINE_FILTER = "##FILTER";
	public static final String HEADER_LINE_INFO = "##INFO";
	public static final String HEADER_LINE_FORMAT = "##FORMAT";	
	public static final String HEADER_LINE_QPG = "##qPG";	
	public static final String HEADER_LINE_CHROM = "#CHROM";	
	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	public static final String STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT = STANDARD_FINAL_HEADER_LINE + "\tFORMAT\t";
	
	public static final String GATK_CMD_LINE = "##GATKCommandLine";
	public static final String GATK_CMD_LINE_VERSION = "Version=";
	
	public enum VcfInfoType {

		UNKNOWN, String, Integer, Float, Flag, Character;

		public static VcfInfoType parse(String str) {
			if(StringUtils.isNullOrEmpty(str))
				return null;
			
			str = str.toUpperCase();
			if (str.equals("STRING")) return VcfInfoType.String;
			if (str.equals("INTEGER")) return VcfInfoType.Integer;
			if (str.equals("FLOAT")) return VcfInfoType.Float;
			if (str.equals("FLAG")) return VcfInfoType.Flag;
			if (str.equals("CHARACTER")) return VcfInfoType.Character;
			if (str.equals("UNKNOWN")) return VcfInfoType.UNKNOWN;
			throw new IllegalArgumentException("Unknown VcfInfoType '" + str + "'");
		}
	} 	

	/**
	 * replace specified sample column string. It will replace empty string with "null" before specified sample column
	 * @param header: a VcfHeader
	 * @param id: sample id
	 * @param noColumn: add the sample id to specified sample column. First sample column is "1"
	 */
	public static void addSampleId(VcfHeader header, String id, int noColumn){
		if (null == header) {
			throw new IllegalArgumentException("null vcf header object passed to VcfHeaderUtils.addQPGLineToHeader");
		}
		if (noColumn < 1) {
			throw new IllegalArgumentException("invlaid sample column number, must be greater than 0");
		}
		
		String[] exsitIds = header.getSampleId();		
		if (exsitIds == null || exsitIds.length < noColumn) {
			String[] newIds = new String[noColumn];
			
			newIds[noColumn -1] = id;
			
			if (exsitIds != null) {
				System.arraycopy(exsitIds, 0, newIds, 0, exsitIds.length);
			}
			
			exsitIds = newIds;						
		} else {
			exsitIds[noColumn-1] = id;
		}
				
	   StringBuilder str = new StringBuilder(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT);
	   for (int i = 0 ; i < exsitIds.length; i ++) {
		   if (i > 0) {
			   str.append(Constants.TAB);   
		   }
		   str.append(exsitIds[i]);
	   }
		header.parseHeaderLine(str.toString());		
	}
	
	public static void addQPGLineToHeader(VcfHeader header, String tool, String version, String commandLine) {
		if (null == header) {
			throw new IllegalArgumentException("null vcf header object passed to VcfHeaderUtils.addQPGLineToHeader");
		}
		if (StringUtils.isNullOrEmpty(tool) 
				|| StringUtils.isNullOrEmpty(version)
				|| StringUtils.isNullOrEmpty(commandLine) ) {
			
			throw new IllegalArgumentException("null or empty tool, version and/or command line values passed to VcfHeaderUtils.addQPGLineToHeader, tool: " + tool + ", version: " + version + ", cl: " + commandLine);
		}
		
		int currentLargestOrder = 0;
		List<QPGRecord> currentQPGLines = new ArrayList<>(header.getqPGLines());	// returns a sorted collection
		if ( ! currentQPGLines.isEmpty()) {
			currentLargestOrder = currentQPGLines.get(0).getOrder();
		}
		
		// create and add to existing collection
		header.addQPGLine(currentLargestOrder + 1, tool, version, commandLine,  DF.format(new Date()));
		
	}
	
	/**
	 * Retrieves the UUID from the uuid VcfHeader.Record supplied to the method.
	 * If this object is null, then null is returned.
	 * Otherwise, the record is split by '=' and the second parameter is returned to the user.
	 * Again, if the record does not contain '=', then null is returned
	 * 
	 * 
	 * @param uuid VcfHEader.Record uuid of the vcf file
	 * @return null if the supplied uuid record is null
	 */
	public static String getUUIDFromHeaderLine(VcfHeader.Record uuid) {
		if (null == uuid) {
			logger.warn("null uuid record passed to getUUIDFromHeaderLine!!!");
			return null;
		}
		String uuidString = splitMetaRecord(uuid)[1];
		return uuidString;
	}
	
	public static String getGATKVersionFromHeaderLine(VcfHeader header) {
		if (null == header) {
			throw new IllegalArgumentException("Null header passed to VcfHeaderUtils.getGATKVersionFromHeaderLine");
		}
		
		for (VcfHeader.Record rec : header.getMetaRecords()) {
			if (rec.getData().startsWith(GATK_CMD_LINE)) {
				String version = null;
				int index = rec.getData().indexOf(GATK_CMD_LINE_VERSION);
				if (index > -1) {
					int commaIndex = rec.getData().indexOf(Constants.COMMA, index);
					version = rec.getData().substring(index + GATK_CMD_LINE_VERSION.length(), commaIndex);
				}
				return version;
			}
		}
		return null;
	}
	
	
	/**
	 * Merge 2 VcfHEader objects together.
	 * Only merges the FILTER, INFO, FORMAT and META header lines.
	 * qPG and other line which don't have '=' (eg. final CHROM line) are left untouched
	 * 
	 * @param original
	 * @param additional
	 * @param overwrite
	 * @return original header merged from additional 
	 */
	public static VcfHeader mergeHeaders(VcfHeader original, VcfHeader additional, boolean overwrite) {
		if (null == original && null == additional) {
			throw new IllegalArgumentException("Null headers passed to VcfHeaderUtils.mergeHeaders");
		} else if (null == original) {
			return additional;
		} else if (null == additional) {
			return original;
		}
		
		// only merging format, filter, info and meta
		
		for (VcfHeader.Record rec : additional.getInfoRecords().values()) {
			original.addInfo(rec, overwrite);
		}
		for (VcfHeader.Record rec : additional.getFormatRecords().values()) {
			original.addFormat(rec, overwrite);
		}
		for (VcfHeader.Record rec : additional.getFilterRecords().values()) {
			original.addFilter(rec, overwrite);
		}
		for (VcfHeader.Record rec : additional.getMetaRecords()) {
			original.addMeta(rec, overwrite);
		}
		return original;
	}
	
	
	/**
	 * Splits a VcfHeader.Record meta object on '=' and returns a String array with the results of the split command.
	 * If the record does not contain '=' then a 2 element array is returned, with the first element containing record.getData(), and the second element containing null
	 * @param rec VcfHeader.Record
	 * @return String [] containing the result of rec.getData().split("=")
	 */
	public static String[]  splitMetaRecord(VcfHeader.Record rec) {
		if (null == rec || null == rec.getData()) {
			throw new IllegalArgumentException("Null record passed to VcfHeaderUtils.splitMetaRecord");
		}
		
		int index = rec.getData().indexOf(Constants.EQ_STRING);
		if (index >= 0) {
			return  rec.getData().split(Constants.EQ_STRING);
		}
		
		return new String[] {rec.getData(), null};
	}
	
	public static class SplitMetaRecord{
		String[] pair; 
		public SplitMetaRecord(Record record){
			this.pair =  splitMetaRecord(record);
		}
		
		public String getKey(){ return pair[0]; }
		public String getValue(){ return pair[1]; }
	}
	
	
	
	
	public static VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName, Class mainClass) throws IOException {	
		DateFormat df = new SimpleDateFormat("yyyyMMdd"); 
//		VcfHeader myHeader = header;  	
 		
		String version = mainClass.getPackage().getImplementationVersion();
		String pg = mainClass.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);	
			
		String inputUuid = (header.getUUID() == null)? null: new VcfHeaderUtils.SplitMetaRecord(header.getUUID()).getValue();   
		header.replace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING;
	    if(pg == null ) pg = Constants.NULL_STRING;
	    if(cmd == null) cmd = Constants.NULL_STRING;
		VcfHeaderUtils.addQPGLineToHeader(header, pg, version, cmd);
		
		return header;
			
	}
	

 
}
