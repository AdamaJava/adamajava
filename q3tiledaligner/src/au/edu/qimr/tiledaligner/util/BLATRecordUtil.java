package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPairs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.NumberUtils;

import htsjdk.samtools.util.SequenceUtil;

public class BLATRecordUtil {
	
	public static final Map<String, Integer> CHR_SIZE_MAP = new ConcurrentHashMap<>();
	
	public static Integer getChromosomeSize(String chr) {
		if (CHR_SIZE_MAP.isEmpty()) {
			for (String s : PositionChrPositionMap.grch37Positions) {
				/*
				 * split, strip and trim
				 */
				String [] sArray = s.split(":");
				String chromosome = sArray[0].replaceAll("##", "");
				int length = Integer.parseInt(sArray[1]);
				
				CHR_SIZE_MAP.put(chromosome, length);
			}
		}
		return CHR_SIZE_MAP.get(chr);
	}
	
	public static String[] getDetailsForBLATRecord(ChrPosition bufferredCP, String [] swDiffs, String name, String sequence, boolean forwardStrand, String bufferedReference) {
			
		String refFromSW = swDiffs[0].replaceAll("-", "");
		String seqFromSW = swDiffs[2].replaceAll("-", "");
		String sequenceToUse = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
		List<int[]> allStartPositions = NumberUtils.getAllStartPositions(swDiffs);
		int [] queryBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[2], '-');
		int [] targetBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[0], '-');
		
		
//		int seqOffset = sequence.indexOf(seqFromSW);
		int seqOffset = sequenceToUse.indexOf(seqFromSW);
		
		int nCount =  StringUtils.getCount(swDiffs[2], 'N');
		int misMatchCount =  StringUtils.getCount(swDiffs[1], '.');
		if (nCount > 0 && misMatchCount > 0) {
			misMatchCount -= nCount;
		}
		String [] array = new String[21];
		array[0] = "" + StringUtils.getCount(swDiffs[1], '|');		//number of matches
		array[1] = "" + misMatchCount;					//number of mis-matches
		array[2] = "0";									//number of rep. matches
		array[3] = "" + nCount;							//number of N's
		array[4] = "" + queryBlockCountAndCounts[0];	// Q gap count
		array[5] = "" + queryBlockCountAndCounts[1];		// Q gap bases
		array[6] = "" + targetBlockCountAndCounts[0];			// T gap count
		array[7] = "" + targetBlockCountAndCounts[1];		// T gap bases
		array[8] = forwardStrand ? "+" : "-";			// strand
		array[9] = name;								// Q name
		array[10] = sequence.length() + "";				// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first bloack, and end of the last
		 * if we are on reverse, need to reverse!
		 */
		int start =  forwardStrand ?  seqOffset + allStartPositions.get(0)[0] : (sequence.length() - (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]));
		int end =  forwardStrand ?  (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]) : (sequence.length() - (seqOffset + allStartPositions.get(0)[0]));
