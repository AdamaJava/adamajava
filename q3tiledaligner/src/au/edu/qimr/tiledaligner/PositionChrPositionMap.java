/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.NumberUtils;

import au.edu.qimr.tiledaligner.util.TiledAlignerUtil;
import htsjdk.samtools.reference.FastaSequenceIndex;

public class PositionChrPositionMap {
	
	private static final QLogger logger = QLoggerFactory.getLogger(PositionChrPositionMap.class);
	
	public static final List<String> grch37Positions = Collections.unmodifiableList(Arrays.asList("##chr1:249250621:1","##chr2:243199373:249250622","##chr3:198022430:492449995","##chr4:191154276:690472425","##chr5:180915260:881626701","##chr6:171115067:1062541961","##chr7:159138663:1233657028",
	"##chr8:146364022:1392795691","##chr9:141213431:1539159713","##chr10:135534747:1680373144","##chr11:135006516:1815907891","##chr12:133851895:1950914407","##chr13:115169878:2084766302","##chr14:107349540:2199936180","##chr15:102531392:2307285720",
	"##chr16:90354753:2409817112","##chr17:81195210:2500171865","##chr18:78077248:2581367075","##chr19:59128983:2659444323","##chr20:63025520:2718573306","##chr21:48129895:2781598826","##chr22:51304566:2829728721","##chrX:155270560:2881033287",
	"##chrY:59373566:3036303847",
	"##GL000191.1:106433:3095677413","##GL000192.1:547496:3095783846","##GL000193.1:189789:3096331342","##GL000194.1:191469:3096521131","##GL000195.1:182896:3096712600","##GL000196.1:38914:3096895496",
	"##GL000197.1:37175:3096934410","##GL000198.1:90085:3096971585","##GL000199.1:169874:3097061670","##GL000200.1:187035:3097231544","##GL000201.1:36148:3097418579","##GL000202.1:40103:3097454727","##GL000203.1:37498:3097494830","##GL000204.1:81310:3097532328",
	"##GL000205.1:174588:3097613638","##GL000206.1:41001:3097788226","##GL000207.1:4262:3097829227","##GL000208.1:92689:3097833489","##GL000209.1:159169:3097926178","##GL000210.1:27682:3098085347","##GL000211.1:166566:3098113029","##GL000212.1:186858:3098279595",
	"##GL000213.1:164239:3098466453","##GL000214.1:137718:3098630692","##GL000215.1:172545:3098768410","##GL000216.1:172294:3098940955","##GL000217.1:172149:3099113249","##GL000218.1:161147:3099285398","##GL000219.1:179198:3099446545",
	"##GL000220.1:161802:3099625743","##GL000221.1:155397:3099787545","##GL000222.1:186861:3099942942","##GL000223.1:180455:3100129803","##GL000224.1:179693:3100310258","##GL000225.1:211173:3100489951","##GL000226.1:15008:3100701124",
	"##GL000227.1:128374:3100716132","##GL000228.1:129120:3100844506","##GL000229.1:19913:3100973626","##GL000230.1:43691:3100993539","##GL000231.1:27386:3101037230","##GL000232.1:40652:3101064616","##GL000233.1:45941:3101105268",
	"##GL000234.1:40531:3101151209","##GL000235.1:34474:3101191740","##GL000236.1:41934:3101226214","##GL000237.1:45867:3101268148","##GL000238.1:39939:3101314015","##GL000239.1:33824:3101353954","##GL000240.1:41933:3101387778","##GL000241.1:42152:3101429711",
	"##GL000242.1:43523:3101471863","##GL000243.1:43341:3101515386","##GL000244.1:39929:3101558727","##GL000245.1:36651:3101598656","##GL000246.1:38154:3101635307","##GL000247.1:36422:3101673461","##GL000248.1:39786:3101709883","##GL000249.1:38502:3101749669",
	"##chrMT:16569:3101788171"));
	
	
	public static Map<ChrPosition, LongRange> loadMap(List<String> chrPosStartPos) {
		/*
		 * check inputs
		 */
		if (null == chrPosStartPos) {
			throw new IllegalArgumentException("Null list passed to PositionChrPositionMap.loadMap");
		}
		
		Map<ChrPosition, LongRange> map = new HashMap<>();
		for (String s : chrPosStartPos) {
			if (s.startsWith("##chr") || s.startsWith("##GL000")) {
				String [] params = s.substring(2).split(Constants.COLON_STRING);
				int chrLength = Integer.parseInt(params[1]);
				long startOffset = Long.parseLong(params[2]);
				ChrPosition cp = new ChrRangePosition(params[0], 1, chrLength);
				LongRange range = new LongRange(startOffset, startOffset + chrLength - 1);
				map.put(cp,  range);
			}
		}
		
		/*
		 * check to see if map contains overlaps
		 * if it does then throw an exception
		 */
		if (doesMapContainOverlaps(map)) {
			throw new IllegalArgumentException("ReferenceIndexFile supplied has regions that overlap!");
		}
		
		return Collections.unmodifiableMap(map);
	}
	
