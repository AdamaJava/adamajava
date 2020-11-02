/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class QPileupFileReader implements Closeable, Iterable<QSnpRecord> {
    private final File file;
    private final InputStream inputStream;

    public QPileupFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<QSnpRecord> iterator() {
        return getRecordIterator();
    }

    public QPileupRecordIterator getRecordIterator() {
        return new QPileupRecordIterator(inputStream);
    }

    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
