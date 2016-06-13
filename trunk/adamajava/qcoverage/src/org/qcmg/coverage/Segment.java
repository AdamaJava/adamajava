/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;

public class Segment implements Comparable<Segment>{
	
	private final ChrPosition position;
	private final int recordCount;
	private final String[] fields;
	private final Feature feature;	
	
	public Segment(String[] fields, Feature feature, int recordCount) {
		this(fields, feature, new ChrRangePosition(fields[0], Integer.parseInt(fields[3]),  Integer.parseInt(fields[4])), recordCount);
	}
	public Segment(String[] fields, Feature feature, ChrPosition chrPos, int recordCount) {
		super();
		this.position =  chrPos;
		this.fields = fields;
		this.feature = feature;
		this.recordCount = recordCount;
	}
	public String[] getFields() {
		return fields;
	}
	public ChrPosition getPosition() {
		return position;
	}
	public Feature getFeature() {
		return feature;
	}
	@Override
	public int compareTo(Segment o) {
		return ((ChrRangePosition) this.position).compareTo(o.position);
	}
	public int getPositionStart() {
		return position.getStartPosition();
	}
	public int getPositionEnd() {
		return position.getEndPosition();
	}
	public int getRecordCount() {
		return recordCount;
	}
	public String getPositionString() {
		return position.getChromosome() + ":" + position.getStartPosition() + "-" + position.getEndPosition();
	}
	@Override
	public String toString() {
		String result = "";
		fields[0] = position.getChromosome();
		fields[3] = Integer.toString(position.getStartPosition());
		fields[4] =  Integer.toString(position.getEndPosition());
		for (String field : fields) {
			result += field + "\t";
		}
		result += "\n";
		return result;
	}
	
}
