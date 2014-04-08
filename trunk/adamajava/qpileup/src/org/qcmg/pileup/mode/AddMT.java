/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.mode;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ncsa.hdf.hdf5lib.H5;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.pileup.Options;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.hdf.MetadataRecordDS;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.NonReferenceRecord;
import org.qcmg.pileup.model.PileupDataRecord;
import org.qcmg.pileup.model.PileupSAMRecord;
import org.qcmg.qbamfilter.query.QueryExecutor;

public class AddMT {
	
	private PileupHDF hdf;	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private int blockSize;
    private final int sleepUnit = 10;
    private int noOfThreads;	
	private List<String> bamFiles;	
	private AtomicInteger exitStatus = new AtomicInteger(0);
	private ConcurrentHashMap<String, AtomicLong> totalSAMRecordMap = new ConcurrentHashMap<String, AtomicLong>();
	private ConcurrentHashMap<String, AtomicLong> filteredSAMRecordMap = new ConcurrentHashMap<String, AtomicLong>();
	private MetadataRecordDS metaDS;
	private long startTime;
	private int bufferNumber;
	private String mode;
	private List<String> ranges;
	private boolean isRemove = false;
	private boolean isBamOverride = false;
	private Integer lowReadCount;
	private Integer nonrefThreshold;
	private AtomicInteger noMDCount = new AtomicInteger();
	private Options options;

	public AddMT(Options options, long startTime, String mode) throws Exception {
		 
		this.hdf = new PileupHDF(options.getHdfFile(), false, false);
		this.bamFiles = options.getBamFiles();
		this.startTime = startTime;
		this.metaDS = new MetadataRecordDS(hdf);
		this.bufferNumber = -1;
		this.mode = mode;
		this.noOfThreads = options.getThreadNo();
		this.ranges = options.getReadRanges();
		this.isBamOverride = options.isBamOverride();
		this.blockSize = PileupUtil.getBlockSize(mode, noOfThreads);
		this.options = options;
		//set up counts for each bam file
		for (String bamFile: bamFiles) {
			logger.info("Bam to : " + mode + " "  + bamFile);
			totalSAMRecordMap.put(bamFile, new AtomicLong());
			filteredSAMRecordMap.put(bamFile, new AtomicLong());
		}
		if (mode.equals("remove")) {
			this.isRemove = true;
		}
		
		logger.info("Bam override: "  + options.isBamOverride());
		logger.info("Block size: " + blockSize);
		if (options.getFilter() != null) {
			logger.info("Filter query: " + options.getFilter());
		}
	}

