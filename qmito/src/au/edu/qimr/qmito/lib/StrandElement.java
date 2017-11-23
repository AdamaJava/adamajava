/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito.lib;

import au.edu.qimr.qmito.Messages;;

public class StrandElement {

	String name;
	int [] intDataMembers = null;
	long [] longDataMembers = null;
	boolean isLong = false;
	
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

	public int[] getIntDataMembers() {
		return intDataMembers;
	}

	public long[] getLongDataMembers() {
		return longDataMembers;
	}

	public boolean isLong() {
		return isLong;
	}

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

//	public void mergeElement(StrandElement strandElement) throws Exception {
//		if (!this.name.equals(strandElement.getName()) || this.isLong != strandElement.isLong()) {
//			String error = "Name: " + this.name + " vs " + strandElement.getName() + " isLongMember" + this.isLong + " vs " + strandElement.isLong(); 
//			throw new Exception(Messages.getMessage("STRAND_ELEMENT_MISMATCH", error));
//		}
//		if (isLong) {
//			long[] newLongDataMembers = strandElement.getLongDataMembers();
//			for (int i=0; i<longDataMembers.length; i++) {
//				longDataMembers[i] += newLongDataMembers[i];
//			}
//		} else {			
//			int[] newIntDataMembers = strandElement.getIntDataMembers();
//			for (int i=0; i<intDataMembers.length; i++) {
//				intDataMembers[i] += newIntDataMembers[i];				
//			}
//		}
//				
//	}

	public long getLongElementValue(int index) {
		return longDataMembers[index];		
	}	

	public int getIntElementValue(int index) {
		return intDataMembers[index];		
	}
}
