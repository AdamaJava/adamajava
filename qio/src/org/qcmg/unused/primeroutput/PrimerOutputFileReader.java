/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.unused.primeroutput;

import java.io.File;

import org.qcmg.unused.primeroutput.PrimerOutputHeader;
import org.qcmg.unused.primeroutput.PrimerOutputRecord;
import org.qcmg.unused.reader.ExtendedFileReader;
import org.qcmg.unused.reader.FileReader;

public class PrimerOutputFileReader extends ExtendedFileReader<PrimerOutputHeader, PrimerOutputRecord> {
	private final static PrimerOutputHeaderSerializer headerSerializer =
		new PrimerOutputHeaderSerializer();
	private final static PrimerOutputRecordSerializer recordSerializer =
		new PrimerOutputRecordSerializer();

	public PrimerOutputFileReader(final File file) throws Exception {
		super(file, recordSerializer, headerSerializer);
	}
}
