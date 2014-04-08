/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import java.io.Serializable;
import java.util.Comparator;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import net.sf.samtools.SAMRecord;

public class MatePair implements Comparable<MatePair> {

    private QLogger logger = QLoggerFactory.getLogger(getClass());
    private String readName;
    private Mate leftMate;
    private Mate rightMate;
    private final PairClassification zp;
	private String pairOrder;
    private SAMRecord leftRecord;
    private SAMRecord rightRecord;

    public MatePair(SAMRecord leftRecord, SAMRecord rightRecord) throws QSVException {
        this.readName = leftRecord.getReadName() + ":" + leftRecord.getReadGroup().getId();
        
        String check = rightRecord.getReadName() + ":" + rightRecord.getReadGroup().getId();
        
        if (!(check.equals(readName))) {
            logger.info("Left" + leftRecord.getReadName() + ":" + leftRecord.getReadGroup().getId());
            logger.info("Right" + rightRecord.getReadName() + ":" + rightRecord.getReadGroup().getId());
            logger.info("Mate" + toString());
            throw new QSVException("PAIR_ERROR");
        }
        this.zp = PairClassification.valueOf(getPairClassificationFromSamRecord(leftRecord));
        this.leftMate = new Mate(leftRecord);
        this.rightMate = new Mate(rightRecord);
        this.leftRecord = leftRecord;
        this.rightRecord = rightRecord;
        checkSortOrder();
        this.pairOrder = setPairOrder();
        this.leftRecord = null;
        this.rightRecord = null;
    }
    
	public MatePair(String readLine) {
		String[] readPairArray = readLine.split(",");
		String name = readPairArray[0];
		Mate leftRead = new Mate(name, readPairArray[1],
				Integer.parseInt(readPairArray[2]),
				Integer.parseInt(readPairArray[3]), readPairArray[4],
				Integer.parseInt(readPairArray[5]),
				new Boolean(readPairArray[6]));
		Mate rightRead = new Mate(name, readPairArray[8],
				Integer.parseInt(readPairArray[9]),
				Integer.parseInt(readPairArray[10]), readPairArray[11],
				Integer.parseInt(readPairArray[12]), 
				new Boolean(readPairArray[13]));

		String zpString = readPairArray[4];
		if (zpString.equals("C**")) {
			zpString = "Cxx";
		}
		this.zp = PairClassification.valueOf(zpString);
		this.readName = name;
		this.leftMate = leftRead;
		this.rightMate = rightRead;
		this.pairOrder = readPairArray[14];
		checkSortOrder(); 
		name = null;
		readPairArray = null;
		readLine = null;
		zpString = null;
	}

