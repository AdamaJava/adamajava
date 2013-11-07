package org.qcmg.primeroutput;

import java.io.File;

import org.qcmg.primeroutput.PrimerOutputHeader;
import org.qcmg.primeroutput.PrimerOutputRecord;
import org.qcmg.reader.ExtendedFileReader;
import org.qcmg.reader.FileReader;

public class PrimerOutputFileReader extends ExtendedFileReader<PrimerOutputHeader, PrimerOutputRecord> {
	private final static PrimerOutputHeaderSerializer headerSerializer =
		new PrimerOutputHeaderSerializer();
	private final static PrimerOutputRecordSerializer recordSerializer =
		new PrimerOutputRecordSerializer();

	public PrimerOutputFileReader(final File file) throws Exception {
		super(file, recordSerializer, headerSerializer);
	}
}
