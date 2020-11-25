/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * @deprecated since it is no longer used.
 */

package org.qcmg.unused.bed;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Deprecated
public final class BEDFileReader implements Closeable, Iterable<BEDRecord> {
    private final File file;
    private final InputStream inputStream;

    public BEDFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
    }

    public Iterator<BEDRecord> iterator() {
        return getRecordIterator();
    }

    public BEDRecordIterator getRecordIterator() {
        return new BEDRecordIterator(inputStream);
    }

    public void close() throws IOException {
    }

    public File getFile() {
        return file;
    }
}
