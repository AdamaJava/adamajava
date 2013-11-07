/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.pileup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class VerifiedSnpFileReader implements Closeable, Iterable<VerifiedSnpRecord> {
    private final File file;
    private final InputStream inputStream;

    public VerifiedSnpFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<VerifiedSnpRecord> iterator() {
        return getRecordIterator();
    }

    public VerifiedSnpRecordIterator getRecordIterator() {
        return new VerifiedSnpRecordIterator(inputStream);
    }

    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
