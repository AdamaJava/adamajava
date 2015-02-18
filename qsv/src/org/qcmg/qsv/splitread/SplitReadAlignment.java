/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.util.QSVUtil;

public class SplitReadAlignment {
	
	private final boolean  positiveStrand;
	private final int startPos;
	private final int endPos;
	private final int queryStart;
	private final int queryEnd;
	private final String reference;


	public SplitReadAlignment(String reference, char strand, Integer startPos, Integer endPos,
			Integer queryStart, Integer queryEnd) {
		super();
		this.reference = reference;
		this.positiveStrand = strand == QSVUtil.PLUS;
		this.startPos = startPos;
		this.endPos = endPos;
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	
	public SplitReadAlignment(BLATRecord r) {
		this.reference = r.getReference();
		this.positiveStrand = r.getStrand()  == QSVUtil.PLUS;
		this.startPos = r.getStartPos();
		this.endPos = r.getEndPos();
		this.queryStart = r.getQueryStart();
		this.queryEnd = r.getQueryEnd();
	}
	
	public boolean strandPositive() {
		return positiveStrand;
	}
	
	public boolean strandNegative() {
		return ! positiveStrand;
	}

	public char getStrand() {
		return positiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS;
	}

	public Integer getStartPos() {
		return startPos;
	}

	public Integer getEndPos() {
		return endPos;
	}

	public Integer getQueryStart() {
		return queryStart;
	}

	public Integer getQueryEnd() {
		return queryEnd;
	}
	
	public String getReference() {
		return reference;
	}
	
	@Override
	public String toString() {
		return reference + "\t" + getStrand() + "\t" + startPos + "\t" + endPos + "\t" + queryStart + "\t" + queryEnd;
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
        	if (startPos == align.getStartPos()) {
        		if(endPos == align.getEndPos()) {
        			if (queryStart == align.getQueryStart()) {
        				if (queryEnd == align.getQueryEnd()) {
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
       return 31*reference.hashCode() + startPos + endPos + queryStart + queryEnd;
    }

}
