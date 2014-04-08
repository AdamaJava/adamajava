/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.simple;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.record.SimpleRecord;

public final class SimpleSerializer {
	
	private static final String DEFAULT_ID_PREFIX = ">";
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static SimpleRecord nextRecord(final BufferedReader reader) throws Exception {
		SimpleRecord result = null;

		String id = nextNonheaderLine(reader);
		String sequence = reader.readLine();
//		return parseRecord(id, sequence);
		if (null != id && null != sequence) {
			result = parseRecord(id, sequence);
		}

		return result;
	}

	static String parseID(final String value) throws Exception {
		if ( ! value.startsWith(DEFAULT_ID_PREFIX)) {
			throw new Exception("Bad id format: " + value);
		}
		return value;
	}
	
	static String parseSequence(final String sequence) throws Exception {
		if (sequence.startsWith(DEFAULT_ID_PREFIX)) {
			throw new Exception("Bad sequence format: " + sequence);
		}
		return sequence;
	}

	static SimpleRecord parseRecord(final String id, final String sequence)
			throws Exception {
		return new SimpleRecord(parseID(id), parseSequence(sequence));
	}

}
