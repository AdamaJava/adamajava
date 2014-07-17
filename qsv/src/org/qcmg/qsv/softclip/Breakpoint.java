/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.assemble.Read;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;

public class Breakpoint implements Comparable<Breakpoint>{
	
	private static int LOW_CONF_CLIPS = 10;
	private static String NEWLINE = System.getProperty("line.separator");
	private static char TAB = '\t';	
	private String reference;
	private Integer breakpoint;
	private boolean isLeft;
	private boolean isGermline;
	private boolean rescued;
	private boolean nonBlatAligned;
	private HashSet<Clip> tumourClips = new HashSet<Clip>();
	private HashSet<Clip> normalClips = new HashSet<Clip>();
	private List<UnmappedRead> tumourSplitReads = new ArrayList<UnmappedRead>();
	private List<UnmappedRead> normalSplitReads = new ArrayList<UnmappedRead>();
	private String name;
	private String type;
	private String strand;
	private String mateReference;
	private int mateBreakpoint;
	private String mateStrand;
	private String referenceKey;
	private BLATRecord blatRecord;
	private ConsensusRead consensusRead;
	private int posStrandCount;
	private int negStrandCount;	
	private int consensusLength;	
	private int splitConsensusReads;
	private int clipConsensusReads;
	private int minInsertSize;
	private int nonTempBases;
	private NavigableMap<Integer, List<UnmappedRead>> splitReadsMap;
	public Breakpoint(Integer position, String reference, boolean isLeft, int consensusLength, int minInsertSize) {
		this.breakpoint = position;
		this.reference = reference;
		this.isLeft = isLeft;
		this.consensusLength = consensusLength;
		this.minInsertSize = minInsertSize;
	}
	

	public Breakpoint() {
		
	}
	

	public String getStrand() {
		return this.strand;
	}

	public void setStrand(String strand) {
		this.strand = strand;
	}
	
	public String getType() {
		return type;
	}

	public String getMateReference() {
		return mateReference;
	}

	public void setMateReference(String mateReference) {
		this.mateReference = mateReference;
	}

	public String getMateStrand() {
		return mateStrand;
	}

	public void setMateStrand(String mateStrand) {
		this.mateStrand = mateStrand;
	}

	public String getReferenceKey() {
		return referenceKey;
	}

	public int getMateBreakpoint() {
		return mateBreakpoint;
	}

	public void setMateBreakpoint(int mateBreakpoint) {
		this.mateBreakpoint = mateBreakpoint;
	}	
	
	public boolean isLeft() {
		return isLeft;
	}

	public boolean isGermline() {
		return isGermline;
	}

	public void setGermline(boolean isGermline) {
		this.isGermline = isGermline;
	}


	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getBreakpoint() {
		return breakpoint;
	}


	public void setBreakpoint(Integer breakpointPosition) {
		this.breakpoint = breakpointPosition;
	}

	public HashSet<Clip> getTumourClips() {
		return tumourClips;
	}


	public void setTumourClips(HashSet<Clip> tumourClips) {
		this.tumourClips = tumourClips;
	}


	public HashSet<Clip> getNormalClips() {
		return normalClips;
	}


	public void setNormalClips(HashSet<Clip> normalClips) {
		this.normalClips = normalClips;
	}


	public void addTumourClip(Clip clip) {		
		tumourClips.add(clip);				
	}
	
	public void addNormalClip(Clip clip) {		
		normalClips.add(clip);		
	}
	
	public List<UnmappedRead> getTumourSplitReads() {
		return tumourSplitReads;
	}
	
	public List<UnmappedRead> getNormalSplitReads() {
		return normalSplitReads;
	}

	public int getPosStrandCount() {
		return posStrandCount;
	}

	public int getNegStrandCount() {
		return negStrandCount;
	}

	public BLATRecord getBlatRecord() {
		return this.blatRecord;
	}

