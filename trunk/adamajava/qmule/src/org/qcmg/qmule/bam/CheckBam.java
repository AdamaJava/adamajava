package org.qcmg.qmule.bam;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qmule.GetBamRecords;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;


public class CheckBam {
	
	private final static String UNMAPPED_READS = "Unmapped";
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private File bamFIle;
	private int numberOfThreads = 1;
	
	private int exitStatus;
	private static QLogger logger;
	
	private final AtomicLong counter = new AtomicLong();
	
	
	public int engage() throws Exception {
		
		logger.info("Get reference contigs from bam header");
		bamFIle = new File(cmdLineInputFiles[0]);
		
		final AbstractQueue<String> sequences = new ConcurrentLinkedQueue<String>();
		
		try (SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
			if ( ! reader.hasIndex() && numberOfThreads > 1) {
				logger.warn("Using 1 producer thread - no index found for bam file: " + bamFIle.getAbsolutePath());
				numberOfThreads = 1;
			}
			
			SAMFileHeader header = reader.getFileHeader();
			List<SAMSequenceRecord> samSequences = header.getSequenceDictionary().getSequences();
			List<SAMSequenceRecord> orderedSamSequences = new ArrayList<SAMSequenceRecord>();
			orderedSamSequences.addAll(samSequences);
			Collections.sort(orderedSamSequences, new Comparator<SAMSequenceRecord>(){
				@Override
				public int compare(SAMSequenceRecord o1, SAMSequenceRecord o2) {
					return o2.getSequenceLength() - o1.getSequenceLength();
				}
			});
			// add the unmapped reads marker
			sequences.add(UNMAPPED_READS);
			
			for (SAMSequenceRecord rec : orderedSamSequences) {
				sequences.add(rec.getSequenceName());
			}
		}
		
		
		logger.info("will create " + numberOfThreads + " threads");

		final CountDownLatch pLatch = new CountDownLatch(numberOfThreads);
//		 setpup and kick-off single Producer thread
		ExecutorService producerThreads = Executors.newFixedThreadPool(numberOfThreads);
		if (1 == numberOfThreads) {
			producerThreads.execute(new SingleProducer(Thread.currentThread(), pLatch));
		} else {
			for (int i = 0 ; i < numberOfThreads ; i++) {
				producerThreads.execute(new Producer(Thread.currentThread(), pLatch,  sequences));
			}
		}

		// don't allow any new threads to start
		producerThreads.shutdown();
		
			logger.info("waiting for Producer thread to finish (max wait will be 20 hours)");
			if ( ! pLatch.await(20, TimeUnit.HOURS)) {
				// we've hit the 20 hour limit - shutdown the threads and throw an exception
				producerThreads.shutdownNow();
				throw new Exception("Producer thread has timed out");
			}
			logger.info("Producer thread finished, counter size: " + counter.longValue());
			
		
//		logger.info("SUMMARY: ");
//		logger.info("Total no of records: " + records.size() );
//		logger.info("No of records with a base at position: " + readsWithBaseAtPosition);
//		logger.info("No of duplicate records (that have a base at position): " + duplicateCount);
//		logger.info("No of unique records (that have a base at position): " + (readsWithBaseAtPosition-duplicateCount));
//		logger.info("No of unique paired records (that have a base at position): " + paired);
//		logger.info("No of unique properly paired records (that have a base at position): " + properlyPaired);
//		logger.info("No of records not primary aligned (that have a base at position): " + notPrimaryAlignment);
//		logger.info("No of records not mapped (that have a base at position): " + unmapped);
//		logger.info("unmappedSecondaryDuplicates (that have a base at position): " + unmappedSecondaryDuplicates);
//		logger.info("unmappedSecondaryDuplicatesProperly (that have a base at position): " + unmappedSecondaryDuplicatesProperly);
//		logger.info("No of paired records (all): " + pairedAll);
//		logger.info("No of properly paired records (all): " + properlyPairedAll);
//		logger.info("Unique record bases: " + baseString.substring(0,baseString.length() > 0 ? baseString.length() : 0));
//		logger.info("Unique record base qualities: " + qualityString.substring(0,qualityString.length() > 0 ? qualityString.length() : 0));
//		logger.info("Unique record base qualities (phred): " + qualityPhredString.substring(0,qualityPhredString.length() > 0 ? qualityPhredString.length() : 0));
//		logger.info("filtered read count: " + filteredCount + " out of " + records.size() );
//		logger.info("Novel start bases: " + new String(novelStartBases));
		
		
		return exitStatus;
	}
	
	
	
	public class Producer implements Runnable {
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		Producer(Thread mainThread, CountDownLatch pLatch, AbstractQueue<String> sequences) {
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.sequences = sequences;
		}

		@Override
		public void run() {
			log.debug("Start Producer ");
			
			long count = 0;
			
			try (SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
			
				while (true) {
					String sequence = sequences.poll();
					if (null == sequence) break;
					SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : reader.query(sequence, 0, 0, false) ;
					log.info("retrieving records for sequence: " + sequence);
					while (iter.hasNext()) {
						iter.next();
						if (++count % 2000000 == 0) {
							log.info("added " + count/1000000 + "M");
						}
					}
					iter.close();
				}
				
			} catch (Exception e) {
				log.error(Thread.currentThread().getName() + " " + e.getMessage(), e);
				mainThread.interrupt();
			} finally {
					pLatch.countDown();
			}
			// update the shared counter
			counter.addAndGet(count);
		}
	}
	
	public class SingleProducer implements Runnable {
		private final Thread mainThread;
		private final QLogger log = QLoggerFactory.getLogger(SingleProducer.class);
		private final CountDownLatch pLatch;
		
		SingleProducer(Thread mainThread, CountDownLatch pLatch) {
			this.mainThread = mainThread;
			this.pLatch = pLatch;
		}
		
		@Override
		public void run() {
			log.debug("Start Producer ");
			
			long count = 0;
			
			try (SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
				
				for (SAMRecord r : reader) {
					if (++count % 2000000 == 0) {
						log.info("added " + count/1000000 + "M");
					}
				}
				
			} catch (Exception e) {
				log.error(Thread.currentThread().getName() + " " + e.getMessage(), e);
				mainThread.interrupt();
			} finally {
				pLatch.countDown();
			}
			// update the shared counter
			counter.addAndGet(count);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception {
		CheckBam sp = new CheckBam();
		int exitStatus = sp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = -1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(GetBamRecords.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("GetBamRecords", GetBamRecords.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			int nt = options.getNumberOfThreads();
			if (nt > 0) {
				numberOfThreads = nt;
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			return engage();
		}
		return returnStatus;
	}

}
