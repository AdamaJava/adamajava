/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qsv.softclip;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.util.Constants;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVCluster;
import org.qcmg.qsv.QSVClusterWriter;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.CustomThreadPoolExecutor;
import org.qcmg.qsv.util.QSVUtil;

import au.edu.qimr.tiledaligner.util.TiledAlignerUtil;
import gnu.trove.map.TIntObjectMap;

public class FindClipClustersMT  {

	private static final String LOW_CONF_FILE_HEADER = "reference\tposition\tmutation_type\tclip_type\tpos_clips\tneg_clips\tconsensus" + QSVUtil.NEW_LINE;

	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final int sleepUnit = 10;
	private final QSVParameters tumourParameters;
	private final QSVParameters normalParameters;
	private final AtomicInteger exitStatus = new AtomicInteger();
	private final String softClipDir;
	private final ConcurrentHashMap<String, List<SoftClipCluster>> clipRecordsMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<PairGroup, Map<String, List<DiscordantPairCluster>>> tumorClusterRecords;
	private final TIntObjectMap<int[]> cache;
	private final boolean singleSided;
	private final boolean isSplitRead;
	private final int CONSENSUS_LENGTH;
	private final int MIN_INSERT_SIZE;
	private final String reference;
    private final boolean translocationOnly;
	private final boolean allChromosomes;
	private final int defineThreadNo;
	private final int noOfThreads;
	private final int noOfFinalThreads;
	private final boolean runClip;
	private final boolean isQCMG;
	private final String lowConfidenceFile;
	private final QSVClusterWriter qsvRecordWriter;
	private final int CLIP_SIZE;

	public FindClipClustersMT(QSVParameters tumourParameters, QSVParameters normalParameters, String softclipDir, Map<PairGroup, Map<String, List<DiscordantPairCluster>>> tumorClusterRecords, Options options, String analysisId, long clipCount, TIntObjectMap<int[]> cache) throws Exception {
		this.noOfThreads = clipCount > 5000000 ? 1 : 3;
		this.noOfFinalThreads = clipCount > 5000000 ? 4 : 10;
		this.defineThreadNo = clipCount > 5000000 ? 2 : 4;

		logger.info("Total clips: " + clipCount + ", will use " + noOfThreads + " main threads, " + noOfFinalThreads + " final threads and " + defineThreadNo + " define threads");

		this.softClipDir = softclipDir;
		this.isSplitRead = options.isSplitRead();
		this.tumourParameters = tumourParameters; 
		this.normalParameters = normalParameters;
		this.cache = cache;
		this.tumorClusterRecords = new ConcurrentHashMap<>(tumorClusterRecords);
		this.isQCMG = options.isQCMG();
		this.singleSided = options.singleSided();
		this.CONSENSUS_LENGTH = options.getConsensusLength();
		this.CLIP_SIZE = options.getClipSize() - 1;
		this.MIN_INSERT_SIZE = options.getMinInsertSize();
		this.reference = options.getReference();
		this.runClip = ! options.getAnalysisMode().equals("pair");
        String platform = options.getPlatform();
		this.translocationOnly = options.getIncludeTranslocations();
		this.allChromosomes = options.allChromosomes();
		this.lowConfidenceFile = tumourParameters.getResultsDir() + "_no_blat_alignment.txt";

		logger.info("Minimum clip size: " + (CLIP_SIZE + 1));
		logger.info("Minimum clip consensus length: " + CONSENSUS_LENGTH);
		createLowConfidenceFile();
		this.qsvRecordWriter =  new QSVClusterWriter(tumourParameters, normalParameters, isQCMG, analysisId, singleSided, (normalParameters != null), MIN_INSERT_SIZE, platform, options.getGffFiles());
	}

	private void createLowConfidenceFile() throws IOException {
		File f = new File(lowConfidenceFile);

		try (FileWriter fw = new FileWriter(f);
				BufferedWriter writer = new BufferedWriter(fw)) {

			writer.write(LOW_CONF_FILE_HEADER);
		}
	}

	private synchronized void writeToLowConfidenceFile(List<Breakpoint> breakpoints) throws IOException {

		try (FileWriter fw = new FileWriter(lowConfidenceFile, true);
             BufferedWriter writer = new BufferedWriter(fw)) {

			for (Breakpoint b : breakpoints) {
				if ( ! b.isGermline()) {
					writer.write(b.toLowConfidenceString());
				}
			}
		}
	}

	public QSVClusterWriter getQSVRecordWriter() {
		return this.qsvRecordWriter;		
	}

