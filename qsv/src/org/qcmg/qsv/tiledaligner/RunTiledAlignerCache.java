package org.qcmg.qsv.tiledaligner;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.string.StringFileReader;

import gnu.trove.map.TIntObjectMap;
import htsjdk.samtools.util.SequenceUtil;

public class RunTiledAlignerCache {
	
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
	
	public static void main(String[] args) throws IOException {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument(s) should be the sequences
		 */
		String tiledAlignerFile = args[0];
		String [] sequences = Arrays.copyOfRange(args, 1, args.length);
		System.out.println("sequences: " + Arrays.deepToString(sequences));
		
		/*
		 * convert to map
		 */
		Map<String, String> sequenceNameMap = new HashMap<>();
		int i = 0;
		String key = "";
		for (String s : sequences) {
			if (i % 2 ==0) {
				key = s;
			} else {
				/*
				 * got value - add to map
				 */
				sequenceNameMap.put(key, s);
			}
			i++;
		}
		
		
		/*
		 * load cache
		 */
		Instant start = Instant.now();
		TIntObjectMap<int[]> cache = TiledAlignerLongMap.getCache(tiledAlignerFile, 64);
		Instant end = Instant.now();
		Duration timeElapsed = Duration.between(start, end);
		System.out.println("Time taken: "+ timeElapsed.toMillis() +" milliseconds");
		
		System.out.println("About to run runTiledAlignerCache with " + Arrays.deepToString(sequences));
		Map<String, List<BLATRecord>> results = TiledAlignerUtil.runTiledAlignerCache(cache, sequenceNameMap, 13, "RunTiledAlignerCache.main", true, true);
		System.out.println("About to run runTiledAlignerCache - DONE");
		
		/*
		 * print out
		 */
		if (null != results && ! results.isEmpty()) {
			for (Entry<String, List<BLATRecord>> entry : results.entrySet()) {
				System.out.println("for sequence: " + entry.getKey() + ", " + entry.getValue().size() + " BLATRecords");
				for (BLATRecord r : entry.getValue()) {
					System.out.println(r);
				}
			}
		} else {
			System.out.println("no results!!!");
		}
		
	}

}
