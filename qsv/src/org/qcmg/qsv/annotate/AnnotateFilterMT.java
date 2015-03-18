/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.Messages;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.softclip.SoftClipStaticMethods;
import org.qcmg.qsv.util.CustomThreadPoolExecutor;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class to annotate discordant read pairs and filter 
 * for high quality discordant pairs, clips and unmapped reads 
 *
 */
public class AnnotateFilterMT implements Runnable {
	
	private static final int noOfThreads = 3;
	private static final int maxRecords = 500000;
	private static final int checkPoint = 100000;

	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final int sleepUnit = 10;
	private final File input;
	private final File output;
	private final String query;
	private final QSVParameters parameters;
	private final CountDownLatch mainLatch;
	private final AtomicLong badRecordCount;
	private final AtomicLong goodPairRecordCount = new AtomicLong(); 
	private final AtomicInteger exitStatus;
	private final File clippedFile;
	private final SAMFileHeader queryNameHeader;
	private final SAMFileHeader coordinateHeader;
	private final String softClipDir;
	private final String clipQuery;
	private boolean runPair;
	private boolean runClip;
	private final boolean isSplitRead;
	private final Collection<String> readGroupIds;
	private final AtomicLong goodClipCount = new AtomicLong(); 
	private final AtomicLong unmappedCount = new AtomicLong();
	private final boolean translocationsOnly; 



	public AnnotateFilterMT(Thread mainThread, CountDownLatch countDownLatch, QSVParameters parameters, AtomicInteger exitStatus, String softclipDir, Options options) {
		this.softClipDir = softclipDir;
		this.query = options.getPairQuery();
		this.clipQuery = options.getClipQuery();
		logger.info("Mapper: " + parameters.getMapper());   
		logger.info("Type of Annotation: " + parameters.getPairingType());
		logger.info("QBamFilter Query to use is: " + this.query);

		this.parameters = parameters;
		this.input = parameters.getInputBamFile();
		this.output = parameters.getFilteredBamFile();
		this.queryNameHeader = parameters.getHeader().clone();
		this.queryNameHeader.setSortOrder(SortOrder.queryname);
		this.coordinateHeader = parameters.getHeader().clone();
		this.coordinateHeader.setSortOrder(SortOrder.coordinate);     
		this.readGroupIds = parameters.getReadGroupIds();
		this.translocationsOnly = options.getIncludeTranslocations();         
		this.mainLatch = countDownLatch;
		this.badRecordCount = new AtomicLong();
		this.exitStatus = exitStatus;
		this.clippedFile = parameters.getClippedBamFile();
		this.isSplitRead = options.isSplitRead();

		if (options.getPreprocessMode().equals("both")) {
			this.runClip = true;
			this.runPair = true;
		} else if (options.getPreprocessMode().equals("clip")) {
			this.runClip = true;
			this.runPair = false;
		} else if (options.getPreprocessMode().equals("pair")) {
			this.runClip = false;
			this.runPair = true;
		} else {
			this.runClip = false;
			this.runPair = false;
		}
	}

