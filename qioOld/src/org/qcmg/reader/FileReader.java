/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.reader;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.qcmg.record.RecordIterator;
import org.qcmg.record.Serializer;

public abstract class FileReader<RecordType> implements Closeable,
		Iterable<RecordType> {
	private final Serializer<RecordType> serializer;
	private final File file;
	private final FileInputStream inputStream;

	public FileReader(final File file, final Serializer<RecordType> serializer)
			throws Exception {
		this.file = file;
		this.serializer = serializer;
		inputStream = new FileInputStream(file);
	}

	public Iterator<RecordType> iterator() {
		try {
			return getIterator();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Iterator<RecordType> getIterator() throws Exception {
		return new RecordIterator<RecordType>(inputStream, serializer);
	}

	public void close() throws IOException {
		inputStream.close();
	}

	public File getFile() {
		return file;
	}

	public FileInputStream getInputStream() {
		return inputStream;
	}

	public Serializer<RecordType> getSerializer() {
		return serializer;
	}

}
