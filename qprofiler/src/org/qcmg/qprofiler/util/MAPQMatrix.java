/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qprofiler.util;

import org.qcmg.qvisualise.util.SummaryByCycleUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class MAPQMatrix {
	
	public enum MatrixType{
		SM,CM,LENGTH,NH,ZM
	}
	
	private final ConcurrentMap<Integer, AtomicLong> smValues = new ConcurrentSkipListMap<>();
	private final ConcurrentMap<Integer, AtomicLong> cmValues = new ConcurrentSkipListMap<>();
	private final ConcurrentMap<Integer, AtomicLong> lengthValues = new ConcurrentSkipListMap<>();
	private final ConcurrentMap<Integer, AtomicLong> nhValues = new ConcurrentSkipListMap<>();
	private final ConcurrentMap<Integer, AtomicLong> zmValues = new ConcurrentSkipListMap<>();

	public void addToMatrix(Integer value, MatrixType type) {
		switch (type) {
		case SM:
			SummaryByCycleUtils.incrementCount(smValues, value);
			break;
		case CM:
			SummaryByCycleUtils.incrementCount(cmValues, value);
			break;
		case LENGTH:
			SummaryByCycleUtils.incrementCount(lengthValues, value);
			break;
		case NH:
			SummaryByCycleUtils.incrementCount(nhValues, value);
			break;
		case ZM:
			SummaryByCycleUtils.incrementCount(zmValues, value);
			break;
		}
	}
	
	public Map<Integer, AtomicLong> getMatrixByType(MatrixType type) {
        return switch (type) {
            case SM -> smValues;
            case CM -> cmValues;
            case LENGTH -> lengthValues;
            case NH -> nhValues;
            case ZM -> zmValues;
        };
    }

	
	///////////////////////////////////////////////////////////////////////////
	// Object method overrides
	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ cmValues.hashCode();
		result = prime * result
		+ lengthValues.hashCode();
		result = prime * result
		+ smValues.hashCode();
		result = prime * result
		+ nhValues.hashCode();
		result = prime * result
		+ zmValues.hashCode();
		return result;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MAPQMatrix other = (MAPQMatrix) obj;
        if (!cmValues.equals(other.cmValues))
            return false;
        if (!lengthValues.equals(other.lengthValues))
            return false;
        if (!smValues.equals(other.smValues))
            return false;
        if (!nhValues.equals(other.nhValues))
            return false;
        return zmValues.equals(other.zmValues);
    }

	@Override
	public String toString() {
		return "smValues.size(): " + smValues.size() + ", cmValues.size(): " + cmValues.size() 
		+ ", lengthValues.size(): " + lengthValues.size() + ", nhValues.size(): " + nhValues.size() 
		+ ", zmValues.size(): " + zmValues.size();
	}
}
