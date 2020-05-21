/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import static org.qcmg.common.util.Constants.TAB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.assemble.Read;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.tiledaligner.TiledAlignerUtil;
import org.qcmg.qsv.util.QSVUtil;

import gnu.trove.map.TIntObjectMap;

public class Breakpoint implements Comparable<Breakpoint>{
	
	@Override
	public String toString() {
		return "Breakpoint [consensusLength=" + consensusLength + ", breakpoint=" + breakpoint + ", isLeft=" + isLeft
				+ ", minInsertSize=" + minInsertSize + ", reference=" + reference + ", isGermline=" + isGermline
				+ ", tumourClips=" + tumourClips + ", normalClips=" + normalClips + ", tumourSplitReads="
				+ tumourSplitReads + ", normalSplitReads=" + normalSplitReads + ", positiveStrand=" + positiveStrand
				+ ", mateReference=" + mateReference + ", mateBreakpoint=" + mateBreakpoint + ", matePositiveStrand="
				+ matePositiveStrand + ", referenceKey=" + referenceKey + ", blatRecord=" + blatRecord
				+ ", consensusRead=" + consensusRead + ", posStrandCount=" + posStrandCount + ", negStrandCount="
				+ negStrandCount + ", splitConsensusReads=" + splitConsensusReads + ", clipConsensusReads="
				+ clipConsensusReads + ", nonTempBases=" + nonTempBases + ", splitReadsMap=" + splitReadsMap + "]";
	}

	private static final String CONSENSUS_READ = "consensusread";
	private static final String SPLIT = "split_";
	private static final String CLIP_FOR = "clipFor";
	private static final String FULLCLIP = "fullclip_";
	private static final int LOW_CONF_CLIPS = 10;
	private static final QLogger logger = QLoggerFactory.getLogger(Breakpoint.class);
	
	private final int consensusLength;
	private final int breakpoint;
	private final boolean isLeft;
	private final int minInsertSize;
	private final String reference;
	
	private boolean isGermline;
	private final Set<Clip> tumourClips;
	private final Set<Clip> normalClips;
	private List<UnmappedRead> tumourSplitReads = new ArrayList<>();
	private List<UnmappedRead> normalSplitReads = new ArrayList<>();
	private boolean positiveStrand;
	private String mateReference;
	private int mateBreakpoint;
	private boolean matePositiveStrand;
	private String referenceKey;
	private BLATRecord blatRecord;
	private ConsensusRead consensusRead;
	private int posStrandCount;
	private int negStrandCount;	
	private int splitConsensusReads;
	private int clipConsensusReads;
	private int nonTempBases;
	private NavigableMap<Integer, List<UnmappedRead>> splitReadsMap;
	
	public Breakpoint(Integer position, String reference, boolean isLeft, int consensusLength, int minInsertSize) {
		this.breakpoint = position;
		this.reference = reference;
		this.isLeft = isLeft;
		this.consensusLength = consensusLength;
		this.minInsertSize = minInsertSize;
		this.tumourClips = new HashSet<>();
		this.normalClips = new HashSet<>();
	}

	public char getStrand() {
		return positiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS;
	}

	public void setStrand(char strand) {
		positiveStrand = strand == QSVUtil.PLUS;
	}
	
	public String getType() {
		return isGermline ? "germline" : "somatic";
	}

	public String getMateReference() {
		return mateReference;
	}

	public void setMateReference(String mateReference) {
		this.mateReference = mateReference;
	}

	public char getMateStrand() {
		return matePositiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS;
	}

