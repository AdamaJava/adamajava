package org.qcmg.primerinput;

import java.io.File;

import org.qcmg.reader.FileReader;

public class PrimerInputFileReader extends FileReader<PrimerInputRecord> {
	private final static PrimerInputRecordSerializer serializer =
		new PrimerInputRecordSerializer();

	public PrimerInputFileReader(final File file) throws Exception {
		super(file, serializer);
	}
}
