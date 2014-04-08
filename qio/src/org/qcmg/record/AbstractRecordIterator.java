/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.record;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.exception.RecordIteratorException;

public abstract class AbstractRecordIterator implements Iterator<Record> {

	protected final BufferedReader reader;
	private final AtomicLong counter;
	protected Record next;

	public AbstractRecordIterator(final InputStream stream) throws Exception {
		InputStreamReader streamReader = new InputStreamReader(stream);
		reader = new BufferedReader(streamReader);
		counter = new AtomicLong(0);
		readNext();
	}

	public boolean hasNext() {
		return null != next;
	}

	public Record next() {
		counter.incrementAndGet();
		Record result = next;
		try {
			readNext();
		} catch (Exception e) {
			throw new RecordIteratorException(e.getMessage() + " [Record count: " + counter.get() +"]", e);
		}
		return result;
	}

	protected abstract void readNext() throws Exception;

	public void remove() {
	}

}
