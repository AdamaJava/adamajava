/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import org.qcmg.qsv.blat.BLATRecord;

public class SplitReadAlignment {
	
	String strand;
	Integer startPos;
	Integer endPos;
	Integer queryStart;
	Integer queryEnd;
	private String reference;


	public SplitReadAlignment(String reference, String strand, Integer startPos, Integer endPos,
			Integer queryStart, Integer queryEnd) {
		super();
		this.reference = reference;
		this.strand = strand;
		this.startPos = startPos;
		this.endPos = endPos;
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	
	public SplitReadAlignment(BLATRecord r) {
		this.reference = r.getReference();
		this.strand = r.getStrand();
		this.startPos = r.getStartPos();
		this.endPos = r.getEndPos();
		this.queryStart = r.getQueryStart();
		this.queryEnd = r.getQueryEnd();
	}
	
	public boolean strandPositive() {
		if (strand.equals("+")) {
			return true;
		}
		return false;
	}
	
	public boolean strandNegative() {
		if (strand.equals("-")) {
			return true;
		}
		return false;
	}

	public String getStrand() {
		return strand;
	}

	public void setStrand(String strand) {
		this.strand = strand;
	}

	public Integer getStartPos() {
		return startPos;
	}

	public void setStartPos(Integer startPos) {
		this.startPos = startPos;
	}

	public Integer getEndPos() {
		return endPos;
	}

	public void setEndPos(Integer endPos) {
		this.endPos = endPos;
	}

	public Integer getQueryStart() {
		return queryStart;
	}

	public void setQueryStart(Integer queryStart) {
		this.queryStart = queryStart;
	}

	public Integer getQueryEnd() {
		return queryEnd;
	}

	public void setQueryEnd(Integer queryEnd) {
		this.queryEnd = queryEnd;
	}	
	
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
	
	@Override
	public String toString() {
		return reference + "\t" + strand + "\t" + startPos + "\t" + endPos + "\t" + queryStart + "\t" + queryEnd;
	}

	public int getInsertSize() {
		return queryEnd - queryStart;
	}
	
    @Override
    public boolean equals(final Object o) {
    	
    	if (o == null) {
    		return false;
    	}
       
        if (!(o instanceof SplitReadAlignment)) return false;
        
        final SplitReadAlignment align = (SplitReadAlignment) o;
        
        if (reference.equals(align.getReference())) {
        	if (startPos.equals(align.getStartPos())) {
        		if(endPos.equals(align.getEndPos())) {
        			if (queryStart.equals(align.getQueryStart())) {
        				if (queryEnd.equals(align.getQueryEnd())) {
        					return true;
        				}
        			}
        		}
        	}
        }
        return false;
    }
    
    @Override
    public int hashCode() {
       return 31*reference.hashCode() + startPos.hashCode() + endPos.hashCode()
    		   + queryStart.hashCode() + queryEnd.hashCode();
    }
	
	

}
