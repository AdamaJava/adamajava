/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupConstants;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class SnpBasePileupByFileMT {

	private static QLogger logger = QLoggerFactory.getLogger(SnpBasePileupByFileMT.class);
	private final Options options;
	private final AtomicInteger totalExamined = new AtomicInteger();
	private final AtomicInteger totalPassedFilters =new AtomicInteger();
	private final AtomicInteger totalReadsNotMapped = new AtomicInteger();
	private final AtomicInteger totalReadsBadBaseQual = new AtomicInteger();
	private final AtomicInteger totalReadsBaseMapQual = new AtomicInteger();
	private final AtomicInteger positionCount = new AtomicInteger();
	private final AtomicInteger uniquePositionCount = new AtomicInteger();
	private int threadNo = 0;
	AtomicInteger exitStatus = new AtomicInteger();
	final int sleepUnit = 20;
	final int maxRecords = 100000;
	final int checkPoint = 10000;
	private Map<String, List<SnpPosition>> snpPositions;
	private List<String> headerLines = new ArrayList<String>();

	public SnpBasePileupByFileMT(Options options) throws Exception {		
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

		final AbstractQueue<InputBAM> readQueue = new ConcurrentLinkedQueue<InputBAM>();    

		final AbstractQueue<String> writeQueue = new ConcurrentLinkedQueue<String>();    

		final CountDownLatch pileupLatch = new CountDownLatch(threadNo); // filtering thread
		final CountDownLatch writeLatch = new CountDownLatch(1); // writing thread for satisfied records

		// set up executor services

		ExecutorService pileupThreads = Executors
				.newFixedThreadPool(threadNo);
		ExecutorService writeThread = Executors.newSingleThreadExecutor();

		try {
			logger.info("Getting snp positions...");
			this.snpPositions = loadPositionsFile(options.getPositionsFile(), options.getFormat());
			logger.info("Finished getting " +snpPositions.size()+ " snp positions...");
			for (InputBAM bam: options.getInputBAMs()) {
				readQueue.add(bam);
			}

			// kick-off pileup threads
			for (int i = 0; i < threadNo; i++) {
				pileupThreads.execute(new Pileup(readQueue,
						writeQueue, Thread.currentThread(), pileupLatch, writeLatch));
			}

			pileupThreads.shutdown();

			// kick-off single writing thread to output the satisfied Records
			writeThread.execute(new Writing(writeQueue, options.getOutput(), Thread.currentThread(), pileupLatch, writeLatch));
			writeThread.shutdown();

			logger.info("waiting for  threads to finish (max wait will be 60 hours)");

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

	private Map<String, List<SnpPosition>> loadPositionsFile(File positionsFile, String format) throws Exception {
		Map<String, List<SnpPosition>> snpPositions = new HashMap<String, List<SnpPosition>>();

		IndexedFastaSequenceFile indexedFastaFile = QBasePileupUtil.getIndexedFastaFile(options.getReference());
		FastaSequenceIndex index = QBasePileupUtil.getFastaIndex(options.getReference());
		int count = 0;
		String line;
		int mutationColumn = -1;
		try (BufferedReader reader = new BufferedReader(new FileReader(positionsFile));) {
	
			while ((line=reader.readLine()) != null) {
				if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE) && line.startsWith("analysis_id")) {
					mutationColumn = QBasePileupUtil.getMutationColumn(line);
				}
				if (!line.startsWith("#") && !line.startsWith("analysis_id") && !line.startsWith("Hugo") &&  !line.startsWith("mutation")) {
					count++;
	
					positionCount.incrementAndGet();
					SnpPosition p = null;
					String[] values = line.split("\t");			     				
	
					String[] columns = QBasePileupUtil.getSNPPositionColumns(format, values, count);
	
					p = new SnpPosition(columns[0],columns[1],new Integer(columns[2]),new Integer(columns[3]), line);
	
					if (options.getOutputFormat() == 2) {
						p.setAltBases(values[5].getBytes());
					} else if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
						p.setAltBases(QBasePileupUtil.getCompoundAltBases(values, mutationColumn));
					}
	
					if (count % 100000 == 0) {
						logger.info("Loaded " + count + " positions");
					}
					p.retrieveReferenceBases(options.getReference());	
//					p.retrieveReferenceBases(indexedFastaFile, index);	
					String fullChromosome = QBasePileupUtil.getFullChromosome(p.getChromosome());
					List<SnpPosition> list = new ArrayList<SnpPosition>();
					if (snpPositions.containsKey(fullChromosome)) {
						list = snpPositions.get(fullChromosome);
					} 
					list.add(p);
					snpPositions.put(fullChromosome, list);
				}
			}
		}
		return snpPositions;
	}

	private class Pileup implements Runnable {

		private final AbstractQueue<InputBAM> queueIn;
		private final AbstractQueue<String> queueOut;
		private final Thread mainThread;
		private final CountDownLatch pileupLatch;
		private final CountDownLatch writeLatch;
		private int countOutputSleep;
		private String file = null;
		private final int added = 0;
		public Pileup(AbstractQueue<InputBAM> queueIn,
				AbstractQueue<String> queueOut, Thread mainThread,
				CountDownLatch pileupLatch, CountDownLatch writeLatch) throws Exception {
			this.queueIn = queueIn;
			this.queueOut = queueOut;
			this.mainThread = mainThread;
			this.pileupLatch = pileupLatch;
			this.writeLatch = writeLatch;
		}

		@Override
		public void run() {

			int sleepcount = 0;
			long count = 0;
			countOutputSleep = 0;
			boolean run = true;

			try {

				InputBAM inputBAM;
				while (run) {
					inputBAM = queueIn.poll();	                    

					if (inputBAM == null) {
						run = false;        
					} else {                  
						file = inputBAM.getBamFile().getAbsolutePath();

						String chromosome = null;
						TreeMap<Integer, List<SnpPositionPileup>> chrMap = null;
						Set<String> fullChromosomeSet = new HashSet<>(snpPositions.keySet());


						SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(file), "silent");

						for (SAMRecord r: reader) {
							//get chromosome length
							if (chromosome == null || ! chromosome.equals(r.getReferenceName())) {
								chromosome = QBasePileupUtil.getFullChromosome(r.getReferenceName());
								fullChromosomeSet.remove(chromosome);
								//write previous map
								if (chrMap != null && chrMap.size() > 0) {

									run = writeMap(chrMap, count);
									chrMap = null;
								}
								//get new map
								if (snpPositions.containsKey(chromosome)) {
									chrMap = getSnpPositions(inputBAM, chromosome);
								}                         			
							}

							count++;  

							if (count % 10000000 == 0) {
								logger.info("Processed " + count + " records in bam: " + file);
							}

							if (chrMap != null && !r.getReadUnmappedFlag()) {
								NavigableMap<Integer, List<SnpPositionPileup>> region = chrMap.subMap(r.getAlignmentStart(), true, r.getAlignmentEnd(), true);

								for (Entry<Integer, List<SnpPositionPileup>> entry : region.entrySet()) {
									List<SnpPositionPileup> snps = entry.getValue();

									for (SnpPositionPileup p: snps) {
										p.addSAMRecord(r);
									}
								}
							}
						}
						reader.close();

						//write final map

						fullChromosomeSet.remove(chromosome);
						if (chrMap != null && chrMap.size() > 0) {  
							run = writeMap(chrMap, count);
							chrMap = null;
						}                 			

						logger.info("Writing positions not in bam file (total: " + fullChromosomeSet.size() + ")");
						//positions where there wasn't anything in the bam
						for (String chr : fullChromosomeSet) {
							if (snpPositions.containsKey(chr)) {
								chrMap = getSnpPositions(inputBAM, chr);
								if (chrMap != null && chrMap.size() > 0) {
									run = writeMap(chrMap, count);
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
				logger.debug(String
						.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d, %d",
								sleepcount, countOutputSleep, sleepUnit,
								queueIn.size(), queueOut.size()));
				pileupLatch.countDown();
			}
		}

		private boolean writeMap(TreeMap<Integer, List<SnpPositionPileup>> chrMap, long count) {	        	
			for (Entry<Integer, List<SnpPositionPileup>> entry: chrMap.entrySet()) {
				List<SnpPositionPileup> list = entry.getValue();
				for (SnpPositionPileup pileup : list) {
					if (count % checkPoint == 0) {

						if (!mainThread.isAlive()) {
							logger.error("mainThread died: " + mainThread.getName());
							return false;
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
								return false;
							}
						} 
					}        
					if (options.getMode().equals("snp")) {
						if (options.getOutputFormat() == 2) {                				
							queueOut.add(pileup.toColumnString());
						} else {
							queueOut.add(pileup.toString() + "\n");
						}                 			
					}
					if (options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
						queueOut.add(pileup.toCompoundString() + "\n");
					}
					if (options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
						queueOut.add(pileup.toMafString() + "\n");
					}
				}
			}
			return true;
		}

		private TreeMap<Integer, List<SnpPositionPileup>> getSnpPositions(InputBAM inputBAM, String chromosome) throws Exception {
			TreeMap<Integer, List<SnpPositionPileup>> snpPileupPositions = new TreeMap<Integer, List<SnpPositionPileup>>();
			QueryExecutor exec = null;				
			if (options.getFilterQuery() != null) {
				exec = new QueryExecutor(options.getFilterQuery());
			}    		
			List<SnpPosition> snpList = snpPositions.get(chromosome);
			for (SnpPosition p : snpList) {
				SnpPositionPileup pileup = new SnpPositionPileup(inputBAM, p, options, exec);	

				List<SnpPositionPileup> pileupList = new ArrayList<SnpPositionPileup>();
				if (snpPileupPositions.containsKey(p.getStart())) {
					pileupList = snpPileupPositions.get(p.getStart());
				}
				pileupList.add(pileup);
				snpPileupPositions.put(p.getStart(), pileupList);    				
			}	
			logger.info("Getting pileups for : " + snpPileupPositions.size() + " positions for chromosome " + chromosome + " in file " + inputBAM.getBamFile());

			return snpPileupPositions;
		}
	}	

	private class Writing implements Runnable {
		private final File resultsFile;
		private final AbstractQueue<String> queue;
		private final Thread mainThread;
		private final CountDownLatch filterLatch;
		private final CountDownLatch writeLatch;
		final static String TAB = "\t";

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
				BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
				Map<String, Map<String, String>> inputMap = new HashMap<String, Map<String, String>>();

				writer.write(getHeader());
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
									+ QBasePileupUtil.getStrackTrace(e));
						}
						if (filterLatch.getCount() == 0 && queue.size() == 0) {
							run = false;
						}

						if ((count % checkPoint == 0) && (!mainThread.isAlive())) {
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
									position += vals[i] + "\t";
								}
								if (i==5) {
									position += vals[i];
								}
								if (i==6) {
									bam += vals[i];
								}
								if (i > 6) {
									if (i == vals.length-1) {
										info += vals[i];
									} else {
										info += vals[i] + "\t";
									}

								}
							}
							Map<String, String> map = new HashMap<String, String>();
							if (inputMap.containsKey(position)) {
								map = inputMap.get(position);
							}

							map.put(bam, info);
							inputMap.put(position, map);
						} else {
							writer.write(record);
						}
						count++;
					}
				}

				if (options.getOutputFormat() == 2) {

					BufferedReader reader = new BufferedReader(new FileReader(options.getPositionsFile()));
					String line; 
					List<InputBAM> inputs = options.getInputBAMs();
					while ((line = reader.readLine()) != null) {
						if (!line.startsWith("#") && !line.startsWith("Hugo")) {
							StringBuilder sb = new StringBuilder();
							Map<String, String> start = inputMap.get(line);
							sb.append(line + "\t");
							for (int i=0; i<inputs.size(); i++) {    
								sb.append(start.get(inputs.get(i).getAbbreviatedBamFileName()));
								if (i != (inputs.size() -1)) {
									sb.append("\t");
								}
							}
							writer.write(sb.toString() + "\n");
						}
					}
					reader.close();
				}

				writer.close();

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
