package org.qcmg.simple;

import java.io.File;
import java.io.IOException;

import org.qcmg.reader.AbstractReader;
import org.qcmg.record.AbstractRecordIterator;

public class SimpleFileReader extends AbstractReader {

	public SimpleFileReader(File file) throws IOException {
		super(file);
	}

	@Override
	public AbstractRecordIterator getRecordIterator() throws Exception{
		return new SimpleRecordIterator(inputStream);
	}

}
