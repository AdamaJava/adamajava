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
package org.qcmg.qprofiler2.bam;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.SummaryReport;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BamSummarizerMT implements Summarizer {
	
	static final QLogger logger = QLoggerFactory.getLogger(BamSummarizerMT.class);	
	private int noOfProducerThreads;
	private final int noOfConsumerThreads;
	private final int maxRecords;
	private final String validation;
	private final boolean isFullBamHeader;
	private final boolean isLongReadBam;
	ValidationStringency vs;
	private static final String UNMAPPED_READS = "Unmapped";
	private static final int LONG_READ_QUEUE_CAPACITY = 1024;
	private static final int SHORT_READ_QUEUE_CAPACITY = 100000;
	
	public BamSummarizerMT(int noOfProducerThreads, int noOfThreads, int maxNoOfRecords, String validation,boolean isFullBamHeader, boolean isLongReadBam) {
		super();
		this.noOfProducerThreads = noOfProducerThreads;
		this.noOfConsumerThreads = noOfThreads;
		this.maxRecords = maxNoOfRecords;
		this.validation = validation;
		this.isFullBamHeader =  isFullBamHeader;
		this.isLongReadBam = isLongReadBam;
	}
		
	@Override
	public SummaryReport summarize(String input, String index) throws Exception {		
		
		vs = null == validation ? BamSummarizer.DEFAULT_VS : ValidationStringency.valueOf(validation);
		// check to see if index file exists - if not, run in single producer mode as will not be able to perform indexed lookups
		SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);
		if ( ! reader.hasIndex() && noOfProducerThreads > 1) {
			logger.warn("using 1 producer thread - no index found for bam file: " + input);
			noOfProducerThreads = 1;
		}	
		
		// get sorted sequence for threads
		final AbstractQueue<String> sequences = new ConcurrentLinkedQueue<>();
		SAMFileHeader header;
		try {
			header = reader.getFileHeader();
			List<SAMSequenceRecord> samSequences = header.getSequenceDictionary().getSequences();
            List<SAMSequenceRecord> orderedSamSequences = new ArrayList<>(samSequences);
			orderedSamSequences.sort((o1, o2) -> o2.getSequenceLength() - o1.getSequenceLength());
			// add the unmapped reads marker
			sequences.add(UNMAPPED_READS);
			for (SAMSequenceRecord rec : orderedSamSequences) {
				sequences.add(rec.getSequenceName());
			}			
		} finally {
			reader.close(); 
		}	
		
				
		@SuppressWarnings("unchecked")
		BlockingQueue<SAMRecord>[] queues = new BlockingQueue[noOfProducerThreads];
		
		for (int i = 0 ; i < noOfProducerThreads ; i++) {
			queues[i] = createRecordQueue();
		}
		
		final long start = System.currentTimeMillis();
        final BamSummaryReport bamSummaryReport =  BamSummarizer.createReport(header, input,  maxRecords, isFullBamHeader, isLongReadBam );
		logger.info("will create " + noOfConsumerThreads + " consumer threads");

		final CountDownLatch pLatch = new CountDownLatch(noOfProducerThreads);
		final CountDownLatch cLatch = new CountDownLatch(noOfConsumerThreads);
		ExecutorService consumerThreads = Executors.newFixedThreadPool(noOfConsumerThreads);
		for (int i = 0 ; i < noOfConsumerThreads ; i++) {
			consumerThreads.execute(noOfProducerThreads == 1 
					? new SingleProducerConsumer(queues[0], bamSummaryReport, Thread.currentThread(), cLatch, pLatch)
			 : new Consumer(queues, bamSummaryReport, Thread.currentThread(), cLatch, pLatch, i % noOfProducerThreads));
		}
		
		// setup and kick-off single Producer thread
		ExecutorService producerThreads = Executors.newFixedThreadPool(noOfProducerThreads);
		if (noOfProducerThreads == 1) {
			producerThreads.execute(new SingleProducer(queues[0], input, index, Thread.currentThread(), pLatch, cLatch));
		} else {
			for (int i = 0 ; i < noOfProducerThreads ; i++) {
				producerThreads.execute(new Producer(queues[i], input, index, Thread.currentThread(), pLatch, cLatch, sequences));
			}
		}

		// call another thread for md5 checksum
		ExecutorService md5Threads = Executors.newFixedThreadPool(1);
		
		Runnable runnableTask = bamSummaryReport::setFileMd5;
		
		md5Threads.execute(runnableTask ) ;

		// don't allow any new threads to start
		md5Threads.shutdown();
		producerThreads.shutdown();
		consumerThreads.shutdown();
		
		// wait for threads to complete
		try {
			logger.info("waiting for Producer thread to finish");
			pLatch.await();
			logger.info("producer thread finished, queue size: " +   getQueueSize(queues)   );
			
			if ( ! cLatch.await(30, TimeUnit.SECONDS)) {
			
				// need to cater for scenario where all consumer threads have died...
				// if after 10 seconds, the q size has not decreased - assume the consumer threads are no more...
				int qSize =  getQueueSize(queues)  ;
				int qSizeTheSameCounter = 0;
				while (qSize > 0 && qSizeTheSameCounter < 10) {
					Thread.sleep(1000);
					if (qSize ==   getQueueSize(queues)  ) {
						qSizeTheSameCounter++;
					} else {
						qSize = getQueueSize(queues);
						qSizeTheSameCounter = 0;	// reset to zero
					}
				}
				
				// final sleep to allow threads to finish processing final record
				if ( ! cLatch.await(10, TimeUnit.SECONDS)) {
					consumerThreads.shutdownNow();
				}
				
			} else {
				logger.info("consumer threads finished");
			}

			// if there are items left on the queue - means that the consumer threads encountered errors and were unable to complete the processing
			if ( getQueueSize(queues)  > 0 ) {
				logger.error("no Consumer threads available to process items [" + getQueueSize(queues) + "] on queue");
				throw new Exception("Consumer threads were unable to process all items on the queue");
			}
			
			logger.info("producer and consumer threads have completed");
		} catch (InterruptedException e) {
			logger.error("terminating due to failed Producer/Consumer threads");
			throw e;
		} finally {
			// Release producers waiting on full queues on any failure or cancellation.
			producerThreads.shutdownNow();
			consumerThreads.shutdownNow();
		}
		
        bamSummaryReport.cleanUp();
		logger.info("records parsed: " + bamSummaryReport.getRecordsInputed());
		
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());

        logger.info("done in " + (System.currentTimeMillis() - start) / 1000 + " secs") ;
        		
		return bamSummaryReport;
	}
	
	BlockingQueue<SAMRecord> createRecordQueue() {
		int totalCapacity = isLongReadBam ? LONG_READ_QUEUE_CAPACITY : SHORT_READ_QUEUE_CAPACITY;
		// Divide the record budget among producers rather than multiplying it by thread count.
		return new ArrayBlockingQueue<>(Math.max(1, totalCapacity / noOfProducerThreads));
	}

	static void enqueueRecord(BlockingQueue<SAMRecord> queue, SAMRecord record, CountDownLatch consumers) throws InterruptedException {
		while (true) {
			if (consumers.getCount() == 0) {
				throw new IllegalStateException("No consumer threads left to process records");
			}
			if (queue.offer(record, 100, TimeUnit.MILLISECONDS)) {
				return;
			}
		}
	}

	private int getQueueSize(BlockingQueue<SAMRecord>[] queues) {
		int totalSize = 0;
		for (BlockingQueue<SAMRecord> q : queues) {
			totalSize += q.size();
		}
		return totalSize;
	}
	
	static class SingleProducerConsumer implements Runnable {
		private final BlockingQueue<SAMRecord> queue;
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch coLatch;
		private final CountDownLatch prLatch;
		
		SingleProducerConsumer(BlockingQueue<SAMRecord> q, BamSummaryReport report, Thread mainThread, CountDownLatch coLatch, CountDownLatch prLatch) {
			queue = q;
			this.report = report;
			this.mainThread = mainThread;
			this.coLatch = coLatch;
			this.prLatch = prLatch;
		}
		
		@Override
		public void run() {
			try {
				logger.debug("start consumer");
				while (!Thread.currentThread().isInterrupted()) {
					SAMRecord record = queue.poll();
					if (record == null && prLatch.getCount() == 0) {
						// A producer may have enqueued its final record after the first poll.
						record = queue.poll();
						if (record == null) {
							break;
						}
					}
					if (null != record) {
						try {
							report.parseRecord(record);
						} catch (Exception e) {
							logger.error("record: " + record.getSAMString());
							logger.error("error caught parsing SAMRecord with readName: " + record.getReadName(), e);
							throw e;
						}
					} else {
						Thread.sleep(5);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				coLatch.countDown();
				logger.debug("consumer finished");
			}
		}
	}
	
	static class Consumer implements Runnable {
		private final BlockingQueue<SAMRecord> [] queues;
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch coLatch;
		private final CountDownLatch prLatch;
		private final int queueId;
		private final int noOfQueues;
		
		Consumer(BlockingQueue<SAMRecord>[] queues, BamSummaryReport report, Thread mainThread, CountDownLatch coLatch, CountDownLatch prLatch, int queueId) {
			this.queues = queues;
			this.report = report;
			this.mainThread = mainThread;
			this.coLatch = coLatch;
			this.prLatch = prLatch;
			this.queueId = queueId;
			this.noOfQueues = queues.length;
		}
		
		@Override
		public void run() {
			try {
				logger.debug("start consumer");
				while (!Thread.currentThread().isInterrupted()) {
					SAMRecord record = getNextRecord();
					if (record == null && prLatch.getCount() == 0) {
						record = getNextRecord();
						if (record == null) {
							break;
						}
					}
					if (null != record) {
						try {
							report.parseRecord(record);
						} catch (Exception e) {
							logger.error("record: " + record.getSAMString());
							logger.error("error caught parsing SAMRecord with readName: " + record.getReadName(), e);
							throw e;
						}
					} else {
						Thread.sleep(5);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				coLatch.countDown();
				logger.debug("consumer finished");
			}
		}
		
		/*
		 * work stealing method
		 * 
		 * the way the threads are added to the queues means that the last queues will 
		 * have fewer threads (should there not be equal no of consumers for each producer)
	 	 * and so, start from the last queue and work our way down
		 */
		private SAMRecord getNextRecord() {
			SAMRecord record = queues[queueId].poll();
			if (null != record) {
				return record;
			}
			
			for (int i = noOfQueues - 1 ; i >=  0 ; i--) {
				record = queues[i].poll();
				if (null != record) {
					return record;
				}
			}
			return null;
		}
	}

	class Producer implements Runnable {
		private final String input;
		private final String index;
		private final BlockingQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch prLatch;
		private final CountDownLatch coLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		Producer(BlockingQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch prLatch, CountDownLatch coLatch, AbstractQueue<String> sequences) {
			queue = q;
			this.input = input;
			this.index = index;
			this.mainThread = mainThread;
			this.prLatch = prLatch;
			this.coLatch = coLatch;
			this.sequences = sequences;
		}

		@Override
		public void run() {
			log.debug("start producer ");
														
			long count = 0;
			int sizeChecker = 1000000;
			try (SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs)) {
				while (true) {
					String sequence = sequences.poll();
					if (null == sequence) {
						break;
					}
					log.info("retrieving records for sequence: " + sequence);
					try (SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : reader.query(sequence, 0, 0, false)) {
						while (iter.hasNext()) {
							enqueueRecord(queue, iter.next(), coLatch);
							if (++count % sizeChecker == 0) {
								log.info(noOfProducerThreads + "(noOfProducerThreads) added " + (count / sizeChecker) + "M records, current queue size: " + queue.size());
							}
							if (maxRecords > 0 && count == maxRecords) {
								break;
							}
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				log.error(Thread.currentThread().getName() + " " + e.getMessage(), e);
				mainThread.interrupt();
			} finally {				
				prLatch.countDown();
			} 
		}
	}
	
	class SingleProducer implements Runnable {
		private final String input;
		private final String index;
		private final BlockingQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch prLatch;
		private final CountDownLatch coLatch;
		
		SingleProducer(BlockingQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch prLatch, CountDownLatch coLatch) {
			queue = q;
			this.input = input;
			this.index = index;
			this.mainThread = mainThread;
			this.prLatch = prLatch;
			this.coLatch = coLatch;
		}

		@Override
		public void run() {
			logger.debug("start producer");
			long count = 0;
			long start = System.currentTimeMillis();
			long end;
			int counter = 1000000;
			try (SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs)) {
                for (SAMRecord record : reader) {
					enqueueRecord(queue, record, coLatch);

					if (++count % counter == 0) {
						int size = queue.size();
						end = System.currentTimeMillis();
						logger.info("added " + (count / counter) + "M, q.size: " + size + ", r/ms: " + (counter / (end - start)));
						start = end;						
					}
					if (maxRecords > 0 && count == maxRecords) {
						break;
					}
				}
				
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				prLatch.countDown();
			}
		}
	}
}
