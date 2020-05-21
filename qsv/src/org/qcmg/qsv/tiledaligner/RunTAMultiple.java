package org.qcmg.qsv.tiledaligner;

import static org.qcmg.common.util.Constants.FILE_SEPARATOR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class RunTAMultiple {
	
	public static final String TILED_ALIGNER_FILE = "/reference/genomeinfo/q3clinvar/q3tiledaligner.out.gz";
	
	public static void main(String[] args) {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument should the the fa file containing the sequences to be blatted, or in this case, tile-aligned
		 * Third argument should the the psl results file containing the blat records from BLAT
		 */
		String tiledAlignerFile = args[0];
		String faFile = args[1];
		String pslFile = args[2];
		
		int numberOfSequencesToRunWith = args.length > 3 ? Integer.parseInt(args[3]) : 50;
		
		/*
		 * get the sequences from the faFile
		 */
		
		try  {
			
			List<String> sequences = Files.lines(new File(faFile).toPath()).collect(Collectors.toList());
			List<String> blatRecords = Files.lines(new File(pslFile).toPath()).collect(Collectors.toList());
			
			
			Map<String, List<BLATRecord>> blatMap = new HashMap<>();
			for (String r : blatRecords) {
				String [] array = r.split("\t");
				/*
				 * first column is score, which we don't want
				 */
				BLATRecord br = new BLATRecord(Arrays.copyOfRange(array, 1, array.length));
				blatMap.computeIfAbsent(br.getName(), f -> new ArrayList<>()).add(br);
			}
			System.out.println("Number of entries in blatMap: " + blatMap.size());
			
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
			
			List<String> sequencesToTA = new ArrayList<>(blatMap.size() + 1);
			
			for (String name : blatMap.keySet()) {
				if ( ! blatMap.get(name).isEmpty()) {
					String seq = nameToSequenceMap.get(name);
//					System.out.println("looking for " + name + ", found: " + seq);
					if (null != seq) {
						sequencesToTA.add(seq);
					}
				}
			}
			
			
			
//			System.out.println("Found " + sequences.size() + " sequences in the fa file");
			System.out.println("Found " + sequencesToTA.size() + " sequences in the fa file");
			
			String [] sequencesArray = sequencesToTA.toArray(new String[] {});
			
			String [] sequencesArraySubSet = Arrays.copyOfRange(sequencesArray, 0, numberOfSequencesToRunWith);
			
			long start = System.currentTimeMillis();
			System.out.println("call TiledAligner with " + sequencesArraySubSet.length + " sequences");
			
			
			 Map<String, BLATRecord> tiledAlignerMap = TiledAlignerUtil.runTiledAligner(tiledAlignerFile, sequencesArraySubSet, 13);
			 System.out.println("map size: " + tiledAlignerMap.size());
			 
			 
			 int noBlatRecord = 0;
			 int match = 0;
			 int matchInList = 0;
			 int partialMatch = 0;
			 int noMatch = 0, noMatchNCount = 0;
			 int noMatchTABestScore = 0, noMatchBLATBestScore = 0, noMatchEqualScore = 0;
			 
			 for (Entry<String, BLATRecord> entry : tiledAlignerMap.entrySet()) {
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
				 ChrPosition tiledAlignerCP = new ChrRangePosition(entry.getValue().getReference(),entry.getValue().getStartPos(), entry.getValue().getEndPos());
				 List<BLATRecord> blatResults = blatMap.get(name);
				 if (null != blatResults && ! blatResults.isEmpty()) {
					 blatResults.sort(null);
					 System.out.println("Found " + blatResults.size() + " blat results for " + name);
					 BLATRecord b = blatResults.get(0);
					 ChrPosition blatCP = new ChrRangePosition(b.getReference(), b.getStartPos(), b.getEndPos());
					 if (ChrPositionUtils.doChrPositionsOverlap(tiledAlignerCP, blatCP)) {
						 if (tiledAlignerCP.equals(blatCP)) {
							 match++;
						 } else {
							 partialMatch++;
						 }
					 } else {
//						 noMatch++;
						 
						 System.out.println("No exact match - looking in list");
						 boolean foundInList = false;
						 for (BLATRecord br : blatResults) {
							 ChrPosition bcp = new ChrRangePosition(br.getReference(), br.getStartPos(), br.getEndPos());
							 
							 if (ChrPositionUtils.doChrPositionsOverlap(tiledAlignerCP, bcp)) {
								 System.out.println("Found match in list!");
								 foundInList = true;
								 matchInList++;
								 System.out.println("seq: " + entry.getKey() + ", name: " + name + ", \nTA: " + entry.getValue() + ", \nblat: " + br);
							 }
						 }
						 
						 if ( ! foundInList) {
							 noMatch++;
							 System.out.println("No Match!: ");
							 for (BLATRecord br : blatResults) {
								 System.out.println("seq: " + entry.getKey() + ", name: " + name + ", \nTA: " + entry.getValue() + ", \nblat: " + br);
							 }
						 }
						 
						 
//						 if (b.getScore() == entry.getValue().getScore()) {
//							 noMatchEqualScore++;
//						 } else if (b.getScore() > entry.getValue().getScore()) {
//							 noMatchBLATBestScore++;
//						 } else {
//							 noMatchTABestScore++;
//						 }
//						 System.out.println("seq: " + entry.getKey() + ", name: " + name + ", \nTA: " + entry.getValue() + ", \nblat: " + b);
					 }
					 
				 } else {
					 noBlatRecord++;
				 }
			 }
			
			 System.out.println("Matches: " + match + ", overlaps: " + partialMatch + ", noMatch: " + noMatch + ", noBlatRecord: " + noBlatRecord + ", matchInList: " + matchInList);
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
