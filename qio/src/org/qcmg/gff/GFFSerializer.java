/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.gff;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.record.Record;

public final class GFFSerializer {

	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static GFFRecord nextRecord(final BufferedReader reader) {
		GFFRecord result = null;

		try {
			String data = nextNonheaderLine(reader);
			if (null != data) {
				result = parseRecord(data);
			}
		} catch (Exception ex) {
			// Fall through to return null
		}

		return result;
	}

	static String[] parseData(String data) throws Exception {
		if (null == data) {
			throw new AssertionError("Record was null");
		}

		String[] fields = data.split(Record.TAB_DELIMITER);

		if (fields.length < 8) {
			throw new Exception("Not enough fields in the Record");
		}

		return fields;
	}

	static GFFRecord parseRecord(final String data) throws Exception {
		return new GFFRecord(parseData(data));
	}

}
