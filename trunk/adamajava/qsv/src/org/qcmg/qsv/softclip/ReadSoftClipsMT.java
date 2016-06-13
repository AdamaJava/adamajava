/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
//package org.qcmg.qsv.softclip;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.util.AbstractQueue;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import htsjdk.samtools.SAMRecord;
//import htsjdk.samtools.SAMRecordIterator;
//
//import org.qcmg.common.log.QLogger;
//import org.qcmg.common.log.QLoggerFactory;
//import org.qcmg.picard.SAMFileReaderFactory;
//import org.qcmg.qsv.Chromosome;
//import org.qcmg.qsv.QSVParameters;
//import org.qcmg.qsv.util.CustomThreadPoolExecutor;
//import org.qcmg.qsv.util.QSVUtil;
//
//
//public class ReadSoftClipsMT implements Runnable {
//	
//	 private final QLogger logger = QLoggerFactory.getLogger(getClass());;
//	    private final int noOfThreads;
//	    private int maxRecords;
//	    private int checkPoint;
//	    private final int sleepUnit = 10;
//	    private QSVParameters parameters;
//	    private final CountDownLatch mainLatch;
//	    private AtomicInteger exitStatus = new AtomicInteger(0);
//		private String softClipDir;
//		private boolean isSplitRead;
//		private boolean runClip;
//		
//	    public ReadSoftClipsMT(CountDownLatch countDownLatch, QSVParameters parameters, String softclipDir, boolean isSplitRead, String analysisMode) {
//	        this.noOfThreads = 5;
//	        this.maxRecords = 100000;
//	        this.checkPoint = 100000;
//	        this.softClipDir = softclipDir;                 
//	        this.parameters = parameters; 
//	        this.mainLatch = countDownLatch;
//	        this.isSplitRead = isSplitRead;
//	        this.runClip = true;
//	        
//	        if (analysisMode.equals("pair")) {
//	        	this.runClip = false;
//	        }
//	    }
//
//	    public void setMaxRecord(int maxRecords) {
//	        this.maxRecords = maxRecords;
//	    }
//
//	    public void setCheckPoint(int checkPoint) {
//	        this.checkPoint = checkPoint;
//	    }
//	    
//	    public AtomicInteger getExitStatus() {
//	    	return this.exitStatus;
//	    }
//	    
//		@Override
//		public void run() {
//	       
//	            // create queue to store the records from input
//	            final AbstractQueue<List<Chromosome>> readQueue = new ConcurrentLinkedQueue<List<Chromosome>>();    
//	              
//	            final CountDownLatch readLatch = new CountDownLatch(1); // reading
//	                                                                    // thread
//	            final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // cluster thread
//	                        
//	            // set up executor services
//	            ExecutorService readThread = new CustomThreadPoolExecutor(1, exitStatus, logger);
//	            ExecutorService filterThreads =  new CustomThreadPoolExecutor(noOfThreads, exitStatus, logger);
//	    
//	            try {
//     
//	                // kick-off single reading thread
//	                readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, filterLatch));
//	                readThread.shutdown();
//	    
//	                // kick-off cluster thread
//	                for (int i = 0; i < noOfThreads; i++) {
//	                    filterThreads.execute(new Clustering(readQueue,
//	                            Thread.currentThread(), readLatch,
//	                            filterLatch));
//	                }
//	                filterThreads.shutdown();   
//	                
//	                  
//	                logger.info("waiting for  threads to finish (max wait will be 20 hours)");
//	                readThread.awaitTermination(60, TimeUnit.HOURS);
//	                filterThreads.awaitTermination(60, TimeUnit.HOURS);
//	    
//	                if (readQueue.size() != 0) {
//	                    throw new Exception(
//	                            " threads have completed but queue isn't empty  (readQueue):  "
//	                                    + readQueue.size());
//	                }
//	                logger.info("All threads finished");
//	                
//	    
//	            } catch (Exception e) {
//	            	logger.error(QSVUtil.getStrackTrace(e));
//	    	        if (exitStatus.intValue() == 0) {
//	    	        	exitStatus.incrementAndGet();
//	    	        }
//	            } finally {
//	                // kill off any remaining threads
//	                readThread.shutdownNow();
//	                filterThreads.shutdownNow();
//	                mainLatch.countDown();
//	            }        
//	    }		
//
//
//		private class Reading implements Runnable {
//
//	        private final AbstractQueue<List<Chromosome>> queue;
//	        private final Thread mainThread;
//	        private final CountDownLatch rLatch;
//	        private final CountDownLatch fLatch;
//
//	        public Reading(AbstractQueue<List<Chromosome>> q, Thread mainThread,
//	                CountDownLatch readLatch, CountDownLatch filterLatch) {
//	            this.queue = q;
//	            this.mainThread = mainThread;
//	            this.rLatch = readLatch;
//	            this.fLatch = filterLatch;
//	        }
//
//	        @Override
//	        public void run() {            
//	            int countSleep = 0;
//	            long count = 0;
//	            try {
//	            	logger.info("Reading soft clips for file: " + parameters.getClippedBamFile());
//	            	Map<String, List<Chromosome>> chromosomes = parameters.getChromosomes();
//	                            
//	                for (Entry<String, List<Chromosome>> entry: chromosomes.entrySet()) {
//	                	logger.info("Adding chromosome to queue: " + entry.getKey() + " in sample " + parameters.getFindType());
//	                    queue.add(entry.getValue());
//
//	                    if (fLatch.getCount() == 0) {
//	                        throw new Exception(
//	                                "No clipping threads left, but reading from input is not yet completed");
//	                    }
//
//	                    if (++count % checkPoint == 1) {
//	                        while (queue.size() >= maxRecords) {
//	                            try {
//	                                Thread.sleep(sleepUnit);
//	                                countSleep++;
//	                            } catch (Exception e) {
//	                                logger.info(Thread.currentThread().getName()
//	                                        + " " + e.getMessage());
//	                            }
//	                        }
//	                    }
//
//	                    if (count % 50000000 == 0) {
//	                        logger.debug("Read " + count + " records from input: "
//	                                + parameters.getFindType());
//	                    }
//
//	                }
//	            } catch (Exception e) {
//	            	logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QSVUtil.getStrackTrace(e));
//	    	        if (exitStatus.intValue() == 0) {
//	    	        	exitStatus.incrementAndGet();
//	    	        }
//	                mainThread.interrupt();
//	            } finally {
//	                rLatch.countDown();
//	                logger.info(String
//	                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
//	                                + "since input queue are full.fLatch  is %d; queus size is %d ",
//	                                countSleep, sleepUnit, fLatch.getCount(),
//	                                queue.size()));
//	            }
//
//	        }
//	    }
//
//	    private class Clustering implements Runnable {
//
//	        private final AbstractQueue<List<Chromosome>> queueIn;  
//	        private final Thread mainThread;
//	        private final CountDownLatch readLatch;
//	        private final CountDownLatch filterLatch;      
//	        private int countOutputSleep;
//			
//	        public Clustering(AbstractQueue<List<Chromosome>> readQueue,
//	                Thread mainThread,
//	                CountDownLatch readLatch, CountDownLatch fLatch) throws Exception {
//	            this.queueIn = readQueue;
//	            this.mainThread = mainThread;
//	            this.readLatch = readLatch;
//	            this.filterLatch = fLatch;
//	        }
//
//	        @Override
//	        public void run() {
//
//	            int sleepcount = 0;
//	            countOutputSleep = 0;
//	            boolean run = true;
//
//	            try {
//	                
//	            	List<Chromosome> chromosomes = null;
//
//	                while (run) {
//	                	chromosomes = queueIn.poll();
//	                    
//	                    
//	                    if (chromosomes == null) {
//	                        // must check whether reading thread finished first.
//	                        if (readLatch.getCount() == 0) {
//	                            run = false;
//	                        }
//
//	                        // qIn maybe filled again during sleep, so sleep should
//	                        // be secondly
//	                        try {
//	                            Thread.sleep(sleepUnit);
//	                            sleepcount++;
//	                        } catch (InterruptedException e) {
//	                            logger.info(Thread.currentThread().getName() + " "
//	                                    + e.toString());
//	                        }
//
//	                    } else {      
//	                    	
//	                    		for (Chromosome c: chromosomes) {
//		                    	    logger.info("Reading records from chromosome: " + c.getName() + " in sample " + parameters.getClippedBamFile());                 	
//		                    	    int queryStart = c.getStartPos() - 100;	                    	
//		                        	if (queryStart < 0) {
//		                        		queryStart = 1;
//		                        	}	                    	
//		                        	int queryEnd = c.getEndPos() + 100;
//		                	    	SAMRecordIterator reader = SAMFileReaderFactory.createSAMFileReader(parameters.getClippedBamFile(), "silent").queryOverlapping(c.getName(), queryStart, queryEnd);
//		                	    	BufferedWriter writer = new BufferedWriter(new FileWriter(new File(SoftClipStaticMethods.getSoftClipFile(c.getName(), parameters.getFindType(), softClipDir)), true));
//		                	    	while (reader.hasNext()) {
//		                	    		
//		                	    		
//		                	    		SAMRecord record = reader.next();
//		                	    		
//		                	    		if (record.getReadUnmappedFlag() && isSplitRead) {
//		                	    			if (record.getAlignmentStart() >= c.getStartPos() && record.getAlignmentStart() <= c.getEndPos()) {
//		                	    				QSVUtil.writeUnmappedRecord(writer, record, null, null, record.getReferenceName(), parameters.getFindType(), softClipDir, parameters.isTumor());
//		                	    			}
//		                	    		} else {
//		                	    			if (runClip) {
//				                	    		if (record.getUnclippedStart() >= c.getStartPos() && record.getUnclippedStart() <= c.getEndPos() || 
//				                	    				record.getUnclippedEnd() >= c.getStartPos() && record.getUnclippedEnd() <= c.getEndPos())
//				                	    			SoftClipStaticMethods.writeSoftClipRecord(writer, record, null, null, record.getReferenceName(), parameters.getFindType(), softClipDir);
//				                	    		}
//		                	    			}
//		                	    	}
//		                	    	
//	 	                	    	reader.close();
//		                    	    writer.close();                	
//	                    	    
//	                    		}
//	                    } // end else
//	                }// end while
//	                logger.info("Completed clipping thread: "
//	                        + Thread.currentThread().getName());
//	            } catch (Exception e) {
//	            	logger.error("Setting exit status in annotation thread to 1 as exception caught: " + QSVUtil.getStrackTrace(e));
//	    	        if (exitStatus.intValue() == 0) {
//	    	        	exitStatus.incrementAndGet();
//	    	        }
//	                mainThread.interrupt();
//	            } finally {
//	                logger.debug(String
//	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn are %d",
//	                                sleepcount, countOutputSleep, sleepUnit,
//	                                queueIn.size()));
//	                filterLatch.countDown();
//	            }
//	        }
//	    }
//}