	public void setMateStrand(char mateStrand) {
		matePositiveStrand = mateStrand == QSVUtil.PLUS;
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

	public String getName() {
		return reference + "_" + breakpoint + "_" + isLeft + "_" + (positiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS);
	}

	public Integer getBreakpoint() {
		return breakpoint;
	}

	public Set<Clip> getTumourClips() {
		return tumourClips;
	}

	public Set<Clip> getNormalClips() {
		return normalClips;
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

	public boolean defineBreakpoint(int clipSize, boolean isRescue, boolean log) throws Exception {
		if (tumourClips.size() > clipSize) {
			this.isGermline = this.normalClips.size() > 0 ;
			calculateStrand();
			if (posStrandCount > clipSize || negStrandCount > clipSize || isRescue) {
				if (log) {
					logger.info("defineBreakpoint: about to call createContig, splitReadsMap.size: " + splitReadsMap.size() + ", clipSize: " + clipSize + ", isRescue: " + isRescue);
					logger.info("splitReadsMap: ");
					for (Entry<Integer, List<UnmappedRead>> entry : splitReadsMap.entrySet()) {
						logger.info("splitReadsMap: entry.getKey(): " + entry.getKey());
						logger.info("splitReadsMap: entry.getValue(): " + entry.getValue().stream().map(UnmappedRead::toString).collect(Collectors.joining(",")));
					}
				}
				this.consensusRead = createContig(splitReadsMap, clipSize, isRescue, log);
				if (log) {
					logger.info("defineBreakpoint: consensusRead: " + consensusRead);
				}
			}
			splitReadsMap = null;
			
			if (log) {
				logger.info("defineBreakpoint: consensusRead.getClipMateSequenceLength(): " + (null != consensusRead ? consensusRead.getClipMateSequenceLength() : 0) + ", consensusLength: " + consensusLength + ", consensusRead.getClipMateSequence(): " + (null != consensusRead ? consensusRead.getClipMateSequence() : "null"));
			}
			
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
		return defineBreakpoint(clipSize, isRescue, splitReadsMap, false);
	}
	
	public boolean defineBreakpoint(int clipSize, boolean isRescue, NavigableMap<Integer, List<UnmappedRead>> splitReadsMap, boolean log) throws Exception {
		this.splitReadsMap = splitReadsMap;
		return defineBreakpoint(clipSize, isRescue, log);
	}

	private boolean belowMinInsertSize() {
		if (this.reference.equals(mateReference)) {
			return belowMinInsertSize(breakpoint, mateBreakpoint, minInsertSize);
		}
		return false;
	}
	
	public static boolean belowMinInsertSize(int breakpoint, int mateBp, int minIS) {
		return breakpoint == mateBp
				|| Math.abs(mateBp - breakpoint) <= minIS;
	}

	private ConsensusRead createContig(NavigableMap<Integer, List<UnmappedRead>> splitReadsMap, int clipSize, boolean isRescue, boolean log) throws Exception {
		
		QSVAssemble assemble = new QSVAssemble();		
		ConsensusRead conRead = null;
		
		if (tumourClips != null) {
			
			List<Read> splitReads = new ArrayList<>();
			for (Clip c : tumourClips) {
				if (c.getReadSequence().length() < QSVAssemble.SEED_LENGTH) {
					logger.warn("c.getReadSequence().length(): " + c.getReadSequence().length() + ", c.getReadSequence(): " + c.getReadSequence() + " for clip: " + c.toString());
				}
				splitReads.add(new Read(FULLCLIP + c.getReadName(), c.getReadSequence()));
			}
			if (isGermline) {
				for (Clip c : normalClips) {
					splitReads.add(new Read(FULLCLIP + c.getReadName(), c.getReadSequence()));
				}
			}
			if (splitReadsMap != null) {
				
				//add clip contig sequence as seed				
				this.tumourSplitReads = new ArrayList<>();
				this.normalSplitReads = new ArrayList<>();
				
				int count = 0;
				
				for (Entry<Integer, List<UnmappedRead>> entry : splitReadsMap.entrySet()) {
					for (UnmappedRead s : entry.getValue()) {
						
						if (count++ > 1000) break;
						
						if (s.isTumour()) {
							tumourSplitReads.add(s);
							
							if (s.getSequence().length() < QSVAssemble.SEED_LENGTH) {
								logger.warn("s.getSequence().length(): " +s.getSequence().length() + ", s.getSequence().length(): " + s.getSequence() + " for unmapped read: " + s.toString());
							}
							
							splitReads.add(new Read(SPLIT + s.getReadName(), s.getSequence()));
						} else {
							if (isGermline) {
								normalSplitReads.add(s);
								splitReads.add(new Read(SPLIT + s.getReadName(), s.getSequence()));
							}
						}
					}
				}
			}
			
			if (log) {
				logger.info("about to call calculateClipConsensus with clipSize: " + clipSize + ", isGermline: " + isGermline + ", isLeft: " + isLeft + ", consensusLength: " + consensusLength + ", positiveStrand: " + positiveStrand + ", posStrandCount: " + posStrandCount + ", negStrandCount: " +negStrandCount);
				logger.info("tumourClips: " + tumourClips.stream().map(Clip::toString).collect(Collectors.joining(",")));
				logger.info("normalClips: " + normalClips.stream().map(Clip::toString).collect(Collectors.joining(",")));
			}
			
			conRead = calculateClipConsensus(clipSize, tumourClips, normalClips, isGermline, isLeft, consensusLength, positiveStrand, posStrandCount, negStrandCount);		
			
			if (conRead != null) {
				ConsensusRead splitConRead = null;				
				if (positiveStrand && (posStrandCount > clipSize || isRescue) && conRead.getClipMateSequence().length() > consensusLength) {
					
					splitConRead = assemble.getContigs(breakpoint, new Read(CLIP_FOR, conRead.getClipMateSequence()), splitReads, false, isLeft);
					
				} else if ( ! positiveStrand && (negStrandCount > clipSize || isRescue)){					
					splitConRead = assemble.getContigs(breakpoint, new Read(CLIP_FOR, QSVUtil.reverseComplement(conRead.getClipMateSequence())), splitReads, true, isLeft);									
				}
				this.clipConsensusReads = tumourClips.size() + normalClips.size();
				if (splitConRead != null) {
					this.splitConsensusReads = assemble.getMatchingSplitReads();					
					if (!isRescue) {						
						//logger.info(isLeft + " " + strand + " assemble from " + this.reference + " " +  this.breakpoint + " " + splitConRead.getClipMateSequence() + " " + splitConsensusReads + " " + splitConRead.getClipMateSequence().length());
					}
					
					String header = splitConRead.getHeader();
					String[] headers = header.split(",");
					Arrays.sort(headers);
//					List<String> headers = Arrays.asList(header.split(","));
					
					tumourSplitReads = parseMatchingSplitReads(headers, tumourSplitReads);
					normalSplitReads = parseMatchingSplitReads(headers, normalSplitReads);
					
					return splitConRead;
				} else {
					this.splitConsensusReads = 0;
					if (!isRescue) {
						//logger.info(strand + " de novo from " + this.reference + " " +  this.breakpoint + " " + conRead.getClipMateSequence() + " " + splitConsensusReads + " " + conRead.getClipMateSequence().length());
					}
					tumourSplitReads = null;
					normalSplitReads = null;
					return conRead;
				}
			}
		}
		return conRead;
	}
	
	public static List<UnmappedRead> parseMatchingSplitReads(String[] headers, List<UnmappedRead> splitReadsList) {
		Iterator<UnmappedRead> i = splitReadsList.iterator();					
		while (i.hasNext()) {
			UnmappedRead r = i.next();
			if (Arrays.binarySearch(headers, SPLIT + r.getReadName()) < 0) {
				i.remove();
			}
		}
		return splitReadsList;
	}
//	public static List<UnmappedRead> parseMatchingSplitReads(List<String> headers, List<UnmappedRead> splitReadsList) {
//		Iterator<UnmappedRead> i = splitReadsList.iterator();					
//		while (i.hasNext()) {
//			UnmappedRead r = i.next();
//			String h = SPLIT + r.getReadName();
//			if ( ! headers.contains(h)) {
//				i.remove();
//			}
//		}
//		return splitReadsList;
//	}

	public static boolean highNCount(String consensus) throws QSVException {
		return QSVUtil.highNCount(consensus, 0.1);
	}

	public static ConsensusRead calculateClipConsensus(int clipSize, Set<Clip> tumourClips, Set<Clip> normalClips, boolean isGermline, boolean isLeft, int consensusLength, boolean positiveStrand, int posStrandCount, int negStrandCount) throws Exception {
		if (positiveStrand && posStrandCount <= clipSize) {
			return null;
		} else if ( ( ! positiveStrand) && negStrandCount <= clipSize) {
			return null;
		}
		
		int negLength = 0;
		int posLength = 0;
		int negReadLength = 0;
		int posReadLength = 0;
		
		for (Clip c: tumourClips) {
			if (QSVUtil.PLUS == c.getStrand()) {
				if (c.getLength() > posLength) {
					posLength = c.getLength();
				}
				if (c.getReferenceSequence().length() > posReadLength) {
					posReadLength = c.getReferenceSequence().length();
				}
			} else {
				if (c.getLength() > negLength) {
					negLength = c.getLength();
				}
				if ( c.getReferenceSequence().length() > negReadLength) {
					negReadLength = c.getReferenceSequence().length();
				}
			}
		}
		if (isGermline) {
			for (Clip c: normalClips) {
				
				if (QSVUtil.PLUS == c.getStrand()) {
					if (c.getLength() > posLength) {
						posLength = c.getLength();
					}
					if (c.getReferenceSequence().length() > posReadLength) {
						posReadLength = c.getReferenceSequence().length();
					}
				} else {
					if (c.getLength() > negLength) {
						negLength = c.getLength();
					}
					if (c.getReferenceSequence().length() > negReadLength) {
						negReadLength = c.getReferenceSequence().length();
					}
				}
			}
		}
		if (positiveStrand) {
			if (posLength > consensusLength) {
				return calculate(QSVUtil.PLUS, clipSize, posLength, posReadLength, tumourClips, normalClips, isGermline, isLeft, consensusLength);
			}
		} else {
			if (negLength > consensusLength) {
				return calculate(QSVUtil.MINUS, clipSize, negLength, negReadLength, tumourClips, normalClips, isGermline, isLeft, consensusLength);
			}
		}
		return null;
	}

	public static ConsensusRead calculate(char strand, int clipSize, int length, int readLength, Set<Clip> tumourClips, Set<Clip> normalClips, boolean isGermline, boolean isLeft, int consensusLength) throws QSVException {
		String readConsensus = readLength > 0 ? calculateConsensus(strand, readLength, false, tumourClips, normalClips, isGermline) : "";
		String clipConsensus = calculateConsensus(strand, length, true, tumourClips, normalClips, isGermline);
		if (highNCount(clipConsensus) || clipConsensus.length() < consensusLength) {
			return null;
		} else {
			if (isLeft) {
				return new ConsensusRead(CONSENSUS_READ, clipConsensus+readConsensus, clipConsensus, readConsensus);
			} else {
				return new ConsensusRead(CONSENSUS_READ, readConsensus+clipConsensus, clipConsensus, readConsensus);
			}		
		}
	}

	public static String calculateConsensus(char strand, int length, boolean isClipConsensus, Set<Clip> tumourClips, Set<Clip> normalClips, boolean isGermline) {
		
		int [][] bases = new int[length][5];
		for (Clip c: tumourClips) {
			if (strand == c.getStrand()) {
				if (isClipConsensus) {
					Clip.getClipBases(bases, c.isLeft(), c.getClipSequence());
				} else {
					Clip.getReferenceBases(bases, c.isLeft(), c.getReferenceSequence());
				}
			}
		}
		if (isGermline) {
			for (Clip c: normalClips) {
				if (strand == c.getStrand()) {
					if (isClipConsensus) {
						Clip.getClipBases(bases, c.isLeft(), c.getClipSequence());
					} else {
						Clip.getReferenceBases(bases, c.isLeft(), c.getReferenceSequence());
					}
				}
			}
		}		
		
		String currentConsensus = getBaseCountString(bases);
		if (QSVUtil.MINUS == strand) {
			currentConsensus = QSVUtil.reverseComplement(currentConsensus);
		} 
		return currentConsensus;		
	}

	void calculateStrand() {
		this.posStrandCount = 0;
		this.negStrandCount = 0;
		for (Clip c : tumourClips) {
			if (QSVUtil.PLUS == c.getStrand()) {				
				posStrandCount++;				
			} else {
				negStrandCount++;			
			}
		}
		if (isGermline()) {
			for (Clip c : normalClips) {
				if (QSVUtil.PLUS == c.getStrand()) {				
					posStrandCount++;				
				} else {
					negStrandCount++;				
				}
			}
		}
		
		positiveStrand = posStrandCount >= negStrandCount;
	}	
	
	public static String getBaseCountString(int[][] bases) {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < bases.length; i++) {
			int max = 0;;
			int b = -1;
			int maxNo = 0;
//			max = 0;
//			b = -1;
//			maxNo = false;

//			int maxNo = 0;
			for (int j = 0; j < 5; j++) {				
				if (bases[i][j] > max) {
					max = bases[i][j];
					b = j;
				} else {
					if ( max != 0 && bases[i][j] == max) {
//						if ( ! maxNo && max != 0 && bases[i][j] == max) {
//						maxNo = true;
						maxNo++;
					}
				}
			}
			
//			char base;			
//			if (maxNo) {
////				if (maxNo > 0) {
//				builder.append(QSVUtil.N);
//			} else {
//				if (b == 0) {
//					builder.append(QSVUtil.A);
//				} else if (b == 1) {
//					builder.append(QSVUtil.C);
//				} else if (b == 2) {
//					builder.append(QSVUtil.T);
//				} else if (b == 3) {
//					builder.append(QSVUtil.G);
//				} else {
//					if (i != bases.length-1) {						
//						builder.append(QSVUtil.N);
//					} else {	
//						break;
//					}
//				}
//			}
			
			char base;			
//			if (maxNo) {
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
					if (i != bases.length-1) {						
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
		
		if (mateRecord != null) {
			
//			if (reference.startsWith(Constants.CHR) && mateRecord.getReference().startsWith(Constants.CHR)) {
				Integer mateBp =  mateRecord.calculateMateBreakpoint(isLeft, reference, breakpoint, (positiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS));
				if (mateBp != null) {
					
					mateReference = mateRecord.getReference();
					setMateStrand(mateRecord.getStrand());
					mateBreakpoint = mateBp;
					blatRecord = mateRecord;
					nonTempBases = mateRecord.getNonTempBases();
					boolean reorder = QSVUtil.reorderByChromosomes(reference, mateReference);
					this.referenceKey = reorder ? mateReference + ":" + reference : reference + ":" + mateReference;
					if ( ! belowMinInsertSize()) {
						return true;
					}
				}
//			}
		}
		return false;
	}

	@Override
	public int compareTo(Breakpoint o) {
		int diff = Integer.compare(breakpoint, o.getBreakpoint());
		if (diff == 0) {			
			return getName().compareTo(o.getName());			 
		} else {
			return diff;
		}
	}
	
	public int getClipsSize() {
		if (this.isGermline) {
			return tumourClips.size() + normalClips.size();
		} else {
			return tumourClips.size();
		}
	}
	
	public static Integer compare (String queryReference, Integer queryBreakpoint, String mateReference, int mateBreakpoint) {
		// chromosomes match
		if (queryReference.equals(mateReference)) {

			if (queryBreakpoint != null) {
				if (queryBreakpoint.intValue() >= (mateBreakpoint - 10)
						&& queryBreakpoint.intValue() <= mateBreakpoint + 10) {
					return queryBreakpoint;
				}
			}
		}
		return null;
	}

	public Integer compare(String queryReference, Integer queryBreakpoint) {
		Integer i = compare(queryReference, queryBreakpoint, mateReference, mateBreakpoint);
		if (null != i) {
			mateBreakpoint = i;
			return i;
		}
//		// chromosomes match
//		if (queryReference.equals(mateReference)) {
//
//			if (queryBreakpoint != null) {
//				if (queryBreakpoint.intValue() >= (mateBreakpoint - 10)
//						&& queryBreakpoint.intValue() <= mateBreakpoint + 10) {
//					mateBreakpoint = queryBreakpoint.intValue();
//					return queryBreakpoint;
//				}
//			}
//		}
		return null;
	}
	
	public String getTumourClipString() {
		return getClipString(tumourClips);
	}

	public String getNormalClipString() {
		return getClipString(normalClips);
	}
	
	public static String getClipString(Set<Clip> clips) {
		return clips.stream().map(Clip::toString).collect(Collectors.joining(""));
	}

	public boolean findRescuedMateBreakpoint(TIntObjectMap<int[]> cache, QSVParameters p, String softclipDir) throws Exception {
		String mateConsensus = getMateConsensus();
		
		if ( ! StringUtils.isNullOrEmpty(mateConsensus) ) {
			System.out.println("looking for " + mateConsensus);
			Map<String, String> seqNameMap = new HashMap<>(2);
			seqNameMap.put(mateConsensus, getName());
			Map<String, List<BLATRecord>> blatMap = TiledAlignerUtil.runTiledAlignerCache(cache, seqNameMap, 13, "Breakpoint.findRescuedMateBreakpoint", false);
			if (null != blatMap && ! blatMap.isEmpty()) {
				List<BLATRecord> recs = blatMap.get(mateConsensus);
				if (null != recs && ! recs.isEmpty()) {
					return findMateBreakpoint(recs.get(recs.size() - 1));
				}
			}
		}
		return false;
	}
	public boolean findRescuedMateBreakpoint(BLAT blat, QSVParameters p, String softclipDir) throws Exception {
		
		String base = softclipDir + QSVUtil.getFileSeparator() + getName();
		String fa = base + ".fa";
		String mateConsensus = getMateConsensus();
		
		if ( ! StringUtils.isNullOrEmpty(mateConsensus) ) {
			
			try (FileWriter fw = new FileWriter(new File(fa));
					BufferedWriter writer = new BufferedWriter(fw);) {
				
				writer.write(">" + getName() + QSVUtil.NEW_LINE);
				writer.write(mateConsensus + QSVUtil.NEW_LINE);
				writer.write(QSVUtil.NEW_LINE);
			}
		}
		
		String outFile = base + ".psl";
		
		Map<String, BLATRecord> blatMap = blat.align(fa, outFile);
		
		BLATRecord record = blatMap.get(getName());
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
			if (positiveStrand) {
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
			if (positiveStrand) {
				return consensusRead.getReferenceSequence();
			} else {
				return QSVUtil.reverseComplement(consensusRead.getReferenceSequence());
			}
		} 
		return null;
	}

	/**
	 * It doesn't look like anything sets this to true, so always returns false
	 * @return
	 */
	public boolean isRescued() {
		return false;
	}

	public void setRescued(boolean rescued) {
//		this.rescued = rescued;
	}

	public String toLowConfidenceString() {
		StringBuilder builder = new StringBuilder();

		builder.append(this.reference).append(TAB);
		builder.append(this.breakpoint).append(TAB);
		builder.append( (isGermline ? "germline" : "somatic") ).append(TAB);
		builder.append(  (isLeft ? "left" : "right")).append(TAB);
		builder.append( (positiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS) ).append(TAB);
		builder.append( posStrandCount).append(TAB);
		builder.append( negStrandCount).append(TAB);
		builder.append( getMateConsensus());
		builder.append(QSVUtil.NEW_LINE);
		
		return builder.toString();		
	}

	public boolean isNonBlatAligned() {
		return posStrandCount >= LOW_CONF_CLIPS || negStrandCount >= LOW_CONF_CLIPS;
	}

	public boolean getMatchingStrands() {
		return positiveStrand ==  matePositiveStrand;
	}

	public String getContigInfo() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(getMateConsensus()).append(TAB);
		sb.append(getStrand()).append(TAB);
		sb.append(mateReference).append(TAB);
		sb.append(mateBreakpoint).append(TAB);
		sb.append(blatRecord.getStartPos()).append(TAB);
		sb.append(blatRecord.getEndPos()).append(TAB);
		sb.append(blatRecord.getQueryStart()).append(TAB);
		sb.append(blatRecord.getQueryEnd()).append(TAB);
		sb.append(matePositiveStrand ? QSVUtil.PLUS : QSVUtil.MINUS);
		
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
		if (positiveStrand) {
			return consensusRead.getClipMateSequence();
		} else {
			return QSVUtil.reverseComplement(consensusRead.getClipMateSequence());
		}
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}

	public void addSplitReadsMap(NavigableMap<Integer, List<UnmappedRead>> splitReadsMap2) {
		this.splitReadsMap = splitReadsMap2;
	}

	public boolean isTranslocation() {
		return ! this.reference.equals(mateReference);
	}
}
