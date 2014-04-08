/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.mode;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.Options;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.PositionDS;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.metrics.SummaryMetric;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.StrandEnum;


public class MetricsMT {
	
	private PileupHDF hdf;	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private int blockSize;
    private final int sleepUnit = 10;
    private int noOfThreads;	
	private AtomicInteger exitStatus = new AtomicInteger();
	private List<String> readRanges;	
	final int maxRecords = 100000;
	final int checkPoint = 10000;
	private String hdfHeader;
	private Options options;
	private SummaryMetric summaryMetric;
	private List<Chromosome> chromosomes;
	
	public MetricsMT(Options options) throws Exception {
		this.options = options;
		this.readRanges = options.getReadRanges();		
		this.hdf = new PileupHDF(options.getHdfFile(), false, false);
		this.noOfThreads = options.getThreadNo();
		this.blockSize = 1000000;			
		this.summaryMetric = options.getSummaryMetric();
		logger.info("Block size: " + blockSize);
	}
	
	public int getExitStatus() {
		return this.exitStatus.intValue();
	}
	

	public int execute() {	
		
		//set up initial metadata characteristics		
		final AbstractQueue<List<Chromosome>> readQueue = new ConcurrentLinkedQueue<List<Chromosome>>();
		 
	    final CountDownLatch readLatch = new CountDownLatch(1); // reading
	                                                            // thread
	    final CountDownLatch metricsLatch = new CountDownLatch(noOfThreads); // filtering thread
	    
	    // set up executor services
	    ExecutorService readThread = Executors.newSingleThreadExecutor();
	    ExecutorService metricsThreads = Executors.newFixedThreadPool(noOfThreads);
	    	    
	    try {
	    	
	    	hdf.open();
	    	
	    	this.hdfHeader = hdf.getHDFHeader();
	    	
	    	int totalPatients = hdf.getBamFilesInHeader().size();
	    	
	    	chromosomes = hdf.getChromosomeLengths(); 
	    	logger.info("***METRICS***");
	    	
	    	summaryMetric.start(hdfHeader,options.getUuid(), options.getQexec().getExecMetaDataToString(), chromosomes, totalPatients);
	    	
	        // kick-off single reading thread
	        readThread.execute(new Setup(readQueue, Thread.currentThread(), readLatch, metricsLatch));
	        readThread.shutdown();
	
	        // kick-off filtering thread
	        for (int i = 0; i < noOfThreads; i++) {
	            metricsThreads.execute(new GetMetrics(readQueue, Thread.currentThread(), readLatch,
	                    metricsLatch));
	        }
	        metricsThreads.shutdown();
	        
	        readThread.awaitTermination(500, TimeUnit.HOURS);
	        metricsThreads.awaitTermination(500, TimeUnit.HOURS);

	        if (readQueue.size() != 0) {
	        	logger.info("Setting exitStatus to 1 in execute thread as readQueue is not empty.");
	        	exitStatus.incrementAndGet();
	            throw new QPileupException("INCOMPLETE_READ_THREAD", "" + readQueue.size());
	        } else if (exitStatus.intValue() > 0) {
	        	throw new QPileupException("EXIT_STATUS_ERROR", "merge", "" + exitStatus.intValue());
	        } else {	        	
	        	logger.info("All threads finished, closing HDF file");		        
	        }

	        summaryMetric.finish();
	        
	        hdf.close();
	        
	        
	    } catch (Exception e) {
	    	logger.error("Setting exit status in execute thread to 1 as exception caught in execute method: " + PileupUtil.getStrackTrace(e));
	    	if (exitStatus.intValue() == 0) {
	    		exitStatus.incrementAndGet();
	    	}
	    } finally {
	        // kill off any remaining threads
	        readThread.shutdownNow();
	        metricsThreads.shutdownNow();	
	    }
	    
		return exitStatus.intValue();
	}

	private class Setup implements Runnable {

	        private final AbstractQueue<List<Chromosome>> queue;
	        private final CountDownLatch rLatch;
	        private final CountDownLatch fLatch;	  
	        private Map<String, List<Chromosome>> queueMap = new TreeMap<String, List<Chromosome>>();
	        
	        public Setup(AbstractQueue<List<Chromosome>> q, Thread currentThread,
	                CountDownLatch readLatch, CountDownLatch filterLatch) {
	            this.queue = q;
	            this.rLatch = readLatch;
	            this.fLatch = filterLatch;
	        }

	        @Override
	        public void run() {
	            int countSleep = 0;

	            try {
	            	
	            	queueMap = PileupUtil.getChromosomeRangeMap(readRanges, chromosomes);	            	
	        			        		
	        		addMapToQueue();
	            } catch (Exception e) {
	            	logger.info("Setting exit status to 1 as exception caught in setup thread: " + PileupUtil.getStrackTrace(e));
	            	if (exitStatus.intValue() == 0) {
	    	    		exitStatus.incrementAndGet();
	    	    	}	                
	                Thread.currentThread().interrupt();
	            } finally {
	                rLatch.countDown();
	                logger.debug(String
	                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
	                                + "since input queue are full.fLatch  is %d; queue size is %d ",
	                                countSleep, sleepUnit, fLatch.getCount(),
	                                queue.size()));
	            }
	        }

			private void addMapToQueue() throws Exception {
				for (Map.Entry<String, List<Chromosome>> entry: queueMap.entrySet()) {
					addToQueue(entry.getKey(), entry.getValue());
				}				
			}