	public int execute() {

		//set up initial metadata characteristics		
		final AbstractQueue<Chromosome> readQueue = new ConcurrentLinkedQueue<Chromosome>();
	
	    final CountDownLatch readLatch = new CountDownLatch(1); // reading
	                                                            // thread
	    final CountDownLatch pileupLatch = new CountDownLatch(noOfThreads); // filtering thread
	
	    // set up executor services
	    ExecutorService readThread = Executors.newSingleThreadExecutor();
	    ExecutorService pileupThreads = Executors
	            .newFixedThreadPool(noOfThreads);
	
	    try {
	    	hdf.open();	    	
	    	//check the bam files to see if they have been entered before.
	    	metaDS.checkBams(isBamOverride, bamFiles, mode);
	    	
	    	lowReadCount = metaDS.getAttribute("low_read_count");
	    	nonrefThreshold = metaDS.getAttribute("non_reference_threshold");
	    	logger.info("Setting from HDF: lowReadCount is below: " + lowReadCount);
	    	logger.info("Setting from HDF: Threshold for high nonreference is above: " + nonrefThreshold + " percent");

			//get the amount of buffer to use
			this.bufferNumber = 200;

	        // kick-off single reading thread
	        readThread.execute(new Reader(readQueue, Thread.currentThread(), readLatch, pileupLatch));
	        readThread.shutdown();
	
	        // kick-off filtering thread
	        for (int i = 0; i < noOfThreads; i++) {
	            pileupThreads.execute(new PileupAnalysis(readQueue,
	                    Thread.currentThread(), readLatch,
	                    pileupLatch));
	        }
	        pileupThreads.shutdown();
	        
	        logger.info("waiting for threads to finish (max wait will be 500 hours)");
	        readThread.awaitTermination(500, TimeUnit.HOURS);
	        pileupThreads.awaitTermination(500, TimeUnit.HOURS);
	       
	        if (readQueue.size() != 0) {
	        	logger.info("Setting exitStatus to 1 in execute thread as readQueue is not empty.");
	        	if (exitStatus.intValue() == 0) {
                	exitStatus.incrementAndGet();	 
                }
	            throw new QPileupException("INCOMPLETE_READ_THREAD", "" + readQueue.size());
	        } else if (exitStatus.get() > 0) {
	        	throw new QPileupException("EXIT_STATUS_ERROR", mode, "" + exitStatus.intValue());
	        } else {
	        	logger.info("All threads finished");
	        } 
	        
	    } catch (Exception e) {
	    	logger.error("Setting exit status in execute thread to 1 as exception caught in execute method: " + PileupUtil.getStrackTrace(e));
	        if (exitStatus.intValue() == 0) {
	        	exitStatus.incrementAndGet();
	        }
	    } finally {
	        // kill off any remaining threads
	        readThread.shutdownNow();
	        pileupThreads.shutdownNow();
	        
	        if (exitStatus.intValue() == 0) {
		        try {
		        	//write metadata
		        	String time = PileupUtil.getRunTime(startTime, System.currentTimeMillis());
		        	for (String bamFile: bamFiles) {
		        		String totalCount = totalSAMRecordMap.get(bamFile).toString();
		        		
		        		logger.info("Total record count for bamfile " + bamFile + " is: " + totalCount);
		        		if (options.getFilter() != null) {
		        			String filteredCount = filteredSAMRecordMap.get(bamFile).toString();
		        			metaDS.writeMember(PileupUtil.getCurrentDate(), PileupUtil.getCurrentTime(":"), mode, bamFile, filteredCount, time, "");
		        			logger.info("Total filtered count for bamfile " + bamFile + " is: " + totalCount);
		        		} else {
		        			metaDS.writeMember(PileupUtil.getCurrentDate(), PileupUtil.getCurrentTime(":"), mode, bamFile, totalCount, time, "");
		        		}
		        		
		        		
		        	}
		        	metaDS.writeAttribute("bams_added");
		        	hdf.close();
		        } catch (Exception e) {
		        	logger.error("Setting exit status in execute thread to 1 as error occured when getting run time or writing metadata" + PileupUtil.getStrackTrace(e));
		        	if (exitStatus.intValue() == 0) {
		        		exitStatus.incrementAndGet();
		        	}
		        	Thread.currentThread().interrupt();
		        }
	        }
	    }    
	    logger.info("Number of H5 objects left open: " + H5.getOpenIDCount());
		return exitStatus.intValue();
	}

	private class Reader implements Runnable {

	        private final AbstractQueue<Chromosome> queue;
	        private final CountDownLatch rLatch;
	        private final CountDownLatch fLatch;
			private List<Chromosome> chromosomes;	        

	        public Reader(AbstractQueue<Chromosome> q, Thread currentThread,
	                CountDownLatch readLatch, CountDownLatch filterLatch) {
	            this.queue = q;
	            this.rLatch = readLatch;
	            this.fLatch = filterLatch;
	        }

	        @Override
	        public void run() {
	        	for (String file: bamFiles) {
	        		logger.info("File to be used in " + mode + " mode is: " + file);
	        	}
	            int countSleep = 0;
	            try {
	            	
	            	chromosomes = hdf.getChromosomeLengths();
	            	
	    	    	if (ranges.size() == 1 && ranges.get(0).equals("all")) {	    	    		
	    	    		addChromosomesToQueue(chromosomes);
	    	    	} else {
	    	    		List<Chromosome> chromosomesToAdd = new ArrayList<Chromosome>();
	    	    		
	    	    		for (String readRange: ranges) {			
	        				if (readRange.contains(":")) {
	        					
	        					logger.info("Reading read range: " + readRange);
	        					String chrName = readRange.split(":")[0];
	        					String pos = readRange.split(":")[1];
	        					Integer start = new Integer(pos.split("-")[0]);
	        					Integer end = new Integer(pos.split("-")[1]);
	        					Chromosome chr = new Chromosome(chrName, getChromosomeByName(chrName).getTotalLength(), start, end);	        					
	        					chromosomesToAdd.add(chr);
	        					
	        				} else {
	        					String chrName = readRange;
	        					Chromosome chr = getChromosomeByName(chrName);
	        					if (chr != null) {
	        						chromosomesToAdd.add(chr);
	        					} else {
	        						throw new QPileupException("BAD_CHROMOSOME", chrName);
	        					}
	        				}
	        			}
	    	    		addChromosomesToQueue(chromosomesToAdd);
	    	    	}
	    	    	logger.info("Completed reading thread");
	            } catch (Exception e) {
	                logger.error("Setting exitstatus to 1. Error occured in Reader Thread when getting chromosome lengths" + PileupUtil.getStrackTrace(e));
	                exitStatus.incrementAndGet();	                
	                Thread.currentThread().interrupt();
	            } finally {
	                rLatch.countDown();
	                logger.debug(String.format("Exit Reading thread, total slept %d times * %d milli-seconds, "
	                                + "since input queue are full.fLatch  is %d; queus size is %d ",
	                                countSleep, sleepUnit, fLatch.getCount(), queue.size()));
	            }
	        }
	        
