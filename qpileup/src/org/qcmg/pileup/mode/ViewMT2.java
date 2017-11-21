/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.mode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.Options;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.PositionDS;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class ViewMT2 {
	
	private PileupHDF hdf;	
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private int blockSize;
    private final int sleepUnit = 10;
    private int noOfThreads;	
	private final AtomicInteger exitStatus = new AtomicInteger();
	private final List<String> readRanges;
	
	private List<StrandEnum> groupElements = new ArrayList<StrandEnum>();
	private List<StrandEnum> viewElements = new ArrayList<StrandEnum>();
	private String pileupDirName;
	private boolean isViewOption = false;
	private boolean isViewHeaderOption = false;
	private String viewHeader;
	private boolean getForwardElements = true;
	private boolean getReverseElements = true;
	private boolean isHDFVersionOption = false;
	private List<PileupHDF> graphHDFs;
	private boolean includeMetricsGraph;
	private File htmlDir;
	private Map<String, String> graphRangeInfoMap;
	final static String DELIMITER = PileupConstants.DELIMITER;
	private final Map<String, Integer> patientMap = new HashMap<String, Integer>();
	private final Map<String, TreeMap<Integer, String>> positionMap;
	
	public ViewMT2(Options options) throws Exception {		
		this.positionMap = options.getPositionMap();
		this.readRanges = options.getReadRanges();		
		this.viewElements = options.getViewElements();
		this.groupElements = options.getGroupElements();
		this.getForwardElements = options.getForwardElements();
		this.getReverseElements = options.getReverseElements();
		
		this.hdf = new PileupHDF(options.getHdfFile(), false, false);
		if (options.hasViewOption()) {
			isViewOption = true;
			isViewHeaderOption = options.hasHeaderOption();
			isHDFVersionOption = options.hasHDFVersionOption();
			this.noOfThreads = 1;
			this.blockSize = 5000000;		
			
		} else {
			this.pileupDirName = options.getPileupDir().getAbsolutePath();
			new File(pileupDirName).mkdir();
			this.noOfThreads = options.getThreadNo();
			this.blockSize = PileupUtil.getBlockSize(options.getMode(), noOfThreads);
			this.graphHDFs = new ArrayList<PileupHDF>();
			
			this.includeMetricsGraph = options.includeViewGraph();
			
			if (includeMetricsGraph) {
				for (String s: options.getGraphHDFs()) {
					PileupHDF h = new PileupHDF(s, false, false);
					graphHDFs.add(h);
				}
				graphRangeInfoMap = options.getGraphRangeInfoMap();
			}
			logger.info("include metrics graph" + includeMetricsGraph);
			logger.info("graphHDFs size" + graphHDFs.size());
			this.htmlDir = options.getHtmlDir();
		}		
	}
	
	public int execute()  {
		try {	
			for (PileupHDF h : graphHDFs) {
				hdf = h;
				hdf.open();
				logger.info("Starting hdf: " + hdf.getFile().getName());
				//only want to check on the version of the file
				if (isHDFVersionOption) {
					System.out.println(showHDFVersionOption());
				} else {
					if (!includeMetricsGraph) {
						this.viewHeader = hdf.getHDFHeader();
					}
					if (isViewHeaderOption) {				
						System.out.println(viewHeader);				
					} else {
						executeMT();
					}
				}

				hdf.close();
				logger.info("Finishing hdf: " + hdf.getFile().getName());
			}
						
			
			
			
		} catch (Exception e) {			
			logger.error("Exception caught when executing view mode" +  PileupUtil.getStrackTrace(e));
			exitStatus.incrementAndGet();
		}
		if (!isViewOption) {
	    	logger.info("Number of H5 objects left open: " + H5.getOpenIDCount());
	    }
		return exitStatus.intValue();
	}
	
    private String getLinks(File htmlDir) {
    	StringBuilder sb  = new StringBuilder();
		File[] files = htmlDir.listFiles();
		
		if (files !=  null) {
			for (File f: files) {
				sb.append("<ul>");					
				String[] values = f.getName().replace(".html",  "").split("_");
				String name = values[0] + ":" + values[1] + "-" + values[2];
				sb.append("<li><a href=\""+f.getName()+"\">"+name+"</a> "+graphRangeInfoMap.get(name)+"</li></br>\n");

				
				sb.append("</ul>");
			}
		}
		return sb.toString();
	}

	private String showHDFVersionOption() throws HDF5LibraryException, HDF5Exception {
		StringBuilder sb = new StringBuilder();
	    	sb.append("## VERSION_BOOTSTRAP=").append(hdf.getVersionMessage()).append("\n");
	    	sb.append("## VERSION_FILE=").append(hdf.getVersionFileMessage()).append("\n");
	    	return sb.toString();
	}



	private int executeMT() {
		if (!isViewOption) {
			logger.info("Block size is " + blockSize + " No of threads: " + noOfThreads);
			if (viewElements.size() == 0 && groupElements.size() == 0) {
				logger.info("No elements specified in inifile, so all strand elements will be written to file");
			}
		}
		
		//set up initial metadata characteristics		
		final AbstractQueue<List<Chromosome>> readQueue = new ConcurrentLinkedQueue<List<Chromosome>>();
	
	    final CountDownLatch readLatch = new CountDownLatch(1); // reading
	                                                            // thread
	    final CountDownLatch viewLatch = new CountDownLatch(noOfThreads); // filtering thread
	
	    // set up executor services
	    ExecutorService readThread = Executors.newSingleThreadExecutor();
	    ExecutorService viewThreads = Executors
	            .newFixedThreadPool(noOfThreads);
	
	    try {
				
	        // kick-off single reading thread
	        readThread.execute(new Setup(readQueue, Thread.currentThread(), readLatch, viewLatch));
	        readThread.shutdown();
	
	        // kick-off filtering thread
	        for (int i = 0; i < noOfThreads; i++) {
	            viewThreads.execute(new View(readQueue,Thread.currentThread(), readLatch,
	                    viewLatch));
	        }
	        viewThreads.shutdown();
	        if (!isViewOption) {
	        	logger.info("waiting for threads to finish (max wait will be 500 hours)");
	        }
	        readThread.awaitTermination(500, TimeUnit.HOURS);
	        viewThreads.awaitTermination(500, TimeUnit.HOURS); 

	        if (readQueue.size() != 0) {
	        	logger.info("Setting exitStatus to 1 in execute thread as readQueue is not empty.");
	        	exitStatus.incrementAndGet();
	            throw new QPileupException("INCOMPLETE_READ_THREAD", "" + readQueue.size());
	        } else if (exitStatus.intValue() > 0) {
	        	throw new QPileupException("EXIT_STATUS_ERROR", "merge", "" + exitStatus.intValue());
	        } else {
	        	if (!isViewOption) {
		        	logger.info("All threads finished, closing HDF file");
		        }
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

	        private final AbstractQueue<List<Chromosome>> queue;
	        private final CountDownLatch rLatch;
	        private final CountDownLatch fLatch;	  
	        private Map<String, List<Chromosome>> queueMap = new TreeMap<String, List<Chromosome>>();
	        private List<Chromosome> chromosomes = new ArrayList<Chromosome>();
			
	        
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
	            	logger.info("Here1");
//	            	if (includeMetricsGraph) {
//	            		chromosomes = graphHDFs.get(0).getChromosomeLengths();
//	            		for (PileupHDF h: graphHDFs) {
//	            			
//	            		}
//	            	} else {
	            		chromosomes = hdf.getChromosomeLengths();
	            		patientMap.put(hdf.getFile().getName(), hdf.getBamFilesInHeader().size());
//	            	}            	    	
	            	logger.info("Here2");
	            	
	            	queueMap = PileupUtil.getChromosomeRangeMap(readRanges, chromosomes);	            	
	            	logger.info("Here3");        		
	        		addMapToQueue();
	            } catch (Exception e) {
	            	logger.info("Setting exit status to 1 as exception caught in setup thread: " + PileupUtil.getStrackTrace(e));
	                exitStatus.incrementAndGet();	                
	                Thread.currentThread().interrupt();
	            } finally {
	                rLatch.countDown();
	                logger.debug(String
	                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
	                                + "since input queue are full.fLatch  is %d; queus size is %d ",
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
				if (!isViewOption) {
					logger.info("Read chromosome: " + key);
				}
	    		if (fLatch.getCount() == 0) {
                    throw new QPileupException("NO_THREADS", "view");
                }
        		
        		if (exitStatus.intValue() > 0) {
        			 throw new QPileupException("EXIT_STATUS_ERROR", "view", "" + exitStatus.intValue());
        		}
				queue.add(list);	
				
			}
	  }
	 
	  private class View implements Runnable {

	        private final AbstractQueue<List<Chromosome>> queueIn;
	        final CountDownLatch readLatch;
	        final CountDownLatch viewLatch;
	        private int countOutputSleep;	
			private BufferedWriter writer;
			private List<Chromosome> chromosomes;
	        /**
	         * 
	         * @param readQueue
	         *            : store SAM record from input file
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
	        public View(AbstractQueue<List<Chromosome>> readQueue,
	                Thread mainThread,
	                CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
	            this.queueIn = readQueue;
	            this.readLatch = rLatch;
	            this.viewLatch = fLatch;  
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
	                        	logger.error("Error in view thread: current chromosome to get is null and exit status is showing error");
		            			throw new QPileupException("EXIT_STATUS_ERROR", "view", "" + exitStatus.intValue());
		            		}

	                        try {
	                            Thread.sleep(sleepUnit);
	                            sleepcount++;
	                        } catch (InterruptedException e) {
	                            logger.info("Interrupted exception: " + Thread.currentThread().getName() + " "
	                                    + e.toString());
	                        }

	                    } else {
	                    	logger.info("Here we are: " + chromosomes.get(0).getName());
	                    	//Collections.sort(chromosomes);
	                    	
	                    	if (!isViewOption) {
	                    		logger.info("Starting chromosome: " + chromosomes.get(0).getName());
	                    	}       	        	             	            
	                    	String file = writeHeader(chromosomes.get(0).getName());
	        	            for (Chromosome chromosome: chromosomes) {
	        	            	
		        	            int totalLength = chromosome.getSectionLength();
		                		
		            			if (blockSize > totalLength) { 
//		            				if (includeMetricsGraph) {
//		            					for (PileupHDF h: graphHDFs) {
//		            						readAndWriteData(h, chromosome, chromosome.getStartPos(), chromosome.getEndPos());
//		            					}		            				
//		            				} else {
		            					readAndWriteData(hdf, chromosome, chromosome.getStartPos(), chromosome.getEndPos());
//		            				}
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
//		            						if (includeMetricsGraph) {
//		            							for (PileupHDF h: graphHDFs) {
//		            								readAndWriteData(h, chromosome, i, i + finalBlockSize-1);
//				            					}
//				            				} else {
				            					readAndWriteData(hdf, chromosome, i, i + finalBlockSize-1);
//				            				}
		            						break;
		            					} else {
//		            						if (includeMetricsGraph) {
//		            							for (PileupHDF h: graphHDFs) {
//		            								readAndWriteData(h, chromosome, i, i + blockSize-1);
//				            					}
//				            				} else {
				            					readAndWriteData(hdf, chromosome, i, i + blockSize-1);
//		            							for (PileupHDF h: graphHDFs) {		            								
//		            								readAndWriteData(h, chromosome, i, i + blockSize-1);
//			            					    }
//				            				}
		            					}				
		            				}            			
		            			}
		            			
		            			
//		            			if (includeMetricsGraph && file != null) {
//		            				String range = chromosome.getName() + ":" + chromosome.getStartPos() + "-" + chromosome.getEndPos();
//		            				MetricsHtmlRecord r = new MetricsHtmlRecord(range, "Position", "Value", file, graphRangeInfoMap.get(range));
//		            				GoogleHtml h = new GoogleHtml(htmlDir.getAbsolutePath(), chromosome.getName() + "_" + chromosome.getStartPos() + "_" + chromosome.getEndPos(), r, ".html");
//		            				h.write();
//		            			}
	        	            }
	        	            if (!isViewOption) {
	        	            	writer.close();
	        	            }
	                    } // end else	                    
	                }// end while
	                if (!isViewOption) {
	                logger.info("Completed pileup thread: "
	                        + Thread.currentThread().getName());
	                }
	            } catch (Exception e) {
	                logger.error("Exit status will be set to one as exception occurred in view thread: " + PileupUtil.getStrackTrace(e));
	              
	                exitStatus.incrementAndGet();
	            	e.printStackTrace();
	                Thread.currentThread().interrupt();
	            } finally {
	                logger.debug(String
	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn is %d",
	                                sleepcount, countOutputSleep, sleepUnit,
	                                queueIn.size()));
	                viewLatch.countDown();
	            }
	        }
	        
	        private String writeHeader(String c) throws IOException {

            	String fileHeader = new String();
            	fileHeader = "## Reference"+DELIMITER+"Position"+DELIMITER+"Ref_base"+DELIMITER + StrandEnum.getHeader() + "\n"; 
            	
            	String pileupFile = pileupDirName + PileupConstants.FILE_SEPARATOR + c + "." + hdf.getFile().getName() + ".txt";
        		this.writer = new BufferedWriter(new FileWriter(pileupFile, true));
        		if (!includeMetricsGraph) {
            		writer.write(viewHeader);
            		writeReadRange();
        		}
        		writer.write(fileHeader);
        		return pileupFile;
			}

			private String writeHeader(Chromosome c) throws IOException {

            	String fileHeader = new String();
            	
            	if (viewElements.size() == 0 && groupElements.size() == 0) {
            		fileHeader = "## Reference"+DELIMITER+"Position"+DELIMITER+"Ref_base"+DELIMITER + StrandEnum.getHeader() + "\n";            		
            	} else {
            		if (viewElements.size() > 0) {
            			fileHeader = "## Reference"+DELIMITER+"Position"+DELIMITER+"Ref_base"+DELIMITER +  StrandEnum.getHeader(viewElements) + "\n";
            		} else {            			
            			fileHeader = "## Reference"+DELIMITER+"Position"+DELIMITER+"Ref_base"+DELIMITER +  StrandEnum.getHeader(groupElements, getForwardElements, getReverseElements) + "\n";
            		}
            	}
            	if (isViewOption) {
            		System.out.print(viewHeader);
            		writeReadRange();
            		System.out.print(fileHeader);
            	} else {
            		String pileupFile = pileupDirName + PileupConstants.FILE_SEPARATOR + c.getName() + "_" + c.getStartPos() + "_" + c.getEndPos() + ".qpileup.txt";
            		this.writer = new BufferedWriter(new FileWriter(pileupFile, true));
            		if (!includeMetricsGraph) {
	            		writer.write(viewHeader);
	            		writeReadRange();
            		}
            		writer.write(fileHeader);
            		return pileupFile;
            	}
            	return null;
			}

			private void writeReadRange() throws IOException {

				if (chromosomes.size()== 1) {
					String currentRange = "## RANGE=" +chromosomes.get(0).getName()+ "\n";
					if (isViewOption) {
						System.out.print(currentRange);
					} else {
						writer.write(currentRange);
					} 
				} else {
					for (Chromosome c : chromosomes) {
						String currentRange = "## RANGE=" + c.toString() + "\n";							
						if (isViewOption) {
							System.out.print(currentRange);
						} else {
							writer.write(currentRange);								
						} 							
					}
				}				
			}

			private void readAndWriteData(PileupHDF hdf, Chromosome chromosome, int startPos, int endPos) throws Exception {
				if (!isViewOption) {
					logger.info("Starting to read from HDF: " + chromosome.getName() + " Start: " + startPos + " End:" + endPos);
				}
				
//				if (includeMetricsGraph && startPos == endPos) {
//					startPos -= 10;
//					endPos += 10;
//				}
	        	int totalToRead = endPos - startPos + 1;
        		int startIndex = startPos - 1;

            	PositionDS position = new PositionDS(hdf, chromosome.getHdfGroupName());
        		StrandDS forward = new StrandDS(hdf, chromosome.getName(), false);	
        		StrandDS reverse = new StrandDS(hdf, chromosome.getName(), true);

        		//read
        		if (!isViewOption) {
        			logger.info("Reading from: " + chromosome.getName() + " Start: " + startPos + " End: " + endPos);
        		}
        		position.readDatasetBlock(startIndex, totalToRead);
        		forward.readDatasetBlock(startIndex, totalToRead);
        		reverse.readDatasetBlock(startIndex, totalToRead);
        		
        		if (!isViewOption) {
        			writer.write("#" + hdf.getFile().getName() + "\n");
        		}
        		
        		//write      		
        		for (int i=0; i<position.getDatasetLength(); i++) {
        			QPileupRecord qRecord = new QPileupRecord(position.getPositionElement(i), forward.getStrandElementMap(i), reverse.getStrandElementMap(i));
        			if (positionMap.get(qRecord.getChromosome()).containsKey((int) qRecord.getBasePosition())) {
        				String output = qRecord.getRecordString(viewElements, groupElements, getForwardElements, getReverseElements);	
            			output += "\t" + patientMap.get(hdf.getFile().getName());
            			if (isViewOption) {
            				System.out.print(output + "\n");
            			} else {        	
            				writer.write(output + "\n");
            			}
        			}
        			
        		} 
        		if (!isViewOption) {
        			logger.info("Finished writing: " + chromosome.getName() + " Start: " + startPos + " End: " + endPos);
        		}
	        }      
	  }
}


