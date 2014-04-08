/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import org.qcmg.qsv.util.QSVUtil;

import net.sf.samtools.SAMRecord;

public class Clip implements Comparable<Clip>{
	
	private String reference;
	private Integer bpPos;
	private int length;
	private String readName;
	private String clipSequence;
	private boolean isReverse = false;
	private boolean isLeft = false;
	private String side;
	private String referenceSequence;
	private String readSequence;

	public Clip(SAMRecord record, int bpPos, String sequence, String aligned, String side) {
		this.readName = record.getReadName() + ":";
		if (record.getReadGroup() != null) {
			this.readName += record.getReadGroup().getId();
		}
		this.readSequence = record.getReadString();
		this.reference = record.getReferenceName();
		this.bpPos = bpPos;
		this.length = sequence.length();
		this.clipSequence = sequence;
		this.referenceSequence = aligned;
		this.isReverse = record.getReadNegativeStrandFlag();
		this.side = side;
		if (this.side.equals("left")) {
			isLeft = true;
		}
	}

	public Clip(String line) {
		String[] values = line.split(",");
		try {
		this.readName = values[0];
		this.reference = values[1];
		this.bpPos = new Integer(values[2]);
		if (values[3].equals("-")) {
			isReverse = true;
		} 
		this.side = values[4];
		if (this.side.equals("left")) {
			isLeft = true;
		}
		this.readSequence = values[5];
		this.clipSequence = values[6];
		this.length = clipSequence.length();
		this.referenceSequence = values[7];
		} catch (Exception e) {
			System.out.println(line + " " + values.length);
		}
	}

	public String getReference() {
		return reference;
	}


	public void setReference(String reference) {
		this.reference = reference;
	}
	
	public boolean isLeft() {
		return isLeft;
	}


	public Integer getLength() {
		return length;
	}


	public void setLength(Integer length) {
		this.length = length;
	}


	public String getReadName() {
		return readName;
	}


	public void setReadName(String readName) {
		this.readName = readName;
	}


	public String getClipSequence() {
		return clipSequence;
	}


	public void setClipSequence(String sequence) {
		this.clipSequence = sequence;
		this.length = sequence.length();
	}


	public boolean isReverse() {
		return isReverse;
	}


	public void setReverse(boolean isReverse) {
		this.isReverse = isReverse;
	}
	
	public String getStrand() {
		if (isReverse) {
			return "-";
		} 
		return "+";
	}
	
	public void setBpPos(Integer bpPos) {
		this.bpPos = bpPos;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setLeft(boolean isLeft) {
		this.isLeft = isLeft;
	}

	public void setSide(String side) {
		this.side = side;
	}
	

	public boolean getIsReverse() {
		return this.isReverse;
	}

	public String getSide() {
		return this.side;
	}
	
	public String getReferenceSequence() {
		return referenceSequence;
	}

	public void setReferenceSequence(String referenceSequence) {
		this.referenceSequence = referenceSequence;
	}
	
	
	public void setReadSequence(String readSequence) {
		this.readSequence = readSequence;
	}


	
	public String toString() {
		return this.readName + "," + this.reference + "," + this.bpPos + "," + getStrand() + "," + this.side + "," + this.readSequence + "," + this.clipSequence + "," + this.referenceSequence +  QSVUtil.getNewLine();		
	}

	@Override
	public int compareTo(Clip other) {
		if (this.bpPos.equals(other.getBpPos())) {
			return this.readName.compareTo(other.getReadName());
		} else {
			return this.bpPos.compareTo(other.getBpPos());
		}
	}
	
	public Integer getBpPos() {
		return this.bpPos;
	}

	@Override
	public boolean equals(Object aThat) {
		
		if (this == aThat)
			return true;

		if (!(aThat instanceof Clip))
			return false;

		Clip other = (Clip) aThat;

		if (this.bpPos.equals(other.getBpPos())) {
			return this.readName.equals(other.getReadName());
		} else {			
			return this.bpPos.equals(other.getBpPos());
		}
	}
	
	@Override
	public int hashCode() {
		
		final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.readName == null) ? 0 : readName.hashCode());
        result += prime * result
                + ((this.bpPos == null) ? 0 : bpPos.hashCode());
        
        return result;
	}


	public void getClipBases(int[][] bases) {
		
		if (this.isLeft) {	
			int index = bases.length - length -1;			
			for (int i=0; i<length; i++) {
				int base = clipSequence.charAt(i);
				index++;
				addBase(index, base, bases);
			}
		} else {
			for (int i=0; i<length; i++) {
				int base = clipSequence.charAt(i);
				addBase(i, base, bases);
			}
		}		
	}
	
	public void getReferenceBases(int[][] bases) {
		if (this.isLeft) {	
			for (int i=0; i<referenceSequence.length(); i++) {
				int base = referenceSequence.charAt(i);
				addBase(i, base, bases);
			}
		} else {			
			int index = bases.length - referenceSequence.length() -1;
			for (int i=0; i<referenceSequence.length(); i++) {
				int base = referenceSequence.charAt(i);
				index++;				
				addBase(index, base, bases);
			}
		}		
	}

	private void addBase(int index, int base, int[][] bases) {
		//ACTGN
		if (base == 65) {
			bases[index][0] += 1;
		} else if (base == 67) {
			bases[index][1] += 1;
		} else if (base == 84) {
			bases[index][2] += 1;
		} else if (base == 71) {
			bases[index][3] += 1;
		} else {
			bases[index][4] += 1;
		}		
	}

	public String getReadSequence() {
		return this.readSequence;
	}

}


