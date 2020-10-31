/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.qcmg.exception.RecordIteratorException;
import org.qcmg.record.AbstractRecordIterator;
import org.qcmg.record.Record;

public abstract class AbstractReader implements Reader, Iterable<Record> {
	
	protected final InputStream inputStream;

	public AbstractReader(final File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		inputStream = stream;
	}

	@Override
	public Iterator<Record> iterator() {
		try {
			return getRecordIterator();
		} catch (Exception e) {
			throw new RecordIteratorException(e);
		}
	}

	public abstract AbstractRecordIterator getRecordIterator() throws Exception;

	@Override
	public void close() throws IOException {
		inputStream.close();
	}
}
