/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
//package org.qcmg.qbasepileup.snp;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.AbstractQueue;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.NavigableMap;
//import java.util.Map.Entry;
//import java.util.TreeMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import net.sf.picard.reference.FastaSequenceIndex;
//import net.sf.picard.reference.IndexedFastaSequenceFile;
//import net.sf.samtools.SAMFileReader;
//import net.sf.samtools.SAMRecord;
//import net.sf.samtools.SAMRecordIterator;
//
//import org.qcmg.common.log.QLogger;
//import org.qcmg.common.log.QLoggerFactory;
//import org.qcmg.common.model.ChrPosition;
//import org.qcmg.picard.SAMFileReaderFactory;
//import org.qcmg.qbamfilter.query.QueryExecutor;
//import org.qcmg.qbasepileup.InputBAM;
//import org.qcmg.qbasepileup.Options;
//import org.qcmg.qbasepileup.QBasePileupUtil;
//
//
//public class SnpBasePileupByChrMT {
//	
//	private static QLogger logger = QLoggerFactory.getLogger(SnpBasePileupByChrMT.class);
//	private Options options;
//	AtomicInteger totalExamined = new AtomicInteger();
//	AtomicInteger positionCount = new AtomicInteger();
//	AtomicInteger uniquePositionCount = new AtomicInteger();
//	int threadNo = 0;
//	AtomicInteger exitStatus = new AtomicInteger();
//	final int sleepUnit = 10;
//	final int maxRecords = 100000;
//	final int checkPoint = 10000;
//	private File outputFile;
//	private File pileupFile;
//	
//	public SnpBasePileupByChrMT(Options options) throws Exception {		
//		this.options = options;	
//		this.outputFile = options.getOutput();
//		this.pileupFile = options.getPositionsFile();
//		threadNo = options.getThreadNo();
//		for (InputBAM inputBAM : options.getInputBAMs()) {			
//			execute(inputBAM);
//		}		
//	}
//	public int getExitStatus() {
//		if (this.exitStatus.intValue() > 0) {
//			return 1;
//		}
//		return 0;
//	}
//	
//	private void execute(InputBAM inputBAM) throws Exception {			
//	
//		final AbstractQueue<String> readQueue = new ConcurrentLinkedQueue<String>();    
//            
//        final AbstractQueue<String> writeQueue = new ConcurrentLinkedQueue<String>();
//    
//        final CountDownLatch readLatch = new CountDownLatch(1); // reading
//                                                                    // thread
//        final CountDownLatch pileupLatch = new CountDownLatch(threadNo); // filtering thread
//        final CountDownLatch writeLatch = new CountDownLatch(1); // writing thread for satisfied records
//    
//        // set up executor services
//        ExecutorService readThread = Executors.newSingleThreadExecutor();
//        ExecutorService pileupThreads = Executors
//                .newFixedThreadPool(threadNo);
//        ExecutorService writeThread = Executors.newSingleThreadExecutor();
//
//        try {    
//                	
//        	List<String> headers = getHeader(options.getPositionsFile());         	
//        	
//            readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, pileupLatch, 
//            		options.getPositionsFile()));
//            readThread.shutdown();
//    
//            // kick-off pileup threads
//            for (int i = 0; i < threadNo; i++) {
//                pileupThreads.execute(new Pileup(readQueue,
//                        writeQueue, Thread.currentThread(), readLatch,
//                        pileupLatch, writeLatch, inputBAM));
//            }
//        
//        	pileupThreads.shutdown();
//
//            // kick-off single writing thread to output the satisfied Records
//            writeThread.execute(new Writing(writeQueue, outputFile, pileupFile, Thread.currentThread(), pileupLatch, writeLatch, headers));
//            writeThread.shutdown();
//
//            logger.info("waiting for  threads to finish (max wait will be 60 hours)");
//            readThread.awaitTermination(60, TimeUnit.HOURS);
//            pileupThreads.awaitTermination(60, TimeUnit.HOURS);
//            writeThread.awaitTermination(60, TimeUnit.HOURS);
//
//            if (readQueue.size() != 0 || writeQueue.size() != 0) {
//            	exitStatus.incrementAndGet();
//                throw new Exception(
//                        " threads have completed but queue isn't empty  (inputQueue, writeQueue ):  "
//                                + readQueue.size() + ", " + writeQueue.size());
//            }
//            logger.info("All threads finished");
//    
//        } catch (Exception e) {
//            logger.info(QBasePileupUtil.getStrackTrace(e));
//            exitStatus.incrementAndGet();
//        } finally {
//            // kill off any remaining threads
//            readThread.shutdownNow();
//            writeThread.shutdownNow();
//            pileupThreads.shutdownNow();
//        }
//    } 
//
//	private List<String> getHeader(File file) throws IOException {
//		
//		BufferedReader reader = new BufferedReader(new FileReader(file));
//		List<String> header = new ArrayList<String>();
//		String line = null;
//		while ((line=reader.readLine()) != null) {
//			if (line.startsWith("#") || line.startsWith("analysis_id") || line.startsWith("Hugo") ||  line.startsWith("mutation")) {
//				header.add(line);
//			} else {
//				break;
//			}
//		}
//		reader.close();
//		return header;
//	}
//
//	private class Reading implements Runnable {
//
//        private final AbstractQueue<String> queue;
//        private final Thread mainThread;
//        private final CountDownLatch readLatch;
//        private final CountDownLatch pileupLatch;		
//		private File inputFile;
//		private ArrayList<String> chromosomes;		
//
//        public Reading(AbstractQueue<String> readQueue, Thread mainThread,
//                CountDownLatch readLatch, CountDownLatch filterLatch, File inputFile) {
//            this.queue = readQueue;
//            this.mainThread = mainThread;
//            this.readLatch = readLatch;
//            this.pileupLatch = filterLatch;
//            this.inputFile = inputFile;
//            this.chromosomes = new ArrayList<String>();
//        }
//
//        @Override
//        public void run() {
//            logger.info("Starting to read file: " + inputFile.getAbsolutePath());
//            int countSleep = 0;
//            long count = 0;
//            try {             	
//        		BufferedReader reader = new BufferedReader(new FileReader(inputFile));        		
//        		
//        		String line;
//        		
//        		while ((line=reader.readLine()) != null) {
//        			if (!line.startsWith("#") && !line.startsWith("analysis_id") && !line.startsWith("Hugo") &&  !line.startsWith("mutation")) {
//        				count++;
//        				String[] values = line.split("\t");
//        				String chr = null;
//        				String format = options.getFormat();
//        				
//        				String[] cols = QBasePileupUtil.getPositionColumns(format, values, count);
//        				
//        				chr = QBasePileupUtil.getFullChromosome(cols[1]); 	    		    	
//        				
//        				if (!chromosomes.contains(chr)) {
//        					chromosomes.add(chr);
//        					logger.info(chr);
//        					queue.add(chr);
//        				}        				
//        			}
//        			if (pileupLatch.getCount() == 0) {
//        				reader.close();
//        				if (exitStatus.intValue() == 0) {
//    	    	        	exitStatus.incrementAndGet();
//    	    	        }
//                        throw new Exception("No pileup threads left, but reading from input is not yet completed");
//                    }
//
//                    if (count % checkPoint == 1) {
//                        while (queue.size() >= maxRecords) {
//                            try {
//                                Thread.sleep(sleepUnit);
//                                countSleep++;
//                            } catch (Exception e) {
//                                logger.info(Thread.currentThread().getName()
//                                        + " " + QBasePileupUtil.getStrackTrace(e));
//                            }
//                        }
//                    }
//        		}
//        		
//        		reader.close();                            
//                
//                logger.info("Completed reading thread, read " + count
//                        + " records from input: " + inputFile.getAbsolutePath());
//            } catch (Exception e) {
//            	logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QBasePileupUtil.getStrackTrace(e));
//    	        if (exitStatus.intValue() == 0) {
//    	        	exitStatus.incrementAndGet();
//    	        }
//                mainThread.interrupt();
//            } finally {
//                readLatch.countDown();
//                logger.debug(String
//                        .format("Exit Reading thread, total slept %d times * %d milli-seconds, "
//                                + "since input queue are full.fLatch  is %d; queus size is %d ",
//                                countSleep, sleepUnit, pileupLatch.getCount(),
//                                queue.size()));
//            }
//
//        }
//    }
//	
//   private class Pileup implements Runnable {
//
//	        private final AbstractQueue<String> queueIn;
//	        private final AbstractQueue<String> queueOut;
//	        private final Thread mainThread;
//	        private final CountDownLatch readLatch;
//	        private final CountDownLatch pileupLatch;
//	        private final CountDownLatch writeLatch;
//	        private int countOutputSleep;
//			private String file = null;
//			private InputBAM inputBAM;
//			private QueryExecutor exec = null;
//
//	        public Pileup(AbstractQueue<String> readQueue,
//	                AbstractQueue<String> writeQueue, Thread mainThread,
//	                CountDownLatch readLatch, CountDownLatch pileupLatch,
//	                CountDownLatch wGoodLatch, InputBAM inputBAM) throws Exception {
//	            this.queueIn = readQueue;
//	            this.queueOut = writeQueue;
//	            this.mainThread = mainThread;
//	            this.readLatch = readLatch;
//	            this.pileupLatch = pileupLatch;
//	            this.writeLatch = wGoodLatch;
//	            this.inputBAM = inputBAM;            
//	        }
//
//	        @Override
//	        public void run() {
//
//	            int sleepcount = 0;
//	            int count = 0;
//	            countOutputSleep = 0;
//	            boolean run = true;
//
//	            try {
//	                logger.info("Thread is starting indel pileups...");
//	            	String chromosome;
//	            	if (options.getFilterQuery() != null) {
//	        			this.exec  = new QueryExecutor(options.getFilterQuery());
//	        		}
//	                while (run) {
//	                	chromosome = queueIn.poll();	                    
//
//	                    if (chromosome == null) {
//	                        // must check whether reading thread finished first.
//	                        if (readLatch.getCount() == 0) {
//	                            run = false;
//	                        }
//	                        try {
//	                            Thread.sleep(sleepUnit);
//	                            sleepcount++;
//	                        } catch (InterruptedException e) {
//	                            logger.info(Thread.currentThread().getName() + " "
//	                                    + e.toString());
//	                        }
//
//	                    } else {	                        
//	                        count++;  
//	                        
//	                        int total = totalExamined.incrementAndGet();
//	                        if  (total % 10000 == 0) {
//	                        	logger.info("Total records processed " + total + " so far...");
//	                        }             
//	                        
//	                        
//                        	TreeMap<Integer, List<SnpPositionPileup>> snpPositions = loadPositionsFile(inputBAM, options.getPositionsFile(), options.getFormat(), chromosome);
//                        	                        	
//                        	SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(inputBAM.getBamFile(), "silent");
//                     		
//                     		//get chromosome length
//                     		String fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);         		
//                     		
//                     		if (snpPositions.size() > 0) {
//	                 			int startKey = snpPositions.firstKey()-250;
//	                 			int endKey = snpPositions.lastKey()+250;
//	                 			logger.info("Getting records from "+fullChromosome+": " + startKey  + " to " + endKey);
//	                     		SAMRecordIterator iter = reader.queryOverlapping(fullChromosome, startKey, endKey);		
//	                     		while (iter.hasNext()) {                         			
//	                     			SAMRecord r = iter.next();                        		
//	                     		
//	                         		if (!r.getReadUnmappedFlag() && (exec == null || (exec != null && exec.Execute(r))) ) {
//	                         			NavigableMap<Integer, List<SnpPositionPileup>> region = snpPositions.subMap(r.getAlignmentStart(), true, r.getAlignmentEnd(), true);
//	                         			
//	                         			for (Entry<Integer, List<SnpPositionPileup>> entry : region.entrySet()) {
//	                         				List<SnpPositionPileup> snps = entry.getValue();
//	                         				
//	                         				for (SnpPositionPileup p: snps) {
//	                         					p.addSAMRecord(r);
//	                         				}
//	                         			}
//	                         		}
//	                     		}	
//	                         		
//	                        	reader.close();
//	                        	
//	                        	for (Entry<Integer, List<SnpPositionPileup>> entry: snpPositions.entrySet()) {                       		
//	                                 	
//	                             	List<SnpPositionPileup> list = entry.getValue();
//	                             	
//	                             	for (SnpPositionPileup pileup : list) {
//	                             		if (count % checkPoint == 0) {
//	                                        
//	                                        if (!mainThread.isAlive()) {
//	                                            logger.error("mainThread died: " + mainThread.getName());
//	                                            run = false;
//	                                        }
//	                                        // check queue size
//	                                        while (queueOut.size() >= maxRecords) {
//	                                            try {
//	                                                Thread.sleep(sleepUnit);
//	                                                countOutputSleep++;
//	                                            } catch (InterruptedException e) {
//	                                                logger.debug(Thread.currentThread().getName() + " "
//	                                                        + QBasePileupUtil.getStrackTrace(e) + " (queue size full) ");
//	                                            }
//	                                            if (writeLatch.getCount() == 0) {
//	                                                logger.error("output queue is not empty but writing thread is complete");
//	                                                run = false;
//	                                            }
//	                                        } 
//	                                    }                                 		
//	                                 		queueOut.add(pileup.toString() + "\n");
//	                             	}
//	                    		} 
//	                         }
//	                    }                 	                                                     
//                    }                       
//
//	                logger.info("Completed pileup thread: "
//	                        + Thread.currentThread().getName());
//	            } catch (Exception e) {
//	            	logger.error("Setting exit status in pileup thread to 1 as exception caught file: " + file + " " + QBasePileupUtil.getStrackTrace(e));
//	    	        if (exitStatus.intValue() == 0) {
//	    	        	exitStatus.incrementAndGet();
//	    	        }
//	                mainThread.interrupt();
//	            } finally {
//	                logger.debug(String
//	                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d, %d",
//	                                sleepcount, countOutputSleep, sleepUnit,
//	                                queueIn.size(), queueOut.size()));
//	                pileupLatch.countDown();
//	            }
//	        }
//
//	        private TreeMap<Integer, List<SnpPositionPileup>> loadPositionsFile(InputBAM inputBAM, File positionsFile, String format, String chromosome) throws Exception {
//	        	TreeMap<Integer, List<SnpPositionPileup>> snpPositions = new TreeMap<Integer, List<SnpPositionPileup>>();
//	    		
//	    		IndexedFastaSequenceFile indexedFastaFile = QBasePileupUtil.getIndexedFastaFile(options.getReference());
//	        	FastaSequenceIndex index = QBasePileupUtil.getFastaIndex(options.getReference());
//	    		BufferedReader reader = new BufferedReader(new FileReader(positionsFile));
//	    		int count = 0;
//	    		String line;
//	    		while ((line=reader.readLine()) != null) {
//	    			if (!line.startsWith("#") && !line.startsWith("analysis_id") && !line.startsWith("Hugo") &&  !line.startsWith("mutation")) {
//	    				count++;
//	    				positionCount.incrementAndGet();
//	    				SnpPosition p = null;
//	    				String[] values = line.split("\t");			     				
//	    				
//	    				String[] columns = QBasePileupUtil.getPositionColumns(format, values, count);
//        				
//    					p = new SnpPosition(columns[0],columns[1],new Integer(columns[2]),new Integer(columns[3]));   			
//	      				
//	    				p.retrieveReferenceBases(indexedFastaFile, index);
//	    				
//	    				QueryExecutor exec = null;				
//	    				if (options.getFilterQuery() != null) {
//	    					exec = new QueryExecutor(options.getFilterQuery());
//	    				}
//	    				String fullChromosome = QBasePileupUtil.getFullChromosome(p.getChromosome());
//	    				if (fullChromosome.equals(chromosome)) {
//	    					SnpPositionPileup pileup = new SnpPositionPileup(inputBAM, p, options, exec);	    				
//		    				
//		    				List<SnpPositionPileup> list = new ArrayList<SnpPositionPileup>();
//		    				if (snpPositions.containsKey(p.getStart())) {
//		    					snpPositions.get(p.getStart());
//		    				}
//		    				list.add(pileup);
//		    				snpPositions.put(p.getStart(), list);		    					
//	    				}	    							
//	    			}
//	    			
//	    		}
//	    		reader.close();
//	    		return snpPositions;
//	    	}
//   }
//	
//	 private class Writing implements Runnable {
//	        private final File resultsFile;
//	        //private final File pileupFile;
//	        private final AbstractQueue<String> queue;
//	        private final Thread mainThread;
//	        private final CountDownLatch filterLatch;
//	        private final CountDownLatch writeLatch;
//			private List<String> headers;
//
//	        public Writing(AbstractQueue<String> q, File f, File pileupFile, Thread mainThread,
//	                CountDownLatch fLatch, CountDownLatch wLatch, List<String> headers) {
//	            queue = q;
//	            resultsFile = f;
//	            //this.pileupFile = pileupFile;
//	            this.mainThread = mainThread;
//	            this.filterLatch = fLatch;
//	            this.writeLatch = wLatch;
//	            this.headers = headers;
//	        }
//
//	        @Override
//	        public void run() {
//	            int countSleep = 0;
//	            boolean run = true;
//	            
//	           
//	            try {
//	                String record;
//	                int count = 0;
//	                BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
//	                               
//	                StringBuffer dccHeader = new StringBuffer();
//	                
//	                for (String h: headers) {
//	                	dccHeader.append(h + "\n");
//	                }	                
//	               
//	                while (run) {
//	                    
//	                    if ((record = queue.poll()) == null) {
//	                        if (filterLatch.getCount() == 0)
//	                            run = false;
//	                        try {
//	                            Thread.sleep(sleepUnit);
//	                            countSleep++;
//	                        } catch (Exception e) {
//	                        	if (exitStatus.intValue() == 0) {
//	        	    	        	exitStatus.incrementAndGet();
//	        	    	        }
//	                            logger.info(Thread.currentThread().getName() + " "
//	                                    + QBasePileupUtil.getStrackTrace(e));
//	                        }
//
//	                        if ((count % checkPoint == 0) && (!mainThread.isAlive())) {
//	                        	writer.close();	                        	
//	                            throw new Exception("Writing threads failed since parent thread died.");
//	                        }
//
//	                    } else {
//	                        writer.write(record);
//	                        count++;
//	                    }
//	                }
//
//	                writer.close();
//	                
//	                //rewrite in order
//	                reorderFile(resultsFile, dccHeader.toString());
//               
//	                
//	                if (!mainThread.isAlive()) {
//	                	if (exitStatus.intValue() == 0) {
//		    	        	exitStatus.incrementAndGet();
//		    	        }
//	                    throw new Exception("Writing threads failed since parent thread died.");
//	                } else {
//	                    logger.info("Completed writing threads, added " + count
//	                            + " records to the output: "
//	                            + resultsFile.getAbsolutePath());
//	                }
//	            } catch (Exception e) {
//	            	logger.error("Setting exit status to 1 as exception caught in writing thread: " + QBasePileupUtil.getStrackTrace(e));
//	    	        if (exitStatus.intValue() == 0) {
//	    	        	exitStatus.incrementAndGet();
//	    	        }
//	                mainThread.interrupt();
//	            } finally {
//	                writeLatch.countDown();
//	                logger.debug("Exit Writing threads, total " + countSleep
//	                        + " times get null from writing queue.");
//	            }
//	        }
//
//			private void reorderFile(File file, String header) throws IOException {
//				BufferedReader reader = new BufferedReader(new FileReader(file));
//				
//				int count = 0;
//				String line = reader.readLine();;
//				Map<ChrPosition, String> map = new TreeMap<ChrPosition, String>();
//				while(line != null) {
//					
//					String[] values = line.split("\t");
//					count++;
//					map.put(new ChrPosition(values[4],new Integer(values[5]),new Integer(values[6]), Integer.toString(count)), line);
//					line = reader.readLine();
//				}
//				reader.close();
//				
//				printMap(map, file, header);
//				
//				
//			}
//			
//			private void printMap(Map<ChrPosition, String> map, File file, String header) throws IOException {
//				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//				writer.write(header);
//				for (Entry<ChrPosition, String> entry: map.entrySet()) {
//					writer.write(entry.getValue() + "\n");
//				}
//				writer.close();
//				
//			}
//	    }
//}
