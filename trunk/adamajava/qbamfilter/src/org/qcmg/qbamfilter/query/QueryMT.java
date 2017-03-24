/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.query;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;

public class QueryMT {

	public class WritingUnmatched implements Runnable {
		private final File unmatchedFile;
		private final AbstractQueue<SAMRecord> outBadQueue;
		private final Thread currentThread;
		private final CountDownLatch fLatch;
		private final CountDownLatch wBadLatch;
		private final CountDownLatch wGoodLatch;

		public WritingUnmatched(File funmatch,
				AbstractQueue<SAMRecord> outBadQueue, Thread currentThread,
				CountDownLatch fLatch, CountDownLatch wGoodLatch, CountDownLatch wBadLatch) {
			this.unmatchedFile = funmatch;
			this.outBadQueue = outBadQueue;
			this.currentThread = currentThread;
			this.fLatch = fLatch;
			this.wBadLatch = wBadLatch;
			this.wGoodLatch = wGoodLatch;
		}

		@Override
		public void run() {
			try {
				boolean presorted = header.getSortOrder().equals(sort); 
				boolean index =  sort.equals(SAMFileHeader.SortOrder.coordinate);
				SAMFileHeader he = header.clone();
				he.setSortOrder(sort);
				 
				SAMOrBAMWriterFactory writeFactory = new SAMOrBAMWriterFactory(he, presorted, unmatchedFile,tmpdir, index);
				
				logger.info("input bam are presorted: " + presorted);
				logger.info("set sort to unmatched reads output:" + sort);
				logger.info("create index file for unmatched reads output " + index);				
				int unmatchedCount = 0;
				try {
					SAMRecord record;					
					while (true) {
						if (  (record = outBadQueue.poll()) == null ){
							if ((fLatch.getCount() == 0) && wGoodLatch.getCount() == 0)
								break;
							try {
								Thread.sleep(sleepUnit);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							writeFactory.getWriter().addAlignment(record);
							unmatchedCount ++ ;						
						}
					}
				} finally {
					writeFactory.closeWriter();
					logger.info(writeFactory.getLogMessage());
					logger.info("completed writing threads, added " + unmatchedCount
									+ " records to the output: " + unmatchedFile.getAbsolutePath());
				}
				
			} finally {
				wBadLatch.countDown();
			}
		}
	}

	private final QLogger logger;
	private final int noOfThreads;
	private final int maxRecords;
	private final int checkPoint;
	private final File input;
	private final File output;
	private final File filterOut;
	private final String query;
	private final SAMFileHeader header;
	private final SAMFileHeader.SortOrder sort;
	private final File tmpdir;
	private final int sleepUnit = 10;
	private final ValidationStringency validation;

	public QueryMT(Options options, QLogger logger) throws Exception {
		noOfThreads = options.getThreadNumber();
		maxRecords = options.getMaxRecordNumber();
		checkPoint = options.getCheckPoint();

		this.logger = logger;
		input = new File(options.getInputFileName());
		output = new File(options.getOutputFileName());
		if (options.getFiltedFileName() != null)
			filterOut = new File(options.getFiltedFileName());
		else
			filterOut = null;
		query = options.getQuery();
		header = options.getHeader();
		sort = options.getSortOrder();
		if(options.getTmpDir() != null)
			tmpdir = new File(options.getTmpDir());
		else
			tmpdir = null;
		
		validation = options.getValidation();
	}

