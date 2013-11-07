/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.genesymbol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public final class GeneSymbolSerializer {
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

	public static GeneSymbolRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		GeneSymbolRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static GeneSymbolRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line, -1);
		if (3 > params.length) {
			throw new Exception("Bad Gene Symbol format. Insufficient columns: '" + line + "'");
		}
		GeneSymbolRecord result = new GeneSymbolRecord();
		result.setGeneId(params[0]);
		result.setTranscriptId(params[1]);
		result.setSymbol(params[2]);
		return result;
	}
}