	public boolean defineBreakpoint(int clipSize, boolean isRescue) throws Exception {
		if (tumourClips.size() > clipSize) {
			this.type = "somatic";
			if (this.normalClips.size() > 0) {
				this.isGermline = true;
				this.type = "germline";
			}
			calculateStrand();
			this.name = reference + "_" + breakpoint + "_" + isLeft + "_" + strand;
			if (posStrandCount > clipSize || negStrandCount > clipSize || isRescue) {					
				this.consensusRead = createContig(splitReadsMap, clipSize, isRescue);				
			}			
			splitReadsMap = null;
			if (consensusRead == null) {
				return false;
			} else {
				if (consensusRead.getClipMateSequenceLength() < consensusLength) {
					return false;
				} else if (highNCount(consensusRead.getClipMateSequence())) {
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean defineBreakpoint(int clipSize, boolean isRescue, NavigableMap<Integer, List<UnmappedRead>> splitReadsMap) throws Exception {
		this.splitReadsMap = splitReadsMap;
		return defineBreakpoint(clipSize, isRescue);
	}

	private boolean belowMinInsertSize() {
		
		if (this.reference.equals(mateReference)) {
			if (breakpoint == mateBreakpoint) {
				return true;
			} else if (breakpoint > mateBreakpoint) {
				if ((breakpoint - mateBreakpoint) <= minInsertSize) {
					return true;
				}
			} else {
				if ((mateBreakpoint - breakpoint) <= minInsertSize) {
					return true;
				}
			}
		}
		
		return false;
	}

	private ConsensusRead createContig(NavigableMap<Integer, List<UnmappedRead>> splitReadsMap, int clipSize, boolean isRescue) throws Exception {
		
		QSVAssemble assemble = new QSVAssemble();		
		ConsensusRead conRead = null;
		
		if (tumourClips != null) {
			
			List<Read> splitReads = new ArrayList<Read>();
			for (Clip c : tumourClips) {				
				splitReads.add(new Read("fullclip_" + c.getReadName(), c.getReadSequence()));
			}
			if (isGermline) {	
				for (Clip c : normalClips) {				
					splitReads.add(new Read("fullclip_" + c.getReadName(), c.getReadSequence()));
				}
			}
			if (splitReadsMap != null) {			
				
				//add clip contig sequence as seed				
				List<UnmappedRead> splits = new ArrayList<UnmappedRead>();		
				for (Entry<Integer, List<UnmappedRead>> entry: splitReadsMap.entrySet()) {						
					splits.addAll(entry.getValue());
				}
				
				this.tumourSplitReads = new ArrayList<UnmappedRead>();
				this.normalSplitReads = new ArrayList<UnmappedRead>();				
				
				int count = 0;

				for (UnmappedRead s: splits) {
					count++;
					if (count < 1000) {
						if (s.isTumour()) {
							tumourSplitReads.add(s);
							
							splitReads.add(new Read("split_" + s.getReadName(), s.getSequence()));
						} else {						
							if (isGermline) {	
								normalSplitReads.add(s);
								splitReads.add(new Read("split_" + s.getReadName(), s.getSequence()));
							}
						}
					}
				} 
				splits = null;
			}			
			conRead = calculateClipConsensus(clipSize);		
			
			if (conRead != null) {		
				ConsensusRead splitConRead = null;				
				if (strand.equals("+") && (posStrandCount > clipSize || isRescue) && conRead.getClipMateSequence().length() > consensusLength) {
					
					splitConRead = assemble.getContigs(breakpoint, new Read("clipFor", conRead.getClipMateSequence()), splitReads, false, isLeft);
					
				} else if (strand.equals("-") && (negStrandCount > clipSize || isRescue)){					
					splitConRead = assemble.getContigs(breakpoint, new Read("clipFor", QSVUtil.reverseComplement(conRead.getClipMateSequence())), splitReads, true, isLeft);									
				}
				this.clipConsensusReads = tumourClips.size() + normalClips.size();
				if (splitConRead != null) {					
					this.splitConsensusReads = assemble.getMatchingSplitReads();					
					if (!isRescue) {						
						//logger.info(isLeft + " " + strand + " assemble from " + this.reference + " " +  this.breakpoint + " " + splitConRead.getClipMateSequence() + " " + splitConsensusReads + " " + splitConRead.getClipMateSequence().length());
					}
					
					String header = splitConRead.getHeader();
					List<String> headers = Arrays.asList(header.split(","));
					
					tumourSplitReads = parseMatchingSplitReads(headers, tumourSplitReads);
					normalSplitReads = parseMatchingSplitReads(headers, normalSplitReads);
					
					assemble = null;
					return splitConRead;
				} else {
					this.splitConsensusReads = 0;
					if (!isRescue) {
						//logger.info(strand + " de novo from " + this.reference + " " +  this.breakpoint + " " + conRead.getClipMateSequence() + " " + splitConsensusReads + " " + conRead.getClipMateSequence().length());
					}
					tumourSplitReads = null;
					normalSplitReads = null;
					assemble = null;
					return conRead;
				}
			}
		}
		assemble = null;
		return conRead;
	}
	
	private List<UnmappedRead> parseMatchingSplitReads(List<String> headers, List<UnmappedRead> splitReadsList) {
		Iterator<UnmappedRead> i = splitReadsList.iterator();					
		while (i.hasNext()) {
			UnmappedRead r = i.next();
			String h = "split_" + r.getReadName();
			if (!headers.contains(h)) {
				i.remove();
			}
		}
		return splitReadsList;
	}

	private boolean highNCount(String consensus) {
		return QSVUtil.highNCount(consensus, 0.1);
	}

	private ConsensusRead calculateClipConsensus(int clipSize) throws Exception {
		int negLength = 0;
		int posLength = 0;
		int negReadLength = 0;
		int posReadLength = 0;
		
		for (Clip c: tumourClips) {
			int length = c.getLength();
			int readLength = c.getReferenceSequence().length();		
			if (c.getStrand().equals("+")) {
				if (length > posLength) {
					posLength = length;
				}
				if (readLength > posReadLength) {
					posReadLength = readLength;
				}
			} else {
				if (length > negLength) {
					negLength = length;
				}
				if (readLength > negReadLength) {
					negReadLength = readLength;
				}
			}
		}
		if (isGermline) {
			for (Clip c: normalClips) {
				int length = c.getLength();
				int readLength = c.getReferenceSequence().length();
				if (c.getStrand().equals("+")) {
					if (length > posLength) {
						posLength = length;
					}
					if (readLength > posReadLength) {
						posReadLength = readLength;
					}
				} else {
					if (length > negLength) {
						negLength = length;
					}
					if (readLength > negReadLength) {
						negReadLength = readLength;
					}
				}
			}
		}
		if (strand.equals("+")) {
			if (posLength > consensusLength && posStrandCount > clipSize) {
				return calculate("+", clipSize, posLength, posReadLength);
			}
		}
		if (strand.equals("-")) {
			if (negLength > consensusLength && negStrandCount > clipSize) {
				return calculate("-", clipSize, negLength, negReadLength);
			}
		}
		return null;
	}

	ConsensusRead calculate(String strand, int clipSize, int length, int readLength) throws Exception {
		String clipConsensus;
		String readConsensus = "";
		clipConsensus = calculateConsensus(strand, length, true);
		if (readLength > 0) {
			readConsensus = calculateConsensus(strand, readLength, false);
		}
		if (highNCount(clipConsensus) || clipConsensus.length() < consensusLength) {
			return null;
		} else {
			if (isLeft) {
				return new ConsensusRead("consensusread", clipConsensus+readConsensus, clipConsensus, readConsensus);
			} else {
				return new ConsensusRead("consensusread", readConsensus+clipConsensus, clipConsensus, readConsensus);
			}		
		}
	}

	String calculateConsensus(String strand, int length, boolean isClipConsensus) {
		
		
		int [][] bases = new int[length][5];		
		for (Clip c: tumourClips) {
			if (c.getStrand().equals(strand)) {
				if (isClipConsensus) {
					c.getClipBases(bases);
				} else {
					c.getReferenceBases(bases);
				}
			}
		}
		if (isGermline) {
			for (Clip c: normalClips) {
				if (c.getStrand().equals(strand)) {
					if (isClipConsensus) {
						c.getClipBases(bases);
					} else {
						c.getReferenceBases(bases);
					}
				}
			}
		}		
		
		String currentConsensus = getBaseCountString(bases);
		if (strand.equals("-")) {
			currentConsensus = QSVUtil.reverseComplement(currentConsensus);
		} 
		return currentConsensus;		
	}

	void calculateStrand() {
		this.posStrandCount = 0;
		this.negStrandCount = 0;
		for (Clip c : tumourClips) {
			if (c.getStrand().equals("+")) {				
					posStrandCount++;				
			}
			if (c.getStrand().equals("-")) {				
					negStrandCount++;			
			}
		}
		if (isGermline()) {
			for (Clip c : normalClips) {
				if (c.getStrand().equals("+")) {				
					posStrandCount++;				
				}
				if (c.getStrand().equals("-")) {				
					negStrandCount++;				
				}
			}
		}
		
		if (posStrandCount==negStrandCount) {
			this.strand = "+";
		} else if (posStrandCount > negStrandCount) {
			strand = "+";
		} else {
			strand = "-";
		}		
	}	
	
	String getBaseCountString(int[][] bases) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<bases.length; i++) {
			int max = 0;
			int b = -1;
			int maxNo = 0;
			int count = 0;
			for (int j=0; j<5; j++) {				
				if (bases[i][j] > max) {
					max = bases[i][j];
					b = j;
				} else {
					if (bases[i][j] == max && max != 0) {
						maxNo++;
					}
				}
			}
			
			char base;			
			if (maxNo > 0) {
				base = 'N';
			} else {
				if (b == 0) {
					base = 'A';
				} else if (b == 1) {
					base = 'C';
				} else if (b == 2) {
					base = 'T';
				} else if (b == 3) {
					base = 'G';
				} else {
					if (count != 1 && i != bases.length-1) {						
						base = 'N';
					} else {	
						break;
					}
				}
			}
			
			builder.append(base);
		}
		return builder.toString();
	}

	
	public boolean findMateBreakpoint(BLATRecord mateRecord) {
		Integer mateBp = null;
		
		if (mateRecord != null) {
			if (reference.contains("chr") && mateRecord.getReference().contains("chr")) {
				mateBp =  mateRecord.calculateMateBreakpoint(isLeft, reference, breakpoint, strand);
				if (mateBp != null) {	
					
					mateReference = mateRecord.getReference();
					mateStrand = mateRecord.getStrand();
					mateBreakpoint = mateBp;
					blatRecord = mateRecord;
					nonTempBases = mateRecord.getNonTempBases();
					boolean reorder = QSVUtil.reorderByChromosomes(reference,
							mateReference);
					if (reorder) {
						this.referenceKey = mateReference + ":" + reference;
					} else {
						this.referenceKey = reference + ":" + mateReference;
					}
					if (!belowMinInsertSize()) {
						return true;
					}					
				}			
			}
		} 	
		return false;
	}

	@Override
	public int compareTo(Breakpoint o) {
		if (this.breakpoint.equals(o.getBreakpoint())) {			
			return this.name.compareTo(o.getName());			 
		} else {
			return this.breakpoint.compareTo(o.getBreakpoint());
		}
	}
	
	public int getClipsSize() {
		if (this.isGermline) {
			return tumourClips.size() + normalClips.size();
		} else {
			return tumourClips.size();
		}
	}

	public Integer compare(String queryReference, Integer queryBreakpoint) {
		// chromosomes match
		if (queryReference.equals(mateReference)) {

			if (queryBreakpoint != null) {
				if (queryBreakpoint >= (mateBreakpoint - 10)
						&& queryBreakpoint <= mateBreakpoint + 10) {
					mateBreakpoint = queryBreakpoint;
					return new Integer(queryBreakpoint);
				}
			}
		}
		return null;
	}
	
	public String getTumourClipString() {
		StringBuilder b = new StringBuilder();
		for (Clip pos : tumourClips) {
			b.append(pos.toString());
		}
		return b.toString();
	}

	public String getNormalClipString() {
		StringBuilder b = new StringBuilder();
		for (Clip pos : normalClips) {
			b.append(pos.toString());
		}
		return b.toString();
	}

	public boolean findRescuedMateBreakpoint(BLAT blat, QSVParameters p, String softclipDir) throws Exception {
		
		String base = softclipDir + QSVUtil.getFileSeparator() + this.name;
		String fa = base + ".fa";
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fa)));
		String mateConsensus = getMateConsensus();
		if (mateConsensus != null) {
			if (mateConsensus.length() > 0) {
				writer.write(">" + getName() + NEWLINE);
				writer.write(mateConsensus + NEWLINE);
				writer.write(NEWLINE);
			} 
		}
				
