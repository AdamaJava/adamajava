/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.isize;

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.annotate.RunTypeRecord;
import org.qcmg.qsv.util.CustomThreadPoolExecutor;
import org.qcmg.qsv.util.QSVUtil;


public class SAMRecordCounterMT {
	
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private File bamFile;
	private ConcurrentHashMap<String, RunTypeRecord> runRecords;
	private int noOfThreads;
	private AtomicInteger exitStatus;
	private int maxRecords;
	private int checkPoint;
	private final int sleepUnit = 10;

	public SAMRecordCounterMT(File file) throws QSVException, IOException {
		this.bamFile = file;
		this.noOfThreads = 1;
        this.maxRecords = 1000000;
        this.checkPoint = 1000;
        this.exitStatus = new AtomicInteger(0);        
		execute();
	}
	
	private void execute() throws QSVException, IOException {
		
		//getReadGroups first
		getReadGroups();
		
		final AbstractQueue<SAMRecord> readQueue = new ConcurrentLinkedQueue<SAMRecord>();	    
        
        final CountDownLatch readLatch = new CountDownLatch(1); // reading
                                                                // thread
        final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // filtering thread

        // set up executor services
        ExecutorService readThread = new CustomThreadPoolExecutor(1, exitStatus, logger);
        ExecutorService processThreads =  new CustomThreadPoolExecutor(noOfThreads, exitStatus, logger);

        try {

            // kick-off single reading thread
            readThread.execute(new Reader(readQueue, Thread.currentThread(), readLatch, filterLatch));
            readThread.shutdown();

            // kick-off parsing thread
            for (int i = 0; i < noOfThreads; i++) {
                processThreads.execute(new Parser(readQueue, Thread.currentThread(), readLatch, filterLatch));
            }
            processThreads.shutdown();

            readThread.awaitTermination(60, TimeUnit.HOURS);
            processThreads.awaitTermination(60, TimeUnit.HOURS);


            if (readQueue.size() != 0) {
                throw new Exception(
                        " threads have completed but queue isn't empty  (readQueue):  "
                                + readQueue.size());
            }
            logger.info("All threads finished");

        } catch (Exception e) {
            logger.info(e.getMessage());
            exitStatus.set(1);
        } finally {
            // kill off any remaining threads
            readThread.shutdownNow();
            processThreads.shutdownNow();

           for (Entry<String, RunTypeRecord> entry: runRecords.entrySet()) {
        	   entry.getValue().calculateISize();
           }
        }		
	}
	
	public List<RunTypeRecord> getRunRecords() {
		List<RunTypeRecord> r = new ArrayList<RunTypeRecord>(runRecords.values());
		return r;
	}
	
	private void getReadGroups() throws IOException {

			this.runRecords = new ConcurrentHashMap<String, RunTypeRecord>();
			
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
			
			SAMFileHeader header = reader.getFileHeader();
			
			List<SAMReadGroupRecord> readGroups = header.getReadGroups();
			
			for (SAMReadGroupRecord r: readGroups) {
				String zc = r.getAttribute("zc");
				String seqMapped;
				String id = r.getId();
				if (zc != null) {
					seqMapped = zc;
				} else {
					seqMapped = r.getLibrary();
				}
				
				runRecords.put(id, new RunTypeRecord(id, null, null, seqMapped));
			}
			
			reader.close();		
	}

	private class Reader implements Runnable {

        private final AbstractQueue<SAMRecord> queue;
        private final Thread mainThread;
        private final CountDownLatch readLatch;
        private final CountDownLatch parserLatch;

        public Reader(AbstractQueue<SAMRecord> q, Thread mainThread,
                CountDownLatch readLatch, CountDownLatch parserLatch) {
            this.queue = q;
            this.mainThread = mainThread;
            this.readLatch = readLatch;
            this.parserLatch = parserLatch;
        }

