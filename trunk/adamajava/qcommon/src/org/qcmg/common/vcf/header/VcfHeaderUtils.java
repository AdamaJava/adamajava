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
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
 

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
	public static final String INFO_CONFIDENCE = "CONF";
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
	public static final String FORMAT_OBSERVED_ALLELES_BY_STRAND = "OABS";
	public static final String FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC = "Observed Alleles By Strand: semi-colon separated list of observed alleles with each one in this format: forward_strand_count[avg_base_quality]reverse_strand_count[avg_base_quality], e.g. A18[39]12[42]";
	public static final String FORMAT_ALLELE_COUNT_COMPOUND_SNP = "ACCS";
	public static final String FORMAT_MUTANT_READS = FILTER_MUTANT_READS;
	public static final String FORMAT_MUTANT_READS_DESC = "Number of reads carrying the alt";
	public static final String FORMAT_NOVEL_STARTS = FILTER_NOVEL_STARTS;
	//GATK specific format fields
	public static final String FORMAT_ALLELIC_DEPTHS = "AD";
	public static final String FORMAT_READ_DEPTH = "DP";
	public static final String FORMAT_READ_DEPTH_DESCRIPTION = "Read depth at this position for this sample";
	public static final String FORMAT_GENOTYPE_QUALITY = "GQ";
	public static final String FORMAT_POSSIBLE_GENOTYPES = "PL";

	
	//Header lines
//	public static final String CURRENT_FILE_FORMAT = "##fileformat=VCFv4.3";
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
	
	public static final String STANDARD_CONTROL_BAMID_1 = "##1:qControlBamUUID";
	public static final String STANDARD_TEST_BAMID_1 = "##1:qTestBamUUID";
	
	public static final String STANDARD_CONTROL_VCF = "##qControlVcf";
	public static final String STANDARD_TEST_VCF = "##qTestVcf";
	public static final String STANDARD_CONTROL_VCF_UUID = "##qControlVcfUUID";
	public static final String STANDARD_TEST_VCF_UUID = "##qTestVcfUUID";
	public static final String STANDARD_CONTROL_VCF_GATK_VER = "##qControlVcfGATKVersion";
	public static final String STANDARD_TEST_VCF_GATK_VER = "##qTestVcfGATKVersion";
	public static final String GERMDB_DONOR_NUMBER = "##dornorNumber";
	
	public static final String STANDARD_DBSNP_LINE = "##dbSNP_BUILD_ID";
	public static final String STANDARD_INPUT_LINE = "##qINPUT";
	public static final String HEADER_LINE_QPG = "##qPG";
	
