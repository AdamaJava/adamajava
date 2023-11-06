/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupUtil;


public class IndelBasePileupMT {
	
	private static QLogger logger = QLoggerFactory.getLogger(IndelBasePileupMT.class);
	private final Options options;
	AtomicInteger totalExamined = new AtomicInteger();
	AtomicInteger positionCount = new AtomicInteger();
	AtomicInteger uniquePositionCount = new AtomicInteger();
	int threadNo = 0;
	AtomicInteger exitStatus = new AtomicInteger();
	final int sleepUnit = 20;
	final int maxRecords = 100000;
	final int checkPoint = 10000;
	private final File inputFile;
	private final File outputFile;
	private final boolean isGermline;
	private final File pileupFile;
	private int[] dccColumns = new int[3];
	
	public IndelBasePileupMT(File inputFile, File outputFile, File pileupFile, boolean isGermline, Options options) throws Exception {		
		this.options = options;	
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.pileupFile = pileupFile;
		this.isGermline = isGermline;
		threadNo = options.getThreadNo();
		execute();
	}
	public int getExitStatus() {
		return this.exitStatus.intValue();
	}
	
	private void execute() {
		
		if (isGermline) {
			logger.info("**********PROCESSING GERMLINE FILE**********");

		} else {
			logger.info("**********PROCESSING SOMATIC FILE**********");
		}
		
		final AbstractQueue<IndelPosition> readQueue = new ConcurrentLinkedQueue<>();
            
        final AbstractQueue<String[]> writeQueue = new ConcurrentLinkedQueue<>();
    
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
        	
        	List<String> headers = QBasePileupUtil.getHeaderLines(inputFile, true);
        	this.dccColumns = QBasePileupUtil.parseDCCHeader(headers);
        	
        	
            readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, pileupLatch, 
            		inputFile));
            readThread.shutdown();
    
            // kick-off pileup threads
            for (int i = 0; i < threadNo; i++) {
                pileupThreads.execute(new Pileup(readQueue,
                        writeQueue, Thread.currentThread(), readLatch,
                        pileupLatch, writeLatch));
            }
        
        	pileupThreads.shutdown();

            // kick-off single writing thread to output the satisfied Records
            writeThread.execute(new Writing(writeQueue, outputFile, Thread.currentThread(), pileupLatch, writeLatch, headers));
            writeThread.shutdown();

            logger.info("waiting for  threads to finish (max wait will be 100 hours)");
            readThread.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
            pileupThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
            writeThread.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);

            if (readQueue.size() != 0 || writeQueue.size() != 0) {
            	exitStatus.incrementAndGet();
                throw new Exception(
                        " threads have completed but queue isn't empty  (inputQueue, writeQueue ):  "
                                + readQueue.size() + ", " + writeQueue.size());
            }
            logger.info("All threads finished");
    
        } catch (Exception e) {
            logger.info(QBasePileupUtil.getStackTrace(e));
            exitStatus.incrementAndGet();
        } finally {
            // kill off any remaining threads
            readThread.shutdownNow();
            writeThread.shutdownNow();
            pileupThreads.shutdownNow();
        }
    } 

	private class Reading implements Runnable {

        private final AbstractQueue<IndelPosition> queue;
        private final Thread mainThread;
        private final CountDownLatch readLatch;
        private final CountDownLatch pileupLatch;		
		private final File inputFile;
		private final ArrayList<IndelPosition> positions;		

        public Reading(AbstractQueue<IndelPosition> q, Thread mainThread,
                CountDownLatch readLatch, CountDownLatch filterLatch, File inputFile) {
            this.queue = q;
            this.mainThread = mainThread;
            this.readLatch = readLatch;
            this.pileupLatch = filterLatch;
            this.inputFile = inputFile;
            this.positions = new ArrayList<>();
        }

        @Override
        public void run() {
            logger.info("Starting to read file: " + inputFile.getAbsolutePath());
            int countSleep = 0;
            long count = 0;
            try {      	
            	
        		BufferedReader reader = new BufferedReader(new FileReader(inputFile));        		
        		
        		String indelFileType = null;
        		if (options.hasPindelOption()) {
        			indelFileType = "pindel";
        		}
        		if (options.hasStrelkaOption()) {
        			indelFileType = "strelka";
        		}
        		if (options.hasGATKOption()) {
        			indelFileType = "gatk";
        		}
        		
        		String line;
        		while ((line=reader.readLine()) != null) {
        			if (!line.startsWith("#") && !line.startsWith("analysis_id") && !line.startsWith("Hugo") &&  !line.startsWith("mutation")) {
        				count++;
        				positionCount.incrementAndGet();
        				
        				IndelPosition p = null;
        				
        				p = new IndelPosition(line, isGermline, dccColumns);
        				
        				if (!positions.contains(p)) {
        					uniquePositionCount.incrementAndGet();        					
        				}     
        				
        				positions.add(p);
    					queue.add(p);
        			}
        			if (pileupLatch.getCount() == 0) {
        				reader.close();
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
                                        + " " + QBasePileupUtil.getStackTrace(e));
                            }
                        }
                    }
        		}
        		
        		reader.close();                            
                
                logger.info("Completed reading thread, read " + count
                        + " records from input: " + inputFile.getAbsolutePath());
            } catch (Exception e) {
            	logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QBasePileupUtil.getStackTrace(e));
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

        private final AbstractQueue<IndelPosition> queueIn;
        private final AbstractQueue<String[]> queueOut;
        private final Thread mainThread;
        private final CountDownLatch readLatch;
        private final CountDownLatch pileupLatch;
        private final CountDownLatch writeLatch;
	   private final InputBAM tumourBam;
		private final InputBAM normalBam;
		QueryExecutor exec = null;
		
        public Pileup(AbstractQueue<IndelPosition> queueIn,
                AbstractQueue<String[]> writeQueue, Thread mainThread,
                CountDownLatch readLatch, CountDownLatch pileupLatch,
                CountDownLatch wGoodLatch) {
            this.queueIn = queueIn;
            this.queueOut = writeQueue;
            this.mainThread = mainThread;
            this.readLatch = readLatch;
            this.pileupLatch = pileupLatch;
            this.writeLatch = wGoodLatch;
            this.tumourBam = options.getTumourBam();
            this.normalBam = options.getNormalBam();	            
        }

	        @Override
	        public void run() {

	            int sleepcount = 0;
	            int count = 0;
				int countOutputSleep = 0;
	            boolean run = true;

	            try {
	            	
	                logger.info("Thread is starting indel pileups...");
		            	IndelPosition position;
		            	if (options.getFilterQuery() != null) {
		        			this.exec  = new QueryExecutor(options.getFilterQuery());
		        		}
	            	
		            	IndexedFastaSequenceFile indexedFasta = QBasePileupUtil.getIndexedFastaFile(options.getReference());
	            	
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
	                            logger.info(Thread.currentThread().getName() + " " + e);
	                        }

	                    } else {	                        
	                        count++;  
	                        
	                        int total = totalExamined.incrementAndGet();
	                        if  (total % 10000 == 0) {
	                        	logger.info("Total records processed " + total + " so far...");
	                        }
	                        
	                        String [] outArray = new String[2];	
	                        
	                        IndelPositionPileup pileup = new IndelPositionPileup(tumourBam, normalBam, position, options, indexedFasta);	                        
	                        pileup.pileupReads(exec);
	                        outArray[0] = pileup.toDCCString();                      
	                        
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
	                                            + QBasePileupUtil.getStackTrace(e) + " (queue size full) ");
	                                }
	                                if (writeLatch.getCount() == 0) {
	                                    logger.error("output queue is not empty but writing thread is complete");
	                                    run = false;
	                                }
	                            } 
	                        }
	                        queueOut.add(outArray);        	                                                     
                        }                       
	                   
	                }
	               
	                indexedFasta.close();
	                logger.info("Completed pileup thread: "
	                        + Thread.currentThread().getName());
	            } catch (Exception e) {
	            	logger.error("Setting exit status in pileup thread to 1 as exception caught file: " + options.getReference() + " " + QBasePileupUtil.getStackTrace(e));
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
	        private final AbstractQueue<String[]> queue;
	        private final Thread mainThread;
	        private final CountDownLatch filterLatch;
	        private final CountDownLatch writeLatch;
			private final List<String> headers;

	        public Writing(AbstractQueue<String[]> q, File f, Thread mainThread,
						   CountDownLatch fLatch, CountDownLatch wLatch, List<String> headers) {
	            queue = q;
	            resultsFile = f;
	            this.mainThread = mainThread;
	            this.filterLatch = fLatch;
	            this.writeLatch = wLatch;
	            this.headers = headers;
	        }

	        @Override
	        public void run() {
	            int countSleep = 0;
	            boolean run = true;
	            
	           
	            try {
	                String[] record;
	                int count = 0;
	                BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
	               
	                while (run) {
	                    
	                    if ((record = queue.poll()) == null) {
	                        
	                        try {
	                            Thread.sleep(sleepUnit);
	                            countSleep++;
	                        } catch (Exception e) {
	                        	if (exitStatus.intValue() == 0) {
	        	    	        	exitStatus.incrementAndGet();
	        	    	        }
	                            logger.info(Thread.currentThread().getName() + " "
	                                    + QBasePileupUtil.getStackTrace(e));
	                        }
	                        
	                        if (filterLatch.getCount() == 0 && queue.size() == 0) {
		                        run = false;
		                    }

	                        if ((count % checkPoint == 0) && (!mainThread.isAlive())) {
	                        	writer.close();
	                        	//pileupFileWriter.close();
	                            throw new Exception("Writing threads failed since parent thread died.");
	                        }

	                    } else {
	                        writer.write(record[0]);
	                        count++;
	                    }
	                }

	                writer.close();
	                
	                //rewrite in order
	                reorderFile(resultsFile, String.join("\n", headers));
	                
	                
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
		            	logger.error("Setting exit status to 1 as exception caught in writing thread: " + QBasePileupUtil.getStackTrace(e));
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

			private void reorderFile(File file, String header) throws IOException {
				Map<ChrRangePosition, String> map = new TreeMap<>();
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					String line = reader.readLine();
					while(line != null) {
						String[] values = line.split("\t");
						map.put(new ChrRangePosition(values[4], Integer.parseInt(values[5]), Integer.parseInt(values[6])), line);
						line = reader.readLine();
					}
				}
				printMap(map, file, header);
			}
			
			private void printMap(Map<ChrRangePosition, String> map, File file, String header) throws IOException {
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
					writer.write(header);
					writer.newLine();
					for (Entry<ChrRangePosition, String> entry: map.entrySet()) {
						writer.write(entry.getValue() + "\n");
					}
				}
			}
	    }
}