	public static Map<ChrPosition, LongRange> loadGRCh37Map() {
		return loadMap(grch37Positions);
	}
	
	/**
	 * Take in a reference index file and return a map of ChrPosition -> Long
	 * 
	 * @param referenceIndexFile
	 * @return
	 */
	public static Map<ChrPosition, LongRange> loadMap(String referenceIndexFile) {
		
		/*
		 * check value for file has been supplied
		 */
		if (StringUtils.isNullOrEmpty(referenceIndexFile)) {
			throw new IllegalArgumentException("Null referenceIndexFile supplied to PositionChrPositionMap.loadMap");
		}
		
		/*
		 * check that we can read the file
		 */
		Path referenceIndexPath = Paths.get(referenceIndexFile);
		if ( ! Files.isReadable(referenceIndexPath)) {
			throw new IllegalArgumentException("ReferenceIndexFile supplied to PositionChrPositionMap.loadMap does not exist or is not readable");
		}
		
		FastaSequenceIndex index = new FastaSequenceIndex(referenceIndexPath);
		Map<ChrPosition, LongRange> map = new HashMap<>();
		AtomicLong startPosition = new AtomicLong(1);
		
		index.forEach(i -> {
			long chrLength = i.getSize();
			ChrPosition cp = new ChrRangePosition(i.getContig(), 1, (int) chrLength);
			LongRange range = new LongRange(startPosition.longValue(), startPosition.longValue() + chrLength - 1);
			startPosition.addAndGet(chrLength);
			map.put(cp,  range);
		});
		
		/*
		 * check to see if map contains overlaps
		 * if it does then throw an exception
		 */
		
		if (doesMapContainOverlaps(map)) {
			throw new IllegalArgumentException("ReferenceIndexFile supplied has regions that overlap!");
		}
		
		return Collections.unmodifiableMap(map);
	}
	
	
	/**
	 * Check that there are no LongRanges in the supplied map that overlap each other
	 * @param map
	 * @return
	 */
	public static boolean doesMapContainOverlaps(Map<ChrPosition, LongRange> map) {
		if (null == map) {
			throw new IllegalArgumentException("Null map passed to PositionChrPositionMap.doesMapContainOverlaps");
		}
		
		List<LongRange> ranges = new ArrayList<>(map.values());
		int rangesSize = ranges.size();
		for (int i = 0; i < rangesSize ; i++) {
			
			LongRange lr1 = ranges.get(i);
			for (int j = i + 1 ; j < rangesSize ; j++) {
				 if (LongRange.doRangesOverlap(lr1, ranges.get(j))) {
					 logger.warn("OVerlapping region found!! range1: " + lr1.from + "-" + lr1.to + " and " + ranges.get(j).from + "-" + ranges.get(j).to);
					 return true;
				 }
			}
		}
		return false;
	}
	
	/**
	 * Return the start position of the ChrPosition object as a long primative
	 * @param cp
	 * @return
	 */
	public static long getLongStartPositionFromChrPosition(ChrPosition cp, Map<ChrPosition, LongRange> map) {
		/*
		 * loop through the keys in the map
		 */
		long startPosition = -1;
		for (Entry<ChrPosition, LongRange> entry : map.entrySet()) {
			if (ChrPositionUtils.doChrPositionsOverlap(entry.getKey(), cp)) {
				startPosition = entry.getValue().from + cp.getStartPosition();
				break;
			}
		}
		return startPosition;
	}
	
