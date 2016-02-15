/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.model.ChrPointPosition;

public class ChrPositionCache {
	
	
	static ConcurrentMap<String, ChrPointPosition> cache = new ConcurrentHashMap<>();
	
	
	public static ChrPointPosition getChrPosition(String chr, int position) {
		String chrPos = chr + '-' + position;
		ChrPointPosition cp = cache.get(chrPos);
		if (null == cp) {
			cp = ChrPointPosition.valueOf(chr, position);
			cache.putIfAbsent(chrPos, cp);
		}
		return cp;
	}

}
	
