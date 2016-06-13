/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.mode;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ncsa.hdf.hdf5lib.H5;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.Options;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.hdf.MetadataRecordDS;
import org.qcmg.pileup.hdf.MetadataReferenceDS;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.PositionDS;
import org.qcmg.pileup.hdf.StrandDS;

public class BootstrapMT {
	
	private File referenceFile;
	private PileupHDF hdf;	
	private QLogger logger = QLoggerFactory.getLogger(getClass());	
	private final int sleepUnit = 10;
	private long startTime;
	private MetadataRecordDS metaDS;
	private MetadataReferenceDS referenceDS;
	private boolean mergeMode = false;
	private AtomicInteger exitStatus = new AtomicInteger();
	private int chunk = 10000;
	private int noOfThreads = 2;
	private List<String> referenceList = new ArrayList<String>();
	
	public BootstrapMT(Options options, long startTime) throws Exception {
		this.referenceFile = new File(options.getReferenceFile());
		if (!this.referenceFile.exists()) {
			throw new QPileupException("REFERENCE_FILE_ERROR");
		}	
		this.startTime = startTime;		
		this.hdf = new PileupHDF(options.getHdfFile(), true, false);
		
		this.metaDS = new MetadataRecordDS(hdf, options.getLowReadCount(), options.getPercentNonRef(), new Integer(0));
		this.referenceDS = new MetadataReferenceDS(hdf, options.getReferenceFile());		
		
		logger.info("Reference file: "  + options.getReferenceFile());
		logger.info("Low read count option: "  + options.getLowReadCount());
		logger.info("Nonreference Percent: "  + options.getPercentNonRef());
		logger.info("Chunk size: "  + chunk);
		
	}
	
	public BootstrapMT(PileupHDF hdf, Options options, long startTime) throws Exception {
		this.referenceFile = new File(options.getReferenceFile());

		if (!this.referenceFile.exists()) {
			throw new QPileupException("REFERENCE_FILE_ERROR");
		}	
		this.startTime = startTime;	
		
		if (options.getMode().equals("merge")) {
			mergeMode  = true;
			this.hdf = new PileupHDF(options.getHdfFile(), false, false);
		} else {			
			throw new Exception("unrecognized mode");
		}
		this.metaDS = new MetadataRecordDS(hdf, options.getLowReadCount(), options.getPercentNonRef(), new Integer(0));
		this.referenceDS = new MetadataReferenceDS(hdf, options.getReferenceFile());
		logger.info("Chunk size is " + chunk + " No of threads: " + noOfThreads + " Mode: " + options.getMode());
	}
	
	public int execute()  {
		try {
			
			//Add file path to referenceDS
			hdf.open();
			if (mergeMode) {
				executeMT();				
			} else {	
				
				//metadata
				metaDS.addDatasetMember(0, metaDS.getMemberString(PileupUtil.getCurrentDate(), PileupConstants.MODE_BOOTSTRAP, "", "", "00:00:00", referenceFile.getAbsolutePath())); 
				metaDS.create();
				
				//bootstrap
				executeMT();				
				
				//write metadata
				String time = PileupUtil.getRunTime(startTime, System.currentTimeMillis());
				metaDS.updateFirstMember(0, PileupConstants.MODE_MERGE, time);
				
				logger.info("Size of the HDF file is: " + hdf.getFileSize());
				referenceDS.addRecords(referenceList);
	    		referenceDS.create();				
			}   
    		
			hdf.close();
		} catch (Exception e) {
			logger.error("Error occurred during bootstrapping");
			logger.error(PileupUtil.getStrackTrace(e));
			exitStatus.incrementAndGet();
		}
		logger.info("Number of H5 objects left open: " + H5.getOpenIDCount());
		return exitStatus.get();
	}	

	public List<String> getReferenceRecords() {
		return this.referenceList;
	}
	
