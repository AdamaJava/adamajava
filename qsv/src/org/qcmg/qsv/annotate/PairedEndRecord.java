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
	
	public static String createZPAnnotation(SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
		String zpAnnotation = "";
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
				
			} else if (null != nh && 1 == nh.intValue() && isDifferentStrand(record)) {
				zpAnnotation = handleOrientation(record, "A");
				zpAnnotation += handleIntervalSize(record, isizeLowerLimit, isizeUpperLimit);
			} else if (null != nh && 1 != nh.intValue() && isDifferentStrand(record)) {
				zpAnnotation = handleOrientation(record, "A");
				zpAnnotation += handleIntervalSize(record, isizeLowerLimit, isizeUpperLimit);
				if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
					zpAnnotation = QSVConstants.Z_STAR_STAR;
				}                
			} else if (null == nh && isDifferentStrand(record)) {
				zpAnnotation = handleOrientation(record, "A");
				zpAnnotation += handleIntervalSize(record, isizeLowerLimit, isizeUpperLimit);
				if ( ! zpAnnotation.equals(QSVConstants.AAA)) {
					zpAnnotation = QSVConstants.Z_STAR_STAR;
				}
				
			} else if (null != nh && 1 == nh && isSameStrand(record)) {
				zpAnnotation = handleOrientation(record, "B");
				zpAnnotation += handleIntervalSize(record, isizeLowerLimit, isizeUpperLimit);
			} else {
				zpAnnotation = QSVConstants.Z_STAR_STAR;
			}
		} else {
			zpAnnotation = QSVConstants.Z_STAR_STAR;
		}
		record.setAttribute(QSVConstants.ZP, zpAnnotation);
		return zpAnnotation;
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
    
    public static String handleOrientation(SAMRecord record, String zpAnnotation) {
    	
    	if ('A' == zpAnnotation.charAt(0)) {
    		
    		if (isOutward(record)) {
    			zpAnnotation += 'B';
    		} else if (isInward(record)) {
    			zpAnnotation += 'A';
    		} else {
    			zpAnnotation += 'X';
    		}
    	} else if ('B' == zpAnnotation.charAt(0)) {
    		if (isF5toF3(record)) {
    			zpAnnotation += 'B';
    		} else if (isF3toF5(record)) {
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
	
	public static String handleIntervalSize(SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
		int absoluteISize = Math.abs(record.getInferredInsertSize());
		if (isDistanceNormal(absoluteISize, isizeLowerLimit, isizeUpperLimit)) {
			return "A";
		} else if (isDistanceTooSmall(absoluteISize, isizeLowerLimit)) {
			return  "B";
		} else if (isDistanceTooLarge(absoluteISize, isizeUpperLimit)) {
			return  "C";
		}
		return Constants.EMPTY_STRING;
	}
    
    private boolean isReadF3() {
        return record.getFirstOfPairFlag();
    }
    public static boolean isReadF3(SAMRecord record) {
    	return record.getFirstOfPairFlag();
    }

    private boolean isReadF5() {
    	return isReadF5(record);
    }
    public static boolean isReadF5(SAMRecord record) {
    	return record.getSecondOfPairFlag();
    }

    private boolean isReadLeftOfMate() {
    	return isReadLeftOfMate(record);
    }
    public static boolean isReadLeftOfMate(SAMRecord record) {
    	return record.getAlignmentStart() < record.getMateAlignmentStart();
    }

    private boolean isReadRightOfMate() {
    	return isReadRightOfMate(record);
    }
    public static boolean isReadRightOfMate(SAMRecord record) {
    	return record.getAlignmentStart() > record.getMateAlignmentStart();
    }

    public boolean isReadForward() {
    	return isReadForward(record);
    }
    public static boolean isReadForward(SAMRecord record) {
    	return ! record.getReadNegativeStrandFlag();
    }

    public boolean isReadReverse() {
    	return isReadReverse(record);
    }
    public static boolean isReadReverse(SAMRecord record) {
    	return ! isReadForward(record);
    }

    public boolean isMateForward() {
    	return isMateForward(record);
    }
    public static boolean isMateForward(SAMRecord record) {
    	return ! record.getMateNegativeStrandFlag();
    }

    public boolean isMateReverse() {
        return ! isMateForward();
    }
    public static boolean isMateReverse(SAMRecord record) {
    	return ! isMateForward(record);
    }

    /**
    * Record and its mate are on the same strand
    * @return true if succcessful
    */
    public boolean isSameStrand() {
    	return isSameStrand(record);
    }
    /**
     * Record and its mate are on the same strand
     * @return true if succcessful
     */
    public static boolean isSameStrand(SAMRecord record) {
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
    public static boolean isDifferentStrand(SAMRecord record) {
    	return ! isSameStrand(record);
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
    
    public static boolean isF5toF3(SAMRecord record) {
    	boolean result = false;
    	if (isReadF5(record) && isReadLeftOfMate(record) && isReadForward(record)
    			&& isMateForward(record)) {
    		result = true;
    	} else if (isReadF5(record) && isReadRightOfMate(record) && isReadReverse(record)
    			&& isMateReverse(record)) {
    		result = true;
    	} else if (isReadF3(record) && isReadRightOfMate(record) && isReadForward(record)
    			&& isMateForward(record)) {
    		result = true;
    	} else if (isReadF3(record) && isReadLeftOfMate(record) && isReadReverse(record)
    			&& isMateReverse(record)) {
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
    
    public static boolean isF3toF5(SAMRecord record) {
    	boolean result = false;
    	if (isReadF3(record) && isReadLeftOfMate(record) && isReadForward(record)
    			&& isMateForward(record)) {
    		result = true;
    	} else if (isReadF3(record) && isReadRightOfMate(record) && isReadReverse(record)
    			&& isMateReverse(record)) {
    		result = true;
    	} else if (isReadF5(record) && isReadRightOfMate(record) && isReadForward(record)
    			&& isMateForward(record)) {
    		result = true;
    	} else if (isReadF5(record) && isReadLeftOfMate(record) && isReadReverse(record)
    			&& isMateReverse(record)) {
    		result = true;
    	}
    	return result;
    }

    /**
     * Checks if pair orientation is inward
     * @return true if inward
     */
    public boolean isInward() {
    	return isInward(record);
//        boolean result = false;
//        if (isReadLeftOfMate() && isReadForward() && isMateReverse()) {
//            result = true;
//        } else if (isReadRightOfMate() && isReadReverse() && isMateForward()) {
//            result = true;
//        }
//        return result;
    }
    
    public static boolean isInward(SAMRecord record) {
    	boolean result = false;
    	if (isReadLeftOfMate(record) && isReadForward(record) && isMateReverse(record)) {
    		result = true;
    	} else if (isReadRightOfMate(record) && isReadReverse(record) && isMateForward(record)) {
    		result = true;
    	}
    	return result;
    }

    /**
     * Checks if pair orientation is outward
     * @return true if outward
     */
    public boolean isOutward() {
    	return isOutward(record);
//        boolean result = false;
//        if (isReadRightOfMate() && isReadForward() && isMateReverse()) {
//            result = true;
//        } else if (isReadLeftOfMate() && isReadReverse() && isMateForward()) {
//            result = true;
//        }
//        return result;
    }
    public static boolean isOutward(SAMRecord record) {
    	boolean result = false;
    	if (isReadRightOfMate(record) && isReadForward(record) && isMateReverse(record)) {
    		result = true;
    	} else if (isReadLeftOfMate(record) && isReadReverse(record) && isMateForward(record)) {
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
    public static boolean isDistanceTooSmall(int absoluteISize, int isizeLowerLimit) {
    	return 0 <= absoluteISize && absoluteISize < isizeLowerLimit;
    }

    /**
     * Insert size falls within the upper and lower limits
     * @return true if successful
     */
    public boolean isDistanceNormal() {
    	return isDistanceNormal(Math.abs(record.getInferredInsertSize()), isizeLowerLimit, isizeUpperLimit);
    }
    
    public static boolean isDistanceNormal(int absoluteISize, int isizeLowerLimit, int isizeUpperLimit) {
    	return isizeLowerLimit <= absoluteISize
    			&& absoluteISize <= isizeUpperLimit;
    }

    /**
     * Insert size  is larger than upper limit 
     * @return true if successful
     */
    public boolean isDistanceTooLarge() {
    	return isDistanceTooLarge(Math.abs(record.getInferredInsertSize()), isizeUpperLimit);
    }
    public static boolean isDistanceTooLarge(int absoluteISize, int isizeUpperLimit) {
    	return absoluteISize > isizeUpperLimit;
    }

    public String getZPAnnotation() {
        return this.zpAnnotation;
    }

    public void setZPAnnotation(String zp) {
    	   this.zpAnnotation = zp;     
        
    }

}
