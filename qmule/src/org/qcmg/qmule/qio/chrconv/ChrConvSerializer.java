/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.chrconv;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;

public class ChrConvSerializer {
	private static final String DEFAULT_HEADER_PREFIX = "#";
	private static final String N_A = "n/a";
	
	private static String nextNonheaderLine(final BufferedReader reader)
		throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}
	
	public static ChromosomeConversionRecord nextRecord(final BufferedReader reader)
	throws Exception {
		ChromosomeConversionRecord result = null;
			String data = nextNonheaderLine(reader);
			if (null != data ) {
				result = parseRecord(data);
			}
		return result;
		}

	static ChromosomeConversionRecord parseRecord(final String line) throws Exception {
		String[] params = TabTokenizer.tokenize(line);
		if (3 != params.length) {
			throw new Exception("Bad Chromosome Conversion format");
		}
		ChromosomeConversionRecord result = new ChromosomeConversionRecord();
		result.setEnsembleV55(params[0]);
		result.setQcmg(params[1]);
		result.setDccV04(N_A.equals(params[2]) ? -1 : Integer.parseInt(params[2]));
		return result;
	}
	
}
