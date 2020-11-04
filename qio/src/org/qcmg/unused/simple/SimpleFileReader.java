/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.unused.simple;

import java.io.File;
import java.io.IOException;

import org.qcmg.qmule.record.AbstractRecordIterator;
import org.qcmg.unused.reader.AbstractReader;

public class SimpleFileReader extends AbstractReader {

	public SimpleFileReader(File file) throws IOException {
		super(file);
	}

	@Override
	public AbstractRecordIterator getRecordIterator() throws Exception{
		return new SimpleRecordIterator(inputStream);
	}

}
