/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.mode;

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

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.Options;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.hdf.MetadataRecordDS;
import org.qcmg.pileup.hdf.MetadataReferenceDS;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.model.Chromosome;

public class MergeMT {

	private PileupHDF hdf;
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private int blockSize;
	private final int sleepUnit = 10;
	private int noOfThreads;
	private List<PileupHDF> inputHDFs;
	private AtomicInteger exitStatus = new AtomicInteger(0);
	private MetadataRecordDS mergedMetaDS;
	private MetadataReferenceDS mergedReferenceDS;
	private long startTime;
	private String mode;
	private boolean isBamOverride = false;
	private Options options;

	public MergeMT(Options options, long startTime, String mode)
			throws Exception {
		this.options = options;
		this.hdf = new PileupHDF(options.getHdfFile(), true, false);
		
		List<String> files = options.getInputHDFFiles();
		inputHDFs = new ArrayList<PileupHDF>();
		for (String f : files) {
			logger.info("HDF to merge: " + f);
			inputHDFs.add(new PileupHDF(f, false, false));
		}
		
		this.startTime = startTime;
		this.mode = mode;
		this.noOfThreads = options.getThreadNo();
		this.blockSize = PileupUtil.getBlockSize(mode, noOfThreads);
		this.isBamOverride = options.isBamOverride();
		this.mergedMetaDS = new MetadataRecordDS(hdf);
		this.mergedReferenceDS = new MetadataReferenceDS(hdf, options.getReferenceFile());
		logger.info("Block size: " + blockSize);
	}

	public int execute() {

		// set up initial metadata characteristics
		final AbstractQueue<Chromosome> readQueue = new ConcurrentLinkedQueue<Chromosome>();

		final CountDownLatch readLatch = new CountDownLatch(1); // reading
																// thread
		final CountDownLatch mergeLatch = new CountDownLatch(noOfThreads); // filtering
																			// thread

		// set up executor services
		ExecutorService readThread = Executors.newSingleThreadExecutor();
		ExecutorService mergeThreads = Executors
				.newFixedThreadPool(noOfThreads);

		try {			
			
			//bootstrap the merged HDF
		    bootstrap();
			
		    hdf.open();

			for (PileupHDF inputHDF: inputHDFs) {
				logger.info("HDF file to merge from: " + inputHDF.getHDFFileName());
				inputHDF.open();
			}
			// get metadata from previous hdfs
		    setupMetadata();
		    
			if (exitStatus.intValue() > 0) {
				throw new QPileupException("MERGE_EXCEPTION");
			}
			
			// kick-off single reading thread
			readThread.execute(new Reader(readQueue, Thread.currentThread(),
					readLatch, mergeLatch));
			readThread.shutdown();

			// kick-off filtering thread
			for (int i = 0; i < noOfThreads; i++) {
				mergeThreads.execute(new Merger(readQueue, Thread
						.currentThread(), readLatch, mergeLatch));
			}
			mergeThreads.shutdown();

			logger.info("waiting for threads to finish (max wait will be 500 hours)");
			readThread.awaitTermination(500, TimeUnit.HOURS);
			mergeThreads.awaitTermination(500, TimeUnit.HOURS);

	        if (readQueue.size() != 0) {
	        	logger.info("Setting exitStatus to 1 as readQueue is not empty.");
	        	exitStatus.incrementAndGet();
	            throw new QPileupException("INCOMPLETE_READ_THREAD", "" + readQueue.size());
	        } else if (exitStatus.intValue() > 0) {
	        	throw new QPileupException("EXIT_STATUS_ERROR", mode, "" + exitStatus.intValue());
	        } else {
	        	logger.info("All threads finished");
	        } 
			

		} catch (Exception e) {
			logger.error("Setting exit status to 1 as error caught in execute thread: " + PileupUtil.getStrackTrace(e));
			exitStatus.incrementAndGet();
		} finally {
			// kill off any remaining threads
			readThread.shutdownNow();
			mergeThreads.shutdownNow();
			if (exitStatus.intValue() == 0) {
				try {
					
					//update the run time
					mergedMetaDS.updateFirstMember(0, PileupConstants.MODE_ADD, PileupUtil.getRunTime(startTime, System.currentTimeMillis()));
					
					hdf.close();
					for (PileupHDF inputHDF: inputHDFs) {
						inputHDF.close();
					}
				} catch (Exception e) {
					logger.error("Setting exit status to 1 as error occurred when updating metadata and closing inputHDFs: " + PileupUtil.getStrackTrace(e));
					exitStatus.incrementAndGet();
					e.printStackTrace();
				}
			}
		}
		logger.info("Number of H5 objects left open: " + H5.getOpenIDCount());
		return exitStatus.intValue();
	}

