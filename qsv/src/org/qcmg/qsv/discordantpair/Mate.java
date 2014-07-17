/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import net.sf.samtools.SAMRecord;

public class Mate {

    private final String readName;
    private final int start;
    private final int end;
    private final String referenceName;
    private final boolean negOrientation;
	private final int flags;
	private final String zp;

    public Mate(String name, String referenceName, int start, int end, String zp, int flags, boolean negativeOrientation) {
        this.readName = name;
        this.start = start;
        this.end = end;
        this.referenceName = referenceName;
        this.negOrientation = negativeOrientation;
        this.flags = flags;
        this.zp = zp;
    }

    public Mate(SAMRecord samRecord) {
        this.readName = samRecord.getReadName() + ":" + samRecord.getReadGroup().getId();
        this.start = samRecord.getAlignmentStart();
        this.end = samRecord.getAlignmentEnd();
        this.referenceName = samRecord.getReferenceName();
        this.negOrientation = samRecord.getReadNegativeStrandFlag();
        this.flags = samRecord.getFlags();
        
        String tmpZP = (String) samRecord.getAttribute("ZP");
        this.zp = tmpZP.equals("C**") ? "Cxx" : tmpZP;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public String getReadName() {
        return readName;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
 
    public boolean getNegOrientation() {
        return negOrientation;
    }
    
    public String getStrand() {
    	if (this.negOrientation) {
    		return "-";
    	} else {
    		return "+";
    	}
    }
    
	public int getFlags() {
		return flags;
	}

	public String getZp() {
		return zp;
	}
}