	public void execute() {

		// create queue to store the records from input
		final AbstractQueue<List<Chromosome>> readQueue = new ConcurrentLinkedQueue<>();
		final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // cluster thread

		ExecutorService filterThreads = new CustomThreadPoolExecutor(noOfThreads, exitStatus, logger);
		try {

			for (Entry<String, List<Chromosome>> entry: tumourParameters.getChromosomes().entrySet()) {
				readQueue.add(entry.getValue());
			}

			// kick-off cluster thread
			for (int i = 0; i < noOfThreads; i++) {
				filterThreads.execute(new Clustering(readQueue,
						Thread.currentThread(), filterLatch));
			}
			filterThreads.shutdown();            

			logger.info("waiting for  threads to finish (max wait will be 100 hours)");
			if (!filterThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
				logger.error("Filtering threads did not finish within the time limit");
			}

			if ( ! readQueue.isEmpty()) {
				throw new Exception(
						" threads have completed but queue isn't empty  (readQueue):  "
								+ readQueue.size());
			}
			logger.info("All threads finished");


		} catch (Exception e) {
			logger.error("Setting exit status as exception caught in execute: "
					+ QSVUtil.getStrackTrace(e));
			if (exitStatus.intValue() == 0) {
				exitStatus.incrementAndGet();
			}
			filterThreads.shutdownNow();
		} finally {
			filterThreads.shutdownNow();
			//find last ones (translocations)
			logger.info("Finished intra-chromosomal");
			if (exitStatus.intValue() == 0) {
				findLastOverlappingClusters();
			}            
		}
	}	

	public AtomicInteger getExitStatus() {
		return this.exitStatus;
	}		

	private void findLastOverlappingClusters() {

		logger.info("Finding clipping translocations");

		final AbstractQueue<String> finalQueue = new ConcurrentLinkedQueue<>();
		ExecutorService overlapThreads =  new CustomThreadPoolExecutor(
				noOfFinalThreads, exitStatus, logger);

		try {
			Set<String> referenceKeys = getReferenceKeys();

            finalQueue.addAll(referenceKeys);

			final CountDownLatch overlapLatch = new CountDownLatch(noOfFinalThreads);	
			for (int i = 0; i < noOfFinalThreads; i++) {
				overlapThreads.execute(new FinalOverlap(finalQueue, Thread
						.currentThread(), overlapLatch));
			}
			overlapThreads.shutdown();
			if (!overlapThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
				logger.error("Overlap threads did not finish within the time limit");
			}
		} catch (Exception e) {
			logger.error("Setting exit status in 1 as exception caught: "
					+ QSVUtil.getStrackTrace(e));
			if (exitStatus.intValue() == 0) {
				exitStatus.incrementAndGet();
			}
		} finally {
			overlapThreads.shutdownNow();
			logger.info("Finished finding clipping translocations");
		}
	}

	private Set<String> getReferenceKeys() {
		Set<String> referenceKeys = new HashSet<>(clipRecordsMap.keySet());

		for (Entry<PairGroup, Map<String, List<DiscordantPairCluster>>> record : tumorClusterRecords.entrySet()) {
			Set<String> keys = record.getValue().keySet();
			referenceKeys.addAll(keys);
		}
		return referenceKeys;
	}

	private void findOverlappingClusters(String referenceKey, List<QSVCluster> records, List<DiscordantPairCluster> clusters, List<SoftClipCluster> clips, boolean translocation) {


		if (clusters != null && clips != null) {
			logger.info(referenceKey + "- number of clusters: "  + clusters.size() + ", number of clips " + clips.size());
			if (translocation) {
				List<SoftClipCluster> properClips = getProperClipSVs(referenceKey, clips);
				findOverlaps(records, clusters, properClips);
			} else {    			
				findOverlaps(records, clusters, clips);
			}    		
		} else if (clusters != null) {
			logger.info(referenceKey + "- number of clusters: "  + clusters.size());
			for (DiscordantPairCluster c: clusters) {
				records.add(new QSVCluster(c, false, tumourParameters.getSampleId()));
			}
		} else {
			logger.info(referenceKey + "- number of clips: "  + clips.size());
			if (translocation) {
				List<SoftClipCluster> properClips = getProperClipSVs(referenceKey, clips);

				for (SoftClipCluster c: properClips) {
					records.add(new QSVCluster(c, tumourParameters.getSampleId()));
				}
			} else {
				for (SoftClipCluster c: clips) {
					records.add(new QSVCluster(c, tumourParameters.getSampleId()));
				}
			}    		
		}
	}

	List<SoftClipCluster> getProperClipSVs(String key, List<SoftClipCluster> bpList) {
		logger.info("Potential records to find SVs in " + key + " is: " + bpList.size());
		Collections.sort(bpList);		 
		Map<SoftClipCluster, Boolean> clipRecords = new HashMap<>();
		Iterator<SoftClipCluster> iterator = bpList.iterator();
		int i = 0;
		int count = 0;
		while (iterator.hasNext()) {
			SoftClipCluster recordOne = iterator.next();
			for (int j = i ; j < bpList.size() ; j++) {
				SoftClipCluster recordTwo = bpList.get(j);
				if ( ! recordOne.equals(recordTwo) && ! recordTwo.alreadyMatched()) {
					if (recordOne.findMatchingBreakpoints(recordTwo)) {
						recordOne.setAlreadyMatched(true);
						recordTwo.setAlreadyMatched(true);
						SoftClipCluster newRecord = new SoftClipCluster(recordOne.getSingleBreakpoint(), recordTwo.getSingleBreakpoint());
						if ( ! clipRecords.containsKey(newRecord)) {
							count++;
							clipRecords.put(newRecord, Boolean.TRUE);
						}
						break;
					}						
				}					
			}

			if ( ! recordOne.alreadyMatched()) {
				clipRecords.put(recordOne, Boolean.TRUE);
			}
			iterator.remove();
		}
		logger.info("Total Clip SVs found for  " + key + " is: " + clipRecords.size() + " matched count: " + count);

		return new ArrayList<>(clipRecords.keySet());
	}

