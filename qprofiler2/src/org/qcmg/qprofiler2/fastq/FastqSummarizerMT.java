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
package org.qcmg.qprofiler2.fastq;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;

public class FastqSummarizerMT implements Summarizer {
	
	final static QLogger logger = QLoggerFactory.getLogger(FastqSummarizerMT.class);
	
	private final int noOfConsumerThreads;
	
	public FastqSummarizerMT(int noOfThreads) {
		super();
		this.noOfConsumerThreads = noOfThreads;
	}
	
	@Override
	public SummaryReport summarize(String file, String index) throws Exception {
		
		Queue<FastqRecord> q  = new ConcurrentLinkedQueue<>();
		
		long start = System.currentTimeMillis();
		
		final FastqSummaryReport fastqSummaryReport = new FastqSummaryReport();
		fastqSummaryReport.setFileName(file );
		fastqSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		
		// set the bam header
		
		logger.info("will create " + (noOfConsumerThreads -1 ) + " consumer threads");

		final CountDownLatch pLatch = new CountDownLatch(1);
		final CountDownLatch cLatch = new CountDownLatch(noOfConsumerThreads -1);
		ExecutorService consumerThreads = Executors.newFixedThreadPool(noOfConsumerThreads - 1);
		for (int i = 0 ; i < noOfConsumerThreads - 1 ; i++) {
			consumerThreads.execute(new Consumer(q, fastqSummaryReport, Thread.currentThread(), cLatch, pLatch));
		}
		
//		 setup and kick-off single Producer thread
		ExecutorService producerThreads = Executors.newFixedThreadPool(1);
		producerThreads.execute(new Producer(q, new File(file), Thread.currentThread(), pLatch,  cLatch));

		// don't allow any new threads to start
		producerThreads.shutdown();
		consumerThreads.shutdown();
		
		// wait for threads to complete
		try {
			logger.info("waiting for Producer thread to finish (max wait will be 20 hours)");
			if ( ! pLatch.await(20, TimeUnit.HOURS)) {
				// we've hit the 20 hour limit - shutdown the threads and throw an exception
				producerThreads.shutdownNow();
				consumerThreads.shutdownNow();
				throw new Exception("Producer thread has timed out");
			}
			logger.info("producer thread finished, queue size: " + q.size());
			
			if ( ! cLatch.await(30, TimeUnit.SECONDS)) {
			
				// need to cater for scenario where all consumer threads have died...
				// if after 10 seconds, the q size has not decreased - assume the consumer threads are no more...
				int qSize = q.size();
				int qSizeTheSameCounter = 0;
				while (qSize > 0 && qSizeTheSameCounter < 10) {
					Thread.sleep(1000);
						if (qSize == q.size()) {
						qSizeTheSameCounter++;
					} else {
						qSize =q.size();
						qSizeTheSameCounter = 0;	// reset to zero
					}
				}
				
				// final sleep to allow threads to finish processing final record
				cLatch.await(10, TimeUnit.SECONDS);
				if (cLatch.getCount() > 0)
					consumerThreads.shutdownNow();
				
			} else {
				logger.info("consumer threads finished");
			}

			// if there are items left on the queue - means that the consumer threads encountered errors and were unable to complete the processing
			if (q.size()  > 0 ) {
				logger.error("no consumer threads available to process items [" + q.size() + "] on queue");
				throw new Exception("Consumer threads were unable to process all items on the queue");
			}
			
			logger.info("producer and consumer threads have completed");
		} catch (InterruptedException e) {
			// restore interrupted status
			logger.info("current thread about to be interrupted...");
			
			// kill off any remaining threads
			producerThreads.shutdownNow();
			consumerThreads.shutdownNow();
			
			logger.error("terminating due to failed Producer/Consumer threads");
			throw e;
		}
		
		logger.info("records parsed: " + fastqSummaryReport.getRecordsParsed());
		
		fastqSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());

        logger.info("done in " + (System.currentTimeMillis() - start) / 1000 + " secs") ;
        		
		return fastqSummaryReport;
	}
	
	public static class Consumer implements Runnable {
		private final Queue<FastqRecord> queue;
		private final FastqSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		
		Consumer(Queue<FastqRecord> q, FastqSummaryReport report, Thread mainThread, CountDownLatch cLatch, CountDownLatch pLatch) {
			queue = q;
			this.report = report;
			this.mainThread = mainThread;
			this.cLatch = cLatch;
			this.pLatch = pLatch;
		}
		
		@Override
		public void run() {
			try {
				logger.debug("Start Consumer");
				while ( true ) {
					FastqRecord record = queue.poll();
					if (null != record) {
						try {
							report.parseRecord(record);
						} catch (Exception e) {
							logger.error("record: " + record.toString());
							logger.error("Error caught parsing FastqRecord with readHeader: " + record.getReadHeader(), e);
							throw e;
						}
					} else {
						if (pLatch.getCount() == 0) break;
						else Thread.sleep(5);
					}
				}
			} catch (InterruptedException e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				cLatch.countDown();
				logger.debug("Consumer finished");
			}
		}
	}
	
	public class Producer implements Runnable {
		private final File file;
		private final Queue<FastqRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		
		Producer(Queue<FastqRecord> q, File f, Thread mainThread, CountDownLatch pLatch, CountDownLatch cLatch) {
			queue = q;
			file = f;
			this.mainThread = mainThread;
			this.cLatch = cLatch;
			this.pLatch = pLatch;
		}

		@Override
		public void run() {
			logger.debug("Start Producer");
			
			
			int size = 0;
			int count = 0;
			int millions = 0;
			
//			long start = System.currentTimeMillis();
//			long end = 0;
			final int counter = 1000000;
			
			try (FastqReader reader =  new FastqReader(file);) {
				for (FastqRecord record : reader) {
					queue.add(record);
					
					if (++count == counter) {
						millions++;
						count = 0;
						size = queue.size();
//						end = System.currentTimeMillis();
						logger.info("added " + millions + "M, q.size: " + size);
//						start = end;
						
						if (cLatch.getCount() == 0 && size > 0) {
							logger.error("No consumer threads left, but queue is not empty");
							break;
						}
						
						// if q size is getting too large - give the Producer a rest
						// having too many items in the queue seems to have a detrimental effect on performance.
						while (size > 100000) {
							Thread.sleep(50);
							size = queue.size();
							if (cLatch.getCount() == 0) {
								throw new Exception("No consumer threads left, but queue is not empty");
							}
						}
					}
				}
			} catch (InterruptedException e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				pLatch.countDown();
			}
		}
	}
}
