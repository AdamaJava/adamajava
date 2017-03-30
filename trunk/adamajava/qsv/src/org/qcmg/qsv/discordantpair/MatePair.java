/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import static org.qcmg.common.util.Constants.COMMA;
import static org.qcmg.common.util.Constants.TAB;

import java.io.Serializable;
import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

public class MatePair implements Comparable<MatePair> {

	private static final QLogger logger = QLoggerFactory.getLogger(MatePair.class);

	private final String readName;
	private Mate leftMate;
	private Mate rightMate;
	private final PairClassification zp;
	private final String pairOrder;

	public MatePair(SAMRecord leftRecord, SAMRecord rightRecord) throws QSVException {
		this.readName = leftRecord.getReadName() + ":" + leftRecord.getReadGroup().getId();

		String check = rightRecord.getReadName() + ":" + rightRecord.getReadGroup().getId();

		if ( ! (check.equals(readName))) {
			logger.info("Left" + readName);
			logger.info("Right" + check);
			logger.info("Mate" + toString());
			throw new QSVException("PAIR_ERROR");
		}
		this.zp = PairClassification.valueOf(getPairClassificationFromSamRecord(leftRecord));
		this.leftMate = new Mate(leftRecord);
		this.rightMate = new Mate(rightRecord);
		boolean needToSwap = checkSortOrder();
		this.pairOrder = setPairOrder(needToSwap ? rightRecord : leftRecord);
	}

	public MatePair(String readLine) {
		String[] readPairArray = readLine.split(",");
		this.readName = readPairArray[0];
		String zpString = readPairArray[4];
		this.leftMate = new Mate(readName, readPairArray[1],
				Integer.parseInt(readPairArray[2]),
				Integer.parseInt(readPairArray[3]), zpString,
				Integer.parseInt(readPairArray[5]),
				Boolean.valueOf(readPairArray[6]));
		this.rightMate = new Mate(readName, readPairArray[8],
				Integer.parseInt(readPairArray[9]),
				Integer.parseInt(readPairArray[10]), readPairArray[11],
				Integer.parseInt(readPairArray[12]), 
				Boolean.valueOf(readPairArray[13]));

		if (zpString.equals(QSVConstants.C_STAR_STAR)) {
			zpString = "Cxx";
		}
		this.zp = PairClassification.valueOf(zpString);
		this.pairOrder = readPairArray[14];
		checkSortOrder(); 
	}

	private String getPairClassificationFromSamRecord(SAMRecord samRecord) {
		String zp = (String) samRecord.getAttribute(QSVConstants.ZP_SHORT);
		if (zp.equals(QSVConstants.C_STAR_STAR)) {
			zp = "Cxx";
		}
		return zp;
	}

	public String getReadName() {
		return readName;
	}

	public Mate getLeftMate() {
		return leftMate;
	}

	public Mate getRightMate() {
		return rightMate;
	}

	public PairClassification getZpType() {
		return zp;
	}

	/**
	 * Check the order of left and right reads to make sure that they are in the
	 * correct order. If no, swap the records
	 */
	private boolean checkSortOrder() {
		boolean needToSwap = false;
		// sort to make sure they are in the correct order: first check if
		// they're on the
		// same chromosome
		if (leftMate.getReferenceName().equals(rightMate.getReferenceName())) {
			// wrong order: swap the records
			if (leftMate.getStart() > rightMate.getStart()) {
				needToSwap = true;
				swapMates();
			}

			// on different chromosomes
		} else {
			boolean reorder = QSVUtil.reorderByChromosomes(leftMate.getReferenceName(), rightMate.getReferenceName());
			if (reorder) {
				needToSwap = true;
				swapMates();
			}
		}

		return needToSwap;
	}

	private void swapMates() {
		Mate temp = leftMate;
		leftMate = rightMate;
		rightMate = temp;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(leftMate.getReadName()).append(COMMA);
		sb.append(leftMate.getReferenceName()).append(COMMA);
		sb.append(leftMate.getStart()).append(COMMA);
		sb.append(leftMate.getEnd()).append(COMMA);
		sb.append(leftMate.getZp()).append(COMMA);
		sb.append(leftMate.getFlags()).append(COMMA);
		sb.append(leftMate.getNegOrientation()).append(COMMA);
		sb.append(rightMate.getReadName()).append(COMMA);
		sb.append(rightMate.getReferenceName()).append(COMMA);
		sb.append(rightMate.getStart()).append(COMMA);
		sb.append(rightMate.getEnd()).append(COMMA);
		sb.append(rightMate.getZp()).append(COMMA);
		sb.append(rightMate.getFlags()).append(COMMA);
		sb.append(rightMate.getNegOrientation()).append(COMMA);
		sb.append(pairOrder);
		sb.append(QSVUtil.getNewLine());

		return sb.toString();
	}

