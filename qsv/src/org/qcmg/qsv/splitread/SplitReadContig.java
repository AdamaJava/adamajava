/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import au.edu.qimr.tiledaligner.util.TiledAlignerUtil;
import gnu.trove.map.TIntObjectMap;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;

public class SplitReadContig {

	private static final QLogger logger = QLoggerFactory.getLogger(SplitReadContig.class);

	private final StructuralVariant knownSV;
	private StructuralVariant splitreadSV = new StructuralVariant();
	private String splitReadBreakpointString = "";
	private String nonTemplateSequence;
	private String microhomology;
	private SplitReadAlignment left = null;
	private SplitReadAlignment right = null;
	private String leftSequence;
	private String rightSequence;
	private int length;
	private boolean findMH;
	private String confidenceLevel;
	private boolean isPotentialRepeat;
	private boolean isPotentialSplitRead;
	private String consensus;
	private Map<String, List<Chromosome>> chromosomes;
	private boolean hasSoftClipEvidence;
	private final TIntObjectMap<int[]> cache;
	private final QSVParameters parameters;
	private String mutationType;
	private String blatFile;
	private String clipContig;
	private String softclipDir;
	private String name;

	

	public SplitReadContig(TIntObjectMap<int[]> cache, QSVParameters p, String leftReference, String rightReference,
			int leftBreakpoint, int rightBreakpoint, String orientationCategory) {

		if (orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
			this.knownSV = new StructuralVariant(rightReference, leftReference, rightBreakpoint, leftBreakpoint, orientationCategory);		
		} else {
			this.knownSV = new StructuralVariant(leftReference, rightReference, leftBreakpoint, rightBreakpoint, orientationCategory);		
		}
		this.cache = cache;
		this.parameters = p;
	}

	public SplitReadContig(TIntObjectMap<int[]> cache, QSVParameters p,
			String softclipDir, String leftReference, String rightReference, int leftRecordBreakpoint, int rightRecordBreakpoint, 
			Set<String> expectedPairClassifications, String clipContig, String confidence, 
			String orientationCategory, String referenceFile, Map<String, List<Chromosome>> chromosomes, boolean hasSoftClipEvidence, String blatFile, boolean log) throws Exception {
		
		this(cache, p, leftReference, rightReference, leftRecordBreakpoint, rightRecordBreakpoint, orientationCategory);
		
		this.hasSoftClipEvidence = hasSoftClipEvidence;
		this.confidenceLevel = confidence;
		this.chromosomes = chromosomes;
		getSplitReadConsensus(softclipDir, expectedPairClassifications, clipContig, blatFile, log);
	}

	private boolean isTumour() {
		return this.parameters.isTumor();
	}

	private int getMaxReadCount() {
		return this.parameters.getRepeatCountCutoff();
	}

	public String getConsensus() {
		return consensus;
	}

	public void setConsensus(String consensus) {
		this.consensus = consensus;
		this.length = consensus.length();
	}

	public void setConfidenceLevel(String confidenceLevel) {
		this.confidenceLevel = confidenceLevel;
	}

	public void setSplitreadSV(StructuralVariant splitreadSV) {
		this.splitreadSV = splitreadSV;
	}

	public String getSplitReadBreakpointString() {
		return splitReadBreakpointString;
	}

	public String getNonTemplateSequence() {
		return nonTemplateSequence;
	}

	public void setNonTemplateSequence(String nonTemplateSequence) {
		this.nonTemplateSequence = nonTemplateSequence;
	}

	public String getMicrohomology() {
		return microhomology;
	}

	public Integer getLeftBreakpoint() {
		if (splitreadSV != null) {
			return splitreadSV.getLeftBreakpoint();
		}
		return null;
	}

	public Integer getRightBreakpoint() {
		if (splitreadSV != null) {
			return splitreadSV.getRightBreakpoint();
		}
		return null;
	}	

	public Integer getUnsortedLeftBreakpoint() {
		if (splitreadSV != null) {
			if (splitreadSV.getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
				return splitreadSV.getRightBreakpoint();
			} else {
				return splitreadSV.getLeftBreakpoint();
			}			
		}
		return null;
	}

	public Integer getUnsortedRightBreakpoint() {
		if (splitreadSV != null) {
			if (splitreadSV.getOrientationCategory().equals(QSVConstants.ORIENTATION_2)) {
				return splitreadSV.getLeftBreakpoint();
			} else {
				return splitreadSV.getRightBreakpoint();
			}			
		}
		return null;
	}	

	public String getOrientationCategory() {
		if (splitreadSV != null) {
			return splitreadSV.getOrientationCategory();
		}
		return "";
	}
	public boolean getIsPotentialRepeatRegion() {
		return this.isPotentialRepeat;
	}

	public boolean getIsPotentialSplitRead() {		
		return isPotentialSplitRead;
	}

	public String getLeftReference() {
		return splitreadSV.getLeftReference();
	}

	public String getRightReference() {
		return splitreadSV.getRightReference();
	}	

	public String getName() {
		Date date = new Date();
		Random r = new Random();
		int random = r.nextInt(900000);
		return "splitcon_xxx_" + knownSV.toString() + "_xxx_" + isTumour() + "_xxx_" + date.getTime() + "_xxx_" + random;
	}
	
	public void setLeftSequence(String leftSequence) {
		this.leftSequence = leftSequence;
	}

	public void setRightSequence(String rightSequence) {
		this.rightSequence = rightSequence;
	}

