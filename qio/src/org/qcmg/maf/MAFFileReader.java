/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.maf;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class MAFFileReader implements Closeable, Iterable<MAFRecord> {
    private final File file;
    private final InputStream inputStream;

    public MAFFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<MAFRecord> iterator() {
        return getRecordIterator();
    }

    public MAFRecordIterator getRecordIterator() {
        return new MAFRecordIterator(inputStream);
    }

    public void close() throws IOException {
    }

    public File getFile() {
        return file;
    }
}
