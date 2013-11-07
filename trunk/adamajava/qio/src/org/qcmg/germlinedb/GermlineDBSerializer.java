/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.germlinedb;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;

public final class GermlineDBSerializer {
	private static final String DEFAULT_HEADER_PREFIX = "analysis_id";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static GermlineDBRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		GermlineDBRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static GermlineDBRecord parseRecord(final String line) throws Exception {
		String [] params = TabTokenizer.tokenize(line);
		GermlineDBRecord result = new GermlineDBRecord();
		result.setChromosome(params[4]);
		result.setPosition(Integer.parseInt(params[5]));
		result.setVariationId(params[2]);
		result.setNormalGenotype(params[11]);
		result.setFlag(params[params.length-1]);
		result.setData(line);
		return result;
	}
}