	        private Chromosome getChromosomeByName(String chrName) {
	        	Chromosome chr = null;
	        	
				for (Chromosome c: chromosomes) {
					if (c.getName().equals(chrName)) {
						chr = c;
						break;
					}
				}
				return chr;
			}

			private void addChromosomesToQueue(List<Chromosome> currentChromosomes) throws Exception {
				
				for (Chromosome chr : currentChromosomes) {
            		if (fLatch.getCount() == 0) {
                        throw new QPileupException("NO_THREADS", "pileup");
                    }
            		
            		if (exitStatus.intValue() > 0) {
            			 throw new QPileupException("EXIT_STATUS_ERROR", mode, "" + exitStatus.intValue());
            		}
	            	
            		int length = chr.getSectionLength();            		
            		            		
        			if (blockSize > length) {
        				addToQueue(chr.getName(), chr.getTotalLength(), chr.getStartPos(), chr.getEndPos());     				
        			} else {
        				int startIndex = chr.getStartPos();
        				int finalBlockSize = length - (blockSize * (length/blockSize));
        				int finalIndex = -1;
        				if (finalBlockSize < 1) {
        					finalIndex = length;
        				} else {
        					finalIndex = startIndex + length - finalBlockSize;
        				}
        				
        				for (int i=startIndex; i<=finalIndex; i+=blockSize) {
        					if (i == finalIndex) {
        						addToQueue(chr.getName(), chr.getTotalLength(), i, i + finalBlockSize-1);			
        						break;
        					} else {
        						addToQueue(chr.getName(), chr.getTotalLength(), i, i + blockSize-1);	
        					}				
        				}            			
        			}
            	}				
			}

			private void addToQueue(String name, int totalLength, int startPos, int endPos) throws QPileupException {
				Chromosome chrToRead = new Chromosome(name, totalLength, startPos, endPos);
				logger.info("Setting up Chromosome/Contig " + name + " from position "+ chrToRead.getStartPos()+ " to position "+ chrToRead.getEndPos() + " section length : " + chrToRead.getSectionLength() + " full length: " + chrToRead.getTotalLength());
				queue.add(chrToRead);
			}
    }
	 
	  private class PileupAnalysis implements Runnable {

	        private final AbstractQueue<Chromosome> queueIn;
	        private final CountDownLatch readLatch;
	        private final CountDownLatch pileupLatch;
	        private int countOutputSleep;
	        private StrandDS forward = null;
	        private StrandDS reverse = null;
	        private NonReferenceRecord forwardNonRef = null;
	        private NonReferenceRecord reverseNonRef = null;			
		    private Chromosome chromosome;
		    private Map<String, SAMFileReader> bamMap = new HashMap<String, SAMFileReader>();
		    
	        public PileupAnalysis(AbstractQueue<Chromosome> qIn,
	               Thread mainThread,
	                CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
	            this.queueIn = qIn;
	            this.readLatch = rLatch;
	            this.pileupLatch = fLatch;
	            
	            for (String bam : bamFiles) {
	            	bamMap.put(bam, SAMFileReaderFactory.createSAMFileReader(bam));
	            }
	        }

