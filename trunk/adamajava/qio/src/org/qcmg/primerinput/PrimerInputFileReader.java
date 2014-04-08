/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
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
