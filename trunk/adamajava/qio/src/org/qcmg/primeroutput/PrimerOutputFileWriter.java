/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.primeroutput;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.qcmg.primeroutput.PrimerOutputRecord;

public final class PrimerOutputFileWriter implements Closeable {
	private static final String EQUALS = "=";
	private static final PrimerOutputRecordSerializer serializer = new PrimerOutputRecordSerializer();
	private final OutputStream outputStream;

	public PrimerOutputFileWriter(final File file) throws Exception {
		OutputStream stream = new FileOutputStream(file);
		outputStream = stream;
	}

	public void add(final PrimerOutputRecord record) throws Exception {
		String encoded = serializer.serialise(record);
		outputStream.write(encoded.getBytes());
		outputStream.flush();
	}

	public void close() throws IOException {
		outputStream.write(EQUALS.getBytes());
		outputStream.flush();
		outputStream.close();
	}
}