	        @Override
	        public void run() {

	            int sleepcount = 0;
	            countOutputSleep = 0;
	            boolean run = true;
	            
	            try {

	            	QueryExecutor exec = null;
	            	
	            	if (options.getFilter() != null) {
	            		exec = new QueryExecutor(options.getFilter());
	            	}
	                while (run) {
	                    chromosome = queueIn.poll();	                   
	                    
	                    if (chromosome == null) {
	                        // must check whether reading thread finished first.
	                        if (readLatch.getCount() == 0) {
	                        	//write final records	                        	
	                            run = false;
	                        }
	                        
	                        if (exitStatus.get() > 0) {
	                        	logger.error("Error in pileup thread: current chromosome to get is null and exit status is showing error");
		            			throw new QPileupException("EXIT_STATUS_ERROR", mode, "" + exitStatus.intValue());
		            		}

	                        // qIn maybe filled again during sleep, so sleep should
	                        // be secondly
	                        try {
	                            Thread.sleep(sleepUnit);
	                            sleepcount++;
	                        } catch (InterruptedException e) {
	                        	 logger.info("Interrupted exception: " + Thread.currentThread().getName() + " "
		                                    + e.toString());
	                        }

	                    } else {
	                    	
	                    	//logger.info("Starting pileup for: " + chromosome.toString());
	                    	//add a bit of buffer to make sure you get all of the reads for hard clips
	                    	int queryStart = chromosome.getStartPos() - bufferNumber;	                    	
	                    	if (queryStart < 0) {
	                    		queryStart = 1;
	                    	}	                    	
	                    	int queryEnd = chromosome.getEndPos() + bufferNumber;
	                    	
	                    	//read from the HDF file
	                    	readFromHDF();
	                    	
	                    	for (String bamFile : bamMap.keySet()) {
	                    		if (exitStatus.get() > 0) {
		                    		logger.error("Error in pileup thread: current chromosome to get is not null but exit status is showing error");
		                    		throw new QPileupException("EXIT_STATUS_ERROR", mode, "" + exitStatus.intValue());
			            		}
	                    		readSAMRecords(bamFile, chromosome.getName(), chromosome.getSectionLength(), queryStart, queryEnd, exec);
	                    	}
 	            	
      	            		writeToHDF();

	      	            	run = true;
	                    } // end else
	                    
	                }// end while
	                
	                logger.info("Completed pileup thread: "
	                        + Thread.currentThread().getName());
	            } catch (Exception e) {
	                logger.error("Setting exitstatus to 1. Exception caught in pileup thread: " + PileupUtil.getStrackTrace(e));
	                exitStatus.incrementAndGet();	                
	                Thread.currentThread().interrupt();
	            } finally {
	            	closeBamFileReaders();
	                logger.debug(String.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d",
	                                sleepcount, countOutputSleep, sleepUnit,
	                                queueIn.size()));
	                
	                pileupLatch.countDown();
	            }
	        }

			private void closeBamFileReaders() {
				for (Entry<String, SAMFileReader> entry: bamMap.entrySet()) {
					entry.getValue().close();
				}
				
			}

