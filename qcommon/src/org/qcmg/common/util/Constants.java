/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.util;

import java.nio.file.FileSystems;

public class Constants {
	public  static final char TAB = '\t';
	public  static final String TAB_STRING = "\t";
	public static final char SC = ';';
	public static final char BAR = '|';
	public static final char SLASH = '/';
	public static final String NEW_LINE = System.lineSeparator();
	public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
	
	public static final char NL = '\n';
	public static final char MISSING_DATA = '.';
	public static final String MISSING_DATA_STRING = MISSING_DATA + "";
	public static final char NULL_CHAR = '\u0000';
	public static final char SEMI_COLON = SC;
	public static final char COLON = ':';
	public static final char COMMA = ',';
	public static final String COMMA_STRING = COMMA + "";
	public static final char EQ = '=';
	public static final String EQ_STRING = EQ + "";
	public static final char HASH = '#';
	public static final String HASH_STRING = HASH + "";
	public static final String DOUBLE_HASH = "##";
	public static final String MUT_DELIM = ">";
	public static final String SEMI_COLON_STRING = SC+"";
	public static final String COLON_STRING = COLON + "";
	public static final String NL_STRING = NL + "";
	public static final String BAR_STRING = "\\|";
	public static final String SLASH_STRING = "/";
	public  static final char MINUS = '-';
	public  static final char OPEN_PARENTHESES = '(';
	public  static final char CLOSE_PARENTHESES = ')';
	public  static final char OPEN_SQUARE_BRACKET = '[';
	public  static final char CLOSE_SQUARE_BRACKET = ']';
	
	//at moment only for VcfInfoFieldRecord
	public static final String NULL_STRING_UPPER_CASE = "NULL";
	public static final String NULL_STRING = "null";
	public static final String EMPTY_STRING = "";
	public static final String CHR = "chr";
	public static final String MISSING_GT = "./.";
	
	/*
	 * Number of hours that an ExecutorService will wait for before terminating
	 * Set to 100 initially
	 */
	public static final int EXECUTOR_SERVICE_AWAIT_TERMINATION = 100;
	
	
	// SAM Header Prefixes
	public static final String HEADER_PREFIX = "@HD";
	public static final String SEQUENCE_PREFIX = "@SQ";
	public static final String READ_GROUP_PREFIX = "@RG";
	public static final String PROGRAM_PREFIX = "@PG";
	public static final String COMMENT_PREFIX = "@CO";
	public static final String COMMENT_Q3BAM_UUID_PREFIX = "@CO\tq3BamUUID";
	
	//  used as delimiter when merging vcf records for format field
	public static final char VCF_MERGE_DELIM = '&';
	//  used as delimiter when merging vcf records for format field
	public static final String VCF_MERGE_INFO = "IN";
	public static final String HIGH_1_HIGH_2 = "HIGH,HIGH";
}