	private void findOverlaps(List<QSVCluster> records, List<DiscordantPairCluster> clusters, List<SoftClipCluster> clips) {
		Iterator<DiscordantPairCluster> iter = clusters.iterator();
		//Sort clips
		Collections.sort(clips);

		// look for matches
		while (iter.hasNext()) {
			DiscordantPairCluster cluster = iter.next();

			QSVCluster record = new QSVCluster(cluster, false, tumourParameters.getSampleId());
			for (SoftClipCluster potentialClip : clips) {

				if (record.findClusterOverlap(potentialClip)) {					
					potentialClip.setHasClusterMatch(true);
				}
			}
			records.add(record);			
			iter.remove();
		}

		// remaining soft clips
		int i = 0, size = clips.size();
		for (SoftClipCluster clip : clips) {
			//already added elsewhere
			if ( ! clip.hasClipMatch() && ! clip.hasClusterMatch()) {
				QSVCluster record = new QSVCluster(clip, tumourParameters.getSampleId());
				//find matches
				for (int j = i + 1; j < size ; j++) {
					SoftClipCluster clip2 = clips.get(j);
					if (! clip2.hasClusterMatch() && ! clip2.hasClipMatch() && record.findClipOverlap(clip2)) {
						clip2.setHasClipMatch(true);
					}
				}
				//then add
				records.add(record);
			}
			i++;
		}
	}
	
	private void rescueQSVRecords(String key, List<QSVCluster> inputClusters) throws Exception {
		
		logger.info("Finding split read alignments in " + inputClusters.size() + " records for " + key);
		int count = 0;

		String blatFile = softClipDir + QSVUtil.getFileSeparator() + UUID.randomUUID();
		for (QSVCluster cluster: inputClusters) {
			cluster.rescueClippping(cache, tumourParameters, normalParameters, softClipDir, CONSENSUS_LENGTH, MIN_INSERT_SIZE);
			cluster.createSplitReadContig(cache, tumourParameters, normalParameters, softClipDir, isSplitRead, reference, blatFile);
		}

		for (QSVCluster r: inputClusters) {
			boolean rescued = r.findSplitReadContig(tumourParameters, isSplitRead, reference, false);
			if (rescued) {
				count++;
			}
		}	
		logger.info("Finished finding split read alignments for "+ key +", number processed: " + count);
	}

	private List<DiscordantPairCluster> getTumourClustersByReferenceKey(String key) {
		List<DiscordantPairCluster> list = new ArrayList<>();

		for (Map.Entry<PairGroup, Map<String, List<DiscordantPairCluster>>> entry : tumorClusterRecords.entrySet()) {
			Map<String, List<DiscordantPairCluster>> currentMap = entry.getValue();

			List<DiscordantPairCluster> listFromMap = currentMap.remove(key);
			if (null != listFromMap) {
				list.addAll(listFromMap);
			}
		}
		return list;
	}

	private class Clustering implements Runnable {

		private final AbstractQueue<List<Chromosome>> queueIn;  
		private final Thread mainThread;
		private final CountDownLatch latch;
        private String currentReferenceKey;
		private Chromosome chromosome;
		private List<QSVCluster> currentQsvRecords;

		public Clustering(AbstractQueue<List<Chromosome>> readQueue,
				Thread mainThread,
				CountDownLatch latch) {
			this.queueIn = readQueue;
			this.mainThread = mainThread;
			this.latch = latch;
		}

