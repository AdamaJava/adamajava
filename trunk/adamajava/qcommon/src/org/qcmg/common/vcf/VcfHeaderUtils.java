/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;


import org.qcmg.common.util.SnpUtils;


public class VcfHeaderUtils {
	
	public static final String DESCRITPION_FILTER_GERMLINE="Mutation is a germline variant in another patient";
	public static final String DESCRITPION_INFO_CONFIDENCE="set to HIGH if more than 5 novel starts, 5 allels and passed all filter;"
			+ "otherwise set to LOW if 4 novel starts, 4 allels and passed one of filter of \"MIUN\" \"MIN\" or \"GERM\";" 			
			+ "set to ZERO for remaining mutations";
	
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

	
	//INFO FIELDS
	public static final String INFO_MUTANT_READS = FILTER_MUTANT_READS;
	public static final String INFO_NOVEL_STARTS = FILTER_NOVEL_STARTS;
	public static final String INFO_MUTATION = "MU";
	public static final String INFO_FLANKING_SEQUENCE = "FS";
	public static final String INFO_DONOR = "DON";	
	public static final String INFO_EFFECT = "EFF";
	public static final String INFO_LOSS_FUNCTION = "LOF";
	public static final String INFO_NONSENSE_MEDIATED_DECAY = "NMD";	
	public static final String INFO_SOMATIC = "SOMATIC";
	public static final String INFO_CONFIDENT = "CONF";
	
	//FORMAT FIELDS
	public static final String FORMAT_GENOTYPE = "GT";
	public static final String FORMAT_GENOTYPE_DETAILS = "GD";
	public static final String FORMAT_ALLELE_COUNT = "AC";
	
	//Header lines
	public static final String CURRENT_FILE_VERSION = "##fileformat=VCFv4.2";
	public static final String STANDARD_FILE_VERSION = "##fileformat"; 
	public static final String STANDARD_FILE_DATE = "##fileDate";
	public static final String STANDARD_SOURCE_LINE = "##source";
	public static final String STANDARD_UUID_LINE = "##uuid";
	public static final String PREVIOUS_UUID_LINE = "##preUuid";
	public static final String STANDARD_DBSNP_LINE = "##dbSNP_BUILD_ID";
	public static final String STANDARD_INPUT_LINE = "##INPUT";
	public static final String HEADER_LINE_FILTER = "##FILTER";
	public static final String HEADER_LINE_INFO = "##INFO";
	public static final String HEADER_LINE_FORMAT = "##FORMAT";	
	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	public static final String PATIENT_ID = "##patient_id";

	public static int parseIntSafe(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * first four keys are required, source and version are recommended
	 * This method only for gatk vcf, not for standard vcf, eg.
	 * eg. ##INFO=<ID=ID,Number=number,Type=type,Description="description",Source="source",Version="version">
	 * return <ID,"description">
	 * @param header VCF header line
	 * @return map of vcf Information field 
	 * 
	 */
/*
	public static Map<String, Integer> getMapFromInfoHeader(VCFHeader header) {
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		
		for (String s : header) {
			// check that is an INFO line
			if (s.startsWith(HEADER_LINE_INFO)) {
				// tokenise on "="
				String [] params = TabTokenizer.tokenize(s, '=');
				// id should be #2, value #5
				String id = params[2];
				String file = params[5];
				
				id = id.substring(0, id.indexOf(","));
				file = file.substring(1, file.length() - 2);
				
				map.put(file, Integer.valueOf(id));
			}
		}
		
		return map;
	}
	
	
	public static int[] getIdsForPatient(Map<String, Integer> mapOfFiles, String patient) {
		
		List<Integer> ids = new ArrayList<Integer>();
		for (Entry<String, Integer> entry : mapOfFiles.entrySet()) {
			if (entry.getKey().contains(patient)) {
				ids.add(entry.getValue());
			}
		}
		
		if ( ! ids.isEmpty()) {
			int j = 0;
			int [] idArray = new int[ids.size()];
			for (Integer i : ids) idArray[j++] = i;
			return idArray;
		}
		
		return null;
	}
*/
}
