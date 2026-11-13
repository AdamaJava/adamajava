/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2026.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

import htsjdk.samtools.SamReader;

/**
 * Holds one open {@link SamReader} per BAM file so that repeated region queries
 * (one or more per candidate SV) don't re-open the file, re-parse the header and
 * reload the index each time.
 * <p>
 * Not thread safe: htsjdk readers support a single open iterator at a time, so an
 * instance must be confined to one thread. Callers must close each iterator before
 * issuing the next query, and close this cache when the thread's work is done.
 */
public class SamReaderCache implements AutoCloseable {

	private static final QLogger logger = QLoggerFactory.getLogger(SamReaderCache.class);

	private final Map<File, SamReader> readers = new HashMap<>(4);

	/**
	 * Returns the open reader for this file, opening it on first use.
	 */
	public SamReader get(File bam) throws IOException {
		SamReader reader = readers.get(bam);
		if (null == reader) {
			reader = SAMFileReaderFactory.createSAMFileReader(bam, "silent");
			readers.put(bam, reader);
		}
		return reader;
	}

	@Override
	public void close() {
		for (Map.Entry<File, SamReader> entry : readers.entrySet()) {
			try {
				entry.getValue().close();
			} catch (IOException e) {
				logger.warn("Failed to close SamReader for " + entry.getKey() + ": " + e.getMessage());
			}
		}
		readers.clear();
	}
}
