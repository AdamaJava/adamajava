/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
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