	public String toVerboseString(boolean isQCMG) {
		StringBuilder sb = new StringBuilder();
		sb.append(leftMate.getReadName()).append(COMMA);
		sb.append(leftMate.getReferenceName() ).append(COMMA);
		sb.append(leftMate.getStart() ).append(COMMA);
		sb.append(leftMate.getEnd()).append(COMMA);
		sb.append(leftMate.getFlags()).append(COMMA);
		sb.append(leftMate.getStrand()).append(COMMA);
		sb.append(rightMate.getReadName()).append(COMMA);
		sb.append(rightMate.getReferenceName()).append(COMMA);
		sb.append(rightMate.getStart()).append(COMMA);
		sb.append(rightMate.getEnd()).append(COMMA);
		sb.append(rightMate.getFlags()).append(COMMA);
		sb.append(rightMate.getStrand()).append(COMMA);
		sb.append(pairOrder).append(COMMA);
		sb.append(QSVUtil.getMutationByPairClassification(zp));
		if (isQCMG) {
			sb.append("" + COMMA + zp);
		}
		sb.append(QSVUtil.getNewLine());

		return sb.toString();
	}

	public String toClusterString() {
		StringBuilder sb = new StringBuilder();
		sb.append(leftMate.getReadName()).append(TAB);
		sb.append(leftMate.getReferenceName()).append(TAB);
		sb.append(leftMate.getStart()).append(TAB);
		sb.append(leftMate.getEnd()).append(TAB);
		sb.append(leftMate.getZp()).append(TAB);
		sb.append(leftMate.getFlags()).append(TAB);
		sb.append(rightMate.getReadName()).append(TAB);
		sb.append(rightMate.getReferenceName()).append(TAB);
		sb.append(rightMate.getStart()).append(TAB);
		sb.append(rightMate.getEnd()).append(TAB);
		sb.append(rightMate.getZp()).append(TAB);
		sb.append(rightMate.getFlags()).append(TAB);
		sb.append(pairOrder);
		sb.append(QSVUtil.getNewLine());
		return sb.toString();
	}

	private String setPairOrder(SAMRecord record) {

		char first = record.getReadNegativeStrandFlag() ? 'R' : 'F';
		char second = record.getMateNegativeStrandFlag() ? 'R' : 'F';
		int pair1 = record.getSecondOfPairFlag() ? 2 : 1;
		int pair2 = record.getSecondOfPairFlag() ? 1 :  2;

		return "" + first + pair1 + second + pair2;
	}

	public String getPairOrder() {
		return this.pairOrder;
	}

	@Override
	public boolean equals(final Object o) {

		if (!(o instanceof MatePair)) return false;

		final MatePair matePair = (MatePair) o;

		return matePair.getReadName().equals(readName);
	}

	@Override
	public int hashCode() {
		return 31*readName.hashCode();
	}

	@Override
	public int compareTo(MatePair pair2) {
		return this.getReadName().compareTo(pair2.getReadName());
	}

	public String getStrandOrientation() {
		char left = leftMate.getNegOrientation() ? '-' : '+';
		char right = rightMate.getNegOrientation() ? '-' : '+';
		return left + "/" + right;
	}

	public boolean hasPairOverlap() {
		if ( ! zp.getPairingClassification().equals("Cxx")) {
			if (this.getLeftMate().getEnd() > this.getRightMate().getStart()) {
				return true;
			}
		}        
		return false;
	}

	public static class ReadMateLeftStartComparator implements Serializable, Comparator<MatePair> {

		private static final long serialVersionUID = 1379396837814177902L;

		@Override
		public int compare(MatePair mate1, MatePair mate2) {
			return Integer.compare(mate1.getLeftMate().getStart(), mate2.getLeftMate().getStart());
		}
	}

	public static class ReadMateLeftEndComparator implements Serializable, Comparator<MatePair> {

		private static final long serialVersionUID = -6403294253402885410L;

		@Override
		public int compare(MatePair pair1, MatePair pair2) {
			return Integer.compare(pair1.getLeftMate().getEnd(), pair2.getLeftMate().getEnd());
		}
	}

