/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class QSVAssemble {
	
	private static final QLogger logger = QLoggerFactory.getLogger(QSVAssemble.class);
	static final int MINIMUM_READ_EXTEND = 1;
	public static final int SEED_LENGTH = 10;
	
	private List<Read> clipReads;
	private Read outputRead;
	private List<Read> splitReads;
	private String fullContigSequence;
	private int matchingSplitReads = 0;
	private Read clipContig;
	private Read reverseClipContig;
	private String fullClipSequence;
	private Read currentContig;


	public QSVAssemble() {
	}	
	
	public void setOutputRead(Read outputRead) {
		this.outputRead = outputRead;
	}

	public void setFullContigSequence(String fullContigSequence) {
		this.fullContigSequence = fullContigSequence;
	}

	public void setClipContig(Read clipContig) {
		this.clipContig = clipContig;
	}

	public ConsensusRead getContigs(Integer bpPos, Read clipContig, List<Read> splitReads, boolean clipReverse, boolean isLeft) throws Exception {
		this.splitReads = splitReads;

		this.clipContig = clipContig;
		
		this.fullContigSequence = assemble();
		
		splitReads.clear();		
			
		if (outputRead != null) {			
			ConsensusRead r = createFinalConsensusRead(isLeft, clipReverse, bpPos);
			if (r != null) {
				if (!QSVUtil.highNCount(r.getSequence(), 0.1)) {
					return r;
				}
			}
		} 
		
		return null;
	}
	
	public ConsensusRead createFinalConsensusRead(boolean isLeft, boolean clipReverse, int bpPos) {
		fullClipSequence = clipContig.getSequence();
		String finalClipString;
		String finalReferenceString;
		String header = "";
		if (fullClipSequence.length() == fullContigSequence.length()) {
			finalClipString = fullClipSequence;
			finalReferenceString = "";				
		} else {
			if (isLeft) {					
				int index = fullContigSequence.indexOf(fullClipSequence) + fullClipSequence.length();
				if (index != -1 && index < fullContigSequence.length()) {
					finalClipString = fullContigSequence.substring(0, index); 
					finalReferenceString = fullContigSequence.substring(index, fullContigSequence.length());
				} else {
					if (index > fullContigSequence.length()) {
						logger.info("Index out of range" + index + " " + bpPos + " " + fullContigSequence.length() + " " + fullContigSequence + " " + fullClipSequence);
					}						
					return null;
				}
			} else {					
				int index = fullContigSequence.indexOf(fullClipSequence); 
				if (index != -1) {
					finalClipString = fullContigSequence.substring(index, fullContigSequence.length()); 
					finalReferenceString = fullContigSequence.substring(0, index);
				} else {						
					return null;
				}
			}
			header = outputRead.getHeader();
		}			
		if (clipReverse) {
			return new ConsensusRead(header, QSVUtil.reverseComplement(fullContigSequence), QSVUtil.reverseComplement(finalClipString), QSVUtil.reverseComplement(finalReferenceString));
		} else {
			return new ConsensusRead(header, fullContigSequence, finalClipString, finalReferenceString);
		}		
	}

	public ConsensusRead getFinalContig(List<UnmappedRead> inputSplitReads, boolean log) throws QSVException {
		splitReads = new ArrayList<>();
		if (log) {
			logger.info("getFinalContig with inputSplitReads count: " + inputSplitReads.size());
		}
		for (UnmappedRead r: inputSplitReads) {
			if (log) {
				logger.info("getFinalContig read: " + r.getReadName() + ", " + r.getSequence());
			}
			splitReads.add(new Read(r.getReadName(), r.getSequence()));
		}
		
		Read contig = getSplitReadsContigs(splitReads, splitReads.size(), log);
		inputSplitReads = null;
		if (contig != null) {
			String seq = contig.getSequence();
			if ( ! QSVUtil.highNCount(seq, 0.1)) {
				return new ConsensusRead(contig.getHeader(), seq, seq, seq);
			}
		} 
		return null;
	}
	
	public String assemble() {		
		outputRead = clipContig;
		
		if (splitReads.size() > 0 && clipContig != null) {			
			
			clipContig.setHeader("clipContigFor");
			splitReads.add(0, clipContig);
			Read forSplitContig = getSplitReadsContigs(splitReads, 1, false);		
			removeRead(splitReads, clipContig);
			
			if (forSplitContig != null) {
				this.reverseClipContig = new Read("clipContigRev," + forSplitContig.getHeader(), QSVUtil.reverseComplement(forSplitContig.getSequence()));
			} else {
				this.reverseClipContig = new Read("clipContigRev" + clipContig.getHeader(), QSVUtil.reverseComplement(clipContig.getSequence()));
			}	
			splitReads.add(0, reverseClipContig);
			Read revSplitContig = getSplitReadsContigs(splitReads, 1, false);
			
			if (revSplitContig != null) {
				outputRead = new Read(revSplitContig.getHeader(), QSVUtil.reverseComplement(revSplitContig.getSequence()));	
			} else {
				if (forSplitContig != null) {				
					outputRead = forSplitContig;
				}
			}
			if (outputRead != null) {			
				String[] headers = outputRead.getHeader().split(",");
				matchingSplitReads = 0;
				for (String s: headers) {
					if (!s.contains("clip")) {
						matchingSplitReads++;
					}
				}
			}
		}		
		
		if (outputRead != null) {	
			return outputRead.getSequence();
		} else {
			if (clipReads.size() == 1) {
				return clipReads.get(0).getSequence();
			} else {
				return null;
			}
		}
	}
	
	private Read getSplitReadsContigs(List<Read> splitReads, int size, boolean log) {
		this.currentContig = null;
		if (log) {
			logger.info("getSplitReadsContigs, splitReads size: " + splitReads.size());
		}
		for (int i = 0; i < size; i++) {
			if (i < splitReads.size()) {
				createSeed(i, splitReads.get(i), splitReads);
				if (log && i < splitReads.size()) {
					logger.info("getSplitReadsContigs, created seed: " + (null != currentContig ? currentContig.getSequence() : "null") + " with i: " + i + ", read: " + splitReads.get(i).getSequence());
				}
			}
		}
		return currentContig;
	}

	private void createSeed(int currentIndex, Read seed, List<Read> reads) {
//		seed.createHashtable();

//		ConcurrentHashMap<Integer, ReadMatch> matches = new ConcurrentHashMap<>();
		
		TIntObjectMap<ReadMatch> matches = findMatches(currentIndex, seed, reads);	
//		findMatches(currentIndex, seed, matches, reads);	
		Alignment alignment = new Alignment(matches.valueCollection(), seed);
//		Alignment alignment = new Alignment(matches, seed);
		
		if (alignmentSizeGreater(alignment.calculateLength(), currentContig)) {
			Read contig = alignment.constructConsensusSequence();
			Iterator<ReadMatch> iter = alignment.getMatchingReads().iterator();
				
			if (contig != null) {
				if (contig.getSequence().contains(seed.getSequence())) {
					
					while (iter.hasNext()) {
						Read read = iter.next().read();
						contig.setHeader(contig.getHeader() + "," + read.getHeader());
						removeRead(reads, read);
					}
					if (currentContig == null) {
						currentContig = contig;
					} else {
						if (contig.length() > currentContig.length()) {
							currentContig = contig;
						}
					}
				} 
			}
		}
	}
	
	private TIntObjectMap<ReadMatch> findMatches(int currentIndex, Read seed, List<Read> reads) {
		TIntObjectMap<ReadMatch> map = new TIntObjectHashMap<>(reads.size());
		for (int j = 0, size = reads.size() ; j < size ; j++) {
			if (j != currentIndex){

				Read read = reads.get(j);
				
				String nextReadSeed = read.getSeed();
				String nextReadReverseSeed = read.getReverseSeed();

				if (seed.getHashtable().containsKey(nextReadSeed)) { //Forward extending
					//Calculate relative position of the read against the seed.
					
//					matches.put(read.hashCode(), new ReadMatch(read, seed.getHashtable().get(nextReadSeed)));
					map.put(read.hashCode(), new ReadMatch(read, seed.getHashtable().get(nextReadSeed)));
				} else if (seed.getHashtable().containsKey(nextReadReverseSeed)) { //Reverse extending
					//Calculate relative position of the read against the seed.
//					matches.put(getIndex(reads, read), new ReadMatch(read, seed.getHashtable().get(nextReadReverseSeed) - read.length() - SEED_LENGTH));
					map.put(getIndex(reads, read), new ReadMatch(read, seed.getHashtable().get(nextReadReverseSeed) - (read.length() - SEED_LENGTH)));
				}
			}			
		}
		return map;
	}
	
	private void findMatches(int currentIndex, Read seed,
			ConcurrentHashMap<Integer, ReadMatch> matches, List<Read> reads) {
		int position;
		for (int j=0, size = reads.size() ; j < size ; j++) {
			if (j != currentIndex){

				Read read = reads.get(j);
				
				String nextReadSeed = read.getSeed();
				String nextReadReverseSeed = read.getReverseSeed();	

				if (seed.getHashtable().containsKey(nextReadSeed)) { //Forward extending
					//Calculate relative position of the read against the seed.
					position = seed.getHashtable().get(nextReadSeed);
					
					matches.put(read.hashCode(), new ReadMatch(read, position));
				} else if (seed.getHashtable().containsKey(nextReadReverseSeed)) { //Reverse extending
					//Calculate relative position of the read against the seed.
					position = seed.getHashtable().get(nextReadReverseSeed);
//					System.out.println("position1: " + position + ", read.length(): " + read.length() + ", SEED_LENGTH: " + SEED_LENGTH + ", seed.getHashtable().get(nextReadReverseSeed): " + seed.getHashtable().get(nextReadReverseSeed));
					position -= read.length() - SEED_LENGTH;
//					System.out.println("position2: " + position);
//					System.out.println("position3: " + (seed.getHashtable().get(nextReadReverseSeed) - (read.length() - SEED_LENGTH) ));
					matches.put(getIndex(reads, read), new ReadMatch(read, position));
				}
			}			
		}		
	}

	public static boolean alignmentSizeGreater(int alignmentLength, Read contig) {
		return null == contig || alignmentLength > contig.length();
	}

	private void removeRead(List<Read> reads, Read read) {
		int index = getIndex(reads, read);	
		if (index >= 0) {
			reads.remove(index);
		}
	}
	
	private int getIndex(List<Read> reads, Read read) {
		int index = -1;
		for (int i = 0, size = reads.size() ; i < size ; i++) {
			if (read.getHeader().equals(reads.get(i).getHeader())) {
				index = i;
				break;
			}
		}
		return index;
	}

	
//	private void findMatches(int currentIndex, Read seed, ConcurrentHashMap<Integer, ReadMatch> matches, List<Read> reads) {
//		int position;
//		for (int j = 0, size = reads.size() ; j < size ; j++) {
//			if (j != currentIndex){
//				
//				Read read = reads.get(j);
//				
//				String nextReadSeed = read.getSeed();
//				String nextReadReverseSeed = read.getReverseSeed();
//				
//				if (seed.getHashtable().containsKey(nextReadSeed)) { //Forward extending
//					//Calculate relative position of the read against the seed.
//					position = seed.getHashtable().get(nextReadSeed);
//					
//					matches.put(read.hashCode(), new ReadMatch(read, position));
//				} else if (seed.getHashtable().containsKey(nextReadReverseSeed)) { //Reverse extending
//					//Calculate relative position of the read against the seed.
//					position = seed.getHashtable().get(nextReadReverseSeed);
//					position -= read.length() - SEED_LENGTH;
//					matches.put(getIndex(reads, read), new ReadMatch(read, position));
//				}
//			}			
//		}		
//	}

	public int getMatchingSplitReads() {
		return matchingSplitReads;
	}

	public String getFinalClipContig(String leftConsensus, String rightConsensus) throws QSVException {
		this.clipReads = new ArrayList<>();
		
		if (leftConsensus.contains(rightConsensus)) {
			return leftConsensus;
		} else if (rightConsensus.contains(leftConsensus)) {
			return rightConsensus;
		} else {
			clipReads.add(new Read("left", leftConsensus));
			clipReads.add(new Read("right", rightConsensus));			
			this.fullContigSequence = assembleFinalContig();

			if (fullContigSequence == null) {
				if (leftConsensus.length() >= rightConsensus.length()) {
					return leftConsensus;
				} else {
					return rightConsensus;
				} 			
			} else {
				return fullContigSequence;
			}
		}
	}
	
	private String assembleFinalContig() throws QSVException {
		createSeed(0, clipReads.get(0), clipReads);
		
		if (currentContig != null) {
			if (currentContig.getHeader().split(",").length == 2 && ! QSVUtil.highNCount(currentContig.getSequence(), 0.1)) {
				return currentContig.getSequence();
			}
		} 
		return null;
	}
}