	public void executor() throws Exception {
		// create queue to store the records from input
		final AbstractQueue<SAMRecordFilterWrapper> readQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		final AbstractQueue<SAMRecordFilterWrapper> postfilteringQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		final AbstractQueue<SAMRecord> outBadQueue = filterOut == null ? null
				: new ConcurrentLinkedQueue<SAMRecord>();

		final CountDownLatch rLatch = new CountDownLatch(1); // reading thread
		final CountDownLatch fLatch = new CountDownLatch(noOfThreads); // filtering
																		// thread
		final CountDownLatch wGoodLatch = new CountDownLatch(1); // writing
																	// thread
																	// for
																	// satisfied
																	// records
		final CountDownLatch wBadLatch = new CountDownLatch(1); // writing
																// thread for
																// unmatched
																// records

		// set up and kick-off single writing thread to output the satisfied
		// Records
		// Writing Threads
		ExecutorService writeThreadA = Executors.newSingleThreadExecutor();
		ExecutorService writeThreadB = Executors.newSingleThreadExecutor();

		writeThreadA.execute(new Writing(postfilteringQueue, output,
				outBadQueue, Thread.currentThread(), fLatch, wGoodLatch));
		writeThreadA.shutdown();
		// kick-off single writing thread to output the unmatched Records if
		// required
		if (filterOut != null) {
			writeThreadB.execute(new WritingUnmatched(filterOut, outBadQueue,
					Thread.currentThread(), fLatch,wGoodLatch, wBadLatch));
			writeThreadB.shutdown();
		}

		// Filtering Threads
		ExecutorService filterThreads = Executors
				.newFixedThreadPool(noOfThreads);
		for (int i = 0; i < noOfThreads; i++) {
			filterThreads.execute(new Filtering(readQueue, postfilteringQueue,
					Thread.currentThread(), rLatch, fLatch, wGoodLatch,
					wBadLatch));
		}
		filterThreads.shutdown();

		// setpup and kick-off single reading thread
		// Reading Thread
		ExecutorService readThreads = Executors.newSingleThreadExecutor();
		readThreads.execute(new Reading(readQueue, Thread.currentThread(),
				rLatch, fLatch));
		readThreads.shutdown();

		// wait for threads to complete
		try {
			logger.info("waiting for  threads to finish (max wait will be 20 hours)");
			readThreads.awaitTermination(20, TimeUnit.HOURS);
			filterThreads.awaitTermination(20, TimeUnit.HOURS);
			writeThreadA.awaitTermination(20, TimeUnit.HOURS);

			if (outBadQueue != null) {
				writeThreadB.awaitTermination(20, TimeUnit.HOURS);
				if (outBadQueue.size() != 0)
					throw new Exception(" threads have completed but Queue of unmatched Records isn't empty):  "
									+ outBadQueue.size());
			}

			if (readQueue.size() != 0 || postfilteringQueue.size() != 0)
				throw new Exception(" threads have completed but queue isn't empty  (inputQueue, postfilteringQueue ):  "
								+ readQueue.size() + ", " + postfilteringQueue.size());

			logger.info("all threads finished");
		} catch (Exception e) {
			logger.error("exception caught whilst waiting for threads to finish: " + e.getMessage(), e);
			throw e;
		} finally {
			// kill off any remaining threads
			readThreads.shutdownNow();
			writeThreadA.shutdownNow();
			filterThreads.shutdownNow();
			if (filterOut != null)
				writeThreadB.shutdownNow();
		}
	}

	class Reading implements Runnable {
		private final AbstractQueue<SAMRecordFilterWrapper> queue;
		private final Thread mainThread;
		final CountDownLatch rLatch;
		final CountDownLatch fLatch;

		Reading(AbstractQueue<SAMRecordFilterWrapper> q, Thread mainThread,
				CountDownLatch rLatch, CountDownLatch fLatch) {
			queue = q;

			this.mainThread = mainThread;
			this.rLatch = rLatch;
			this.fLatch = fLatch;
		}