	private int executeMT() {
				
		//set up initial metadata characteristics		
		final AbstractQueue<ReferenceSequence> readQueue = new ConcurrentLinkedQueue<ReferenceSequence>();
	
	    final CountDownLatch readLatch = new CountDownLatch(1); // reading
	                                                            // thread
	    final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // filtering thread
	
	    // set up executor services
	    ExecutorService readThread = Executors.newSingleThreadExecutor();
	    ExecutorService viewThreads = Executors
	            .newFixedThreadPool(noOfThreads);
	
	    try {
				
	        // kick-off single reading thread
	        readThread.execute(new Setup(readQueue, Thread.currentThread(), readLatch, filterLatch));
	        readThread.shutdown();
	
	        // kick-off filtering thread
	        for (int i = 0; i < noOfThreads; i++) {
	            viewThreads.execute(new Bootstrap(readQueue,Thread.currentThread(), readLatch,
	                    filterLatch));
	        }
	        viewThreads.shutdown();

	        readThread.awaitTermination(20, TimeUnit.HOURS);
	        viewThreads.awaitTermination(20, TimeUnit.HOURS);	         
	        
	        if (readQueue.size() != 0) {
	        	logger.info("Setting exitStatus to 1 in execute thread as readQueue is not empty.");
	        	exitStatus.incrementAndGet();
	            throw new QPileupException("INCOMPLETE_READ_THREAD", "" + readQueue.size());
	        } else if (exitStatus.intValue() > 0) {
	        	throw new QPileupException("EXIT_STATUS_ERROR", "bootsrap", "" + exitStatus.intValue());
	        } else {
	        	logger.info("All threads finished, closing HDF file");
	        } 
	        
	    } catch (Exception e) {
	    	logger.error("Setting exit status in execute thread to 1 as exception caught in execute method: " + PileupUtil.getStrackTrace(e));
	        exitStatus.incrementAndGet();
	    } finally {
	        // kill off any remaining threads
	        readThread.shutdownNow();
	        viewThreads.shutdownNow();	
	    }
	    
		return exitStatus.intValue();
	}

	private class Setup implements Runnable {

        private final AbstractQueue<ReferenceSequence> queue;
        private final CountDownLatch setupLatch;
        private final CountDownLatch fLatch;
        private int countSleep = 0;

        public Setup(AbstractQueue<ReferenceSequence> q, Thread currentThread,
                CountDownLatch readLatch, CountDownLatch filterLatch) {
            this.queue = q;
            this.setupLatch = readLatch;
            this.fLatch = filterLatch;
        }

        @Override
        public void run() {
        	
        	try {        		
        		//Read from reference file and add to queue
        		File indexFile = new File(referenceFile.getPath() + ".fai");		
        		
        		if (!indexFile.exists()) {
        			throw new QPileupException("FASTA_INDEX_ERROR");
        		}
        		FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
        		IndexedFastaSequenceFile f = new IndexedFastaSequenceFile(referenceFile, index);	

        		ReferenceSequence currentSequence = f.nextSequence();
        		
        		while (currentSequence != null) {        			
        			logger.info("Adding " + currentSequence.getName() + " to queue");
        			referenceList.add(referenceDS.getMemberString(currentSequence.getName(), currentSequence.length()));
        			addToQueue(currentSequence);		
        			currentSequence = f.nextSequence();        			
        		}
        		f.close();
        	} catch (Exception e) {
        		logger.info("Setting exit status to 1 as exception caught in setup thread: " + PileupUtil.getStrackTrace(e));
                exitStatus.incrementAndGet();	                
                Thread.currentThread().interrupt();
        	} finally {
                setupLatch.countDown();
                
                logger.info(String
                        .format("Exit Setup thread, total slept %d times * %d milli-seconds, "
                                + "since input queue are full.fLatch  is %d; queus size is %d ",
                                countSleep, sleepUnit, fLatch.getCount(),
                                queue.size()));
            }        	
        }

		private void addToQueue(ReferenceSequence currentSequence) throws QPileupException {
			int maxRecords = 10;
			
			if (fLatch.getCount() == 0) {
                throw new QPileupException("NO_THREADS", "bootstrap");
            }
    		
    		if (exitStatus.intValue() > 0) {
    			 throw new QPileupException("EXIT_STATUS_ERROR", "bootstrap", "" + exitStatus.intValue());
    		}
    		
			while (queue.size() >= maxRecords ) {
                 try {
                     Thread.sleep(sleepUnit);
                     countSleep++;
                 } catch (Exception e) {
                     logger.info(Thread.currentThread().getName()
                             + " " + e.getMessage());
                 }
             }
			
    		queue.add(currentSequence);			
		}
	}
	
