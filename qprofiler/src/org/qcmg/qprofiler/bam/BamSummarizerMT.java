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
package org.qcmg.qprofiler.bam;

import static java.util.stream.Collectors.toList;

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

import htsjdk.samtools.*;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

/**
 * This class will be called only when option "--threads-consumer" is specified and value > 0;
 * otherwise a single thread mode will be called, which is BamSummarizer.
 * 
 * Here A SingleProducer will be created if without option "--threads-producer" or value <= 1; 
 * otherwise multiply Producer will be created based on value of "--threads-producer". 
 * 
 * Once A SingleProducer is created, a number of SingleProducerConsumer will be created based on "--threads-consumer" value.
 * 
 * By summary 
 * --------------------------------------------------------------------------------------------
 * | threads-consumer | threads-producer  | threads name
 * | null,0 (def = 0 )| null,0,1 (def = 1)| single thread mode 
 * | null,0 (def = 0 )| 	> 1			  | single thread mode 
 * |	>= 1		  | null,0,1(def = 1 )| a SingleProducer + multiply SingleProducerConsumer
 * |	>= 1		  | 	> 1			  | multiply Producer + multiply Consumer
 * ______________________________________________________________________________________________
 * 
 * @author christix
 *
 */
public class BamSummarizerMT implements Summarizer {
	
	final static QLogger logger = QLoggerFactory.getLogger(BamSummarizerMT.class);
	
	private int noOfProducerThreads;
	private final int noOfConsumerThreads;
	private final int maxRecords;
	private final String [] includes;
	private static String bamHeader;
	private static SAMSequenceDictionary samSeqDict;
	private static List<String> readGroupIds;
	private final String [] tags;
	private final String [] tagsInt;
	private final String [] tagsChar;
	private final String validation;
	ValidationStringency vs;
	private final static String UNMAPPED_READS = "Unmapped";
	private boolean torrentBam = false;
	
	public BamSummarizerMT(int noOfProducerThreads, int noOfThreads, String [] includes, 
			int maxNoOfRecords, String [] tags, String [] tagsInt, String [] tagsChar, String validation) {
		super();
		this.noOfProducerThreads = noOfProducerThreads;
		this.noOfConsumerThreads = noOfThreads;
		this.includes = includes;
		this.maxRecords = maxNoOfRecords;
		this.tags = tags;
		this.tagsInt = tagsInt;
		this.tagsChar = tagsChar;
		this.validation = validation;
	}
		
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception {
		
		vs= null == validation ? BamSummarizer.DEFAULT_VS : ValidationStringency.valueOf(validation);
		// check to see if index file exists - if not, run in single producer mode as will not be able to perform indexed lookups		
		final AbstractQueue<String> sequences = new ConcurrentLinkedQueue<>();
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);) {
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
			
			samSeqDict = reader.getFileHeader().getSequenceDictionary();
			bamHeader = HeaderUtils.getHeaderStringFromHeader(header);
			readGroupIds = header.getReadGroups().stream().map(SAMReadGroupRecord::getId).collect(toList());

			List<SAMProgramRecord> pgLines = header.getProgramRecords();
			for (SAMProgramRecord pgLine : pgLines) {
				if ("tmap".equals(pgLine.getId())) torrentBam = true;
			}
			if ( ! reader.hasIndex() && noOfProducerThreads > 1) {
				logger.warn("using 1 producer thread - no index found for bam file: " + input);
				noOfProducerThreads = 1;
			}						
		} 	
		
		ConcurrentLinkedQueue<SAMRecord>[] queues = null;
		AbstractQueue<SAMRecord> q  = null;
		if (noOfProducerThreads == 1) {
			q = new ConcurrentLinkedQueue<>();
		} else {
			queues = new ConcurrentLinkedQueue[noOfProducerThreads];
			for (int i = 0 ; i < noOfProducerThreads ; i++) {
				queues[i] = new ConcurrentLinkedQueue<SAMRecord>();
			}
		}
		long start = System.currentTimeMillis();
		
		final BamSummaryReport bamSummaryReport = new BamSummaryReport( includes, maxRecords, tags, tagsInt, tagsChar );
		bamSummaryReport.setFileName(input);
		bamSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		

		
		bamSummaryReport.setTorrentBam(torrentBam);
		// set the bam header
		bamSummaryReport.setBamHeader(bamHeader);
		bamSummaryReport.setSamSequenceDictionary(samSeqDict);
		bamSummaryReport.setReadGroups(readGroupIds);
		 		
		logger.info("will create " + noOfConsumerThreads + " consumer threads");

		final CountDownLatch pLatch = new CountDownLatch(noOfProducerThreads);
		final CountDownLatch cLatch = new CountDownLatch(noOfConsumerThreads);
		ExecutorService consumerThreads = Executors.newFixedThreadPool(noOfConsumerThreads);
		for (int i = 0 ; i < noOfConsumerThreads ; i++) {
			consumerThreads.execute(noOfProducerThreads == 1 
					? new SingleProducerConsumer(q, bamSummaryReport, Thread.currentThread(), cLatch, pLatch)
			 : new Consumer(queues, bamSummaryReport, Thread.currentThread(), cLatch, pLatch, i % noOfProducerThreads));
		}
		
