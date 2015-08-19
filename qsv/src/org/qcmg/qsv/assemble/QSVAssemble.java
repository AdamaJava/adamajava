/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
	private int matchingClips = 0;
	private String fullClipSequence;
	private Read currentContig;


	public QSVAssemble() throws Exception {
	}	
	
	public List<Read> getClipReads() {
		return clipReads;
	}

	public Read getOutputRead() {
		return outputRead;
	}

	public void setOutputRead(Read outputRead) {
		this.outputRead = outputRead;
	}

	public List<Read> getSplitReads() {
		return splitReads;
	}

	public String getFullContigSequence() {
		return fullContigSequence;
	}

	public void setFullContigSequence(String fullContigSequence) {
		this.fullContigSequence = fullContigSequence;
	}

	public Read getClipContig() {
		return clipContig;
	}

	public void setClipContig(Read clipContig) {
		this.clipContig = clipContig;
	}

	public Read getReverseClipContig() {
		return reverseClipContig;
	}

	public String getFullClipSequence() {
		return fullClipSequence;
	}

	public Read getCurrentContig() {
		return currentContig;
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
		String finalClipString = null;
		String finalReferenceString = null;
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
		splitReads = new ArrayList<Read>();
		for (UnmappedRead r: inputSplitReads) {
			splitReads.add(new Read(r.getReadName(), r.getSequence()));
		}
		
		Read contig = getSplitReadsContigs(splitReads, clipContig, splitReads.size());
		splitReads = null;
		inputSplitReads = null;
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
		
		if (clipContig != null) {
			matchingClips = clipContig.getHeader().split(",").length;	
		}		
		
		if (splitReads.size() > 0 && clipContig != null) {			
			
			clipContig.setHeader("clipContigFor");
			splitReads.add(0, clipContig);
			Read forSplitContig = getSplitReadsContigs(splitReads, clipContig, 1);		
			removeRead(splitReads, clipContig);
			
			if (forSplitContig != null) {
				this.reverseClipContig = new Read("clipContigRev," + forSplitContig.getHeader(), QSVUtil.reverseComplement(forSplitContig.getSequence()));
			} else {
				this.reverseClipContig = new Read("clipContigRev" + clipContig.getHeader(), QSVUtil.reverseComplement(clipContig.getSequence()));
			}	
			splitReads.add(0, reverseClipContig);
			Read revSplitContig = getSplitReadsContigs(splitReads, reverseClipContig, 1);
			
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
				matchingClips++;
				return clipReads.get(0).getSequence();
			} else {
				return null;
			}
		}
	}
	
	private Read getSplitReadsContigs(List<Read> splitReads, Read seedContig, int size) throws Exception {
		this.currentContig = null;
		
		for (int i=0; i<size; i++) {
			if (i<splitReads.size()) {
				createSeed(i, splitReads.get(i), splitReads, false);				
			}
		}
		return currentContig;
	}

	private void createSeed(int currentIndex, Read seed, List<Read> reads, boolean isClips) throws Exception {
		seed.createHashtable();

		ConcurrentHashMap<Integer, ReadMatch> matches = new ConcurrentHashMap<Integer, ReadMatch>();
		
		findMatches(currentIndex, seed, matches, reads);	
		Alignment alignment = new Alignment(matches, seed);
//		alignment.determingMatchingReads();
		
		if (alignmentSizeGreater(alignment.calculateLength())) {
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
					alignment = null;
				} 
			}
		}
	}

	private boolean alignmentSizeGreater(int alignmentLength) {
		if (currentContig == null) {
			return true;
		} else if (alignmentLength > currentContig.length()) {
			return true;
		}
		return false;
	}

	private void removeRead(List<Read> reads, Read read) {
		int index = getIndex(reads, read);	
		if (index>=0) {
			reads.remove(index);
		}
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
					position -= read.length() - SEED_LENGTH;
					matches.put(getIndex(reads, read), new ReadMatch(read, position));
				}
			}			
		}		
	}

	public int getMatchingSplitReads() {
		return matchingSplitReads;
	}
	
	public int getMatchingClips() {
		return matchingClips;
	}

	public String getFinalClipContig(String leftConsensus, String rightConsensus) throws Exception {
		this.clipReads = new ArrayList<Read>();
		
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
		createSeed(0, clipReads.get(0), clipReads, true);
		
		if (currentContig != null) {
			if (currentContig.getHeader().split(",").length == 2 && !QSVUtil.highNCount(currentContig.getSequence(), 0.1)) {
				return currentContig.getSequence();
			}
		} 
		return null;
	}
}

