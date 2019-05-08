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

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.qcmg.qprofiler2.report.SummaryReport;


public class BamSummarizerMT2 implements Summarizer {
	
	final static QLogger logger = QLoggerFactory.getLogger(BamSummarizerMT2.class);
	
	private int noOfProducerThreads;
	private final int noOfConsumerThreads;
	private final int maxRecords;
	private final String validation;
	private final boolean isFullBamHeader;
	ValidationStringency vs;
	private final static String UNMAPPED_READS = "Unmapped";

	
	public BamSummarizerMT2(int noOfProducerThreads, int noOfThreads, int maxNoOfRecords, String validation,boolean isFullBamHeader) {
		super();
		this.noOfProducerThreads = noOfProducerThreads;
		this.noOfConsumerThreads = noOfThreads;
		this.maxRecords = maxNoOfRecords;
		this.validation = validation;
		this.isFullBamHeader =  isFullBamHeader;
	}
		
	@Override
	public SummaryReport summarize(String input, String index) throws Exception {
		
		File file = new File(input);
		vs= null == validation ? BamSummarizer2.DEFAULT_VS : ValidationStringency.valueOf(validation);
		// check to see if index file exists - if not, run in single producer mode as will not be able to perform indexed lookups
		SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);
		if ( ! reader.hasIndex() && noOfProducerThreads > 1) {
			logger.warn("using 1 producer thread - no index found for bam file: " + input);
			noOfProducerThreads = 1;
		}	
		
		//get sorted sequence for threads
		final AbstractQueue<String> sequences = new ConcurrentLinkedQueue<>();
		try {
			SAMFileHeader header = reader.getFileHeader();
			List<SAMSequenceRecord> samSequences = header.getSequenceDictionary().getSequences();
			List<SAMSequenceRecord> orderedSamSequences = new ArrayList<SAMSequenceRecord>();
			orderedSamSequences.addAll(samSequences);
			Collections.sort(orderedSamSequences, new Comparator<SAMSequenceRecord>(){
				@Override
				public int compare(SAMSequenceRecord o1, SAMSequenceRecord o2) {
					return o2.getSequenceLength() - o1.getSequenceLength();
				}
			});
			// add the unmapped reads marker
			sequences.add(UNMAPPED_READS);
			for (SAMSequenceRecord rec : orderedSamSequences) {
				sequences.add(rec.getSequenceName());
			}			
		} finally { reader.close(); }	
		
				
		@SuppressWarnings("unchecked")
		ConcurrentLinkedQueue<SAMRecord>[] queues = new ConcurrentLinkedQueue[noOfProducerThreads];
		for (int i = 0 ; i < noOfProducerThreads ; i++) {  
			queues[i] = new ConcurrentLinkedQueue<SAMRecord>();	
		}
		
		long start = System.currentTimeMillis();
		
		final BamSummaryReport2 bamSummaryReport =  BamSummarizer2.createReport(file,  maxRecords, isFullBamHeader );				 		
		logger.info("will create " + noOfConsumerThreads + " consumer threads");

		final CountDownLatch pLatch = new CountDownLatch(noOfProducerThreads);
		final CountDownLatch cLatch = new CountDownLatch(noOfConsumerThreads);
		ExecutorService consumerThreads = Executors.newFixedThreadPool(noOfConsumerThreads);
		for (int i = 0 ; i < noOfConsumerThreads ; i++) {
			consumerThreads.execute(noOfProducerThreads == 1 
					? new SingleProducerConsumer(queues[0], bamSummaryReport, Thread.currentThread(), cLatch, pLatch)
			 : new Consumer(queues, bamSummaryReport, Thread.currentThread(), cLatch, pLatch, i % noOfProducerThreads));
		}
		
