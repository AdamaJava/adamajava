/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ma;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public final class MAFileReader implements Closeable, Iterable<MARecord> {
    private final File file;
    private final MAHeader header;
    private final InputStream inputStream;

    public MAFileReader(final File file) throws Exception {
        this.file = file;

        FileInputStream stream = new FileInputStream(file);
        InputStreamReader streamReader = new InputStreamReader(stream);
        BufferedReader in = new BufferedReader(streamReader);

        header = MASerializer.readHeader(in);
        stream.close();

        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public MAHeader getHeader() {
        return header;
    }

    @Override
    public Iterator<MARecord> iterator() {
        return getRecordIterator();
    }

    public MARecordIterator getRecordIterator() {
        return new MARecordIterator(inputStream);
    }

    	@Override
    public void close() throws IOException {
    		inputStream.close();
    	}

    public File getFile() {
        return file;
    }
}
