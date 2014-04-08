/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import net.sf.samtools.SAMRecord;

public class Mate {

    private String readName;
    private int start;
    private int end;
    private String referenceName;
    private boolean negOrientation;
	private int flags;
	private String zp;

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
        this.zp = (String) samRecord.getAttribute("ZP");
        
        if (this.zp.equals("C**")) {
        	this.zp = "Cxx";
        }
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getReadName() {
        return readName;
    }

    public void setReadName(String name) {
        this.readName = name;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
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

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public String getZp() {
		return zp;
	}

	public void setZp(String zp) {
		this.zp = zp;
	}

	public void setNegOrientation(boolean negOrientation) {
		this.negOrientation = negOrientation;
	}


}
