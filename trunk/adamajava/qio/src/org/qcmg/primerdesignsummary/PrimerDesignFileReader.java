package org.qcmg.primerdesignsummary;

import java.io.File;

import org.qcmg.reader.FileReader;

public class PrimerDesignFileReader extends FileReader<PrimerDesignRecord> {
	private final static PrimerDesignRecordSerializer serializer =
		new PrimerDesignRecordSerializer();

	public PrimerDesignFileReader(final File file) throws Exception {
		super(file, serializer);
	}
}
