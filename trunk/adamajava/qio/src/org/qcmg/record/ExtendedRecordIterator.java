package org.qcmg.record;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public final class ExtendedRecordIterator<HeaderType, RecordType> implements
		Iterator<RecordType> {
	private final Serializer<RecordType> serializer;
	private final BufferedReader reader;
	private final AtomicLong counter;
	private RecordType next;

	public ExtendedRecordIterator(final InputStream stream,
			final Serializer<RecordType> serializer,
			final Serializer<HeaderType> headerSerializer) throws Exception {
		InputStreamReader streamReader = new InputStreamReader(stream);
		reader = new BufferedReader(streamReader);
		counter = new AtomicLong(0);
		this.serializer = serializer;
		headerSerializer.nextRecord(reader); // skip header
		readNext();
	}

	public boolean hasNext() {
		return null != next;
	}

	public RecordType next() {
		counter.incrementAndGet();
		RecordType result = next;
		try {
			readNext();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage() + " [Record count: "
					+ counter.get() + "]", e);
		}
		return result;
	}

	private void readNext() throws Exception {
		next = serializer.nextRecord(reader);
	}

	public void remove() {
	}
}
