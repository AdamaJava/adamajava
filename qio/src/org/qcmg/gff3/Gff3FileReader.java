/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.gff3;

import java.io.File;
import java.io.IOException;
import org.qcmg.record.RecordReader;

public final class Gff3FileReader extends RecordReader<Gff3Record> {
	public Gff3FileReader(File file) throws IOException {
		super(file, "#");
	}

	@Override
	public Gff3Record getRecord(String line) throws Exception {
		return new Gff3Record(line);
	}
}