			private void readSAMRecords(String bamFile, String name, int size, int queryStart, int queryEnd, QueryExecutor exec) throws QPileupException{							
            	
            	try {            		
	        		//set up overall stats for the bam
	            	forwardNonRef = new NonReferenceRecord(name, size, false, lowReadCount, nonrefThreshold);
	            	reverseNonRef = new NonReferenceRecord(name, size, true, lowReadCount, nonrefThreshold);
					
					SAMFileReader reader = bamMap.get(bamFile);
	            	SAMRecordIterator iterator = reader.queryOverlapping(name, queryStart, queryEnd);
	            	
	            	//check to make sure there are some reads
	            	if (iterator.hasNext()) {
	            		//read SAMRecords for the current bam
	                	while (iterator.hasNext()) {
	                		SAMRecord record = iterator.next();               		
	                		
	                		
	                		//only count the record if it is between the start and end of the chromosome
	                		if (record.getAlignmentStart() >= chromosome.getStartPos() && record.getAlignmentStart() <= chromosome.getEndPos()) {
	                			totalSAMRecordMap.get(bamFile).incrementAndGet();
	                		}
	                		
	                		long currentCount = totalSAMRecordMap.get(bamFile).longValue();
	                		if ((currentCount > 0) && (currentCount % 1000000) == 0) {
	                			logger.info(currentCount + " records processed"  + " in bam: " + bamFile);
	                		}            		
	                	
	                		if (!record.getReadUnmappedFlag()) {
	                			if (record.getAttribute("MD") != null) {
	                				if (exec == null || exec.Execute(record)) {
		                				PileupSAMRecord p = new PileupSAMRecord(record);    	      	            	
		    	      	            	p.pileup();
		
		    	      	            	if (mode.equals("add")) {
		    	      	            		addToStrandDS(p);
		    	      	            	} else {
		    	      	            		removeFromStrandDS(p);
		    	      	            	}
		    	      	            	
		    	      	            	if (exec != null) {
		    	      	            		filteredSAMRecordMap.get(bamFile).incrementAndGet();
		    	      	            	}
		    	      	            	p = null;
	                				}	    	      	            			                    
	                			} else {
	                				noMDCount.incrementAndGet();
	            					
	            					if (noMDCount.intValue() > 10000) {
	            						reader.close();
	            						throw new QPileupException("NO_MD_TAG");
	            					}
	                			}
	            			} 
	                	}   	            
	            	}
	            	iterator.close();	
	            	//add the stats that need to be done at the end to the datasets				
                	forward.finalizeMetrics(size, isRemove, forwardNonRef);
                	reverse.finalizeMetrics(size, isRemove, reverseNonRef); 
            	} catch (Exception e) {
            		logger.error("Setting exitstatus to 1. Exception caught in pileup thread when reading file: " + bamFile + " \nException is: "+ PileupUtil.getStrackTrace(e));
	                if (exitStatus.intValue() == 0) {
	                	exitStatus.incrementAndGet();	 
	                }
	                throw new QPileupException("BAM_FILE_READ_ERROR", bamFile);
            	} 	
			}

			private void readFromHDF() throws Exception {
	        	int startIndex = chromosome.getStartPos()-1;
	        	int size = chromosome.getSectionLength();
	        	
	        	logger.info("Reading strand dataset: " + chromosome.getName() + " start: " + startIndex + " size:" + size);
            	
            	forward = new StrandDS(hdf, chromosome.getName(), false);
            	forward.readDatasetBlock(startIndex, size);
            	reverse = new StrandDS(hdf, chromosome.getName(), true);
            	reverse.readDatasetBlock(startIndex, size);          	
			}

			private void addToStrandDS(PileupSAMRecord p) throws Exception {
	        	List<PileupDataRecord> records = p.getPileupDataRecords();
				
				for (PileupDataRecord dataRecord : records) {
					if (dataRecord.getPosition() >= chromosome.getStartPos() && dataRecord.getPosition() <= chromosome.getEndPos()) {
						
						int index = dataRecord.getPosition() - chromosome.getStartPos();
						if (dataRecord.isReverse()) {
							reverse.modifyStrandDS(dataRecord, index, false);
							reverseNonRef.addNonReferenceMetrics(dataRecord, index);
						} else {
							forward.modifyStrandDS(dataRecord, index, false);
							forwardNonRef.addNonReferenceMetrics(dataRecord, index);
						}
					} 
				}								
			}			

	        private void removeFromStrandDS(PileupSAMRecord p) throws QPileupException {
	        	List<PileupDataRecord> records = p.getPileupDataRecords();
				
				for (PileupDataRecord dataRecord : records) {
					if (dataRecord.getPosition() >= chromosome.getStartPos() && dataRecord.getPosition() <= chromosome.getEndPos()) {
						int index = dataRecord.getPosition() - chromosome.getStartPos();
						if (dataRecord.isReverse()) {
							reverse.modifyStrandDS(dataRecord, index, true);
							reverseNonRef.addNonReferenceMetrics(dataRecord, index);
						} else {
							forward.modifyStrandDS(dataRecord, index, true);
							forwardNonRef.addNonReferenceMetrics(dataRecord, index);
						}
					} 
				}				
			}

			private void writeToHDF() throws Exception {				
				int index = chromosome.getStartPos() -1;
				forward.writeDatasetBlocks(index,chromosome.getSectionLength(), isRemove);	    
				reverse.writeDatasetBlocks(index,chromosome.getSectionLength(), isRemove);	    
				logger.info("Finished writing strand dataset: " + chromosome.getName() + " start: " + index + " size:" + chromosome.getSectionLength());	        	
			}			
	  }
}