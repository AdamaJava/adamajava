package org.qcmg.gff;

import java.io.File;
import java.io.IOException;

import org.qcmg.unused.reader.AbstractReader;
import org.qcmg.unused.record.AbstractRecordIterator;

public class GFFReader extends AbstractReader {

	public GFFReader(File file) throws IOException {
		super(file);
	}

	@Override
	public AbstractRecordIterator getRecordIterator() throws Exception {
		return new GFFIterator(inputStream);
	}
}
