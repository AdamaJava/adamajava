/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;


/**
 * Utility methods for qsv
 */
public class QSVUtil {
	
	public static final String NEW_LINE = System.lineSeparator();
	public static final char PLUS = '+';
	public static final char MINUS = '-';
	public static final char COLON = ':';
	
	public static ConcurrentMap<String, byte[]> REFERENCE;
	public static final ReferenceNameComparator REF_NAME_COMP =  new ReferenceNameComparator();
	
	private static final String ABA = "ABA";
	private static final String AAB = "AAB";
	private static final String ABC = "ABC";
	private static final String BAB = "BAB";
	private static final String BBA = "BBA";
	private static final String BBB = "BBB";
	private static final String BAA = "BAA";
	private static final String BAC = "BAC";
	private static final String BBC = "BBC";
	private static final String ABB = "ABB";
	private static final String AAC = "AAC";
	private static final String CTX = "CTX";

	/**
	 * Takes start and end date and return a string of the time between these two Date objects
	 *
	 * @param timeType the time type
	 * @param dateStart the date start
	 * @param dateEnd the date end
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String writeTime(String timeType, Date dateStart, Date dateEnd) throws IOException {
		long time = dateEnd.getTime() - dateStart.getTime();
        return timeType + secondsToString(time / 1000);
	}

	/**
	 * Converts seconds to hours:minutes:seconds string
	 *
	 * @param time the time
	 * @return the time string
	 */
	public static String secondsToString(long time) {
		int seconds = (int) (time % 60);
		int minutes = (int) ((time / 60) % 60);
		int hours = (int) ((time / 3600));
		String secondsStr = (seconds < 10 ? "0" : Constants.EMPTY_STRING) + seconds;
		String minutesStr = (minutes < 10 ? "0" : Constants.EMPTY_STRING) + minutes;
		String hoursStr = (hours < 10 ? "0" : Constants.EMPTY_STRING) + hours;
		return hoursStr + COLON + minutesStr + COLON + secondsStr;
	}

	/**
	 * Removes the directory.
	 *
	 * @param directory the directory
	 * @return true, if successful
	 */
	public static boolean removeDirectory(File directory) {

		if (directory == null)
			return false;
		if (!directory.exists())
			return false;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();
		if (null != list) {
            for (String s : list) {
                File entry = new File(directory, s);

                if (entry.isDirectory()) {
                    if (!removeDirectory(entry)) {
                        return false;
                    }
                } else {
                    if (!entry.delete()) {
                        return false;
                    }
                }
            }
		}
		return directory.delete();
	}

	/**
	 * Gets a strack trace and converted it to a string.
	 *
	 * @param e the exception
	 * @return the strack trace
	 */
	public static String getStrackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	/**
	 * Gets the strack trace.
	 *
	 * @param t the t
	 * @return the strack trace
	 */
	public static String getStrackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * Reorder by chromosomes.
	 *
	 * @param leftReference the left reference
	 * @param rightReference the right reference
	 * @return true, if successful
	 */
	public static boolean reorderByChromosomes(String leftReference, String rightReference) {
		return REF_NAME_COMP.compare(leftReference, rightReference) > 0;
	}

	/**
	 * Reverse complement a DNA sequence.
	 *
	 * @param consensus the consensus
	 * @return the reverse complement
	 */
	public static String reverseComplement(String consensus) {
		StringBuilder reverse = new StringBuilder();

		for (int i = consensus.length() - 1; i >= 0; i--) {
			char pos = consensus.charAt(i);
			if (pos == 'A') {
				reverse.append('T');
			} else if (pos == 'T') {
				reverse.append('A');
			} else if (pos == 'C') {
				reverse.append('G');
			} else if (pos == 'G') {
				reverse.append('C');
			} else {
				reverse.append('N');
			}
		}
		return reverse.toString();
	}

	/**
	 * Gets the pair group by zp.
	 *
	 * @param zpType the zp type
	 * @return the pair group by zp
	 */
	public static PairGroup getPairGroupByZP(PairClassification zpType) {
		String zp = zpType.toString();
        return switch (zp) {
            case BAA, BBA -> PairGroup.valueOf("BAA_BBA");
            case BAB, BBB -> PairGroup.valueOf("BAB_BBB");
            case BAC, BBC -> PairGroup.valueOf("BAC_BBC");
            default -> PairGroup.valueOf(zp);
        };
	}