	public static class ReadMateRightStartComparator implements Serializable, Comparator<MatePair> {

		private static final long serialVersionUID = 1903207693120505632L;

		@Override
		public int compare(MatePair pair1, MatePair pair2) {
			return Integer.compare(pair1.getRightMate().getStart(), pair2.getRightMate().getStart());
		}
	}

	public static class ReadMateRightEndComparator implements Serializable, Comparator<MatePair> {

		static final long serialVersionUID = -5699305034250941079L;

		@Override
		public int compare(MatePair pair1, MatePair pair2) {
			return Integer.compare(pair1.getRightMate().getEnd(), pair2.getRightMate().getEnd());
		}
	}

	public String getSVCategoryForLMP() {
		String pc = zp.toString();
		if ((pc.equals("Cxx") || pc.equals("AAC")) && (pairOrder.equals("F2F1") || pairOrder.equals("R1R2"))) {
			return QSVConstants.ORIENTATION_1; 				
		} else if ((pc.equals("Cxx") || pc.equals("ABA") || pc.equals("ABC") || pc.equals("ABB")) 
				&& (pairOrder.equals("F1F2") || pairOrder.equals("R2R1"))) {
			return QSVConstants.ORIENTATION_2;			
		} else if (pc.equals("Cxx") || pc.equals("BAA") || pc.equals("BBA") || pc.equals("BAB") || pc.equals("BBB") || pc.equals("BAC") || pc.equals("BBC")) {
			if (pairOrder.equals("F2R1") || pairOrder.equals("R1F2")) {
				return QSVConstants.ORIENTATION_3;
			}
			if (pairOrder.equals("F1R2") || pairOrder.equals("R2F1")) {
				return QSVConstants.ORIENTATION_4;
			}    				
		} else if (pc.equals("AAB") && (pairOrder.equals("F2F1") || pairOrder.equals("R1R2"))) {
			return QSVConstants.ORIENTATION_2;
		}
		return null;
	}

	public String getSVCategoryForPE() {
		String pc =zp.toString();
		if ((pc.equals("Cxx") || pc.equals("AAC")) && (pairOrder.equals("F2R1") || pairOrder.equals("F1R2"))) {
			return QSVConstants.ORIENTATION_1;				
		} else if ((pc.equals("AAB") && (pairOrder.equals("R1F2") || pairOrder.equals("R2F1")))  || (pc.equals("Cxx") || pc.equals("ABA") || pc.equals("ABC") || pc.equals("AAB") || pc.equals("ABB")) && (pairOrder.equals("R1F2") || pairOrder.equals("R2F1"))) {
			return QSVConstants.ORIENTATION_2;
		} else if (pc.equals("Cxx") || pc.equals("BAA") || pc.equals("BBA") || pc.equals("BAB") || pc.equals("BBB") || pc.equals("BAC") || pc.equals("BBC")) {					
			if (pairOrder.equals("F1F2") || pairOrder.equals("F2F1")) {
				return QSVConstants.ORIENTATION_3;
			}
			if (pairOrder.equals("R2R1") || pairOrder.equals("R1R2")) {
				return QSVConstants.ORIENTATION_4;
			}
		}
		return null;
	}

	public String getSVCategoryForIMP() {
		String pc =zp.toString();
		if ((pc.equals("Cxx") || pc.equals("AAC")) && (pairOrder.equals("R2F1") || pairOrder.equals("R1F2"))) {
			return QSVConstants.ORIENTATION_1;				
		} else if ((pc.equals("AAB") && (pairOrder.equals("F1R2") || pairOrder.equals("F2R1")))  || (pc.equals("Cxx") || pc.equals("ABA") || pc.equals("ABC") || pc.equals("AAB") || pc.equals("ABB")) && (pairOrder.equals("F1R2") || pairOrder.equals("F2R1"))) {
			return QSVConstants.ORIENTATION_2;
		} else if (pc.equals("Cxx") || pc.equals("BAA") || pc.equals("BBA") || pc.equals("BAB") || pc.equals("BBB") || pc.equals("BAC") || pc.equals("BBC")) {					
			if (pairOrder.equals("F1F2") || pairOrder.equals("F2F1")) {
				return QSVConstants.ORIENTATION_4;
			}
			if (pairOrder.equals("R2R1") || pairOrder.equals("R1R2")) {
				return QSVConstants.ORIENTATION_3;
			}
		}
		return null;
	}
}
