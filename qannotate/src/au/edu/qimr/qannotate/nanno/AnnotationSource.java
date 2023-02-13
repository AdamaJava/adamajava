package au.edu.qimr.qannotate.nanno;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.RecordReader;

public abstract class AnnotationSource implements Closeable {
	
	public static final String FIELD_DELIMITER_EQ = "=";
	public static final char DEFAULT_DELIMITER = '\t';
	public static final String FIELD_DELIMITER_TAB = "\t";
	public static final Comparator<String> COMP = ChrPositionComparator.getStringComparatorForHG38();
	
	static final QLogger logger = QLoggerFactory.getLogger(AnnotationSource.class);
	
	List<String> currentRecords;
	List<String> nextRecords;
	ChrPosition currentCP;
	ChrPosition nextCP;
	
	protected final RecordReader<String> reader;
	protected final Iterator<String> iter;
	protected final int chrPositionInRecord;
	protected final int positionPositionInRecord;
	protected final int refPositionInFile;
	protected final int altPositionInFile;
	
	
	public AnnotationSource(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord,
			int refPositionInFile, int altPositionInFile) {
		super();
		this.reader = reader;
		this.iter = reader.iterator();
		this.chrPositionInRecord = chrPositionInRecord - 1;
		this.positionPositionInRecord = positionPositionInRecord - 1;
		this.refPositionInFile = refPositionInFile - 1;
		this.altPositionInFile = altPositionInFile - 1;
	}


	public abstract String annotationToReturn(String record);
	
	public static String getEmptyRecordReturnValue(String fieldNames) {
		return Arrays.stream(fieldNames.split(",")).map(s -> s + "=").collect(Collectors.joining(FIELD_DELIMITER_TAB));
	}
	
