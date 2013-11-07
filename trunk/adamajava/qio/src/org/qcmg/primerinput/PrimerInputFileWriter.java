/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.primerinput;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class PrimerInputFileWriter implements Closeable {
	private static final String EQUALS = "=";
	private static final PrimerInputRecordSerializer serializer = new PrimerInputRecordSerializer();
	private final OutputStream outputStream;

	public PrimerInputFileWriter(final File file) throws Exception {
		OutputStream stream = new FileOutputStream(file);
		outputStream = stream;
	}

	public void add(final PrimerInputRecord record) throws Exception {
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
