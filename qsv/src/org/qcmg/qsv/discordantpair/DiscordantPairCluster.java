/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.annotate.Annotator;
import org.qcmg.qsv.discordantpair.MatePair.ReadMateLeftEndComparator;
import org.qcmg.qsv.discordantpair.MatePair.ReadMateLeftStartComparator;
import org.qcmg.qsv.discordantpair.MatePair.ReadMateRightEndComparator;
import org.qcmg.qsv.discordantpair.MatePair.ReadMateRightStartComparator;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class to represent discordant pair evidence for an SV
 */
public class DiscordantPairCluster {

	private static final ReadMateLeftStartComparator READ_MATE_LEFT_START_COMP = new MatePair.ReadMateLeftStartComparator();
	private static final ReadMateLeftEndComparator READ_MATE_LEFT_END_COMP = new MatePair.ReadMateLeftEndComparator();
	private static final ReadMateRightStartComparator READ_MATE_RIGHT_START_COMP = new MatePair.ReadMateRightStartComparator();
	private static final ReadMateRightEndComparator READ_MATE_RIGHT_END_COMP = new MatePair.ReadMateRightEndComparator();

	private final List<MatePair> clusterMatePairs;
	private final List<MatePair> matchedNormalMatePairs;
	private final String leftReferenceName;
	private final String rightReferenceName;
	private final String zp;
	private final int windowSize;
	private final String sampleType;
	private final Map<String, AtomicInteger> strandOrientations;    
	private final int qPrimerThreshold;

	private int lowConfidenceNormalMatePairs;
	private int leftStart = -1;
	private int rightStart = -1;
	private int leftEnd = -1;
	private int rightEnd = -1;
	private String type;
	private String strandOrientation = "";
	private int id;
	private int compareLeftStart;
	private int compareRightStart;
	private int compareLeftEnd;
	private int compareRightEnd;
	private String referenceKey;
	private boolean rescuedTumour;
	private QPrimerCategory qPrimerCateory;
	private boolean isQCMG = false;		

	public DiscordantPairCluster(String leftReferenceName, String rightReferenceName, String zp, QSVParameters findParameters, boolean isQCMG) {
		this.clusterMatePairs = new ArrayList<MatePair>();
		this.matchedNormalMatePairs = new ArrayList<MatePair>();
		this.leftReferenceName = leftReferenceName;
		this.rightReferenceName = rightReferenceName;
		this.zp = zp;
		this.type = "";
		this.sampleType = findParameters.getFindType();
		this.windowSize = findParameters.getUpperInsertSize();
		this.strandOrientations = new HashMap<>(8);
		this.qPrimerThreshold = findParameters.getqPrimerThreshold();
		this.lowConfidenceNormalMatePairs = 0;
		this.isQCMG = isQCMG;
	}

	public int getLowConfidenceNormalMatePairs() {
		return lowConfidenceNormalMatePairs;
	}

	public String getOrientationCategory() {
		return qPrimerCateory.getPrimaryCategoryNo();
	}

	public String getLeftReferenceName() {
		return leftReferenceName;
	}

	public String getRightReferenceName() {
		return rightReferenceName;
	}

	public int getLeftStart() {
		return leftStart;
	}

	public void setLeftStart(int leftStart) {
		this.leftStart = leftStart;
	}

	public int getRightStart() {
		return rightStart;
	}

	public void setRightStart(int rightStart) {
		this.rightStart = rightStart;
	}

	public int getLeftEnd() {
		return leftEnd;
	}

	public void setLeftEnd(int leftEnd) {
		this.leftEnd = leftEnd;
	}

	public int getRightEnd() {
		return rightEnd;
	}

	public void setRightEnd(int rightEnd) {
		this.rightEnd = rightEnd;
	}

	public List<MatePair> getClusterMatePairs() {
		return clusterMatePairs;
	}

