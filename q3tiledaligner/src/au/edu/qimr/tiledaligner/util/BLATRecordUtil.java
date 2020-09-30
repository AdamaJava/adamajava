package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
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
		System.out.println("getDetailsForBLATRecord with positions: " + positions.stream().map(ChrPosition::toString).collect(Collectors.joining(",")) + ", name: " + name + ", sequence: " + sequence + ", fs: " + forwardStrand);
		
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

}
