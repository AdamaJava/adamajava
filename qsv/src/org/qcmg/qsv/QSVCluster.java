/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.QPrimerCategory;
import org.qcmg.qsv.softclip.SoftClipCluster;
import org.qcmg.qsv.splitread.SplitReadContig;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import gnu.trove.map.TIntObjectMap;


/**
 * Class representing a structural variant. Can contain discordant pair evidence, soft clip and
 * split read contig evidence. 
 *
 */
public class QSVCluster {
	
	private static final QLogger logger = QLoggerFactory.getLogger(QSVCluster.class);
	private static final  char TAB = '\t';
	
	private final DiscordantPairCluster pairRecord;
	private List<SoftClipCluster> clipRecords;
	private boolean isGermline = false;
	private String sampleId;
	private final String leftReference;
	private final String rightReference;
	private String leftReferenceFlank = new String();
	private String rightReferenceFlank = new String();
	private String svId;
	private String analysisId;
	private String consensus = new String();
	private boolean rescued;
	private SplitReadContig splitReadContig;
	private boolean potentialRepeatRegion;
	private SplitReadContig normalSplitReadContig;
	private String features = new String();
	
	public QSVCluster(SoftClipCluster clipRecord, String sampleId) {
		this.clipRecords = new ArrayList<>();
		clipRecords.add(clipRecord);
		this.pairRecord = null;
		this.sampleId = sampleId;
		clipRecord.checkOrder();
		this.isGermline = clipRecord.isGermline();
		this.leftReference = clipRecord.getLeftReference();
		this.rightReference = clipRecord.getRightReference();		
	}

	public QSVCluster(DiscordantPairCluster clusterRecord, boolean isNormalGermline, String sampleId) {
		this.pairRecord = clusterRecord;
		this.clipRecords = null;
		this.sampleId = sampleId;
		this.leftReference = clusterRecord.getLeftReferenceName();
		this.rightReference = clusterRecord.getRightReferenceName();
		if ( ! isNormalGermline) {
			findGermline();
		}
	}	

	public void setSplitReadContig(SplitReadContig splitReadContig) {
		this.splitReadContig = splitReadContig;
	}

	public boolean isGermline() {
		return isGermline;
	}

	public void setGermline(boolean isGermline) {
		this.isGermline = isGermline;
	}
	
	public DiscordantPairCluster getPairRecord() {
		return pairRecord;
	}
	
	public String getLeftReference() {
		return this.leftReference;
	}
	
	public String getRightReference() {
		return this.rightReference;
	}
	
	private int getLeftBps() {
		return getPrimarySoftClipCluster().getRealLeftBreakpoint();
	}
	
	private int getRightBps() {
		return getPrimarySoftClipCluster().getRealRightBreakpoint();
	}

	public List<SoftClipCluster> getClipRecords() {
		return clipRecords;
	}
	
	public String getReference() {
		String left = getLeftReference();
		String right = getRightReference();
		
		if (left!= null && right!= null) {
			if (left.equals(right)) {
				return left;
			} else {
				return "interchromosomal";
			}
		} else if (left != null) {
			return left;
		} else {
			return "unknown";
		}
	}
	
	/**
	 * @return the left breakpoint for the cluster
	 */
	public int getLeftBreakpoint() {		
		return clipRecords == null ? pairRecord.getLeftBreakPoint() : getLeftBps();
	}
	
	/**
	 * 
	 * @return the right breakpoint for the cluster
	 */
	public int getRightBreakpoint() {
		return clipRecords == null ? pairRecord.getRightBreakPoint() : getRightBps();
	}	
	
	/**
	 * For the final breakpoint, category 2 breakpoints are swapped. 
	 * Used when writing results to output files
	 * @return the final left breakpoint for the cluster
	 */
	public int getFinalLeftBreakpoint() {				
		return getOrientationCategory().equals(QSVConstants.ORIENTATION_2) ? getFinalRight() : getFinalLeft();			
	}
	
	/*
	 * Left breakpoint - final value. 
	 */
	private int getFinalLeft() {
		//split read contig breakpoint
		if (isPairWithSplitRead()) {
			return splitReadContig.getUnsortedLeftBreakpoint().intValue();
		//clip record breakpoint
		} else if (clipRecords == null) {
			return pairRecord.getLeftBreakPoint();
		} else {			
			//discordant pair breakpoint
			return getLeftBps();
		}
	}
	
	/*
	 * Right breakpoint - final value. 
	 */
	private int getFinalRight() {
		//split read contig breakpoint
		if (isPairWithSplitRead()) {
			return splitReadContig.getUnsortedRightBreakpoint().intValue();
			//clip record breakpoint
		} else if (clipRecords == null) {
			return pairRecord.getRightBreakPoint();
		} else {
			//discordant pair breakpoint
			return getRightBps();
		}
	}
	

	/**
	 * For the final breakpoint, category 2 breakpoints are swapped
	 * Used when writing results to output files
	 * @return the final right breakpoint for the cluster
	 */
	public int getFinalRightBreakpoint() {		
		if (getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
			return getFinalLeft();
		} else {	
			return getFinalRight();	
		}		
	}

	/**
	 * Add QSVClip record
	 * 
	 */
	public void addQSVClipRecord(SoftClipCluster clipRecord) {
		if (clipRecords == null) {
			clipRecords = new ArrayList<>();
		}		
		clipRecords.add(clipRecord);
	}
	
	/**
	 * 
	 * @return true if there are clip records present 
	 */
	public boolean hasSoftClipEvidence() {
		return clipRecords != null && ! clipRecords.isEmpty();
	}

	/*
	 * The primary soft clip cluster has a double sided breakpoint. 
	 */
	private boolean hasMatchingBreakpoints() {
		if (clipRecords != null) {
			SoftClipCluster c = getPrimarySoftClipCluster();
			return c.findMatchingBreakpoints();
		}
		return false;
	}

	/**
	 * Determine if the cluster is germline
	 */
	public void findGermline() {
		
		//look for germline in clip records
		if (clipRecords != null) {
			for (SoftClipCluster c: clipRecords) {
				if (c.isGermline()) {
					this.isGermline = true;
					return;
				}
			}
		}
		
		//look for germline pair record
		if (pairRecord != null) {
			String type =  pairRecord.getType();
			if (type.equals("germline")) {
				this.isGermline = true;
			}
		}
	}