	public static long[] getLongStartAndStopPositionFromChrPosition(ChrPosition cp, Map<ChrPosition, LongRange> map) {
		/*
		 * loop through the keys in the map
		 */
		long [] position = new long[2];
		for (Entry<ChrPosition, LongRange> entry : map.entrySet()) {
			if (ChrPositionUtils.doChrPositionsOverlap(entry.getKey(), cp)) {
				position[0] = entry.getValue().from + cp.getStartPosition();
				position[1] = entry.getValue().to + cp.getStartPosition();
				break;
			}
		}
		return position;
	}
	
	public static ChrPosition getChrPositionFromLongPosition(long position, Map<ChrPosition, LongRange> map) {
		return getChrPositionFromLongPosition(position, 0, map);
	}
	
	public static ChrPosition getChrPositionFromLongPosition(long position, int length, Map<ChrPosition, LongRange> map) {
		/*
		 * Need to loop through our map values, and check each one to see if the position falls within the range.
		 * Should only every have 1 range that encompasses a position....
		 */
		boolean reverseStrand = NumberUtils.isBitSet(position, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
		position = NumberUtils.getLongPositionValueFromPackedLong(position, TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
		
		for (Entry<ChrPosition, LongRange> entry : map.entrySet()) {
			LongRange lr = entry.getValue();
			
			
			if (lr.isPositionWithinRange(position)) {
				/*
				 * Calculate position within contig
				 */
				long positionWithinContig = (position - lr.getFrom());
				if (positionWithinContig > Integer.MAX_VALUE) {
					// oops
					logger.warn("positionWithinContig can't be cast to int without overflow!!!");
				}
				ChrPositionName cp = new ChrPositionName(entry.getKey().getChromosome(), (int) positionWithinContig, (int) positionWithinContig + length, (reverseStrand ? "R" : "F"));
				return cp;
			}
		}
		logger.warn("could not find ChrPosition for postion: " + position);
		return null;
	}
	
	public static ChrPosition getBufferedChrPositionFromLongPosition(long position, int length, int lhsBuffer, int rhsBuffer, Map<ChrPosition, LongRange> map) {
		/*
		 * Need to loop through our map values, and check each one to see if the position falls within the range.
		 * Should only ever have 1 range that encompasses a position....
		 */
		boolean reverseStrand = NumberUtils.isBitSet(position, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
		position = NumberUtils.getLongPositionValueFromPackedLong(position, TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
		
		for (Entry<ChrPosition, LongRange> entry : map.entrySet()) {
			LongRange lr = entry.getValue();
			if (lr.isPositionWithinRange(position)) {
				/*
				 * Calculate position within contig
				 */
				long positionWithinContig = (position - lr.getFrom());
				if (positionWithinContig > Integer.MAX_VALUE) {
					// oops
					logger.warn("positionWithinContig can't be cast to int without overflow!!!");
				}
				int startPosition = (int) positionWithinContig - lhsBuffer;
				int endPosition = (int) positionWithinContig + length + rhsBuffer;
				if (endPosition <= startPosition) {
					logger.error("end position is before (or equal to) start position! startPos: " + startPosition  + ", end position: " + endPosition 
							+ ", long position: " + position + ", length: " + length + ", lhsBuffer: " + lhsBuffer + ", rhsBuffer: " + rhsBuffer); 
				}
				return new ChrPositionName(entry.getKey().getChromosome(), Math.max(1, startPosition), endPosition, (reverseStrand ? "R" : "F"));
			}
		}
		logger.warn("could not find ChrPosition for postion: " + position);
		return null;
	}
	
	public static class LongRange {
		@Override
		public int hashCode() {
			return Objects.hash(from, to);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LongRange other = (LongRange) obj;
			return from == other.from && to == other.to;
		}

		private final long from;
		private final long to;
		
		public LongRange(long from, long to) {
			this.from = from;
			this.to = to;
		}
		
		public boolean isPositionWithinRange(long position) {
			return from <= position && position <= to;
		}
		
		public long getFrom() {
			return from;
		}
		
		public static boolean doRangesOverlap(LongRange lr1, LongRange lr2) {
			if (lr2.from > lr1.to || lr2.to < lr1.from) {
				return false;
			}
			return true;
		}
	}

}
