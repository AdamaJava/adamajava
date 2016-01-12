package org.qcmg.qmule.bam;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

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
import java.util.concurrent.atomic.AtomicLongArray;

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
	
	
	private static final int READ_PAIRED_FLAG = 0x1;
    private static final int PROPER_PAIR_FLAG = 0x2;
    private static final int READ_UNMAPPED_FLAG = 0x4;
    private static final int MATE_UNMAPPED_FLAG = 0x8;
    private static final int READ_STRAND_FLAG = 0x10;
    private static final int MATE_STRAND_FLAG = 0x20;
    private static final int FIRST_OF_PAIR_FLAG = 0x40;
    private static final int SECOND_OF_PAIR_FLAG = 0x80;
    private static final int NOT_PRIMARY_ALIGNMENT_FLAG = 0x100;
    private static final int READ_FAILS_VENDOR_QUALITY_CHECK_FLAG = 0x200;
    private static final int DUPLICATE_READ_FLAG = 0x400;
    private static final int SUPPLEMENTARY_ALIGNMENT_FLAG = 0x800;
	
	
	private int exitStatus;
	private static QLogger logger;
	
	private final AtomicLong counter = new AtomicLong();
	
//	long [] flagCounter = new long[5000];
	AtomicLongArray flags = new AtomicLongArray(5000);
	
	
	public int engage() throws Exception {
		
		logger.info("Get reference contigs from bam header");
		bamFIle = new File(cmdLineInputFiles[0]);
		
		final AbstractQueue<String> sequences = new ConcurrentLinkedQueue<String>();
		
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
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
		// output flag stats too
		long dups = 0;
		long sups = 0;
		long mapped = 0;
		long paired = 0;
		long properPair = 0;
		long r1 = 0;
		long r2 = 0;
		for (int i = 0 ; i < flags.length() ; i++) {
			long l = flags.get(i);
			if (l > 0) {
				
				if ((i & READ_PAIRED_FLAG)  != 0) {
					paired += l;
				}
				if ((i & PROPER_PAIR_FLAG)  != 0) {
					properPair += l;
				}
				if ((i & READ_UNMAPPED_FLAG)  == 0) {
					mapped += l;
				}
				if ((i & FIRST_OF_PAIR_FLAG)  != 0) {
					r1 += l;
				}
				if ((i & SECOND_OF_PAIR_FLAG)  != 0) {
					r2 += l;
				}
				if ((i & DUPLICATE_READ_FLAG)  != 0) {
					dups += l;
				}
				if ((i & SUPPLEMENTARY_ALIGNMENT_FLAG)  != 0) {
					sups += l;
				}
				logger.info("flag: " + i + " : " + l + " hits");
			}
		}
		logger.info("total read count: " + counter.longValue());
		logger.info("dups: " + dups + " (" + (((double) dups / counter.longValue()) * 100) + "%)");
		logger.info("sups: " + sups + " (" + (((double) sups / counter.longValue()) * 100) + "%)");
		logger.info("mapped: " + mapped + " (" + (((double) mapped / counter.longValue()) * 100) + "%)");
		logger.info("paired: " + paired + " (" + (((double) paired / counter.longValue()) * 100) + "%)");
		logger.info("properPair: " + properPair + " (" + (((double)properPair / counter.longValue()) * 100) + "%)");
		logger.info("r1: " + r1 + " (" + (((double) r1 / counter.longValue()) * 100) + "%)");
		logger.info("r2: " + r2 + " (" + (((double) r2 / counter.longValue()) * 100) + "%)");
		
		return exitStatus;
	}
	
	
	
	public class Producer implements Runnable {
		private final Thread mainThread;
		private final CountDownLatch pLatch;
		private final AbstractQueue<String> sequences;
		private final QLogger log = QLoggerFactory.getLogger(Producer.class);
		
		private final long [] flagCounter = new long[5000];
		
		Producer(Thread mainThread, CountDownLatch pLatch, AbstractQueue<String> sequences) {
			this.mainThread = mainThread;
			this.pLatch = pLatch;
			this.sequences = sequences;
		}

		@Override
		public void run() {
			log.debug("Start Producer ");
			
			long count = 0;
			
			try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
			
				while (true) {
					String sequence = sequences.poll();
					if (null == sequence) break;
					SAMRecordIterator iter = UNMAPPED_READS.equals(sequence) ? reader.queryUnmapped() : reader.query(sequence, 0, 0, false) ;
					log.info("retrieving records for sequence: " + sequence);
					while (iter.hasNext()) {
						int flag = iter.next().getFlags();
						flagCounter[flag] ++ ;
						// update count for this flag
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
			//update the flag Counter
			int i = 0 ;
			for (long l : flagCounter) {
				if (l > 0) {
					flags.addAndGet(i, l);
				}
				i++;
			}
		}
	}
	
	public class SingleProducer implements Runnable {
		private final Thread mainThread;
		private final QLogger log = QLoggerFactory.getLogger(SingleProducer.class);
		private final CountDownLatch pLatch;
		private final long [] flagCounter = new long[5000];
		
		SingleProducer(Thread mainThread, CountDownLatch pLatch) {
			this.mainThread = mainThread;
			this.pLatch = pLatch;
		}
		
		@Override
		public void run() {
			log.debug("Start SingleProducer ");
			
			long count = 0;
			
			try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFIle);) {
				
				for (SAMRecord r : reader) {
					int flag = r.getFlags();
					flagCounter[flag] ++ ;
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
			//update the flag Counter
			int i = 0 ;
			for (long l : flagCounter) {
				if (l > 0) {
					flags.addAndGet(i, l);
				}
				i++;
			}
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
			logger.logInitialExecutionStats("CheckBam", CheckBam.class.getPackage().getImplementationVersion(), args);
			
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
