/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

public class PileupConstants {
	
	public static final int READ_LENGTH = 50;	
	
	public static final String MODE_BOOTSTRAP = "bootstrap";
	
	public static final String MODE_MERGE = "merge";
	
	public static final String MODE_ADD = "add";

	public static final String GENOME_BAM = "genome";

	public static final String EXOME_BAM = "exome";

	public static final String RNA_BAM = "mRNA";

	public static final String UNKNOWN_BAM = "unknown";

	public static final String MODE_READ = "read";

	public static final String BASES = "bases";

	public static final String BASE_QUAL = "base_qual";

	public static final String CIGAR = "cigar";
	
	public static final String READ_STAT = "read_stat";

	public static final String ALL = "all";
	
	//public static final long totalBases = 3000000000L;

	public static final String DELIMITER = "\t";

	public static final String COMMA_DELIMITER = ",";

	public static final String TAB_DELIMITER = "\t";
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	public static final String METRIC_SNP = "snp";
	
	public static final String METRIC_CLIP = "clip";

	public static final String METRIC_UNMAPPED_MATE = "unmapped_mate";

	public static final String NEWLINE = System.getProperty("line.separator");

	public static final String DEL = "DEL";

	public static final String INS = "INS";	

	public static String METRIC_INDEL = "indel";
	
	public static String METRIC_NONREFBASE = "nonreference_base";

	public static String METRIC_MAPPING = "mapping_qual";
	
	public static String METRIC_STRAND_BIAS = "strand_bias";
	
	public static String METRIC_HCOV = "high_coverage";
	
	//public static String[] METRIC_LIST = {METRIC_SNP, METRIC_CLIP, METRIC_INDEL, METRIC_NONREFBASE, METRIC_UNMAPPED_MATE, METRIC_MAPPING, METRIC_STRAND_BIAS, METRIC_HCOV};

	public static final String LOW_MAPPING_QUAL = "LowMappingQual";
	
	public static String IRREGULAR = "Irregular";
	
	public static String REGULAR = "Regular";
	
	public static String MIXED = "Mixed";
	
	public static final String HCOV = "HighCoverage";
	
	public static final String MIXED_AND_LOWMAPING = LOW_MAPPING_QUAL + MIXED;

	public static final String MIXED_AND_REGULAR = REGULAR + MIXED;
	
	public static final String MIXED_AND_IRREGULAR = IRREGULAR + MIXED;
	
	public static final String MIXED_AND_HCOV = HCOV + MIXED;	

	public static double MIN_NONREF_REGULAR_PERCENT = 85; 
	
	public static double MIN_INDEL_REGULAR_PERCENT = 85; 
	
	public static double MIN_CLIP_REGULAR_PERCENT = 35;

	public static int WINDOW_SIZE = 30; 
	
	public static int STEP_SIZE = 10;

	public static String TOTAL_BASE = "total_base";

	public static double SBIAS_MIN = 80; 
	
}
