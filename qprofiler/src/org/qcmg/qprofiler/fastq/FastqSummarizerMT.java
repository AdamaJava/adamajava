/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.fastq;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

/**
 * Multi-threaded fastq summarizer.
 * <p>
 * The calling thread reads the fastq file and places batches of records on a bounded queue.
 * Each consumer thread takes batches from the queue and tallies them into its own {@link FastqSummaryReport},
 * so that the consumer threads are not all contending for the same counters.
 * Once the file has been read, the consumer reports are merged into a single report.
 */
public class FastqSummarizerMT implements Summarizer {

	final static QLogger logger = QLoggerFactory.getLogger(FastqSummarizerMT.class);

	static final int BATCH_SIZE = 1000;
	static final int IO_BUFFER_SIZE = 1024 * 1024;
	// placed on the queue (once for each consumer) to let the consumers know that there are no more records
	private static final List<FastqRecord> NO_MORE_RECORDS = Collections.emptyList();

	private final int noOfThreads;
	private final String [] excludes;

	public FastqSummarizerMT(int noOfThreads) {
		this(noOfThreads, null);
	}

	public FastqSummarizerMT(int noOfThreads, String [] excludes) {
		super();
		this.noOfThreads = noOfThreads;
		this.excludes = excludes;
	}

	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception {

		long start = System.currentTimeMillis();

		final FastqSummaryReport fastqSummaryReport = new FastqSummaryReport(excludes);
		fastqSummaryReport.setFileName(input);
		fastqSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());

		File file = new File(input);
		// the reading thread (and the decompressing thread for gzipped input) are counted as part of the requested number of threads
		boolean gzipped = isGzipped(file);
		final int noOfConsumerThreads = Math.max(1, noOfThreads - (gzipped ? 2 : 1));
		logger.info("will create " + noOfConsumerThreads + " consumer threads" + (gzipped ? ", and a decompression thread" : ""));

		final BlockingQueue<List<FastqRecord>> queue = new ArrayBlockingQueue<>(noOfConsumerThreads * 4);
		ExecutorService consumerThreads = Executors.newFixedThreadPool(noOfConsumerThreads);
		List<Future<FastqSummaryReport>> consumers = new ArrayList<>(noOfConsumerThreads);
		for (int i = 0 ; i < noOfConsumerThreads ; i++) {
			consumers.add(consumerThreads.submit(() -> consume(queue)));
		}
		consumerThreads.shutdown();

		try {
			long recordsRead = 0;
			try (FastqReader reader = new FastqReader(file, openForReading(file, true))) {
				List<FastqRecord> batch = new ArrayList<>(BATCH_SIZE);
				for (FastqRecord record : reader) {
					batch.add(record);
					if (batch.size() == BATCH_SIZE) {
						put(queue, batch, consumers);
						batch = new ArrayList<>(BATCH_SIZE);
					}
					if (++recordsRead % FEEDBACK_LINES_COUNT == 0) {
						logger.info("records read: " + (recordsRead / FEEDBACK_LINES_COUNT) + "M, queue size: " + queue.size());
					}
				}
				if ( ! batch.isEmpty()) {
					put(queue, batch, consumers);
				}
			}
			for (int i = 0 ; i < noOfConsumerThreads ; i++) {
				put(queue, NO_MORE_RECORDS, consumers);
			}
			logger.info("finished reading file, records read: " + recordsRead);

			// wait for the consumers to finish, and combine their reports
			for (Future<FastqSummaryReport> consumer : consumers) {
				fastqSummaryReport.merge(consumer.get());
			}
			logger.info("consumer threads have completed");
		} catch (ExecutionException e) {
			consumerThreads.shutdownNow();
			logger.error("terminating due to failed consumer thread", e.getCause());
			throw new Exception("Consumer thread failed whilst processing records", e.getCause());
		} catch (Exception e) {
			consumerThreads.shutdownNow();
			logger.error("terminating due to exception", e);
			throw e;
		}

		logger.info("records parsed: " + fastqSummaryReport.getRecordsParsed());

		fastqSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());

		logger.info("done in " + (System.currentTimeMillis() - start) / 1000 + " secs") ;

		return fastqSummaryReport;
	}

	/**
	 * Opens the file for reading, decompressing if it is gzipped (detected from the magic bytes rather than the file extension).
	 * <p>
	 * The reading thread is the bottleneck for gzipped input, and htsjdk's default (GZIPInputStream with a 512 byte buffer
	 * over an unbuffered stream) results in a lot of small native inflate calls, so use larger buffers.
	 * 
	 * @param asyncDecompression if true, gzipped input is decompressed on a separate thread
	 */
	static BufferedReader openForReading(File file, boolean asyncDecompression) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE);
		if (isGzipped(is)) {
			is = new GZIPInputStream(is, IO_BUFFER_SIZE);
			if (asyncDecompression) {
				// decompress on a separate thread so that it runs alongside the parsing of the records
				is = new AsyncInputStream(is);
			}
		}
		return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8), IO_BUFFER_SIZE);
	}

	static boolean isGzipped(File file) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			return isGzipped(is);
		}
	}

	private static boolean isGzipped(InputStream is) throws IOException {
		is.mark(2);
		int b1 = is.read();
		int b2 = is.read();
		is.reset();
		return b1 == 0x1f && b2 == 0x8b;
	}

	/**
	 * InputStream that reads the wrapped stream on a background thread, a chunk at a time,
	 * so that the (expensive) reading of the wrapped stream (eg. decompression) runs alongside whatever is consuming this stream.
	 * <p>
	 * Any exception thrown when reading the wrapped stream is re-thrown to the reader of this stream.
	 */
	static class AsyncInputStream extends InputStream {
		private static final int CHUNK_SIZE = 1024 * 1024;
		private static final byte[] END_OF_STREAM = new byte[0];

		private final BlockingQueue<byte[]> chunks = new ArrayBlockingQueue<>(8);
		private final Thread readerThread;
		private volatile IOException readerException;
		private byte[] current;
		private int currentLength;
		private int position;
		private boolean finished;

		AsyncInputStream(InputStream wrapped) {
			readerThread = new Thread(() -> {
				try (InputStream in = wrapped) {
					while (true) {
						byte[] chunk = new byte[CHUNK_SIZE];
						int length = in.readNBytes(chunk, 0, CHUNK_SIZE);
						if (length > 0) {
							chunks.put(length == CHUNK_SIZE ? chunk : Arrays.copyOf(chunk, length));
						}
						if (length < CHUNK_SIZE) {
							break;
						}
					}
				} catch (IOException e) {
					readerException = e;
				} catch (RuntimeException e) {
					readerException = new IOException(e);
				} catch (InterruptedException e) {
					// closed by the reader of this stream - nothing more to do
					return;
				}
				try {
					chunks.put(END_OF_STREAM);
				} catch (InterruptedException e) {
					// closed by the reader of this stream - nothing more to do
				}
			}, "AsyncInputStream");
			readerThread.setDaemon(true);
			readerThread.start();
		}

		/**
		 * Makes sure there are bytes available in the current chunk, returning false if the end of the stream has been reached
		 */
		private boolean fill() throws IOException {
			while ( ! finished && (current == null || position == currentLength)) {
				try {
					current = chunks.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted whilst waiting for data", e);
				}
				if (current == END_OF_STREAM) {
					finished = true;
					if (null != readerException) {
						throw readerException;
					}
				}
				currentLength = current.length;
				position = 0;
			}
			return ! finished;
		}

		@Override
		public int read() throws IOException {
			return fill() ? current[position++] & 0xFF : -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}
			if ( ! fill()) {
				return -1;
			}
			int toCopy = Math.min(len, currentLength - position);
			System.arraycopy(current, position, b, off, toCopy);
			position += toCopy;
			return toCopy;
		}

		@Override
		public void close() {
			readerThread.interrupt();
		}
	}

	private FastqSummaryReport consume(BlockingQueue<List<FastqRecord>> queue) throws InterruptedException {
		FastqSummaryReport report = new FastqSummaryReport(excludes);
		while (true) {
			List<FastqRecord> batch = queue.take();
			if (batch == NO_MORE_RECORDS) {
				break;
			}
			for (FastqRecord record : batch) {
				try {
					report.parseRecord(record);
				} catch (RuntimeException e) {
					logger.error("Error caught parsing FastqRecord with readHeader: " + record.getReadHeader(), e);
					throw e;
				}
			}
		}
		return report;
	}

	/**
	 * Places the batch on the queue, waiting for space to become available if necessary.
	 * If a consumer thread finishes (which it should only do on failure) before all batches have been queued,
	 * its exception is thrown, rather than waiting forever for space on the queue.
	 */
	private static void put(BlockingQueue<List<FastqRecord>> queue, List<FastqRecord> batch, List<Future<FastqSummaryReport>> consumers) throws InterruptedException, ExecutionException {
		while ( ! queue.offer(batch, 1, TimeUnit.SECONDS)) {
			for (Future<FastqSummaryReport> consumer : consumers) {
				if (consumer.isDone()) {
					consumer.get();
					throw new IllegalStateException("Consumer thread finished before all records were read");
				}
			}
		}
	}
}