	public String getAnnotation(ChrPosition requestedCp) {
		
    	logger.debug(reader.getFile().getName() + ":  requestedCp is " + (null != requestedCp ? requestedCp.toIGVString() : null) + ", currentCP: " + (null != currentCP ? currentCP.toIGVString() : null) + ", nextCP: " + (null != nextCP ? nextCP.toIGVString() : null));
		
		
		/*
		 * check to see if the records we currently have stored are a match
		 */
		if ( areCPsEqual(requestedCp, currentCP) == 0) {
			
			/*
			 * we match on position
			 * lets see if there are any records that match on ref and alt
			 */
			for (String rec : currentRecords) {
				String [] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
				String recRef = recArray[refPositionInFile];
				String recAlt = recArray[altPositionInFile];
				
				if (((ChrPositionRefAlt)requestedCp).getName().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAlt)) {
					return annotationToReturn(rec);
				}
			}
			
		} else if (null != nextCP && areCPsEqual(requestedCp, nextCP) < 0) {
			/*
			 * requestedCp is "less than" next CP
			 * return empty list here
			 */
		} else {
			logger.debug(reader.getFile().getName() + ": getting next record. requestedCp: " + (null != requestedCp ? requestedCp.toIGVString() : null) + ", currentCP: " + (null != currentCP ? currentCP.toIGVString() : null));
			getNextRecord(requestedCp);
			if ( areCPsEqual(requestedCp, currentCP) == 0) {
				/*
				 * we match on position
				 * lets see if there are any records that match on ref and alt
				 */
				for (String rec : currentRecords) {
					String [] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
					String recRef = recArray[refPositionInFile];
					String recAlt = recArray[altPositionInFile];
					
					if (((ChrPositionRefAlt)requestedCp).getName().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAlt)) {
						return annotationToReturn(rec);
					}
				}
			} else {
				/*
				 * requestedCP and currentCP are not equal
				 */
			}
		}
		return annotationToReturn(null);
	}
	
	private void getNextRecord(ChrPosition requestedCp) {
		currentRecords = new ArrayList<>();
		
		/*
		 * check to see if the nextCP meets our criteria
		 * if it does, set currentCP to be next CP
		 * we still need to iterate through the file to see if we have more than this entry
		 */
		int nextCPmatch = areCPsEqual(requestedCp, nextCP);
		if (nextCPmatch == 0) {
			currentCP = nextCP;
			currentRecords = nextRecords;
		}
		
		
		while (iter.hasNext()) {
			String nextRecord = iter.next();
			/*
			 * check to see if this record is the one we want
			 */
			String[] nextRecordArray = TabTokenizer.partialTokenize(nextRecord, DEFAULT_DELIMITER, Math.max(chrPositionInRecord, positionPositionInRecord) + 1);
			
			int match = isThisOurRecord(requestedCp, nextRecordArray, chrPositionInRecord, positionPositionInRecord);
			
			if (match == 0) {
				/*
				 * got a match!
				 * we could have more than 1 entry for each position
				 */
				currentCP = getCpFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord);
				currentRecords.add(nextRecord);
				
			} else if (match < 0) {
				/*
				 * we have overshot - set nextCP and break out
				 */
				nextCP = getCpFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord);
				nextRecords = new ArrayList<>();
				nextRecords.add(nextRecord);
				break;
		    } else {
				
				/*
				 * keep going
				 */
			}
			
		}
	}
	
	public static ChrPosition getCpFromRecord(String[] rec, int chrPositionInRecord, int positionPositionInRecord) {
		if (null == rec || rec.length == 0) {
			throw new IllegalArgumentException("String array rec is null or empty");
		}
		if (Math.max(chrPositionInRecord, positionPositionInRecord) >= rec.length) {
			throw new IllegalArgumentException("String array rec is of length: " + rec.length + ", and Math.max(chrPositionInRecord, positionPositionInRecord): " + Math.max(chrPositionInRecord, positionPositionInRecord));
		}
		return new ChrPointPosition(rec[chrPositionInRecord], Integer.parseInt(rec[positionPositionInRecord]));
	}
	
	/*
	 * 1 based numbering
	 * much like a compare method, this will return 0 if the requestedCp is the same as the rec,
	 * 1 if the requestedCp is upstream of the rec
	 * -1 if the requestedCp is downstream of the rec
	 */
	public static int isThisOurRecord(ChrPosition requestedCp, String[] recArray, int chrPositionInRecord, int positionPositionInRecord) {
		
		return isThisOurRecord(requestedCp, recArray[chrPositionInRecord], Integer.parseInt(recArray[positionPositionInRecord]));
	}
	
	public static int isThisOurRecord(ChrPosition requestedCp, String recordChr, int recordPosition) {
		if (null == requestedCp) {
			return 1;
		}
		return compareChromosomeNameAndStartPositions(requestedCp.getChromosome(), requestedCp.getStartPosition(), recordChr, recordPosition);
	}
	
	/**
	 * 
	 * Compares chromosome names and positions
	 */
	public static int compareChromosomeNameAndStartPositions(String chr1, int position1, String chr2, int position2) {
		if (null == chr1) {
			return 1;
		}
		if (null == chr2) {
			return -1;
		}
		boolean chr1StartsWithChr = chr1.startsWith("chr");
		boolean chr2StartsWithChr = chr2.startsWith("chr");
		int diff = COMP.compare((chr1StartsWithChr ? chr1.substring(3) : chr1), (chr2StartsWithChr ? chr2.substring(3) : chr2));
		if (diff != 0) {
			return diff;
		}
		/*
		 * check position now
		 */
		return Integer.compare(position1, position2);
	}
	
	
	/**
	 * 
	 * THis is effectively comparing the 2 supplied ChrPositions.
	 * If the first cp is null, 1 is returned.
	 * If the second cp is null, -1 is returned
	 * 
	 * NOTE that if both cps supplied are null, 1 is returned (due to the first cp being null)!
	 * 
	 * If they are both non-null, the contig names and start and end positions are compared
	 * 
	 * 
	 * @param cp1
	 * @param cp2
	 * @return
	 */
	public static int areCPsEqual(ChrPosition cp1 ,ChrPosition cp2) {
		if (null == cp1) {
			return 1;
		}
		if (null == cp2) {
			return -1;
		}
		
		int nameAndStartPosisionMatch = compareChromosomeNameAndStartPositions(cp1.getChromosome(), cp1.getStartPosition(), cp2.getChromosome(), cp2.getStartPosition());
		if (nameAndStartPosisionMatch != 0) {
			return nameAndStartPosisionMatch;
		}
		
		return Integer.compare(cp1.getEndPosition(), cp2.getEndPosition());
	}

}
