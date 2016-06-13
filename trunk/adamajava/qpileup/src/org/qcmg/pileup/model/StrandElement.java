/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import org.qcmg.pileup.QPileupException;

public class StrandElement {

	final String name;
	int [] intDataMembers = null;
	long [] longDataMembers = null;
	final boolean isLong;
	
	public StrandElement(String name, boolean isLong) {
		this.name = name;
		this.isLong = isLong;
	}
	
	public StrandElement(String name, int datasetLength, boolean isLong) {
		this.name = name;
		this.isLong = isLong;
		if (isLong) {
			this.longDataMembers = new long[datasetLength];
		} else {
			this.intDataMembers = new int [datasetLength];
		}
	}

	public String getName() {
		return name;
	}

//	public void setName(String name) {
//		this.name = name;
//	}

	public int[] getIntDataMembers() {
		return intDataMembers;
	}

	public void setIntDataMembers(int[] intDataMembers) {
		this.intDataMembers = intDataMembers;
	}

	public long[] getLongDataMembers() {
		return longDataMembers;
	}

	public void setLongDataMembers(long[] longDataMembers) {
		this.longDataMembers = longDataMembers;
	}

	public boolean isLong() {
		return isLong;
	}

//	public void setLong(boolean isLong) {
//		this.isLong = isLong;
//	}
	
	public void addElement(int index, int size) {		
			if (isLong) {
				longDataMembers[index] += size;
			} else {
				intDataMembers[index] += size;
			}		
	}
	
	public void removeElement(int index, int size) {
		if (isLong) {
			longDataMembers[index] -= size;
		} else {
			intDataMembers[index] -= size;
		}
	}
	
	public void addElement(int index, long size) {
		if (isLong) {
			longDataMembers[index] += size;
		} else {
			intDataMembers[index] += size;
		}
	}
	
	public void removeElement(int index, long size) {
		if (isLong) {
			longDataMembers[index] -= size;
		} else {
			intDataMembers[index] -= size;
		}
	}

	public Object getStrandElementMember(int index) {
		if (isLong) {
			return longDataMembers[index];
		} else {
			return intDataMembers[index];
		}
	}

	public void mergeElement(StrandElement strandElement) throws QPileupException {
		if (!this.name.equals(strandElement.getName()) || this.isLong != strandElement.isLong()) {
			String error = "Name: " + this.name + " vs " + strandElement.getName() + " isLongMember" + this.isLong + " vs " + strandElement.isLong(); 
			throw new QPileupException("STRAND_ELEMENT_MISMATCH", error);
		}
		if (isLong) {
			long[] newLongDataMembers = strandElement.getLongDataMembers();
			for (int i=0; i<longDataMembers.length; i++) {
				longDataMembers[i] += newLongDataMembers[i];
			}
		} else {			
			int[] newIntDataMembers = strandElement.getIntDataMembers();
			for (int i=0; i<intDataMembers.length; i++) {
				intDataMembers[i] += newIntDataMembers[i];				
			}
		}
				
	}

	public long getLongElementValue(int index) {
		return longDataMembers[index];		
	}	

	public int getIntElementValue(int index) {
		return intDataMembers[index];		
	}
}
