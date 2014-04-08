/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.gff3;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class GFF3FileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;

	public GFF3FileWriter(final File file) throws Exception {
		this.file = file;
		OutputStream stream = new FileOutputStream(file);
		outputStream = stream;
	}

	public void add(final GFF3Record record) throws Exception {
		String encoded = GFF3Serializer.serialise(record) + "\n";
		outputStream.write(encoded.getBytes());
		outputStream.flush();
	}

	@Override
	public void close() throws IOException {
	}

	public File getFile() {
		return file;
	}
}