	/**
	 * Gets the mutation type by pair group.
	 *
	 * @param pg the pg
	 * @return the mutation by pair group
	 */
	public static String getMutationByPairGroup(String pg) {
        return switch (pg) {
            case "Cxx" -> CTX;
            case AAC -> "DEL/ITX";
            case "BAC_BBC", "BAA_BBA", "BAB_BBB" -> "INV/ITX";
            case ABC, ABA, AAB, ABB -> "DUP/INS/ITX";
            default -> pg;
        };
	}

	/**
	 * Gets the mutation type by pair classification.
	 *
	 * @param pClass the class
	 * @return the mutation by pair classification
	 */
	public static String getMutationByPairClassification(PairClassification pClass) {
		String zp = pClass.toString();
        return switch (zp) {
            case "Cxx" -> CTX;
            case AAC -> "DEL/ITX";
            case BAC, BBC, BAA, BBA, BAB, BBB -> "INV/ITX";
            case ABC, ABA, ABB, AAB -> "DUP/INS/ITX";
            default -> zp;
        };
	}

	/**
	 * Gets the file separator.
	 *
	 * @return the file separator
	 */
	public static String getFileSeparator() {
		return FileSystems.getDefault().getSeparator();
	}

	/**
	 * Gets the pair query.
	 *
	 * @param pairingType the pairing type
	 * @param mapper the mapper
	 * @return the pair query
	 */
	public static String getPairQuery(String pairingType, String mapper) {
		String lifescopeQuery = "and(Cigar_M > 35, MD_mismatch < 3, MAPQ > 0, flag_DuplicateRead == false)";
		String lmpQuery = "and(Cigar_M > 35, option_SM > 14, MD_mismatch < 3, flag_DuplicateRead == false)";
		String peQuery = "and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)";  

		if (pairingType.equals("lmp")) {
			if (mapper.equals("lifescope")) {
				return lifescopeQuery; 
			} else {
				return  lmpQuery;
			}
		} else { 
			return peQuery;
		}		
	}

	/**
	 * Gets a separator for a new line.
	 *
	 * @return the new line
	 */
	public static String getNewLine() {
		return NEW_LINE;
	}

	/**
	 * Gets the category by pair group.
	 *
	 * @param zpGroup the zp group
	 * @return the category by pair group
	 */
	public static List<String> getCategoryByPairGroup(PairGroup zpGroup) {
		String zp = zpGroup.toString();
		List<String> list = new ArrayList<>();
        switch (zp) {
            case AAC -> list.add(QSVConstants.ORIENTATION_1);
            case "Cxx" -> {
                list.add(QSVConstants.ORIENTATION_1);
                list.add(QSVConstants.ORIENTATION_2);
                list.add(QSVConstants.ORIENTATION_3);
                list.add(QSVConstants.ORIENTATION_4);
            }
            case AAB, ABC, ABA, ABB -> list.add(QSVConstants.ORIENTATION_2);
            case "BAA_BBA", "BAB_BBB", "BAC_BBC" -> {
                list.add(QSVConstants.ORIENTATION_3);
                list.add(QSVConstants.ORIENTATION_4);
            }
        }

		return list;
	}		

	/**
	 * High n count.
	 *
	 * @param consensus the consensus
	 * @param limit the limit
	 * @return true, if successful
	 * @throws QSVException 
	 */
	public static boolean highNCount(String consensus, double limit) throws QSVException {
		if (StringUtils.isNullOrEmpty(consensus)) {
			throw new QSVException("NULL_OR_EMPTY_STRING","QSVUtil.highNCount", consensus);
		}
		if (limit < 0 || limit > 1) {
			throw new QSVException("INVALID_PERCENTAGE","QSVUtil.highNCount", "" + limit);
		}
		int count = 0;
		int len = consensus.length();
		for (int i = 0 ; i < len ; i ++) {
			if (consensus.charAt(i) == 'N' || consensus.charAt(i) == 'n') {
				count++;
			}
		}

		return  (double) count / len >= limit;
	}

	/**
	 * Gets the pair groups by orientation category.
	 *
	 * @param oc the oc
	 * @return the pair groups by orientation category
	 */
	public static Set<String> getPairGroupsByOrientationCategory(String oc) {
		Set<String> list = new HashSet<>();
        switch (oc) {
            case "1" -> list.add(AAC);
            case "2" -> {
                list.add(ABA);
                list.add(ABB);
                list.add(AAB);
                list.add(ABC);
            }
            case "3", "4" -> {
                list.add(BAA);
                list.add(BBA);
                list.add(BAB);
                list.add(BBB);
                list.add(BAC);
                list.add(BBC);
            }
        }

		return list;
	}