	public List<MatePair> getMatchedReadPairs() {
		return matchedNormalMatePairs;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getZp() {
		return zp;
	}

	public int getCompareLeftStart() {
		return compareLeftStart;
	}

	public void setCompareLeftStart(int compareLeftStart) {
		this.compareLeftStart = compareLeftStart;
	}

	public int getCompareRightStart() {
		return compareRightStart;
	}

	public void setCompareRightStart(int compareRightStart) {
		this.compareRightStart = compareRightStart;
	}

	public int getCompareLeftEnd() {
		return compareLeftEnd;
	}

	public int getCompareRightEnd() {
		return compareRightEnd;
	}

	public String getReferenceKey() {
		return referenceKey;
	}

	public boolean isRescuedTumour() {
		return rescuedTumour;
	}

	public String getStrandOrientation() {
		return strandOrientation;
	}

	public void setStrandOrientation(String strandOrientation) {
		this.strandOrientation = strandOrientation;
	}

	public String getMutationType(boolean isDCC) {
		if (isQCMG && isDCC) {
			return zp;
		} else {
			return QSVUtil.getMutationByPairGroup(zp);
		}		
	}

	public String getRealMutationType() {
		return QSVUtil.getMutationByPairGroup(zp);		
	}

	public Integer getQPrimerThreshold() {
		return this.qPrimerThreshold;
	}

	public void setClusterEnds() {
		findLeftStartOfCluster();
		findLeftEndOfCluster();
		findRightStartOfCluster();
		findRightEndOfCluster();               
	}

	public void findLeftStartOfCluster() {
		clusterMatePairs.sort(READ_MATE_LEFT_START_COMP);
		this.leftStart = clusterMatePairs.get(0).getLeftMate().getStart();
	}

	/**
	 * Sort mate pairs by left mate start and find left end of the cluster
	 */
	 public void findLeftEndOfCluster() {
		clusterMatePairs.sort(READ_MATE_LEFT_END_COMP);
		int indexLast = clusterMatePairs.size() - 1;
		this.leftEnd = clusterMatePairs.get(indexLast).getLeftMate().getEnd();
	 }

	 /**
	  * Sort mate pairs by the right mate start and find right start
	  */
	 public void findRightStartOfCluster() {
		 clusterMatePairs.sort(READ_MATE_RIGHT_START_COMP);
		 this.rightStart = clusterMatePairs.get(0).getRightMate().getStart();
	 }    

	 private void findRightEndOfCluster() {
		 clusterMatePairs.sort(READ_MATE_RIGHT_END_COMP);
		 int indexLast = clusterMatePairs.size() - 1;
		 this.rightEnd = clusterMatePairs.get(indexLast).getRightMate().getEnd();
	 }

	 String countStrandOrientations() {
		 strandOrientations.put("+/+", new AtomicInteger());
		 strandOrientations.put("-/-", new AtomicInteger());
		 strandOrientations.put("+/-", new AtomicInteger());
		 strandOrientations.put("-/+", new AtomicInteger());

		 for (MatePair m: clusterMatePairs) {
			 String orientation = m.getStrandOrientation();
			 strandOrientations.get(orientation).incrementAndGet();

//			 if (orientation.equals("-/-")) {
//				 strandOrientations.get("-/-").incrementAndGet();
//			 } else if (orientation.equals("+/-")){
//				 strandOrientations.get("+/-").incrementAndGet();
//			 } else if (orientation.equals("-/+")) {
//				 strandOrientations.get("-/+").incrementAndGet();
//			 } else {
//				 strandOrientations.get("+/+").incrementAndGet();
//			 }
		 }

		 int maxValue = 0;
		 String key = "";

		 for (Map.Entry<String, AtomicInteger> entry : strandOrientations.entrySet()) {
			 if (entry.getValue().get() == maxValue && maxValue != 0) {
				 key += ";" + entry.getKey();
			 } else {
				 if (entry.getValue().get() > maxValue) {
					 maxValue = entry.getValue().get();
					 key = entry.getKey();
				 }
			 }
		 }        
		 setStrandOrientation(key);
		 return getStrandOrientation();
	 }

	 public String getChrRegionFrom() {
		 if (qPrimerCateory.getPrimaryCategoryNo().equals(QSVConstants.ORIENTATION_2)) {
			 return this.getRightReferenceName() + ":" + rightStart + "-" + rightEnd;
		 } else {
			 return this.getLeftReferenceName() + ":" + leftStart + "-" + leftEnd;
		 }

	 }

	 public String getChrRegionTo() {
		 if (qPrimerCateory.getPrimaryCategoryNo().equals(QSVConstants.ORIENTATION_2)) {
			 return this.getLeftReferenceName() + ":" + leftStart + "-" + leftEnd;
		 } else {
			 return this.getRightReferenceName() + ":" + rightStart + "-" + rightEnd; 
		 }        
	 }

	 public int findBestCluster(int startPairIndex, int nextFindIndex, int clusterSize) { 

		 //find outlier: check to see if first value is an outlier 

		 int nextIndex = 0;
		 List<MatePair> testPairs = copyAndOrderCurrentClusterPairs();
		 List<MatePair> badPairs = new ArrayList<MatePair>();

		 //check all pairs firstly
		 int startPos = testPairs.get(0).getRightMate().getStart();
		 int range = startPos + windowSize;
		 int initialCount = 0;
		 for (MatePair m : testPairs) {
			 int rightMateStart = m.getRightMate().getStart();
			 if (rightMateStart >= startPos && rightMateStart <= range) {
				 initialCount++;
			 } else {
				 badPairs.add(m);
			 }
		 }

		 int maxStart = clusterMatePairs.size() - clusterSize;
		 if (maxStart < 0) {
			 maxStart = 0;
		 }

		 int matchCount = 0;
		 List<MatePair> badStartPairs = new ArrayList<MatePair>();
		 List<MatePair> removedPairs = new ArrayList<MatePair>();

		 for (int i=0; i<maxStart; i++) {
			 removedPairs.add(testPairs.remove(0));
			 startPos = testPairs.get(0).getRightMate().getStart();
			 range = startPos + windowSize;
			 int count = 0;
			 List<MatePair> currentBadPairs = new ArrayList<MatePair>();
			 for (MatePair m : testPairs) {
				 int rightMateStart = m.getRightMate().getStart();
				 if (rightMateStart >= startPos && rightMateStart <= range) { 
					 count++;
				 } else {
					 currentBadPairs.add(m);
				 }
			 }

			 if (count > matchCount) {
				 badStartPairs.clear();
				 badStartPairs.addAll(currentBadPairs);
				 badStartPairs.addAll(removedPairs);
				 matchCount = count;
			 } 
		 }
		 //determine which method gives the best cluster
		 if (initialCount >= matchCount) {
			 nextIndex = resolveBadPairs(badPairs);
		 } else {
			 if (matchCount >= clusterSize) {
				 nextIndex = resolveBadPairs(badStartPairs);

			 } else {
				 nextIndex = resolveBadPairs(badPairs);
			 }
		 }

		 Collections.sort(clusterMatePairs, READ_MATE_LEFT_START_COMP);
		 return nextIndex;
	 }


	 private int resolveBadPairs(List<MatePair> badPairs) {
		 int nextIndex = 0;
		 Collections.sort(badPairs, READ_MATE_RIGHT_START_COMP);  

		 for (int i = (badPairs.size() -1); i >= 0; i--) {
			 MatePair bad = badPairs.get(i);

			 clusterMatePairs.remove(bad);
			 int index = getIndexOfPair(bad);
			 if (index == clusterMatePairs.size() -1) {
				 nextIndex++;
			 }
		 }

		 if(clusterMatePairs.size() == 1) {
			 clusterMatePairs.clear();
		 }

		 return nextIndex;
	 }    

	 public int getIndexOfPair(MatePair matePair) {
		 int index = -1;

		 for (int i=0; i<clusterMatePairs.size(); i++) {
			 if (matePair.getReadName().equals(clusterMatePairs.get(i).getReadName())) {
				 index = i;
			 }
		 }
		 return index;
	 }   

	 public List<MatePair> copyAndOrderCurrentClusterPairs() {
		 //test pairs to use for cluster detection
		 List<MatePair> testPairs = new ArrayList<MatePair>(getClusterMatePairs());

		 //sort by start of right reads
		 Collections.sort(testPairs, READ_MATE_RIGHT_START_COMP); 
		 return testPairs;
	 }

	 public int getLeftBreakPoint() {

		 String cat = qPrimerCateory.getPrimaryCategoryNo();

		 if (cat.equals("1") || cat.equals("3")) {
			 return leftEnd;
		 } else if (cat.equals("2") || cat.equals("4") || cat.equals("5")) {
			 return leftStart;  		
		 } 

		 return 0;
	 }

	 public int getRightBreakPoint() {

		 String cat = qPrimerCateory.getPrimaryCategoryNo();

		 if (cat.equals("1") || cat.equals("4")) {
			 return rightStart;
		 } else if (cat.equals("2") || cat.equals("3") || cat.equals("5")) {
			 return rightEnd;  		
		 } 

		 return 0;
	 }

	 public Map<String, AtomicInteger> getStrandOrientations() {
		 return strandOrientations;
	 }

	 public int getWindowSize() {
		 return this.windowSize;
	 }

	 public QPrimerCategory getqPrimerCateory() {
		 return qPrimerCateory;
	 }

	 public void setqPrimerCateory(QPrimerCategory qPrimerCateory) {
		 this.qPrimerCateory = qPrimerCateory;
	 }

	 public void setId(int count) {
		 this.id = count;
	 }

	 public int getId() {
		 return this.id;
	 }

	 public String toVerboseString(String compareType, boolean isQCMG) {
		 StringBuilder sb = new StringBuilder();
		 sb.append(">>" +getChrRegionFrom() + " | " + getChrRegionTo() + QSVUtil.NEW_LINE);

		 if (clusterMatePairs.size() > 0) {
			 sb.append(">>" + sampleType + "_DISCORDANT_READS" + QSVUtil.NEW_LINE);
			 Collections.sort(clusterMatePairs, READ_MATE_LEFT_START_COMP);
			 for (MatePair p : clusterMatePairs) {
				 sb.append(p.toVerboseString(isQCMG));
			 }
		 }

		 if (matchedNormalMatePairs.size() > 0) {
			 sb.append("#" + compareType + "_DISCORDANT_READS" + QSVUtil.NEW_LINE);	      
			 Collections.sort(matchedNormalMatePairs, READ_MATE_LEFT_START_COMP);
			 for (MatePair p : matchedNormalMatePairs) {
				 sb.append(p.toVerboseString(isQCMG));
			 }
		 }

		 return sb.toString();
	 }



	 public void finalize(QSVParameters findParameters, QSVParameters compareParameters, String type, int count, String query, String pairType, boolean isQCMG) throws Exception {
		 setType(type);
		 setId(count);
		 countStrandOrientations();

		 this.referenceKey = leftReferenceName + ":" + rightReferenceName;
		 if (getType().equals("somatic")) {

			 if (compareParameters != null) {
				 rescueGermlineReads(findParameters, compareParameters, query);
			 }         
		 }

		 findQPrimerCategory(pairType);

		 Collections.sort(getClusterMatePairs(), READ_MATE_LEFT_START_COMP);
	 }

	 private void rescueGermlineReads(QSVParameters findParameters, QSVParameters compareParameters, String query) throws Exception {
		 Map<String, SAMRecord[]> map = new HashMap<String, SAMRecord[]>();
		 int leftMiddle = ((leftEnd - leftStart)/2) + leftStart;
		 int rightMiddle = ((rightEnd - rightStart)/2) + rightStart;	
		 int tumourCompareLeftStart = leftMiddle - findParameters.getUpperInsertSize();
		 int tumourCompareLeftEnd = leftMiddle + findParameters.getUpperInsertSize();
		 int tumourCompareRightStart = rightMiddle - findParameters.getUpperInsertSize();
		 int tumourCompareRightEnd = rightMiddle + findParameters.getUpperInsertSize();

		 int start = Math.min(compareLeftStart, tumourCompareLeftStart);
		 int end = Math.max(compareLeftEnd, tumourCompareLeftEnd);

		 readAndAnnotateRecords(findParameters, compareParameters, map, leftReferenceName, start, end, compareParameters.getAnnotator(), compareParameters.getInputBamFile());

		 start = Math.min(compareRightStart, tumourCompareRightStart);
		 end = Math.max(compareRightEnd, tumourCompareRightEnd);
		 readAndAnnotateRecords(findParameters, compareParameters, map, rightReferenceName, start, end, compareParameters.getAnnotator(), compareParameters.getInputBamFile());

		 for (Entry<String, SAMRecord[]> entry: map.entrySet()) {
			 SAMRecord[] records = entry.getValue();

			 if (records[0] != null && records[1] != null) {
				 
				 String key = entry.getKey();

				 MatePair m = new MatePair(records[0], records[1]);

				 if (key.startsWith("normal")) {
					 if (QSVUtil.doesMatePairOverlapRegions(m, compareLeftStart, compareLeftEnd, 
							 compareRightStart, compareRightEnd)) {
						 lowConfidenceNormalMatePairs++;
					 }
				 } else if (key.startsWith("tumour")) {

					 if (QSVUtil.doesMatePairOverlapRegions(m, tumourCompareLeftStart, tumourCompareLeftEnd, 
							 tumourCompareRightStart, tumourCompareRightEnd)) {
						 lowConfidenceNormalMatePairs++;
					 }
				 }
			 }
		 }    
		 map = null;
	 }

	 public void setLowConfidenceNormalMatePairs(int lowConfidenceNormalMatePairs) {
		 this.lowConfidenceNormalMatePairs = lowConfidenceNormalMatePairs;
	 }

	 private void readAndAnnotateRecords(QSVParameters findParameters, QSVParameters compareParameters, Map<String, SAMRecord[]> map, String ref, int start,
			 int end, Annotator annotator, File bamFile) throws Exception {
		 
		 SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, ValidationStringency.SILENT);
		 SAMRecordIterator iter = reader.queryOverlapping(ref, start-200, end+200);

		 String zp1 = null;
		 String zp2 = null;
		 if (zp.contains("_")) {
			 zp1 = zp.split("_")[0];
			 zp2 = zp.split("_")[1];
		 } else {
			 zp1 = zp;
		 }
		 Set<String> readGroupIds = compareParameters.getReadGroupIdsAsSet();
//		 List<String> readGroupIds = compareParameters.getReadGroupIds();

		 while (iter.hasNext()) {
			 SAMRecord r = iter.next();

			 if (readGroupIds.contains(r.getReadGroup().getId())) {

				 annotator.annotate(r);

				 if (passesZPFilter((String) r.getAttribute("ZP"), zp1, zp2)) {
					 String key = "normal" + r.getReadName() + ":" + r.getReadGroup().getId();
					 
					 SAMRecord[] arr = map.get(key);
					 if (null == arr) {
						 map.put(key, new SAMRecord[]{r,null});
					 } else {
						 if ( ! r.equals(arr[0])) {
							 arr[1] = r;
						 }
					 }
				 } else {
					 annotator.annotateByTumorISize(findParameters.getLowerInsertSize(), findParameters.getUpperInsertSize(), r);

					 if (passesZPFilter((String) r.getAttribute("ZP"), zp1, zp2)) {

						 String key = "tumour" + r.getReadName() + ":" + r.getReadGroup().getId();
						 
						 
						 SAMRecord[] arr = map.get(key);
						 if (null == arr) {
							 map.put(key, new SAMRecord[]{r,null});
						 } else {
							 if ( ! r.equals(arr[0])) {
								 arr[1] = r;
							 }
						 }
					 }
				 }
			 }
		 }        
		 reader.close();		
	 }