//	public static final String HEADER_LINE_FILTER = "##FILTER";
//	public static final String HEADER_LINE_INFO = "##INFO";
//	public static final String HEADER_LINE_FORMAT = "##FORMAT";	
//	public static final String HEADER_LINE_CHROM = "#CHROM";	
////	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	public static final String STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\t";
	
	public static final String GATK_CMD_LINE = "##GATKCommandLine";
	public static final String GATK_CMD_LINE_VERSION = "Version";
	
	public static final String VERSION = "Version";
	public static final String TOOL = "Tool";
	public static final String DATE = "Date";
	public static final String COMMAND_LINE = "CL";

	
	
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
		if ( null == header ) {
			throw new IllegalArgumentException("null vcf header object passed to VcfHeaderUtils.addQPGLineToHeader");
		}
		if ( noColumn < 1 ) {
			throw new IllegalArgumentException("invlaid sample column number, must be greater than 0");
		}
		
		String[] exsitIds = header.getSampleId();		
		if (exsitIds == null || exsitIds.length < noColumn) {
			String[] newIds = new String[noColumn];
			
			newIds[noColumn -1] = id;
			
			if ( exsitIds != null ) {
				System.arraycopy( exsitIds, 0, newIds, 0, exsitIds.length );
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
		header.addOrReplace(str.toString());		
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
		List<VcfHeaderRecord> currentQPGLines = new ArrayList<>(getqPGRecords(header));	// returns a sorted collection
		if ( ! currentQPGLines.isEmpty()) {
			currentLargestOrder = Integer.parseInt( currentQPGLines.get(0).getId() );
		}
		
		// create and add to existing collection
		addQPGLine(header, currentLargestOrder + 1, tool, version, commandLine,   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		
	}
	
	/**
	 * Retrieves the UUID from the uuid VcfHeaderRecord supplied to the method.
	 * If this object is null, then null is returned.
	 * Otherwise, the record is split by '=' and the second parameter is returned to the user.
	 * Again, if the record does not contain '=', then null is returned
	 * 
	 * 
	 * @param uuid VcfHeaderRecord uuid of the vcf file
	 * @return null if the supplied uuid record is null
	 */
	public static String getUUIDFromHeaderLine(VcfHeaderRecord uuid) {
		if (null == uuid) {
			logger.warn("null uuid record passed to getUUIDFromHeaderLine!!!");
			return null;
		}
		return uuid.getMetaValue();
	}
	
	public static String getGATKVersionFromHeaderLine(VcfHeader header) {
		if (null == header) 
			throw new IllegalArgumentException("Null header passed to VcfHeaderUtils.getGATKVersionFromHeaderLine");
						
		List<VcfHeaderRecord> recs = header.getRecords(GATK_CMD_LINE);		
		for(VcfHeaderRecord rec : recs)
			if(rec.getId() != null)
				return rec.getSubFieldValue(GATK_CMD_LINE_VERSION);	 		 
		return null; 	
	}
	
	
	/**
	 * Merge 2 VcfHEader objects together.
	 * Only merges the FILTER, INFO, FORMAT and META header lines.
	 * qPG and other line which don't have '=' (eg. final CHROM line) are left untouched
	 * 
	 * @param original
	 * @param additional
	 * @param overwrite: replace existing header line if same id exists; otherwise keep all header line
	 * @return original header merged from additional 
	 */
	public static VcfHeader mergeHeaders(VcfHeader original, VcfHeader additional, boolean overwrite) {
		if (null == original && null == additional) 
			throw new IllegalArgumentException("Null headers passed to VcfHeaderUtils.mergeHeaders");
		 else if (null == original)  return additional;
		 else if (null == additional)  return original;
		 		
		// only merging format, filter, info and meta		
		for (VcfHeaderRecord rec : additional.getInfoRecords())  
			original.addOrReplace(rec.toString(), overwrite);
		 
		for (VcfHeaderRecord rec : additional.getFormatRecords())  
			original.addOrReplace(rec.toString(), overwrite);
		 
		for (VcfHeaderRecord rec : additional.getFilterRecords())  
			original.addOrReplace(rec.toString(), overwrite);
		 
		for (VcfHeaderRecord rec : additional.getAllMetaRecords())  
			original.addOrReplace(rec.toString(), overwrite);
		 
		return original;
	}
	
	
	/**
	 * Splits a VcfHeaderRecord meta object on '=' and returns a String array with the results of the split command.
	 * If the record does not contain '=' then a 2 element array is returned, with the first element containing record.toString(), and the second element containing null
	 * @param <T>
	 * @param rec VcfHeaderRecord
	 * @return String [] containing the result of rec.toString().split("=")
	 */
//	public static String[]  splitMetaRecord(VcfHeaderRecord rec) {
//		if (null == rec ) 
//			throw new IllegalArgumentException("Null record passed to VcfHeaderUtils.splitMetaRecord");
//		 		
//		return  (rec.getMetaValue() == null)? new String[]{ rec.getMetaKey(), null } : new String[]{rec.getMetaKey(), rec.getMetaValue()};
//		
////		int index = rec.toString().indexOf(Constants.EQ_STRING);
////		if (index >= 0) {
////			return  rec.toString().split(Constants.EQ_STRING);
////		}
////		
////		return new String[] {rec.toString(), null};
//	}
	
//	public static class SplitMetaRecord{
//		String[] pair; 
//		public SplitMetaRecord(Record record){
//			this.pair =  splitMetaRecord(record);
//		}
//		
//		public String getKey(){ return pair[0]; }
//		public String getValue(){ return pair[1]; }
//	}
		
	public static <T> VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName, Class<T> mainClass) throws IOException {	
		DateFormat df = new SimpleDateFormat( "yyyyMMdd" ); 
 		
		String version = mainClass.getPackage().getImplementationVersion();
		String pg = mainClass.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);	
			
		String inputUuid = (header.getUUID() == null)? null: header.getUUID().getMetaValue();   
		header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING_UPPER_CASE;
	    if(pg == null ) pg = Constants.NULL_STRING_UPPER_CASE;
	    if(cmd == null) cmd = Constants.NULL_STRING_UPPER_CASE;
		VcfHeaderUtils.addQPGLineToHeader(header, pg, version, cmd);
		
		return header;			
	}
	
	public static boolean containsQIMRDetails(VcfHeader header) {  
		return  (header.getUUID() != null && ( !getqPGRecords(header).isEmpty()) );
	}		
	
	/**
	 * append a new pg line if not exist. otherwise replace the exsiting PG line with same order
	 * @param i: ith pg line
	 * @param tool : tool name
	 * @param version : tool version
	 * @param commandLine: command line
	 * @param date: date to run the tool
	 */
	public static void addQPGLine(VcfHeader header, int i, String tool, String version, String commandLine, String date) { 
		String line = VcfHeaderUtils.HEADER_LINE_QPG + "=<ID=" + i + Constants.COMMA + TOOL + Constants.EQ + tool +				
				Constants.COMMA + VERSION + Constants.EQ + version + Constants.COMMA + DATE + Constants.EQ + date +
				Constants.COMMA + COMMAND_LINE + Constants.EQ + VcfHeaderRecord.parseDescription(commandLine) + ">";		
		header.addOrReplace(line);
	} 
	
	/**
	 * append a new pg line or replace the existing PG line with same order
	 * @param order: PG line order
	 * @param exec
	 */
	public static void addQPGLine(VcfHeader header, int order, QExec exec) { 
		String line = VcfHeaderUtils.HEADER_LINE_QPG + "=<ID=" + order + Constants.COMMA + TOOL + Constants.EQ + exec.getToolName().getValue() +				
		Constants.COMMA + VERSION + Constants.EQ + exec.getToolVersion().getValue() + Constants.COMMA + DATE + Constants.EQ + exec.getStartTime().getValue() +
		Constants.COMMA + COMMAND_LINE + Constants.EQ + VcfHeaderRecord.parseDescription(exec.getCommandLine().getValue() )+ ">"  ;
		header.addOrReplace(line);	
	} 	
	
	public static List<VcfHeaderRecord> getqPGRecords(VcfHeader header) {  		
		return header.getRecords(VcfHeaderUtils.HEADER_LINE_QPG).stream().map(x -> x).collect(Collectors.toList());		
	}
	
	public static VcfHeaderRecord getqPGRecord(VcfHeader header, String id){ 	return header.getIDRecord(VcfHeaderUtils.HEADER_LINE_QPG, id); }	
	public static int getQPGOrder( VcfHeaderRecord  qpg){ return Integer.parseInt(qpg.getId()); }
	public static String getQPGTool(VcfHeaderRecord  qpg){ return  qpg.getSubFieldValue(TOOL);}
	public static String getQPGDate(VcfHeaderRecord  qpg){ return  qpg.getSubFieldValue(DATE);}
	public static String getQPGVersion(VcfHeaderRecord  qpg){ return  qpg.getSubFieldValue(VERSION);}
	public static String getQPGCommandLine(VcfHeaderRecord  qpg){ return  qpg.getSubFieldValue(COMMAND_LINE);}
	public static final String HEADER_LINE_FILTER = "##FILTER";

	public static final String HEADER_LINE_INFO = "##INFO";

	public static final String HEADER_LINE_FORMAT = "##FORMAT";

	public static final String CURRENT_FILE_FORMAT = "##fileformat=VCFv4.3";

	public static final String STANDARD_FILE_FORMAT = "##fileformat";

	public static final String STANDARD_FILE_DATE = "##fileDate";

	public static final String STANDARD_SOURCE_LINE = "##qSource";

	public static final String STANDARD_UUID_LINE = "##qUUID";

	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
}
