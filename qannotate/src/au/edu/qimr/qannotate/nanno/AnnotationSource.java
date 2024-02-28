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
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.RecordReader;

public abstract class AnnotationSource implements Closeable {

    public static final String FIELD_DELIMITER_EQ = "=";
    public static final char DEFAULT_DELIMITER = '\t';
    public static final String FIELD_DELIMITER_TAB = "\t";
    public static final Comparator<String> COMP = ChrPositionComparator.getChrNameComparatorNoChrsOneToM();

    static final QLogger logger = QLoggerFactory.getLogger(AnnotationSource.class);

    List<String> currentRecords;
    List<String> nextRecords;
    long currentCPAsLong;
    long nextCPAsLong;


    protected final RecordReader<String> reader;
    protected final Iterator<String> iter;
    protected final int chrPositionInRecord;
    protected final int positionPositionInRecord;
    protected final int refPositionInFile;
    protected final int altPositionInFile;
    protected final boolean canUseStartsWith;

    protected final boolean chrStartsWithChr;


    public AnnotationSource(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord, int refPositionInFile, int altPositionInFile, boolean chrStartsWithChr) {
        super();
        this.reader = reader;
        this.iter = reader.iterator();
        this.chrPositionInRecord = chrPositionInRecord - 1;
        this.positionPositionInRecord = positionPositionInRecord - 1;
        this.refPositionInFile = refPositionInFile - 1;
        this.altPositionInFile = altPositionInFile - 1;
        this.canUseStartsWith = this.chrPositionInRecord == 0 && this.positionPositionInRecord == 1;
        this.chrStartsWithChr = chrStartsWithChr;
    }


    public abstract String annotationToReturn(String[] record);

    public static String getEmptyRecordReturnValue(String fieldNames) {
        return Arrays.stream(fieldNames.split(",")).map(s -> s + FIELD_DELIMITER_EQ).collect(Collectors.joining(FIELD_DELIMITER_TAB));
    }

    public String getAnnotation(long requestedCpAsLong, ChrPosition requestedCp) {

        logger.debug(reader.getFile().getName() + ":  requestedCp is " + requestedCpAsLong + ", currentCP: " + currentCPAsLong + ", nextCP: " + nextCPAsLong);


        /*
         * check to see if the records we currently have stored are a match
         */
        if (requestedCpAsLong == currentCPAsLong) {

            /*
             * we match on position
             * lets see if there are any records that match on ref and alt
             */
            return getAnnotationsFromCurrentRecords(requestedCp);

        } else {
            int matchWithNextCP = Long.compare(requestedCpAsLong, nextCPAsLong);
            if (nextCPAsLong > -1 && matchWithNextCP < 0) {
                /*
                 * requestedCp is "less than" next CP
                 * return empty list here
                 */
            } else {
//                logger.debug(reader.getFile().getName() + ": getting next record. requestedCp: " + (null != requestedCp ? requestedCp.toIGVString() : null) + ", currentCP: " + (null != currentCP ? currentCP.toIGVString() : null));
                getNextRecord(requestedCpAsLong, matchWithNextCP);
                if (requestedCpAsLong == currentCPAsLong) {
                    return getAnnotationsFromCurrentRecords(requestedCp);
                }
                /*
                 * requestedCP and currentCP are not equal
                 */
            }
        }
        return annotationToReturn(null);
    }

    private String getAnnotationsFromCurrentRecords(ChrPosition requestedCp) {
        if (requestedCp instanceof ChrPositionRefAlt reqCpRefAlt) {
            String reqRef = reqCpRefAlt.getRef();
            String reqAlt = reqCpRefAlt.getAlt();

            for (String rec : currentRecords) {
                String[] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
                String recRef = recArray[refPositionInFile];
                String recAlt = recArray[altPositionInFile];

                if (reqRef.equals(recRef) && reqAlt.equals(recAlt)) {
                    return annotationToReturn(recArray);
                }
            }
        }
        return annotationToReturn(null);
    }

