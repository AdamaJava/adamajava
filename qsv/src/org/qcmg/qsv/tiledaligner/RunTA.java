package org.qcmg.qsv.tiledaligner;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.qsv.blat.BLATRecord;

public class RunTA {
	
	public static final String TILED_ALIGNER_FILE = "/reference/genomeinfo/q3clinvar/q3tiledaligner.out.gz";
	
	public static void main(String[] args) {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument should the the fa file containing the sequences to be blatted, or in this case, tile-aligned
		 * Third argument should the the psl results file containing the blat records from BLAT
		 */
		String tiledAlignerFile = args[0];
		String faFile = args[1];
		String pslSinlgeResultsFile = args[2];
		String pslMultipleResultsFile = args[3];
		String output = args[4];
		
		int bufferSize = args.length > 5 ? Integer.parseInt(args[5]) : 64;
		int numberOfSequencesToRunWith = args.length > 6 ? Integer.parseInt(args[6]) : 10000;
		
		/*
		 * get the sequences from the faFile
		 */
		
		try  {
			
			List<String> sequences = Files.lines(new File(faFile).toPath()).collect(Collectors.toList());
			List<String> blatRecordsSinlgeResults = Files.lines(new File(pslSinlgeResultsFile).toPath()).collect(Collectors.toList());
			List<String> blatRecordsMultipleResults = Files.lines(new File(pslMultipleResultsFile).toPath()).collect(Collectors.toList());
			
			
			Map<String, BLATRecord> blatMap = new HashMap<>();
			for (String r : blatRecordsSinlgeResults) {
				String [] array = r.split("\t");
				/*
				 * first column is score, which we don't want
				 */
				BLATRecord br = new BLATRecord(Arrays.copyOfRange(array, 1, array.length));
				blatMap.put(br.getName(), br);
			}
			System.out.println("Number of entries in blatMap after adding singles: " + blatMap.size());
			for (String r : blatRecordsMultipleResults) {
				String [] array = r.split("\t");
				/*
				 * first column is score, which we don't want
				 */
				BLATRecord br = new BLATRecord(Arrays.copyOfRange(array, 1, array.length));
				blatMap.put(br.getName(), br);
			}
			System.out.println("Number of entries in blatMap after adding multiples: " + blatMap.size());
			
			Map<String, String> nameToSequenceMap = new HashMap<>();
			for (int i = 0, len = sequences.size() ; i < len ; i += 2) {
				if (sequences.get(i).startsWith(">") && ! StringUtils.isNullOrEmpty(sequences.get(i + 1)) && ! "null".equals(sequences.get(i + 1))) {
					nameToSequenceMap.put(sequences.get(i).substring(1, sequences.get(i).length()), sequences.get(i + 1));
				} else {
					/*
					 * just print out a few of the problem positions
					 */
					if (i < 200) {
						System.out.println("expecting a name, got a sequence!!! i: " + i + ", sequences.get(i):" + sequences.get(i));
					}
				}
			}
			
			List<String> sequencesToTA = new ArrayList<>(nameToSequenceMap.values());
			
//			for (String name : blatMap.keySet()) {
//				String seq = nameToSequenceMap.get(name);
////				System.out.println("looking for " + name + ", found: " + seq);
//				if (null != seq) {
//					sequencesToTA.add(seq);
//				}
//			}
			
			
			
//			System.out.println("Found " + sequences.size() + " sequences in the fa file");
			System.out.println("Found " + nameToSequenceMap.size() + " sequences in the fa file");
//			System.out.println("Found " + sequencesToTA.size() + " sequences in the fa file that have blat results!");
			
			
			String [] sequencesArray = sequencesToTA.toArray(new String[] {});
			
			if (numberOfSequencesToRunWith > sequencesArray.length) {
				numberOfSequencesToRunWith = sequencesArray.length;
			}
			
			String [] sequencesArraySubSet = Arrays.copyOfRange(sequencesArray, 0, numberOfSequencesToRunWith);
			
			long start = System.currentTimeMillis();
			System.out.println("call TiledAligner with " + sequencesArraySubSet.length + " sequences");
			int nCount = 0;
			for (String s : sequencesArraySubSet) {
				if (s.indexOf('N') > -1) nCount++;
			}
			System.out.println("number of sequences with N's: " + nCount);
			
//			System.exit(1);
			
			
//			 Map<String, BLATRecord> tiledAlignerMap = TiledAlignerUtil.runTiledAlignerCache(tiledAlignerFile, sequencesArraySubSet, 13, bufferSize);
			 Map<String, List<BLATRecord>> tiledAlignerMap = TiledAlignerUtil.runTiledAlignerCache(tiledAlignerFile, sequencesArraySubSet, 13, bufferSize, output);
//			 Map<String, BLATRecord> tiledAlignerMap = TiledAlignerUtil.runTiledAligner(tiledAlignerFile, sequencesArraySubSet, 13);
			 System.out.println("map size: " + tiledAlignerMap.size());
			 
			 
			 int noBlatRecord = 0;
			 int match = 0;
			 int partialMatch = 0;
			 int noMatch = 0, noMatchNCount = 0;
			 int noMatchTABestScore = 0, noMatchBLATBestScore = 0, noMatchEqualScore = 0;
			 
			 for (Entry<String, List<BLATRecord>> entry : tiledAlignerMap.entrySet()) {
				 String name = "";
				 
				 for (Entry<String, String> entry2 : nameToSequenceMap.entrySet()) {
					 if (entry2.getValue().equals(entry.getKey())) {
						 name = entry2.getKey();
					 }
				 }
//				 System.out.println("seq: " + entry.getKey() + ", name: " + name + ", TA: " + entry.getValue());
				 /*
				  * check to see what kind of match we have!
				  */
				 
				 List<BLATRecord> blatRecs = entry.getValue();
				 if (null != blatRecs && ! blatRecs.isEmpty()) {
					 
					 /*
					  * get best tiledAligned record - the last one in this list
					  */
					 BLATRecord bestTARecord = blatRecs.get(blatRecs.size() - 1);
					 
					 ChrPosition tiledAlignerCP = new ChrRangePosition(bestTARecord.getReference(), bestTARecord.getStartPos(), bestTARecord.getEndPos());
					 BLATRecord blatResult = blatMap.get(name);
					 if (null != blatResult) {
						 ChrPosition blatCP = new ChrRangePosition(blatResult.getReference(), blatResult.getStartPos(), blatResult.getEndPos());
						 
						 if (ChrPositionUtils.doChrPositionsOverlap(tiledAlignerCP, blatCP)) {
							 if (tiledAlignerCP.equals(blatCP)) {
								 match++;
							 } else {
								 partialMatch++;
							 }
						 } else {
							 noMatch++;
							 if (blatResult.getScore() == bestTARecord.getScore()) {
								 noMatchEqualScore++;
							 } else if (blatResult.getScore() > bestTARecord.getScore()) {
								 noMatchBLATBestScore++;
							 } else {
								 noMatchTABestScore++;
							 }
							 System.out.println("seq: " + entry.getKey() + ", name: " + name + ", \nTA: " + bestTARecord + ", \nblat: " + blatResult);
						 }
					 } else {
						 noBlatRecord++;
					 }
				 }
			 }
			
			 System.out.println("Matches: " + match + ", overlaps: " + partialMatch + ", noMatch: " + noMatch + ", noBlatRecord: " + noBlatRecord);
			 System.out.println("No Matches: noMatchTABestScore: " + noMatchTABestScore + ", noMatchBLATBestScore: " + noMatchBLATBestScore + ", noMatchEqualScore: " + noMatchEqualScore);
			 System.out.println("call TiledAligner - DONE! time taken: " + (System.currentTimeMillis() - start));
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
