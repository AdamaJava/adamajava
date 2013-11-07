/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
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