	/**
	 * Gets the analysis id.
	 *
	 * @param qcmg the qcmg
	 * @param sampleName the sample name
	 * @param analysisDate the analysis date
	 * @return the analysis id
	 */
	@Deprecated
	public static String getAnalysisId(boolean qcmg, String sampleName, Date analysisDate) {
		if (qcmg) {
			return QExec.createUUid();       	
		} else {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyddMM_HHmm");
			return "qSV_" + sampleName + "_" + formatter.format(analysisDate);
		}
	}
	
	
	public static ConcurrentMap<String, byte[]> getReferenceMap() {
		return REFERENCE;
	}
	
	public static synchronized void setupReferenceMap(File referenceFile) {
		if (null == REFERENCE) {
			REFERENCE = new ConcurrentHashMap<>();
			ReferenceSequenceFile f = getReferenceFile(referenceFile);
			if (f == null) {
				f = new FastaSequenceFile(referenceFile, true);
			}
			ReferenceSequence nextSeq = f.nextSequence();
			
			while (nextSeq != null) {
				if (null != nextSeq.getName() && null != nextSeq.getBases()) {
					REFERENCE.put(nextSeq.getName(), nextSeq.getBases());
				}
				nextSeq = f.nextSequence();
			}
		}
	}

	/**
	 * Gets the reference file.
	 *
	 * @param referenceFile the reference file
	 * @return the reference file
     */
	private static IndexedFastaSequenceFile getReferenceFile(File referenceFile) {

		if (referenceFile.exists()) {
			File indexFile = new File(referenceFile.getPath() + ".fai");		

			if ( ! indexFile.exists()) {
				return null;
			}

			FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
            return new IndexedFastaSequenceFile(referenceFile, index);
		}
		return null;
	}	

	/**
	 * Write unmapped record.
	 *
	 * @param writer the writer
	 * @param record the record
	 * @param start the start
	 * @param end the end
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws QSVException 
	 */
	public static void writeUnmappedRecord(BufferedWriter writer, SAMRecord record, String rgId, int start, int end) throws IOException, QSVException {

		int recordStart = record.getMateAlignmentStart();

		if (createRecord(recordStart, start, end)) {
			String readString = record.getReadString();

			if (! QSVUtil.highNCount(readString, 0.2)) {
                String sb = "unmapped," + record.getReadName() + COLON + rgId + Constants.COMMA +
                        record.getReferenceName() + Constants.COMMA +
                        recordStart + Constants.COMMA +
                        readString + NEW_LINE;

				writer.write(sb);
			}
		}
	}

	/**
	 * Return true if both of the supplied Integer objects are null, or if the the int bpPos is >= start and <= end
	 *
	 * @param bpPos the bp pos
	 * @param chrStart the chr start
	 * @param chrEnd the chr end
	 * @return true, if successful
	 */
	public static boolean createRecord(int bpPos, int chrStart, int chrEnd) {
		return  bpPos >= chrStart &&  bpPos <= chrEnd;
	}

	/**
	 * Checks if is translocation pair.
	 *
	 * @param record the record
	 * @return true, if is translocation pair
	 */
	public static boolean isTranslocationPair(SAMRecord record) {
		return  ! record.getReferenceName().equals(record.getMateReferenceName());
	}


	public static boolean doesMatePairOverlapRegions(MatePair m, int leftStart, int leftEnd, int rightStart, int rightEnd) {

		int leftMateStart = m.getLeftMate().getStart();
		int leftMateEnd = m.getLeftMate().getEnd();

		if ((leftMateStart >= leftStart && leftMateStart <= leftEnd)
				|| (leftMateEnd >= leftStart && leftMateEnd <= leftEnd)) {

			// load up RHS details
			int rightMateStart = m.getRightMate().getStart();
			int rightMateEnd = m.getRightMate().getEnd();

			//check the right start or end is within the left range
            return (rightMateStart >= rightStart && rightMateStart <= rightEnd)
                    || (rightMateEnd >= rightStart && rightMateEnd <= rightEnd);
		}	
		return false;
	}
}

