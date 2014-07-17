/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import net.sf.samtools.SAMRecord;

import org.qcmg.qsv.util.QSVUtil;

public class Clip implements Comparable<Clip>{
	
	private final String reference;
	private final int bpPos;
	private final int length;
	private final String readName;
	private final String clipSequence;
	private final boolean isReverse;
	private final boolean isLeft;
	private final String referenceSequence;
	private final String readSequence;

	public Clip(SAMRecord record, int bpPos, String sequence, String aligned, String side) {
		this.readName = record.getReadName() + ":" + (record.getReadGroup() != null ? record.getReadGroup().getId() : "");
		this.readSequence = record.getReadString();
		this.reference = record.getReferenceName();
		this.bpPos = bpPos;
		this.length = sequence.length();
		this.clipSequence = sequence;
		this.referenceSequence = aligned;
		this.isReverse = record.getReadNegativeStrandFlag();
		this.isLeft = side.equals("left");
	}

	public Clip(String line) {
		String[] values = line.split(",");
//		try {
		this.readName = values[0];
		this.reference = values[1];
		this.bpPos = Integer.parseInt(values[2]);
		this.isReverse = values[3].equals("-");
		this.isLeft = values[4].equals("left");
		this.readSequence = values[5];
		this.clipSequence = values[6];
		this.length = clipSequence.length();
		this.referenceSequence = values[7];
//		} catch (Exception e) {
//			System.out.println(line + " " + values.length);
//		}
	}

	public String getReference() {
		return reference;
	}

	public boolean isLeft() {
		return isLeft;
	}

	public Integer getLength() {
		return length;
	}

	public String getReadName() {
		return readName;
	}

	public String getClipSequence() {
		return clipSequence;
	}

	public boolean isReverse() {
		return isReverse;
	}

	public String getStrand() {
		if (isReverse) {
			return "-";
		} 
		return "+";
	}
	
	public boolean getIsReverse() {
		return this.isReverse;
	}

	public String getReferenceSequence() {
		return referenceSequence;
	}

	@Override
	public String toString() {
		return this.readName + "," + this.reference + "," + this.bpPos + "," + getStrand() + "," + (this.isLeft ? "left" : "right") + "," + this.readSequence + "," + this.clipSequence + "," + this.referenceSequence +  QSVUtil.getNewLine();		
	}

	@Override
	public int compareTo(Clip other) {
		int diff = Integer.compare(this.bpPos, other.getBpPos());
		if (diff == 0) {
			return this.readName.compareTo(other.getReadName());
		} else {
			return diff;
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

		int diff = Integer.compare(bpPos, other.getBpPos());
		if (diff == 0) {
			return this.readName.equals(other.getReadName());
		} else {			
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		
		final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.readName == null) ? 0 : readName.hashCode());
        result += prime * result + this.bpPos;
        
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
