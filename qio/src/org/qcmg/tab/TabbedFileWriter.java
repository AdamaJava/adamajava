/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.tab;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class TabbedFileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;

	public TabbedFileWriter(final File file) throws Exception {
		this.file = file;
		OutputStream stream = new FileOutputStream(file);
		outputStream = stream;
	}
	
	public void addHeader(final TabbedHeader header) throws Exception {
		for (String headerLine : header) {
			String encoded = headerLine + "\n";
			outputStream.write(encoded.getBytes());
		}
		outputStream.flush();
	}
	
	public void add(final TabbedRecord record) throws Exception {
		String encoded = record.getData() + "\n";
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
