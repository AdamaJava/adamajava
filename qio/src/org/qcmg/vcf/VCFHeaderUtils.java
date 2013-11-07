package org.qcmg.vcf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.util.TabTokenizer;

public class VCFHeaderUtils {
	
	public static final String INFO_HEADER_LINE = "##INFO";
	
	public static Map<String, Integer> getMapFromInfoHeader(VCFHeader header) {
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		
		for (String s : header) {
			// check that is an INFO line
			if (s.startsWith(INFO_HEADER_LINE)) {
				// tokenise on "="
				String [] params = TabTokenizer.tokenize(s, '=');
				// id should be #2, value #5
				String id = params[2];
				String file = params[5];
				
				id = id.substring(0, id.indexOf(","));
				file = file.substring(1, file.length() - 2);
				
				map.put(file, Integer.valueOf(id));
			}
		}
		
		return map;
	}
	
	
	public static int[] getIdsForPatient(Map<String, Integer> mapOfFiles, String patient) {
		
		List<Integer> ids = new ArrayList<Integer>();
		for (Entry<String, Integer> entry : mapOfFiles.entrySet()) {
			if (entry.getKey().contains(patient)) {
				ids.add(entry.getValue());
			}
		}
		
		if ( ! ids.isEmpty()) {
			int j = 0;
			int [] idArray = new int[ids.size()];
			for (Integer i : ids) idArray[j++] = i;
			return idArray;
		}
		
		return null;
	}

}
