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

import java.io.IOException;
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
		ConcurrentLinkedQueue<SAMRecord>[] queues = new ConcurrentLinkedQueue[noOfProducerThreads];
		
		for (int i = 0 ; i < noOfProducerThreads ; i++) {
			queues[i] = new ConcurrentLinkedQueue<>();
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
			// restore interrupted status
			logger.info("current thread about to be interrupted...");
			
			// kill off any remaining threads
			producerThreads.shutdownNow();
			consumerThreads.shutdownNow();
			
			logger.error("terminating due to failed Producer/Consumer threads");
			throw e;
		}
		
        bamSummaryReport.cleanUp();
		logger.info("records parsed: " + bamSummaryReport.getRecordsInputed());
		
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());

        logger.info("done in " + (System.currentTimeMillis() - start) / 1000 + " secs") ;
        		
		return bamSummaryReport;
	}
	
	private int getQueueSize( AbstractQueue<SAMRecord>[]  abstractQueues) {
		int totalSize = 0;
		for (AbstractQueue<SAMRecord> q : abstractQueues) {
			totalSize += q.size();
		}
		return totalSize;
	}
	
	static class SingleProducerConsumer implements Runnable {
		private final AbstractQueue<SAMRecord> queue;
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch coLatch;
		private final CountDownLatch prLatch;
		
		SingleProducerConsumer(AbstractQueue<SAMRecord> q, BamSummaryReport report, Thread mainThread, CountDownLatch coLatch, CountDownLatch prLatch) {
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
				while ( true ) {
					SAMRecord record = queue.poll();
					if (null != record) {
						try {
							report.parseRecord(record);
						} catch (Exception e) {
							logger.error("record: " + record.getSAMString());
							logger.error("error caught parsing SAMRecord with readName: " + record.getReadName(), e);
							throw e;
						}
					} else {
						if (prLatch.getCount() == 0) {
							break;
						} else {
							Thread.sleep(5);
						}
					}
				}
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
		private final AbstractQueue<SAMRecord> [] queues;
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch coLatch;
		private final CountDownLatch prLatch;
		private final int queueId;
		private final int noOfQueues;
		
		Consumer(AbstractQueue<SAMRecord>[] queues, BamSummaryReport report, Thread mainThread, CountDownLatch coLatch, CountDownLatch prLatch, int queueId) {
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
				while ( true ) {
					SAMRecord record = getNextRecord();
					if (null != record) {
						try {
							report.parseRecord(record);
						} catch (Exception e) {
							logger.error("record: " + record.getSAMString());
							logger.error("error caught parsing SAMRecord with readName: " + record.getReadName(), e);
							throw e;
						}
					} else {
						if (prLatch.getCount() == 0) {
							break;
						} else {
							Thread.sleep(5);
						}
					}
				}
			} catch (InterruptedException e) {
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
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch prLatch;
		private final CountDownLatch coLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		Producer(AbstractQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch prLatch, CountDownLatch coLatch, AbstractQueue<String> sequences) {
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
					SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : 
						reader.query(sequence, 0, 0, false) ;
					log.info("retrieving records for sequence: " + sequence);
					
					while (iter.hasNext()) {
						queue.add(iter.next());

						
						if (++count % sizeChecker == 0) {
							long size = queue.size();
							while (size > (100000 / noOfProducerThreads)) {
								// if q size is getting too large - give the Producer a rest
								// having too many items in the queue seems to have a detrimental effect on performance.
								Thread.sleep(75);
								size = queue.size();
								log.info("sleeping, current queue size: " + size);
								if (coLatch.getCount() == 0) {
									throw new Exception("No consumer threads left, but queue is not empty");
								}
							}
							log.info(noOfProducerThreads + "(noOfProducerThreads) added " + (count / 1000000) + "M records, current queue size: " + size);
							
							if (coLatch.getCount() == 0) {
								log.error("no consumer threads left, but queue is not empty");
								throw new Exception("No consumer threads left, but queue is not empty");
							}
							

						} 
						if (maxRecords > 0 && count == maxRecords) {
							break;
						} 
					} 
					iter.close();
				}
			} catch (InterruptedException | IOException e) {
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
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch prLatch;
		private final CountDownLatch coLatch;
		
		SingleProducer(AbstractQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch prLatch, CountDownLatch coLatch) {
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
			SamReader reader = null;
			try {
				reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			
			long count = 0;
			long start = System.currentTimeMillis();
			long end;
			int counter = 1000000;
			try {
                assert reader != null;
                for (SAMRecord record : reader) {
					queue.add(record);

					if (++count % counter == 0) {
						int size = queue.size();
						// if q size is getting too large - give the Producer a rest
						// having too many items in the queue seems to have a detrimental effect on performance.
						while (size > 100000) {
							Thread.sleep(10);
							size = queue.size();
							if (coLatch.getCount() == 0) {
								throw new Exception("No consumer threads left, but queue is not empty");
							}
						}
						end = System.currentTimeMillis();
						logger.info("added " + (count / counter) + "M, q.size: " + size + ", r/ms: " + (counter / (end - start)));
						start = end;						
						if (coLatch.getCount() == 0 && size > 0) {
							logger.error("no consumer threads left, but queue is not empty");
							break;
						}										
					}
					if (maxRecords > 0 && count == maxRecords) {
						break;
					}
				}
				
				
			} catch (InterruptedException e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			} catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				try {
                    assert reader != null;
                    reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  finally {
					prLatch.countDown();
				}
			}
		}
	}
}
