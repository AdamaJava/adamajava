/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.unused;

import java.io.File;

import org.qcmg.reader.ExtendedFileReader;
import org.qcmg.reader.FileReader;
import org.qcmg.unused.PrimerOutputHeader;
import org.qcmg.unused.PrimerOutputRecord;

public class PrimerOutputFileReader extends ExtendedFileReader<PrimerOutputHeader, PrimerOutputRecord> {
	private final static PrimerOutputHeaderSerializer headerSerializer =
		new PrimerOutputHeaderSerializer();
	private final static PrimerOutputRecordSerializer recordSerializer =
		new PrimerOutputRecordSerializer();

	public PrimerOutputFileReader(final File file) throws Exception {
		super(file, recordSerializer, headerSerializer);
	}
}