        @Override
        public void run() {
            logger.info("Starting to read input: " + bamFile.getAbsolutePath());
            int countSleep = 0;
            int count = 0;
            try {
             	
            	//read records           	 
            	SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
                            
                for (SAMRecord record : reader) {
                	
                	queue.add(record);
                	count++;

                    if (parserLatch.getCount() == 0) {
                        throw new Exception(
                                "No filtering threads left, but reading from input is not yet completed");
                    }

                    if (count % checkPoint == 0) {
                        while (queue.size() >= maxRecords) {
                            try {
                                Thread.sleep(sleepUnit);
                                countSleep++;
                            } catch (Exception e) {
                                logger.info(Thread.currentThread().getName()
                                        + " " + e.getMessage());
                            }
                        }
                    }

                    if (count % 10000000 == 0) {
                        logger.debug("Read " + count + " records from input: "
                                + bamFile);
                    }
                }

                reader.close();
                logger.info("Completed reading thread, read " + count
                        + " records from input: " + bamFile.getAbsolutePath());
            } catch (Exception e) {
            	logger.error("Setting exit status in CountReaderWorker thread to 1 as exception caught in reading method: " + QSVUtil.getStrackTrace(e));
    	        if (exitStatus.intValue() == 0) {
    	        	exitStatus.incrementAndGet();
    	        }
                mainThread.interrupt();
            } finally {
            	readLatch.countDown();
                logger.debug(String
                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
                                + "since input queue are full.fLatch  is %d; queus size is %d ",
                                countSleep, sleepUnit, parserLatch.getCount(),
                                queue.size()));
            }
        }
    }
	
	  private class Parser implements Runnable {

	        private final AbstractQueue<SAMRecord> queueIn;
	        private final Thread mainThread;
	        private final CountDownLatch readLatch;
	        private final CountDownLatch parserLatch;
	        private int countOutputSleep;

	        /**
	         * 
	         * @param qIn
	         *            : store SAM record from input file
	         * @param qOutGood
	         *            : store satisfied record based on query
	         * @param qOutBad
	         *            : store unsatisfied record based on query (null is
	         *            allowed)
	         * @param query
	         *            : query string
	         * @param maxRecords
	         *            : queue size
	         * @param mainThread
	         *            : parent thread
	         * @param rLatch
	         *            : the counter for reading thread
	         * @param fLatch
	         *            : the counter for filtering thread (current type)
	         */
	        public Parser(AbstractQueue<SAMRecord> qIn, Thread mainThread,
	                CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
	            this.queueIn = qIn;
	            this.mainThread = mainThread;
	            this.readLatch = rLatch;
	            this.parserLatch = fLatch;
	        }

	        @Override
	        public void run() {

	            int sleepcount = 0;
	            countOutputSleep = 0;
	            boolean run = true;

	            try {
	                SAMRecord record;

	                while (run) {
	                    record = queueIn.poll();
	                    
	                    
	                    if (record == null) {
	                        // must check whether reading thread finished first.
	                        if (readLatch.getCount() == 0) {
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
	                        // check to see if max records found for all reads	
	                    	if (record.getReadGroup() != null) {
		        				String rdId = record.getReadGroup().getId();				
		        						        					
	        					runRecords.get(rdId).addToMap(record.getInferredInsertSize());		        				
	                    	}

	                    } // end else
	                }// end while
	                logger.info("Completed parsing thread: "
	                        + Thread.currentThread().getName());
	            } catch (Exception e) {
	            	logger.error("Setting exit status in CountReaderWorker thread to 1 as exception caught: " + QSVUtil.getStrackTrace(e));
	    	        if (exitStatus.intValue() == 0) {
	    	        	exitStatus.incrementAndGet();
	    	        }
	                mainThread.interrupt();
	            } finally {
	                logger.debug(String
	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood are %d",
	                                sleepcount, countOutputSleep, sleepUnit,
	                                queueIn.size()));
	                parserLatch.countDown();
	            }
	        }
	  }

	public int getExitStatus() {
		return exitStatus.intValue();
	}
}
