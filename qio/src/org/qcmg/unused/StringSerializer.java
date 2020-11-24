/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.unused;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class StringSerializer {
	
    public static StringHeader readHeader(final BufferedReader reader, String headerDiscriminator) throws IOException {
		List<String> headerLines = new ArrayList<>();
		String line = reader.readLine();
		while (null != line && line.startsWith(headerDiscriminator)) {
		    headerLines.add(line);
		    line = reader.readLine();
		}
		return new StringHeader(headerLines);
	}

	private static String nextNonheaderLine(final BufferedReader reader, String headerDiscriminator) throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(headerDiscriminator)) {
			line = reader.readLine();
		}
		return line;
	}

	public static String nextRecord(final BufferedReader reader, String headerDiscriminator) throws IOException {
		return nextNonheaderLine(reader, headerDiscriminator);
	}
}
