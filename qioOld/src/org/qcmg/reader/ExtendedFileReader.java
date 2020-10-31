/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.qcmg.record.ExtendedRecordIterator;
import org.qcmg.record.Serializer;

public abstract class ExtendedFileReader<HeaderType, RecordType> extends
		FileReader<RecordType> {
	private final Serializer<HeaderType> headerSerializer;
	private final HeaderType header;

	public ExtendedFileReader(final File file,
			final Serializer<RecordType> recordSerializer,
			final Serializer<HeaderType> headerSerializer) throws Exception {
		super(file, recordSerializer);
		FileInputStream inputStream = new FileInputStream(file);
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader reader = new BufferedReader(inputStreamReader);
		this.headerSerializer = headerSerializer;
		header = headerSerializer.parseRecord(reader);
	}

	public HeaderType getHeader() {
		return header;
	}

	@Override
	public Iterator<RecordType> getIterator() throws Exception {
		return new ExtendedRecordIterator<HeaderType, RecordType>(
				getInputStream(), getSerializer(), headerSerializer);
	}

}