	private void bootstrap() {
		try {
			BootstrapMT bootstrap = new BootstrapMT(hdf, options, startTime);
			
			int exit = bootstrap.execute();
			mergedReferenceDS.addRecords(bootstrap.getReferenceRecords());
			if (exit > 0) {
				logger.error("Setting exit status to 1 as error occurred in bootstrap");
				exitStatus.incrementAndGet();
			} 
		} catch (Exception e) {
			logger.error("Setting exit status to 1 as exception caught during bootstrap" + PileupUtil.getStrackTrace(e));
			exitStatus.incrementAndGet();
		}
	}

	private void setupMetadata() throws Exception {
		// check bams of each HDF to check overlaps
		
		mergedMetaDS.addDatasetMember(0, mergedMetaDS.getMemberString(PileupUtil.getCurrentDate(), mode, "", "", "00:00:00", ""));
		
		for (PileupHDF h : inputHDFs) {
			
			MetadataRecordDS metaDS = new MetadataRecordDS(h);
			metaDS.instantiate();
			mergedMetaDS.checkHDFMetadata(metaDS.getRecords(), isBamOverride,
							metaDS.getLowReadCount(),
							metaDS.getNonreferenceThreshold(), metaDS.getBamsAdded());
		}

		// write metadata to new HDF
		mergedMetaDS.create();
		mergedReferenceDS.create();
	}

	private class Reader implements Runnable {

		private final AbstractQueue<Chromosome> queue;
		final CountDownLatch rLatch;
		final CountDownLatch fLatch;
		private List<Chromosome> chromosomes;

		public Reader(AbstractQueue<Chromosome> q, Thread currentThread,
				CountDownLatch readLatch, CountDownLatch filterLatch) {
			this.queue = q;
			this.rLatch = readLatch;
			this.fLatch = filterLatch;
		}