		@Override
		public void run() {
			logger.info("start read input: " + input.getAbsolutePath());
			int countSleep = 0;
			long count = 0;
			try {
				SamReader reader = SAMFileReaderFactory.createSAMFileReader(input,validation);
				try {
					for (SAMRecord record : reader) {
	
						SAMRecordFilterWrapper wrapper = new SAMRecordFilterWrapper(record, ++count);
						queue.add(wrapper);
	
						if (fLatch.getCount() == 0)
							throw new Exception(
									"No filtering threads left, but reading from input is not yet completed");
	
						if (count % checkPoint == 0) { // checkPoint defaults to 100000
							while (queue.size() >= maxRecords) {								
								try {
									Thread.sleep(100);
									countSleep++;
								} catch (Exception e) {
									logger.info(Thread.currentThread().getName() + " " + e.getMessage());
								}
							}
						}
	
					}// end for loop
				} finally {
					reader.close();
				}
				logger.info("completed reading thread, read " + count
						+ " records from input: " + input.getAbsolutePath());
			} catch (Exception e) {
				logger.error("Exception caught in Reading thread", e);
				mainThread.interrupt();
			} finally {
				rLatch.countDown();
				logger.debug(String.format("Exit Reading thread, total slept %d times * %d milli-seconds, "
					+ "since input queue are full.fLatch  is %d; queus size is %d ",
					countSleep, sleepUnit, fLatch.getCount(), queue.size()));
			}

		}
	}

	class Filtering implements Runnable {

		private final AbstractQueue<SAMRecordFilterWrapper> qIn;
		private final AbstractQueue<SAMRecordFilterWrapper> qOut;

		private final Thread mainThread;
		final CountDownLatch rLatch;
		final CountDownLatch fLatch;
		final CountDownLatch wGoodLatch;
		final CountDownLatch wBadLatch;

		private int countOutputSleep;

		/**
		 * 
		 * @param qIn : store SAM record from input file
		 * @param qOutGood : store unmatched record based on query
		 * @param qOutBad: store unmatched record based on query (null is allowed)
		 * @param query : query string
		 * @param maxRecords : queue size
		 * @param mainThread : parent thread
		 * @param rLatch : the counter for reading thread
		 * @param fLatch : the counter for filtering thread (current type)
		 */
		Filtering(AbstractQueue<SAMRecordFilterWrapper> qIn,
				AbstractQueue<SAMRecordFilterWrapper> qOut, Thread mainThread,
				CountDownLatch rLatch, CountDownLatch fLatch,
				CountDownLatch wGoodLatch, CountDownLatch wBadLatch)
				throws Exception {
			this.qIn = qIn;
			this.qOut = qOut;
			this.mainThread = mainThread;
			this.rLatch = rLatch;
			this.fLatch = fLatch;
			this.wGoodLatch = wGoodLatch;
			this.wBadLatch = wBadLatch;

		}

		@Override
		public void run() {

			long sleepcount = 0;
			long count = 0;
			countOutputSleep = 0;
			boolean run = true;

			try {
				QueryExecutor ExQuery = new QueryExecutor(query);
				SAMRecordFilterWrapper record;

				while (run) {
					record = qIn.poll();

					if (record == null) {
						// must check whether reading thread finished first.
						if (rLatch.getCount() == 0) {
							run = false;
						}

						// qIn maybe filled again during sleep, so sleep should
						// be secondly
						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.info(Thread.currentThread().getName() + " "
									+ e.toString());
						}

					} else {
						// pass the record to filter and then to output queue
						record.setPassesFilter(ExQuery.Execute(record.getRecord()));
						qOut.add(record);

						if (++count % 500000 == 0) {
							int sortingQueueSize = qOut.size();
							if (count % 1000000 == 0) {
								logger.info("filter thread record count: " + count
										+ ", sorting queue size: "
										+ sortingQueueSize);
							}
							while (sortingQueueSize > 200000) {
								Thread.sleep(250);
								sortingQueueSize = qOut.size();
							}
						}

					} // end else
				}// end while
				logger.info("completed filtering thread: "
						+ Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("exception caught in Filterer thread", e);
				mainThread.interrupt();
			} finally {
				fLatch.countDown();
				logger.debug(String.format(" total slept %d times since input queue is " +
						"empty and %d time since either output queue is full. " +
						"each sleep take %d mill-second. queue size for qIn, qOutqOutBad are %d,%d",
						sleepcount, countOutputSleep, sleepUnit, qIn.size(), qOut.size()));

			}
		}
	}

	class Writing implements Runnable {
		private final File file;
		private final AbstractQueue<SAMRecordFilterWrapper> queue;
		private final Thread mainThread;
		private final AbstractQueue<SAMRecord> outBadQueue;

