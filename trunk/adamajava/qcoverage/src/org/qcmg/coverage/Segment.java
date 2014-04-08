/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import org.qcmg.common.model.ChrPosition;

public class Segment implements Comparable<Segment>{
	
	ChrPosition position;
	String[] fields;
	Feature feature;	
	
	public Segment(String[] fields, Feature feature, int recordCount) {
		super();
		this.position =  new ChrPosition(fields[0], new Integer(fields[3]),  new Integer(fields[4]), Integer.toString(recordCount));
		this.fields = fields;
		this.feature = feature;
	}
	public String[] getFields() {
		return fields;
	}
	public void setFields(String[] fields) {
		this.fields = fields;
	}
	public Feature getFeature() {
		return feature;
	}
	public void setFeature(Feature feature) {
		this.feature = feature;
	}
	@Override
	public int compareTo(Segment o) {
		return this.position.compareTo(o.position);
	}
	public int getPositionStart() {
		return position.getPosition();
	} 
	public int getPositionEnd() {
		return position.getEndPosition();
	}
	public String getPositionString() {
		return position.getChromosome() + ":" + position.getPosition() + "-" + position.getEndPosition();
	}
	public void setPositionStart(int positionStart) {
		position.setPosition(positionStart);
		
	}
	public void setPositionEnd(int positionEnd) {
		position.setEndPosition(positionEnd);		
	} 
	public String toString() {
		String result = "";
		fields[0] = position.getChromosome();
		fields[3] = Integer.toString(position.getPosition());
		fields[4] =  Integer.toString(position.getEndPosition());
		for (String field : fields) {
			result += field + "\t";
		}
		result += "\n";
		return result;
	}
	
	
	

}