	/**
	 * Class entry point. Sets up read, write and annotating/filtering threads
	 */
	@Override
	public void run() {

		// create queue to get the chromosomes to reads
		final AbstractQueue<List<Chromosome>> readQueue = new ConcurrentLinkedQueue<List<Chromosome>>();

		// create queue to store the satisfied BAM records for discordant pairs
		final AbstractQueue<SAMRecord> writeQueue = new ConcurrentLinkedQueue<SAMRecord>();

		// create queue to store the satisfied BAM records for clips and unmapped reads
		final AbstractQueue<SAMRecord> writeClipQueue = new ConcurrentLinkedQueue<SAMRecord>();

		final CountDownLatch readLatch = new CountDownLatch(1); // reading thread
		final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // annotating filtering threads
		final CountDownLatch writeLatch = new CountDownLatch(2); // writing thread for satisfied records

		// set up executor services
		final ExecutorService readThread = new CustomThreadPoolExecutor(1, exitStatus, logger);
		final ExecutorService filterThreads = new CustomThreadPoolExecutor(noOfThreads, exitStatus, logger);
		final ExecutorService writeThreads = new CustomThreadPoolExecutor(2, exitStatus, logger);

		try {

			// kick-off single reading thread
			readThread.execute(new Reading(readQueue, Thread.currentThread(), readLatch, filterLatch));
			readThread.shutdown();

			// kick-off filtering thread
			for (int i = 0; i < noOfThreads; i++) {
				filterThreads.execute(new AnnotationFiltering(readQueue,
						writeQueue, writeClipQueue, Thread.currentThread(), readLatch,
						filterLatch, writeLatch));
			}
			filterThreads.shutdown();

			//paired reads
			writeThreads.execute(new Writing(writeQueue, output, Thread.currentThread(), filterLatch, writeLatch, queryNameHeader));

			//soft clips                
			writeThreads.execute(new Writing(writeClipQueue, clippedFile, Thread.currentThread(), filterLatch, writeLatch, coordinateHeader));
			writeThreads.shutdown();

			logger.info("waiting for  threads to finish (max wait will be 20 hours)");
			readThread.awaitTermination(60, TimeUnit.HOURS);
			filterThreads.awaitTermination(60, TimeUnit.HOURS);
			writeThreads.awaitTermination(60, TimeUnit.HOURS);

			if (readQueue.size() != 0 || writeQueue.size() != 0 || writeClipQueue.size() != 0) {
				throw new Exception(
						" threads have completed but queue isn't empty  (readQueue, writeQueue, writeClipQueue ):  "
								+ readQueue.size() + ", " + writeQueue.size() +  "," + writeClipQueue.size());
			}
			logger.info("All threads finished");

			if (runPair && goodPairRecordCount.longValue() == 0) {
				throw new QSVException("LOW_FILTER_COUNT", "discordant pair");
			}
			if (runClip && goodClipCount.longValue() == 0) {
				throw new QSVException("LOW_FILTER_COUNT", "clip");
			}
			logger.info("Finished filtering file: " + input.getAbsolutePath() + " .Discordant Pair records: " + 
					goodPairRecordCount.longValue() + "; Soft clip records: " + goodClipCount.longValue() + "; Unmapped records: " + unmappedCount.intValue() );

		} catch (final Exception e) {

			logger.error(QSVUtil.getStrackTrace(e));
			if (exitStatus.intValue() == 0) {
				exitStatus.incrementAndGet();
			}
		} finally {
			// kill off any remaining threads            	
			readThread.shutdownNow();
			writeThreads.shutdownNow();
			filterThreads.shutdownNow();
			mainLatch.countDown();
		}
	}

	public AtomicLong getClipCount() {
		return goodClipCount;
	}

	/**
	 * Class to determine the chromosomes/contigs to read/write
	 * @author felicity
	 *
	 */
	private class Reading implements Runnable {

		private final AbstractQueue<List<Chromosome>> queue;
		private final Thread mainThread;
		private final CountDownLatch rLatch;
		private final CountDownLatch fLatch;

		public Reading(AbstractQueue<List<Chromosome>> q, Thread mainThread,
				CountDownLatch readLatch, CountDownLatch filterLatch) {
			this.queue = q;
			this.mainThread = mainThread;
			this.rLatch = readLatch;
			this.fLatch = filterLatch;
		}

		@Override
		public void run() {
			logger.info("Starting to read input: " + input.getAbsolutePath());
			int countSleep = 0;
			long count = 0;
			try {
				final Map<String, List<Chromosome>> chromosomes = parameters.getChromosomes();

				for (final Entry<String, List<Chromosome>> entry: chromosomes.entrySet()) {
					logger.info("Adding chromosome to queue: " + entry.getKey() + " in sample " + parameters.getFindType());
					queue.add(entry.getValue());

					if (fLatch.getCount() == 0) {
						throw new Exception(
								"No filtering threads left, but reading from input is not yet completed");
					}

					if (++count % checkPoint == 1) {
						while (queue.size() >= maxRecords) {
							try {
								Thread.sleep(sleepUnit);
								countSleep++;
							} catch (final Exception e) {
								logger.info(Thread.currentThread().getName()
										+ " " +QSVUtil.getStrackTrace(e));
							}
						}
					}

					if (count % 50000000 == 0) {
						logger.debug("Read " + count + " records from input: "
								+ parameters.getFindType());
					}

				}

				logger.info("Completed reading thread, read " + count
						+ " records from input: " + input.getAbsolutePath());
			} catch (final Exception e) {
				logger.error("Setting exit status in execute thread to 1 as exception caught in reading method: " + QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				rLatch.countDown();
				logger.info(String
						.format("Exit Reading thread, total slept %d times * %d milli-seconds, "
								+ "since input queue are full.fLatch  is %d; queus size is %d ",
								countSleep, sleepUnit, fLatch.getCount(),
								queue.size()));
			}

		}
	}