	 private boolean passesZPFilter(String currentZP, String zp1, String zp2) {
		 return currentZP.equals(zp1)
				 || (null != zp2 && currentZP.equals(zp2));
	 }

	 private void findQPrimerCategory(String pairType) throws Exception {

		 this.qPrimerCateory = new QPrimerCategory(zp, leftReferenceName, rightReferenceName, pairType);
		 qPrimerCateory.findClusterCategory(clusterMatePairs, leftStart, leftEnd, rightStart, rightEnd);

	 }

	 public static class QSVRecordComparator implements Comparator<DiscordantPairCluster> {

		 @Override
		 public int compare(DiscordantPairCluster o1, DiscordantPairCluster o2) {
			 if (o1.getLeftReferenceName().equals(o2.getLeftReferenceName())) {
				 int i1 = o1.getLeftStart();
				 int i2 = o2.getLeftStart();
				 if (i1 == i2) {
					 int i3 = o1.getRightEnd();
					 int i4 = o2.getRightEnd();
					 return Integer.compare(i3, i4);
				 } else {
					 return Integer.compare(i1, i2);
				 }
			 } else {
				 return o1.getLeftReferenceName().compareTo(o2.getLeftReferenceName());
			 }
		 }
	 }

	 public void setNormalRange(int maxISize) {
		 int leftMiddle = ((leftEnd - leftStart)/2) + leftStart;
		 int rightMiddle = ((rightEnd - rightStart)/2) + rightStart;

		 //normal double
		 compareLeftStart = leftMiddle - maxISize;
		 compareLeftEnd = leftMiddle + maxISize;
		 compareRightStart = rightMiddle - maxISize;
		 compareRightEnd = rightMiddle + maxISize;

	 }

}