	public void setSplitReadAlignments(SplitReadAlignment left, SplitReadAlignment right) {
		this.left = left;
		this.right = right;
	}

	public void setFindMH(boolean b) {
		this.findMH = b;		
	}		

	public SplitReadAlignment getLeft() {
		return left;
	}

	public SplitReadAlignment getRight() {
		return right;
	}

	public static SplitReadAlignment getSingleSplitReadAlignment(SplitReadAlignment left, SplitReadAlignment right) {

		if (left == null && right != null) {
			return right;
		} else if (right == null && left != null) {
			return left;
		}
		return null;
	}

	private void getSplitReadConsensus(String softclipDir, Set<String> expectedPairClassifications, String clipContig, String blatFile, boolean log) throws Exception {
		Map<String, UnmappedRead[]> map = new HashMap<>();
		List<UnmappedRead> splitReads = new ArrayList<>();		

		this.microhomology = QSVConstants.UNTESTED;
		this.nonTemplateSequence = QSVConstants.UNTESTED;

		// setup the SamReader just once
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(parameters.getInputBamFile(), "silent")) {

			//left reads		
			readSplitReads(reader, map, splitReads, knownSV.getLeftReference(), knownSV.getLeftBreakpoint(), expectedPairClassifications);

			//right reads
			readSplitReads(reader, map, splitReads, knownSV.getRightReference(), knownSV.getRightBreakpoint(), expectedPairClassifications);
		}

