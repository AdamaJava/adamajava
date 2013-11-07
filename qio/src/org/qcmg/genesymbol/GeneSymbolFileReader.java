/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.genesymbol;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class GeneSymbolFileReader implements Closeable, Iterable<GeneSymbolRecord> {
    private final File file;
    private final InputStream inputStream;

    public GeneSymbolFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<GeneSymbolRecord> iterator() {
        return getRecordIterator();
    }

    public GeneSymbolRecordIterator getRecordIterator() {
        return new GeneSymbolRecordIterator(inputStream);
    }

    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