	private String getPairClassificationFromSamRecord(SAMRecord samRecord) {
        String zp = (String) samRecord.getAttribute("ZP");
        if (zp.equals("C**")) {
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
    private void checkSortOrder() {
        // sort to make sure they are in the correct order: first check if
        // they're on the
        // same chromosome
        if (leftMate.getReferenceName().equals(rightMate.getReferenceName())) {
            // wrong order: swap the records
            if (leftMate.getStart() > rightMate.getStart()) {
                swapMates();
            }

            // on different chromosomes
        } else {
            boolean reorder = QSVUtil.reorderByChromosomes(leftMate.getReferenceName(), rightMate.getReferenceName());
            if (reorder) {
            	swapMates();
            }
        }
    }

    private void swapMates() {
        Mate temp = leftMate;
        leftMate = rightMate;
        rightMate = temp;
        
        if (leftRecord != null && rightRecord != null) {
        	SAMRecord tempRecord = leftRecord;
        	leftRecord = rightRecord;
        	rightRecord = tempRecord;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
          sb.append(leftMate.getReadName() + ",");
          sb.append(leftMate.getReferenceName() + ",");
          sb.append(leftMate.getStart() + ",");
          sb.append(leftMate.getEnd() + ",");
          sb.append(leftMate.getZp()+ ",");
          sb.append(leftMate.getFlags()+ ",");
          sb.append(leftMate.getNegOrientation()+ ",");
          sb.append(rightMate.getReadName() + ",");
          sb.append(rightMate.getReferenceName() + ",");
          sb.append(rightMate.getStart() + ",");
          sb.append(rightMate.getEnd()+ ",");
          sb.append(rightMate.getZp()+ ",");
          sb.append(rightMate.getFlags()+ ",");
          sb.append(rightMate.getNegOrientation()+ ",");
          sb.append(pairOrder);
          sb.append(QSVUtil.getNewLine());
        
        return sb.toString();
    }

    public String toVerboseString(boolean isQCMG) {
        StringBuffer sb = new StringBuffer();
          sb.append(leftMate.getReadName() + ",");
          sb.append(leftMate.getReferenceName() + ",");
          sb.append(leftMate.getStart() + ",");
          sb.append(leftMate.getEnd() + ",");
          //sb.append(leftMate.getZp()+ ",");
          sb.append(leftMate.getFlags()+ ",");
          sb.append(leftMate.getStrand()+ ",");
          sb.append(rightMate.getReadName() + ",");
          sb.append(rightMate.getReferenceName() + ",");
          sb.append(rightMate.getStart() + ",");
          sb.append(rightMate.getEnd()+ ",");
          //sb.append(rightMate.getZp()+ ",");
          sb.append(rightMate.getFlags()+ ",");
          sb.append(rightMate.getStrand()+ ",");
          sb.append(pairOrder + ",");
          sb.append(QSVUtil.getMutationByPairClassification(zp));
          if (isQCMG) {
        	  sb.append("," + zp);
          }
          sb.append(QSVUtil.getNewLine());
        
        return sb.toString();
    }
    
    public String toClusterString() {
        StringBuffer sb = new StringBuffer();
          sb.append(leftMate.getReadName() + "\t");
          sb.append(leftMate.getReferenceName() + "\t");
          sb.append(leftMate.getStart() + "\t");
          sb.append(leftMate.getEnd() + "\t");
          sb.append(leftMate.getZp()+ "\t");
          sb.append(leftMate.getFlags()+ "\t");
          sb.append(rightMate.getReadName() + "\t");
          sb.append(rightMate.getReferenceName() + "\t");
          sb.append(rightMate.getStart() + "\t");
          sb.append(rightMate.getEnd()+ "\t");
          sb.append(rightMate.getZp()+ "\t");
          sb.append(rightMate.getFlags()+ "\t");
          sb.append(pairOrder);
          sb.append(QSVUtil.getNewLine());
        return sb.toString();
    }
    
    private String setPairOrder() {
        
        String first = "F";
        String second = "F";
        String pair1 = "1";
        String pair2 = "2";
        
        if (leftRecord.getReadNegativeStrandFlag()) {
            first = "R";
        }
        
        if (leftRecord.getMateNegativeStrandFlag()) {
            second = "R";
        }
        
        if (leftRecord.getSecondOfPairFlag()) {
            pair1 = "2";
            pair2 = "1";
        }
        return first + pair1 + second + pair2;
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
       
        //both negative
        if (leftMate.getNegOrientation() && rightMate.getNegOrientation()) {
            return "-/-";
          //positive/negative  
        } else if (!leftMate.getNegOrientation() && rightMate.getNegOrientation()){
            return "+/-";
          //negative/positive  
        } else if (leftMate.getNegOrientation() && !rightMate.getNegOrientation()) {
            return "-/+";
        } else {
            return "+/+";
        }
    }

    public boolean hasPairOverlap() {
        if (!zp.getPairingClassification().equals("Cxx")) {
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

            int pair1Int = mate1.getLeftMate().getStart();
            int pair2Int = mate2.getLeftMate().getStart();
            if (pair1Int > pair2Int) {
                return 1;
            } else if (pair1Int < pair2Int) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public static class ReadMateLeftEndComparator implements Serializable, Comparator<MatePair> {

        private static final long serialVersionUID = -6403294253402885410L;

        @Override
        public int compare(MatePair pair1, MatePair pair2) {
            int pair1Int = pair1.getLeftMate().getEnd();
            int pair2Int = pair2.getLeftMate().getEnd();
            if (pair1Int > pair2Int) {
                return 1;
            } else if (pair1Int < pair2Int) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public static class ReadMateRightStartComparator implements Serializable, Comparator<MatePair> {

        private static final long serialVersionUID = 1903207693120505632L;

        @Override
        public int compare(MatePair pair1, MatePair pair2) {
            int pair1Int = pair1.getRightMate().getStart();
            int pair2Int = pair2.getRightMate().getStart();
            if (pair1Int > pair2Int) {
                return 1;
            } else if (pair1Int < pair2Int) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    
    public static class ReadMateRightEndComparator implements Serializable, Comparator<MatePair> {
        
        static final long serialVersionUID = -5699305034250941079L;

        @Override
        public int compare(MatePair pair1, MatePair pair2) {
            int pair1Int = pair1.getRightMate().getEnd();
            int pair2Int = pair2.getRightMate().getEnd();
            if (pair1Int > pair2Int) {
                return 1;
            } else if (pair1Int < pair2Int) {
                return -1;
            } else {
                return 0;
            }
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