	/**
	 * Class to annotate and filter discordant pairs, clips and unmapped readsS
	 *
	 */
	class AnnotationFiltering implements Runnable {

		private final AbstractQueue<List<Chromosome>> queueIn;
		private final AbstractQueue<SAMRecord> queueOutPair;
		private final Thread mainThread;
		private final CountDownLatch readLatch;
		private final CountDownLatch filterLatch;
		private final CountDownLatch writeLatch;
		private int countOutputSleep;
		private final AbstractQueue<SAMRecord> queueOutClip;
		private CountDownLatch clipLatch;
		QueryExecutor pairQueryEx ;
		QueryExecutor lifescopeQueryEx;		
		QueryExecutor clipQueryEx;


		public AnnotationFiltering(AbstractQueue<List<Chromosome>> readQueue,
				AbstractQueue<SAMRecord> writeQueue, AbstractQueue<SAMRecord> writeClipQueue, Thread mainThread,
				CountDownLatch readLatch, CountDownLatch fLatch,
				CountDownLatch wGoodLatch) throws Exception {
			this.queueIn = readQueue;
			this.queueOutPair = writeQueue;
			this.queueOutClip = writeClipQueue;
			this.mainThread = mainThread;
			this.readLatch = readLatch;
			this.filterLatch = fLatch;
			this.writeLatch = wGoodLatch;
			if (runPair && ! StringUtils.isNullOrEmpty(query)) {
				pairQueryEx = new QueryExecutor(query);
			}
			 if (runClip && ! StringUtils.isNullOrEmpty(clipQuery)) {
				 clipQueryEx = new QueryExecutor(clipQuery);
			 }
			if (parameters.getPairingType().equals("lmp") && parameters.getMapper().equals("bioscope")) {
				lifescopeQueryEx = new QueryExecutor("and(Cigar_M > 35, MD_mismatch < 3, MAPQ > 0, flag_DuplicateRead == false)");
			}
		}

