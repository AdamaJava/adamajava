/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import htsjdk.samtools.SAMRecord;

import org.qcmg.qsv.discordantpair.PairGroup;

/**
 * Class for discordant pair annotation for illumina long mate pair
 * 
 *
 */
public class IlluminaLongMatePairRecord {
    
    private final SAMRecord record;
    private String zpAnnotation;
    private final int isizeUpperLimit;
    private final int isizeLowerLimit;
    private final int nullNHZ = 0;
    private int endZ = 0;
    private int sameStrandZ = 0;
	private int diffStrandNullZ = 0;
    private int diffStrandZ = 0;
	private int sameStrandNullZ = 0;
	private boolean abbConverted = false;


	public IlluminaLongMatePairRecord (SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
        this.record = record;
        this.isizeLowerLimit = isizeLowerLimit;
        this.isizeUpperLimit = isizeUpperLimit;
        this.zpAnnotation = "";
    }
    
	/**
	 * Create ZP annotation for the record (ZP=discordant pair classification)
	 */
	public void createZPAnnotation() {
        
        if (!record.getDuplicateReadFlag()) {
        	
        	Integer nh = record.getIntegerAttribute("NH");
        	
            if (null != nh && 1 == nh && record.getMateUnmappedFlag()) {
                zpAnnotation = "D**";
                record.setAttribute("ZP", zpAnnotation);                
            } else if (null != nh
                    && 1 == nh
                    && !record.getReferenceName().equals(
                            record.getMateReferenceName())
                    && !record.getMateReferenceName().equals("=")) {            	
                zpAnnotation = "C**";
                record.setAttribute("ZP", zpAnnotation);
                
            } else if (null != nh && 1 == nh
                    && record.getReadFailsVendorQualityCheckFlag()) {
                zpAnnotation = "E**";
                record.setAttribute("ZP", zpAnnotation);
                
            } else if (null != nh && 1 == nh && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                record.setAttribute("ZP", zpAnnotation);                
            } else if (null != nh && 1 != nh && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if (zpAnnotation.equals("AAA")) {
                    record.setAttribute("ZP", zpAnnotation);
                } else {
                	diffStrandZ += 1;
                	zpAnnotation = "Z**";
                    record.setAttribute("ZP", "Z**");
                }                
            } else if (null == nh && isDifferentStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if (zpAnnotation.equals("AAA")) {
                    record.setAttribute("ZP", zpAnnotation);
                } else {      
                	diffStrandNullZ += 1;
                	zpAnnotation = "Z**";
                    record.setAttribute("ZP", "Z**");
                }
                
            } else if (null != nh && 1 == nh && isSameStrand()) {
                zpAnnotation = "B";
                handleOrientation();
                handleIntervalSize();
                record.setAttribute("ZP", zpAnnotation);                
            } else if (null == nh && isSameStrand()) {
            	zpAnnotation = "Z**";
                record.setAttribute("ZP", "Z**"); 
                sameStrandNullZ  += 1;               
            } else {
           	 	sameStrandZ += 1;
           	 zpAnnotation = "Z**";
                record.setAttribute("ZP", "Z**");                
            }
        } else {
        	endZ += 1;
        	zpAnnotation = "Z**";
            record.setAttribute("ZP", "Z**");            
        }
        
        //***Due to the present of incorrect orientations, the ABBs must become AAAs
        if (zpAnnotation.equals(PairGroup.ABB.toString())) {
        	zpAnnotation = "AAA";
        	record.setAttribute("ZP", "AAA");        
        	abbConverted = true;
        }
    }
    
    public boolean isAbbConverted() {
		return abbConverted;
	}

	public void setAbbConverted(boolean abbConverted) {
		this.abbConverted = abbConverted;
	}

	/**
	 * Determine orientation of the record
	 * @return current zp annotation
	 */
	public String handleOrientation() {
    	//opposite to illumina paired end
        if ('A' == zpAnnotation.charAt(0)) {
        	
            if (isInward()) {
                zpAnnotation += 'B';
            } else if (isOutward()) {
                zpAnnotation += 'A';
            } else {
                zpAnnotation += 'X';
            }
        } else if ('B' == zpAnnotation.charAt(0)) {
            if (isF5toF3()) {
                zpAnnotation += 'A';
            } else if (isF3toF5()) {
                zpAnnotation += 'B';
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
        return !record.getReadNegativeStrandFlag();
    }

    public boolean isReadReverse() {
        return !isReadForward();
    }

    public boolean isMateForward() {
        return !record.getMateNegativeStrandFlag();
    }

    public boolean isMateReverse() {
        return !isMateForward();
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
        return record.getReadNegativeStrandFlag() != record
                .getMateNegativeStrandFlag();
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
    
    public int getNullNHZ() {
		return nullNHZ;
	}

	public int getEndZ() {
		return endZ;
	}

	public int getSameStrandZ() {
		return sameStrandZ;
	}

	public int getDiffStrandNullZ() {
		return diffStrandNullZ;
	}

	public int getDiffStrandZ() {
		return diffStrandZ;
	}

    public int getSameStrandNullZ() {
		return sameStrandNullZ;
	}

}
