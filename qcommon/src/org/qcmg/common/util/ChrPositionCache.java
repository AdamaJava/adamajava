/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;

public class ChrPositionCache {
	
	
	static ConcurrentMap<String, ChrPointPosition> cache = new ConcurrentHashMap<>();
	static ConcurrentMap<ChrPosition, Integer> cacheWithIndex = new ConcurrentHashMap<>();
	static AtomicInteger index = new AtomicInteger();
	
	
	public static ChrPointPosition getChrPosition(String chr, int position) {
		String chrPos = chr + '-' + position;
		ChrPointPosition cp = cache.get(chrPos);
		if (null == cp) {
			cp = ChrPointPosition.valueOf(chr, position);
			cache.putIfAbsent(chrPos, cp);
		}
		return cp;
	}
	
	public static int getChrPositionIndex(String chr, int position) {
		ChrPosition cp = new ChrPointPosition(chr, position);
		Integer i = cacheWithIndex.get(cp);
		if (null == i) {
			i = index.incrementAndGet();
			cacheWithIndex.put(cp, i);
		}
		return i;
	}

}
	