		@Override
		public void run() {

			int countSleep = 0;
			try {
				
				chromosomes = hdf.getChromosomeLengths();
								
				addChromosomesToQueue(chromosomes);

				logger.info("Completed reading thread");
			} catch (Exception e) {
				logger.info("Setting exit status to 1 as exception caught in reader thread: " + PileupUtil.getStrackTrace(e));
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

		private void addChromosomesToQueue(List<Chromosome> currentChromosomes)
				throws Exception {

			for (Chromosome chr : currentChromosomes) {
				if (fLatch.getCount() == 0) {
					throw new QPileupException("NO_THREADS", "merge");							
				}

				if (exitStatus.intValue() > 0) {
					throw new QPileupException("ADD_EXCEPTION");
				}

				int length = chr.getSectionLength();

				if (blockSize > length) {
					addToQueue(chr.getName(), chr.getTotalLength(),
							chr.getStartPos(), chr.getEndPos());
				} else {
					int startIndex = chr.getStartPos();
					int finalBlockSize = length - (blockSize * (length / blockSize));
					int finalIndex = -1;
					if (finalBlockSize < 1) {
						finalIndex = length;
					} else {
						finalIndex = startIndex + length - finalBlockSize;
					}

					for (int i = startIndex; i <= finalIndex; i += blockSize) {
						if (i == finalIndex) {
							addToQueue(chr.getName(), chr.getTotalLength(), i,
									i + finalBlockSize - 1);
							break;
						} else {
							addToQueue(chr.getName(), chr.getTotalLength(), i,
									i + blockSize - 1);
						}
					}
				}
			}
		}

		private void addToQueue(String name, int totalLength, int startPos,
				int endPos) throws QPileupException {

			Chromosome chrToRead = new Chromosome(name, totalLength, startPos,
					endPos);
			logger.info("Setting up Chromosome/Contig " + name
					+ " from position " + chrToRead.getStartPos()
					+ " to position " + chrToRead.getEndPos());
			queue.add(chrToRead);
		}
	}

	private class Merger implements Runnable {

		private final AbstractQueue<Chromosome> queueIn;
		private final CountDownLatch readLatch;
		private final CountDownLatch mergeLatch;
		private int countOutputSleep;
		private StrandDS forward = null;
		private StrandDS reverse = null;
		private Chromosome chromosome;

		/**
		 * 
		 * @param qIn
		 *            : store SAM record from input file
		 * @param writeQueue
		 *            : store satisfied record based on query
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
		public Merger(AbstractQueue<Chromosome> qIn, Thread mainThread,
				CountDownLatch rLatch, CountDownLatch fLatch) throws Exception {
			this.queueIn = qIn;
			this.readLatch = rLatch;
			this.mergeLatch = fLatch;
		}

		@Override
		public void run() {

			int sleepcount = 0;
			countOutputSleep = 0;
			boolean run = true;

			try {

				while (run) {
					chromosome = queueIn.poll();

					if (chromosome == null) {
						// must check whether reading thread finished first.
						if (readLatch.getCount() == 0) {
							// write final records
							run = false;
						}

						if (exitStatus.intValue() > 0) {
							logger.error("Error occurred in merge thread as chromosome is null and exit status is 1");
							throw new QPileupException("ADD_EXCEPTION");
						}

						// qIn maybe filled again during sleep, so sleep should
						// be secondly
						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.info("Interrupted exception:" + Thread.currentThread().getName() + " "
									+ e.toString());
						}

					} else {
						if (exitStatus.intValue() > 0) {
							logger.error("Error occurred in merge thread as chromosome is not null and but exit status is 1");
							throw new QPileupException("ADD_EXCEPTION");
						}
						
						// read from the HDF file
						readFromHDF();
						
						//merge the HDFs
						mergeHDF();

						//write results to HDF
						writeToHDF();

						run = true;
					} // end else

				}// end while

				logger.info("Completed pileup thread: " + Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Setting exit status to 1 as exception occurred in merge thread: " + PileupUtil.getStrackTrace(e));
				exitStatus.incrementAndGet();
				Thread.currentThread().interrupt();
			} finally {
				logger.debug(String
						.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d",
								sleepcount, countOutputSleep, sleepUnit,
								queueIn.size()));

				mergeLatch.countDown();
			}
		}

		private void mergeHDF() throws Exception {
			for (PileupHDF inputHDF : inputHDFs) {
				//read
				int startIndex = chromosome.getStartPos() - 1;
				int size = chromosome.getSectionLength();
				
				StrandDS currentForwardDS = new StrandDS(inputHDF, chromosome.getName(), false);
				currentForwardDS.readDatasetBlock(startIndex, size);
				StrandDS currentReverseDS = new StrandDS(inputHDF, chromosome.getName(), true);
				currentReverseDS.readDatasetBlock(startIndex, size);
				
				//merge				
				forward.mergeDatasets(currentForwardDS.getElementsMap());
				reverse.mergeDatasets(currentReverseDS.getElementsMap());
			}			
		}

		private synchronized void readFromHDF() throws Exception {
			int startIndex = chromosome.getStartPos() - 1;
			int size = chromosome.getSectionLength();
			
			logger.info("Reading strand dataset: " + chromosome.getName()
					+ " start: " + startIndex + " size:" + size);

			forward = new StrandDS(hdf, chromosome.getName(), false);
			forward.readDatasetBlock(startIndex, size);
			reverse = new StrandDS(hdf, chromosome.getName(), true);
			reverse.readDatasetBlock(startIndex, size);			
		}

		private void writeToHDF() throws Exception {
			int index = chromosome.getStartPos() - 1;
			forward.writeDatasetBlocks(index, chromosome.getSectionLength(),
					false);
			reverse.writeDatasetBlocks(index, chromosome.getSectionLength(),
					false);
			logger.info("Finished writing strand dataset: "
					+ chromosome.getName() + " start: " + index + " size:"
					+ chromosome.getSectionLength());
		}
	}
}