		@Override
		public void run() {

			int sleepcount = 0;
			countOutputSleep = 0;
			boolean run = true;

			try {

				List<Chromosome> chromosomes = null;
				while (run) {
					chromosomes = queueIn.poll();               

					if (chromosomes == null) {
						// must check whether reading thread finished first.
						if (readLatch.getCount() == 0) {
							run = false;
						}

						// qIn maybe filled again during sleep, so sleep should
						// be secondly
						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (final InterruptedException e) {
							logger.info(Thread.currentThread().getName() + " "
									+ e.toString());
						}

					} else {
						// pass the record to filter and then to output queue                    	
						for (final Chromosome chromosome: chromosomes) {
							logger.info("Reading records from chromosome: " + chromosome.getName() + " in sample " + parameters.getFindType());

							BufferedWriter writer = null;

							//set up writer to write clips to txt file
							if (runClip) {
								writer = new BufferedWriter(new FileWriter(new File(SoftClipStaticMethods.getSoftClipFile(chromosome.getName(), parameters.getFindType(), softClipDir)), true));
							}

							//write reads
							run = readAndWriteData(writer, chromosome, chromosome.getStartPosition(), chromosome.getEndPosition());	

							if (runClip) {
								writer.close();
							}
						}
					} // end else
				}// end while
				logger.info("Completed filtering thread: " + Thread.currentThread().getName());

			} catch (final Exception e) {
				logger.error("Setting exit status in annotation thread to 1 as exception caught: " + QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {

				logger.debug(String
						.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d, %d",
								sleepcount, countOutputSleep, sleepUnit,
								queueIn.size(), queueOutPair.size()));
				filterLatch.countDown();
			}
		}

		private boolean readAndWriteData(BufferedWriter writer, Chromosome chromosome, int startPos, int endPos) throws Exception {        	

			boolean result = true;
			
			try ( SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(input, "silent");) {
				int queryStart = startPos - 100;
				if (queryStart <= 0) {
					queryStart = 1;
				}
				final int queryEnd = endPos + 100;
				//get reads from bam
				final SAMRecordIterator iter = reader.queryOverlapping(chromosome.getName(), queryStart, queryEnd);
	
				int count = 0;
	
				while (iter.hasNext()) {
					final SAMRecord record = iter.next();
					count++;
					boolean writersResult = true;
					boolean pileupResult = true;
	
					final SAMReadGroupRecord srgr = record.getReadGroup();
					if (null != srgr 
							&& null != srgr.getId() 
							&& readGroupIds.contains(srgr.getId())) {
	
						//discordant pairs
						if (runPair && record.getAlignmentStart() >= startPos && record.getAlignmentStart() <= endPos) {
							writersResult = addToPairWriter(record, count);
						}
	
						if (runClip) {
							//soft clips
							if (( ! record.getReadUnmappedFlag() && ! record.getDuplicateReadFlag() && record.getCigarString().contains("S"))
									|| (isSplitRead && record.getReadUnmappedFlag()) ) {
								
								pileupResult = pileupSoftClips(writer, record, startPos, endPos, chromosome);
							}
						}
	
						//check to make sure there aren't any errors
						if ( ! pileupResult || ! writersResult) {
							if ( ! pileupResult) {	            		
								logger.error("Error finding soft clip records in " + chromosome.toString());
							}
							if ( ! writersResult) {
								logger.error("Error finding discordant read records in " + chromosome.toString());
							}
	
							if (exitStatus.intValue() == 0) {
								exitStatus.incrementAndGet();
							}
							result =  false;
	
						}
					} else {
						logger.warn("SAMReadGroupRecord : " + srgr + ":" + record.getSAMString());
						logger.warn("SAMReadGroupRecord was null, or id was not in collection: " + srgr.getId() + ":" + record.getSAMString());
						throw new Exception("Null SAMReadGroupRecord");
					}
				}
			}

			return result; 
		}        

		/*
		 * write soft clips and unmapped reads
		 */
		private boolean pileupSoftClips(BufferedWriter writer, SAMRecord record, int start, int end, Chromosome chromosome) throws Exception {
			if (record.getReadUnmappedFlag()) {
				unmappedCount.incrementAndGet();
				QSVUtil.writeUnmappedRecord(writer, record, start, end, parameters.isTumor());						
//				QSVUtil.writeUnmappedRecord(writer, record, start, end, chromosome.getName(), parameters.getFindType(), softClipDir, parameters.isTumor());						
				return add2queue(record, queueOutClip, start, clipLatch);
			}


			//see if clips pass the filter
			if (clipQueryEx.Execute(record)) {
				goodClipCount.incrementAndGet();
				SoftClipStaticMethods.writeSoftClipRecord(writer, record, start, end, chromosome.getName());    			
				return add2queue(record, queueOutClip, start, clipLatch);
			} else {
				return true;
			}			
		}

		/*
		 * Write discordant pairs to bam
		 */
		private boolean addToPairWriter(SAMRecord record, int count) throws Exception {

			if ( ! record.getReadUnmappedFlag()) {

				if ((translocationsOnly && QSVUtil.isTranslocationPair(record)) || !translocationsOnly) {

					//annotate the read
					parameters.getAnnotator().annotate(record);
					final String zp = (String) record.getAttribute(QSVConstants.ZP_SHORT);

					//make sure it is discordant
					if (zp.contains("A") || zp.equals("C**") || zp.contains("B")) {
						if ( ! zp.equals(QSVConstants.AAA)) {
							//check if it passes the filter
							if (pairQueryEx.Execute(record)) {
								goodPairRecordCount.incrementAndGet();                 		    
								return add2queue(record, queueOutPair, count, writeLatch);			                                
							} else if (lifescopeQueryEx != null && record.getAttribute("SM") == null && record.getAttribute("XC") != null) {
								//try to filter lifescope reads
								if (lifescopeQueryEx.Execute(record)) {
									goodPairRecordCount.incrementAndGet();
									return add2queue(record, queueOutPair, count, writeLatch);
								} else {
									record.setAttribute(QSVConstants.ZP, record.getAttribute("XC"));
									if (lifescopeQueryEx.Execute(record)) {
										return add2queue(record, queueOutPair, count, writeLatch);
									}
								} 
								badRecordCount.incrementAndGet();
							} else {
								badRecordCount.incrementAndGet();
							}
						}
					} 	               	
				}
				return true;

			} else {
				return true;
			}			
		}

		//add to write queue
		private boolean add2queue(SAMRecord re, AbstractQueue<SAMRecord> queue,
				int count, CountDownLatch latch) {
			// check queue size in each checkPoint number
			if (count % checkPoint == 0) {
				// check mainThread
				if (!mainThread.isAlive()) {
					logger.error("mainThread died: " + mainThread.getName());
					return false;
				}
				// check queue size
				while (queue.size() >= maxRecords) {
					try {
						Thread.sleep(sleepUnit);
						countOutputSleep++;
					} catch (final InterruptedException e) {
						logger.info(Thread.currentThread().getName() + " "
								+ QSVUtil.getStrackTrace(e) + " (queue size full) ");
					}
					if (latch.getCount() == 0) {
						logger.error("output queue is not empty but writing thread are completed");
						return false;
					}
				} // end while
			}

			queue.add(re);

			return true;
		}
	}