		//add discordant pairs		
		int discordantPairCount = 0;
		for (Entry<String, UnmappedRead[]> entry: map.entrySet()) {
			UnmappedRead[] records = entry.getValue();
			if (records[0] != null && records[1] != null) {
				discordantPairCount++;
				splitReads.add(records[0]);
				splitReads.add(records[1]);
			}
			if (discordantPairCount >= 1000 && isPotentialRepeat) {
				break;
			}
		}
		assemble(splitReads, clipContig, softclipDir, blatFile, log);
	}

	private void assemble(List<UnmappedRead> splitReads, String clipContig, String softclipDir, String blatFile, boolean log) throws Exception {
		QSVAssemble qAssemble = new QSVAssemble();

		ConsensusRead fullContig =  qAssemble.getFinalContig(splitReads);
		
		if ("chr9_131403219_chr9_131418820_1".equals(knownSV.toString())) {
			logger.info("SplitReadContig in assemble, knownSV: " + knownSV + ", clipContig: " + clipContig + ", fullContig: " + fullContig.getSequence());
		}

		if (parameters.isTumor()) {
			if (log) {
				logger.info("assemble: setting consensus to clipConfig: " + clipContig);
			}
			this.consensus = clipContig;
		}
		if (fullContig != null) {
			if (log) {
				logger.info("assemble: setting consensus to fullContig: " + fullContig.getSequence());
			}
			this.consensus = fullContig.getSequence();			
		}

		this.clipContig = clipContig;
		this.softclipDir = softclipDir;
		this.blatFile = blatFile;
		this.name = getName();
	}

	public static List<BLATRecord> getBlatResults(Map<String, List<BLATRecord>> blatRecords, String leftReference, String rightReference, String name, String sequence, boolean debug) {
		List<BLATRecord> records = new ArrayList<>();
		
		if (debug) {
			logger.info("SplitReadContig in getBlatResults, name: " + name + ", sequence: " + sequence + ", leftReference: " + leftReference + ", rightReference: " + rightReference + ", number of blat records: " + blatRecords.values().stream().mapToLong(Collection::size).sum());
			for (Entry<String, List<BLATRecord>> entry : blatRecords.entrySet()) {
				logger.info("SplitReadContig in getBlatResults, key: " + entry.getKey() + ", no of blat recs: " + entry.getValue().size());
			}
		}
		
		if (null != blatRecords && ! blatRecords.isEmpty()) {
			List<BLATRecord> recs = blatRecords.get(sequence);
			if (null != recs) {
				for (BLATRecord record : recs) {
					if (record.isValid() && record.getQName().equals(name)) {
						if (debug) {
							logger.info("SplitReadContig BLAT rec (valid): " + record);
						}
						if (leftReference != null && rightReference != null) {
							if (record.getTName().equals(leftReference) || record.getTName().equals(rightReference)) {
								record.setQName(name);
								records.add(record);				
							}
						} else {
							record.setQName(name);
							records.add(record);
						}
					} else {
						if (debug) {
							logger.info("SplitReadContig BLAT rec (INVALID): " + record);
						}
					}
				}
			}
		}
		
		/*
		 * java 8 List sort should be better than Collections.sort
		 */
		records.sort(null);
		return records;
	}
	public void findSplitRead(boolean log) throws Exception {
		if (consensus != null && consensus.length() > TiledAlignerUtil.TILE_LENGTH) {

			this.length = consensus.length();
			if (length > 0) {
				if (log) {
					logger.info("findSplitRead, about to tiled aligner results for consensus:  " + consensus + ", name: " + name);
				}
				Map<String, String> sequenceNameMap = new HashMap<>(2);
				sequenceNameMap.put(consensus, name);
				List<BLATRecord> records = getBlatResults(TiledAlignerUtil.runTiledAlignerCache(parameters.getReference(), parameters.getRefIndexPositionMap(), cache, sequenceNameMap, TiledAlignerUtil.TILE_LENGTH, "SplitReadContig.findSplitRead", log, true),  knownSV.getLeftReference(), knownSV.getRightReference(), name, consensus, log);

				if (log) {
					logger.info("findSplitRead, getBlatResults size: " + records.size() + ", blat rec scores: " + records.stream().map(br -> br.getScore() + "").collect(Collectors.joining(",")));
				}

				if (!records.isEmpty()) {

					parseConsensusAlign(records);
					if ( ! isPotentialSplitRead && isTumour() && !clipContig.isEmpty()) {
						consensus = clipContig;
						if (log) {
							logger.info("findSplitRead, setting consensus: " + consensus);
						}
						length = consensus.length();
						if (length > 0) {

							sequenceNameMap.clear();
							sequenceNameMap.put(clipContig, name + "_clip");

							/*
							 * why is this run twice?
							 */
							records = alignConsensus(TiledAlignerUtil.runTiledAlignerCache(parameters.getReference(), parameters.getRefIndexPositionMap(), cache, sequenceNameMap, TiledAlignerUtil.TILE_LENGTH, "SplitReadContig.findSplitRead", log, true), clipContig, knownSV.getLeftReference(), knownSV.getRightReference());
							if (!records.isEmpty()) {
								if (log) {
									logger.info("findSplitRead, about to call parseConsensusAlign again, records.size: " + records.size() + ", top rec: " + records.getLast().toString());
								}
								parseConsensusAlign(records);
								if ( ! isPotentialSplitRead && isTumour()) {
									consensus = clipContig;
									if (log) {
										logger.info("findSplitRead, setting consensus: " + consensus);
									}
								}
							}
						}
					}
				}
			}
		} else {
			if (log) {
				logger.info("findSplitRead, consensus: " + (consensus == null ? "null" : consensus) + ", consensus length: " + (consensus == null ? 0 : consensus.length()));
			}	
		}
	}
	
	public static List<BLATRecord> alignConsensus(Map<String, List<BLATRecord>> blatRecords, String consensus, String leftReference, String rightReference) throws IOException, QSVException {
		
		List<BLATRecord> records = new ArrayList<>();
		if (null != blatRecords && ! blatRecords.isEmpty()) {
			List<BLATRecord> recs = blatRecords.get(consensus);
			if (null != recs) {
				for (BLATRecord record : recs) {
					if (record.isValid()) {
						if (leftReference != null && rightReference != null) {
							if (record.getTName().equals(leftReference) || record.getTName().equals(rightReference)) {
								records.add(record);				
							}
						} else {
							records.add(record);
						}
					}
				}
			}
		}
		
		/*
		 * java 8 List sort should be better than Collections.sort
		 */
		records.sort(null);
		return records;		
	}

	private void readSplitReads(SamReader reader, Map<String, UnmappedRead[]> map, List<UnmappedRead> splitReads, 
			String reference, int intBreakpoint, Set<String> expectedPairClassifications) throws QSVException {
		int buffer = parameters.getUpperInsertSize() + 200;
		int count = 0;
		
		SAMRecordIterator iter = reader.queryOverlapping(reference, intBreakpoint-buffer, intBreakpoint+buffer);

		Set<String> readGroupIds = parameters.getReadGroupIdsAsSet();

		while (iter.hasNext()) {
			SAMRecord r = iter.next();	 

			if ( ! r.getDuplicateReadFlag()) {
				
				String readGroupId = r.getReadGroup().getId();
				
				if (readGroupIds.contains(readGroupId)) {
					if (r.getReadUnmappedFlag() || r.getCigarString().contains("S")) {
						count++;
						if (count > 1000) {
							if (count > getMaxReadCount()) {
								isPotentialRepeat = true;
							}
							logger.info("Greater than " + getMaxReadCount() + " records found for rescue of : " + knownSV.toString());
							break;
						}
						//forward and reverse sequence
						/*
						 * Check that the read sequence is longer than the QSVAssemble.SEED_LENGTH
						 * otherwise we will get exceptions later on...
						 */
						if (r.getReadLength() >= QSVAssemble.SEED_LENGTH) {
							splitReads.add(new UnmappedRead(r, readGroupId, true));
							if (r.getReadUnmappedFlag()) {
								splitReads.add(new UnmappedRead(r, readGroupId, true, true));
							}
						}
					} else {

						parameters.getAnnotator().annotate(r, readGroupId);
						String zp = (String) r.getAttribute("ZP");
						if (expectedPairClassifications.contains(zp)) {

							if (count > 1000) {
								if (count > getMaxReadCount()) {
									isPotentialRepeat = true;
								}
								logger.info("Greater than " + getMaxReadCount() + " records found for rescue of : " + knownSV.toString());
								break;
							} else {
								if (r.getReadLength() >= QSVAssemble.SEED_LENGTH) {
									String key = r.getReadName() + ":" + readGroupId;
									UnmappedRead[] arr = map.get(key);

									if (null == arr) {
										arr = new UnmappedRead[]{new UnmappedRead(r, readGroupId, true), null};
										map.put(key, arr);
									} else {
										UnmappedRead newRead = new UnmappedRead(r, readGroupId, true);
										if (newRead.getBpPos().intValue() != arr[0].getBpPos().intValue()) {
											arr[1] = newRead;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		iter.close();

	}

	public void parseConsensusAlign(List<BLATRecord> records) throws QSVException, IOException {
		left = null;
		right = null;

	    if (confidenceLevel.equals(QSVConstants.LEVEL_SINGLE_CLIP) || knownSV.equalReference()) {
			getSplitReadAlignments(null, records);		
		} else {
			List<BLATRecord> leftRecords = new ArrayList<>();
			List<BLATRecord> rightRecords = new ArrayList<>();

			for (BLATRecord r: records) {
				if (r.getTName().equals(knownSV.getLeftReference())) {
					leftRecords.add(r);
				}
				if (r.getTName().equals(knownSV.getRightReference())) {
					rightRecords.add(r);
				}
			}
			getTranslocationSplitReadAlignments(null, leftRecords, rightRecords);
		}

		if (left != null & right != null & knownAndPotentialReferencesMatch(knownSV, left, right)) {

			//rearrange
			reorder();				
			leftSequence = null;
			rightSequence = null;
			splitreadSV.setReferences(left.getReference(), right.getReference());

			if (left.getStrand() == right.getStrand()) {
				//cat 1 or 2			
				if ((leftLower(left.getQueryStart(), right.getQueryStart()) && left.strandNegative() || ! leftLower(left.getQueryStart(), right.getQueryStart()) && left.strandPositive())) {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_2, left.getStartPos(), right.getEndPos());					
				} else {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_1,left.getEndPos(), right.getStartPos());
				}	

			} else {
				//cat 3 or 4
				if (( ! leftLower(left.getQueryStart(), right.getQueryStart()) && left.strandNegative()) || leftLower(left.getQueryStart(), right.getQueryStart()) && left.strandPositive()) {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_3, left.getEndPos(), right.getEndPos());
				} else {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_4, left.getStartPos(), right.getStartPos());
				}				
			}

			if (splitreadSV.getOrientationCategory() != null) {

				String orientationCategory = splitreadSV.getOrientationCategory();
				
				int length = consensus.length();
				if (left.getQueryStart() > length || left.getQueryEnd() > length) {
					logger.warn("left.getQueryStart() or getQueryEnd > length!! left.getQueryStart(): " + left.getQueryStart() + ",left.getQueryEnd(): " + left.getQueryEnd() + ", length: " + length + ", consensus: " + consensus);
					logger.warn("right.getQueryStart() or getQueryEnd > length!! right.getQueryStart(): " + right.getQueryStart() + ",right.getQueryEnd(): " + right.getQueryEnd() + ", length: " + length + ", consensus: " + consensus);
				} else if (right.getQueryStart() > length || right.getQueryEnd() > length) {
					logger.warn("left.getQueryStart() or getQueryEnd > length!! left.getQueryStart(): " + left.getQueryStart() + ",left.getQueryEnd(): " + left.getQueryEnd() + ", length: " + length + ", consensus: " + consensus);
					logger.warn("right.getQueryStart() or getQueryEnd > length!! right.getQueryStart(): " + right.getQueryStart() + ",right.getQueryEnd(): " + right.getQueryEnd() + ", length: " + length + ", consensus: " + consensus);
				}
				if (left.getQueryStart() < 1) {
					logger.warn("left.getQueryStart() is less than 1: " + left.getQueryStart() + ", left: " + left.toString() + ", consensus: " + consensus);
				}
				if (left.getQueryEnd() > length) {
					logger.warn("left.getQueryEnd() is >= consensus length: " + left.getQueryEnd() + ", left: " + left.toString() + ", consensus: " + consensus);
				}
				if (right.getQueryStart() < 1) {
					logger.warn("right.getQueryStart() is less than 1: " + right.getQueryStart() + ", right: " + right.toString() + ", consensus: " + consensus);
				}
				if (right.getQueryEnd() > length) {
					logger.warn("right.getQueryEnd() is >= consensus length: " + right.getQueryEnd() + ", right: " + right.toString() + ", consensus: " + consensus);
				}
				
				leftSequence = consensus.substring(Math.max(0,  left.getQueryStart() - 1), Math.min(length, left.getQueryEnd()));
				rightSequence = consensus.substring(Math.max(0, right.getQueryStart() - 1), Math.min(length, right.getQueryEnd()));

				if (needToReverseComplement(orientationCategory, left.getStrand())) {
					leftSequence = QSVUtil.reverseComplement(leftSequence);
					rightSequence = QSVUtil.reverseComplement(rightSequence);					
				}	

				if (orientationCategory.equals(QSVConstants.ORIENTATION_2)) {				
					String tmp = leftSequence;			
					leftSequence = rightSequence;
					rightSequence = tmp;
					splitreadSV.swap();					
					SplitReadAlignment tmpAlign = left;
					left = right;
					right = tmpAlign;
				}

				if (orientationCategory.equals(QSVConstants.ORIENTATION_1) || orientationCategory.equals(QSVConstants.ORIENTATION_2)) {					
					setCat1and2NonTemplate();					
				} else if (orientationCategory.equals(QSVConstants.ORIENTATION_3) || (orientationCategory.equals(QSVConstants.ORIENTATION_4))) {
					setCat3and4NonTemplate();
				}

				if (needToReverseComplement(orientationCategory, left.getStrand())) {
					nonTemplateSequence = QSVUtil.reverseComplement(nonTemplateSequence);
				}

				if (nonTemplateSequence.isEmpty()) {
					nonTemplateSequence = QSVConstants.NOT_FOUND;
				}

				determineSplitReadPotential();
				calculateMicrohomology();

			}			

			if (nonTemplateSequence == null) {
				nonTemplateSequence = QSVConstants.UNTESTED;
			} 

			this.splitReadBreakpointString = splitreadSV.toSplitReadString();

		} else {
			this.splitReadBreakpointString = "";
		}		
	}

	public static boolean knownAndPotentialReferencesMatch(StructuralVariant currentSV, SplitReadAlignment leftAlign, SplitReadAlignment rightAlign) {
		if (leftAlign != null & rightAlign != null) {
			return (leftAlign.getReference().equals(currentSV.getLeftReference()) && rightAlign.getReference().equals(currentSV.getRightReference())) ||
					(rightAlign.getReference().equals(currentSV.getLeftReference()) && leftAlign.getReference().equals(currentSV.getRightReference()));
		}
		return false;
	}

	public static boolean leftLower(int leftStart, int rightStart) {
		return leftStart < rightStart;
	}

	public static boolean needToReverseComplement(String orientationCategory, char leftStrand) {
		return ((leftStrand == QSVUtil.MINUS && ! orientationCategory.equals(QSVConstants.ORIENTATION_4)) ||
				(leftStrand == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_4))) ;
	}

	void calculateMicrohomology() {
		if (nonTemplateSequence == null) {
			this.microhomology = QSVConstants.UNTESTED;
		} else if (!nonTemplateSequence.isEmpty() && !nonTemplateSequence.equals(QSVConstants.NOT_FOUND)) {
			this.microhomology = QSVConstants.NOT_FOUND;
		} else {
			String tempMh = "";
			if (findMH) {				
				if (leftSequence != null && rightSequence != null) {					
					for (int i = 1; i < rightSequence.length(); i++) {				
						String currentRight = rightSequence.substring(0, i);				
						if (leftSequence.endsWith(currentRight)) {
							if (currentRight.length() > tempMh.length()) {
								tempMh = currentRight;
							}
						}			
					}
					if (!tempMh.isEmpty()) {
						this.microhomology = tempMh;
					} else {
						this.microhomology = QSVConstants.NOT_FOUND;
					}
				} else {
					this.microhomology = QSVConstants.UNTESTED;
				}
			} else {
				if (leftSequence != null && rightSequence != null) {	
					recheckMicrohomologyForSingleAlignment();					
				}
			}
		}
	}

	void recheckMicrohomologyForSingleAlignment() {
		String leftRef = getReferenceSequence(splitreadSV.getLeftReference(), splitreadSV.getLeftBreakpoint() + 1, 0, 50, chromosomes);
		String rightRef = getReferenceSequence(splitreadSV.getRightReference(), splitreadSV.getRightBreakpoint() - 1, 50, 0, chromosomes);	
		int end = leftSequence.length();
		String leftMh = "";
		for (int i = leftSequence.length() - 1; i > 0; i--) {				
			String currentLeft = leftSequence.substring(i, end);
			if (rightRef.endsWith(currentLeft)) {
				if (currentLeft.length() > leftMh.length()) {
					leftMh = currentLeft;
				}
			}			
		}
		String rightMh = "";
		for (int i = 1; i < rightSequence.length(); i++) {				
			String currentRight = rightSequence.substring(0, i);				
			if (leftRef.startsWith(currentRight)) {
				if (currentRight.length() > rightMh.length()) {
					rightMh = currentRight;
				}
			}			
		}
		splitreadSV.setLeftBreakpoint(splitreadSV.getLeftBreakpoint() + rightMh.length());
		splitreadSV.setRightBreakpoint(splitreadSV.getRightBreakpoint() - leftMh.length());
		this.microhomology = leftMh + rightMh;
		if (microhomology.isEmpty()) {
			microhomology = QSVConstants.NOT_FOUND;
		}		
	}

	public static String getReferenceSequence(String reference, int breakpoint, int leftBuffer, int rightBuffer, Map<String, List<Chromosome>> map) {		
		List<Chromosome> list = map.get(reference);

		if (list != null) {
			Chromosome c = list.getFirst();
			int start = breakpoint - leftBuffer;
			if (start < 0) {
				start = 1;
			}
			int end = breakpoint + rightBuffer;
			if (end > c.getTotalLength()) {
				end = c.getTotalLength();
			}
			
			ConcurrentMap<String,byte[]> referenceMap = QSVUtil.getReferenceMap();
			byte[] basesArray = referenceMap.get(reference);

			if (start > end) {
				logger.info("In getReferenceSequence method - Reference: " + reference + "breakpoint: " +  breakpoint + " chromosome: " + c.getName() + "  chromosome length: "  + c.getTotalLength() + " start: " + start + " end: " + end + " basesArray length: " + basesArray.length);
				throw new IllegalArgumentException("Start of reference sequence to retrieve is greater than end: " + start + " " + end);
			}
			// array is zero-based, and picard is 1-based
			return new String(Arrays.copyOfRange(basesArray, start - 1, end - 1));
		}
		return "";		
	}

	public void determineSplitReadPotential() {
		int buffer = ( ! QSVConstants.LEVEL_HIGH.equals(confidenceLevel) &&  ! hasSoftClipEvidence) ? 100 : 20;
		if (splitreadSV.splitReadEquals(knownSV, confidenceLevel, buffer)) {
			isPotentialSplitRead = true;
			mutationType = determineMutationType(splitreadSV);
		}

		if ( ! isPotentialSplitRead) {
			splitReadBreakpointString = "";
		}
	}

	public static String determineMutationType(StructuralVariant splitreadSV) {
		if (!splitreadSV.getLeftReference().equals(splitreadSV.getRightReference())) {
			return QSVConstants.CTX;
		} else {
			String orientationCat = splitreadSV.getOrientationCategory();
			if (null != orientationCat) {
                switch (orientationCat) {
                    case QSVConstants.ORIENTATION_1 -> {
                        return QSVConstants.DEL;
                    }
                    case QSVConstants.ORIENTATION_2 -> {
                        return QSVConstants.DUP;
                    }
                    case QSVConstants.ORIENTATION_3, QSVConstants.ORIENTATION_4 -> {
                        return QSVConstants.INV;
                    }
                }
			}
		}
		return "";
	}

	public void setCat1and2NonTemplate() {		
		nonTemplateSequence = "";
		if (left.getStrand() == QSVUtil.MINUS) {			
			if ((left.getQueryStart() - right.getQueryEnd()) > 1) {
				nonTemplateSequence = consensus.substring(right.getQueryEnd(), left.getQueryStart() - 1);
			} 
		} else {
			if ((right.getQueryStart() - left.getQueryEnd()) > 1) {
				nonTemplateSequence = consensus.substring(left.getQueryEnd(), right.getQueryStart() - 1);							
			} 
		}		
	}

	public void setCat3and4NonTemplate() {
		nonTemplateSequence = "";
		String orientationCategory = splitreadSV.getOrientationCategory();
		if (left.getStrand() == QSVUtil.MINUS && orientationCategory.equals(QSVConstants.ORIENTATION_4)
				|| left.getStrand() == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_3)) {
			if ((right.getQueryStart() - left.getQueryEnd()) > 1) {
				nonTemplateSequence = consensus.substring(left.getQueryEnd(), right.getQueryStart() - 1);							
			}			
		}

		if (left.getStrand() == QSVUtil.MINUS && orientationCategory.equals(QSVConstants.ORIENTATION_3)
				|| left.getStrand() == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_4)) {
			if ((left.getQueryStart() - right.getQueryEnd()) > 1) {
				nonTemplateSequence = consensus.substring(right.getQueryEnd(), left.getQueryStart() - 1);
			} 			
		}		
	}

	public void reorder() {
		boolean rearrange;
		if (left.getReference().equals(right.getReference()) &&
                left.getStartPos() > right.getStartPos()) {
			rearrange = true;        
		} else {
			rearrange = QSVUtil.reorderByChromosomes(left.getReference(), right.getReference());            
		}

		if (rearrange) {			
			SplitReadAlignment temp = left;			
			left = right;
			right = temp;		
		}		
	}

	private void getSplitReadAlignments(SplitReadAlignment align, List<BLATRecord> records) {
		left = align;
		int index1 = -1;
		int index2 = -1;
		BLATRecord record = records.getLast();
		if (record.getBlockCount() > 1 && checkBlockSize(record)) {
			getBestBlocksFromBLAT(record);
			index1 = records.size() - 1;					
		} else if (records.size() > 1 && align == null) {
			findMH = true;			
			index2 = records.size() - 2;
			index1 = records.size() - 1;
			BLATRecord r2 = records.get(index2);	
			setSplitReadAlignments(record, r2);			
		} else if (!records.isEmpty() && align != null) {
			index1 = setSecondAlignment(records);						
		}		
		if (!checkAlignments() && index1 != -1) {
			rerunSplitReadAlignments(index1, index2, records);				
		}		
	}

	private void rerunSplitReadAlignments(int index1, int index2, List<BLATRecord> records) {
		findMH = false;		
		if (index1 != -1) {
			records.remove(index1);
		}
		if (index2 != -1) {
			records.remove(index2);
		}
		if (!records.isEmpty()) {
			SplitReadAlignment a = getSingleSplitReadAlignment(left, right);
			getSplitReadAlignments(a, records);
		}		
	}
	
	public static Pair<SplitReadAlignment, SplitReadAlignment> getBestBlocksFromBLAT(BLATRecord record, boolean log, SplitReadAlignment left, SplitReadAlignment right, String confidenceLevel, StructuralVariant knownSV, int length) {
		if (log) {
			logger.info("getBestBlocksFromBLAT rec: " + record + ", left: " + left + ", right: " + right + ", confidenceLevel: " + confidenceLevel + ", knownSV: " + knownSV + ", length: " + length);
		}
		
		int[] starts = record.getUnmodifiedStarts();
		int[] blocks = record.getBlockSizes();
		int[] startPoses = record.getTStarts();
		if (starts != null && blocks != null && startPoses != null) {
			for (int i = 0; i < record.getBlockCount(); i ++) {
				int startPos = startPoses[i];
				int endPos = startPos + blocks[i] - 1;
				int start = starts[i];
				int end = start + blocks[i] - 1;
				SplitReadAlignment newAlign = new SplitReadAlignment(record.getTName(), record.getStrand(), startPos, endPos, start, end);
				if (passesNewAlignmentFilters(newAlign, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint(), length)) {
					if (left == null && right == null) {
						left = newAlign;
					} else if (left != null && right == null) {
						right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), record, left, length, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint());
						break;
					}
				}
			}
		}
		
		if (log) {
			logger.info("getBestBlocksFromBLAT - DONE, left: " + left + ", right: " + right);
		}
		return Pair.of(left, right);
	}

	private void getBestBlocksFromBLAT(BLATRecord record) {

		int[] starts = record.getUnmodifiedStarts();
		int[] blocks = record.getBlockSizes();
		int[] startPoses = record.getTStarts();
		if (starts != null && blocks != null && startPoses != null) {
			for (int i = 0; i < record.getBlockCount(); i ++) {
				int startPos = startPoses[i];
				int endPos = startPos + blocks[i] - 1;
				int start = starts[i];
				int end = start + blocks[i] - 1;
				SplitReadAlignment newAlign = new SplitReadAlignment(record.getTName(), record.getStrand(), startPos, endPos, start, end);
				if (passesNewAlignmentFilters(newAlign, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint(), length)) {
					if (left == null && right == null) {
						left = newAlign;
					} else if (left != null && right == null) {
						right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), record, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint());
						break;
					} 					
				}			
			}
		} 
	}
	
	public static Pair<SplitReadAlignment, SplitReadAlignment> setSplitReadAlignments(BLATRecord r1, BLATRecord r2, int lhsBp, int rhsBp, String confidenceLevel, int length) {
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		if (r1.getBlockCount() == 1 && (r2.getBlockCount() >= 1)) {
			left = new SplitReadAlignment(r1);
			if (passesNewAlignmentFilters(left, confidenceLevel, lhsBp, rhsBp, length)) {
				right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), r2, left, length, confidenceLevel, lhsBp, rhsBp);
			}
		} else if (r1.getBlockCount() > 1 && r2.getBlockCount() == 1) {
			right = new SplitReadAlignment(r2); 
			if (passesNewAlignmentFilters(right, confidenceLevel, lhsBp, rhsBp, length)) {
				left = getPutativeOtherAlignment(right.getQueryStart(), right.getQueryEnd(), r1, right, length, confidenceLevel, lhsBp, rhsBp);
			}	
		} else {
			left = getPutativeOtherAlignment(null, null, r1, left, length, confidenceLevel, lhsBp, rhsBp);
			right = getPutativeOtherAlignment(null, null, r2, right, length, confidenceLevel, lhsBp, rhsBp);
		}
		return Pair.of(left,  right);
	}
	
	private void setSplitReadAlignments(BLATRecord r1, BLATRecord r2) {
		/*
		 * call the static method
		 */
		Pair<SplitReadAlignment, SplitReadAlignment> pair = setSplitReadAlignments(r1, r2, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint(), confidenceLevel, length);
		left = pair.getLeft();
		right = pair.getRight();
	}

	private void getTranslocationSplitReadAlignments(SplitReadAlignment align, List<BLATRecord> leftRecords, List<BLATRecord> rightRecords) {
		//left ref		
		int index1 = -1;
		int index2 = -1;
		left = align;
		findMH = true;
		Collections.sort(leftRecords);
		Collections.sort(rightRecords);
		if (!leftRecords.isEmpty() && !rightRecords.isEmpty() && align == null) {
			BLATRecord r1 = leftRecords.getLast();
			BLATRecord r2 = rightRecords.getLast();
			index1 = leftRecords.size() - 1;
			index2 = rightRecords.size() - 1;
			setSplitReadAlignments(r1, r2);	
		} else if (left != null) {
			if (left.getReference().equals(knownSV.getLeftReference()) && !leftRecords.isEmpty()) {
				index1 = setSecondAlignment(leftRecords);
			}
			if (left.getReference().equals(knownSV.getRightReference()) && !rightRecords.isEmpty()) {
				index2 = setSecondAlignment(rightRecords);
			}
		}	

		if ( ! checkAlignments() && (index1 != -1 || index2 != -1)) {
			findMH = false;
			if (index1 != -1) {
				leftRecords.remove(index1);
			}
			if (index2 != -1) {
				rightRecords.remove(index2);
			}

			getTranslocationSplitReadAlignments(getSingleSplitReadAlignment(left, right), leftRecords, rightRecords);
		}
	}

	private int setSecondAlignment(List<BLATRecord> records) {
		findMH = true;
		BLATRecord r1 = records.getLast();
		int index1 = records.size() - 1;
		if (left == null) {
			left = getPutativeOtherAlignment(right.getQueryStart(), right.getQueryEnd(), r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint());
		} else {
			right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint());
		}

		return index1;
	}

	public static SplitReadAlignment getPutativeOtherAlignment(Integer queryStart, Integer queryEnd, BLATRecord r, SplitReadAlignment singleSplitReadAlignment, int length, String confidenceLevel, int svLhsBp, int svRhsBp) {
		int[] starts = r.getUnmodifiedStarts();
		int[] blocks = r.getBlockSizes();
		int[] startPoses = r.getTStarts();
		SplitReadAlignment align = null;
        if (r.getBlockCount() > 1) {
			for (int i = 0; i < starts.length; i++) {
				int start = starts[i];
				int end = start + blocks[i] - 1;
				int startPos = startPoses[i];
				int endPos = startPos + blocks[i] - 1;				
				align = new SplitReadAlignment(r.getTName(), r.getStrand(), startPos, endPos, start, end);
				if ( ! align.equals(singleSplitReadAlignment) && passesNewAlignmentFilters(align, confidenceLevel, svLhsBp, svRhsBp, length)) {
					return getAlignmentDifference(queryStart, queryEnd, length, align, confidenceLevel, svLhsBp, svRhsBp, length);
				} else {
					align = null;
				}
			}			
		} else {
			align = new SplitReadAlignment(r);
			if ( ! align.equals(singleSplitReadAlignment) && passesNewAlignmentFilters(align, confidenceLevel, svLhsBp, svRhsBp, length)) {
				return getAlignmentDifference(queryStart, queryEnd, length, align, confidenceLevel, svLhsBp, svRhsBp, length);
			} else {
				align = null;
			}
		}
		return align;
	}

	public static SplitReadAlignment getAlignmentDifference(Integer queryStart, Integer queryEnd, int difference, SplitReadAlignment align, String confidenceLevel, int svLhsBp, int svRhsBp, int length) {
		if (passesBreakpointFilter(align, confidenceLevel, svLhsBp, svRhsBp)) {
			if ((queryStart == null || queryEnd == null)
					|| (queryStart != null && queryEnd != null && matchingQueryString(difference, queryStart, queryEnd, align, length))) {
				return align;
			}
		}
		return null;		
	}

	private boolean checkAlignments() {		
		if (left == null || right == null) {	
			return false;
		}
		if ( ! passesBreakpointFilter(left, right, confidenceLevel, knownSV.getLeftBreakpoint(), knownSV.getRightBreakpoint())
				|| ! passesQueryPositionFilter(left,right, length) 
				|| ! queryLengthFilter(left, right, length)) {
			if (left.getInsertSize() < right.getInsertSize()) {
				left = null;
			} else {
				right = null;
			}
			return false;
		}
		return true;
	}	

	public static boolean passesBreakpointFilter(SplitReadAlignment a,	SplitReadAlignment b, String confidenceLevel, int svLhsBp, int svRhsBp) {
		if (QSVConstants.LEVEL_SINGLE_CLIP.equals(confidenceLevel)) {
			return true;
		} else {
            return (getMatch(a.getStartPos(), svLhsBp) || getMatch(a.getEndPos(), svLhsBp) &&
                    getMatch(b.getStartPos(), svRhsBp) || getMatch(b.getEndPos(), svRhsBp))
                    ||
                    (getMatch(a.getStartPos(), svRhsBp) || getMatch(a.getEndPos(), svRhsBp) &&
                            getMatch(b.getStartPos(), svLhsBp) || getMatch(b.getEndPos(), svLhsBp));
		}
    }

	public static boolean passesQueryPositionFilter(SplitReadAlignment left, SplitReadAlignment right, int length) {
		return matchingQueryString(length, left.getQueryStart(), left.getQueryEnd(), right, length)
				&& matchingQueryString(length, right.getQueryStart(), right.getQueryEnd(), left, length); 
	}	

	public static boolean queryLengthFilter(SplitReadAlignment left, SplitReadAlignment right, int length) {
		int leftIS = left.getInsertSize();
		int rightIS = right.getInsertSize();
		if (leftIS < 20 || rightIS < 20 || leftIS > (length - 20) || rightIS > (length - 20)) {
			return false;
		}
		if (left.getQueryStart() >= right.getQueryStart() && left.getQueryStart() <= right.getQueryEnd() &&
				left.getQueryEnd() >= right.getQueryStart() && left.getQueryEnd() <= right.getQueryEnd()) {
			return false;
		}
        return right.getQueryStart() < left.getQueryStart() || right.getQueryStart() > left.getQueryEnd() ||
                right.getQueryEnd() < left.getQueryStart() || right.getQueryEnd() > left.getQueryEnd();
    }

	public static boolean passesSizeFilter(SplitReadAlignment a, int length) {
		int iSize = a.getInsertSize();
		if (iSize < 20) {
			return false;
		}
		double max = ((double) length - 20) / length;
		return ! ((double) iSize / length > max) ;			
	}	

	public static boolean checkBlockSize(BLATRecord r) {
		if (r.getBlockCount() > 1) {
			int[] blocks = r.getBlockSizes();
			int passCount = 0;
            for (int block : blocks) {
                if (block > 20) {
                    passCount++;
                }
            }
            return passCount >= 2;
		}		
		return false;
	}

	public static boolean passesBreakpointFilter(SplitReadAlignment a, String confidenceLevel, int svLhsBp, int svRhsBp) {
		if (QSVConstants.LEVEL_SINGLE_CLIP.equals(confidenceLevel)) {
			return true;
		} else {
            return getMatch(a.getStartPos(), svLhsBp) || getMatch(a.getEndPos(), svLhsBp) ||
                    getMatch(a.getStartPos(), svRhsBp) || getMatch(a.getEndPos(), svRhsBp);
		}
    }

	public static boolean getMatch(int pos, int compareBp) {
		return (pos >= compareBp - 50 && pos <= compareBp + 50) ;
	}

	public static boolean passesNewAlignmentFilters(SplitReadAlignment newAlign, String confidenceLevel, int svLhsBp, int svRhsBp, int length) {
		return (passesBreakpointFilter(newAlign, confidenceLevel, svLhsBp, svRhsBp) && passesSizeFilter(newAlign, length)) ;
	}	

	public static boolean matchingQueryString(int difference, int queryStart, int queryEnd, SplitReadAlignment newAlign, int length) {
		int start = newAlign.getQueryStart();
		int end = newAlign.getQueryEnd();
		int buffer = 50;

		if (queryEnd > start && (end >= queryStart - buffer && end <= queryStart + buffer)) {				 
			if ((difference == length) || Math.abs(end - queryStart) < difference) {
				return true;
			}			
		}
		if (queryStart < end && (start >= queryEnd - buffer && start <= queryEnd + buffer)) {
            return (difference == length) || Math.abs(start - queryEnd) < difference;
		}
		return false;
	}

	public String getMutationType() {
		return this.mutationType;
	}

}
