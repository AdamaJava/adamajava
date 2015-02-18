/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import org.qcmg.qsv.util.QSVConstants;


public class StructuralVariant {
	
	String leftReference = null;
	String rightReference = null;
	Integer leftBreakpoint = null;
	Integer rightBreakpoint = null;
	String orientationCategory = null;
	
	public StructuralVariant() {
		
	}
	
	public StructuralVariant(String leftReference, String rightReference,
			Integer leftBreakpoint, Integer rightBreakpoint,
			String orientationCategory) {
		this.leftReference = leftReference;
		this.rightReference = rightReference;
		this.leftBreakpoint = leftBreakpoint;
		this.rightBreakpoint = rightBreakpoint;
		this.orientationCategory = orientationCategory;
	}

	public String getLeftReference() {
		return leftReference;
	}

	private void setLeftReference(String leftReference) {
		this.leftReference = leftReference;
	}

	public String getRightReference() {
		return rightReference;
	}

	private void setRightReference(String rightReference) {
		this.rightReference = rightReference;
	}

	public Integer getLeftBreakpoint() {
		return leftBreakpoint;
	}

	public void setLeftBreakpoint(Integer leftBreakpoint) {
		this.leftBreakpoint = leftBreakpoint;
	}

	public Integer getRightBreakpoint() {
		return rightBreakpoint;
	}

	public void setRightBreakpoint(Integer rightBreakpoint) {
		this.rightBreakpoint = rightBreakpoint;
	}

	public String getOrientationCategory() {
		return orientationCategory;
	}

	private void setOrientationCategory(String orientationCategory) {
		this.orientationCategory = orientationCategory;
	}
	
	@Override
	public String toString() {
		return leftReference + "_" + leftBreakpoint + "_" + rightReference + "_" + rightBreakpoint + "_"+ orientationCategory; 
	}

	public boolean equalReference() {
		return leftReference.equals(rightReference);
	}

	public void setReferences(String left, String right) {
		setLeftReference(left);
		setRightReference(right);
	}

	public void setBreakpointsByCategory(String orientation, Integer leftBP,
			Integer rightBP) {
		setOrientationCategory(orientation);
		setLeftBreakpoint(leftBP);
		setRightBreakpoint(rightBP);		
	}

	public void swap() {
		Integer tmpInt = leftBreakpoint;
		leftBreakpoint = rightBreakpoint;					
		rightBreakpoint = tmpInt;
		swapReference();		
	}

	public String toSplitReadString() {
		return orientationCategory + " | " + leftReference + ":" + leftBreakpoint + " | " + rightReference + ":" + rightBreakpoint;
	}

	public boolean splitReadEquals(StructuralVariant knownSV, String confidenceLevel, int buffer) {
		
		
		if (orientationCategory.equals(knownSV.getOrientationCategory()) || confidenceLevel.equals(QSVConstants.LEVEL_SINGLE_CLIP)) {			
			if (leftReference.equals(knownSV.getLeftReference()) && (rightReference.equals(knownSV.getRightReference()))) {				
				if (leftBreakpoint >= knownSV.getLeftBreakpoint() - buffer && leftBreakpoint <= knownSV.getLeftBreakpoint()  + buffer) {					
					if (rightBreakpoint >= knownSV.getRightBreakpoint() - buffer && rightBreakpoint <= knownSV.getRightBreakpoint() + buffer) {						
						return true;
					}
				}
			}						
		}
		return false;
	}

	public void swapReference() {
		String tempRef = leftReference;
		leftReference = rightReference;
		rightReference = tempRef;
		
	}
	
	
}
