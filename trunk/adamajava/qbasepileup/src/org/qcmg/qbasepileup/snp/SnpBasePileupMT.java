/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupConstants;
import org.qcmg.qbasepileup.QBasePileupException;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class SnpBasePileupMT {
	
	private static final ReferenceNameComparator REF_NAME_COMP = new ReferenceNameComparator();

	private static QLogger logger = QLoggerFactory.getLogger(SnpBasePileupMT.class);
	private final Options options;
	private final AtomicInteger totalExamined = new AtomicInteger();
	private final AtomicInteger totalPassedFilters =new AtomicInteger();
	private final AtomicInteger totalReadsNotMapped = new AtomicInteger();
	private final AtomicInteger totalReadsBadBaseQual = new AtomicInteger();
	private final AtomicInteger totalReadsBaseMapQual = new AtomicInteger();
	private final AtomicInteger positionCount = new AtomicInteger();
//	private final AtomicInteger uniquePositionCount = new AtomicInteger();
	int threadNo = 0;
	private final AtomicInteger exitStatus = new AtomicInteger();
	final int sleepUnit = 20;
	final int maxRecords = 100000;
	final int checkPoint = 10000;
	private List<String> headerLines = new ArrayList<String>();


	public SnpBasePileupMT(Options options) throws Exception {		
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

		//get maf headers
		if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
			headerLines = QBasePileupUtil.getHeaderLines(options.getPositionsFile());			
		}

//		final AbstractQueue<InputBAM> readQueue = new ConcurrentLinkedQueue<>();    
		final AbstractQueue<SnpPosition> readQueue = new ConcurrentLinkedQueue<SnpPosition>();    
		final List<SnpPosition> positions = new ArrayList<>();

		final AbstractQueue<String> writeQueue = new ConcurrentLinkedQueue<String>();

//		final CountDownLatch readLatch = new CountDownLatch(1); // reading
		// thread
		final CountDownLatch pileupLatch = new CountDownLatch(threadNo); // filtering thread
		final CountDownLatch writeLatch = new CountDownLatch(1); // writing thread for satisfied records

		// set up executor services
//		ExecutorService readThread = Executors.newSingleThreadExecutor();
		ExecutorService pileupThreads = Executors
				.newFixedThreadPool(threadNo);
		ExecutorService writeThread = Executors.newSingleThreadExecutor();

		try {

//			// kick-off single reading thread
//			readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, pileupLatch, 
//					options.getPositionsFile(), options.getFormat(), positions));
//			readThread.shutdown();
			
			new Reading(readQueue, Thread.currentThread(),
					options.getPositionsFile(), options.getFormat(), positions).run();
//			new Reading(readQueue, Thread.currentThread(), readLatch, pileupLatch, 
//					options.getPositionsFile(), options.getFormat(), positions).run();
			
			logger.info("no of snp positions in array: " + positions.size());
			
			
			final ReferenceNameComparator refNameComp = new ReferenceNameComparator();
			
			final Set<SnpPosition> uniqueSnpPositions = new TreeSet<>(new Comparator<SnpPosition>() {
				@Override
				public int compare(SnpPosition o1, SnpPosition o2) {
					int chrDiff = refNameComp.compare(o1.getFullChromosome(), o2.getFullChromosome());
					if (chrDiff != 0) return chrDiff;
					return o1.getStart() - o2.getStart();
				}
			});
			uniqueSnpPositions.addAll(positions);
			
//			readQueue.addAll(options.getInputBAMs());

			// kick-off pileup threads
			for (int i = 0; i < threadNo; i++) {
				pileupThreads.execute(new Pileup(readQueue,
						writeQueue, Thread.currentThread(), 
						pileupLatch, writeLatch, options.getInputBAMs(), uniqueSnpPositions));
//				pileupThreads.execute(new Pileup(readQueue,
//						writeQueue, Thread.currentThread(), readLatch,
//						pileupLatch, writeLatch, options.getInputBAMs(), uniqueSnpPositions));
			}

			pileupThreads.shutdown();

			// kick-off single writing thread to output the satisfied Records
			writeThread.execute(new Writing(writeQueue, options.getOutput(), Thread.currentThread(), pileupLatch, writeLatch));
			writeThread.shutdown();

			logger.info("waiting for  threads to finish (max wait will be 60 hours)");
//			readThread.awaitTermination(60, TimeUnit.HOURS);
			pileupThreads.awaitTermination(60, TimeUnit.HOURS);
			writeThread.awaitTermination(60, TimeUnit.HOURS);

//			if (readQueue.size() != 0 || writeQueue.size() != 0) {
//				exitStatus.incrementAndGet();
//				throw new Exception(
//						" threads have completed but queue isn't empty  (inputQueue, writeQueue ):  "
//								+ readQueue.size() + ", " + writeQueue.size());
//			}
			logger.info("All threads finished");

		} catch (Exception e) {
			logger.info(QBasePileupUtil.getStrackTrace(e));
			exitStatus.incrementAndGet();
		} finally {
			// kill off any remaining threads
//			readThread.shutdownNow();
			writeThread.shutdownNow();
			pileupThreads.shutdownNow();
		}

		logger.debug("TOTAL POSITIONS: \t\t\t" + positionCount);
		logger.debug("UNIQUE POSITIONS: \t\t\t" + new HashSet<>(positions).size());
		logger.debug("TOTAL READS EXAMINED:\t\t"+totalExamined+"");
		logger.debug("---------------------------------------------");
		logger.debug("TOTAL READS KEPT:\t\t"+totalPassedFilters);
		logger.debug("TOTAL READS NOT ON SNP:\t\t"+totalReadsNotMapped);
		logger.debug("READS WITH BAD BASE QUALITY:\t"+totalReadsBadBaseQual);
		logger.debug("READS WITH BAD MAPPING QUALITY:\t"+totalReadsBaseMapQual);
	} 

	private class Reading implements Runnable {

		private final AbstractQueue<SnpPosition> queue;
		private final Thread mainThread;
//		private final CountDownLatch readLatch;
//		private final CountDownLatch pileupLatch;
		private final File positionsFile;
		private final String format;
		private final List<SnpPosition> positions;		

//		public Reading(Thread mainThread,
				public Reading(AbstractQueue<SnpPosition> q, Thread mainThread,
				 File positionsFile, String format, List<SnpPosition> positions) {
//					public Reading(AbstractQueue<SnpPosition> q, Thread mainThread,
//							CountDownLatch readLatch, CountDownLatch filterLatch, File positionsFile, String format, List<SnpPosition> positions) {
			this.queue = q;
			this.mainThread = mainThread;
//			this.readLatch = readLatch;
//			this.pileupLatch = filterLatch;
			this.positionsFile = positionsFile;
			this.format = format;
			this.positions = positions;
		}

		@Override
		public void run() {
			logger.info("Starting to read positions file: " + positionsFile.getAbsolutePath());
			int countSleep = 0;
			long count = 0;
//			FastaSequenceIndex index = QBasePileupUtil.getFastaIndex(options.getReference());
			boolean outputFormat2 = options.getOutputFormat() == 2;

//			IndexedFastaSequenceFile indexedFastaFile = QBasePileupUtil.getIndexedFastaFile(options.getReference());
			try (BufferedReader reader = new BufferedReader(new FileReader(positionsFile));) {

				String line;
				int mutationColumn = -1;
				while ((line=reader.readLine()) != null) {

					if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE) && line.startsWith("analysis_id")) {
						mutationColumn = QBasePileupUtil.getMutationColumn(line);
					}

					if ( ! line.startsWith("#") && ! line.startsWith("\n") && ! line.startsWith("analysis_id") && ! line.startsWith("Hugo") &&  ! line.startsWith("mutation")) {
						count++;

						positionCount.incrementAndGet();
						SnpPosition p = null;
						String[] values = line.split("\t");

						if (outputFormat2 && values.length < 6) {
							logger.warn("positions file has " + values.length + " entries from file: " + positionsFile + ", line: " + line);
							throw new Exception("There are fewer than 6 columns in the positions file and the output format is set to columns - this is prohibited!");
						}

						String[] columns = QBasePileupUtil.getSNPPositionColumns(format, values, count);

						p = new SnpPosition(columns[0], columns[1], Integer.valueOf(columns[2]), Integer.valueOf(columns[3]), line);        				       				      				
//						if ( ! positions.contains(p)) {
//							uniquePositionCount.incrementAndGet();    					
//						}

						if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {        					
							p.setAltBases(QBasePileupUtil.getCompoundAltBases(values, mutationColumn));
						}

						if (outputFormat2) {
							p.setAltBases(values[5].getBytes());
						}

						p.retrieveReferenceBases(options.getReference()); 
//						p.retrieveReferenceBases(indexedFastaFile, index); 

						positions.add(p);
						queue.add(p);
					}        			

//					if (pileupLatch.getCount() == 0) {
//						reader.close();
//						if (exitStatus.intValue() == 0) {
//							exitStatus.incrementAndGet();
//						}
//						throw new Exception("No pileup threads left, but reading from input is not yet completed");
//					}

//					if (count % checkPoint == 1) {
//						while (queue.size() >= maxRecords) {
//							try {
//								Thread.sleep(sleepUnit);
//								countSleep++;
//							} catch (Exception e) {
//								logger.info(Thread.currentThread().getName()
//										+ " " + QBasePileupUtil.getStrackTrace(e));
//							}
//						}
//					}
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
//				readLatch.countDown();
//				logger.debug(String
//						.format("Exit Reading thread, total slept %d times * %d milli-seconds, "
//								+ "since input queue are full.fLatch  is %d; queus size is %d ",
//								countSleep, sleepUnit, pileupLatch.getCount(),
//								queue.size()));
			}

		}
	}

	private class Pileup implements Runnable {

//		private final AbstractQueue<InputBAM> queueIn;
		private final AbstractQueue<SnpPosition> queueIn;
		private final AbstractQueue<String> queueOut;
		private final Thread mainThread;
//		private final CountDownLatch readLatch;
		private final CountDownLatch pileupLatch;
		private final CountDownLatch writeLatch;
		private int countOutputSleep;
		private final List<InputBAM> currentInputs;
		private final Set<SnpPosition> snpPositions;
		private final String file = null;
		private QueryExecutor exec = null;

//		public Pileup(AbstractQueue<InputBAM> queueIn,
				public Pileup(AbstractQueue<SnpPosition> queueIn,
				AbstractQueue<String> queueOut, Thread mainThread,
				CountDownLatch pileupLatch,
//				CountDownLatch readLatch, CountDownLatch pileupLatch,
				CountDownLatch wGoodLatch, List<InputBAM> inputs, Set<SnpPosition> uniqueSnps) throws Exception {
			this.queueIn = queueIn;
			this.queueOut = queueOut;
			this.mainThread = mainThread;
//			this.readLatch = readLatch;
			this.pileupLatch = pileupLatch;
			this.writeLatch = wGoodLatch;
			this.currentInputs = new ArrayList<InputBAM>();
			for (InputBAM i : inputs) {
				currentInputs.add(i);
			}
			this.snpPositions = uniqueSnps;
		}

		@Override
		public void run() {

			int sleepcount = 0;
			int count = 0;
			countOutputSleep = 0;
			boolean run = true;
			
			if (options.getFilterQuery() != null) {
				try {
					this.exec  = new QueryExecutor(options.getFilterQuery());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			SAMRecordIterator iter = null;
			SamReader reader = null;
			
//			while (true && snpPositions.size() > 0) {
//				
//				InputBAM bam = queueIn.poll();
//				
//				if (null == bam) {
//					break;
//				}
//				
//				reader = bam.getSAMFileReader();
//				
//				// get 
//				SAMSequenceDictionary seqDict = reader.getFileHeader().getSequenceDictionary();
//				
//				QueryInterval [] intervals = new QueryInterval[snpPositions.size()];
//				
//				// populate
//				int a = 0;
//				for (SnpPosition snp : snpPositions) {
//					QueryInterval qi = new QueryInterval(seqDict.getSequenceIndex(snp.getFullChromosome()), snp.getStart(), snp.getEnd());
//					intervals[a++] = qi;
//				}
//				intervals = QueryInterval.optimizeIntervals(intervals);
//				
//				List<SAMRecord> bamRecords = new ArrayList<>();
//				
//				logger.info("retrieving records from " + bam.getAbbreviatedBamFileName());
//				iter = reader.queryOverlapping(intervals);
//				while (iter.hasNext()) {
//					bamRecords.add(iter.next());
//				}
//				iter.close();
//				
//				
//				
////				for (SnpPosition snp : snpPositions) {
//////					logger.info("snp pos" + snp.toString());
////					iter = reader.queryOverlapping(snp.getFullChromosome(), snp.getStart(), snp.getEnd());
////					while (iter.hasNext()) {
////						bamRecords.add(iter.next());
////					}
////					iter.close();
////				}
//				
//				
//				// got all records for this snp position from this bam file
//				Set<SAMRecord> uniqueRecs = new HashSet<>(bamRecords);
//				Set<SAMRecord> uniqueFilteredRecs = new HashSet<>();
//				if (null != exec) {
//					for (SAMRecord r : uniqueRecs) {
//						try {
//							if (exec.Execute(r)) {
//								uniqueFilteredRecs.add(r);
//							}
//						} catch (Exception e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				} else {
//					uniqueFilteredRecs = uniqueRecs;
//				}
//				logger.info("No of records from " + bam.getAbbreviatedBamFileName() + " is " + bamRecords.size() + " of which " + uniqueRecs.size() + " are unique, and remain after filtering: " + uniqueFilteredRecs.size());
//				
//				// split set into map keyed on chromosome
//				Map<String, List<SAMRecord>> samsByChr = new HashMap<>();
//				for (SAMRecord r : uniqueFilteredRecs) {
//					String key = r.getReferenceName();
//					List<SAMRecord> records = samsByChr.get(key);
//					if (null == records) {
//						records = new ArrayList<>();
//						samsByChr.put(key, records);
//					}
//					records.add(r);
//				}
//				
//				for (SnpPosition snp : snpPositions) {
//					
////					StringBuilder sb = new StringBuilder();
////					logger.info("snp pos" + snp.toString());
//					SnpPositionPileup pileup = null;
//					try {
//						pileup = new SnpPositionPileup(bam, snp, options, exec);
//					} catch (QBasePileupException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
//					// get list of sams according to chr
//					List<SAMRecord> sams = samsByChr.get(snp.getFullChromosome());
//					try {
//						pileup.pileup(sams, true);
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					totalExamined.addAndGet(pileup.getTotalExamined());
//					totalPassedFilters.addAndGet(pileup.getPassFiltersCount());
//					totalReadsNotMapped.addAndGet(pileup.getDoesntMapCount());
//					totalReadsBadBaseQual.addAndGet(pileup.getBasesNotPassBaseQual());
//					totalReadsBaseMapQual.addAndGet(pileup.getReadsNotPassMapQual());
//					if (options.getMode().equals("snp")) {
//						if (options.getOutputFormat() == 2) {
//							queueOut.add(pileup.toColumnString());
//						} else {
//							queueOut.add(pileup.toString() + "\n");
//						}                 			
//					}
//					if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
//						queueOut.add(pileup.toCompoundString() + "\n");
//					}
//					if (options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
//						queueOut.add(pileup.toMafString() + "\n");
//					}
//				}
//			}
			

			try {

				SnpPosition position;
//				if (options.getFilterQuery() != null) {
//					this.exec  = new QueryExecutor(options.getFilterQuery());
//				}

				Map<InputBAM, SamReader> samReaderCache = new HashMap<>();
				while (run) {
					position = queueIn.poll();	                    

					if (position == null) {

						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.info(Thread.currentThread().getName() + " "
									+ e.toString());
						}

						// must check whether reading thread finished first.
						if (queueIn.size() == 0) {
							run = false;
						}

					} else {	                        
						count++;  
						if (count % 1000 == 0) {
							logger.info("Processed " + count + " records");
						}
						StringBuilder sb = new StringBuilder();
						for (InputBAM i : currentInputs) {
							SamReader sfReader = samReaderCache.get(i);
							if (null == sfReader) {
								sfReader = i.getSAMFileReader();
								samReaderCache.put(i, sfReader);
							}
//							file = i.getBamFile().getAbsolutePath();

							SnpPositionPileup pileup = new SnpPositionPileup(i, position, options, exec);
							pileup.pileup(sfReader);
							totalExamined.addAndGet(pileup.getTotalExamined());
							totalPassedFilters.addAndGet(pileup.getPassFiltersCount());
							totalReadsNotMapped.addAndGet(pileup.getDoesntMapCount());
							totalReadsBadBaseQual.addAndGet(pileup.getBasesNotPassBaseQual());
							totalReadsBaseMapQual.addAndGet(pileup.getReadsNotPassMapQual());
							if (options.getMode().equals("snp")) {
								if (options.getOutputFormat() == 2) {
									queueOut.add(pileup.toColumnString());
								} else {
									queueOut.add(pileup.toString() + "\n");
								}                 			
							}
							if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
								sb.append(pileup.toCompoundString() + "\n");
							}
							if (options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
								queueOut.add(pileup.toMafString() + "\n");
							}
						}

						if (sb.length() > 0) {
							queueOut.add(sb.toString());        	                                                     
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
				//	            	for (Input i : currentInputs) {
				//                    	i.close();
				//	                }
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
//		private final CountDownLatch readLatch;
		final static char TAB = '\t';

		public Writing(AbstractQueue<String> q, File f, Thread mainThread,
				 CountDownLatch fLatch, CountDownLatch wLatch) {
//			public Writing(AbstractQueue<String> q, File f, Thread mainThread,
//					CountDownLatch readLatch, CountDownLatch fLatch, CountDownLatch wLatch) {
			queue = q;
			resultsFile = f;
			this.mainThread = mainThread;
//			this.readLatch = readLatch;
			this.filterLatch = fLatch;
			this.writeLatch = wLatch;
		}

		@Override
		public void run() {
			int countSleep = 0;
			boolean run = true;
			try {
				String record;
				int count = 0;
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));) {
					Map<String, Map<String, String>> inputMap = new HashMap<String, Map<String, String>>();
					
//					List<String> outputList = new ArrayList<>();

					writer.write(getHeader());
					while (run) {

						if ((record = queue.poll()) == null) {
							//last sleep in case of slow queue
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
							if (filterLatch.getCount() == 0 && queue.size() == 0) {
//								if (readLatch.getCount() == 0 && filterLatch.getCount() == 0 && queue.size() == 0) {
								run = false;
							}

							if ((count % checkPoint == 0) && ( ! mainThread.isAlive())) {
								writer.close();
								throw new Exception("Writing threads failed since parent thread died.");
							}

						} else {
							if (options.getOutputFormat() == 2) {
								String[] vals = record.split("\t");
								String position = "";
								String bam = "";
								String info = "";
								for (int i=0; i<vals.length; i++) {
									if (i<5) {
										position += vals[i] + TAB;
									}
									if (i==5) {
										position += vals[i];
									}
									if (i==6) {
										bam = vals[i];
									}
									if (i > 6) {
										if (i == vals.length-1) {
											info += vals[i];
										} else {
											info += vals[i] + TAB;
										}

									}
								}

								Map<String, String> map = inputMap.get(position);
								if (null == map) {
									map = new HashMap<String, String>();
									inputMap.put(position, map);
								}

								map.put(bam, info);

							} else {
//								outputList.add(record);
								writer.write(record);
							}
							count++;
						}
					}

					if (options.getOutputFormat() == 2) {

						// load entries from positions file into a collection
						// then check that all entries in collection have a corresponding entry in the inputMap
						// if not, print out the offenders and exit (fail)
						// if yes, write to file as normal
						List<String> positionsFileList = new ArrayList<>();
						try (BufferedReader reader = new BufferedReader(new FileReader(options.getPositionsFile()));) {
							String line; 
							while ((line = reader.readLine()) != null) {
								if ( ! line.startsWith("#") && ! line.startsWith("Hugo")) {
									positionsFileList.add(line);
								}
							}
						}

						boolean allGood = true;
						StringBuilder positionsMissingInMap = new StringBuilder();
						for (String s : positionsFileList) {
							if ( ! inputMap.containsKey(s)) {
								positionsMissingInMap.append(s).append("\n");
								allGood = false;
							}
						}

						if (allGood) {

							List<InputBAM> inputs = options.getInputBAMs();
							for (String line : positionsFileList) {
								Map<String, String> start = inputMap.get(line);

								StringBuilder sb = new StringBuilder(line);
								sb.append(TAB);

								for (int i=0; i<inputs.size(); i++) {
									sb.append(start.get(inputs.get(i).getAbbreviatedBamFileName()));
									if (i != (inputs.size() -1)) {
										sb.append(TAB);
									}
								}
								writer.write(sb.toString() + "\n");
							}
						} else {
							logger.warn("The following entries in the positions file did not have any results - incorrect reference bases?\n" + positionsMissingInMap.toString());
							throw new QBasePileupException("POSITION_FILE_ERROR");
						}
//					} else {
//						
//						
//						// sort list of results and write
//						Comparator<String> comp = new Comparator<String>() {
//							@Override
//							public int compare(String o1, String o2) {
//								String[] o1Arr = TabTokenizer.tokenize(o1);
//								String[] o2Arr = TabTokenizer.tokenize(o2);
//								
//								// chr. pos, then file id
//								int diff = REF_NAME_COMP.compare(o1Arr[4], o2Arr[4]);
//								if (diff != 0) {
//									return diff;
//								}
//								diff = Integer.compare(Integer.parseInt(o1Arr[5]), Integer.parseInt(o2Arr[5]));
//								if (diff != 0) {
//									return diff;
//								}
//								return Integer.compare(Integer.parseInt(o1Arr[0]), Integer.parseInt(o2Arr[0]));
//							}
//						};
//						
//						Collections.sort(outputList, comp);
//						
//						for (String s : outputList) {
//							writer.write(s);
//						}
						
						
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
			StringBuilder sb = new StringBuilder();
			//version

			//column headers				
			if (options.getMode().equals("snp")) {
				sb.append("##qbasepileup version 1.0\n");
				if (options.getOutputFormat() == 1) {
					sb.append(QBasePileupUtil.getStandardSnpHeader());	
				}
				if (options.getOutputFormat() == 2) {
					sb.append(QBasePileupUtil.getColumnsSnpHeader(options.getInputBAMs()));	
				}									
			}

			if (options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
				for (String line: headerLines) {
					if (line.startsWith("Hugo")) {
						sb.append(line + "\t" + "Indel_Check\n");
					} else {
						sb.append(line + "\n");
					}						
				}
			}

			if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
				sb.append("##qbasepileup version 1.0\n");
				sb.append(QBasePileupUtil.getCompoundSnpHeader());					
			}

			return sb.toString();
		}
	}



}