	 private class Bootstrap implements Runnable {

	        private final AbstractQueue<ReferenceSequence> queueIn;
	        final CountDownLatch readLatch;
	        final CountDownLatch bootstrapLatch;
	        private int countOutputSleep;	
			private ReferenceSequence currentSequence;
			
			  public Bootstrap(AbstractQueue<ReferenceSequence> readQueue,
		                Thread mainThread,
		                CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
		            this.queueIn = readQueue;
		            this.readLatch = rLatch;
		            this.bootstrapLatch = fLatch;
		        }

		        @Override
		        public void run() {

		            int sleepcount = 0;
		            countOutputSleep = 0;
		            boolean run = true;
		            String chromosome = "";
		            try {
		            while (run) {
		            	
		            	currentSequence = queueIn.poll();		            	
		            	  
	                    if (currentSequence == null) {
	                    	if (readLatch.getCount() == 0) {
	                        	//write final records	                        	
	                            run = false;
	                        }
	                        
	                        if (exitStatus.get() > 0) {
	                        	logger.error("Error in view thread: current reference sequence is null and exit status is showing error");
		            			throw new QPileupException("EXIT_STATUS_ERROR", "bootstrap", "" + exitStatus.intValue());
		            		}

	                        try {
	                            Thread.sleep(sleepUnit);
	                            sleepcount++;
	                        } catch (InterruptedException e) {
	                            logger.info("Interrupted exception: " + Thread.currentThread().getName() + " "
	                                    + e.toString());
	                        }
	                    } else {
	                    	if (exitStatus.get() > 0) {
	                        	logger.error("Error in view thread: current reference sequence is not null and but exit status is showing error");
		            			throw new QPileupException("EXIT_STATUS_ERROR", "bootstrap", "" + exitStatus.intValue());
		            		}
	                    	chromosome = currentSequence.getName();
	                    	createScalarDS();
	                    	
	                    	run = true;
	                    }
		            }
		            } catch (Exception e) {
		            	logger.error("Exit status will be set to one as exception occurred in bootstrap thread: " + PileupUtil.getStrackTrace(e));			              
		                exitStatus.incrementAndGet();
		            	e.printStackTrace();
		                Thread.currentThread().interrupt();
		            } finally {
		                logger.info(String
		                        .format("Finished thread for: " + chromosome + " total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn is %d",
		                                sleepcount, countOutputSleep, sleepUnit,
		                                queueIn.size()));
		                bootstrapLatch.countDown();
		            }
        }	 
	 
		private void createScalarDS() throws Exception {
			String name = currentSequence.getName();
			//create a group for the chromosome
			
									
			byte[] bases = currentSequence.getBases();
			int datasetLength = bases.length;
			
			logger.info("Dataset size for " +name+ ": " + datasetLength);			
			
			String groupName = hdf.bootstrapReferenceGroup(name, datasetLength);
			
			int chunkSize = chunk;
			if (datasetLength < chunkSize) {
				chunkSize = datasetLength;				
			}
			
			PositionDS positionDS = new PositionDS(hdf, groupName,datasetLength, chunkSize, datasetLength);
			
			for (int i = 0; i < datasetLength; i++) {
				//create position dataset member
				int baseCount = i + 1;

				//add data member 
				positionDS.addMember(i, baseCount,(char) bases[i]);

				if (i % 25000000 == 0) {
					//logger.info(name + " count: " + i);
				}		
			}
			
			//create the dataset for position information
			positionDS.createDataset();	
			
			positionDS = null;
			
			//create strand datasets
			
			createStrandDataset(chunkSize, datasetLength, false);
			createStrandDataset(chunkSize, datasetLength, true);
			
		}

		private void createStrandDataset(int chunkSize, int datasetLength, boolean isReverse) throws Exception {
			StrandDS strandDS = new StrandDS(hdf, chunkSize, datasetLength, currentSequence.getName(), isReverse);
			strandDS.createStrandGroup();
			strandDS.createDatasets();	
			strandDS = null;			
		}
	 }
	
}
