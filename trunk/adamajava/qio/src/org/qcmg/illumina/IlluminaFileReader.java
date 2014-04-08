/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.illumina;

import java.io.File;
import java.io.IOException;

import org.qcmg.reader.AbstractReader;

public final class IlluminaFileReader extends AbstractReader {

    public IlluminaFileReader(final File file) throws IOException {
    	super(file);
    }

    public IlluminaRecordIterator getRecordIterator() throws Exception {
        return new IlluminaRecordIterator(inputStream);
    }

}
