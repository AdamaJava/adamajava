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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.splitread.UnmappedRead;


/**
 * Utility methods for qsv
 */
public class QSVUtil {
	
	public static final String NEW_LINE = System.getProperty("line.separator");
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
		String write = timeType + secondsToString(time / 1000);
		return write;
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
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);
	
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
		String reverse = "";

		for (int i = consensus.length() - 1; i >= 0; i--) {
			char pos = consensus.charAt(i);
			if (pos == 'A') {
				reverse += 'T';
			} else if (pos == 'T') {
				reverse += 'A';
			} else if (pos == 'C') {
				reverse += 'G';
			} else if (pos == 'G') {
				reverse += 'C';
			} else {
				reverse += 'N';
			}
		}
		return reverse;		
	}

	/**
	 * Gets the pair group by zp.
	 *
	 * @param zpType the zp type
	 * @return the pair group by zp
	 */
	public static PairGroup getPairGroupByZP(PairClassification zpType) {
		String zp = zpType.toString();
		if (zp.equals(BAA) || zp.equals(BBA)) {
			return PairGroup.valueOf("BAA_BBA");
		} else if (zp.equals(BAB) || zp.equals(BBB)) {
			return PairGroup.valueOf("BAB_BBB");
		} else if (zp.equals(BAC) || zp.equals(BBC)) {
			return  PairGroup.valueOf("BAC_BBC");
		} else {
			return PairGroup.valueOf(zp);
		}    	
	}

	/**
	 * Gets the mutation type by pair group.
	 *
	 * @param pg the pg
	 * @return the mutation by pair group
	 */
	public static String getMutationByPairGroup(String pg) {
		if (pg.equals("Cxx")) {
			return CTX;
		} else if (pg.equals(AAC)) {
			return "DEL/ITX";
		} else if (pg.equals("BAC_BBC") || pg.equals("BAA_BBA") || pg.equals("BAB_BBB")) {
			return "INV/ITX";
		} else if (pg.equals(ABC) || pg.equals(ABA) || pg.equals(AAB) || pg.equals(ABB)) {
			return "DUP/INS/ITX";
		} else {
			return pg;
		}     	
	}

	/**
	 * Gets the mutation type by pair classification.
	 *
	 * @param pClass the class
	 * @return the mutation by pair classification
	 */
	public static String getMutationByPairClassification(PairClassification pClass) {
		String zp = pClass.toString();
		if (zp.equals("Cxx")) {
			return CTX;
		} else if (zp.equals(AAC)) {
			return "DEL/ITX";
		} else if (zp.equals(BAC) || zp.equals(BBC) ||zp.equals(BAA) || zp.equals(BBA) || zp.equals(BAB) || zp.equals(BBB) ) {
			return "INV/ITX";
		} else if (zp.equals(ABC) || zp.equals(ABA) || zp.equals(ABB) || zp.equals(AAB)) {
			return "DUP/INS/ITX";
		} else {
			return zp;
		}    	
	}

	/**
	 * Gets the file separator.
	 *
	 * @return the file separator
	 */
	public static String getFileSeparator() {
		return System.getProperty("file.separator");
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
	 * Gets a sepatator for a new line.
	 *
	 * @return the new line
	 */
	public static String getNewLine() {
		return NEW_LINE;
	}

	/**
	 * Gets the category by zp.
	 *
	 * @param zpPairClass the zp pair class
	 * @return the category by zp
	 */
	public static List<String> getCategoryByZP(PairClassification zpPairClass) {
		String zp = zpPairClass.toString();
		List<String> list = new ArrayList<>();
		if (zp.equals(AAC)) {
			list.add(QSVConstants.ORIENTATION_1);			
		} else if (zp.equals("Cxx")) {
			list.add(QSVConstants.ORIENTATION_1);
			list.add(QSVConstants.ORIENTATION_2);
			list.add(QSVConstants.ORIENTATION_3);
			list.add(QSVConstants.ORIENTATION_4);
		} else if (zp.equals(AAB) || zp.equals(ABB) || zp.equals(ABA) || zp.equals(ABC)) {
			list.add(QSVConstants.ORIENTATION_2);
		} else if (zp.equals(BBA) || zp.equals(BAA) || zp.equals(BAB) || zp.equals(BBB) 
				|| zp.equals(BAC) || zp.equals(BBC)) {
			list.add(QSVConstants.ORIENTATION_3);
			list.add(QSVConstants.ORIENTATION_4);
		}

		return list;
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
		if (zp.equals(AAC)) {
			list.add(QSVConstants.ORIENTATION_1);			
		} else if (zp.equals("Cxx")) {
			list.add(QSVConstants.ORIENTATION_1);
			list.add(QSVConstants.ORIENTATION_2);
			list.add(QSVConstants.ORIENTATION_3);
			list.add(QSVConstants.ORIENTATION_4);
		} else if (zp.equals(AAB) || zp.equals(ABC) || zp.equals(ABA) || zp.equals(ABB)) {
			list.add(QSVConstants.ORIENTATION_2);
		} else if (zp.equals("BAA_BBA") || zp.equals("BAB_BBB") || zp.equals("BAC_BBC")) {
			list.add(QSVConstants.ORIENTATION_3);
			list.add(QSVConstants.ORIENTATION_4);
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
		if (oc.equals("1")) {
			list.add(AAC);			
		} else if (oc.equals("2")) {
			list.add(ABA);
			list.add(ABB);
			list.add(AAB);
			list.add(ABC);
		} else if (oc.equals("3") || oc.equals("4")) {
			list.add(BAA);
			list.add(BBA);
			list.add(BAB);
			list.add(BBB);
			list.add(BAC);
			list.add(BBC);
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
	
	public static synchronized void setupReferenceMap(File referenceFile) throws QSVException {
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
	 * @throws QSVException the qSV exception
	 */
	private static IndexedFastaSequenceFile getReferenceFile(File referenceFile) throws QSVException {

		if (referenceFile.exists()) {
			File indexFile = new File(referenceFile.getPath() + ".fai");		

			if ( ! indexFile.exists()) {
				return null;
			}

			FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
			IndexedFastaSequenceFile f = new IndexedFastaSequenceFile(referenceFile, index);
			return f;
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
	 * @param name the name
	 * @param chr the chr
	 * @param softClipDir the soft clip dir
	 * @param isTumour the is tumour
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws QSVException 
	 */
	public static void writeUnmappedRecord(BufferedWriter writer,
			SAMRecord record, Integer start, Integer end, String name, String chr, String softClipDir, boolean isTumour) throws IOException, QSVException {

		if (createRecord(record.getMateAlignmentStart(), start, end)) {
			if ( ! QSVUtil.highNCount(record.getReadString(), 0.2)) {
				UnmappedRead read = new UnmappedRead(record, isTumour);
				writer.write(read.toTmpString());				
			}
		}		
	}

	public static void writeUnmappedRecord(BufferedWriter writer,
			SAMRecord record, Integer start, Integer end, boolean isTumour) throws IOException, QSVException {

		int recordStart = record.getMateAlignmentStart();

		if (createRecord(recordStart, start, end)) {
			String readString = record.getReadString();

			if (! QSVUtil.highNCount(readString, 0.2)) {

				StringBuilder sb = new StringBuilder("unmapped,");
				sb.append(record.getReadName()).append(COLON ).append(record.getReadGroup() != null ? record.getReadGroup().getId() : Constants.EMPTY_STRING).append(Constants.COMMA);
				sb.append(record.getReferenceName()).append(Constants.COMMA);
				sb.append(recordStart).append(Constants.COMMA);
				sb.append(readString).append(getNewLine());

				writer.write(sb.toString());
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
	public static boolean createRecord(int bpPos, Integer chrStart, Integer chrEnd) {

		return chrStart == null && chrEnd == null
				|| (null != chrStart && bpPos >= chrStart.intValue() && null != chrEnd && bpPos <= chrEnd.intValue());

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
			if ((rightMateStart >= rightStart && rightMateStart <= rightEnd)
					|| (rightMateEnd >= rightStart && rightMateEnd <= rightEnd)) {

				return true;
			}
		}	
		return false;
	}
}

