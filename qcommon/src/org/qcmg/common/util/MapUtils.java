/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapUtils {
	
	
	public static <K, V> List<Map<K, V>> splitMap(Map<K, V> map, int noOfSubmaps) {
		
		if (noOfSubmaps <= 0) throw new IllegalArgumentException("invalid number of submaps requested: " + noOfSubmaps);
		
		int mapSize = map.size();
		int subMapSize = mapSize / noOfSubmaps;
		List<Map<K, V>> subMaps = new ArrayList<Map<K, V>>();
		for (int i = 0 ; i < noOfSubmaps ; i++) {
			subMaps.add(new HashMap<K, V>());
		}
		
		int i = 0, j = 1;
		
		for (Entry<K, V> entry : map.entrySet()) {
			if (i++ < (subMapSize * j)) {
				Map<K, V> subMap = subMaps.get(j-1);
				subMap.put(entry.getKey(), entry.getValue());
			} else {
				// time for a new submap
				j++;
				if (j >= subMaps.size())
					j = subMaps.size();
				Map<K, V> subMap = subMaps.get(j - 1);
				subMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		return subMaps;
	}

}
