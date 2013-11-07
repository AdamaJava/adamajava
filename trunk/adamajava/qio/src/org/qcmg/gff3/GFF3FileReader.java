/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.gff3;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class GFF3FileReader implements Closeable, Iterable<GFF3Record> {
    private final File file;
    private final InputStream inputStream;

    public GFF3FileReader(final File file) throws Exception {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<GFF3Record> iterator() {
        return getRecordIterator();
    }

    public GFF3RecordIterator getRecordIterator() {
        return new GFF3RecordIterator(inputStream);
    }

    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
