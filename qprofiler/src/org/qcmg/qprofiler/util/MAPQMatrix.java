/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qprofiler.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.qvisualise.util.SummaryByCycleUtils;

public class MAPQMatrix {
	
	public enum MatrixType{
		SM,CM,LENGTH,NH,ZM;
	}
	
	private final ConcurrentMap<Integer, AtomicLong> smValues = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentMap<Integer, AtomicLong> cmValues = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentMap<Integer, AtomicLong> lengthValues = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentMap<Integer, AtomicLong> nhValues = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentMap<Integer, AtomicLong> zmValues = new ConcurrentSkipListMap<Integer, AtomicLong>();

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
		switch (type) {
		case SM:
			return smValues;
		case CM:
			return cmValues;
		case LENGTH:
			return lengthValues;
		case NH:
			return nhValues;
		case ZM:
			return zmValues;
		}
		return Collections.emptyMap();
	}

	
	///////////////////////////////////////////////////////////////////////////
	// Object method overrides
	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((cmValues == null) ? 0 : cmValues.hashCode());
		result = prime * result
		+ ((lengthValues == null) ? 0 : lengthValues.hashCode());
		result = prime * result
		+ ((smValues == null) ? 0 : smValues.hashCode());
		result = prime * result
		+ ((nhValues == null) ? 0 : nhValues.hashCode());
		result = prime * result
		+ ((zmValues == null) ? 0 : zmValues.hashCode());
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
		if (cmValues == null) {
			if (other.cmValues != null)
				return false;
		} else if (!cmValues.equals(other.cmValues))
			return false;
		if (lengthValues == null) {
			if (other.lengthValues != null)
				return false;
		} else if (!lengthValues.equals(other.lengthValues))
			return false;
		if (smValues == null) {
			if (other.smValues != null)
				return false;
		} else if (!smValues.equals(other.smValues))
			return false;
		if (nhValues == null) {
			if (other.nhValues != null)
				return false;
		} else if (!nhValues.equals(other.nhValues))
			return false;
		if (zmValues == null) {
			if (other.zmValues != null)
				return false;
		} else if (!zmValues.equals(other.zmValues))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "smValues.size(): " + smValues.size() + ", cmValues.size(): " + cmValues.size() 
		+ ", lengthValues.size(): " + lengthValues.size() + ", nhValues.size(): " + nhValues.size() 
		+ ", zmValues.size(): " + zmValues.size();
	}
}
