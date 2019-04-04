/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

public class SplitReadContig {

	private static final QLogger logger = QLoggerFactory.getLogger(SplitReadContig.class);

	private StructuralVariant knownSV;
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
	private final BLAT blat;
	private final QSVParameters parameters;
	private String mutationType;
	private String blatFile;
	private String clipContig;
	private String softclipDir;
	private String name;

	

	public SplitReadContig(BLAT blat, QSVParameters p, String leftReference, String rightReference,
			int leftBreakpoint, int rightBreakpoint, String orientationCategory) {

		if (orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
			this.knownSV = new StructuralVariant(rightReference, leftReference, rightBreakpoint, leftBreakpoint, orientationCategory);		
		} else {
			this.knownSV = new StructuralVariant(leftReference, rightReference, leftBreakpoint, rightBreakpoint, orientationCategory);		
		}
		this.blat = blat;
		this.parameters = p;
	}

	public SplitReadContig(BLAT blat, QSVParameters p, 
			String softclipDir, String leftReference, String rightReference, int leftRecordBreakpoint, int rightRecordBreakpoint, 
			Set<String> expectedPairClassifications, String clipContig, String confidence, 
			String orienationCategory, String referenceFile, Map<String, List<Chromosome>> chromosomes, boolean hasSoftClipEvidence, String blatFile) throws Exception {

		this(blat, p, leftReference, rightReference, leftRecordBreakpoint, rightRecordBreakpoint, orienationCategory);

		this.hasSoftClipEvidence = hasSoftClipEvidence;
		this.confidenceLevel = confidence;
		this.chromosomes = chromosomes;
		getSplitReadConsensus(softclipDir, expectedPairClassifications, clipContig, blatFile);
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
		return "splitcon_" + knownSV.toString() + "_" + isTumour() + "_" + date.getTime() + "_" + random;
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

	private void getSplitReadConsensus(String softclipDir, Set<String> expectedPairClassifications, String clipContig, String blatFile) throws Exception {		
		Map<String, UnmappedRead[]> map = new HashMap<>();
		List<UnmappedRead> splitReads = new ArrayList<>();		

		this.microhomology = QSVConstants.UNTESTED;
		this.nonTemplateSequence = QSVConstants.UNTESTED;

		// setup the SamReader just once
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(parameters.getInputBamFile(), "silent");) {

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
		assemble(splitReads, clipContig, softclipDir, blatFile);
	}

	private void assemble(List<UnmappedRead> splitReads, String clipContig, String softclipDir, String blatFile) throws Exception {
		QSVAssemble qAssemble = new QSVAssemble();

		ConsensusRead fullContig =  qAssemble.getFinalContig(splitReads);

		if (parameters.isTumor()) {
			this.consensus = clipContig;
		}
		if (fullContig != null) {
			this.consensus = fullContig.getSequence();			
		}

		this.clipContig = clipContig;
		this.softclipDir = softclipDir;
		this.blatFile = blatFile;
		this.name = getName();
		File blatF = new File(blatFile);

		try (FileWriter fw = new FileWriter(blatF, true);
				BufferedWriter writer = new BufferedWriter(fw);) {

			writer.write(">" + name + QSVUtil.getNewLine());
			writer.write(consensus + QSVUtil.getNewLine());
		}

		//		this.knownSV = new StructuralVariant("chr22", "chr22", 21353096, 21566646, "3");
		//		this.consensus = "CAGATAGAAGTTTCTAGAGCAACAACAGCTGCCTTTCTTTCGGGAATAATCCGTGATAAAGAAATAAATCATCAGAGGCAGACAGGAGCATTGTAGGAACAGCACCCCGAGCCAGTGAGTAGATGAAGGAGCTGGCCTTAGCCAAAAAGGAGGGCAGAGGGACACCTGGCCCACATGGCTGGGCGCTGCAGCCTGCACTCCACTTCCAGGTGCTGACATATCCTCCCCAGCCATGGTCCTGGGCCTGCTGTGT";

		//			this.consensus = "TTTTCCCCACATCCTTGCCAGTATTCATTATCGCCTGTCTATTTGAACACAAAGCCATTTTACCTGGGGTAAGATGATATTTCATTGTGATTTTGCTTTGCATTTCTTTCATGATTAGTGATGATGAACATTTTTAAATAACTGTTGCCACGAGAGTACACAGAGCAAAGGAGACAGGGTCATTTATACCCTGATGCGTCCACCCCACTGCTGTGTCCGGTTTCCATTGG";
		//		this.knownSV = new StructuralVariant("chr10", "chr17", 
		//				13230927, 12656100, "4");	

		//		10   +   13231026	17   +   12656098		
		//		Cat1RC
		//		17   -   12656098	10   -   13231026		
		//
		//		Cat2
		//		17   +   12656199	10   +   13230926		
		//		Cat2RC
		//		10   -   13230926	17   -   12656199		
		//
		//		Cat3
		//		10   +   13231026	17   -   12656200		
		//		Cat3RC
		//		17   +   12656200	10   -   13231026		
		//
		//		Cat4
		//		10   -   13230927	17   +   12656100		
		//		Cat4RC
		//		17   -   12656100	10   +   13230927	
		//cat1
		//		this.consensus= "AGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTGTTTTAGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCA";
		//		this.consensus= "TGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCTAAAACACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCT";
		//		//cat2
		//		this.consensus = "AGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCATTTTAGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTG";
		//	    this.consensus = "CACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCTAAAATGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCT";
		//cat3
		//	    this.consensus= "AGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTGTTTTTGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCT";
		//		this.consensus= "AGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCAAAAACACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCT";
		//	    //cat4
		//		this.consensus= "CACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCTTTTTAGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCA";
		//		this.consensus= "TGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCTAAAAAGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTG";

		//		this.consensus = "CAGATAGAAGTTTCTAGAGCAACAACAGCTGCCTTTCTTTCGGGAATAATCCGTGATAAAGAAATAAATCATCAGAGGCAGACAGGAGCATTGTAGGAACAGCACCCCGAGCCAGTGAGTAGATGAAGGAGCTGGCCTTAGCCAAAAAGGAGGGCAGAGGGACACCTGGCCCACATGGCTGGGCGCTGCAGCCTGCACTCCACTTCCAGGTGCTGACATATCCTCCCCAGCCATGGTCCTGGGCCTGCTGTGT";
		//		this.consensus = "ACTCAGCCCCAGCTGACCAGATCGCCCCTGATCAAGCCCCACAGCCCAGGTTTGCACTGACCAGACACCAAACATCTGGCAGCCAATAGGTCCCCACTCACCAAAACCCCTAAAACTTGATCCCACTAATAAGACCCTCTCTAAGCAGACCCCTGCTGACCACGATCCCACTAAATAGGCCTCACTGACCTACCACTGACCAGGCCCACAATGATCAGGCTCCTCCTAACCACACCGGAAATCCAAGCGGCAATGATATGTT";
		//		this.consensus = "";
		//		this.consensus = "";
		//		this.consensus = "";
		//		this.consensus = "";
		//		this.consensus = "";


	}

	public void findSplitRead() throws Exception {
		if (consensus != null) {

			this.length = consensus.length();
			if (length > 0) {
				List<BLATRecord> records = blat.getBlatResults(blatFile, knownSV.getLeftReference(), knownSV.getRightReference(), name);			
				if (records.size() > 0) {	

					parseConsensusAlign(records);
					if ( ! isPotentialSplitRead && isTumour() && clipContig.length() > 0) {
						consensus = clipContig;
						length = consensus.length();
						if (length > 0) {
							records = blat.alignConsensus(softclipDir, name + "_clip", clipContig, knownSV.getLeftReference(), knownSV.getRightReference());			
							if (records.size() > 0) {		
								parseConsensusAlign(records);
								if ( ! isPotentialSplitRead && isTumour()) {
									consensus = clipContig;	
								}
							}
						}
					}
				}			
			}		
		}
	}

	private void readSplitReads(SamReader reader, Map<String, UnmappedRead[]> map, List<UnmappedRead> splitReads, 
			String reference, int intBreakpoint, Set<String> expectedPairClassifications) throws QSVException {
		//		SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);  
		//        SamReader reader = SAMFileReaderFactory.createSAMFileReader(parameters.getInputBamFile(), "silent");
		int buffer = parameters.getUpperInsertSize().intValue() + 200;
		int count = 0;
		
		SAMRecordIterator iter = reader.queryOverlapping(reference, intBreakpoint-buffer, intBreakpoint+buffer);

		Set<String> readGroupIds = parameters.getReadGroupIdsAsSet();

		while (iter.hasNext()) {
			SAMRecord r = iter.next();	 

			if ( ! r.getDuplicateReadFlag()) {
				if (readGroupIds.contains(r.getReadGroup().getId())) {
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
							splitReads.add(new UnmappedRead(r, true));
							if (r.getReadUnmappedFlag()) {
								splitReads.add(new UnmappedRead(r, true, true));
							}
						}
					} else {

						parameters.getAnnotator().annotate(r);
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
									String key = r.getReadName() + ":" + r.getReadGroup().getId();
									UnmappedRead[] arr = map.get(key);
									
									if (null == arr) {
										arr = new UnmappedRead[]{new UnmappedRead(r, true), null};
										map.put(key, arr);
									} else {
										UnmappedRead newRead = new UnmappedRead(r, true);
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
				if (r.getReference().equals(knownSV.getLeftReference())) {
					leftRecords.add(r);
				}
				if (r.getReference().equals(knownSV.getRightReference())) {
					rightRecords.add(r);
				}
			}
			getTranslocationSplitReadAlignments(null, leftRecords, rightRecords);	
		}



		if (left != null & right != null) {

			//rearrange
			reorder();				
			leftSequence = null;
			rightSequence = null;
			splitreadSV.setReferences(left.getReference(), right.getReference());

			if (left.getStrand() == right.getStrand()) {
				//cat 1 or 2			
				if ((leftLower(left.getQueryStart().intValue(), right.getQueryStart().intValue()) && left.strandNegative() || ! leftLower(left.getQueryStart().intValue(), right.getQueryStart().intValue()) && left.strandPositive())) {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_2, left.getStartPos(), right.getEndPos());					
				} else {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_1,left.getEndPos(), right.getStartPos());
				}	

			} else {
				//cat 3 or 4
				if (( ! leftLower(left.getQueryStart().intValue(), right.getQueryStart().intValue()) && left.strandNegative()) || leftLower(left.getQueryStart().intValue(), right.getQueryStart().intValue()) && left.strandPositive()) {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_3, left.getEndPos(), right.getEndPos());
				} else {
					splitreadSV.setBreakpointsByCategory(QSVConstants.ORIENTATION_4, left.getStartPos(), right.getStartPos());
				}				
			}

			if (splitreadSV.getOrientationCategory() != null) {

				String orientationCategory = splitreadSV.getOrientationCategory();
				leftSequence = consensus.substring(left.getQueryStart() - 1, left.getQueryEnd());
				rightSequence = consensus.substring(right.getQueryStart() - 1, right.getQueryEnd());

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

				if (nonTemplateSequence.equals("")) {
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

	public static boolean leftLower(int leftStart, int rightStart) {
		return leftStart < rightStart;
	}

	public static boolean needToReverseComplement(String orientationCategory, char leftStrand) {
		return ((leftStrand == QSVUtil.MINUS && ! orientationCategory.equals(QSVConstants.ORIENTATION_4)) ||
				(leftStrand == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_4))) ;
	}

	void calculateMicrohomology() throws QSVException, IOException {
		if (nonTemplateSequence == null) {
			this.microhomology = QSVConstants.UNTESTED;
		} else if (nonTemplateSequence.length() > 0 && !nonTemplateSequence.equals(QSVConstants.NOT_FOUND)) {
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
					if (tempMh.length() > 0) {
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

	void recheckMicrohomologyForSingleAlignment() throws QSVException, IOException {
		
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
		if (microhomology.equals("")) {
			microhomology = QSVConstants.NOT_FOUND;
		}		
	}

	public static String getReferenceSequence(String reference, int breakpoint, int leftBuffer, int rightBuffer, Map<String, List<Chromosome>> map) {		
		List<Chromosome> list = map.get(reference);

		if (list != null) {
			Chromosome c = list.get(0);			
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
				if (orientationCat.equals(QSVConstants.ORIENTATION_1)) {
					return QSVConstants.DEL;
				} else if (orientationCat.equals(QSVConstants.ORIENTATION_2)) {
					return QSVConstants.DUP;
				} else if (orientationCat.equals(QSVConstants.ORIENTATION_3) || orientationCat.equals(QSVConstants.ORIENTATION_4)) {
					return QSVConstants.INV;
				}
			}
		}
		return "";
	}

	public void setCat1and2NonTemplate() {		
		nonTemplateSequence = "";
		if (left.getStrand() == QSVUtil.MINUS) {			
			if ((left.getQueryStart().intValue() - right.getQueryEnd().intValue()) > 1) {
				nonTemplateSequence = consensus.substring(right.getQueryEnd(), left.getQueryStart() - 1);
			} 
		} else {
			if ((right.getQueryStart().intValue() - left.getQueryEnd().intValue()) > 1) {
				nonTemplateSequence = consensus.substring(left.getQueryEnd(), right.getQueryStart() - 1);							
			} 
		}		
	}

	public void setCat3and4NonTemplate() {
		nonTemplateSequence = "";
		String orientationCategory = splitreadSV.getOrientationCategory();
		if (left.getStrand() == QSVUtil.MINUS && orientationCategory.equals(QSVConstants.ORIENTATION_4)
				|| left.getStrand() == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_3)) {
			if ((right.getQueryStart().intValue() - left.getQueryEnd().intValue()) > 1) {
				nonTemplateSequence = consensus.substring(left.getQueryEnd(), right.getQueryStart() - 1);							
			}			
		}

		if (left.getStrand() == QSVUtil.MINUS && orientationCategory.equals(QSVConstants.ORIENTATION_3)
				|| left.getStrand() == QSVUtil.PLUS && orientationCategory.equals(QSVConstants.ORIENTATION_4)) {
			if ((left.getQueryStart().intValue() - right.getQueryEnd().intValue()) > 1) {
				nonTemplateSequence = consensus.substring(right.getQueryEnd(), left.getQueryStart() - 1);
			} 			
		}		
	}

	public void reorder() {
		boolean rearrange;
		if (left.getReference().equals(right.getReference()) && 
				left.getStartPos().intValue() > right.getStartPos().intValue()) {
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
		BLATRecord record = records.get(records.size() - 1);		
		if (record.getBlockCount() > 1 && checkBlockSize(record)) {
			getBestBlocksFromBLAT(record);
			index1 = records.size() - 1;					
		} else if (records.size() > 1 && align == null) {
			findMH = true;			
			index2 = records.size() - 2;
			index1 = records.size() - 1;
			BLATRecord r2 = records.get(index2);	
			setSplitReadAlignments(record, r2);			
		} else if (records.size() >= 1 && align != null) {
			index1 = setSecondAlignment(records);						
		}		
		if (!checkAlignments() && (index1 != -1 || index2 != -1)) {
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
		if (records.size() > 0) {
			SplitReadAlignment a = getSingleSplitReadAlignment(left, right);
			getSplitReadAlignments(a, records);
		}		
	}

	private void getBestBlocksFromBLAT(BLATRecord record) {

		int[] starts = record.getUnmodifiedStarts();
		int[] blocks = record.getBlockSizes();
		int[] startPoses = record.gettStarts();
		if (starts != null && blocks != null && startPoses != null) {
			for (int i = 0; i < record.getBlockCount(); i ++) {
				int startPos = startPoses[i];
				int endPos = startPos + blocks[i] - 1;
				int start = starts[i];
				int end = start + blocks[i] - 1;
				SplitReadAlignment newAlign = new SplitReadAlignment(record.getReference(), record.getStrand(), startPos, endPos, start, end);
				if (passesNewAlignmentFilters(newAlign, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue(), length)) {
					if (left == null && right == null) {
						left = newAlign;
					} else if (left != null && right == null) {
						right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), record, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
						break;
					} 					
				}			
			}
		} 
	}

	private void setSplitReadAlignments(BLATRecord r1, BLATRecord r2) {
		if (r1.getBlockCount() == 1 && (r2.getBlockCount() >= 1)) {				
			left = new SplitReadAlignment(r1); 
			if (passesNewAlignmentFilters(left, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue(), length)) {
				right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), r2, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
			}
		} else if (r1.getBlockCount() >1 && r2.getBlockCount() == 1) {				
			right = new SplitReadAlignment(r2); 
			if (passesNewAlignmentFilters(right, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue(), length)) {
				left = getPutativeOtherAlignment(right.getQueryStart(), right.getQueryEnd(), r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
			}	
		} else {
			left = getPutativeOtherAlignment(null, null, r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
			right = getPutativeOtherAlignment(null, null, r2, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
		}		
	}

	private void getTranslocationSplitReadAlignments(SplitReadAlignment align, List<BLATRecord> leftRecords, List<BLATRecord> rightRecords) {
		//left ref		
		int index1 = -1;
		int index2 = -1;
		left = align;
		findMH = true;
		Collections.sort(leftRecords);
		Collections.sort(rightRecords);
		if (leftRecords.size() > 0 && rightRecords.size() > 0 && align == null) {		
			BLATRecord r1 = leftRecords.get(leftRecords.size() - 1);
			BLATRecord r2 = rightRecords.get(rightRecords.size() - 1);
			index1 = leftRecords.size() - 1;
			index2 = rightRecords.size() - 1;
			setSplitReadAlignments(r1, r2);	
		} else if (left != null) {
			if (left.getReference().equals(knownSV.getLeftReference()) && leftRecords.size() > 0) {				
				index1 = setSecondAlignment(leftRecords);
			}
			if (left.getReference().equals(knownSV.getRightReference()) && rightRecords.size() > 0) {				
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
		BLATRecord r1 = records.get(records.size() - 1);
		int index1 = records.size() - 1;
		if (left == null) {
			left = getPutativeOtherAlignment(right.getQueryStart(), right.getQueryEnd(), r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
		} else {
			right = getPutativeOtherAlignment(left.getQueryStart(), left.getQueryEnd(), r1, getSingleSplitReadAlignment(left, right), length, confidenceLevel, knownSV.getLeftBreakpoint().intValue(), knownSV.getRightBreakpoint().intValue());
		}

		return index1;
	}

	public static SplitReadAlignment getPutativeOtherAlignment(Integer queryStart, Integer queryEnd, BLATRecord r, SplitReadAlignment singleSplitReadAlignment, int length, String confidenceLevel, int svLhsBp, int svRhsBp) {
		int[] starts = r.getUnmodifiedStarts();
		int[] blocks = r.getBlockSizes();
		int[] startPoses = r.gettStarts();
		SplitReadAlignment align = null;
		int difference = length;
		if (r.getBlockCount() > 1) {
			for (int i = 0; i < starts.length; i++) {
				int start = starts[i];
				int end = start + blocks[i] - 1;
				int startPos = startPoses[i];
				int endPos = startPos + blocks[i] - 1;				
				align = new SplitReadAlignment(r.getReference(), r.getStrand(), startPos, endPos, start, end);
				if ( ! align.equals(singleSplitReadAlignment) && passesNewAlignmentFilters(align, confidenceLevel, svLhsBp, svRhsBp, length)) {
					return getAlignmentDifference(queryStart, queryEnd, difference, align, confidenceLevel, svLhsBp, svRhsBp, length);
				} else {
					align = null;
				}
			}			
		} else {
			align = new SplitReadAlignment(r);
			if ( ! align.equals(singleSplitReadAlignment) && passesNewAlignmentFilters(align, confidenceLevel, svLhsBp, svRhsBp, length)) {
				return getAlignmentDifference(queryStart, queryEnd, difference, align, confidenceLevel, svLhsBp, svRhsBp, length);
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
		if ( ! passesBreakpointFilter(left, right, confidenceLevel, knownSV.getLeftBreakpoint().intValue() ,knownSV.getRightBreakpoint().intValue()) 
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
			if ((getMatch(a.getStartPos(), svLhsBp) || getMatch(a.getEndPos(), svLhsBp) &&
					getMatch(b.getStartPos(),  svRhsBp) || getMatch(b.getEndPos(),  svRhsBp))
					||
					(getMatch(a.getStartPos(), svRhsBp) || getMatch(a.getEndPos(), svRhsBp) &&
							getMatch(b.getStartPos(), svLhsBp) || getMatch(b.getEndPos(), svLhsBp))	
					) {

				return true;		
			}
		}
		return false;
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
		if (right.getQueryStart() >= left.getQueryStart() && right.getQueryStart() <= left.getQueryEnd() &&
				right.getQueryEnd() >= left.getQueryStart() && right.getQueryEnd() <= left.getQueryEnd()	
				) {
			return false;
		}		
		return true;
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
			for (int i = 0; i < blocks.length; i++) {
				if (blocks[i] > 20) {
					passCount++;
				}
			}
			if (passCount >= 2) {
				return true;
			}
		}		
		return false;
	}

	public static boolean passesBreakpointFilter(SplitReadAlignment a, String confidenceLevel, int svLhsBp, int svRhsBp) {
		if (QSVConstants.LEVEL_SINGLE_CLIP.equals(confidenceLevel)) {
			return true;
		} else {
			if (getMatch(a.getStartPos().intValue(), svLhsBp) || getMatch(a.getEndPos().intValue(), svLhsBp) ||
					getMatch(a.getStartPos().intValue(), svRhsBp) || getMatch(a.getEndPos().intValue(), svRhsBp)) {
					return true;
			}
		}
		return false;
	}
	
	public static boolean passesBreakpointFilter(SplitReadAlignment a, String confidenceLevel, StructuralVariant sv) {
		return passesBreakpointFilter(a, confidenceLevel, sv.getLeftBreakpoint().intValue(), sv.getRightBreakpoint().intValue());
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
			if ((difference == length) || Math.abs(start - queryEnd) < difference) {
				return true;
			}
		}
		return false;
	}

	public String getMutationType() {
		return this.mutationType;
	}
}
