/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.coverage;

import org.qcmg.qbasepileup.Options;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;




import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class CoveragePileupMT {
	
	private static QLogger logger = QLoggerFactory.getLogger(CoveragePileupMT.class);
	private final static int sleepUnit = 10;
	private final static int maxRecords = 100000;
	private final static int checkPoint = 10000;
	
	private final Options options;
	private final AtomicInteger totalExamined = new AtomicInteger();
	private final AtomicInteger totalPassedFilters =new AtomicInteger();
	private final AtomicInteger totalReadsNotMapped = new AtomicInteger();
	private final AtomicInteger totalReadsBadBaseQual = new AtomicInteger();
	private final AtomicInteger totalReadsBaseMapQual = new AtomicInteger();
	private final AtomicInteger positionCount = new AtomicInteger();
	private final AtomicInteger uniquePositionCount = new AtomicInteger();
	private int threadNo = 0;
	private final AtomicInteger exitStatus = new AtomicInteger();

	 
	
	public CoveragePileupMT(Options options) throws Exception {		
		this.options = options;	
		threadNo = options.getThreadNo();
		execute();
	}
	public int getExitStatus() {
		if (this.exitStatus.intValue() > 0) {
			return 1;
		}
		return 0;
	}
	
	private void execute() throws Exception {		
		
		final AbstractQueue<ChrRangePosition> readQueue = new ConcurrentLinkedQueue<>();    
            
        final AbstractQueue<String> writeQueue = new ConcurrentLinkedQueue<String>();
    
        final CountDownLatch readLatch = new CountDownLatch(1); // reading
                                                                    // thread
        final CountDownLatch pileupLatch = new CountDownLatch(threadNo); // filtering thread
        final CountDownLatch writeLatch = new CountDownLatch(1); // writing thread for satisfied records
    
        // set up executor services
        ExecutorService readThread = Executors.newSingleThreadExecutor();
        ExecutorService pileupThreads = Executors
                .newFixedThreadPool(threadNo);
        ExecutorService writeThread = Executors.newSingleThreadExecutor();

        try {
    
            // kick-off single reading thread
            readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, pileupLatch, 
            		options.getPositionsFile(), options.getFormat()));
            readThread.shutdown();
    
            // kick-off pileup threads
            for (int i = 0; i < threadNo; i++) {
                pileupThreads.execute(new Pileup(readQueue,
                        writeQueue, Thread.currentThread(), readLatch,
                        pileupLatch, writeLatch, options.getInputBAMs()));
            }
        
        	pileupThreads.shutdown();

            // kick-off single writing thread to output the satisfied Records
            writeThread.execute(new Writing(writeQueue, options.getOutput(), Thread.currentThread(), pileupLatch, writeLatch));
            writeThread.shutdown();

            logger.info("waiting for  threads to finish (max wait will be 60 hours)");
            readThread.awaitTermination(60, TimeUnit.HOURS);
            pileupThreads.awaitTermination(60, TimeUnit.HOURS);
            writeThread.awaitTermination(60, TimeUnit.HOURS);

            if (readQueue.size() != 0 || writeQueue.size() != 0) {
            	exitStatus.incrementAndGet();
                throw new Exception(
                        " threads have completed but queue isn't empty  (inputQueue, writeQueue ):  "
                                + readQueue.size() + ", " + writeQueue.size());
            }
            logger.info("All threads finished");
    
        } catch (Exception e) {
            logger.info(QBasePileupUtil.getStrackTrace(e));
            exitStatus.incrementAndGet();
        } finally {
            // kill off any remaining threads
            readThread.shutdownNow();
            writeThread.shutdownNow();
            pileupThreads.shutdownNow();
        }
        
	    	logger.debug("TOTAL POSITIONS: \t\t\t" + positionCount);
	    	logger.debug("UNIQUE POSITIONS: \t\t\t" + uniquePositionCount);
		logger.debug("TOTAL READS EXAMINED:\t\t"+totalExamined+"");
		logger.debug("---------------------------------------------");
		logger.debug("TOTAL READS KEPT:\t\t"+totalPassedFilters);
		logger.debug("TOTAL READS NOT ON SNP:\t\t"+totalReadsNotMapped);
		logger.debug("READS WITH BAD BASE QUALITY:\t"+totalReadsBadBaseQual);
		logger.debug("READS WITH BAD MAPPING QUALITY:\t"+totalReadsBaseMapQual);
    } 

	private class Reading implements Runnable {

        private final AbstractQueue<ChrRangePosition> queue;
        private final Thread mainThread;
        private final CountDownLatch readLatch;
        private final CountDownLatch pileupLatch;
		private final File positionsFile;
		private final String format;
		private final ArrayList<ChrRangePosition> positions;		

        public Reading(AbstractQueue<ChrRangePosition> q, Thread mainThread,
                CountDownLatch readLatch, CountDownLatch filterLatch, File positionsFile, String format) {
            this.queue = q;
            this.mainThread = mainThread;
            this.readLatch = readLatch;
            this.pileupLatch = filterLatch;
            this.positionsFile = positionsFile;
            this.format = format;
            this.positions = new ArrayList<ChrRangePosition>();
        }

        @Override
        public void run() {
            logger.info("Starting to read positions file: " + positionsFile.getAbsolutePath());
            int countSleep = 0;
            long count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(positionsFile))) {
        		
        		String line;
        		while ((line=reader.readLine()) != null) {
        			if ( ! line.startsWith("#") && !line.startsWith("analysis_id") && !line.startsWith("Hugo") &&  !line.startsWith("mutation")) {
        				count++;
        				positionCount.incrementAndGet();
        				ChrRangePosition p = null;
        				String[] values = line.split("\t");			     				
        				
        				if (format.equals("dcc1")) {
        					p = new ChrRangePosition(values[4], Integer.parseInt(values[5]),Integer.parseInt(values[6]));
	        		    	} else if (format.equals("dccq")) {
	        		    		p = new ChrRangePosition(values[2],Integer.parseInt(values[3]),Integer.parseInt(values[4]));
	        		    	} else if (format.equals("vcf")) {
	        		    		p = new ChrRangePosition(values[0],Integer.parseInt(values[1]),Integer.parseInt(values[1]));
	        		    	} else if (format.equals("maf")){
	        		    		p = new ChrRangePosition(values[4],Integer.parseInt(values[5]),Integer.parseInt(values[6]));
	        		    	} else if (format.equals("tab")) {
	        		    		p = new ChrRangePosition(values[1],Integer.parseInt(values[2]),Integer.parseInt(values[3]));
	        		    	} else if (format.equals("gff3") || format.equals("gtf")) {
	        		    		p = new ChrRangePosition(values[0],Integer.parseInt(values[3]),Integer.parseInt(values[4]));
	        		    	}         				      				
        				
        				if (!positions.contains(p)) {
        					uniquePositionCount.incrementAndGet();        					
        				}     
        				
        				positions.add(p);
    					queue.add(p);
        			}
        			if (pileupLatch.getCount() == 0) {
        				if (exitStatus.intValue() == 0) {
		    	    	        	exitStatus.incrementAndGet();
		    	    	     }
                     throw new Exception("No pileup threads left, but reading from input is not yet completed");
                 }

                    if (count % checkPoint == 1) {
                        while (queue.size() >= maxRecords) {
                            try {
                                Thread.sleep(sleepUnit);
                                countSleep++;
                            } catch (Exception e) {
                                logger.info(Thread.currentThread().getName()
                                        + " " + QBasePileupUtil.getStrackTrace(e));
                            }
                        }
                    }
        		}
        		
                
                logger.info("Completed reading thread, read " + count
                        + " records from input: " + positionsFile.getAbsolutePath());
            } catch (Exception e) {
            	logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QBasePileupUtil.getStrackTrace(e));
    	        if (exitStatus.intValue() == 0) {
    	        	exitStatus.incrementAndGet();
    	        }
                mainThread.interrupt();
            } finally {
                readLatch.countDown();
                logger.debug(String
                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
                                + "since input queue are full.fLatch  is %d; queus size is %d ",
                                countSleep, sleepUnit, pileupLatch.getCount(),
                                queue.size()));
            }

        }
    }
	
   private class Pileup implements Runnable {

	        private final AbstractQueue<ChrRangePosition> queueIn;
	        private final AbstractQueue<String> queueOut;
	        private final Thread mainThread;
	        private final CountDownLatch readLatch;
	        private final CountDownLatch pileupLatch;
	        private final CountDownLatch writeLatch;
	        private int countOutputSleep;
			private final List<InputBAM> currentInputs;
			private String file = null;
			private QueryExecutor exec = null;

	        public Pileup(AbstractQueue<ChrRangePosition> queueIn,
	                AbstractQueue<String> queueOut, Thread mainThread,
	                CountDownLatch readLatch, CountDownLatch pileupLatch,
	                CountDownLatch wGoodLatch, List<InputBAM> inputs) throws Exception {
	            this.queueIn = queueIn;
	            this.queueOut = queueOut;
	            this.mainThread = mainThread;
	            this.readLatch = readLatch;
	            this.pileupLatch = pileupLatch;
	            this.writeLatch = wGoodLatch;
	            this.currentInputs = new ArrayList<>();
	            for (InputBAM i : inputs) {
	            		currentInputs.add(i);
	            }
	        }

	        @Override
	        public void run() {

	            int sleepcount = 0;
	            int count = 0;
	            countOutputSleep = 0;
	            boolean run = true;

	            try {
	               
	            		ChrRangePosition position;
	                if (options.getFilterQuery() != null) {
		        			this.exec  = new QueryExecutor(options.getFilterQuery());
		        		}
	                while (run) {
	                    position = queueIn.poll();	                    
	                    
	                    if (position == null) {
	                        // must check whether reading thread finished first.
	                        if (readLatch.getCount() == 0) {
	                            run = false;
	                        }
	                        try {
	                            Thread.sleep(sleepUnit);
	                            sleepcount++;
	                        } catch (InterruptedException e) {
	                            logger.info(Thread.currentThread().getName() + " "
	                                    + e.toString());
	                        }

	                    } else {	                        
	                        count++;  
	                        
	                        if (count % 1000 == 0) {
	                        	logger.info("Processed " + count + " records");
	                        }
	                        StringBuilder sb = new StringBuilder();
	                        for (InputBAM i : currentInputs) {
	                        	file = i.getBamFile().getAbsolutePath();	                        	
	                        	
                        		RangePositionPileup pileup = new RangePositionPileup(i, position, options, exec);
	                        	pileup.pileup();
	                        	
		            			sb.append(pileup.toString());
	                        }      	            			
	                        
	                        if (count % checkPoint == 0) {
	                           
	                            if (!mainThread.isAlive()) {
	                                logger.error("mainThread died: " + mainThread.getName());
	                                run = false;
	                            }
	                            // check queue size
	                            while (queueOut.size() >= maxRecords) {
	                                try {
	                                    Thread.sleep(sleepUnit);
	                                    countOutputSleep++;
	                                } catch (InterruptedException e) {
	                                    logger.debug(Thread.currentThread().getName() + " "
	                                            + QBasePileupUtil.getStrackTrace(e) + " (queue size full) ");
	                                }
	                                if (writeLatch.getCount() == 0) {
	                                    logger.error("output queue is not empty but writing thread is complete");
	                                    run = false;
	                                }
	                            } 
	                        }
	                        queueOut.add(sb.toString());        	                                                     
                        }                       
	                   
	                }
	               
	                logger.info("Completed pileup thread: "
	                        + Thread.currentThread().getName());
	            } catch (Exception e) {
		            	logger.error("Setting exit status in pileup thread to 1 as exception caught file: " + file + " " + QBasePileupUtil.getStrackTrace(e));
		    	        if (exitStatus.intValue() == 0) {
		    	        		exitStatus.incrementAndGet();
		    	        }
	                mainThread.interrupt();
	            } finally {
	                logger.debug(String
	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d, %d",
	                                sleepcount, countOutputSleep, sleepUnit,
	                                queueIn.size(), queueOut.size()));
	                pileupLatch.countDown();
	            }
	        }
     }
	
	 private class Writing implements Runnable {
	        private final File resultsFile;
	        private final AbstractQueue<String> queue;
	        private final Thread mainThread;
	        private final CountDownLatch filterLatch;
	        private final CountDownLatch writeLatch;

	        public Writing(AbstractQueue<String> q, File f, Thread mainThread,
	                CountDownLatch fLatch, CountDownLatch wLatch) {
	            queue = q;
	            resultsFile = f;
	            this.mainThread = mainThread;
	            this.filterLatch = fLatch;
	            this.writeLatch = wLatch;
	        }

	        @Override
	        public void run() {
	            int countSleep = 0;
	            boolean run = true;
	            String record;
	            int count = 0;
	            try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile))) {
	               
	                writer.write(getHeader());
	                while (run) {
	                    
	                    if ((record = queue.poll()) == null) {
	                        if (filterLatch.getCount() == 0)
	                            run = false;
	                        try {
	                            Thread.sleep(sleepUnit);
	                            countSleep++;
	                        } catch (Exception e) {
	                        		if (exitStatus.intValue() == 0) {
			        	    	        		exitStatus.incrementAndGet();
			        	    	        }
	                            logger.info(Thread.currentThread().getName() + " "
	                                    + QBasePileupUtil.getStrackTrace(e));
	                        }

	                        if ((count % checkPoint == 0) && (!mainThread.isAlive())) {
	                            throw new Exception("Writing threads failed since parent thread died.");
	                        }

	                    } else {
	                        writer.write(record);
	                        count++;
	                    }
	                }

	                if (!mainThread.isAlive()) {
		                	if (exitStatus.intValue() == 0) {
			    	        	exitStatus.incrementAndGet();
			    	        }
	                    throw new Exception("Writing threads failed since parent thread died.");
	                } else {
	                    logger.info("Completed writing threads, added " + count
	                            + " records to the output: "
	                            + resultsFile.getAbsolutePath());
	                }
	            } catch (Exception e) {
		            	logger.error("Setting exit status to 1 as exception caught in writing thread: " + QBasePileupUtil.getStrackTrace(e));
		    	        if (exitStatus.intValue() == 0) {
		    	        	exitStatus.incrementAndGet();
		    	        }
	                mainThread.interrupt();
	            } finally {
	                writeLatch.countDown();
	                logger.debug("Exit Writing threads, total " + countSleep
	                        + " times get null from writing queue.");
	            }
	        }

			private String getHeader() {
				return "##qbasepileup version 1.0\nChromosome\tPosition\tTotal\tBam_file\n";
			}
	    }


}