			private void addToQueue(String key, List<Chromosome> list) throws Exception {
				
				logger.info("Read chromosome: " + key);
				
	    		if (fLatch.getCount() == 0) {
                    throw new QPileupException("NO_THREADS", "metrics");
                }
        		
        		if (exitStatus.intValue() > 0) {
        			 throw new QPileupException("EXIT_STATUS_ERROR", "metrics", "" + exitStatus.intValue());
        		}
        		
				queue.add(list);	
				
			}			
	  }
	 
	  private class GetMetrics implements Runnable {

	        private final AbstractQueue<List<Chromosome>> queueIn;
	        final CountDownLatch readLatch;
	        final CountDownLatch metricsLatch;
	        private int countOutputSleep;	
			private List<Chromosome> chromosomes;
			

	        public GetMetrics(AbstractQueue<List<Chromosome>> readQueue, 
	                Thread mainThread, CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
	            this.queueIn = readQueue;
	            this.readLatch = rLatch;
	            this.metricsLatch = fLatch;  
	            this.chromosomes = null;
	        }

	        @Override
	        public void run() {

	            int sleepcount = 0;
	            countOutputSleep = 0;
	            boolean run = true;
	            
	            try {

	                while (run) {
	                    chromosomes = queueIn.poll();	                   
	                    
	                    if (chromosomes == null) {
	                        // must check whether reading thread finished first.
	                        if (readLatch.getCount() == 0) {
	                        	//write final records	                        	
	                            run = false;
	                        }
	                        
	                        if (exitStatus.get() > 0) {
	                        	logger.error("Error in metrics thread: current chromosome to get is null and exit status is showing error");
		            			throw new QPileupException("EXIT_STATUS_ERROR", "metrics", "" + exitStatus.intValue());
		            		}

	                        try {
	                            Thread.sleep(sleepUnit);
	                            sleepcount++;
	                        } catch (InterruptedException e) {
	                            logger.info("Interrupted exception: " + Thread.currentThread().getName() + " "
	                                    + e.toString());
	                        }

	                    } else {
	                    	
	                    	Collections.sort(chromosomes);	               	
	                    	
                    		logger.info("Starting chromosome: " + chromosomes.get(0).getName());	                    	

	        	            for (Chromosome chromosome: chromosomes) {
	        	            	//check map for chromosome key
		                    	summaryMetric.addChromosome(chromosome);
		        	            int totalLength = chromosome.getSectionLength();
		                		
		            			if (blockSize > totalLength) {           
		            				run = readData(chromosome, chromosome.getStartPos(), chromosome.getEndPos());     				
		            			} else {
		            				int startIndex = chromosome.getStartPos();
		            				int finalBlockSize = totalLength - (blockSize * (totalLength/blockSize));
		            				int finalIndex = -1;
		            				if (finalBlockSize < 1) {
		            					finalIndex = totalLength;
		            				} else {
		            					finalIndex = startIndex + totalLength - finalBlockSize;
		            				}
		            				for (int i=startIndex; i<=finalIndex; i+=blockSize) {
		            					if (i == finalIndex) {
		            						run = readData(chromosome, i, i + finalBlockSize-1);			
		            						break;
		            					} else {
		            						run = readData(chromosome, i, i + blockSize-1);	
		            					}				
		            				}            			
		            			}
		            			
		            			summaryMetric.writeFinalRecords(chromosome, hdf);
	        	            }
	        	            
	        	            logger.info("Finishing chromosome: " + chromosomes.get(0).getName());
        	            
	                    } // end else	                    
	                }// end while
	                
	                logger.info("Completed pileup thread: "
	                        + Thread.currentThread().getName());
	                
	            } catch (Exception e) {
	                logger.error("Exit status will be set to one as exception occurred in metrics thread: " + PileupUtil.getStrackTrace(e));
	              
	                if (exitStatus.intValue() == 0) {
	    	    		exitStatus.incrementAndGet();
	    	    	}
	                Thread.currentThread().interrupt();
	            } finally {
	                logger.debug(String
	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn is %d",
	                                sleepcount, countOutputSleep, sleepUnit,
	                                queueIn.size()));
	                metricsLatch.countDown();
	            }
	        }

			private boolean readData(Chromosome chromosome, int startPos, int endPos) throws Exception {
				int end = endPos;
				if (endPos != chromosome.getTotalLength()) {
	        		end += PileupConstants.STEP_SIZE;
	        	}
				
				logger.debug("Starting to read from HDF: " + chromosome.getName() + " Start: " + startPos + " End:" + endPos);
				
	        	int totalToRead = end - startPos + 1;	        	
	        	
        		int startIndex = startPos - 1;
        		
            	PositionDS position = new PositionDS(hdf, chromosome.getHdfGroupName());
        		StrandDS forward = new StrandDS(hdf, chromosome.getName(), false, StrandEnum.getMetricsElements());	
        		StrandDS reverse = new StrandDS(hdf, chromosome.getName(), true, StrandEnum.getMetricsElements());

        		//read        		
    			logger.debug("Reading from: " + chromosome.getName() + " Start: " + startPos + " End: " + endPos);
        		
        		position.readDatasetBlock(startIndex, totalToRead);
        		forward.readDatasetBlock(startIndex, totalToRead);
        		reverse.readDatasetBlock(startIndex, totalToRead);
        		
        		summaryMetric.processRecords(chromosome, position, forward, reverse, startPos, endPos);  
        		
        		position = null;
        		forward = null;
        		reverse = null;
        		
        		logger.debug("Finished examining: " + chromosome.getName() + " Start: " + startPos + " End: " + endPos);        		
        		if (exitStatus.get() > 0) {
                	logger.error("Error in metrics thread: current chromosome to get is null and exit status is showing error");
                	return false;
        		}
                return true;
	        }     
	  }	
}



