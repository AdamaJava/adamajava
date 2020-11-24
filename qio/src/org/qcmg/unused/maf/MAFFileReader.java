/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.qcmg.common.maf.MAFRecord;

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
