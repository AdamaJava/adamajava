package org.qcmg.qvisualise.util;

public class QProfilerXmlUtils {
	
	public static final String EMPTY = "EMPTY";
	public static final String TOTAL = "Total";
	public static final String UNKNOWN_READGROUP = "unkown_readgroup_id";
	
	//summary
	public static final String readGroup = "readGroup";
	public static final String summary = "bamSummary";
	public static final String reads = "reads"; 
	public static final String readPairs= "readPairs"; 
	public static final String FirstOfPair = "firstReadInPair"; 
	public static final String SecondOfPair = "secondReadInPair";
	public static final String mdCycle = "mdMismatchCycles";	
	public static final String discardReads = "discardedReads";
	public static final String fileReads = "fileReads";	
	public static final String duplicateReads = "duplicateReads";
	public static final String unmappedReads = 	"unmappedReads";
	public static final String nonCanonicalPair = "nonCanonicalPairs";
	public static final String trimmedBase = "trimmedBase";
	public static final String softClippedBases = "softClippedBases";
	public static final String hardClippedBases = "hardClippedBases";
	public static final String overlapBases = "overlappedBases";
	
 
	// tag name for old xml
	public static final String valueTally = "ValueTally";
	public static final String rangeTally = "RangeTally";
	public static final String cycleTally = "CycleTally";
	public static final String tallyItem = "TallyItem";
	public static final String rangeTallyItem = "RangeTallyItem";
	
	//count	
	public static final String source = "source";
	public static final String totalCount = "totalCount";
	public static final String totalBase = "totalBases";
	public static final String counts = "counts";
	public static final String count = "count";	
	public static final String readCount = "readCount";
	public static final String value = "value";
	public static final String percent = "percent";
	public static final String possibles = "possibleValues";
	public static final String start = "start";
	public static final String end = "end";	

	//commly used on fastq bam
	public static final String qname = "QNAME";
	public static final String flag = "FLAG";
	public static final String rname = "RNAME";
	public static final String pos = "POS";
	public static final String mapq = "MAPQ";
	public static final String cigar = "CIGAR";
	public static final String tlen = "TLEN";
	public static final String seq = "SEQ"; 
	public static final String tag = "TAG";
	public static final String cycle = "Cycle";	
	public static final String seqBase = "seqBase";
	public static final String seqLength = "seqLength";
	public static final String badBase = "badBase";
	public static final String qual = "QUAL";
	public static final String qualBase = "qualBase";
	public static final String qualLength = "qualLength";
	public static final String overall = "overall"; //overall information for reads 

}