		final CountDownLatch fLatch;
		final CountDownLatch wLatch;

		private long count = 1;
		private long filteredCount;
		private final boolean writeUnmatchedOutput;

		Writing(AbstractQueue<SAMRecordFilterWrapper> q, File f,
				AbstractQueue<SAMRecord> outBadQueue, Thread mainThread,
				CountDownLatch fLatch, CountDownLatch wLatch) {
			queue = q;
			file = f;
			this.mainThread = mainThread;
			this.fLatch = fLatch;
			this.wLatch = wLatch;
			this.outBadQueue = outBadQueue;
			writeUnmatchedOutput = null != outBadQueue;
		}

		@Override
		public void run() {
			int countSleep = 0;
			boolean run = true;
			
			try {
				boolean presorted = header.getSortOrder().equals(sort);
				boolean index =  sort.equals(SAMFileHeader.SortOrder.coordinate);
				//make sure don't change the input header since the writer of unmatched reads  should use it. 
				SAMFileHeader he = header.clone();
				he.setSortOrder(sort);
				SAMOrBAMWriterFactory writeFactory = new SAMOrBAMWriterFactory(he , presorted, file,tmpdir, index);
				
				logger.info("input bam are presorted: " + presorted);
				logger.info("set sort to " + sort);
				logger.info("create index file " + index);
				
				try {
					List<SAMRecordFilterWrapper> unorderedRecords = new ArrayList<SAMRecordFilterWrapper>();
					SAMRecordFilterWrapper record;
					while (run) {
						// when queue is empty,maybe filtering is done
						if ((record = queue.poll()) == null) {
							if (fLatch.getCount() == 0)
								run = false;
							try {
								Thread.sleep(sleepUnit);
								countSleep++;
							} catch (Exception e) {
								logger.info(Thread.currentThread().getName() + " " + e.getMessage());
							}

							if ((count % checkPoint == 0) && (!mainThread.isAlive()))
								throw new Exception( "Writing threads failed since parent thread died.");

						} else {
							if (count == record.getPosition()) {
								addRecordToWriter(record, writeFactory.getWriter());

								// check the list to see if the next element has already been retrieved
								int size = unorderedRecords.size();
								if (size > 0) {
									int i = 0;
									if (size > 1) {
										Collections.sort(unorderedRecords);
									}
									while (i < size
											&& (record = unorderedRecords
													.get(0)).getPosition() == count) {
										addRecordToWriter(record, writeFactory.getWriter());
										unorderedRecords.remove(0);
										i++;
									}
								}
							} else if (count < record.getPosition()) {
								unorderedRecords.add(record);
							} else {
								logger.error("count: " + count + " is higher that recently processed record: " + record.getPosition());
							}
						}
					}
				} finally {
					writeFactory.closeWriter();
					logger.info(writeFactory.getLogMessage());
				}

				if (!mainThread.isAlive())
					throw new Exception("Writing threads failed since parent thread died.");
				else
					logger.info("completed writing threads, added " + filteredCount
							+ " records to the output: " + file.getAbsolutePath());

			} catch (Exception e) {
				logger.error("Exception caught in Writer thread", e);
				logger.debug(file.getAbsolutePath() + " failed ");
				mainThread.interrupt();
			} finally {
				wLatch.countDown();
				logger.debug("Exit Writing threads, total " + countSleep
						+ " times get null from writing queue.");
			}
		}

		private void addRecordToWriter(SAMRecordFilterWrapper record, SAMFileWriter writer) {
			if (record.getPassesFilter()) {
				writer.addAlignment(record.getRecord());
				if (++filteredCount % 1000000 == 0)
					logger.info("writer thread record count: " + filteredCount);

			} else if (writeUnmatchedOutput) {
				// deal with unmatched records here
				
				outBadQueue.add(record.getRecord());
				
				if (count % 1000000 == 0 ) {
					int queueSize = outBadQueue.size();
					logger.info("unmatched records queue size: " + queueSize);
					while (queueSize > 100000) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
						queueSize = outBadQueue.size();
					}
				}
			}
			count++;
		}
	}

}