	/**
	 * Checks to see if both breakpoints in the supplied clip record overlap by +/-50 bp any other clip
	 * records in the cluster
	 * @param clipRecord 
	 * @return true if clip record overlaps by +-50bp
	 */
	public boolean findClipOverlap(SoftClipCluster clipRecord) {
		int potentialLeftStart = clipRecords.get(0).getLeftBreakpoint();
		int potentialLeftEnd =  clipRecords.get(0).getLeftBreakpoint();
		
		int potentialRightStart = clipRecords.get(0).getRightBreakpoint();
		int potentialRightEnd =  clipRecords.get(0).getRightBreakpoint();
		
		
		for (int i=1, size = clipRecords.size() ; i < size ; i++) {
			SoftClipCluster clip = clipRecords.get(i);
			if (clip.getLeftBreakpoint() < potentialLeftStart) {
				potentialLeftStart = clip.getLeftBreakpoint();
			}
			if (clip.getLeftBreakpoint() > potentialLeftEnd) {
				potentialLeftEnd = clip.getLeftBreakpoint();
			}
			
			if (clip.getRightBreakpoint() < potentialRightStart) {
				potentialRightStart = clip.getRightBreakpoint();
			}
			if (clip.getRightBreakpoint() > potentialRightEnd) {
				potentialRightEnd = clip.getRightBreakpoint();
			}
		}
		
		if (clipRecord.getLeftBreakpoint() >= (potentialLeftStart - 50)
				&& clipRecord.getLeftBreakpoint() <= (potentialLeftEnd + 50) &&
				clipRecord.getRightBreakpoint() >= (potentialRightStart - 50) 
				&& clipRecord.getRightBreakpoint() <= (potentialRightEnd + 50)) {
			
			clipRecords.add(clipRecord);
			if (clipRecord.isGermline()) {
				isGermline = true;
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Checks to see if both breakpoints in the supplied clip record overlap by +/-100 bp 
	 * with the ends of the discordant pair record
	 * @param clipRecord
	 * @return
	 */
	public boolean findClusterOverlap(SoftClipCluster clipRecord) {
		clipRecord.checkOrder();
		Integer leftBP = clipRecord.getLeftBreakpoint();
		Integer rightBP = clipRecord.getRightBreakpoint();
		
		boolean hasLeftOverlap = false;
		boolean hasRightOverlap = false;
		
		if (clipRecord.getLeftReference().equals(pairRecord.getLeftReferenceName()) && 
				clipRecord.getRightReference().equals(pairRecord.getRightReferenceName()) ||
				clipRecord.getLeftReference().equals(pairRecord.getRightReferenceName()) && 
				clipRecord.getRightReference().equals(pairRecord.getLeftReferenceName())) {
			
			hasLeftOverlap  = getOverlap(true, leftBP);
			hasRightOverlap = getOverlap(false, rightBP);
		}
		
		if (hasLeftOverlap && hasRightOverlap) {
			//if the clip record has matching breakpoints, check to make sure the predicted mutation
			//types are the same
			if (clipRecord.hasMatchingBreakpoints()) {				
				if ( ! clipRecord.getMutationType().equals(pairRecord.getRealMutationType()) 
				&& ! clipRecord.getOrientationCategory().equals(pairRecord.getOrientationCategory())) {			 
					return false;
				}
			}
			if (clipRecords == null) {
				clipRecords = new ArrayList<SoftClipCluster>();
			}
			if (clipRecord.isGermline()) {
				isGermline = true;
			}
			clipRecords.add(clipRecord);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Looks for overlap between a clip breakpoint and the end of a 
	 * discordant pair cluster. 
	 * @param isLeftBreakpoint - the left breakpoint
	 * @param clipBreakpoint - position of the breakpoint
	 * @return true if there is overlap
	 */
	public boolean getOverlap(boolean isLeftBreakpoint, int clipBreakpoint) {

		boolean hasOverlap = false;
		//Category 1: left end, right start
		//Category 2: left start, right end
		//Category 3: left end, right end
		//Category 4: left start, right start
		
		String primaryCategoryNo = pairRecord.getqPrimerCateory().getPrimaryCategoryNo();		
		if (primaryCategoryNo.equals("1")) {			
			if (isLeftBreakpoint) {
				return getOverlap(clipBreakpoint, pairRecord.getLeftEnd());
			} else {
				return getOverlap(clipBreakpoint, pairRecord.getRightStart());
			}
		} else if (primaryCategoryNo.equals("2") || primaryCategoryNo.equals("5")) {
			if (isLeftBreakpoint) {
				return getOverlap(clipBreakpoint, pairRecord.getLeftStart());
			} else {
				return getOverlap(clipBreakpoint, pairRecord.getRightEnd());
			}
		} else if (primaryCategoryNo.equals("3")) {
			if (isLeftBreakpoint) {
				return getOverlap(clipBreakpoint, pairRecord.getLeftEnd());
			} else {
				return getOverlap(clipBreakpoint, pairRecord.getRightEnd());
			}
		} else if (primaryCategoryNo.equals("4")) {
			if (isLeftBreakpoint) {
				return getOverlap(clipBreakpoint, pairRecord.getLeftStart());
			} else {
				return getOverlap(clipBreakpoint, pairRecord.getRightStart());
			}
		} else {}

		return hasOverlap;
	}
	
	/*
	 * Check for +/-100bp overlap between cluster boundary and clip breakpoint
	 */
	public static boolean getOverlap(int clipBreakpoint, int clusterBoundary) {
		int start = clusterBoundary - 100;
		if (start < 0) {
			start = 0;
		}
		int end = clusterBoundary + 100;
		
		return (clipBreakpoint >= start && clipBreakpoint <= end) ;
	}
	
	/**
	 * Confidence level for the SV. 
	 * Levels range from 1-6. (1-3 are SV, 4 is potential germline, 5 is potential repeat, 6 is single sided clip)
	 * @return confidence level 1-6
	 */
	public String getConfidenceLevel() {
		return getConfidenceLevel(false);
	}
	public String getConfidenceLevel(boolean log) {
	    String cat = "";
	    //check if it is a potential germline
		if (getPotentialGermline()) {
			cat =  QSVConstants.LEVEL_GERMLINE;
		} else if (potentialRepeatRegion) {
			cat = QSVConstants.LEVEL_REPEAT;					
		} else {
		    //Discordant pair and clip records present
			if (pairRecord != null && clipRecords != null) {
				
				if (log) {
					logger.info("getConfidenceLevel pairRecord != null && clipRecords != null");
				}
				 //double sided clip
				 if (hasMatchingBreakpoints()) {
					if (isPotentialSplitRead()) {
						cat =  QSVConstants.LEVEL_HIGH;
					} else {
						cat =  QSVConstants.LEVEL_MID;
					}
				//single sided clip
				} else {
					if (isPotentialSplitRead()) {
						cat =  QSVConstants.LEVEL_MID;
					} else {
						cat =  QSVConstants.LEVEL_LOW;
					}
				}	
			//Discordant pair only
			} else if (pairRecord != null) {
				if (log) {
					logger.info("getConfidenceLevel pairRecord != null");
				}
				if (isPotentialSplitRead()) {
					cat =  QSVConstants.LEVEL_MID;
				} else {
					cat =  QSVConstants.LEVEL_LOW;
				}
			//Clip only
			} else {
				if (log) {
					logger.info("getConfidenceLevel clipRecords != null");
				}
				//Double sided
				if (hasMatchingBreakpoints()) {
					if (isPotentialSplitRead()) {
						cat =  QSVConstants.LEVEL_MID;
					} else {
						cat =  QSVConstants.LEVEL_LOW;
					}
				//single sided
				} else {
					if (isPotentialSplitRead()) {							
						cat =  QSVConstants.LEVEL_LOW;
					} else {
						cat =  QSVConstants.LEVEL_SINGLE_CLIP;
					}
				}
			}
		}
		return cat;
	}

	/**
	 * Check to see if the SV is potentially germline. Will return true if 
	 * there is a normal split read contig or if the discordant pair has > 10% low 
	 * confidence normal mater pairs
	 * @return
	 */
	public boolean getPotentialGermline() {
		if (pairRecord != null) {
			if (pairRecord.getLowConfidenceNormalMatePairs() > 0 && (double)pairRecord.getClusterMatePairs().size() > 0) {
				if (pairRecord.getClusterMatePairs().size() <= 20) {
					return true;
				}
				double result = (double) pairRecord.getLowConfidenceNormalMatePairs()/(double)pairRecord.getClusterMatePairs().size();
				if (result > 0.1) {
					return true;
				}
			} 
		}
		return isNormalPotentialSplitRead();			
	}
	
	private boolean isNormalPotentialSplitRead() {
		return normalSplitReadContig != null 
				&& normalSplitReadContig.getIsPotentialSplitRead()
				&& splitReadContig != null
				&& splitReadContig.getIsPotentialSplitRead();
	}

	/**
	 * @return the mutation type for the cluster
	 */
	public String getMutationType() {
		
		if (pairRecord != null && clipRecords != null) {	
			return pairRecord.getMutationType(false);
		} else if (clipRecords != null){
			return getClipsMutationType();
		} else {
			return pairRecord.getMutationType(false);
		}
	}
	
	/*
	 * Gets the mutation type for the clips
	 */
	private String getClipsMutationType() {		
		String mut = "";
		
		if (clipRecords != null) {
			SoftClipCluster c = getPrimarySoftClipCluster();
			
			if (!getConfidenceLevel().equals(QSVConstants.LEVEL_SINGLE_CLIP) && splitReadContig != null) {		
				if (splitReadContig.getIsPotentialSplitRead()) {
					mut = splitReadContig.getMutationType();
				} else {
					mut = c.getMutationType();
				}
			} else {
				mut = c.getMutationType();
			}
		}		
		return mut;		
	}	


	
	/*
	 * Get count of clips for writing to output files
	 */
	public static String getClipCount(SoftClipCluster c, boolean isTumour, boolean leftPos) {
		if (c != null) {
			return String.valueOf(c.getClipCount(isTumour, leftPos));
		}
		return "0";
	}
	
	/**
	 * If there are more than one clip records in the cluster, this
	 * method find the most representative one
	 * @return primary soft clip cluster
	 */
	public SoftClipCluster getPrimarySoftClipCluster() {
		if (clipRecords != null) {
			//only one record
			if (clipRecords.size() == 1) {
				return clipRecords.get(0);
			} else {
				List<SoftClipCluster> potentialMatches = clipRecords.stream().filter(SoftClipCluster::hasMatchingBreakpoints).collect(Collectors.toList());
				
				if (potentialMatches.size() == 1) {
					return potentialMatches.get(0);
				} else if (potentialMatches.size()  > 1) {
					//chose the one with the most clips
					return getSoftClipClusterWithHighestClipCount(potentialMatches);
				} else {
					//no double sided clip clusters, chose single sided with highest clip count
					return getSoftClipClusterWithHighestClipCount(clipRecords);
				}
			}			
		}
		return null;
	}
	
	public static SoftClipCluster getSoftClipClusterWithHighestClipCount(List<SoftClipCluster> list) {
		SoftClipCluster match = null;
		int maxCount = 0;
		for (SoftClipCluster c: list) {
			int count = c.getClipCount(true, true) + c.getClipCount(true, false);
			if (count > maxCount) {
				maxCount = count;
				match = c;	
			}
		}
		return match;
	}
	
	/**
	 * Attempts to rescue reads that were previously not identified. Only rescue if it
	 * is a somatic call
	 * @param blat
	 * @param p
	 * @param n
	 * @param softclipDir
	 * @param consensusLength
	 * @param isQCMG
	 * @param minInsertSize
	 * @param singleSided
	 * @param isSplitRead
	 * @param referenceFile
	 * @return
	 * @throws Exception
	 */
	public boolean findSplitReadContig(QSVParameters p, boolean isSplitRead, String referenceFile) throws Exception {
		return findSplitReadContig( p,  isSplitRead,  referenceFile, false);
	}
	public boolean findSplitReadContig(QSVParameters p, boolean isSplitRead, String referenceFile, boolean log) throws Exception {
		boolean rescue = false;
		
		if ( ! isGermline && ! rescued) {
			
			//if running split read mode, try to get a split read contig
			if (isSplitRead) {
				if (this.splitReadContig != null) {
					if (log) {
						logger.info("findSplitReadContig, splitReadContig.getConsensus() before findSplitRead: " + splitReadContig.getConsensus());
					}
				    this.splitReadContig.findSplitRead(log);	
					this.consensus = splitReadContig.getConsensus();
					if (log) {
						logger.info("findSplitReadContig, splitReadContig.getConsensus() after findSplitRead: " + splitReadContig.getConsensus());
					}
					this.potentialRepeatRegion = splitReadContig.getIsPotentialRepeatRegion();
				
					if (this.normalSplitReadContig != null) {
						if (log) {
							logger.info("findSplitReadContig, normalSplitReadContig.getConsensus() before findSplitRead: " + normalSplitReadContig.getConsensus());
						}
						normalSplitReadContig.findSplitRead(log);
						if (log) {
							logger.info("findSplitReadContig, normalSplitReadContig.getConsensus() after findSplitRead: " + normalSplitReadContig.getConsensus());
						}
					}						
					rescue = true;					
				}
			}

			if (referenceFile != null) {
				if ((leftReferenceFlank == null && rightReferenceFlank == null) ||
						(Constants.EMPTY_STRING.equals(leftReferenceFlank) && Constants.EMPTY_STRING.equals(rightReferenceFlank))) {
					
					getReferenceFlank(new File(referenceFile), p.getChromosomes());
				}
			}

		} else {
			if (this.consensus == null) {
				this.consensus = "";
			}			
		}
		
		this.rescued = true;
		return rescue;
	}
//	public boolean findSplitReadContig(BLAT blat, QSVParameters p, QSVParameters n, String softclipDir, int consensusLength, boolean isQCMG, int minInsertSize, 
//			boolean singleSided, boolean isSplitRead, String referenceFile) throws Exception {
//		boolean rescue = false;
//		
//		
//		if ( ! isGermline && ! rescued) {
//			
//			//if running split read mode, try to get a split read contig
//			if (isSplitRead) {
//				if (this.splitReadContig != null) {
//					this.splitReadContig.findSplitRead();			
//					this.consensus = splitReadContig.getConsensus();
//					
//					this.potentialRepeatRegion = splitReadContig.getIsPotentialRepeatRegion();
//					
//					if (this.normalSplitReadContig != null) {
//						normalSplitReadContig.findSplitRead();
//					}						
//					rescue = true;					
//				}
//			}
//			
//			if (referenceFile != null) {
//				if ((leftReferenceFlank == null && rightReferenceFlank == null) ||
//						(Constants.EMPTY_STRING.equals(leftReferenceFlank) && Constants.EMPTY_STRING.equals(rightReferenceFlank))) {
//					
//					getReferenceFlank(new File(referenceFile), p.getChromosomes());
//				}
//			}
//			
//		} else {
//			if (this.consensus == null) {
//				this.consensus = "";
//			}			
//		}
//		
//		this.rescued = true;
//		return rescue;
//	}

	/*
	 * For dcc file - get the 200bp upstream and downstream of each breakpoint
	 */
	private void getReferenceFlank(File referenceFile, Map<String, List<Chromosome>> chromosomes) {
		if (referenceFile != null) {
			ConcurrentMap<String, byte[]> referenceMap = QSVUtil.getReferenceMap();
    		
			if (referenceMap != null) {
				leftReferenceFlank = getCurrentFlankSeq(referenceMap, leftReference, getLeftBreakpoint(), chromosomes.get(leftReference));
				rightReferenceFlank = getCurrentFlankSeq(referenceMap, rightReference, getRightBreakpoint(), chromosomes.get(rightReference));
			}
			
		}				
	}

	/*
	 * For dcc file - get the 200bp upstream and downstream of a breakpoint
	 */
	public static String getCurrentFlankSeq(ConcurrentMap<String, byte[]> referenceMap, String reference,
			int breakpoint, List<Chromosome> list) {
		Chromosome c = null;
		if (list != null) {
			c = list.get(0);
		}
		String bases = Constants.EMPTY_STRING;
		if (c != null) {
			int start = Math.max(breakpoint - 200, 1);
			int end = Math.min(breakpoint + 200, c.getTotalLength()); 
			
			try {
				byte[] basesArray = referenceMap.get(reference);
				if (null == basesArray) {
					logger.warn("Could not find reference " + reference + " in reference file. Is this the same reference file as was used to map the bam file?");
				} else {
					// array is 0-based, whereas picard is 1-based
					bases = new String(basesArray, start - 1 , (end - start) + 1);
				}
			} catch (Exception e) {
				logger.warn("Trying to get " + reference + " " + start + " " + end + " from chr " + c.toString());
				logger.warn(QSVUtil.getStrackTrace(e));
			}
		} 
		return bases;		
	}

	/*
	 * Return true if it is potential split read  region. Potiential repeat
	 * if there are > 1000 clips in the immediate region around a breakpoint
	 */
	public boolean isPotentialSplitRead() {
		return splitReadContig != null
				&& splitReadContig.getIsPotentialSplitRead();
	}

	private Set<String> findExpectedPairClassifications() {
		Set<String> list = new HashSet<>();
		if (pairRecord != null) {
			String pg = pairRecord.getZp();
			if (pg.contains("_")) {
				list.add(pg.split("_")[0]);
				list.add(pg.split("_")[1]);
			} else {
				list.add(pg);
			}
		} else {
			for (SoftClipCluster c : clipRecords) {
				if (c.getMutationType().equals("CTX")) {
					list.add("Cxx");
				} else {
					list.addAll(QSVUtil.getPairGroupsByOrientationCategory(c.getOrientationCategory()));
				}				
			}
		}
		return list;
	}

	/*
	 * For one sided clips, go back to bam to see if clips at the other breakpoint can be rescues
	 */
	private void rescueClipping(TIntObjectMap<int[]> cache, QSVParameters p, QSVParameters n, String softclipDir, int consensusLength, int minInsertSize) throws Exception {
		if (clipRecords != null) {
			for (SoftClipCluster r: clipRecords) {
				if ( ! r.hasMatchingBreakpoints()) {
					if (n == null) {
						r.rescueClips(p, cache, p.getClippedBamFile(), null, softclipDir, consensusLength, 5, minInsertSize);
					} else {
						r.rescueClips(p, cache, p.getClippedBamFile(), n.getInputBamFile(), softclipDir, consensusLength, 5, minInsertSize);
					}
				}
			}
		}
	}
//	private void rescueClipping(BLAT blat, QSVParameters p, QSVParameters n, String softclipDir, int consensusLength, int minInsertSize) throws Exception {
//		if (clipRecords != null) {
//			for (SoftClipCluster r: clipRecords) {
//				if ( ! r.hasMatchingBreakpoints()) {
//					if (n == null) {
//						r.rescueClips(p, blat, p.getClippedBamFile(), null, softclipDir, consensusLength, 5, minInsertSize);
//					} else {
//						r.rescueClips(p, blat, p.getClippedBamFile(), n.getInputBamFile(), softclipDir, consensusLength, 5, minInsertSize);
//					}
//				}
//			}
//		}
//	}

	/**
	 * Get the qprimer string for the cluster
	 * @return qprimer string
	 */
	public String getQPrimerString() {
		QPrimerCategory c = null;		
		
		if (pairRecord != null) {
			c = pairRecord.getqPrimerCateory();
			return c.toString(svId);
		} 
		
		return "";
	}

	public boolean singleSidedClip() {
		
		if (pairRecord != null) {
			return false;
		} else {
			SoftClipCluster clip = getPrimarySoftClipCluster();			
			if (clip.hasMatchingBreakpoints()) {
				return false;
			}			
		}
		return true;
	}	



	/*
	 * Get the orientation category for the cluster (1-4)
	 */
	private String getOrientationCategory() {
		String clusterCat = null;
		String clipCat = null;
	 
		//piar record orientation category
		if (pairRecord != null) {
			clusterCat = pairRecord.getOrientationCategory();
		} 
		
		//orintation category for clips
		if (clipRecords != null) {
			SoftClipCluster clip = getPrimarySoftClipCluster();
			//clips double sided
			if (clip.hasMatchingBreakpoints()) {
				clipCat = clip.getOrientationCategory();
			} else {			
				//orientation category for split reads
				if (isPotentialSplitRead() && splitReadContig != null) {
					return splitReadContig.getOrientationCategory();
				}
			}
		}
		
		//chose pair record over clips
		if (clusterCat != null && clipCat != null) {
			return clusterCat;
		} else if (clusterCat != null) {
			return clusterCat;
		} else  {
			if (clipCat != null) {		
				return clipCat;
			}
		} 
		return "";
	}
	
	/*
	 * get contig sequence for clips from both breakpoints
	 */
	private String getClipContigSequence() throws Exception {
		if (clipRecords != null && clipRecords.size() == 1) {
			return clipRecords.get(0).getOverlappingContigSequence();				
		}
		return "";
	}
	
	/*
	 * Gets the non template sequence for the cluster
	 */
	private String getNonTemplateSequence() {
		//Only get homology for the somatics
		if (isGermline) {
			return QSVConstants.UNTESTED;
		}
		
		String splitNonTmp = QSVConstants.UNTESTED;
		String clipNonTmp = QSVConstants.UNTESTED;
		if (splitReadContig != null) {
			splitNonTmp = splitReadContig.getNonTemplateSequence();
		}		
		
		String category = null;
		if (pairRecord != null) {
			category = pairRecord.getOrientationCategory();
		}
		boolean splitReadMatch = false;
		if (clipRecords != null) {
			//get cluster if it has matching breakpoints, or single sided with discordant pair evidence
			if (hasMatchingBreakpoints() || (!hasMatchingBreakpoints() && pairRecord != null)) {
				SoftClipCluster c = getPrimarySoftClipCluster();				
				clipNonTmp = c.getNonTemplateSequence(category);
				splitReadMatch = getSplitReadClipClusterMatch(c);				
			}
		}	
		//if the cluster is pair only with split read, or is a clip, but couldn't test for non-temp, return 
		//split read non-temp
		if (isPairWithSplitRead() || (clipNonTmp.equals(QSVConstants.UNTESTED) && splitReadMatch)) {			
			if (splitNonTmp.equals("")) {
				return QSVConstants.NOT_FOUND;
			} else {
				return splitNonTmp;
			}
		} else {
			//otherwise return clip non-tep
			if (clipNonTmp.equals("")) {
				return QSVConstants.NOT_FOUND;
			} else {
				return clipNonTmp;
			}
		}
	}

	/*
	 * Get microhomology for the cluster. 
	 */
	private String getMicrohomology() {
		
		//Only get homology for the somatics
		if (isGermline) {
			return QSVConstants.UNTESTED;
		}
		
		
		
		String splitMh = QSVConstants.UNTESTED;
		String clipMh = QSVConstants.UNTESTED;
		
		if (splitReadContig != null) {
			splitMh = splitReadContig.getMicrohomology();
		}		
		
		String category = null;
		if (pairRecord != null) {
			category = pairRecord.getOrientationCategory();
		}
		
		boolean splitReadMatch = false;
		if (clipRecords != null) {
			//get cluster if it has matching breakpoints, or single sided with discordant pair evidence
			if (hasMatchingBreakpoints() || (!hasMatchingBreakpoints() && pairRecord != null)) {						
				SoftClipCluster c = getPrimarySoftClipCluster();				
				clipMh = c.getMicrohomology(category);
				splitReadMatch = getSplitReadClipClusterMatch(c);
//				logger.info("in getMicrohomology, svId: " + svId + ", SoftClipCluster.getSoftClipConsensusString: " + c.getSoftClipConsensusString(svId));
//				logger.info("in getMicrohomology, svId: " + svId + ", SoftClipCluster.hasMatchingBreakpoints: " + c.hasMatchingBreakpoints());
//				if (null != splitReadContig) {
//					logger.info("in getMicrohomology, svId: " + svId + ", splitReadContig.getLeftReference: " + splitReadContig.getLeftReference() + ", splitReadContig.getRightReference(): " + splitReadContig.getRightReference() + ", splitReadContig.getLeftBreakpoint(): " + splitReadContig.getLeftBreakpoint() + ", splitReadContig.getRightBreakpoint(): " + splitReadContig.getRightBreakpoint());
//				} else {
//					logger.info("in getMicrohomology, svId: " + svId + ", splitReadContig is NULL");
//				}
			}
		}
		
//		logger.info("in getMicrohomology, details : " +  getClusterDetails(clipRecords, pairRecord, splitReadContig, this));
//		logger.info("in getMicrohomology, svId: " + svId + ", category: " + category + ", splitMh: " + splitMh + ", clipMh: " + clipMh+ ", splitReadMatch: " + splitReadMatch+ ", isPairWithSplitRead: " + isPairWithSplitRead());
		//if the cluster is pair only with split read, or is a clip, but couldn't test for mh, return 
		//split read mh
		if (isPairWithSplitRead() || (clipMh.equals(QSVConstants.UNTESTED) && splitReadMatch)) {
				return splitMh;
		} else {
			//otherwise return clip mh
			if (clipMh.equals("")) {
				return QSVConstants.NOT_FOUND;
			} else {
				return clipMh;
			}
		}
	}
	
	public static String getClusterDetails(List<SoftClipCluster> softClipClusters, DiscordantPairCluster dpc, SplitReadContig splitReadContig, QSVCluster cluster) {
		String s = "";
		
		s += "svid: " + cluster.svId;
		if (null != softClipClusters) {
			s += ", softClipClusters.size(): " + softClipClusters.size();
			s += ", cluster.hasMatchingBreakpoints(): " + cluster.hasMatchingBreakpoints();
			s += ", cluster.getPrimarySoftClipCluster().getSoftClipConsensusString(cluster.svId): " + cluster.getPrimarySoftClipCluster().getSoftClipConsensusString(cluster.svId);
			s += ", cluster.getPrimarySoftClipCluster().hasMatchingBreakpoints(cluster.svId): " + cluster.getPrimarySoftClipCluster().hasMatchingBreakpoints();
			
		} else {
			s += ", softClipClusters.size(): 0 (null)";
		}
		
		if (null != splitReadContig) {
			s += ", splitReadContig: " + splitReadContig.toString();
		} else {
			s += ", splitReadContig: null";
		}
		if (null != dpc) {
			s += ", pairRecord: " + dpc.toString();
		} else {
			s += ", pairRecord: null";
		}
		
		return s;
	}
	/*
	 *Check to make sure the clip breakpoints are the same as the split read breakpoints 
	 */
	private boolean getSplitReadClipClusterMatch(SoftClipCluster c) {
		if (splitReadContig != null) {
			if (splitReadContig.getIsPotentialSplitRead()) {
				if (c.hasMatchingBreakpoints()) {
					logger.info("in getMicrohomology - about to call c.matchesSplitReadBreakpoints with splitReadContig.getLeftReference(): " + splitReadContig.getLeftReference() + ", splitReadContig.getRightReference(): " + splitReadContig.getRightReference() + ", splitReadContig.getLeftBreakpoint(): " + splitReadContig.getLeftBreakpoint() + ", splitReadContig.getRightBreakpoint(): " + splitReadContig.getRightBreakpoint() + ", c.getLeftReference(): " + c.getLeftReference() + ", c.getLeftBreakpoint(): " + c.getLeftBreakpoint() + ", c.getRightReference(): " + c.getRightReference() + ", c.getRightBreakpoint(): " + c.getRightBreakpoint());
					return c.matchesSplitReadBreakpoints(splitReadContig.getLeftReference(), splitReadContig.getRightReference(), splitReadContig.getLeftBreakpoint(), splitReadContig.getRightBreakpoint());
				} else {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * return true if there is discordant pair evidence and 
	 * split read evidence
	 */
	private boolean isPairWithSplitRead() {
		return ! hasSoftClipEvidence() 
				&& isPotentialSplitRead()
				&& ! getConfidenceLevel().equals(QSVConstants.LEVEL_HIGH);
	}
	
	/**
	 * Set the QSVCluster ids: sv, analysis and sample
	 * @param svId
	 * @param analysisId
	 * @param sampleId
	 * @param analysisDate
	 */
	public void setIdParameters(String svId, String analysisId, String sampleId) {
		this.svId = svId;		
		this.analysisId = analysisId;
		this.sampleId = sampleId;
	}

	/**
	 * get string summary of the SV cluster. File types that have a string associtated 
	 * with them are: dcc, tab delimited, verbose, qprimer, softclip
	 * @param fileType
	 * @param tumourFindType
	 * @param normalFindType
	 * @param isQCMG
	 * @param validationPlatform
	 * @return
	 */
	public String getDataString(String fileType, String tumourFindType, String normalFindType, boolean isQCMG, String validationPlatform) {
		if (fileType.equals("dcc")) {
//			this.validationPlatform = validationPlatform;
			return toDCCString(validationPlatform);
		} else if (fileType.equals("tab")) {
			return toTabString();
		} else if (fileType.equals("verbose")) {
			return toVerboseString(tumourFindType, normalFindType, isQCMG);
		} else if (fileType.equals("qprimer")) {
			return getQPrimerString();
		} else if (fileType.equals("softclip")) {
			return getSoftClipConsensus();
		}		
		return "";
	}

	/*
	 * Get the consensus string for the soft clip record
	 */
	private String getSoftClipConsensus() {
		StringBuilder sb = new StringBuilder();
		if (clipRecords != null && clipRecords.size() > 0) {
			int count = 0;
			for (SoftClipCluster c: clipRecords) {
				sb.append(c.getSoftClipConsensusString(svId));
				if (count != clipRecords.size()-1) {
					sb.append(QSVUtil.getNewLine());
				}
				count++;
			}
		}		
		return sb.toString();
	}

	/**
	 * @param minInsertSize
	 * @return true if the insert size for the sv is greater than the supplied minInsertSize
	 */
	public boolean passesMinInsertSize(int minInsertSize) {
		if (leftReference.equals(rightReference)) {				
			if (Math.abs(getRightBreakpoint() - getLeftBreakpoint()) < minInsertSize) {				
				return false;
			}
		}
		return true;
	}

	private String getNormalSplitReadBreakpointString() {
		if (isNormalPotentialSplitRead()) {
			return normalSplitReadContig.getSplitReadBreakpointString();
		}
		return "";
	}

	/*
	 * For dcc file - get the flanking sequence (200bp)
	 */
	private String getFlankSequence(boolean isLeft, String category) {
		if (isLeft) {
			if (category.equals(QSVConstants.ORIENTATION_2)) {
				return rightReferenceFlank;
			} else {
				return leftReferenceFlank;
			}
		} else {
			if (category.equals(QSVConstants.ORIENTATION_2)) {
				return leftReferenceFlank;
			} else {
				return rightReferenceFlank;
			}
		}
	}

	/*
	 * For notes column - add complex if greater than 1 clip record, 
	 * and add GFF 3 features
	 */
	private String getEventNotes() {
		StringBuilder b = new StringBuilder();
		if (clipRecords != null) {
			if (clipRecords.size() > 1) {
				b.append("complex");
				if (!features.isEmpty()) {
					b.append(" ; " );
				}
			}
		}
		
		b.append(features);
		
		return b.toString();
	}

	/*
	 * return pairing category if pair record is present
	 */
	private String getPairingCategory() {
		if (pairRecord != null) {
			return pairRecord.getMutationType(true);
		}
		return "";
	}

	/**
	 * Get the reference flank if it hasn't already been retrieved
	 * @param referenceFile
	 * @param chromosomes
	 * @throws QSVException
	 * @throws IOException 
	 */
	public void checkReferenceFlank(String referenceFile, Map<String, List<Chromosome>> chromosomes) throws IOException {
		if ( ! isGermline) {
			if (referenceFile != null) {
				if (leftReferenceFlank == null && rightReferenceFlank == null ||
						Constants.EMPTY_STRING.equals(leftReferenceFlank) && Constants.EMPTY_STRING.equals(rightReferenceFlank)) {
					
					getReferenceFlank(new File(referenceFile), chromosomes);
				}
			}
		}
	}

	/**
	 * If a gff file for comparison has been provided, the SV will be check to 
	 * see if it overlaps with a region in the gff file
	 * @param gffMap map of the gff3 records to compare the SV against
	 */
	public void checkGFF(Map<String, List<GFF3Record>> gffMap) {
		String ref1 = leftReference;
		String ref2 = rightReference;
		if (getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
			ref1 = rightReference;
			ref2 = leftReference;
		}                                                    
		List<String> features = new ArrayList<>();
		if (!ref1.equals(ref2)) {
			List<GFF3Record> leftRecords = gffMap.get(ref1);
			List<GFF3Record> rightRecords = gffMap.get(ref2);
			features.addAll(findGFFFeatureOverlap("pos1", leftRecords, (getFinalLeftBreakpoint())));
			features.addAll(findGFFFeatureOverlap("pos2", rightRecords, (getFinalRightBreakpoint())));
		} else {
			List<GFF3Record> records = gffMap.get(leftReference);
			features.addAll(findGFFFeatureOverlap("pos1", records, (getFinalLeftBreakpoint())));
			features.addAll(findGFFFeatureOverlap("pos2", records, (getFinalRightBreakpoint())));			
		}
		StringBuilder builder = new StringBuilder();
		if (features.size() > 0) {
			for (int i=0; i<features.size(); i++) {
				builder.append(features.get(i));
				if (i != features.size()-1) {
					builder.append(" ; ");
				}
			}
		}
		
		this.features = builder.toString();
	}

	/*
	 * Find overlap between SV breakpoint and GFF feature 
	 */
	private List<String> findGFFFeatureOverlap(String pos, List<GFF3Record> gffRecords, int breakpoint) {
		List<String> features = new ArrayList<>();
		if (gffRecords != null) {
			for (GFF3Record r: gffRecords) {
				if (breakpoint >= r.getStart() && breakpoint <= r.getEnd()) {
					features.add(pos + "=" + r.getType());				
				}
			}
		}
		return features;
	}

	/**
	 * Determines if the cluster is written to the results file
	 * if the single sided option is chosen. 
	 * @param isSingleSided
	 * @return true if the record should be written
	 */
	public boolean printRecord(boolean isSingleSided) {
		if (isSingleSided) {
			return true;
		} else {
			String conf = getConfidenceLevel();
			if ("chr10".equals(leftReference) && leftReference.equals(rightReference)) {
				logger.info("conf: " + conf);
			}
			if (conf.equals(QSVConstants.LEVEL_SINGLE_CLIP)) {
				return false;
			} else if (conf.equals(QSVConstants.LEVEL_HIGH) || conf.equals(QSVConstants.LEVEL_MID) || conf.equals(QSVConstants.LEVEL_LOW)) {
				return true;
			} else {
				if ("chr10".equals(leftReference) && leftReference.equals(rightReference)) {
					logger.info("! (singleSidedClip() && !isPotentialSplitRead());: " + ! (singleSidedClip() && !isPotentialSplitRead()));
				}
				return ! (singleSidedClip() && !isPotentialSplitRead());
			}
		}
	}

	/**
	 * Try to rescue clipping for any one side clip records
	 * @param blat
	 * @param tumourParameters
	 * @param normalParameters
	 * @param softclipDir
	 * @param consensusLength
	 * @param minInsertSize
	 * @throws Exception
	 */
	public void rescueClippping(TIntObjectMap<int[]> cache, QSVParameters tumourParameters,
			QSVParameters normalParameters, String softclipDir,
			Integer consensusLength, Integer minInsertSize) throws Exception {
		if ( ! isGermline && ! rescued) {
			
			//one sided evidence or none at all
			if (clipRecords != null) {	
				if ( ! hasMatchingBreakpoints()) {					
					rescueClipping(cache, tumourParameters, normalParameters, softclipDir, consensusLength, minInsertSize);
				}
			}
		}		
	}
//	public void rescueClippping(BLAT blat, QSVParameters tumourParameters,
//			QSVParameters normalParameters, String softclipDir,
//			Integer consensusLength, Integer minInsertSize) throws Exception {
//		if ( ! isGermline && ! rescued) {
//			
//			//one sided evidence or none at all
//			if (clipRecords != null) {	
//				if ( ! hasMatchingBreakpoints()) {					
//					rescueClipping(blat, tumourParameters, normalParameters, softclipDir, consensusLength, minInsertSize);
//				}
//			}
//		}		
//	}

	/**
	 * Attempts to create a split read contig for the SV using clips, unmapped reads and discordant pairs
	 * @param blat
	 * @param tumourParameters
	 * @param normalParameters
	 * @param softclipDir
	 * @param consensusLength
	 * @param isQCMG
	 * @param minInsertSize
	 * @param singleSided
	 * @param isSplitRead
	 * @param reference
	 * @param blatFile
	 * @throws Exception
	 */
	public void createSplitReadContig(TIntObjectMap<int[]> cache,
			QSVParameters tumourParameters, QSVParameters normalParameters,
			String softclipDir, Integer consensusLength, boolean isQCMG,
			Integer minInsertSize, boolean singleSided, boolean isSplitRead,
			String reference, String blatFile) throws Exception {
		createSplitReadContig(cache, tumourParameters,  normalParameters, softclipDir,  consensusLength,  isQCMG, minInsertSize,  singleSided,  isSplitRead, reference,  blatFile, false);
	}
		public void createSplitReadContig(TIntObjectMap<int[]> cache,
				QSVParameters tumourParameters, QSVParameters normalParameters,
				String softclipDir, Integer consensusLength, boolean isQCMG,
				Integer minInsertSize, boolean singleSided, boolean isSplitRead,
				String reference, String blatFile, boolean log) throws Exception {
		
		if (!isGermline && !rescued) {
			
			if (isSplitRead) {	
			    this.splitReadContig = new SplitReadContig(cache, tumourParameters, softclipDir, leftReference, rightReference, 
						getLeftBreakpoint(), getRightBreakpoint(), 
						findExpectedPairClassifications(), getClipContigSequence(), getConfidenceLevel(), 
						getOrientationCategory(), reference, tumourParameters.getChromosomes(), hasSoftClipEvidence(), blatFile + ".tumour.fa", log);				
								
				if (!potentialRepeatRegion && 
						!getConfidenceLevel().equals(QSVConstants.LEVEL_REPEAT) &&  !getConfidenceLevel().equals(QSVConstants.LEVEL_GERMLINE) && normalParameters != null) {	
					
					this.normalSplitReadContig = new SplitReadContig(cache, normalParameters, softclipDir, leftReference, rightReference, 
								getLeftBreakpoint(), getRightBreakpoint(), 
								findExpectedPairClassifications(), getClipContigSequence(), getConfidenceLevel(), 
								getOrientationCategory(), reference, normalParameters.getChromosomes(), hasSoftClipEvidence(), blatFile + ".normal.fa", log);
				}						
			}
		}		
	}
//	public void createSplitReadContig(BLAT blat,
//			QSVParameters tumourParameters, QSVParameters normalParameters,
//			String softclipDir, Integer consensusLength, boolean isQCMG,
//			Integer minInsertSize, boolean singleSided, boolean isSplitRead,
//			String reference, String blatFile) throws Exception {
//		
//		if (!isGermline && !rescued) {
//			
//			if (isSplitRead) {	
//				this.splitReadContig = new SplitReadContig(blat, tumourParameters, softclipDir, leftReference, rightReference, 
//						getLeftBreakpoint(), getRightBreakpoint(), 
//						findExpectedPairClassifications(), getClipContigSequence(), getConfidenceLevel(), 
//						getOrientationCategory(), reference, tumourParameters.getChromosomes(), hasSoftClipEvidence(), blatFile + ".tumour.fa");				
//				
//				if (!potentialRepeatRegion && 
//						!getConfidenceLevel().equals(QSVConstants.LEVEL_REPEAT) &&  !getConfidenceLevel().equals(QSVConstants.LEVEL_GERMLINE) && normalParameters != null) {	
//					
//					this.normalSplitReadContig = new SplitReadContig(blat, normalParameters, softclipDir, leftReference, rightReference, 
//							getLeftBreakpoint(), getRightBreakpoint(), 
//							findExpectedPairClassifications(), getClipContigSequence(), getConfidenceLevel(), 
//							getOrientationCategory(), reference, normalParameters.getChromosomes(), hasSoftClipEvidence(), blatFile + ".normal.fa");
//				}						
//			}
//		}		
//	}
	
	/**
	 * Returns details of the SV cluster in a tab delimited string
	 * to be written in the dcc1 results file
	 * @return dcc string
	 * @throws Exception
	 */
	private String toDCCString(String validationPlatform) {
		StringBuilder sb = new StringBuilder();
		
		String category = getOrientationCategory();	
		String chrFrom = getDCCChr(leftReference.replace("chr", ""));
	    String chrTo = getDCCChr(rightReference.replace("chr", ""));
	    if (category.equals(QSVConstants.ORIENTATION_2)) {
		    	String tmp = chrFrom;
		    	chrFrom = chrTo;
		    	chrTo = tmp;
	    }	    
	    
	    String confidence = getConfidenceLevel();
	    
        sb.append(analysisId).append(TAB); // analysis_id
        sb.append(sampleId).append(TAB); // tumour_sample_id
        sb.append(svId).append(TAB); // sv_id
        sb.append("1").append(TAB); // placement
        sb.append(getMutationType()).append(TAB); // annotation
        sb.append("-999").append(TAB); // interpreted_annotation
        sb.append("-999").append(TAB); // variant_type
        sb.append(chrFrom).append(TAB); // chr_from
        sb.append(getFinalLeftBreakpoint()).append(TAB); // chr_from_bkpt
        sb.append(getStrandOne(category, true)).append(TAB); // chr_from_strand
        sb.append(getChrRange(confidence)).append(TAB); // chr_from_range
        sb.append(getFlankSequence(true, category)).append(TAB); // chr_from_flanking_seq
        sb.append(chrTo).append(TAB); // chr_to
        sb.append(getFinalRightBreakpoint()).append(TAB); // chr_to_bkpt
        sb.append(getStrandTwo(category, true)).append(TAB); // chr_to_strand
        sb.append(getChrRange(confidence)).append(TAB); // chr_to_range
        sb.append(getFlankSequence(false, category)).append(TAB); // chr_to_flanking_seq
        sb.append(getMicrohomology()).append(TAB); // microhomology_sequence
        sb.append(getNonTemplateSequence()).append(TAB); // non_templated_sequence
        sb.append("4").append(TAB); // evidence
        sb.append("-999").append(TAB); // quality_score
        sb.append("-999").append(TAB); // probability
        sb.append("-999").append(TAB); // zygosity
        sb.append("0").append(TAB); // validation_status
        sb.append("-999").append(TAB); // validation_platform
        sb.append("-999").append(TAB); // db_xref
        sb.append("-999").append(TAB); // note
        if (pairRecord != null) {
	        sb.append(pairRecord.getClusterMatePairs().size()).append(TAB); // number_of_reads
	       	sb.append(pairRecord.getMatchedReadPairs().size()).append(TAB); // number_of_normal_reads 
	       	sb.append(pairRecord.getLowConfidenceNormalMatePairs()).append(TAB); // number_of_normal_reads
        } else {
        	 sb.append("0").append(TAB); // number_of_reads
             sb.append("0").append(TAB); // number_of_normal_reads   
             sb.append("0").append(TAB); // number_of_normal_reads 
        }       
        SoftClipCluster c = getPrimarySoftClipCluster();
        sb.append(getClipCount(c, true, true)).append(TAB);
        sb.append(getClipCount(c, true, false)).append(TAB);
        sb.append(getClipCount(c, false, true)).append(TAB);
        sb.append(getClipCount(c, false, false)).append(TAB);       
        sb.append(getConfidenceLevel()).append(TAB);   
        sb.append(getOrientationCategory()).append(TAB); 
        sb.append(getPairingCategory()).append(TAB);
        sb.append(getEventNotes()).append(TAB);
        sb.append(this.consensus);
        return sb.toString();	
	}
	
	/**
	 * Returns details of the SV cluster in a tab delimited string
	 * to be written in the tab delimited results file
	 * @return tab string
	 * @throws Exception
	 */
	public String toTabString() {
		StringBuilder sb = new StringBuilder();
		String category = getOrientationCategory();				
		String chrFrom = leftReference.replace("chr", "");
	    String chrTo = rightReference.replace("chr", "");
	    
	    if (category.equals(QSVConstants.ORIENTATION_2)) {
	    	String tmp = chrFrom;
	    	chrFrom = chrTo;
	    	chrTo = tmp;
	    }

        sb.append(analysisId).append(TAB); // analysis_id
        sb.append(sampleId).append(TAB); // tumour_sample_id
        sb.append(svId).append(TAB); // sv_id
        sb.append(getMutationType()).append(TAB); // annotation
        sb.append(chrFrom).append(TAB); // chr_from
        sb.append(getFinalLeftBreakpoint()).append(TAB); // chr_from_bkpt
        sb.append(getStrandOne(category, false)).append(TAB);
        sb.append(chrTo).append(TAB); // chr_to
        sb.append(getFinalRightBreakpoint()).append(TAB); // chr_to_bkpt
        sb.append(getStrandTwo(category, false)).append(TAB);
        if (pairRecord != null) {
	        sb.append(pairRecord.getClusterMatePairs().size()).append(TAB); // number_of_reads
	       	sb.append(pairRecord.getMatchedReadPairs().size()).append(TAB); // number_of_normal_reads 
	       	sb.append(pairRecord.getLowConfidenceNormalMatePairs()).append(TAB); // number_of_normal_reads
        } else {
        	 sb.append("0").append(TAB); // number_of_reads
             sb.append("0").append(TAB); // number_of_normal_reads   
             sb.append("0").append(TAB); // number_of_normal_reads 
        }    
        SoftClipCluster c = getPrimarySoftClipCluster();
        sb.append(getClipCount(c, true, true)).append(TAB);
        sb.append(getClipCount(c, true, false)).append(TAB);
        sb.append(getClipCount(c, false, true)).append(TAB);
        sb.append(getClipCount(c, false, false)).append(TAB);
        sb.append(getConfidenceLevel()).append(TAB);
        sb.append(getMicrohomology()).append(TAB);
        sb.append(getNonTemplateSequence()).append(TAB);        
    	sb.append(getSplitReadBreakpointString()).append(TAB);
    	sb.append(getNormalSplitReadBreakpointString()).append(TAB);
    	sb.append(getEventNotes()).append(TAB);
        sb.append(this.consensus);
        return sb.toString();
	}
	
	/**
	 * Write details of the QSVCluster in verbose format. This include information about the
	 * clips or discorant reads that are part of the cluster. 
	 * @param findType
	 * @param compareType
	 * @param isQCMG
	 * @return verbose string of QSVCluster
	 */
	private String toVerboseString(String findType, String compareType, boolean isQCMG) {
		StringBuilder builder = new StringBuilder();
		builder.append(getHeader(svId));
		
		if (pairRecord != null) {
			builder.append(">>DISCORDANT_PAIRS:").append(QSVUtil.NEW_LINE ).append(pairRecord.toVerboseString(compareType, isQCMG));
		}
		
		if (clipRecords != null) {
			builder.append(">>").append(QSVUtil.NEW_LINE);
			builder.append(">>SOFT_CLIPS:").append(QSVUtil.NEW_LINE).append(clipRecordsVerboseString(findType, compareType));
		}
		
		builder.append(QSVUtil.NEW_LINE);
		
		return builder.toString();
	}
	
	/*
	 * Verbose output for clip records
	 */
	private String clipRecordsVerboseString(String findType, String compareType) {
		StringBuilder builder = new StringBuilder();
		
		builder.append(getClipRecordsVerboseHeader());				
		String left = getClipBreakpointString(true, findType, compareType);
		String right = getClipBreakpointString(false, findType, compareType);
		if (getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
			if (!right.equals("")) {
				builder.append(">>POS1_CLIPS").append(QSVUtil.NEW_LINE).append(right);
			}			
			if (!left.equals("")) {
				builder.append(">>POS2_CLIPS").append(QSVUtil.NEW_LINE).append(left);
			}			
		} else {
			if (!left.equals("")) {
				builder.append(">>POS1_CLIPS").append(QSVUtil.NEW_LINE).append(left);
			}			
			if (!right.equals("")) {
				builder.append(">>POS2_CLIPS").append(QSVUtil.NEW_LINE).append(right);
			}
		}
		
		return builder.toString();
	}
	
	/*
	 * Get string for left or right clip breakpoint
	 */
	private String getClipBreakpointString(boolean isLeft, String findType, String compareType) {
		StringBuilder b = new StringBuilder();
		
		SoftClipCluster c = getPrimarySoftClipCluster();		
		String td = c.getClips(isLeft, true);
		String nd = c.getClips(isLeft, false);
		String lowTD = c.getLowConfClips(isLeft, true);
		String lowND = c.getLowConfClips(isLeft, false);
		
		if (!td.equals("")) {
			b.append(">>").append(findType).append("_clips").append(QSVUtil.NEW_LINE).append(td);
		}	
		if (!nd.equals("")) {
			b.append(">>").append(compareType).append("_clips").append(QSVUtil.NEW_LINE).append(nd);
		}
		if (!lowTD.equals("")) {
			b.append(">>").append(findType).append("_rescued_clips").append(QSVUtil.NEW_LINE).append(lowTD);
		}	
		if (!lowND.equals("")) {
			b.append(">>").append(compareType).append("_rescued_clips").append(QSVUtil.NEW_LINE).append(lowND);
		}
		return b.toString();
	}
	
	/*
	 * Header for clip records
	 */
	private String getClipRecordsVerboseHeader() {
		if (getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
			return ">>" + rightReference + ":" + getFinalLeftBreakpoint() + " | " + leftReference + ":" + getFinalRightBreakpoint() + QSVUtil.getNewLine();
		} else {
			return ">>" + leftReference + ":" + getFinalLeftBreakpoint() + " | " + rightReference + ":" + getFinalRightBreakpoint() + QSVUtil.getNewLine();
		}
	}
	
	/*
	 * Header for split reads
	 */
	private String getSplitReadBreakpointString() {
		if (splitReadContig != null) {
			if (splitReadContig.getIsPotentialSplitRead()) {
				return splitReadContig.getSplitReadBreakpointString();
			}
		}
		return "";
	}
	
	/*
	 * Verbose string header
	 */
	public String getHeader(String id) {
		StringBuilder header = new StringBuilder();
		String dis = "absent";
		String clip = "absent";
		if (pairRecord != null) {
			dis = pairRecord.getMutationType(false);
		}
		if (clipRecords != null) {
			clip =  getClipsMutationType();
		}
		
		String bp = leftReference + ":" + getFinalLeftBreakpoint() + " | " + rightReference + ":" + getFinalRightBreakpoint();
		header.append(">>").append(id).append(QSVUtil.NEW_LINE);
		header.append(">>Breakpoints: ").append(bp).append(QSVUtil.NEW_LINE);
		header.append(">>Discordant pair signature: ").append(dis).append(QSVUtil.NEW_LINE);
		header.append(">>Soft clipping signature:").append(clip).append(QSVUtil.NEW_LINE);
		header.append(">>").append(QSVUtil.NEW_LINE);

		return header.toString();
	}	

	/**
	 * @param orientationCategory
	 * @param isDCC 
	 * @return  strand for the first breakpoint
	 */
	public String getStrandOne(String orientationCategory, boolean isDCC) {
		if (orientationCategory.equals(QSVConstants.ORIENTATION_4)) {
			if (isDCC) {
				return "minus";
			} else {
				return "-";
			}
		} else {
			if (isDCC) {
				return "plus";
			} else {
				return "+";
			}
		}
	}
	/**
	 * @param orientationCategory
	 * @param isDCC 
	 * @return  strand for the first breakpoint
	 */
	public String getStrandTwo(String orientationCategory, boolean isDCC) {
		if (orientationCategory.equals(QSVConstants.ORIENTATION_3)) {
			if (isDCC) {
				return "minus";
			} else {
				return "-";
			}
		} else {
			if (isDCC) {
				return "plus";
			} else {
				return "+";
			}
		}
	}

	private String getDCCChr(String chr) {
		if (chr.equals("X")) {
			return "23";
		} else if (chr.equals("Y")) {
			return "24";
		} else if (chr.equals("MT")) {
			return "25";
		} 
		return chr;
	}

	private int getChrRange(String confidence) {
		if (pairRecord != null && !hasSoftClipEvidence()) {
			if (!isPairWithSplitRead()) {
				return QSVConstants.PAIR_CHR_RANGE;
			}
		}
		return QSVConstants.CLIP_CHR_RANGE;		
	}
}
