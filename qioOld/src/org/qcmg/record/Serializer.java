/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.record;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public abstract class Serializer<RecordType> {
	public static final String HASH = "#";
	public static final String NEWLINE = "\n";
	public static final String EQUALS = "=";
	public static final Pattern tabbedPattern = Pattern.compile("[\\t]+");
	public static final Pattern colonPattern = Pattern.compile("[:]+");
	public static final Pattern hyphenPattern = Pattern.compile("[-]+");
	public static final Pattern equalsPattern = Pattern.compile("[=]+");
	public static final Pattern commaPattern = Pattern.compile("[,]+");

	public RecordType nextRecord(final BufferedReader reader) throws Exception {
		RecordType result = null;
		try {
			result = parseRecord(reader);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		}
		return result;
	}

	public abstract String serialise(final RecordType record) throws Exception;

	public abstract RecordType parseRecord(BufferedReader reader)
			throws Exception;
}
