/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.pileup;

import java.io.BufferedReader;
import java.io.IOException;

public final class PileupSerializer {
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static String nextRecord(final BufferedReader reader)
			throws Exception {
//		PileupRecord result = null;
		String line = nextNonheaderLine(reader);
//		if (null != line) {
//			result = parseRecord(line);
//		}
		return line;
	}
//	public static PileupRecord nextRecord(final BufferedReader reader)
//	throws IOException , Exception {
//		PileupRecord result = null;
//		String line = nextNonheaderLine(reader);
//		if (null != line) {
//			result = parseRecord(line);
//		}
//		return result;
//	}

	static PileupRecord parseRecord(final String line) throws Exception {
//		String[] params = tabbedPattern.split(line, -1);
//		if (4 > params.length) {
//			throw new Exception("Bad Pileup format. Insufficient columns: '" + line + "'");
//		}
		PileupRecord result = new PileupRecord();
//		result.setChromosome(params[0]);
//		result.setPosition(Integer.parseInt(params[1]));
//		result.setRef(params[2].charAt(0));
		result.setPileup(line);
		return result;
	}
}
