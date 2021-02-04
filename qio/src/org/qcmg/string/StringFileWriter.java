/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.string;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class StringFileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;

	public StringFileWriter(final File file) throws Exception {
		this.file = file;
		OutputStream stream = new FileOutputStream(file);
		outputStream = stream;
	}
	
	public void addHeader(final StringHeader header) throws Exception {
		for (String headerLine : header) {
			String encoded = headerLine + "\n";
			outputStream.write(encoded.getBytes());
		}
		outputStream.flush();
	}
	
	public void add(final String record) throws Exception {
		String encoded = record + "\n";
		outputStream.write(encoded.getBytes());
		outputStream.flush();
	}

	@Override
	public void close() throws IOException {
		// flush anything outstanding and then close
		outputStream.flush();
		outputStream.close();
	}

	public File getFile() {
		return file;
	}
}