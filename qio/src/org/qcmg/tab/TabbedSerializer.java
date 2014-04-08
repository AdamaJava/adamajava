/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Vector;

public final class TabbedSerializer {
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final String DEFAULT_HEADER_PREFIX = "#";
	
    public static TabbedHeader readHeader(final BufferedReader reader) throws Exception {
		Vector<String> headerLines = new Vector<String>();
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
		    headerLines.add(line);
		    line = reader.readLine();
		}
		return new TabbedHeader(headerLines);
	}

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static TabbedRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		TabbedRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static TabbedRecord parseRecord(final String line) throws Exception {
		TabbedRecord result = new TabbedRecord();
		result.setData(line);
		return result;
	}
}
