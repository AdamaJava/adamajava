/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.dbsnp;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


public final class DbsnpFileReader implements Closeable, Iterable<Dbsnp130Record> {
    private final File file;
    private final InputStream inputStream;

    public DbsnpFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<Dbsnp130Record> iterator() {
        return getRecordIterator();
    }

    public DbsnpRecordIterator getRecordIterator() {
        return new DbsnpRecordIterator(inputStream);
    }

    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
