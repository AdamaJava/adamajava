/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.model.ChrRangePosition;

public class ChrPositionCache {
	
	
	static ConcurrentMap<String, ChrRangePosition> cache = new ConcurrentHashMap<>();
	
	
	public static ChrRangePosition getChrPosition(String chr, int position) {
		String chrPos = chr + '-' + position;
		ChrRangePosition cp = cache.get(chrPos);
		if (null == cp) {
			cp = new ChrRangePosition(chr, position);
			cache.putIfAbsent(chrPos, cp);
		}
		return cp;
	}

}
	