//		int start = forwardStrand ?  blocks.get(0).getMinimum().intValue() : (sequence.length() - blocks.get(blocks.size() - 1).getMaximum().intValue());
//		int end = forwardStrand ?  blocks.get(blocks.size() - 1).getMaximum().intValue() : (sequence.length() - blocks.get(0).getMinimum().intValue());
		
		array[11] = "" + start;							// Q start
		array[12] = "" + end;	// Q end
		array[13] = bufferredCP.getChromosome();			// T name
		Integer contigLength =  getChromosomeSize(bufferredCP.getChromosome());
		array[14] = contigLength == null ? "12345" : contigLength.toString();								// T size
		int indexOfRefInBufferedRef = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW);
		int tStart = indexOfRefInBufferedRef + bufferredCP.getStartPosition();
		
		array[15] = "" + tStart;								// T start
		array[16] = "" + (refFromSW.length() + tStart);			// T end
		
		array[17] = "" + allStartPositions.size();					// block count
		array[18] = allStartPositions.stream().map(b -> "" + (b[1])).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = allStartPositions.stream().map(b -> "" + (b[0] + seqOffset)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes					// Q block starts
		array[20] = allStartPositions.stream().map(b -> "" + (b[2] + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes			// T block starts
		
		return array;
	}
	
	public static BLATRecord getRecordFromStartPositionsAndLengths(ChrPosition bufferredCP, int[][] startPositionsAndLengths, String name, String sequence, boolean forwardStrand) {
		
		String sequenceToUse = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
		int referenceOffset = bufferredCP.getStartPosition();
		
//		int seqOffset = sequence.indexOf(seqFromSW);
		
		int nCount =  StringUtils.getCount(sequenceToUse, 'N');
		int matchCount = 0;
		int misMatchCount = 0;
		int qGap = 0;
		int seqLastStop = 0;
		int qGapCount = 0;
		int tGap = 0;
		int refLastStop = 0;
		int tGapCount = 0;
		for (int [] array : startPositionsAndLengths) {
			matchCount += array[2];
			if (seqLastStop > 0) {
				if (array[1] > seqLastStop + 1) {
					qGap += array[1] - (seqLastStop + 1);
					qGapCount++;
				}
			}
			seqLastStop = array[1] + array[2];
			if (refLastStop > 0) {
				if (array[0] > refLastStop + 1) {
					tGap += array[0] - (refLastStop + 1);
					tGapCount++;
				}
			}
			refLastStop = array[0] + array[2];
		}
		if (nCount > 0 && misMatchCount > 0) {
			misMatchCount -= nCount;
		}
		String [] array = new String[21];
		array[0] = "" + matchCount;		//number of matches
		array[1] = "" + misMatchCount;					//number of mis-matches
		array[2] = "0";									//number of rep. matches
		array[3] = "" + nCount;							//number of N's
		array[4] = "" + qGapCount;	// Q gap count
		array[5] = "" + qGap;		// Q gap bases
		array[6] = "" + tGapCount;			// T gap count
		array[7] = "" + tGap;		// T gap bases
		array[8] = forwardStrand ? "+" : "-";			// strand
		array[9] = name;								// Q name
		array[10] = sequence.length() + "";				// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first bloack, and end of the last
		 * if we are on reverse, need to reverse!
		 */
//		int start =  forwardStrand ?  seqOffset + allStartPositions.get(0)[0] : (sequence.length() - (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]));
//		int end =  forwardStrand ?  (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]) : (sequence.length() - (seqOffset + allStartPositions.get(0)[0]));
//		int start = forwardStrand ?  blocks.get(0).getMinimum().intValue() : (sequence.length() - blocks.get(blocks.size() - 1).getMaximum().intValue());
//		int end = forwardStrand ?  blocks.get(blocks.size() - 1).getMaximum().intValue() : (sequence.length() - blocks.get(0).getMinimum().intValue());
		
		array[11] = "" + startPositionsAndLengths[0][1];							// Q start
		array[12] = "" + (startPositionsAndLengths[startPositionsAndLengths.length - 1][1] + startPositionsAndLengths[startPositionsAndLengths.length - 1][2]);	// Q end
		array[13] = bufferredCP.getChromosome();			// T name
		Integer contigLength =  getChromosomeSize(bufferredCP.getChromosome());
		array[14] = contigLength == null ? "12345" : contigLength.toString();								// T size
//		int indexOfRefInBufferedRef = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW);
//		int tStart = indexOfRefInBufferedRef + bufferredCP.getStartPosition();
		
		array[15] = "" + (referenceOffset + startPositionsAndLengths[0][0]);								// T start
		array[16] = "" + (referenceOffset + startPositionsAndLengths[startPositionsAndLengths.length - 1][0] + startPositionsAndLengths[startPositionsAndLengths.length - 1][2]);			// T end
		
		array[17] = "" + startPositionsAndLengths.length;					// block count
		array[18] = Arrays.stream(startPositionsAndLengths).map(b -> "" + b[2]).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = Arrays.stream(startPositionsAndLengths).map(b -> "" + b[1]).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes					// Q block starts
		array[20] = Arrays.stream(startPositionsAndLengths).map(b -> "" + (referenceOffset + b[0])).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes			// T block starts
		
		return new BLATRecord(array);
	}

	public static String[] getDetailsForBLATRecordPerfectMatch(ChrPosition cp, String name, String sequence, boolean forwardStrand) {
		
		int sequenceLength = sequence.length();
		String [] array = new String[21];
		array[0] = "" + sequenceLength; 		//number of matches
		array[1] = "0" ;		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";		// Q gap count
		array[5] = "0";		// Q gap bases
		array[6] = "0";		// T gap count
		array[7] = "0";		// T gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequenceLength + "";			// Q size
		array[11] = "0";							// Q start
		array[12] = "" + (sequenceLength - 1);		// Q end
		array[13] = cp.getChromosome();				// T name
		Integer contigLength =  getChromosomeSize(cp.getChromosome());
		array[14] = contigLength == null ? "12345" : contigLength.toString();								// T size
		int tStart = cp.getStartPosition();
		
		array[15] = "" + tStart;					// T start
		array[16] = "" + (sequenceLength + tStart);	// T end
		
		array[17] = "1";							// block count
		array[18] = "" + sequenceLength;			// block sizes
		array[19] = "0";							// Q block starts
		array[20] = "" + cp.getStartPosition();		// T block starts
		return array;
	}

	/**
	 * @deprecated Please only use this method with a single ChrPosition in the list
	 * results are unintuitive if more than 1 is used....
	 * 
	 * @param positions
	 * @param name
	 * @param sequence
	 * @param forwardStrand
	 * @return
	 */
	public static String[] getDetailsForBLATRecord(List<ChrPosition> positions, String name,  String sequence, boolean forwardStrand) {
//		System.out.println("getDetailsForBLATRecord with positions: " + positions.stream().map(ChrPosition::toString).collect(Collectors.joining(",")) + ", name: " + name + ", sequence: " + sequence + ", fs: " + forwardStrand);
		
		int totalGapSize = 0;
		int totalMatches = 0;
		int lastEnd = 0;
		int numberOfPositions = positions.size();
		List<ChrPosition> positionsToUse = positions;
		
		if (numberOfPositions > 1) {
			/*
			 * want the ChrPositions sorted
			 */
			positions.sort(null);
			
			positionsToUse = TiledAlignerUtil.checkForOverlappingSequence(positions, sequence);
			numberOfPositions = positionsToUse.size();
		}
		
		if (numberOfPositions == 0) {
			return new String[]{};
		}
		
		for (ChrPosition cp : positionsToUse) {
			if (lastEnd > 0) {
				totalGapSize += cp.getStartPosition() - lastEnd;
			}
			totalMatches += cp.getLength() - 1;
			lastEnd = cp.getEndPosition();
		}
		
		String [] array = new String[21];
		array[0] = "" + totalMatches;		//number of matches
		array[1] = "0";		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";	// T gap count
		array[5] = "0";		// T gap bases
		array[6] = "" + (numberOfPositions - 1);		// Q gap count
		array[7] = "" + totalGapSize;				// Q gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequence.length() + "";			// Q size
		array[11] = "" + sequence.indexOf(positionsToUse.get(0).getName());					// Q start
		array[12] = "" + (sequence.indexOf(positionsToUse.get(numberOfPositions - 1).getName()) + positionsToUse.get(numberOfPositions - 1).getLength() - 1);	// Q end
		array[13] = positionsToUse.get(0).getChromosome();			// T name
		Integer contigLength =  getChromosomeSize(positionsToUse.get(0).getChromosome());
		array[14] = contigLength == null ? "12345" : contigLength.toString();								// T size
		int tStart = positionsToUse.get(0).getStartPosition();
		
		array[15] = "" + tStart;								// T start
		array[16] = "" + (positions.get(numberOfPositions - 1).getEndPosition());		// T end
		
		array[17] = "" + numberOfPositions;					// block count
		array[18] = positionsToUse.stream().map(b -> "" + (b.getLength() - 1)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = positionsToUse.stream().map(b -> "" + sequence.indexOf(b.getName())).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
		array[20] = positionsToUse.stream().map(b -> "" + b.getStartPosition()).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
		
		return array;
	}
	
	public static String[] getDetailsForBLATRecord(ChrPosition position, int misMatchCount, int queryStartPosition, String name,  String sequence, boolean forwardStrand) {
//		System.out.println("getDetailsForBLATRecord with positions: " + positions.stream().map(ChrPosition::toString).collect(Collectors.joining(",")) + ", name: " + name + ", sequence: " + sequence + ", fs: " + forwardStrand);
		
		int totalMatches = position.getLength() - 1;
		int lastEnd = position.getEndPosition();
		
		String [] array = new String[21];
		array[0] = "" + totalMatches;		//number of matches
		array[1] = misMatchCount + "";		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";		// T gap count
		array[5] = "0";		// T gap bases
		array[6] = "0";		// Q gap count
		array[7] = "0";		// Q gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequence.length() + "";			// Q size
		array[11] = "" + sequence.indexOf(position.getName());					// Q start
		array[12] = "" + (sequence.indexOf(position.getName()) + totalMatches);	// Q end
		array[13] = position.getChromosome();									// T name
		Integer contigLength =  getChromosomeSize(position.getChromosome());
		array[14] = contigLength == null ? "12345" : contigLength.toString();	// T size
		int tStart = position.getStartPosition();
		
		array[15] = "" + tStart;				// T start
		array[16] = "" + lastEnd;				// T end
		
		array[17] = "1";						// block count
		array[18] = "" + totalMatches;			// block sizes
		array[19] = "" + queryStartPosition;	// Q block starts
		array[20] = "" + tStart;				// T block starts
		
		return array;
	}
	
	
	public static Optional<BLATRecord> findRecordInRange(List<BLATRecord> recs, int queryStart, int queryStop) {
		BLATRecord bestMatch = null;
		if (null != recs && ! recs.isEmpty()) {
			long bestScore = 0;
			/*
			 * start with best scoring records
			 */
			for (int i = recs.size() - 1 ; i >= 0 ; i--) {
				BLATRecord br = recs.get(i);
				long overlap = NumberUtils.getOverlap(queryStart, queryStop, br.getQueryStart(), br.getQueryEnd());
				long thisBRScore = (overlap - br.getMisMatches()) - br.getQueryGapCount();
				if (thisBRScore > bestScore) {
					bestScore = thisBRScore;
					bestMatch = br;
				}
			}
		}
		return Optional.ofNullable(bestMatch);
	}
	
	
	/**
	 * will attempt to merge all the BLAT records in the supplies list into a single BLAT record.
	 * This means that the entries in the list must be on the same contig, strand and within 500000 bases of each other
	 * 
	 * Returns empty Optional if this is not possible (or if list is empty)
	 * 
	 * @param recordsToMerge
	 * @return
	 */
	public static Optional<BLATRecord> mergeBLATRecs(List<BLATRecord> records, int nCount) {
		if (null == records || records.isEmpty()) {
			return Optional.empty();
		}
		
		/*
		 * check that the entries will make a valid single BLAT record
		 */
		for (int i = 0 ; i < records.size() ; i++) {
			BLATRecord rec = records.get(i);
			for (int j = i + 1 ; j < records.size() ; j++) {
				BLATRecord nextRec = records.get(j);
				
				if ( ! rec.getReference().equals(nextRec.getReference())
						|| rec.getStrand() != nextRec.getStrand()
						|| Math.abs(rec.getStartPos() - nextRec.getStartPos()) > 500000) {
					return Optional.empty();
				}
			}
		}
		
		/*
		 * sort the records by position in chromosome
		 */
		records.sort((r1, r2) -> Integer.compare(r1.getStartPos(), r2.getStartPos()));
		
		BLATRecord thisRec = records.get(0);
		int mismatchCount = thisRec.getMisMatches();
		int tGapCount = 0;
		int qGapCount = 0;
		int [] blockLengths = new int[records.size()];
		int [] queryStarts = new int[records.size()];
		int [] templateStarts = new int[records.size()];
		blockLengths[0] = thisRec.getQueryLength();
		queryStarts[0] = thisRec.getQueryStart() - 1;
		templateStarts[0] = thisRec.getStartPos() - 1;
		for (int i = 1 ; i < records.size() ; i++) {
			BLATRecord nextRec = records.get(i);
			
			/*
			 * check to see if there is an overlap on the sequence or genomic position
			 */
			int genomicDiff = thisRec.getEndPos() - nextRec.getStartPos();
			if (genomicDiff > 0) {
				/*
				 * reduce size of larger record
				 */
				
				if (thisRec.getQueryLength() > nextRec.getQueryLength()) {
					blockLengths[i - 1] -= genomicDiff;
					blockLengths[i] = nextRec.getQueryLength();
				} else {
					blockLengths[i] = nextRec.getQueryLength() - genomicDiff;
				}
			}
			tGapCount += Math.abs(genomicDiff);
			
			/*
			 * and now for sequence overlaps
			 */
			int seqDiff = thisRec.getQueryEnd() - (nextRec.getQueryStart() - 1);
			if (seqDiff > 0) {
				/*
				 * reduce size of larger record
				 */
				if (thisRec.getQueryLength() > nextRec.getQueryLength()) {
					blockLengths[i - 1] -= seqDiff;
					blockLengths[i] = nextRec.getQueryLength();
				} else {
					blockLengths[i] = nextRec.getQueryLength() - seqDiff;
					queryStarts[i] = (nextRec.getQueryStart() - 1)  + seqDiff;
					templateStarts[i] = (nextRec.getStartPos() - 1) + seqDiff;
				}
			} else {
				qGapCount += Math.abs(seqDiff);
			}
			
			/*
			 * if block count hasn't been set - nows the time
			 */
			if (blockLengths[i] == 0) {
				blockLengths[i] = nextRec.getQueryLength();
			}
			/*
			 * if query start hasn't been set - nows the time
			 */
			if (queryStarts[i] == 0) {
				queryStarts[i] = nextRec.getQueryStart() - 1;
			}
			/*
			 * if template start hasn't been set - nows the time
			 */
			if (templateStarts[i] == 0) {
				templateStarts[i] = nextRec.getStartPos() - 1;
			}
			
			/*
			 * update some stats
			 */
			mismatchCount += nextRec.getMisMatches();
		}
		
		String[] array = new String[21];
		array[0] = "" + Arrays.stream(blockLengths).sum();	//number of matches
		array[1] = "" + mismatchCount;	//number of mis-matches
		array[2] = "0";					//number of rep. matches
		array[3] = "" + nCount;			//number of N's
		array[4] = qGapCount > 0 ? "1" : "0";		// Q gap count
		array[5] = "" + qGapCount;					// Q gap bases
		array[6] = tGapCount > 0 ? "1" : "0" ;		// T gap count
//		array[6] = "" + (keys.length - 1);			// T gap count
		array[7] = "" + tGapCount;					// T gap bases
		array[8] = "" + thisRec.getStrand();		// strand
		array[9] = thisRec.getName();				// Q name
		array[10] = thisRec.getSize() + "";			// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first block, and end of the last
		 * if we are on reverse, need to reverse!
		 */
//		int start = reverseStrand ? (seqLength - positionInSequence - length) :  positionInSequence;
//		int end = reverseStrand ?  (seqLength - positionInSequence) : positionInSequence + length;
		
		array[11] = "" + queryStarts[0];						// Q start
		// get last bloack
//		int[] lastBlock = chrPosBlocks.get(keys[keys.length - 1]);
		array[12] = "" + (queryStarts[queryStarts.length - 1] + blockLengths[blockLengths.length - 1]);	// Q end
		array[13] = thisRec.getReference();				// T name
		array[14] = "" + thisRec.getChromsomeLength();	// T size
//		int tStart = keys[0].getStartPosition();
		
		array[15] = "" + templateStarts[0];						// T start
		array[16] = "" + (templateStarts[templateStarts.length - 1] + blockLengths[blockLengths.length - 1]);		// T end
		array[17] = "" + blockLengths.length;								// block count
		array[18] = Arrays.stream(blockLengths).mapToObj(i -> "" + i).collect(Collectors.joining(","));					// block sizes
		array[19] = Arrays.stream(queryStarts).mapToObj(i -> "" + i).collect(Collectors.joining(","));				// Q block starts, strand dependent
		array[20] = Arrays.stream(templateStarts).mapToObj(i -> "" + i).collect(Collectors.joining(","));	// T block starts
		
		return Optional.of(new BLATRecord(array));
	}
	
	
	/**
	 * Examine the supplied list of BLATRecords and if there are any that can be merged into a single BLATRecord, then do so, removing the constituent records from the returned list
	 * 
	 * Only merge records that contain a single block (for now...)
	 * 
	 * @param blatRecords
	 * @return
	 */
//	public static List<BLATRecord> mergeBLATRecords(List<BLATRecord> blatRecords) {
//		if (null == blatRecords || blatRecords.size() <= 1) {
//			return blatRecords;
//		}
//		int size = blatRecords.size();
//		List<BLATRecord> mergedBlatRecords = new ArrayList<>(size);
//		blatRecords.sort(null);
//		
//		/*
//		 * largest first
//		 */
//		BLATRecord br = blatRecords.get(size - 1);
//		if (br.getBlockCount() == 1) {
//			/*
//			 * we have a candidate
//			 */
//		}
//		
//		
//		
//		return mergedBlatRecords;
//	}
	
	/**
	 * needs to be:
	 * - same chromosome
	 * - same strand
	 * - within 500k bp
	 * - not overlapping (within buffer)
	 * - block size of 1
	 * 
	 *  
	 * @param record
	 * @param potentialMatches
	 * @return
	 */
//	public static BLATRecord getPotentialMatch(BLATRecord record, List<BLATRecord> potentialMatches) {
//		List<int[]> potentialRanges = TARecordUtil.getPossibleTileRanges(record.getSize(), record.getQueryStart(), record.getQueryEnd() - record.getQueryEnd(), 20, record.getStrand() == '-');
//		if (potentialRanges.size() > 0) {
//			for (BLATRecord br : potentialMatches) {
//				if (br.getBlockCount() == 1
//						&& br.getReference().equals(record.getReference())
//						&& br.getStrand() == record.getStrand()
//						&& Math.abs(br.getStartPos() - record.getStartPos()) < 500000) {
//					
//					
//					
//				}
//			}
//		}
//		return null;
//	}
	
	
	/**
	 * Returns an int array (optional).
	 * First element is the coverage of the sequence that the supplied list of records provides.
	 * Second element is the overlap of the records that are supplied.
	 * @param records
	 * @return
	 */
	public static Optional<int []> getCombinedNonOverlappingScore(List<BLATRecord> records) {
		if (null != records && ! records.isEmpty()) {
			
			/*
			 * first step is to sort the list by sequence start position
			 */
			records.sort((r1, r2) -> Integer.compare(r1.getQueryStart(), r2.getQueryStart()));
			
			int overlap = 0;
			int coverage = 0;
			int lastRecordEnd = 0;
			
			for (BLATRecord br : records) {
				int thisOverlap = br.getQueryStart() < lastRecordEnd ? lastRecordEnd - br.getQueryStart() + 1: 0;
				int thisCoverage = ((br.getQueryEnd() - br.getQueryStart() + 1) - thisOverlap);
				
				coverage += thisCoverage;
				overlap += thisOverlap;
				
				lastRecordEnd = br.getQueryEnd();
			}
			return Optional.of(new int[] {coverage, overlap});
		}
		return Optional.empty();
	}
	
	/**
	 * 
	 * 
	 * @param originalList
	 * @return
	 */
	public static List<BLATRecord> removeOverlappingRecords(List<BLATRecord> originalList) {
		if (originalList.size() < 2) {
			return originalList;
		}
		
		List<BLATRecord> nonOverlappingList = new ArrayList<>(originalList.size());
		
		/*
		 * sort the original list 
		 */
		originalList.sort(null);
		/*
		 * add the entry with the highest score to the new list
		 */
		int size = originalList.size();
		nonOverlappingList.add(originalList.get(size - 1));
		
		for (int i = size - 2 ; i >= 0 ; i--) {
			if ( ! doesRecordOverlapEntriesInList(nonOverlappingList, originalList.get(i))) {
				nonOverlappingList.add(originalList.get(i));
			}
		}
		return nonOverlappingList;
	}
	
	public static boolean doesRecordOverlapEntriesInList(List<BLATRecord> records, BLATRecord record) {
		
		/*
		 * nothing for it but to iterate over the list, examining each pair
		 */
		for (BLATRecord r : records) {
			if (doRecordsOverlapReference(r, record)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean doRecordsOverlapReference(BLATRecord r1, BLATRecord r2) {
		
		if (r1.getReference().equals(r2.getReference())) {
			int r1Start = r1.getStartPos();
			int r1End = r1.getEndPos();
			int r2Start = r2.getStartPos();
			int r2End = r2.getEndPos();
			
			if ((r2Start >= r1Start && r2Start < r1End) || (r2End > r1Start && r2End <= r1End)) {
				return true;
			}
		}
		return false;
	}
}