	/**
	 * Class to write discordant pairs, clips and unmapped reads to file
	 */
	private class Writing implements Runnable {
		private final File file;
		private final AbstractQueue<SAMRecord> queue;
		private final Thread mainThread;
		private final CountDownLatch filterLatch;
		private final CountDownLatch writeLatch;
		private final SAMFileHeader header;

		public Writing(AbstractQueue<SAMRecord> q, File f, Thread mainThread,
				CountDownLatch fLatch, CountDownLatch wLatch,SAMFileHeader header) {
			queue = q;
			file = f;
			this.mainThread = mainThread;
			this.filterLatch = fLatch;
			this.writeLatch = wLatch;
			this.header = header;
		}

		@Override
		public void run() {        	
			int countSleep = 0;
			boolean run = true;
			try {
				int count = 0;
				if (file != null) {
					final String commandLine = query;
					HeaderUtils.addProgramRecord(header, "qsv",
							Messages.getVersionMessage(), commandLine);
					final SAMFileWriterFactory writeFactory = new SAMFileWriterFactory();
					if (header.getSortOrder().equals(SortOrder.coordinate)) {
						writeFactory.setCreateIndex(true);
					}
					// there is a bug at current version, cause an exception but we
					// can use below static method
					net.sf.samtools.SAMFileWriterImpl.setDefaultMaxRecordsInRam(maxRecords);

					final SAMFileWriter writer = writeFactory.makeBAMWriter(header, false, file, 1);

					// debug
					final File tmpLocation = net.sf.samtools.util.IOUtil.getDefaultTmpDir();
					logger.info("all tmp BAMs are located on " + tmpLocation.getCanonicalPath());
					logger.info("default maxRecordsInRam " + net.sf.samtools.SAMFileWriterImpl.getDefaultMaxRecordsInRam());

					SAMRecord record;
					try {
						while (run) {
							// when queue is empty,maybe filtering is done
							if ((record = queue.poll()) == null) {
	
								try {
									Thread.sleep(sleepUnit);
									countSleep++;
								} catch (final Exception e) {
									logger.info(Thread.currentThread().getName() + " "
											+ QSVUtil.getStrackTrace(e));
								}
	
								if ((count % checkPoint == 0)
										&& (!mainThread.isAlive()))
									throw new Exception(
											"Writing thread failed since parent thread died.");
	
								while (queue.size() >= maxRecords) {
									try {
										Thread.sleep(sleepUnit);
										countSleep++;
									} catch (final Exception e) {
										logger.info(Thread.currentThread().getName()
												+ " " + QSVUtil.getStrackTrace(e));
									}
								}
	
								if (filterLatch.getCount() == 0) {	                        	
									run = false;
								}
	
							} else {
	
								writer.addAlignment(record);
								count++;
							}
						}
					} catch (Exception e) {
						logger.error("Exception caught whilst writing SAMRecords to bam file: " + file.getAbsolutePath() + " : " + QSVUtil.getStrackTrace(e));
						throw e;
					} finally {
						writer.close();
					}
				}
				if (!mainThread.isAlive()) {
					throw new Exception(
							"Writing thread failed since parent thread died.");
				} else {
					if (file != null) {
						logger.info("Completed writing thread, added " + count
								+ " records to the output: "
								+ file.getAbsolutePath());
						logger.info("Record count not passing filter for the "
								+ parameters.getFindType() + " sample is "
								+ badRecordCount.longValue());
					}
				}
			} catch (final Exception e) {
				logger.error("Setting exit status to 1 as exception caught in writing thread: " + QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				writeLatch.countDown();
				logger.info("Exit Writing thread, total " + countSleep
						+ " times get null from writing queue.");
			}
		}
	}
}

