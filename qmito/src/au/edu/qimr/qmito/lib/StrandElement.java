/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito.lib;

public class StrandElement {

//	private final String name;
	private int [] intDataMembers = null;
	private long [] longDataMembers = null;
	private boolean isLong = false;
	
	public StrandElement(String name, boolean isLong) {
//		this.name = name;
		this.isLong = isLong;
	}
	
	public StrandElement(String name, int datasetLength, boolean isLong) {
//		this.name = name;
		this.isLong = isLong;
		if (isLong) {
			this.longDataMembers = new long[datasetLength];
		} else {
			this.intDataMembers = new int [datasetLength];
		}
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

	public long getLongElementValue(int index) {
		return longDataMembers[index];		
	}	

	public int getIntElementValue(int index) {
		return intDataMembers[index];		
	}
}
