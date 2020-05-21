package org.qcmg.qsv.tiledaligner;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.util.Constants;
import org.qcmg.string.StringFileReader;

import htsjdk.samtools.util.SequenceUtil;

public class CreateSubCache {
	
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
	
	public static Map<String, String> createMapFromSequences(String [] sequences, String strand) {
		boolean forwardStrand = "fs".equalsIgnoreCase(strand) || "both".equalsIgnoreCase(strand);
		boolean reverseStrand = "rs".equalsIgnoreCase(strand)|| "both".equalsIgnoreCase(strand);
		Map<String, String> results = new HashMap<>();
		for (String s : sequences) {
			String rc = SequenceUtil.reverseComplement(s);
			int length = s.length();
			for (int i = 0 ; i < length - TILE_LENGTH + 1 ; i++) {
				if (forwardStrand) {
					String tile = s.substring(i, i + TILE_LENGTH);
					if (tile.indexOf('N') > -1) {
						List<String> alternativeTiles = TiledAlignerUtil.getAlternativeSequences(tile);
						for (String altTile : alternativeTiles) {
							results.put(altTile, null);
						}
					} else {
						results.put(tile, null);
					}
				}
				if (reverseStrand) {
					String tileRc = rc.substring(i, i + TILE_LENGTH);
					if (tileRc.indexOf('N') > -1) {
						List<String> alternativeTiles = TiledAlignerUtil.getAlternativeSequences(tileRc);
						for (String altTile : alternativeTiles) {
							results.put(altTile, null);
						}
					} else {
						results.put(tileRc, null);
					}
				}
			}
		}
		System.out.println("Created map with " + results.size() + " entries in createMapFromSequences");
		return results;
	}
	
	public static void main(String[] args) throws IOException {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument should be the desired output file
		 * Third argument(s) should be the sequences
		 */
		String tiledAlignerFile = args[0];
		String output = args[1];
		String strand = args[2];
		String [] sequences = Arrays.copyOfRange(args, 3, args.length);
		
		System.out.println("Will attempt to find " + Arrays.deepToString(sequences) + " sequences in cache");
		/*
		 * get the tiles to search the tiledAligner for
		 */
		Map<String, String> sequenceTiles = createMapFromSequences(sequences, strand);
		
		/*
		 * run through the tiled aligner file looking for the sequenceTiles - update map in place if found
		 */
		getEntriesFromTiledAligner(tiledAlignerFile, sequenceTiles);
		
		/*
		 * write output
		 */
		writeToFile(output, sequenceTiles);
		
	}

}
