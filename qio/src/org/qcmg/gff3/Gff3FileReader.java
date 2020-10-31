/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.gff3;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class GFF3FileReader implements Closeable, Iterable<GFF3Record> {
    private final File file;
    private final InputStream inputStream;

    public GFF3FileReader(final File file) throws FileNotFoundException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    @Override
	public Iterator<GFF3Record> iterator() {
        return getRecordIterator();
    }

    public GFF3RecordIterator getRecordIterator() {
        return new GFF3RecordIterator(inputStream);
    }

    @Override
	public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
