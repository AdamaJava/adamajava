/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import net.sf.samtools.SAMRecord;

/**
 * Class for discordant pair annotation for discordant paired end
 * 
 *
 */
public class SOLiDLongMatePairRecord {
    
    private final SAMRecord record;
    private String zpAnnotation;
    private final int isizeUpperLimit;
    private final int isizeLowerLimit;
    
    public SOLiDLongMatePairRecord (SAMRecord record, int isizeLowerLimit, int isizeUpperLimit) {
        this.record = record;
        this.isizeLowerLimit = isizeLowerLimit;
        this.isizeUpperLimit = isizeUpperLimit;
    }
    
    /**
	 * Create ZP annotation for the record (ZP=discordant pair classification)
	 */
    public void createZPAnnotation() {
        Integer nh = record.getIntegerAttribute("NH");
        if (!record.getDuplicateReadFlag()) {
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
            } else if (null != nh && 1 == nh && isSameStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                record.setAttribute("ZP", zpAnnotation);
            } else if (null != nh && 1 != nh && isSameStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if (zpAnnotation.equals("AAA")) {
                    record.setAttribute("ZP", zpAnnotation);
                } else {
                    record.setAttribute("ZP", "Z**");
                }
            } else if (null == nh && isSameStrand()) {
                zpAnnotation = "A";
                handleOrientation();
                handleIntervalSize();
                if (zpAnnotation.equals("AAA")) {
                    record.setAttribute("ZP", zpAnnotation);
                } else {
                    record.setAttribute("ZP", "Z**");
                }
            } else if (null != nh && 1 == nh && isDifferentStrand()) {
                zpAnnotation = "B";
                handleOrientation();
                handleIntervalSize();
                record.setAttribute("ZP", zpAnnotation);
            } else {
                record.setAttribute("ZP", "Z**");
            }
        } else {
            record.setAttribute("ZP", "V**");
        }
    }

    public boolean isReadF3() {
        return record.getFirstOfPairFlag();
    }

    public boolean isReadR3() {
        return record.getSecondOfPairFlag();
    }

    public boolean isReadLeftOfMate() {
        return record.getAlignmentStart() < record.getMateAlignmentStart();
    }

    public boolean isReadRightOfMate() {
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

    public boolean isR3ToF3() {
        boolean result = false;
        if (isReadR3() && isReadLeftOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadR3() && isReadRightOfMate() && isReadReverse()
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

    public boolean isF3ToR3() {
        boolean result = false;
        if (isReadF3() && isReadLeftOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadF3() && isReadRightOfMate() && isReadReverse()
                && isMateReverse()) {
            result = true;
        } else if (isReadR3() && isReadRightOfMate() && isReadForward()
                && isMateForward()) {
            result = true;
        } else if (isReadR3() && isReadLeftOfMate() && isReadReverse()
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
    

    public String handleOrientation() {
        if ('A' == zpAnnotation.charAt(0)) {
            if (isR3ToF3()) {
                zpAnnotation += 'A';
            } else if (isF3ToR3()) {
                zpAnnotation += 'B';
            } else {
                zpAnnotation += 'X';
            }
        } else if ('B' == zpAnnotation.charAt(0)) {
            if (isOutward()) {
                zpAnnotation += 'A';
            } else if (isInward()) {
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
    public String handleIntervalSize() {
        
        if (isDistanceNormal()) {
            zpAnnotation += 'A';
        } else if (isDistanceTooSmall()) {
            zpAnnotation += 'B';
        } else if (isDistanceTooLarge()) {
            zpAnnotation += 'C';
        }
        return zpAnnotation;
    }

    public String getZPAnnotation() {
        return (String) record.getAttribute("ZP");
    }   

    public void setZPAnnotation(String zp) {
        this.zpAnnotation = zp;        
    }
}
