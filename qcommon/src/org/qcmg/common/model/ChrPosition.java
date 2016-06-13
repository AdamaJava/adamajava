/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.model;

public interface ChrPosition {
	
	String getChromosome();
	int getStartPosition();
	
	/*
	 * Default methods - don't need to override these unless required
	 */
	default int getEndPosition() {
		return getStartPosition();
	}
	default int getLength() {
		return (getEndPosition() - getStartPosition()) + 1;
	}
	default String getName() {
		return null;
	}
	default boolean isPointPosition() {
		return getStartPosition() == getEndPosition();
	}
	default boolean isRangePosition(){
		return getEndPosition() > getStartPosition();
	}
	default String toIGVString() {
		return getChromosome() + ":" + getStartPosition() + "-" + getEndPosition();
	}

}
