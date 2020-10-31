/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.germlinedb;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public final class GermlineDBFileWriter implements Closeable {
	private final FileOutputStream outputStream;
	private final FileChannel fileChannel;
	private final FileLock lock;
	private static final QLogger logger = QLoggerFactory.getLogger(GermlineDBFileWriter.class);

	public GermlineDBFileWriter(final File file) throws IOException {
		// append to the end of the existing germline db file
		FileOutputStream stream = new FileOutputStream(file, true);
		outputStream = stream;
		
		logger.info("Attempting to get an exclusive write lock on the germline db file");
		long start = System.currentTimeMillis();
		fileChannel = stream.getChannel();
		// blocks until it can get a write lock on the file
		lock = fileChannel.lock();
		logger.info("It took " + (System.currentTimeMillis() - start) + "ms to get a write lock on the germlinedb file");
	}

	public void add(final String record) throws IOException {
		outputStream.write(record.getBytes());
		outputStream.flush();
	}

	public void close() throws IOException {
		outputStream.flush();
		lock.release();
		fileChannel.close();
		outputStream.close();
	}
}
