/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import net.sf.samtools.SAMRecord;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.util.QSVConstants;

/**
 * Class for discordant pair annotation for discordant paired end
 * 
 *
 */
public class PairedEndRecord {
//	private static final int nullNHZ = 0;
    
	private final SAMRecord record;
    private String zpAnnotation;
    private final int isizeUpperLimit;
    private final int isizeLowerLimit;


	public PairedEndRecord (SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
        this.record = record;
        this.isizeLowerLimit = isizeLowerLimit;
        this.isizeUpperLimit = isizeUpperLimit;
        this.zpAnnotation = "";
    }
    
	/**
	 * Create ZP annotation for the record (ZP=discordant pair classification)
	 */
	public void createZPAnnotation() {
        
        if ( ! record.getDuplicateReadFlag()) {
        	
        	Integer nh = record.getIntegerAttribute(QSVConstants.NH);
        	
            if (null != nh && 1 == nh.intValue() && record.getMateUnmappedFlag()) {
                zpAnnotation = QSVConstants.D_STAR_STAR;
            } else if (null != nh
                    && 1 == nh.intValue()
                    && !record.getReferenceName().equals(
                            record.getMateReferenceName())
                    && !record.getMateReferenceName().equals(Constants.EQ_STRING)) {
                zpAnnotation = QSVConstants.C_STAR_STAR;
                
            } else if (null != nh && 1 == nh.intValue()
                    && record.getReadFailsVendorQualityCheckFlag()) {
                zpAnnotation = QSVConstants.E_STAR_STAR;
                
            } else if (null != nh && 1 == nh.intValue() && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
            } else if (null != nh && 1 != nh.intValue() && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
                		zpAnnotation = QSVConstants.Z_STAR_STAR;
                }                
            } else if (null == nh && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
                		zpAnnotation = QSVConstants.Z_STAR_STAR;
                }
                
            } else if (null != nh && 1 == nh && isSameStrand()) {
                zpAnnotation = "B";
                handleOrientation();
                handleIntervalSize();
            } else {
            		zpAnnotation = QSVConstants.Z_STAR_STAR;
            }
        } else {
        		zpAnnotation = QSVConstants.Z_STAR_STAR;
        }
        record.setAttribute(QSVConstants.ZP, zpAnnotation);
        	
    }
    
    public String handleOrientation() {
    	
        if ('A' == zpAnnotation.charAt(0)) {
        	
            if (isOutward()) {
                zpAnnotation += 'B';
            } else if (isInward()) {
                zpAnnotation += 'A';
            } else {
                zpAnnotation += 'X';
            }
        } else if ('B' == zpAnnotation.charAt(0)) {
            if (isF5toF3()) {
                zpAnnotation += 'B';
            } else if (isF3toF5()) {
                zpAnnotation += 'A';
            } else {
                zpAnnotation += 'X';
            }
        }
        
        return zpAnnotation;
                
    }

    /**
	 * Determine the isize between this record and its mate
	 */
	public void handleIntervalSize() {
        if (isDistanceNormal()) {
            zpAnnotation += 'A';
        } else if (isDistanceTooSmall()) {
            zpAnnotation += 'B';
        } else if (isDistanceTooLarge()) {
            zpAnnotation += 'C';
        }
    }
    
    private boolean isReadF3() {
        return record.getFirstOfPairFlag();
    }

    private boolean isReadF5() {
        return record.getSecondOfPairFlag();
    }

    private boolean isReadLeftOfMate() {
        return record.getAlignmentStart() < record.getMateAlignmentStart();
    }

    private boolean isReadRightOfMate() {
        return record.getAlignmentStart() > record.getMateAlignmentStart();
    }

    public boolean isReadForward() {
        return ! record.getReadNegativeStrandFlag();
    }

    public boolean isReadReverse() {
        return ! isReadForward();
    }

    public boolean isMateForward() {
        return ! record.getMateNegativeStrandFlag();
    }

    public boolean isMateReverse() {
        return ! isMateForward();
    }

    /**
    * Record and its mate are on the same strand
    * @return true if succcessful
    */
    public boolean isSameStrand() {
    	
        return record.getReadNegativeStrandFlag() == record
                .getMateNegativeStrandFlag();
    }

    /**
     * Record and its mate are on different strands
     * @return
     */
    public boolean isDifferentStrand() {
    		return ! isSameStrand();
    }

    public boolean isF5toF3() {
        boolean result = false;
        if (isReadF5() && isReadLeftOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadF5() && isReadRightOfMate() && isReadReverse()
                && isMateReverse()) {
            result = true;
        } else if (isReadF3() && isReadRightOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadF3() && isReadLeftOfMate() && isReadReverse()
                && isMateReverse()) {
            result = true;
        }
        return result;
    }

    public boolean isF3toF5() {
        boolean result = false;
        if (isReadF3() && isReadLeftOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadF3() && isReadRightOfMate() && isReadReverse()
                && isMateReverse()) {
            result = true;
        } else if (isReadF5() && isReadRightOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadF5() && isReadLeftOfMate() && isReadReverse()
                && isMateReverse()) {
            result = true;
        }
        return result;
    }

    /**
     * Checks if pair orientation is inward
     * @return true if inward
     */
    public boolean isInward() {
        boolean result = false;
        if (isReadLeftOfMate() && isReadForward() && isMateReverse()) {
            result = true;
        } else if (isReadRightOfMate() && isReadReverse() && isMateForward()) {
            result = true;
        }
        return result;
    }

    /**
     * Checks if pair orientation is outward
     * @return true if outward
     */
    public boolean isOutward() {
        boolean result = false;
        if (isReadRightOfMate() && isReadForward() && isMateReverse()) {
            result = true;
        } else if (isReadLeftOfMate() && isReadReverse() && isMateForward()) {
            result = true;
        }
        return result;
    }

    /**
     * Insert size  is smaller than lower limit 
     * @return true if successful
     */
    public boolean isDistanceTooSmall() {
        int absoluteISize = Math.abs(record.getInferredInsertSize());
        return 0 <= absoluteISize && absoluteISize < isizeLowerLimit;
    }

    /**
     * Insert size falls within the upper and lower limits
     * @return true if successful
     */
    public boolean isDistanceNormal() {
        int absoluteISize = Math.abs(record.getInferredInsertSize());
        return isizeLowerLimit <= absoluteISize
                && absoluteISize <= isizeUpperLimit;
    }

    /**
     * Insert size  is larger than upper limit 
     * @return true if successful
     */
    public boolean isDistanceTooLarge() {
        int absoluteISize = Math.abs(record.getInferredInsertSize());
        return absoluteISize > isizeUpperLimit;
    }

    public String getZPAnnotation() {
        return this.zpAnnotation;
    }

    public void setZPAnnotation(String zp) {
    	   this.zpAnnotation = zp;     
        
    }

}