		writer.close();
		
		String outFile = base + ".psl";
		
		Map<String, BLATRecord> blatMap = blat.align(fa, outFile);
		
		BLATRecord record = null;

		if (blatMap.containsKey(getName())) {
			record = blatMap.get(getName());	
		}
		
		return findMateBreakpoint(record);
	}

	public String getMateConsensus() {
		//consensus sequence from clip breakpoint
		if (consensusRead != null) {
			return consensusRead.getClipMateSequence();
		} 
		return null;
	}
	
	public String getCompleteConsensus() {
		//consensus sequence from crossing the breakpoint
		if (consensusRead != null) {
			if (strand.equals("+")) {
				return consensusRead.getSequence();
			} else {
				return QSVUtil.reverseComplement(consensusRead.getSequence());
			}
		} 
		return null;
	}
	
	public String getBreakpointConsenus() {
		//consensus sequence aligning to current breakpoint
		if (consensusRead != null) {
			if (strand.equals("+")) {
				return consensusRead.getReferenceSequence();
			} else {
				return QSVUtil.reverseComplement(consensusRead.getReferenceSequence());
			}
		} 
		return null;
	}

	public boolean isRescued() {
		return rescued;
	}

	public void setRescued(boolean rescued) {
		this.rescued = rescued;
	}

	public String toLowConfidenceString() {
		StringBuilder builder = new StringBuilder();

		String side = "right";
		
		if (this.isLeft) {
			side = "left";			
		}
		
		String type = "somatic";
		
		if (this.isGermline) {
			type = "germline";
		}
		
		builder.append(this.reference + TAB + this.breakpoint + TAB + type + TAB + side + TAB);
		
		builder.append(this.strand + TAB + posStrandCount + TAB + negStrandCount + TAB + getMateConsensus());
		
		builder.append(NEWLINE);
		
		return builder.toString();		
	}

	public void setNonBlatAligned() {
		if (posStrandCount >= LOW_CONF_CLIPS || negStrandCount >= LOW_CONF_CLIPS) {
			this.nonBlatAligned = true;
		}
	}

	public boolean isNonBlatAligned() {
		return this.nonBlatAligned;
	}

	public boolean getMatchingStrands() {
		return strand.equals(mateStrand);
	}

	public String getContigInfo() {
		String tab = "\t";
		StringBuilder sb = new StringBuilder();
		
		sb.append(getMateConsensus()).append(tab);
		sb.append(getStrand()).append(tab);
		sb.append(mateReference).append(tab);
		sb.append(mateBreakpoint).append(tab);
		sb.append(blatRecord.getStartPos()).append(tab);
		sb.append(blatRecord.getEndPos()).append(tab);
		sb.append(blatRecord.getQueryStart()).append(tab);
		sb.append(blatRecord.getQueryEnd()).append(tab);
		sb.append(mateStrand);
		
		return sb.toString();
	}

	public void setBlatRecord(BLATRecord blatRecord2) {
		this.blatRecord = blatRecord2;		
	}

	public boolean getMaxBreakpoint(int buffer, TreeMap<Integer, List<UnmappedRead>> splitReads, int maxBpLength) throws Exception {
		Integer start = breakpoint - buffer;
		Integer end  = breakpoint + buffer;
		NavigableMap<Integer, List<UnmappedRead>> splitReadsMap = splitReads.subMap(start, true, end, true);
		
		defineBreakpoint(0, true, splitReadsMap);	
		
		if (getMateConsensus() != null) {
			if (getMateConsensus().length() > maxBpLength) {
				return true;
			}
		}
		return false;
	}

	public ConsensusRead getConsensusRead() {
		return this.consensusRead;
	}
	public void setConsensusRead(ConsensusRead consensusRead) {
		this.consensusRead = consensusRead;
	}


	public int getSplitReadsSize() {
		return tumourSplitReads.size() + normalSplitReads.size();
	}
	
	public int getSplitConsensusReads() {
		return splitConsensusReads;
	}

	public int getClipConsensusReads() {
		return clipConsensusReads;
	}

	public String getMateConsensusPosStrand() {
		if (this.strand.equals("+")) {
			return consensusRead.getClipMateSequence();
		} else {
			return QSVUtil.reverseComplement(consensusRead.getClipMateSequence());
		}
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}

	public void addSplitReadsMap(
			NavigableMap<Integer, List<UnmappedRead>> splitReadsMap2) {
		this.splitReadsMap = splitReadsMap2;
		
	}

	public boolean isTranslocation() {
		return ! this.reference.equals(mateReference);
	}

}

