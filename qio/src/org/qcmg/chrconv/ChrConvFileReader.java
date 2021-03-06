/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.chrconv;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Deprecated
public final class ChrConvFileReader implements Closeable, Iterable<ChromosomeConversionRecord> {
    private final File file;
    private final InputStream inputStream;

    public ChrConvFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    @Override
    public Iterator<ChromosomeConversionRecord> iterator() {
        return getRecordIterator();
    }

    public ChrConvRecordIterator getRecordIterator() {
        return new ChrConvRecordIterator(inputStream);
    }

    @Override
    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
