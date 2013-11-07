/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.bed;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public final class BEDSerializer {
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static BEDRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		BEDRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static BEDRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line, -1);
		if (3 > params.length) {
			throw new Exception("Bad BED format. Insufficient columns: '" + line + "'");
		}
		BEDRecord result = new BEDRecord();
		result.setChrom(params[0]);
		result.setChromStart(Integer.parseInt(params[1]));
		result.setChromEnd(Integer.parseInt(params[2]));
		if (params.length > 3) {
			if (params.length >= 4)
				result.setName(params[3]);			
			if (params.length >= 5) 
				result.setScore(Integer.parseInt(params[4]));
			if (params.length >= 6)				
				result.setStrand(params[5]);
			if (params.length >= 7)				
				result.setThickStart(Integer.parseInt(params[6]));
			if (params.length >= 8)				
				result.setThickEnd(Integer.parseInt(params[7]));
			if (params.length >= 9)				
				result.setItemRGB(params[8]);
			if (params.length >= 10)				
				result.setBlockCount(Integer.parseInt(params[9]));
			if (params.length >= 11)				
				result.setBlockSizes(Integer.parseInt(params[10]));
			if (params.length >= 12)				
				result.setBlockStarts(Integer.parseInt(params[11]));
		}
		return result;
	}

}