		// setpup and kick-off single Producer thread
		ExecutorService producerThreads = Executors.newFixedThreadPool(noOfProducerThreads);
		if (noOfProducerThreads == 1) {
			producerThreads.execute(new SingleProducer(queues[0], file, Thread.currentThread(), pLatch, cLatch));
		} else {
			for (int i = 0 ; i < noOfProducerThreads ; i++) {
				producerThreads.execute(new Producer(queues[i], file, Thread.currentThread(), pLatch, cLatch, sequences, bamSummaryReport));
			}
		}

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
		for (AbstractQueue<SAMRecord> q : abstractQueues)
			totalSize += q.size();
		return totalSize;
	}
	
	public static class SingleProducerConsumer implements Runnable {
		private final AbstractQueue<SAMRecord> queue;
		private final BamSummaryReport2 report;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		
		SingleProducerConsumer(AbstractQueue<SAMRecord> q, BamSummaryReport2 report, Thread mainThread, CountDownLatch cLatch, CountDownLatch pLatch) {
			queue = q;
			this.report = report;
			this.mainThread = mainThread;
			this.cLatch = cLatch;
			this.pLatch = pLatch;
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
				logger.debug("consumer finished");
			}
		}
	}
	
	public static class Consumer implements Runnable {
		private final AbstractQueue<SAMRecord> [] queues;
		private final BamSummaryReport2 report;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		private final int queueId;
		private final int noOfQueues;
		
		Consumer(AbstractQueue<SAMRecord>[] queues, BamSummaryReport2 report, Thread mainThread, CountDownLatch cLatch, CountDownLatch pLatch, int queueId) {
			this.queues = queues;
			this.report = report;
			this.mainThread = mainThread;
			this.cLatch = cLatch;
			this.pLatch = pLatch;
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
			if (null != record) return record;
			
			for (int i = noOfQueues - 1 ; i >=  0 ; i--) {
				record = queues[i].poll();
				if (null != record) return record;
			}
			return null;
		}
	}

	public class Producer implements Runnable {
		private final File file;
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final CountDownLatch cLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		Producer(AbstractQueue<SAMRecord> q, File f, Thread mainThread, CountDownLatch pLatch, CountDownLatch cLatch, AbstractQueue<String> sequences, BamSummaryReport2 report) {
			queue = q;
			file = f;
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.cLatch = cLatch;
			this.sequences = sequences;
		}

		@SuppressWarnings("resource")
		@Override
		public void run() {
			log.debug("start producer ");
											
			
			long count = 0;
			try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);) {
				while (true) {
					String sequence = sequences.poll();
					if (null == sequence) break;
					SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : reader.query(sequence, 0, 0, false) ;
					log.info("retrieving records for sequence: " + sequence);
					
					while (iter.hasNext()) {
						queue.add(iter.next());
						long size = queue.size();
						while (size > (100000 / noOfProducerThreads)) {
							//while (size > (100000 / noOfProducerThreads)) {
								Thread.sleep(75);
								size = queue.size();
								log.info("sleep when queue size is " + size +  " records, current queu size: " + size);
								if (cLatch.getCount() == 0) {
									throw new Exception("No consumer threads left, but queue is not empty");
								}
							}
						
						if (++count % 2000000 == 0) {
							size = queue.size();
							log.info(noOfProducerThreads +"(noOfProducerThreads) added " + count/1000000 + "M records, current queu size: " + size);
							
							if (cLatch.getCount() == 0) {
								log.error("no consumer threads left, but queue is not empty");
								throw new Exception("No consumer threads left, but queue is not empty");
							}
							
							// if q size is getting too large - give the Producer a rest
							// having too many items in the queue seems to have a detrimental effect on performance.
							
						}
						if (maxRecords > 0 && count == maxRecords)
							break;
					}
					iter.close();
				}
			} catch (InterruptedException | IOException e) {
				log.info(Thread.currentThread().getName() + " " + e.getMessage());
			}catch (Exception e) {
				log.error(Thread.currentThread().getName() + " " + e.getMessage(), e);
				mainThread.interrupt();
			} finally {				
				pLatch.countDown();
			}
		}
	}
	public class SingleProducer implements Runnable {
		private final File file;
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final CountDownLatch cLatch;
		
		SingleProducer(AbstractQueue<SAMRecord> q, File f, Thread mainThread, CountDownLatch pLatch, CountDownLatch cLatch) {
			queue = q;
			file = f;
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.cLatch = cLatch;
		}

		@Override
		public void run() {
			logger.debug("start producer");
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);

			
			long count = 0;
			long start = System.currentTimeMillis();
			long end = 0;
			int counter = 1000000;
			try {
				for (SAMRecord record : reader) {					
					queue.add(record);
					
					// if q size is getting too large - give the Producer a rest
					// having too many items in the queue seems to have a detrimental effect on performance.
					int size = queue.size();
					while (size > 100000) {
						Thread.sleep(10);
						size = queue.size();
						if (cLatch.getCount() == 0) {
							throw new Exception("No consumer threads left, but queue is not empty");
						}
					}
										
					if (++count % counter == 0) {
						size = queue.size();
						end = System.currentTimeMillis();
						logger.info("added " + count/counter + "M, q.size: " + size + ", r/ms: " + (counter / (end - start)));
						start = end;						
						if (cLatch.getCount() == 0 && size > 0) {
							logger.error("no consumer threads left, but queue is not empty");
							break;
						}										
					}
					if (maxRecords > 0 && count == maxRecords)
						break;
				}
				
				
			} catch (InterruptedException e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			}catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				try {reader.close(); } 
				catch (IOException e) {e.printStackTrace(); } 
				finally {pLatch.countDown();}
			}
		}
	}
}
