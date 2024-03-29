/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import htsjdk.samtools.SamReader;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.Constants;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SnpBasePileupMT {
	
	private static QLogger logger = QLoggerFactory.getLogger(SnpBasePileupMT.class);
	private final Options options;
	private final AtomicInteger totalExamined = new AtomicInteger();
	private final AtomicInteger totalPassedFilters =new AtomicInteger();
	private final AtomicInteger totalReadsNotMapped = new AtomicInteger();
	private final AtomicInteger totalReadsBadBaseQual = new AtomicInteger();
	private final AtomicInteger totalReadsBaseMapQual = new AtomicInteger();
	private final AtomicInteger positionCount = new AtomicInteger();
	int threadNo = 0;
	private final AtomicInteger exitStatus = new AtomicInteger();
	final int sleepUnit = 20;
	final int maxRecords = 100000;
	final int checkPoint = 10000;
	private List<String> headerLines = new ArrayList<>();


	public SnpBasePileupMT(Options options) throws IOException {		
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

	private void execute() throws IOException {

		//get maf headers
		if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
			headerLines = QBasePileupUtil.getHeaderLines(options.getPositionsFile());			
		}

		final AbstractQueue<SnpPosition> readQueue = new ConcurrentLinkedQueue<>();    
		final List<SnpPosition> positions = new ArrayList<>();

		final AbstractQueue<String> writeQueue = new ConcurrentLinkedQueue<>();

		// thread
		final CountDownLatch pileupLatch = new CountDownLatch(threadNo); // filtering thread
		final CountDownLatch writeLatch = new CountDownLatch(1); // writing thread for satisfied records

		// set up executor services
		ExecutorService pileupThreads = Executors.newFixedThreadPool(threadNo);
		ExecutorService writeThread = Executors.newSingleThreadExecutor();

		try {

//			// kick-off single reading thread
			
			new Reading(readQueue, Thread.currentThread(),
					options.getPositionsFile(), options.getFormat(), positions).run();
			
			logger.info("no of snp positions in array: " + positions.size());
			
			
			final ReferenceNameComparator refNameComp = new ReferenceNameComparator();
			
			final Set<SnpPosition> uniqueSnpPositions = new TreeSet<>((o1, o2) -> {
				int chrDiff = refNameComp.compare(o1.getFullChromosome(), o2.getFullChromosome());
				if (chrDiff != 0) return chrDiff;
				return o1.getStart() - o2.getStart();
			});
			uniqueSnpPositions.addAll(positions);
			

			// kick-off pileup threads
			for (int i = 0; i < threadNo; i++) {
				pileupThreads.execute(new Pileup(readQueue,
						writeQueue, Thread.currentThread(), 
						pileupLatch, writeLatch, options.getInputBAMs()));
			}

			pileupThreads.shutdown();

			// kick-off single writing thread to output the satisfied Records
			writeThread.execute(new Writing(writeQueue, options.getOutput(), Thread.currentThread(), pileupLatch, writeLatch));
			writeThread.shutdown();

			logger.info("waiting for  threads to finish (max wait will be 100 hours)");
			pileupThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, java.util.concurrent.TimeUnit.HOURS);
			writeThread.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, java.util.concurrent.TimeUnit.HOURS);

			logger.info("All threads finished");

		} catch (Exception e) {
			logger.info(QBasePileupUtil.getStackTrace(e));
			exitStatus.incrementAndGet();
		} finally {
			// kill off any remaining threads
			writeThread.shutdownNow();
			pileupThreads.shutdownNow();
		}

		logger.debug("TOTAL POSITIONS: \t\t\t" + positionCount);
		logger.debug("UNIQUE POSITIONS: \t\t\t" + new HashSet<>(positions).size());
		logger.debug("TOTAL READS EXAMINED:\t\t"+totalExamined);
		logger.debug("---------------------------------------------");
		logger.debug("TOTAL READS KEPT:\t\t"+totalPassedFilters);
		logger.debug("TOTAL READS NOT ON SNP:\t\t"+totalReadsNotMapped);
		logger.debug("READS WITH BAD BASE QUALITY:\t"+totalReadsBadBaseQual);
		logger.debug("READS WITH BAD MAPPING QUALITY:\t"+totalReadsBaseMapQual);
	} 

	private class Reading implements Runnable {

		private final AbstractQueue<SnpPosition> queue;
		private final Thread mainThread;
		private final File positionsFile;
		private final String format;
		private final List<SnpPosition> positions;		

				public Reading(AbstractQueue<SnpPosition> q, Thread mainThread,
				 File positionsFile, String format, List<SnpPosition> positions) {
			this.queue = q;
			this.mainThread = mainThread;
			this.positionsFile = positionsFile;
			this.format = format;
			this.positions = positions;
		}

		@Override
		public void run() {
			logger.info("Starting to read positions file: " + positionsFile.getAbsolutePath());
			long count = 0;
			boolean outputFormat2 = options.getOutputFormat() == 2;

			try (BufferedReader reader = new BufferedReader(new FileReader(positionsFile))) {

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

						if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {        					
							p.setAltBases(QBasePileupUtil.getCompoundAltBases(values, mutationColumn));
						}

						if (outputFormat2) {
							p.setAltBases(values[5].getBytes());
						}

						p.retrieveReferenceBases(options.getReference()); 

						positions.add(p);
						queue.add(p);
					}        			

				}

				logger.info("Completed reading thread, read " + count
						+ " records from input: " + positionsFile.getAbsolutePath());
			} catch (Exception e) {
				logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QBasePileupUtil.getStackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
			}

		}
	}

	private class Pileup implements Runnable {

		private final AbstractQueue<SnpPosition> queueIn;
		private final AbstractQueue<String> queueOut;
		private final Thread mainThread;
		private final CountDownLatch pileupLatch;
		private final CountDownLatch writeLatch;
		private final List<InputBAM> currentInputs;
		private final String file = null;
		private QueryExecutor exec = null;

		public Pileup(AbstractQueue<SnpPosition> queueIn,
				AbstractQueue<String> queueOut, Thread mainThread,
				CountDownLatch pileupLatch,
				CountDownLatch wGoodLatch, List<InputBAM> inputs) {
			this.queueIn = queueIn;
			this.queueOut = queueOut;
			this.mainThread = mainThread;
			this.pileupLatch = pileupLatch;
			this.writeLatch = wGoodLatch;
			this.currentInputs = new ArrayList<>();
			currentInputs.addAll(inputs);
		}

		@Override
		public void run() {

			int sleepcount = 0;
			int count = 0;
			int countOutputSleep = 0;
			boolean run = true;
			
			if (options.getFilterQuery() != null) {
				try {
					this.exec  = new QueryExecutor(options.getFilterQuery());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {

				SnpPosition position;

				Map<InputBAM, SamReader> samReaderCache = new HashMap<>();
				while (run) {
					position = queueIn.poll();	                    

					if (position == null) {

						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.info(Thread.currentThread().getName() + " "
									+ e);
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
									queueOut.add(pileup + "\n");
								}                 			
							}
							if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
								sb.append(pileup.toCompoundString()).append("\n");
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
											+ QBasePileupUtil.getStackTrace(e) + " (queue size full) ");
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
				logger.error("Setting exit status in pileup thread to 1 as exception caught file: " + file + " " + QBasePileupUtil.getStackTrace(e));
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
		final static char TAB = '\t';

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
			try {
				String record;
				int count = 0;
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile))) {
					Map<String, Map<String, String>> inputMap = new HashMap<>();
					
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
										+ QBasePileupUtil.getStackTrace(e));
							}
							if (filterLatch.getCount() == 0 && queue.size() == 0) {
								run = false;
							}

							if ((count % checkPoint == 0) && ( ! mainThread.isAlive())) {
								writer.close();
								throw new Exception("Writing threads failed since parent thread died.");
							}

						} else {
							if (options.getOutputFormat() == 2) {
								String[] vals = record.split("\t");
								String position = Arrays.stream(vals, 0, 6).collect(Collectors.joining(Constants.TAB_STRING));
								String info = Arrays.stream(vals, 7, vals.length).collect(Collectors.joining(Constants.TAB_STRING));
								String bam = vals[6];
								
								inputMap.computeIfAbsent(position, f ->  new HashMap<>()).put(bam,  info);
							} else {
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
								writer.write(sb + "\n");
							}
						} else {
							logger.warn("The following entries in the positions file did not have any results - incorrect reference bases?\n" + positionsMissingInMap);
							throw new QBasePileupException("POSITION_FILE_ERROR");
						}
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
						sb.append(line).append("\tIndel_Check\n");
					} else {
						sb.append(line).append("\n");
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
