/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.util.QSVConstants;

/**
 * Class for discordant pair annotation for discordant paired end
 * 
 *
 */
public class PairedEndRecord {
    
	private final SAMRecord record;
    private String zpAnnotation = QSVConstants.Z_STAR_STAR;
    private final int isizeUpperLimit;
    private final int isizeLowerLimit;


	public PairedEndRecord (SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
        this.record = record;
        this.isizeLowerLimit = isizeLowerLimit;
        this.isizeUpperLimit = isizeUpperLimit;
    }
    
	/**
	 * Create ZP annotation for the record (ZP=discordant pair classification)
	 */
	public void createZPAnnotation() {
        
        if ( ! record.getDuplicateReadFlag()) {
        	
        	Integer nh = record.getIntegerAttribute(QSVConstants.NH);
        	
        	if (null != nh) {
        		
        		if (1 == nh.intValue()) {
        			if (record.getMateUnmappedFlag()) {
        				zpAnnotation = QSVConstants.D_STAR_STAR;
        			} else if ( ! record.getReferenceName().equals(record.getMateReferenceName()) && ! record.getMateReferenceName().equals(Constants.EQ_STRING)) {
        				zpAnnotation = QSVConstants.C_STAR_STAR;
        			} else if (record.getReadFailsVendorQualityCheckFlag()) {
        				zpAnnotation = QSVConstants.E_STAR_STAR;
        			} else if (isDifferentStrand()) {
        				zpAnnotation = "A";
        				handleOrientation();
        				handleIntervalSize();
        			} else if (isSameStrand()) {
        				zpAnnotation = "B";
        	            handleOrientation();
        	            handleIntervalSize();
        			}
        		} else {
        			if (isDifferentStrand()) {
        				zpAnnotation = "A";
        	            handleOrientation();
        	            handleIntervalSize();
        	            if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
        	                zpAnnotation = QSVConstants.Z_STAR_STAR;
        	            }                
        			}
        		}
        	} else {
        		/*
        		 * null == nh
        		 */
        		if (isDifferentStrand()) {
        			 zpAnnotation = "A";
                     handleOrientation();
                     handleIntervalSize();
                     if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
                    	 zpAnnotation = QSVConstants.Z_STAR_STAR;
                     }
        		}
        	}
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
		int absISize = Math.abs(record.getInferredInsertSize());
        if (isDistanceNormal(absISize)) {
            zpAnnotation += 'A';
        } else if (isDistanceTooSmall(absISize)) {
            zpAnnotation += 'B';
        } else if (isDistanceTooLarge(absISize)) {
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
        return record.getReadNegativeStrandFlag() == record.getMateNegativeStrandFlag();
    }

    /**
     * Record and its mate are on different strands
     * @return
     */
    public boolean isDifferentStrand() {
    		return ! isSameStrand();
    }

    public boolean isF5toF3() {
    	boolean isF5 = isReadF5();
    	boolean isLeftOfMate = isReadLeftOfMate();
    	boolean isForward = isReadForward();
    	boolean isMateForward = isMateForward();
    	
    	if (isF5 && isLeftOfMate && isForward && isMateForward) {
    		return true;
    	}
    	boolean isRightOfMate = isReadRightOfMate();
    	if (isF5 && isRightOfMate && ! isForward && ! isMateForward) {
    		return true;
    	}
    	boolean isF3 = isReadF3();
    	if (isF3 && isRightOfMate && isForward && isMateForward) {
    		return true;
    	}
    	if (isF3 && isLeftOfMate && ! isForward && ! isMateForward) {
    		return true;
    	}
    	return false;
    }

    public boolean isF3toF5() {
    	boolean isF3 = isReadF3();
    	boolean isLeftOfMate = isReadLeftOfMate();
    	boolean isForward = isReadForward();
    	boolean isMateForward = isMateForward();
		if (isF3 && isLeftOfMate && isForward && isMateForward) {
	        return true;
		}
		boolean isRightOfMate = isReadRightOfMate();
    	if (isF3 && isRightOfMate && ! isForward && ! isMateForward) {
    		return true;
    	}
        boolean isF5 = isReadF5();
        if (isF5 && isRightOfMate && isForward && isMateForward) {
        	return true;
        }
        if (isF5 && isLeftOfMate &&  ! isForward && ! isMateForward) {
            return true;
        }
        return false;
        
    }

    /**
     * Checks if pair orientation is inward
     * @return true if inward
     */
    public boolean isInward() {
        boolean isForward = isReadForward();
    	boolean isMateForward = isMateForward();
        if (isReadLeftOfMate() && isForward && ! isMateForward) {
            return true;
        } else if (isReadRightOfMate() && ! isForward && isMateForward) {
            return true;
        }
        return false;
    }

    /**
     * Checks if pair orientation is outward
     * @return true if outward
     */
    public boolean isOutward() {
        boolean isForward = isReadForward();
    	boolean isMateForward = isMateForward();
        if (isReadRightOfMate() && isForward && ! isMateForward) {
            return true;
        } else if (isReadLeftOfMate() && ! isForward && isMateForward) {
            return true;
        }
        return false;
    }

    /**
     * Insert size  is smaller than lower limit 
     * @return true if successful
     */
    public boolean isDistanceTooSmall() {
    	return isDistanceTooSmall(Math.abs(record.getInferredInsertSize()));
    }
    public boolean isDistanceTooSmall(int absoluteISize) {
    	return absoluteISize < isizeLowerLimit;
    }

    /**
     * Insert size falls within the upper and lower limits
     * @return true if successful
     */
    public boolean isDistanceNormal() {
    	return isDistanceNormal(Math.abs(record.getInferredInsertSize()));
    }
    
    public boolean isDistanceNormal(int absoluteISize) {
    	return isizeLowerLimit <= absoluteISize
    			&& absoluteISize <= isizeUpperLimit;
    }

    /**
     * Insert size  is larger than upper limit 
     * @return true if successful
     */
    public boolean isDistanceTooLarge() {
    	return isDistanceTooLarge(Math.abs(record.getInferredInsertSize()));
    }
    public boolean isDistanceTooLarge(int absoluteISize) {
    	return absoluteISize > isizeUpperLimit;
    }

    public String getZPAnnotation() {
        return this.zpAnnotation;
    }

    public void setZPAnnotation(String zp) {
    	   this.zpAnnotation = zp;     
        
    }

}
