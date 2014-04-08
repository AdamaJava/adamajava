/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.util;

public class QSVConstants {
	
	//General options	
	public static final String DEFAULT_LOGLEVEL = "INFO";
	
	public static final String DEFAULT_SV_ANALYSIS = "both";
	
	public static final int DEFAULT_MIN_INSERT_SIZE = 50;
	
	public static String DEFAULT_SEQ_PLATFORM = "illumina";
	
	//Pair options	
	public static final String DEFAULT_PAIRING_TYPE = "pe";
	
	public static final String DEFAULT_MAPPER = "bwa";
	
	public static final String DEFAULT_LMP_QUERY = "";
	
	public static final String DEFAULT_PE_QUERY = "";
	
	public static final int DEFAULT_CLUSTER_SIZE = 3;
	
	public static final int DEFAULT_FILTER_SIZE = 1;
	
	//Clip options
	public static final String DEFAULT_CLIP_QUERY = "and (Cigar_M > 34, MD_mismatch < 3, MapQ > 0, flag_DuplicateRead == false)";
	
	public static final int DEFAULT_CLIP_SIZE = 3;
	
	public static final int DEFAULT_CONSENSUS_LENGTH = 20;
	
	
	public static final String LEVEL_HIGH = "1";
	
	public static final String LEVEL_MID = "2";
	
	public static final String LEVEL_LOW = "3";
	
	public static final String LEVEL_GERMLINE = "4";
	
	public static final String LEVEL_REPEAT = "5";

	public static final String LEVEL_SINGLE_CLIP = "6";
	
	public static final String DISEASE_SAMPLE = "test";
	
	public static final String CONTROL_SAMPLE = "control";

	public static final String SPLIT_READ = "*";

	public static final int PAIR_CHR_RANGE = 100;
	
	public static final int CLIP_CHR_RANGE = 10;

	public static final String UNTESTED = "not tested";
	
	public static final String ORIENTATION_1 = "1";
	
	public static final String ORIENTATION_2 = "2";
	
	public static final String ORIENTATION_3 = "3";
	
	public static final String ORIENTATION_4 = "4";

	public static final String ORIENTATION_5 = "5";

	public static final String NOT_FOUND = "not found";
	
	public static final String DEL = "DEL/ITX";
	
	public static final String DUP = "DUP/INS/ITX";
	
	public static final String CTX = "CTX";
	
	public static final String INV = "INV/ITX";
	
}