		@Override
		public void run() {

			int sleepcount = 0;
            int countOutputSleep = 0;
			boolean run = true;

			try {

				List<Chromosome> chromosomes;

				while (run) {
					chromosomes = queueIn.poll();                

					if (chromosomes == null) {
						run = false;
						// qIn maybe filled again during sleep, so sleep should
						// be secondly
						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.error("Interrupted exception caught (a): "
									+ QSVUtil.getStrackTrace(e));
							if (exitStatus.intValue() == 0) {
								exitStatus.incrementAndGet();
							}
							throw e;
						}

					} else {
						//check to make sure exceptions haven't occurred
						if (exitStatus.intValue() != 0) {
                            logger.error("Terminating clustering thread due to exception in another thread: ");
						}


						if (runClip) {
							currentQsvRecords = new ArrayList<>();

							for (Chromosome c: chromosomes) {
								this.chromosome = c;            			
								clusterSoftClips(chromosome);        						      	 
							}


							List<DiscordantPairCluster> clusterList = getTumourClustersByReferenceKey(currentReferenceKey);
							findOverlappingClusters(currentReferenceKey, currentQsvRecords, clusterList,
									clipRecordsMap.get(currentReferenceKey), false);
                            clipRecordsMap.remove(currentReferenceKey);
							rescueCurrentQSVRecords(currentReferenceKey, currentQsvRecords);						

							currentReferenceKey = null;
						}
                    } // end else
				}// end while

				logger.info("Completed clipping thread: "
						+ Thread.currentThread().getName());

			} catch (Exception e) {
				logger.error("Setting exit status in clip cluster thread to 1 as exception caught: "
						+ QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				logger.debug(String
						.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn are %d",
								sleepcount, countOutputSleep, sleepUnit,
								queueIn.size()));
				latch.countDown();
			}
		}

		private void clusterSoftClips(Chromosome chromosome) throws Exception {

			TreeMap<Integer, Breakpoint> leftPositions = new TreeMap<>();
			TreeMap<Integer, Breakpoint> rightPositions = new TreeMap<>();
			TreeMap<Integer, List<UnmappedRead>> splitReads = new TreeMap<>();

			getClipPositions(true, leftPositions, rightPositions, splitReads, tumourParameters.getFindType());

			logger.info("Clip left positions: " + leftPositions.size() + ", right positions: " + rightPositions.size() + ", for " + chromosome.getName());
			long noOfUnmapped = 0;
			for (List<UnmappedRead> ur : splitReads.values()) {
				noOfUnmapped += ur.size();
			}
			logger.info("Split reads: " + noOfUnmapped + " for " + chromosome.getName());

			if ( ! leftPositions.isEmpty() || ! rightPositions.isEmpty()) {
				if (normalParameters != null) {
					getClipPositions(false, leftPositions, rightPositions, splitReads, normalParameters.getFindType());
				}
			}
			this.currentReferenceKey = chromosome.getName() + ":" + chromosome.getName();

			if ( ! leftPositions.isEmpty() || ! rightPositions.isEmpty()) {
				Map<String, List<Breakpoint>> breakpointMap = defineClipPositions(leftPositions, rightPositions, splitReads);
                findMatchingClipBreakpoint(breakpointMap);
			}			
		}

