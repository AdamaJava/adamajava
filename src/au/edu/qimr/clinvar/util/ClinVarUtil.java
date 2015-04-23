package au.edu.qimr.clinvar.util;

import org.apache.commons.lang3.StringUtils;

public class ClinVarUtil {
	
	public static int [] getDoubleEditDistance(String read1, String read2, String primer1, String primer2, int editDistanceCutoff) {
		
		String r1SubString = read1.substring(0, primer1.length());
		int editDistance = StringUtils.getLevenshteinDistance(primer1, r1SubString);
		int editDistance2 = Integer.MAX_VALUE;
		
		if (editDistance <= editDistanceCutoff) {
			// get read2 edit distance
			String r2SubString = read2.substring(0, primer2.length());
			editDistance2 = StringUtils.getLevenshteinDistance(primer2, r2SubString);
		}
		
		return new int [] {editDistance, editDistance2};
	}

}
