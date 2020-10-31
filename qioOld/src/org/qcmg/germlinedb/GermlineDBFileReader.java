/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.germlinedb;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public final class GermlineDBFileReader implements Closeable, Iterable<GermlineDBRecord> {
    private final File file;
    private final InputStream inputStream;
    private final FileChannel fileChannel;
    private final FileLock lock;
    private static final QLogger logger = QLoggerFactory.getLogger(GermlineDBFileReader.class);

    public GermlineDBFileReader(final File file) throws IOException {
        this.file = file;
        FileInputStream fileStream = new FileInputStream(file);
        inputStream = fileStream;
        
        logger.info("Attempting to get a shared read lock on the germline db file");
        long start = System.currentTimeMillis();
        fileChannel = fileStream.getChannel();
        lock = fileChannel.lock(0, Long.MAX_VALUE, true);
        logger.info("It took " + (System.currentTimeMillis() - start) + "ms to get a read lock on the germlinedb file");
    }

    public Iterator<GermlineDBRecord> iterator() {
        return getRecordIterator();
    }

    public GermlineDBRecordIterator getRecordIterator() {
        return new GermlineDBRecordIterator(inputStream);
    }

    public void close() throws IOException {
    	lock.release();
    	fileChannel.close();
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
