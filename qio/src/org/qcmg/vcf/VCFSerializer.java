/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;

public final class VCFSerializer {
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}
	
	public static VcfHeader readHeader(final BufferedReader reader) throws IOException {
		final List<String> headerLines = new ArrayList<>();

		String line = reader.readLine();
		while (null != line && line.startsWith("#")) {
			headerLines.add(line);
			line = reader.readLine();
		}
		return new VcfHeader(headerLines);
	}

	public static VcfRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		VcfRecord result = null;
		final String line = nextNonheaderLine(reader);	
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	public static VcfRecord parseRecord(final String line) throws Exception {
		final String[] params = TabTokenizer.tokenize(line);
		final int arrayLength = params.length; 
		if (8 > arrayLength) {
			throw new Exception("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		final VcfRecord result = new VcfRecord(params);
		
		
		return result;
	}
}
