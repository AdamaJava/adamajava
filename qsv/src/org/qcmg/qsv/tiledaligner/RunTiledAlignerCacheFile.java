package org.qcmg.qsv.tiledaligner;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.string.StringFileReader;

import gnu.trove.map.TIntObjectMap;
import htsjdk.samtools.util.SequenceUtil;

public class RunTiledAlignerCacheFile {
	
	public static final String TILED_ALIGNER_FILE = "/reference/genomeinfo/q3clinvar/q3tiledaligner.out.gz";
	public static final int TILE_LENGTH = 13;
	
	public static void getEntriesFromTiledAligner(String input, Map<String, String> sequenceTiles) {
		int bufferSize = 64;
		try (StringFileReader reader = new StringFileReader(new File(null == input ? TILED_ALIGNER_FILE : input), bufferSize * 1024)) {
			long start = System.currentTimeMillis();
			for (String rec : reader) {
				int tabIndex = rec.indexOf(Constants.TAB);
				String taTile = rec.substring(0, tabIndex);
				
				if (sequenceTiles.containsKey(taTile)) {
					sequenceTiles.put(taTile, rec.substring(tabIndex + 1));
				}
				
			}
			System.out.println("Time taken with buffer size: " + bufferSize + "kb is: " + (System.currentTimeMillis() - start));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void writeToFile(String output, Map<String, String> sequenceTiles) throws IOException {
		
		try (FileWriter writer = new FileWriter(output)) {
			List<String> sortedKeys = new ArrayList<>(sequenceTiles.keySet());
			sortedKeys.sort(null);
			
			for (String key : sortedKeys) {
				String value = sequenceTiles.get(key);
				if (null != value) {
					writer.write(key + Constants.TAB + value + "\n");
				}
			}
		}
	}
	
	public static Map<String, String> createMapFromSequences(String [] sequences) {
		Map<String, String> results = new HashMap<>();
		for (String s : sequences) {
			String rc = SequenceUtil.reverseComplement(s);
			int length = s.length();
			for (int i = 0 ; i < length - TILE_LENGTH + 1 ; i++) {
				String tile = s.substring(i, i + TILE_LENGTH);
				String tileRc = rc.substring(i, i + TILE_LENGTH);
				results.put(tile, null);
				results.put(tileRc, null);
			}
		}
		System.out.println("Created map with " + results.size() + " entries in createMapFromSequences");
		return results;
	}
	
	public static Map<String, List<BLATRecord>> loadBlatResultsFromFile(String psl, boolean includeSplitCon) throws IOException {
		List<String> blatRecords = Files.lines(new File(psl).toPath()).collect(Collectors.toList());
		
		/*
		 * convert to map of name, list of blat records
		 */
		return blatRecords.stream().map(s -> new BLATRecord(s.split("\t"))).filter(br -> includeSplitCon || ! br.getName().startsWith("splitcon")).collect(Collectors.groupingBy(BLATRecord::getName));
	}
	
	public static Map<String, List<String>> getSequencesAndNamesFromFile(String input, boolean includeSplitCon) {
		Map<String, List<String>> map = new HashMap<>();
		
		List<String> sequences = null;
		try {
			sequences = Files.lines(new File(input).toPath()).collect(Collectors.toList());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for (int i = 0, len = sequences.size() ; i < len ; i += 2) {
			if (sequences.get(i).startsWith(">") && ! StringUtils.isNullOrEmpty(sequences.get(i + 1)) && ! "null".equals(sequences.get(i + 1))) {
//				System.out.println("about to put " + sequences.get(i) + " and " + sequences.get(i + 1) + " into map");
				if (sequences.get(i).substring(1).startsWith("chr7_57160469_false_+")) {
					System.out.println("found chr7_57160469_false_+ in input file, seq: " + sequences.get(i + 1));
				}
				if (includeSplitCon || ! sequences.get(i).substring(1).startsWith("splitcon")) {
					if (sequences.get(i + 1).length() >= 20) {
						if (sequences.get(i).substring(1).startsWith("chr7_57160469_false_+")) {
							System.out.println("adding to map chr7_57160469_false_+ in input file");
						}
						map.computeIfAbsent(sequences.get(i + 1), f -> new ArrayList<>(2)).add(sequences.get(i).substring(1));
					} else {
						System.out.println("Sequence is less than 20 in length: " + sequences.get(i + 1).length() + ", " + sequences.get(i + 1));
					}
				}
			} else {
				/*
				 * just print out a few of the problem positions
				 */
				if (i < 200) {
					System.out.println("expecting a name, got a sequence!!! i: " + i + ", sequences.get(i):" + sequences.get(i));
				}
			}
		}
		return map;
	}
	
	public static Map<String, String> convertListToString(Map<String, List<String>> origMap) {
		Map<String, String> newMap = new HashMap<>();
		for (Entry<String, List<String>> entry : origMap.entrySet()) {
			newMap.put(entry.getKey(), entry.getValue().stream().collect(Collectors.joining(":")));
		}
		return newMap;
	}
	
	public static boolean doesBlatRecMatchName(String name, String recChr, int recStart, int recStop) {
		String [] nameArray = name.split("_");
		int positionBase = 0;
		if (nameArray[positionBase].equals("splitcon")) {
			positionBase = 1;
		}
		String nameChr = nameArray[positionBase];
		int namePos = Integer.parseInt(nameArray[positionBase + 1]);
		if (nameChr.equals(recChr) && Math.abs(namePos - recStart) < 1000) {
			return true;
		}
		return false;
	}
	
	public static void checkResults(Map<String, List<BLATRecord>> pslResults, Map<String, List<BLATRecord>> taResults, Map<String, String> nameSeqMap) {
		
		/*
		 * get stats on whether top result corresponds with name
		 */
		int pslOriginalRecordCount = pslResults.size();
		int taOriginalRecordCount = taResults.size();
		int pslNameChrMatch = 0;
		int pslNameChrPosMatch = 0;
		int taNameChrMatch = 0;
		int taNameChrPosMatch = 0;
		for (Entry<String, List<BLATRecord>> entry : pslResults.entrySet()) {
			List<BLATRecord> recs = entry.getValue();
			recs.sort(null);
			BLATRecord topRec = recs.get(recs.size() - 1);
			if (entry.getKey().contains(topRec.getReference())) {
				pslNameChrMatch++;
				if (doesBlatRecMatchName(entry.getKey(), topRec.getReference(), topRec.getStartPos(), topRec.getEndPos())) {
					pslNameChrPosMatch++;
				}
			}
		}
		System.out.println("psl recs: pslNameChrMatch: " + pslNameChrMatch + ", pslNameChrPosMatch: " + pslNameChrPosMatch + " out of " + pslResults.size());
		
		for (Entry<String, List<BLATRecord>> entry : taResults.entrySet()) {
			List<BLATRecord> recs = entry.getValue();
			recs.sort(null);
			BLATRecord topRec = recs.get(recs.size() - 1);
			if (entry.getKey().contains(topRec.getReference())) {
				taNameChrMatch++;
				if (doesBlatRecMatchName(entry.getKey(), topRec.getReference(), topRec.getStartPos(), topRec.getEndPos())) {
					taNameChrPosMatch++;
				}
			}
		}
		System.out.println("ta recs: taNameChrMatch: " + taNameChrMatch + ", taNameChrPosMatch: " + taNameChrPosMatch + " out of " + taResults.size());
		
		
		/*
		 * we should see corresponding entries for each entry...
		 */
		int match = 0;
		int noMatch = 0;
		int partialMatch = 0;
		int pslHigherScore = 0;
		int taHigherScore = 0;
		int equalScore = 0;
		int noResults = 0;
		int pslWinsButInTASWList = 0;
		
		int totalBlatScore = 0;
		int totalTAScore = 0;
		
		IntSummaryStatistics overallSeqLength = new IntSummaryStatistics(); 
		IntSummaryStatistics overallNCount = new IntSummaryStatistics();
		IntSummaryStatistics overallCardinality = new IntSummaryStatistics();
		
		IntSummaryStatistics matchSeqLength = new IntSummaryStatistics(); 
		IntSummaryStatistics matchNCount = new IntSummaryStatistics();
		IntSummaryStatistics matchCardinality = new IntSummaryStatistics();
		
		IntSummaryStatistics taWinsSeqLength = new IntSummaryStatistics(); 
		IntSummaryStatistics taWinsNCount = new IntSummaryStatistics();
		IntSummaryStatistics taWinsCardinality = new IntSummaryStatistics();
		IntSummaryStatistics blatWinsSeqLength = new IntSummaryStatistics(); 
		IntSummaryStatistics blatWinsNCount = new IntSummaryStatistics();
		IntSummaryStatistics blatWinsCardinality = new IntSummaryStatistics();
		IntSummaryStatistics blatWinsTAPresentSeqLength = new IntSummaryStatistics(); 
		IntSummaryStatistics blatWinsTAPresentNCount = new IntSummaryStatistics();
		IntSummaryStatistics blatWinsTAPresentCardinality = new IntSummaryStatistics();
		
		for (Entry<String, List<BLATRecord>> entry : pslResults.entrySet()) {
			
			List<BLATRecord> taRecs = taResults.remove(entry.getKey());
			String sequence = nameSeqMap.get(entry.getKey());
			int seqLength = sequence.length();
			int nCount = StringUtils.getCount(sequence, 'N');
			int complexity = StringUtils.determineSequenceComplexity(sequence).cardinality();
			
			overallSeqLength.accept(seqLength);
			overallNCount.accept(nCount);
			overallCardinality.accept(complexity);
			
			
			if (null == taRecs) {
				System.out.println("no TA record for " + entry.getKey());
				noResults++;
				
				List<BLATRecord> pslRecords = entry.getValue();
				pslRecords.sort(null);
				BLATRecord bestPsl = pslRecords.get(pslRecords.size() - 1);
				int blatScore = bestPsl.getScore();
				totalBlatScore += blatScore;
			} else {
				/*
				 * get best rec from both and compare....
				 */
				List<BLATRecord> pslRecords = entry.getValue();
				pslRecords.sort(null);
				BLATRecord bestPsl = pslRecords.get(pslRecords.size() - 1);
				BLATRecord bestTA = taRecs.get(taRecs.size() - 1);
				
				int blatScore = bestPsl.getScore();
				int taScore = bestTA.getScore();
				totalBlatScore += blatScore;
				totalTAScore += taScore;
				
				if (doBlatRecsMatch(bestPsl, bestTA)) {
					match++;
					
					matchSeqLength.accept(seqLength);
					matchNCount.accept(nCount);
					matchCardinality.accept(complexity);
					
					
				} else {
					noMatch++;
					
					
					/*
					 * do a little digging
					 * get top 3 recs from each caller and compare
					 */
//					if (top3Overlap(pslRecords, taRecs)) {
//						partialMatch++;
//					} else {
//						System.out.println("Number of psl recs: " + pslRecords.size() + ", top scorer: " + bestPsl.getScore() + ", number of ta recs: " + taRecs.size() + ", top scorer: " + bestTA.getScore());
						if (bestPsl.getScore() > bestTA.getScore()) {
							pslHigherScore++;
//							totalBlatScore += blatScore;
							if (isRecordInCollection(bestPsl, taRecs, sequence)) {
								pslWinsButInTASWList++;
								blatWinsTAPresentSeqLength.accept(seqLength);
								blatWinsTAPresentNCount.accept(nCount);
								blatWinsTAPresentCardinality.accept(complexity);
							}
							
							blatWinsSeqLength.accept(seqLength);
							blatWinsNCount.accept(nCount);
							blatWinsCardinality.accept(complexity);
							
							
						} else if (bestPsl.getScore() == bestTA.getScore()) {
							equalScore++;
//							totalBlatScore += blatScore;
//							totalTAScore += taScore;
						} else {
							taHigherScore++;
//							totalTAScore += taScore;
							taWinsSeqLength.accept(seqLength);
							taWinsNCount.accept(nCount);
							taWinsCardinality.accept(complexity);
						}
//					}
				}
			}
		}
		System.out.println("psl record count: " + pslOriginalRecordCount);
		System.out.println("ta record count: " + taOriginalRecordCount);
		System.out.println("match: " + match + ", noMatch: " + noMatch + ", partialMatch: " + partialMatch + ", pslHigherScore: " + pslHigherScore + ", (psl wins but in TA/SW list: " + pslWinsButInTASWList + "), taHigherScore: " + taHigherScore + ", equalScore: " + equalScore + ", noResults: " + noResults + ", remaining TA recs: " + taResults.size());
		
		
		System.out.println("overallSeqLength, count: " + overallSeqLength.getCount() + ", average: " + overallSeqLength.getAverage());
		System.out.println("overallNCount, count: " + overallNCount.getCount() + ", average: " + overallNCount.getAverage());
		System.out.println("overallCardinality, count: " + overallCardinality.getCount() + ", average: " + overallCardinality.getAverage());
		System.out.println("matchSeqLength, count: " + matchSeqLength.getCount() + ", average: " + matchSeqLength.getAverage());
		System.out.println("matchNCount, count: " + matchNCount.getCount() + ", average: " + matchNCount.getAverage());
		System.out.println("matchCardinality, count: " + matchCardinality.getCount() + ", average: " + matchCardinality.getAverage());
		System.out.println("taWinsSeqLength, count: " + taWinsSeqLength.getCount() + ", average: " + taWinsSeqLength.getAverage());
		System.out.println("taWinsNCount, count: " + taWinsNCount.getCount() + ", average: " + taWinsNCount.getAverage());
		System.out.println("taWinsCardinality, count: " + taWinsCardinality.getCount() + ", average: " + taWinsCardinality.getAverage());
		System.out.println("blatWinsSeqLength, count: " + blatWinsSeqLength.getCount() + ", average: " + blatWinsSeqLength.getAverage());
		System.out.println("blatWinsNCount, count: " + blatWinsNCount.getCount() + ", average: " + blatWinsNCount.getAverage());
		System.out.println("blatWinsCardinality, count: " + blatWinsCardinality.getCount() + ", average: " + blatWinsCardinality.getAverage());
		System.out.println("blatWinsTAPresentSeqLength, count: " + blatWinsTAPresentSeqLength.getCount() + ", average: " + blatWinsTAPresentSeqLength.getAverage());
		System.out.println("blatWinsTAPresentNCount, count: " + blatWinsTAPresentNCount.getCount() + ", average: " + blatWinsTAPresentNCount.getAverage());
		System.out.println("blatWinsTAPresentCardinality, count: " + blatWinsTAPresentCardinality.getCount() + ", average: " + blatWinsTAPresentCardinality.getAverage());
		System.out.println("total blat score: " + totalBlatScore + ", total TA score: " + totalTAScore);
	}
	
	
	public static boolean doBlatRecsMatch(BLATRecord b1, BLATRecord b2) {
		return doBlatRecsMatch(b1, b2, 100);
	}
	public static boolean doBlatRecsMatch(BLATRecord b1, BLATRecord b2, int buffer) {
		return 	b1.getReference().equals(b2.getReference())
				&& Math.abs(b1.getStartPos() - b2.getStartPos()) < buffer;
	}
	
	
	public static boolean top3Overlap(List<BLATRecord> pslBlats, List<BLATRecord> taBlats) {
		/*
		 * looking for any kind of match between the records in these collections
		 */
		for (BLATRecord b1 : pslBlats) {
			for (BLATRecord b2 : taBlats) {
				if (doBlatRecsMatch(b1, b2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isRecordInCollection(BLATRecord rec, List<BLATRecord> recs, String seq) {
		/*
		 * looking for any kind of match between the records in these collections
		 */
		for (BLATRecord r : recs) {
			if (doBlatRecsMatch(r, rec)) {
				System.out.println("found a match for rec: " + rec + " and it is r: " + r + ", seq: " + seq);
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws IOException {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument(s) should be the sequences
		 */
		String tiledAlignerFile = args[0];
		String faFile = args[1];
		String pslFile = args[2];
		
		boolean includeSplitCon = false;
		
		
		Map<String, List<String>> seqNameMap = getSequencesAndNamesFromFile(faFile, includeSplitCon);
		System.out.println("Loaded " + seqNameMap.size() + " entries into seqNameMap");
		
		
		Map<String, String> seqNameAsStringMap = convertListToString(seqNameMap);
		
		Map<String, String> nameSeqMap = new HashMap<>(seqNameMap.size() * 2);
		for (Entry<String, List<String>> entry : seqNameMap.entrySet()) {
			for (String name : entry.getValue()) {
				nameSeqMap.put(name, entry.getKey());
			}
		}
		
		System.out.println("Loaded " + nameSeqMap.size() + " entries into nameSeqMap");
		
		String name = "chr7_57160469_false_+";
		String seq = nameSeqMap.get(name);
		System.out.println("seq for name " + name + ", is: " + seq);
		
		int i = 0;
		for (String s : seqNameMap.keySet()) {
			if (++i > 10) {
				break;
			}
			System.out.println("seq: " + s + ", name: " + seqNameMap.get(s));
		}
		i = 0;
		for (String s : nameSeqMap.keySet()) {
			if (++i > 10) {
				break;
			}
			System.out.println("name: " + s + ", seq: " + nameSeqMap.get(s));
		}
		
		
		Map<String, List<BLATRecord>> pslBlatResults = loadBlatResultsFromFile(pslFile, includeSplitCon);
		System.out.println("blatResults size: " + pslBlatResults.size());
		/*
		 * check to see if we have psl results for all names in map
		 */
		int match = 0, noMatch = 0;
		i = 0;
		for (String k : nameSeqMap.keySet()) {
			if (pslBlatResults.containsKey(k)) {
				match++;
			} else {
				noMatch++;
				if (i++ < 10) {
					System.out.println("no match for name: " + k);
				}
			}
		}
		System.out.println("psl results, match: " + match + ", noMatch: " + noMatch);
		
		/*
		 * load cache
		 */
		TIntObjectMap<int[]> cache = TiledAlignerLongMap.getCache(tiledAlignerFile, 64);
		
		long start = System.currentTimeMillis();
		System.out.println("About to run runTiledAlignerCache " );
		Map<String, List<BLATRecord>> taResults = TiledAlignerUtil.runTiledAlignerCache(cache, seqNameAsStringMap, 13, "RunTiledAlignerCacheFile.main", false);
//		Map<String, List<BLATRecord>> taResults = TiledAlignerUtil.runTiledAlignerCache(cache, seqNameMap.keySet().toArray(new String[] {}), 13, "RunTiledAlignerCacheFile.main", false);
		
		if (null != seq) {
			List<BLATRecord> blatsForSeq = taResults.get(seq);
			System.out.println("Found " + (null == blatsForSeq ? "null" : ""+blatsForSeq.size()) + " ta blat recs for seq: " + seq);
		}
		
		System.out.println("About to run runTiledAlignerCache - DONE number of results: " + taResults.size());
		System.out.println("SmithWaterman count: " + TiledAlignerUtil.swCounter.get());
		System.out.println("leaderboard distribution count: " + TiledAlignerUtil.iss.getCount() + ", max: " + TiledAlignerUtil.iss.getMax() + ", min: " + TiledAlignerUtil.iss.getMin() + ", average: " + TiledAlignerUtil.iss.getAverage() + ", count: " + TiledAlignerUtil.iss.getCount());
		System.out.println("dist:");
		int [] keys = TiledAlignerUtil.leaderboardStats.keys();
		Arrays.sort(keys);
		for (int j : keys) {
			System.out.println("key: " + j + ", count: " + TiledAlignerUtil.leaderboardStats.get(j).get());
		}
		
		
		AtomicIntegerArray firstPositionDist = TiledAlignerUtil.taClassifierArrayPosition1;
		System.out.println("First position dist:");
		for (int j = 0 ; j < firstPositionDist.length() ; j++) {
			if (firstPositionDist.get(j) > 0) {
				System.out.println("classification: " + TAClassifier.getTAClassifier(j) + ", count: " + firstPositionDist.get(j));
			}
		}
		
		AtomicIntegerArray secondPositionDist = TiledAlignerUtil.taClassifierArrayPosition2;
		System.out.println("Second position dist:");
		for (int j = 0 ; j < secondPositionDist.length() ; j++) {
			if (secondPositionDist.get(j) > 0) {
				System.out.println("classification: " + TAClassifier.getTAClassifier(j) + ", count: " + secondPositionDist.get(j));
			}
		}
		
		ConcurrentMap<String, List<String>> sequenceMethodNameMap = TiledAlignerUtil.sequenceOriginatingMethodMap;
		System.out.println("sequenceMethodNameMap stats, size: " + sequenceMethodNameMap.size());
		
		int longestSeq = 0;
		int shortestSeq = Integer.MAX_VALUE;
		int mostCalled = 0;
		int leastCalled = Integer.MAX_VALUE;
		for (Entry<String, List<String>> entry : sequenceMethodNameMap.entrySet()) {
			int seqLength = entry.getKey().length();
			int calledCount = entry.getValue().size();
			if (seqLength > longestSeq) {
				longestSeq = seqLength;
			}
			if (seqLength < shortestSeq) {
				shortestSeq = seqLength;
			}
			if (calledCount > mostCalled) {
				mostCalled = calledCount;
			}
			if (calledCount < leastCalled) {
				leastCalled = calledCount;
			}
		}
		System.out.println("sequenceMethodNameMap stats, longestSeq: " + longestSeq + ", shortestSeq: " + shortestSeq + ", mostCalled: " + mostCalled + ", leastCalled: " + leastCalled);
		System.out.println("Time taken to run tiledAligner (sw): " + (System.currentTimeMillis() - start));
		
		
		/*
		 * taResults is currently sequence to list of blat records - need this to be name to list of blat records
		 */
		Map<String, List<BLATRecord>> updatedTAResults = updateMapWithName(taResults, seqNameMap);
		
		
		
//		/*
//		 * checking for chr7_57160469_false_+
//		 */
//		List<BLATRecord> pslRecsForTest = pslBlatResults.get("chr7_57160469_false_+");
//		List<BLATRecord> taRecsForTest = updatedTAResults.get("chr7_57160469_false_+");
//		System.out.println("pslRecsForTest size: " + (null != pslRecsForTest ? pslRecsForTest.size() : 0) + ", taRecsForTest size: " + (taRecsForTest == null ? 0 : taRecsForTest.size())); 
		
		checkResults(pslBlatResults, updatedTAResults, nameSeqMap);
		
		
		/*
		 * print out
		 */
//		if (null != results && ! results.isEmpty()) {
//			for (Entry<String, List<BLATRecord>> entry : results.entrySet()) {
//				System.out.println("for sequence: " + entry.getKey() + ", " + entry.getValue().size() + " BLATRecords");
//				for (BLATRecord r : entry.getValue()) {
//					System.out.println(r);
//				}
//			}
//		} else {
//			System.out.println("no results!!!");
//		}
		
	}
	private static Map<String, List<BLATRecord>> updateMapWithName(Map<String, List<BLATRecord>> taResults, Map<String, List<String>> seqToNamesMap) {
		Map<String, List<BLATRecord>> map = new HashMap<>(taResults.size() * 2);
		for (Entry<String, List<BLATRecord>> entry : taResults.entrySet()) {
			List<String> names = seqToNamesMap.get(entry.getKey());
			if (entry.getValue().size() > 0) {
				for (String name : names) {
					map.put(name, entry.getValue());
				}
			}
		}
		return map;
	}

}