//		setpup and kick-off single Producer thread
		ExecutorService producerThreads = Executors.newFixedThreadPool(noOfProducerThreads);
		if (noOfProducerThreads == 1) {
			producerThreads.execute(new SingleProducer(q, input, index, Thread.currentThread(), pLatch, cLatch));
		} else {
			for (int i = 0 ; i < noOfProducerThreads ; i++) {
				producerThreads.execute(new Producer(queues[i], input, index, Thread.currentThread(), pLatch, cLatch, sequences, bamSummaryReport));
			}
		}

		// don't allow any new threads to start
		producerThreads.shutdown();
		consumerThreads.shutdown();
		
		// wait for threads to complete
		try {
			logger.info("waiting for Producer thread to finish");
			 pLatch.await();
			logger.info("producer thread finished, queue size: " + (q == null ? getQueueSize(queues) : getQueueSize(q) ) );
			
			if ( ! cLatch.await(30, TimeUnit.SECONDS)) {
			
				// need to cater for scenario where all consumer threads have died...
				// if after 10 seconds, the q size has not decreased - assume the consumer threads are no more...
				int qSize = q == null ? getQueueSize(queues) : getQueueSize(q);
				int qSizeTheSameCounter = 0;
				while (qSize > 0 && qSizeTheSameCounter < 10) {
					Thread.sleep(1000);
						if (qSize == (q == null ? getQueueSize(queues) : getQueueSize(q))) {
						qSizeTheSameCounter++;
					} else {
						qSize = (q == null ? getQueueSize(queues) : getQueueSize(q));
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
			if ((q == null ? getQueueSize(queues) : getQueueSize(q))  > 0 ) {
				logger.error("no Consumer threads available to process items [" + q.size() + "] on queue");
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
		logger.info("records parsed: " + bamSummaryReport.getRecordsParsed());
		
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());

        logger.info("done in " + (System.currentTimeMillis() - start) / 1000 + " secs") ;
        		
		return bamSummaryReport;
	}
	
	private int getQueueSize(AbstractQueue<SAMRecord> ...abstractQueues) {
		int totalSize = 0;
		for (AbstractQueue<SAMRecord> q : abstractQueues)
			totalSize += q.size();
		return totalSize;
	}
	
	public static class SingleProducerConsumer implements Runnable {
		private final AbstractQueue<SAMRecord> queue;
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		
		SingleProducerConsumer(AbstractQueue<SAMRecord> q, BamSummaryReport report, Thread mainThread, CountDownLatch cLatch, CountDownLatch pLatch) {
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
		private final BamSummaryReport report;
		private final Thread mainThread;
		private final CountDownLatch cLatch;
		private final CountDownLatch pLatch;
		private final int queueId;
		private final int noOfQueues;
		
		Consumer(AbstractQueue<SAMRecord>[] queues, BamSummaryReport report, Thread mainThread, CountDownLatch cLatch, CountDownLatch pLatch, int queueId) {
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
		private final String input;
		private final String index;
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final CountDownLatch cLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		Producer(AbstractQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch pLatch, CountDownLatch cLatch, AbstractQueue<String> sequences, BamSummaryReport report) {
			queue = q;
			this.input = input;
			this.index = index;
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.cLatch = cLatch;
			this.sequences = sequences;
		}

		@Override
		public void run() {
			log.debug("start producer ");
			
			SamReader reader = null;
			try {
				reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			long size = 0;
			long count = 0;
			try {
				while (true) {
					String sequence = sequences.poll();
					if (null == sequence) break;
					SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : reader.query(sequence, 0, 0, false) ;
					log.info("retrieving records for sequence: " + sequence);
					while (iter.hasNext()) {
						queue.add(iter.next());
						
						if (++count % 2000000 == 0) {
							size = queue.size();
							log.info("added " + count/1000000 + "M records, current queue size: " + size);
							
							if (cLatch.getCount() == 0) {
								log.error("no consumer threads left, but queue is not empty");
								throw new Exception("No consumer threads left, but queue is not empty");
//								break;
							}
							
							// if q size is getting too large - give the Producer a rest
							// having too many items in the queue seems to have a detrimental effect on performance.
							//FIXME 4???
							while (size > (100000 / noOfProducerThreads)) {
//								sleepCounter++;
								Thread.sleep(75);
								size = queue.size();
								if (cLatch.getCount() == 0) {
									throw new Exception("No consumer threads left, but queue is not empty");
								}
							}
						}
						if (maxRecords > 0 && count == maxRecords)
							break;
					}
					iter.close();
				}
				
			} catch (InterruptedException e) {
				log.info(Thread.currentThread().getName() + " " + e.getMessage());
			}catch (Exception e) {
				log.error(Thread.currentThread().getName() + " " + e.getMessage(), e);
				mainThread.interrupt();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					pLatch.countDown();
				}
			}
		}
	}
	public class SingleProducer implements Runnable {
		private final String input;
		private final String index;
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final CountDownLatch cLatch;
		
		SingleProducer(AbstractQueue<SAMRecord> q, String input, String index, Thread mainThread, CountDownLatch pLatch, CountDownLatch cLatch) {
			queue = q;
			this.input = input;
			this.index = index;
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.cLatch = cLatch;
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
			
			int size = 0;
			long count = 0;
			long start = System.currentTimeMillis();
			long end = 0;
			int counter = 1000000;
			try {
				for (SAMRecord record : reader) {
					
					queue.add(record);
					
					if (++count % counter == 0) {
						size = queue.size();
						end = System.currentTimeMillis();
						logger.info("added " + count/counter + "M, q.size: " + size + ", r/ms: " + (counter / (end - start)));
						start = end;
						
						if (cLatch.getCount() == 0 && size > 0) {
							logger.error("no consumer threads left, but queue is not empty");
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
					if (maxRecords > 0 && count == maxRecords)
						break;
				}
				
				// set the bam header
				bamHeader = reader.getFileHeader().getTextHeader();
				
			} catch (InterruptedException e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
			}catch (Exception e) {
				logger.info(Thread.currentThread().getName() + " " + e.getMessage());
				mainThread.interrupt();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					pLatch.countDown();
				}
			}
		}
	}
}