    void getNextRecord(long requestedCpAsLong, int matchWithNextCP) {
//    void getNextRecord(ChrPosition requestedCp, int matchWithNextCP) {
        currentRecords = new ArrayList<>(4);

        /*
         * check to see if the nextCP meets our criteria
         * if it does, set currentCP to be next CP
         * we still need to iterate through the file to see if we have more than this entry
         */
        if (matchWithNextCP == 0) {
            currentCPAsLong = nextCPAsLong;
            currentRecords = nextRecords;
        }

//        String startsWithString = (chrStartsWithChr ? requestedCp.getChromosome() : requestedCp.getChromosome().substring(3));
        while (iter.hasNext()) {
            /*
             * check to see if this record is the one we want
             */
            String nextRecord = iter.next();
            int match;
            String[] nextRecordArray = null;
            if (canUseStartsWith) {
                match = isThisOurRecordShortcut(requestedCpAsLong, nextRecord, chrStartsWithChr);
            } else {
                nextRecordArray = TabTokenizer.partialTokenize(nextRecord, DEFAULT_DELIMITER, Math.max(chrPositionInRecord, positionPositionInRecord) + 1);
                match = isThisOurRecord(requestedCpAsLong, nextRecordArray, chrPositionInRecord, positionPositionInRecord, chrStartsWithChr);
            }
            if (match == 0) {
                /*
                 * got a match!
                 * we could have more than 1 entry for each position
                 */
                currentCPAsLong = getChrPositionAsLongFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord, nextRecord, chrStartsWithChr);
                currentRecords.add(nextRecord);

            } else if (match < 0) {
                /*
                 * we have overshot - set nextCP and break out
                 */
                nextCPAsLong = getChrPositionAsLongFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord, nextRecord, chrStartsWithChr);
                nextRecords = new ArrayList<>();
                nextRecords.add(nextRecord);
                break;
            }
            /*
             * no match yet - keep going
             */
        }
    }

    public static ChrPosition getChrPositionFromRecord(String[] nextRecordArray, int chrPositionInRecord, int positionPositionInRecord, String nextRecord) {
        if (null == nextRecordArray) {
            int firstTabIndex = nextRecord.indexOf(DEFAULT_DELIMITER);
            int secondTabIndex = nextRecord.indexOf(DEFAULT_DELIMITER, firstTabIndex + 1);
            return new ChrPointPosition(nextRecord.substring(0, firstTabIndex), Integer.parseInt(nextRecord.substring(firstTabIndex + 1, secondTabIndex)));
        } else {
            return getCpFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord);
        }
    }

    public static long getChrPositionAsLongFromRecord(String[] nextRecordArray, int chrPositionInRecord, int positionPositionInRecord, String nextRecord, boolean chrStartsWithChr) {
        if (null == nextRecordArray) {
            int firstTabIndex = nextRecord.indexOf(DEFAULT_DELIMITER);
            int secondTabIndex = nextRecord.indexOf(DEFAULT_DELIMITER, firstTabIndex + 1);
            String contig = chrStartsWithChr ? nextRecord.substring(3, firstTabIndex) : nextRecord.substring(0, firstTabIndex);
            int position = Integer.parseInt(nextRecord, firstTabIndex + 1, secondTabIndex, 10);
            return ChrPositionUtils.convertContigAndPositionToLong(contig, position);
        } else {
            return getCpAsLongFromRecord(nextRecordArray, chrPositionInRecord, positionPositionInRecord, chrStartsWithChr);
        }
    }

    /**
     * Determines if the given record is considered our record by comparing the requested chromosome position (cp) with the record's cp.
     * The method compares the chromosome and start positions between the requested cp and the record's cp.
     * The cp is represented as a long value, where the upper 32 bits represent the chromosome and the lower 32 bits represent the position.
     * If the record's cp matches the requested cp or is downstream of the requested cp, 1 is returned.
     * If the record's cp is upstream of the requested cp, -1 is returned.
     * If the requested cp is -1, indicating a wildcard, 1 is returned.
     *
     * @param requestedCpAsLong The requested chromosome position as a long value.
     * @param recordLine        The record line to compare.
     * @param chrStartsWithChr  Indicates if the chromosome name in the record starts with "chr".
     * @return 1 if the record is our record or downstream of the requested cp, -1 if it is upstream, 0 otherwise.
     */
    public static int isThisOurRecordShortcut(long requestedCpAsLong, String recordLine, boolean chrStartsWithChr) {
        if (requestedCpAsLong == -1) {
            return 1;
        }
        int firstTabIndex = recordLine.indexOf(DEFAULT_DELIMITER);
        int recordChrInt = ChrPositionUtils.convertContigNameToInt(recordLine.substring(chrStartsWithChr ? 3 : 0, firstTabIndex));

        if (recordChrInt == requestedCpAsLong >>> 32) {
            // same chromosome, examine the position
            int position = Integer.parseInt(recordLine, firstTabIndex + 1, recordLine.indexOf(DEFAULT_DELIMITER, firstTabIndex + 1), 10);
            return Integer.compare((int) (requestedCpAsLong & 0x00000000FFFFFFFFL), position);
        } else {
            // examine the chromosome only
            return Integer.compare((int) (requestedCpAsLong >>> 32), recordChrInt);
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

    public static long getCpAsLongFromRecord(String[] rec, int chrPositionInRecord, int positionPositionInRecord, boolean chrStartsWithChr) {
        if (null == rec || rec.length == 0) {
            throw new IllegalArgumentException("String array rec is null or empty");
        }
        if (Math.max(chrPositionInRecord, positionPositionInRecord) >= rec.length) {
            throw new IllegalArgumentException("String array rec is of length: " + rec.length + ", and Math.max(chrPositionInRecord, positionPositionInRecord): " + Math.max(chrPositionInRecord, positionPositionInRecord));
        }

        String recordChr = chrStartsWithChr ? rec[chrPositionInRecord].substring(3) : rec[chrPositionInRecord];
        int position = Integer.parseInt(rec[positionPositionInRecord]);
        return ChrPositionUtils.convertContigAndPositionToLong(recordChr, position);
    }

    /*
     * 1 based numbering
     * much like a compare method, this will return 0 if the requestedCp is the same as the rec,
     * 1 if the requestedCp is upstream of the rec
     * -1 if the requestedCp is downstream of the rec
     */
    public static int isThisOurRecord(long requestedCpAsLong, String[] recArray, int chrPositionInRecord, int positionPositionInRecord, boolean chrStartsWithChr) {

        return isThisOurRecord(requestedCpAsLong, recArray[chrPositionInRecord], Integer.parseInt(recArray[positionPositionInRecord]), chrStartsWithChr);
    }

    public static int isThisOurRecord(long requestedCpAsLong, String recordChr, int recordPosition, boolean chrStartsWithChr) {
        if (requestedCpAsLong == -1) {
            return 1;
        }
        long recordAsLong = ChrPositionUtils.convertContigAndPositionToLong(chrStartsWithChr ? recordChr.substring(3) : recordChr, recordPosition);
        return Long.compare(requestedCpAsLong, recordAsLong);
    }

    /**
     * Compares the chromosome name and start positions of two variants.
     *
     * @param chr1      The chromosome name of the first variant.
     * @param position1 The start position of the first variant.
     * @param chr2      The chromosome name of the second variant.
     * @param position2 The start position of the second variant.
     * @return 1 if the first variant is greater, -1 if the second variant is greater, 0 if they are equal.
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
     * Compares two ChrPosition objects and determines if they are equal.
     *
     * @param cp1 The first ChrPosition object to compare.
     * @param cp2 The second ChrPosition object to compare.
     * @return 0 if the two ChrPosition objects are equal, 1 if cp1 is greater than cp2 or cp2 is null, -1 if cp1 is smaller than cp2 or cp1 is null.
     */
    public static int compareCPs(ChrPosition cp1, ChrPosition cp2) {
        if (null == cp1) {
            return 1;
        }
        if (null == cp2) {
            return -1;
        }

        if (areChrPointPositionsEqual(cp1, cp2, true)) {
            return 0;
        }

        int nameAndStartPositionMatch = compareChromosomeNameAndStartPositions(cp1.getChromosome(), cp1.getStartPosition(), cp2.getChromosome(), cp2.getStartPosition());
        if (nameAndStartPositionMatch != 0) {
            return nameAndStartPositionMatch;
        }

        return Integer.compare(cp1.getEndPosition(), cp2.getEndPosition());
    }

    /**
     * Compares two ChrPosition objects and determines if their chromosome and start positions are equal.
     *
     * @param cp1              The first ChrPosition object to compare.
     * @param cp2              The second ChrPosition object to compare.
     * @param ignoreChromosome If true, removes 'chr' from chromosome name (if present) when comparing.
     * @return True if the chromosome and start positions are equal, otherwise false.
     */
    public static boolean areChrPointPositionsEqual(ChrPosition cp1, ChrPosition cp2, boolean ignoreChromosome) {
        if (cp1 == null || cp2 == null) {
            return false;
        }
        if (cp1.getStartPosition() == cp2.getStartPosition()) {
            if (cp1.getChromosome().equals(cp2.getChromosome())) {
                return true;
            }
            if (ignoreChromosome) {
                boolean cp1StartsWithChr = cp1.getChromosome().startsWith("chr");
                boolean cp2StartsWithChr = cp2.getChromosome().startsWith("chr");
                if ((cp1StartsWithChr && cp2StartsWithChr) || (!cp1StartsWithChr && !cp2StartsWithChr)) {
                    return false;
                } else {
                    return (cp1StartsWithChr ? cp1.getChromosome().substring(3) : cp1.getChromosome()).equals((cp2StartsWithChr ? cp2.getChromosome().substring(3) : cp2.getChromosome()));
                }
            }
        }
        return false;
    }

}