		private Map<String, List<Breakpoint>> defineClipPositions(TreeMap<Integer, Breakpoint> leftClipPositions, TreeMap<Integer, Breakpoint> rightClipPositions,TreeMap<Integer, List<UnmappedRead>> splitReads) throws Exception {
			int buffer = tumourParameters.getUpperInsertSize() + 100;
			AbstractQueue<Breakpoint> queueIn = new ConcurrentLinkedQueue<>();
			logger.info("Before " +  chromosome.getName() + " left: " + leftClipPositions.size() + " right:" + rightClipPositions.size()) ;
			for (Entry<Integer, Breakpoint> entry : leftClipPositions.entrySet()) {
				Integer key = entry.getKey();
				Breakpoint leftBP = entry.getValue();

				Breakpoint rightBP = rightClipPositions.remove(key);
				int leftBreakpoint = leftBP.getBreakpoint();
				Integer start = leftBreakpoint - buffer;
				Integer end  = leftBreakpoint + buffer;
				NavigableMap<Integer, List<UnmappedRead>> splitReadsMap = splitReads.subMap(start, true, end, true);
				if (splitReadsMap.size() > 10000) {
					logger.warn("Large number (" + splitReadsMap.size() + ") of unmapped reads for breakpoint: " + leftBP.getName());
				}
				leftBP.addSplitReadsMap(splitReadsMap);
				queueIn.add(leftBP);
				if (rightBP != null) {
					rightBP.addSplitReadsMap(splitReadsMap);					
					queueIn.add(rightBP);
				}
			}

			for (Entry<Integer, Breakpoint> entry : rightClipPositions.entrySet()) {
				Breakpoint rightBP = entry.getValue();		
				int rightBreakpoint = rightBP.getBreakpoint();
				Integer start = rightBreakpoint - buffer;
				Integer end  = rightBreakpoint + buffer;
				NavigableMap<Integer, List<UnmappedRead>> splitReadsMap = splitReads.subMap(start, true, end, true);
				if (splitReadsMap.size() > 10000) {
					logger.warn("Large number (" + splitReadsMap.size() + ") of unmapped reads for breakpoint: " + rightBP.getName());
				}
				rightBP.addSplitReadsMap(splitReadsMap);					
				queueIn.add(rightBP);				
			}

			ExecutorService executorService = new CustomThreadPoolExecutor(defineThreadNo, exitStatus, logger);
			CountDownLatch countDownLatch = new CountDownLatch(defineThreadNo);
			Set<Future<List<Breakpoint>>> set = new HashSet<>();

			for (int i=0; i<defineThreadNo; i++) {
				Callable<List<Breakpoint>> callable = new DefineBreakpoint(queueIn, mainThread, countDownLatch, CLIP_SIZE, false);
				Future<List<Breakpoint>> future = executorService.submit(callable);
				set.add(future);
			}

			executorService.shutdown();

			try {
				if (!executorService.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
					logger.error("Threads did not finish within the time limit");
				}


			} catch (InterruptedException e) {             
				logger.error("Interrupted exception caught (b): "
						+ QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				executorService.shutdownNow();
				throw e;
			}
			executorService.shutdownNow();
			List<Breakpoint> breakpoints = new ArrayList<>();
			for (Future<List<Breakpoint>> f : set) {
				breakpoints.addAll(f.get());
			}		         

			// blat for the matching breakpoint
			int size = breakpoints.size();
			breakpoints.sort(null);
			logger.info("Examining tiledAligner to find matching positions for " + size + " breakpoints on " + chromosome.getName());
			Map<String, List<Breakpoint>> breakpointMap = blatBreakpoints(breakpoints);
			logger.info("Finished examining tiledAligner to find matching positions for " + size + " breakpoints on " + chromosome.getName());
			return breakpointMap;
		}

		private void findMatchingClipBreakpoint(Map<String, List<Breakpoint>> leftMap) {
			logger.info("Finding matching clip breakpoint for " + chromosome.getName());

			for (Entry<String, List<Breakpoint>> entry : leftMap.entrySet()) {
				String key = entry.getKey();
				//at this stage only match intra-chromosomal
				List<SoftClipCluster> bpList = new ArrayList<>();
				for (Breakpoint b : entry.getValue()) {
					bpList.add(new SoftClipCluster(b));
				}

				if (key.equals(currentReferenceKey)) {
					List<SoftClipCluster> properClipRecords = getProperClipSVs(key, bpList);
					if (clipRecordsMap.containsKey(key)) {
						clipRecordsMap.get(key).addAll(properClipRecords);
					} else {
						clipRecordsMap.put(key, properClipRecords);	
					}
				} else {
					if (clipRecordsMap.containsKey(key)) {
						clipRecordsMap.get(key).addAll(bpList);
					} else {
						clipRecordsMap.put(key, bpList);	
					}
				}				 
			}
		}

		private Map<String, List<Breakpoint>> blatBreakpoints(List<Breakpoint> breakpoints) throws Exception {
			
			if ( ! breakpoints.isEmpty()) {
				
				Map<String, String> sequenceNameMapToSendToTiledAligner = breakpoints.stream().filter(b -> b.getMateConsensus() != null).collect(Collectors.toMap(Breakpoint::getMateConsensus, Breakpoint::getName, (s, a) -> s + ", " + a));
	
				/*
				 * This gives me possibly many BLATRecords per sequence. Here, we only want the "best" record (the one with the highest score)
				 */
				Map<String, List<BLATRecord>> allBlatRecords = TiledAlignerUtil.runTiledAlignerCache(tumourParameters.getReference(), tumourParameters.getRefIndexPositionMap(), cache, sequenceNameMapToSendToTiledAligner, 13, "FindClipClustersMT.blatBreakpoints", false, false);
				logger.debug("in blatBreakpoints all, allBlatRecords size: " + allBlatRecords.size());
				for (Entry<String, List<BLATRecord>> entry : allBlatRecords.entrySet()) {
					logger.debug("in blatBreakpoints all: key: " + entry.getKey() + ", blat recs: " + entry.getValue().stream().map(BLATRecord::toString).collect(Collectors.joining(",")));
				}
				
				
				/*
				 * The  bestBlatRecords map has the name rather than the sequence as the key, as this is what is queried for downstream
				 */
				Map<String, BLATRecord> bestBlatRecords = new HashMap<>();
				
				for (Entry <String, List<BLATRecord>> entry : allBlatRecords.entrySet()) {
					bestBlatRecords.put(sequenceNameMapToSendToTiledAligner.get(entry.getKey()), null == entry.getValue() || entry.getValue().isEmpty() ? null : entry.getValue().getLast());
				}
				logger.debug("in blatBreakpoints best, bestBlatRecords size: " + bestBlatRecords.size());
				for (Entry<String, BLATRecord> entry : bestBlatRecords.entrySet()) {
					logger.debug("in blatBreakpoints best: key: " + entry.getKey() + ", blat rec: " + (null == entry.getValue() ? "null" : entry.getValue().toString()));
				}
				/*
				 * get copy of list of breakpoints, as they are cleared upon calling matchBlatBreakpoints
				 */
				logger.debug("match blat - original");
				Map<String, List<Breakpoint>> originalMap = matchBlatBreakpoints(breakpoints, bestBlatRecords);
				logger.debug("DONE - match blat - original");
				
				return originalMap;
			}
			return Collections.emptyMap();
		}

		private Map<String, List<Breakpoint>> matchBlatBreakpoints(List<Breakpoint> breakpoints, Map<String,BLATRecord> blatRecords) throws IOException {
			Map<String, List<Breakpoint>> breakpointMap = new TreeMap<>();
			//determine the breakpoint 		
			int count = 0;
			List<Breakpoint> nonBlatAligned = new ArrayList<>();

			for (Breakpoint r : breakpoints) {
				
				BLATRecord blatR = blatRecords.get(r.getName());
				if (null != blatR) {
					
					logger.debug("breakpoint: " + r.toLowConfidenceString() + ", blat: " + blatR);

					boolean matchingBreakpoint = r.findMateBreakpoint(blatR);

					logger.debug("match?: " + matchingBreakpoint + "allChromosomes?: " + allChromosomes + ", translocationOnly: " + translocationOnly + ", r.isTranslocation(): " + r.isTranslocation());
					if (matchingBreakpoint) {
						//if running translocations, only get the matches on different chromosomes
						if (allChromosomes || (translocationOnly && r.isTranslocation()) || (!translocationOnly && !r.isTranslocation())) {
							logger.debug("allChromosomes || (translocationOnly && r.isTranslocation()) || (!translocationOnly && !r.isTranslocation()) is true ");
							count++;
							
							breakpointMap.computeIfAbsent(r.getReferenceKey(), v -> new ArrayList<>()).add(r);
						}
					} else {
						if (r.isNonBlatAligned()) {
							nonBlatAligned.add(r);
						}
					}
				} else {
					if (r.isNonBlatAligned()) {
						nonBlatAligned.add(r);
					}
				}
			}
			// check to see if we have somatic results to write out
			boolean somaticExists = false;
			for (Breakpoint b :  nonBlatAligned) {
				if ( ! b.isGermline()) {
					somaticExists = true;
					break;
				}
			}
			if (somaticExists) {
				writeToLowConfidenceFile(nonBlatAligned);
			}
			breakpoints.clear();

			logger.info("Matched clip positions found for " + chromosome.getName() + " " + count);
			return breakpointMap;
		}

		private void getClipPositions(boolean isTumour, TreeMap<Integer, Breakpoint> leftPositions,
				TreeMap<Integer, Breakpoint> rightPositions, TreeMap<Integer, List<UnmappedRead>> splitReads, String type)
						throws IOException, QSVException {
			File file = new File(SoftClipStaticMethods.getSoftClipFile(chromosome.getName(), type, softClipDir));			

			logger.info("Getting  " + type + " clips for " + chromosome.getName());

			if (file.exists()) {
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

					String line = reader.readLine();

					while (line != null) {
						if (line.startsWith("unmapped")) {
							if (isSplitRead) {
								UnmappedRead r = new UnmappedRead(line, isTumour);
                                List<UnmappedRead> reads = splitReads.computeIfAbsent(r.getBpPos(), k -> new ArrayList<>());
                                reads.add(r);
							}
						} else {
							Clip record = new Clip(line);

							if (record.getBpPos() >= chromosome.getStartPosition()
									&& record.getBpPos() <= chromosome.getEndPosition()) {
								
								Integer key = record.getBpPos();
								if (record.isLeft()) {

									Breakpoint b = leftPositions.get(key);
									if (isTumour) {
										if (null == b) {
											b = new Breakpoint(key, record.getReference(), true, CONSENSUS_LENGTH, MIN_INSERT_SIZE);
											leftPositions.put(key, b);
										}
										b.addTumourClip(record);

									} else {
										if (null != b) {
											b.addNormalClip(record);
										}
									}
								} else {
									Breakpoint b = rightPositions.get(key);
									if (isTumour) {
										if (null == b) {
											b = new Breakpoint(key, record.getReference(), false, CONSENSUS_LENGTH, MIN_INSERT_SIZE);
											rightPositions.put(key, b);
										}
										b.addTumourClip(record);
									} else {
										if (null != b) {
											b.addNormalClip(record);
										}
									}
								}
							}
						}
						line = reader.readLine();
					}
				}
			} else {
				logger.warn("No file found for: " + chromosome.getName() + ", file: " + file.getAbsolutePath());
			}
			if (isTumour) {
				logger.info("Total number of " + type + " clip positions for " + chromosome.getName() + ": " + (leftPositions.size() + rightPositions.size()));
			}
		}
		private void rescueCurrentQSVRecords(String key, List<QSVCluster> records) throws Exception {

			if (!records.isEmpty()) {
				logger.info("Finding split read alignments in " + records.size() + " records for " + key);
				AbstractQueue<List<QSVCluster>> queueIn = new ConcurrentLinkedQueue<>();

				int listSize = 50;

				for (int i = 0; i < records.size(); i += listSize) {
					if (((records.size()) - i) < listSize) {						
						queueIn.add(records.subList(i, records.size()));
						break;
					}
					if (i % listSize == 0) {						
						queueIn.add(records.subList(i, i + listSize));
					}							
				}	
				currentQsvRecords = null;
				ExecutorService executorService = new CustomThreadPoolExecutor(defineThreadNo, exitStatus, logger);
				CountDownLatch countDownLatch = new CountDownLatch(defineThreadNo);
				Set<Future<List<QSVCluster>>> set = new HashSet<>();

				for (int i = 0; i < defineThreadNo; i++) {
					Callable<List<QSVCluster>> callable = new RescueRecord(queueIn, mainThread, countDownLatch, cache, tumourParameters, normalParameters, softClipDir, CONSENSUS_LENGTH, isQCMG, MIN_INSERT_SIZE, singleSided, isSplitRead, reference);
					Future<List<QSVCluster>> future = executorService.submit(callable);
					set.add(future);
				}

				executorService.shutdown();

				try {
					if (!executorService.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
						logger.error("Threads did not finish within the time limit");
					}


				} catch (InterruptedException e) {             
					logger.error("Interrupted exception caught (c): "
							+ QSVUtil.getStrackTrace(e));
					if (exitStatus.intValue() == 0) {
						exitStatus.incrementAndGet();
					}
					executorService.shutdownNow();
					throw e;
				}
				executorService.shutdownNow();
				List<QSVCluster> rescuedClusters = new ArrayList<>();
				for (Future<List<QSVCluster>> f : set) {
					rescuedClusters.addAll(f.get());
				}
				int count = rescuedClusters.size();
				records.clear();
				
				qsvRecordWriter.writeTumourSVRecords(rescuedClusters);
				logger.info("Finished finding split read alignments for "+ key +", number processed: " + count);
			}
		}
	}

	private class FinalOverlap implements Runnable {

		private final AbstractQueue<String> queueIn;  
		private final Thread mainThread;
		private final CountDownLatch latch;

        public FinalOverlap(AbstractQueue<String> readQueue,
				Thread mainThread,
				CountDownLatch latch) {
			this.queueIn = readQueue;
			this.mainThread = mainThread;
			this.latch = latch;
		}

		@Override
		public void run() {

			int sleepcount = 0;
            int countOutputSleep = 0;
			boolean run = true;

			try {

				String referenceKey;

				while (run) {
					referenceKey = queueIn.poll();                

					if (referenceKey == null) {
						run = false;
						// qIn maybe filled again during sleep, so sleep should
						// be secondly
						try {
							Thread.sleep(sleepUnit);
							sleepcount++;
						} catch (InterruptedException e) {
							logger.error("Interrupted exception caught (d): "
									+ QSVUtil.getStrackTrace(e));
							if (exitStatus.intValue() == 0) {
								exitStatus.incrementAndGet();
							}
							throw e;
						}

					} else {      

						logger.info("Finalising overlapping clusters for: " + referenceKey);
						List<SoftClipCluster> clips = clipRecordsMap.get(referenceKey);
						List<DiscordantPairCluster> clusters = new ArrayList<>();

						for (Entry<PairGroup, Map<String, List<DiscordantPairCluster>>> record : tumorClusterRecords.entrySet()) {
							List<DiscordantPairCluster> value = record.getValue().get(referenceKey);
							if (value != null) {
								clusters.addAll(value);
							}
						}	 
						List<QSVCluster> records = new ArrayList<>();
						findOverlappingClusters(referenceKey, records, clusters, clips, true);
						rescueQSVRecords(referenceKey, records);

						qsvRecordWriter.writeTumourSVRecords(records);
					} // end else
				}// end while
				logger.info("Completed clipping thread: "
						+ Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Setting exit status in annotation thread to 1 as exception caught: "
						+ QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				logger.info(String
						.format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn are %d",
								sleepcount, countOutputSleep, sleepUnit,
								queueIn.size()));
				latch.countDown();
			}
		}	
	}

	private class DefineBreakpoint implements Callable<List<Breakpoint>> {

		private final AbstractQueue<Breakpoint> queueIn;  
		private final Thread mainThread;
		private final CountDownLatch latch;      
		private final List<Breakpoint> breakpoints = new ArrayList<>();
		private final int clipSize;
		private final boolean isRescue;

		public DefineBreakpoint(AbstractQueue<Breakpoint> readQueue,
				Thread mainThread,
				CountDownLatch latch, int clipSize, boolean isRescue) {
			this.queueIn = readQueue;
			this.mainThread = mainThread;
			this.latch = latch;
			this.clipSize = clipSize;
			this.isRescue = isRescue;
		}

		@Override
		public List<Breakpoint> call() {

			boolean run = true;

			try {

				Breakpoint breakpoint;
				while (run) {
					breakpoint = queueIn.poll();

					if (breakpoint == null) {

						// qIn maybe filled again during sleep, so sleep should
						// be secondly

						try {
							Thread.sleep(sleepUnit);
						} catch (InterruptedException e) {
							logger.error("Interrupted exception caught (e): "
									+ QSVUtil.getStrackTrace(e));
							if (exitStatus.intValue() == 0) {
								exitStatus.incrementAndGet();
							}
							throw e;
						}

						if (queueIn.isEmpty()) {
							run = false;
						}

					} else {

						if (breakpoint.defineBreakpoint(clipSize, isRescue)) {                		
							breakpoints.add(breakpoint);
						}
					} // end else
				}// end while
				logger.info("Completed define breakpoint thread: "
						+ Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Setting exit status when defining breakpoint thread to 1 as exception caught: "
						+ QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				latch.countDown();
			}
			return breakpoints;
		}
	}

	private class RescueRecord implements Callable<List<QSVCluster>> {

		private final AbstractQueue<List<QSVCluster>> queueIn;  
		private final Thread mainThread;
		private final CountDownLatch latch;      
		private final List<QSVCluster> clusters = new ArrayList<>();
		private final String reference;
		private final boolean isSplitRead;
		private final boolean singleSided;
		private final Integer minInsertSize;
		private final boolean isQCMG;
		private final Integer consensusLength;
		private final String softclipDir;
		private final QSVParameters normalParameters;
		private final QSVParameters tumourParameters;
		private final TIntObjectMap<int[]> cache;

		public RescueRecord(AbstractQueue<List<QSVCluster>> queueIn2, Thread mainThread, CountDownLatch latch, TIntObjectMap<int[]> cache,
							QSVParameters tumourParameters, QSVParameters normalParameters, String softClipDir, Integer consensusLength,
							boolean isQCMG, Integer minInsertSize, boolean singleSided, boolean isSplitRead, String reference) {
			this.queueIn = queueIn2;
			this.mainThread = mainThread;
			this.latch = latch;
			this.cache = cache;
			this.tumourParameters = tumourParameters;
			this.normalParameters = normalParameters;
			this.softclipDir = softClipDir;
			this.consensusLength = consensusLength;
			this.isQCMG = isQCMG;
			this.minInsertSize = minInsertSize;
			this.singleSided = singleSided;
			this.isSplitRead = isSplitRead;
			this.reference = reference;
		}

		@Override
		public List<QSVCluster> call() {

			boolean run = true;

			try {

				List<QSVCluster> inputClusters;
				while (run) {
					inputClusters = queueIn.poll();                

					if (inputClusters == null) {

						// qIn maybe filled again during sleep, so sleep should
						// be secondly

						try {
							Thread.sleep(sleepUnit);
						} catch (InterruptedException e) {
							logger.error("Interrupted exception caught (f): "
									+ QSVUtil.getStrackTrace(e));
							if (exitStatus.intValue() == 0) {
								exitStatus.incrementAndGet();
							}
							throw e;
						}

						if (queueIn.isEmpty()) {
							run = false;
						}

					} else {
						//Rescue clipping

						String blatFile = softClipDir + QSVUtil.getFileSeparator() + UUID.randomUUID();
						boolean log = false;
						
						for (QSVCluster cluster: inputClusters) {
							cluster.rescueClippping(cache, tumourParameters, normalParameters, softclipDir, consensusLength, minInsertSize);
							logger.debug("rescue clipping about to call createSplitReadContig");
							cluster.createSplitReadContig(cache, tumourParameters, normalParameters, softclipDir, isSplitRead, reference, blatFile, log);
							logger.debug("rescue clipping, cluster: " + cluster.toTabString());
						}
						
						for (QSVCluster cluster: inputClusters) {
							logger.debug("rescue clipping about to call findSplitReadContig, cluster: " + cluster.toTabString());
							logger.debug("rescue clipping about to call findSplitReadContig, getConfidenceLevel: " + cluster.getConfidenceLevel(true));
							logger.debug("rescue clipping about to call findSplitReadContig, isPotentialSplitRead: " + cluster.isPotentialSplitRead());
							logger.debug("tumourParameters getChromosomes size " + tumourParameters.getChromosomes().size());
							logger.debug("isSplitRead: " + isSplitRead);
							cluster.findSplitReadContig(tumourParameters, isSplitRead, reference, log);
							logger.debug("rescue clipping called findSplitReadContig, cluster: " + cluster.toTabString());
							logger.debug("rescue clipping called findSplitReadContig, getConfidenceLevel: " + cluster.getConfidenceLevel(true));
							logger.debug("rescue clipping called findSplitReadContig, isPotentialSplitRead: " + cluster.isPotentialSplitRead());
							clusters.add(cluster);
						}

                    } // end else
				}// end while
				logger.info("Completed rescue thread: "
						+ Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Setting exit status in rescue record thread to 1 as exception caught: "
						+ QSVUtil.getStrackTrace(e));
				if (exitStatus.intValue() == 0) {
					exitStatus.incrementAndGet();
				}
				mainThread.interrupt();
			} finally {
				latch.countDown();
			}
			return clusters;
		}
	}
}
