/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.model.ChrPosition;

public class ChrPositionCache {
	
	
	static ConcurrentMap<String, ChrPosition> cache = new ConcurrentHashMap<>();
	
	
	public static ChrPosition getChrPosition(String chr, int position) {
		String chrPos = chr + '-' + position;
		ChrPosition cp = cache.get(chrPos);
		if (null == cp) {
			cp = new ChrPosition(chr, position);
			cache.putIfAbsent(chrPos, cp);
		}
		return cp;
	}

}
	