/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;

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
	/*
	 * index of the first read with each header in the list currently being assembled (what getIndex returns).
	 * Built when first needed, and reset whenever that list changes.
	 */
	private Map<String, Integer> firstIndexByHeader;


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
	
	public ConsensusRead createFinalConsensusRead(boolean isLeft, boolean clipReverse, int bpPos) throws Exception {
		fullClipSequence = clipContig.getSequence();
		String finalClipString;
		String finalReferenceString;
		String header = "";
		if (fullClipSequence.length() == fullContigSequence.length()) {
			finalClipString = fullClipSequence;
			finalReferenceString = "";				
		} else {
			if (isLeft) {					
				int index = fullContigSequence.indexOf(fullClipSequence)+ fullClipSequence.length();
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

	public ConsensusRead getFinalContig(List<UnmappedRead> inputSplitReads) throws Exception {
		splitReads = new ArrayList<>();
		for (UnmappedRead r: inputSplitReads) {
			splitReads.add(new Read(r.getReadName(), r.getSequence()));
		}
		
		Read contig = getSplitReadsContigs(splitReads, splitReads.size());
        if (contig != null) {
			ConsensusRead r = new ConsensusRead(contig.getHeader(), contig.getSequence(), contig.getSequence(), contig.getSequence());
			if (!QSVUtil.highNCount(r.getSequence(), 0.1)) {
				return r;
			}
		} 
		return null;
	}
	
	public String assemble() throws Exception {		
		outputRead = clipContig;
		
		if (!splitReads.isEmpty() && clipContig != null) {
			
			clipContig.setHeader("clipContigFor");
			splitReads.addFirst(clipContig);
			Read forSplitContig = getSplitReadsContigs(splitReads, 1);
			removeRead(splitReads, clipContig);

			if (forSplitContig != null) {
				this.reverseClipContig = new Read("clipContigRev," + forSplitContig.getHeader(), QSVUtil.reverseComplement(forSplitContig.getSequence()));
			} else {
				this.reverseClipContig = new Read("clipContigRev" + clipContig.getHeader(), QSVUtil.reverseComplement(clipContig.getSequence()));
			}	
			splitReads.addFirst(reverseClipContig);
			Read revSplitContig = getSplitReadsContigs(splitReads, 1);
			
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
				return clipReads.getFirst().getSequence();
			} else {
				return null;
			}
		}
	}
	
	private Read getSplitReadsContigs(List<Read> splitReads, int size) throws Exception {
		this.currentContig = null;
		this.firstIndexByHeader = null;
		
		for (int i=0; i<size; i++) {
			if (i<splitReads.size()) {
				createSeed(i, splitReads.get(i), splitReads);
			}
		}
		return currentContig;
	}

	/*
	 * The matches found for a seed.
	 * 
	 * Originally these were put straight into a ConcurrentHashMap, keyed on read.hashCode() for forward matches and on the index of 
	 * the first read with the same header for reverse matches, so that later matches with the same key replaced earlier ones.
	 * That map was the single biggest CPU cost in qsv, as it is filled for every seed, and most seeds are never accepted.
	 * 
	 * The length and consensus only depend on which matches are kept, not on their order, so a cheap primitive map with the same keys 
	 * is used for those. The order of the ConcurrentHashMap does matter for the contig header (and so the order reads are listed in), 
	 * so the puts are recorded, and replayed into a ConcurrentHashMap in the same order if the contig is accepted, giving exactly 
	 * the same order as before.
	 */
	/*
	 * reused by each createSeed call
	 */
	private final Matches matches = new Matches();
	
	static final class Matches {
		private final TIntObjectHashMap<ReadMatch> byKey = new TIntObjectHashMap<>();
		private final TIntArrayList putKeys = new TIntArrayList();
		private final List<ReadMatch> putValues = new ArrayList<>();
		
		/*
		 * keeps the capacity of the map and lists, so that a Matches can be reused for each seed without re-growing them
		 * (the Alignment does not depend on the order of values())
		 */
		void clear() {
			byKey.clear();
			putKeys.clearQuick();
			putValues.clear();
		}
		
		void put(int key, ReadMatch match) {
			byKey.put(key, match);
			putKeys.add(key);
			putValues.add(match);
		}
		
		Collection<ReadMatch> values() {
			return byKey.valueCollection();
		}
		
		/*
		 * the matches in the order the original ConcurrentHashMap would have returned them
		 */
		Collection<ReadMatch> valuesInOriginalOrder() {
			ConcurrentHashMap<Integer, ReadMatch> map = new ConcurrentHashMap<>();
			for (int i = 0, size = putKeys.size(); i < size; i++) {
				map.put(putKeys.get(i), putValues.get(i));
			}
			return map.values();
		}
	}

	private void createSeed(int currentIndex, Read seed, List<Read> reads) throws Exception {
		TIntIntHashMap seedPositions = seed.createSeedPositions();

		Matches matches = this.matches;
		matches.clear();
		
		findMatches(currentIndex, seedPositions, matches, reads);	
		Alignment alignment = new Alignment(matches.values(), seed);
		
		if (alignmentSizeGreater(alignment.calculateLength())) {
			Read contig = alignment.constructConsensusSequence();		
				
			if (contig != null) {
				if (contig.getSequence().contains(seed.getSequence())) {
					
					StringBuilder header = new StringBuilder(contig.getHeader());
					Map<String, Integer> removalsByHeader = new HashMap<>();
					for (ReadMatch match : matches.valuesInOriginalOrder()) {
						String readHeader = match.read().getHeader();
						header.append(',').append(readHeader);
						removalsByHeader.merge(readHeader, 1, Integer::sum);
					}
					contig.setHeader(header.toString());
					if (removeReads(reads, removalsByHeader)) {
						firstIndexByHeader = null;
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

	private boolean alignmentSizeGreater(int alignmentLength) {
		if (currentContig == null) {
			return true;
		} else return alignmentLength > currentContig.length();
    }

	private void removeRead(List<Read> reads, Read read) {
		int index = getIndex(reads, read);	
		if (index>=0) {
			reads.remove(index);
			firstIndexByHeader = null;
		}
	}
	
	/*
	 * Same result as calling removeRead once per removal (each call removes the first remaining read with that header),
	 * but in a single pass over the list rather than one scan plus an array shift per removal.
	 * ie. for each header, the first n reads with that header are removed, where n is the number of removals requested for it.
	 */
	static boolean removeReads(List<Read> reads, Map<String, Integer> removalsByHeader) {
		return reads.removeIf(r -> {
			String header = r.getHeader();
			Integer remaining = removalsByHeader.get(header);
			if (remaining == null || remaining == 0) {
				return false;
			}
			removalsByHeader.put(header, remaining - 1);
			return true;
		});
	}
	
	private int getIndex(List<Read> reads, Read read) {
		int index = -1;
		for (int i=0, size = reads.size() ; i < size ; i++) {
			if (read.getHeader().equals(reads.get(i).getHeader())) {
				index = i;
				break;
			}
		}
		return index;
	}

	private void findMatches(int currentIndex, TIntIntHashMap seedPositions,
			Matches matches, List<Read> reads) {
		int position;
		for (int j=0, size = reads.size() ; j < size ; j++) {
			if (j != currentIndex){

				Read read = reads.get(j);
				
				position = seedPositions.get(read.getSeedKey());
				if (position != -1) { //Forward extending
					//Calculate relative position of the read against the seed.
					matches.put(read.hashCode(), new ReadMatch(read, position));
				} else {
					position = seedPositions.get(read.getReverseSeedKey());
					if (position != -1) { //Reverse extending
						//Calculate relative position of the read against the seed.
						position -= read.length() - SEED_LENGTH;
						if (firstIndexByHeader == null) {
							// only built if there is a reverse match, as it costs a pass over the reads
							firstIndexByHeader = new HashMap<>(size * 2);
							for (int k = 0; k < size; k++) {
								firstIndexByHeader.putIfAbsent(reads.get(k).getHeader(), k);
							}
						}
						matches.put(firstIndexByHeader.get(read.getHeader()), new ReadMatch(read, position));
					}
				}
			}			
		}		
	}

	public int getMatchingSplitReads() {
		return matchingSplitReads;
	}

	public String getFinalClipContig(String leftConsensus, String rightConsensus) throws Exception {
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
	
	private String assembleFinalContig() throws Exception {
		firstIndexByHeader = null;
		createSeed(0, clipReads.getFirst(), clipReads);
		
		if (currentContig != null) {
			if (currentContig.getHeader().split(",").length == 2 && !QSVUtil.highNCount(currentContig.getSequence(), 0.1)) {
				return currentContig.getSequence();
			}
		} 
		return null;
	}
}

