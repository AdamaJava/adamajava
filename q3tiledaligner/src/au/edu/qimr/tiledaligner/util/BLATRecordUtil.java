/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPair;
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
	
	public static BLATRecord getBLATRecord(ChrPosition bufferredCP, String [] swDiffs, String name, String sequence, boolean forwardStrand, String bufferedReference) {
			
		String refFromSW = swDiffs[0].replaceAll("-", "");
		String seqFromSW = swDiffs[2].replaceAll("-", "");
		String sequenceToUse = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
		List<int[]> allStartPositions = NumberUtils.getAllStartPositions(swDiffs);
		int [] queryBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[2], '-');
		int [] targetBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[0], '-');
		
		int seqOffset = sequenceToUse.indexOf(seqFromSW);
		
		int nCount =  StringUtils.getCount(swDiffs[2], 'N');
		int misMatchCount =  StringUtils.getCount(swDiffs[1], '.');
		if (nCount > 0 && misMatchCount > 0) {
			misMatchCount -= nCount;
		}
		int qStart =  forwardStrand ?  seqOffset + allStartPositions.get(0)[0] : (sequence.length() - (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]));
		int qEnd =  forwardStrand ?  (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]) : (sequence.length() - (seqOffset + allStartPositions.get(0)[0]));
		Integer contigLength =  getChromosomeSize(bufferredCP.getChromosome());
		int indexOfRefInBufferedRef = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW);
		int tStart = indexOfRefInBufferedRef + bufferredCP.getStartPosition();
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(StringUtils.getCount(swDiffs[1], '|'))
				.withMisMatch(misMatchCount)
				.withRepMatch(0)
				.withNCount(nCount)
				.withQNumInsert(queryBlockCountAndCounts[0])
				.withQBaseInsert(queryBlockCountAndCounts[1])
				.withTNumInsert(targetBlockCountAndCounts[0])
				.withTBaseInsert(targetBlockCountAndCounts[1])
				.withStrand(forwardStrand ? '+' : '-')
				.withQName(name)
				.withSize(sequence.length())
				.withQStart(qStart)
				.withQEnd(qEnd)
				.withTName(bufferredCP.getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart(tStart)
				.withTEnd(refFromSW.length() + tStart)
				.withBlockCount(allStartPositions.size())
				.withBlockSizes(allStartPositions.stream().map(b -> "" + b[1]).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withQStarts(allStartPositions.stream().map(b -> "" + (b[0] + seqOffset)).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withTStarts(allStartPositions.stream().map(b -> "" + (b[2] + tStart)).collect(Collectors.joining(Constants.COMMA_STRING)))
				.build();
		return br;
	}
	
	public static BLATRecord getRecordFromStartPositionsAndLengths(ChrPosition bufferredCP, int[][] startPositionsAndLengths, String name, String sequence, boolean forwardStrand) {
		
		String sequenceToUse = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
		int referenceOffset = bufferredCP.getStartPosition();
		
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
		Integer contigLength =  getChromosomeSize(bufferredCP.getChromosome());
		
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(matchCount)
				.withMisMatch(misMatchCount)
				.withRepMatch(0)
				.withNCount(nCount)
				.withQNumInsert(qGapCount)
				.withQBaseInsert(qGap)
				.withTNumInsert(tGapCount)
				.withTBaseInsert(tGap)
				.withStrand(forwardStrand ? '+' : '-')
				.withQName(name)
				.withSize(sequence.length())
				.withQStart(startPositionsAndLengths[0][1])
				.withQEnd((startPositionsAndLengths[startPositionsAndLengths.length - 1][1] + startPositionsAndLengths[startPositionsAndLengths.length - 1][2]))
				.withTName(bufferredCP.getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart(referenceOffset + startPositionsAndLengths[0][0])
				.withTEnd(referenceOffset + startPositionsAndLengths[startPositionsAndLengths.length - 1][0] + startPositionsAndLengths[startPositionsAndLengths.length - 1][2])
				.withBlockCount(startPositionsAndLengths.length)
				.withBlockSizes(Arrays.stream(startPositionsAndLengths).map(b -> "" + b[2]).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withQStarts(Arrays.stream(startPositionsAndLengths).map(b -> "" + b[1]).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withTStarts(Arrays.stream(startPositionsAndLengths).map(b -> "" + (referenceOffset + b[0])).collect(Collectors.joining(Constants.COMMA_STRING)))
				.build();
		return br;
	}

	public static BLATRecord getDetailsForBLATRecordPerfectMatch(ChrPosition cp, String name, String sequence, boolean forwardStrand) {
		
		int sequenceLength = sequence.length();
		Integer contigLength =  getChromosomeSize(cp.getChromosome());
		int tStart = cp.getStartPosition();
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(sequenceLength)
				.withMisMatch(0)
				.withRepMatch(0)
				.withNCount(0)
				.withQNumInsert(0)
				.withQBaseInsert(0)
				.withTNumInsert(0)
				.withTBaseInsert(0)
				.withStrand(forwardStrand ? '+' : '-')
				.withQName(name)
				.withSize(sequenceLength)
				.withQStart(0)
				.withQEnd(sequenceLength - 1)
				.withTName(cp.getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart( tStart)
				.withTEnd(sequenceLength + tStart)
				.withBlockCount(1)
				.withBlockSizes("" + sequenceLength)
				.withQStarts("0")
				.withTStarts("" + tStart)
				.build();
		return br;
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
	public static Optional<BLATRecord> getDetailsForBLATRecord(List<ChrPosition> positions, String name,  String sequence, boolean forwardStrand) {
		
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
			return Optional.empty();
		}
		
		for (ChrPosition cp : positionsToUse) {
			if (lastEnd > 0) {
				totalGapSize += cp.getStartPosition() - lastEnd;
			}
			totalMatches += cp.getLength() - 1;
			lastEnd = cp.getEndPosition();
		}
		Integer contigLength =  getChromosomeSize(positionsToUse.get(0).getChromosome());
		int tStart = positionsToUse.get(0).getStartPosition();
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(totalMatches)
				.withMisMatch(0)
				.withRepMatch(0)
				.withNCount(0)
				.withQNumInsert(0)
				.withQBaseInsert(0)
				.withTNumInsert(numberOfPositions - 1)
				.withTBaseInsert(totalGapSize)
				.withStrand(forwardStrand ? '+' : '-')
				.withQName(name)
				.withSize(sequence.length())
				.withQStart(sequence.indexOf(positionsToUse.get(0).getName()))
				.withQEnd(sequence.indexOf(positionsToUse.get(numberOfPositions - 1).getName()) + positionsToUse.get(numberOfPositions - 1).getLength() - 1)
				.withTName(positionsToUse.get(0).getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart(tStart)
				.withTEnd(positions.get(numberOfPositions - 1).getEndPosition())
				.withBlockCount(numberOfPositions)
				.withBlockSizes(positionsToUse.stream().map(b -> "" + (b.getLength() - 1)).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withQStarts(positionsToUse.stream().map(b -> "" + sequence.indexOf(b.getName())).collect(Collectors.joining(Constants.COMMA_STRING)))
				.withTStarts(positionsToUse.stream().map(b -> "" + b.getStartPosition()).collect(Collectors.joining(Constants.COMMA_STRING)))
				.build();
		return Optional.of(br);
	}
	
	public static BLATRecord getDetailsForBLATRecord(ChrPosition position, int misMatchCount, int queryStartPosition, String name,  String sequence, boolean forwardStrand) {
		
		int totalMatches = position.getLength() - 1;
		int lastEnd = position.getEndPosition();
		int qStart = sequence.indexOf(position.getName());
		Integer contigLength =  getChromosomeSize(position.getChromosome());
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(totalMatches)
				.withMisMatch(misMatchCount)
				.withRepMatch(0)
				.withNCount(0)
				.withQNumInsert(0)
				.withQBaseInsert(0)
				.withTNumInsert(0)
				.withTBaseInsert(0)
				.withStrand(forwardStrand ? '+' : '-')
				.withQName(name)
				.withSize(sequence.length())
				.withQStart(qStart)
				.withQEnd(qStart + totalMatches)
				.withTName(position.getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart( position.getStartPosition())
				.withTEnd(lastEnd)
				.withBlockCount(1)
				.withBlockSizes("" + totalMatches)
				.withQStarts("" + queryStartPosition)
				.withTStarts("" + position.getStartPosition())
				.build();
		return br;
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
	
	public static int[] getBuffers(int seqLength, int startPos, int endPos, boolean split, int [] commonTilePositions, int standardBuffer, int splitBuffer) {
		int lhsBuffer = startPos == 0 ? 0 : split ? startPos + splitBuffer : standardBuffer + startPos;
		int rhsBuffer = endPos == seqLength ? 0 : split ? splitBuffer + (seqLength - endPos) : standardBuffer + (seqLength - endPos);
		return new int[] {lhsBuffer, rhsBuffer};
	}
	
	public static int[] getBuffersUsingOverlaps(int seqLength, int startPos, int endPos, boolean split, int [] commonTilePositions, int standardBuffer, int splitBuffer) {
		int lhsBuffer = startPos == 0 ? 0 : split ? startPos + splitBuffer : standardBuffer + startPos;
		int rhsBuffer = endPos == seqLength ? 0 : split ? splitBuffer + (seqLength - endPos) : standardBuffer + (seqLength - endPos);
		
		if (split) {
			if (startPos > 0) {
				/*
				 * examine commonTilePositions
				 */
				int commonTileCountAtStart = NumberUtils.getContinuousCountFromValue(startPos - 1, commonTilePositions, false);
				
				if (commonTileCountAtStart > 0) {
					/*
					 * get count of common tiles at start of this sequence segment
					 */
					lhsBuffer += commonTileCountAtStart; 
				}
			}
			if (endPos < seqLength) {
				/*
				 * examine commonTilePositions
				 */
				int commonTileCountAtEnd = NumberUtils.getContinuousCountFromValue(endPos - 1, commonTilePositions, true);
				if (commonTileCountAtEnd > 0) {
					/*
					 * get count of common tiles at start of this sequence segment
					 */
					rhsBuffer += commonTileCountAtEnd; 
				}
			}
		}
		return new int[] {lhsBuffer, rhsBuffer};
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
				
				if ( ! rec.getTName().equals(nextRec.getTName())
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
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(Arrays.stream(blockLengths).sum())
				.withMisMatch(mismatchCount)
				.withRepMatch(0)
				.withNCount(nCount)
				.withQNumInsert(qGapCount > 0 ? 1 : 0)
				.withQBaseInsert(qGapCount)
				.withTNumInsert(tGapCount > 0 ? 1 : 0)
				.withTBaseInsert(tGapCount)
				.withStrand(thisRec.getStrand())
				.withQName(thisRec.getQName())
				.withSize(thisRec.getSize())
				.withQStart(queryStarts[0])
				.withQEnd(queryStarts[queryStarts.length - 1] + blockLengths[blockLengths.length - 1])
				.withTName(thisRec.getTName())
				.withTSize(thisRec.getChromsomeLength())
				.withTStart(templateStarts[0])
				.withTEnd((templateStarts[templateStarts.length - 1] + blockLengths[blockLengths.length - 1]))
				.withBlockCount(blockLengths.length)
				.withBlockSizes(Arrays.stream(blockLengths).mapToObj(i -> "" + i).collect(Collectors.joining(",")))
				.withQStarts(Arrays.stream(queryStarts).mapToObj(i -> "" + i).collect(Collectors.joining(",")))
				.withTStarts(Arrays.stream(templateStarts).mapToObj(i -> "" + i).collect(Collectors.joining(",")))
				.build();
		return Optional.of(br);
	}
	
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
	 * Creates a new list, and populates it with BLATReords from the supplied list that do not overlap each other.
	 * This is done by sorting the original list, and inserting into the nes list the largest entry from the original list.
	 * 
	 * @param originalList
	 * @return
	 */
	public static List<BLATRecord> removeOverlappingRecords(List<BLATRecord> originalList) {
		if (null == originalList) {
			throw new IllegalArgumentException("Null list supplied as argument to removeOverlappingRecords!");
		}
		
		int size = originalList.size();
		if (size < 2) {
			return originalList;
		}
		
		List<BLATRecord> nonOverlappingList = new ArrayList<>(size);
		
		/*
		 * sort the original list 
		 */
		originalList.sort(null);
		/*
		 * add the entry with the highest score to the new list
		 */
		nonOverlappingList.add(originalList.get(size - 1));
		
		for (int i = size - 2 ; i >= 0 ; i--) {
			if ( ! doesRecordOverlapEntriesInList(nonOverlappingList, originalList.get(i))) {
				nonOverlappingList.add(originalList.get(i));
			}
		}
		return nonOverlappingList;
	}
	
	/**
	 *  Checks the supplied list of BLATRecords to see if any of them overlap the supplied BLATRecord.
	 *  Loops through the list and uses {@code doRecordsOverlapReference} to make this determination.
	 *  Will return true as soon as a record is found in the list that overlaps the main record.
	 *   
	 * @see doRecordsOverlapReference
	 * @param records
	 * @param record
	 * @return
	 */
	public static boolean doesRecordOverlapEntriesInList(List<BLATRecord> records, BLATRecord record) {
		
		/*
		 * nothing for it but to iterate over the list, examining each pair
		 */
		if (null != records) {
			for (BLATRecord r : records) {
				if (doRecordsOverlapReference(r, record)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Looks at the 2 supplied BLATRecord objects. If they are both not null, and on the same chromosome, 
	 * them their start and end positions are examined to see if there is any overlap.
	 * Returns true if there is, false otherwise.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static boolean doRecordsOverlapReference(BLATRecord r1, BLATRecord r2) {
		if (null != r1 && null != r2 && r1.getTName().equals(r2.getTName())) {
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

	public static BLATRecord blatRecordFromSplit(IntLongPair split, String name, int seqLength, PositionChrPositionMap headerMap, int tileLength) {
		ChrPosition cp = headerMap.getChrPositionFromLongPosition(split.getLong());
		boolean reverseStrand = NumberUtils.isBitSet(split.getLong(), TARecordUtil.REVERSE_COMPLEMENT_BIT);
		int length = NumberUtils.getPartOfPackedInt(split.getInt(), true) + tileLength - 1;
		int mismatch = NumberUtils.getPartOfPackedInt(split.getInt(), false);
		int positionInSequence = NumberUtils.getShortFromLong(split.getLong(), TARecordUtil.TILE_OFFSET);
		int qStart = reverseStrand ? (seqLength - positionInSequence - length) :  positionInSequence;
		int qEnd = reverseStrand ?  (seqLength - positionInSequence) : positionInSequence + length;
		Integer contigLength =  getChromosomeSize(cp.getChromosome());
		int tStart = cp.getStartPosition();
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(length)
				.withMisMatch(mismatch)
				.withRepMatch(0)
				.withNCount(0)
				.withQNumInsert(0)
				.withQBaseInsert(0)
				.withTNumInsert(0)
				.withTBaseInsert(0)
				.withStrand(reverseStrand ? '-' : '+')
				.withQName(name)
				.withSize(seqLength)
				.withQStart(qStart)
				.withQEnd(qEnd)
				.withTName(cp.getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart( tStart)
				.withTEnd(tStart + length)
				.withBlockCount(1)
				.withBlockSizes("" + length)
				.withQStarts("" + positionInSequence)
				.withTStarts("" + tStart)
				.build();
		return br;
	}

	public static BLATRecord blatRecordFromSplit(IntLongPair split, String name, int seqLength, PositionChrPositionMap headerMap) {
		return blatRecordFromSplit(split, name, seqLength, headerMap, TARecordUtil.TILE_LENGTH);
	}

	/**
	 * 
	 * @param splits
	 * @param name
	 * @param seqLength
	 * @param headerMap
	 * @param tileLength
	 * @return
	 */
	public static Optional<BLATRecord> blatRecordFromSplits(IntLongPairs splits, String name, int seqLength, PositionChrPositionMap headerMap, int tileLength) {
		
		Map<ChrPosition, int[]> chrPosBlocks = TARecordUtil.getChrPositionAndBlocksFromSplits(splits, seqLength, headerMap);
		/*
		 * order the keys
		 */
		ChrPosition[] keys = new ChrPosition[chrPosBlocks.size()];
		chrPosBlocks.keySet().toArray(keys);
		Arrays.sort(keys);
		
		List<int[]> values = new ArrayList<>(chrPosBlocks.values());
		values.sort((int[] array1, int[] array2) -> array1[0] - array2[0]);

		int qGapBases = 0;
		
		for (int i = 0 ; i < values.size() - 1; i++) {
			int [] thisBlock = values.get(i);
			int [] nextBlock = values.get(i + 1);
			qGapBases += nextBlock[0] - (thisBlock[1] + thisBlock[0]);
		}
		
		boolean reverseStrand = keys[0].getName().equals("R");
		/*
		 * get length and qGapBases
		 */
		int length = 0;
		int tGapBases = 0;
		for (int i = 0 ; i < keys.length - 1; i++) {
			ChrPosition thisCp = keys[i];
			length += thisCp.getLength() - 1; 
			ChrPosition nextCp = keys[i + 1];
			
			/*
			 * If either of these ChrPositions are wholly contained within the other, then return
			 */
			if (ChrPositionUtils.isChrPositionContained(thisCp, nextCp) || ChrPositionUtils.isChrPositionContained(nextCp, thisCp)) {
				return Optional.empty();
			}
			
			tGapBases += (nextCp.getStartPosition() - thisCp.getEndPosition());
		}
		/*
		 * add last length
		 */
		length += keys[keys.length - 1].getLength() - 1;
		int[] lastBlock = chrPosBlocks.get(keys[keys.length - 1]);
		Integer contigLength =  getChromosomeSize(keys[0].getChromosome());
		int tStart = keys[0].getStartPosition();
		
		BLATRecord br = new BLATRecord.Builder()
				.withMatch(length)
				.withMisMatch(IntLongPairsUtil.getMismatches(splits))
				.withRepMatch(0)
				.withNCount(0)
				.withQNumInsert(qGapBases > 0 ? 1 : 0)
				.withQBaseInsert(qGapBases)
				.withTNumInsert(keys.length - 1)
				.withTBaseInsert(tGapBases)
				.withStrand(reverseStrand ? '-' : '+')
				.withQName(name)
				.withSize(seqLength)
				.withQStart(chrPosBlocks.get(keys[0])[0])
				.withQEnd(lastBlock[0] + lastBlock[1] - 1)
				.withTName(keys[0].getChromosome())
				.withTSize(contigLength == null ? 1000000000 : contigLength)
				.withTStart( tStart)
				.withTEnd(keys[keys.length - 1].getEndPosition())
				.withBlockCount(keys.length)
				.withBlockSizes(Arrays.stream(keys).map(cp -> "" + (cp.getLength() - 1)).collect(Collectors.joining(",")))
				.withQStarts(Arrays.stream(keys).map(cp -> "" + (cp.getName().equals("R") ? seqLength - (chrPosBlocks.get(cp)[0] + chrPosBlocks.get(cp)[1]) : chrPosBlocks.get(cp)[0])).collect(Collectors.joining(",")))
				.withTStarts(Arrays.stream(keys).map(cp -> "" + cp.getStartPosition()).collect(Collectors.joining(",")))
				.build();
		return Optional.of(br);
	}
}
