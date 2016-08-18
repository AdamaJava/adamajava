/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;

import java.io.BufferedReader;

import static org.qcmg.common.util.Constants.HASH_STRING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class TabbedSerializer {
	
    public static TabbedHeader readHeader(final BufferedReader reader) throws IOException {
		List<String> headerLines = new ArrayList<>();
		String line = reader.readLine();
		while (null != line && line.startsWith(HASH_STRING)) {
		    headerLines.add(line);
		    line = reader.readLine();
		}
		return new TabbedHeader(headerLines);
	}

	private static String nextNonheaderLine(final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(HASH_STRING)) {
			line = reader.readLine();
		}
		return line;
	}

	public static TabbedRecord nextRecord(final BufferedReader reader) throws IOException {
		TabbedRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static TabbedRecord parseRecord(final String line) {
		TabbedRecord result = new TabbedRecord();
		result.setData(line);
		return result;
	}
}
