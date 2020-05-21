package org.qcmg.qsv.tiledaligner;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

public class ExaminePSLResults {
	
	
	public static Map<String, List<BLATRecord>> loadBlatResultsFromFile(String psl) throws IOException {
		List<String> blatRecords = Files.lines(new File(psl).toPath()).collect(Collectors.toList());
		
		/*
		 * convert to map of name, list of blat records
		 */
		return blatRecords.stream().map(s -> new BLATRecord(s.split("\t"))).collect(Collectors.groupingBy(BLATRecord::getName));
	}
	
	public static Map<String, List<String>> getSequencesAndNamesFromFile(String input) {
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
				if (sequences.get(i + 1).length() >= 20) {
					if (sequences.get(i).substring(1).startsWith("chr7_57160469_false_+")) {
						System.out.println("adding to map chr7_57160469_false_+ in input file");
					}
					map.computeIfAbsent(sequences.get(i + 1), f -> new ArrayList<>(2)).add(sequences.get(i).substring(1));
				} else {
					System.out.println("Sequence is less than 20 in length: " + sequences.get(i + 1).length() + ", " + sequences.get(i + 1));
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
	
	
	public static void main(String[] args) throws IOException {
		/*
		 * First argument should be the tiled aligner file
		 * Second argument(s) should be the sequences
		 */
		String pslFile = args[0];
		String faFile = args[1];
		Map<String, List<String>> seqNameMap = getSequencesAndNamesFromFile(faFile);
		Map<String, String> nameSeqMap = new HashMap<>(seqNameMap.size() * 2);
		for (Entry<String, List<String>> entry : seqNameMap.entrySet()) {
			for (String name : entry.getValue()) {
				nameSeqMap.put(name, entry.getKey());
			}
		}
		
		Map<String, List<BLATRecord>> pslResults = loadBlatResultsFromFile(pslFile);
		
		int [] splitconBlockCount = new int[20];
		int [] splitconClipBlockCount = new int[20];
		int [] regularBlockCount = new int[20];
		int splitconChrMatch = 0;
		int splitconCount = 0;
		int splitconClipCount = 0;
		int splitconClipChrMatch = 0;
		int regularCount = 0;
		int regularChrMatch = 0;
		int [] splitconBlockCountO1 = new int[20];
		int [] splitconClipBlockCountO1 = new int[20];
		int [] regularBlockCountO1 = new int[20];
		int splitconChrMatchO1 = 0;
		int splitconCountO1 = 0;
		int splitconClipCountO1 = 0;
		int splitconClipChrMatchO1 = 0;
		int regularCountO1 = 0;
		int regularChrMatchO1 = 0;
		
		for (Entry<String, List<BLATRecord>> entry : pslResults.entrySet()) {
			String name = entry.getKey();
			int seqLength = nameSeqMap.get(name).length();
			List<BLATRecord> recs = entry.getValue();
			recs.sort(null);
			
			
			
			
			BLATRecord bestRec = recs.get(recs.size() - 1);
			
			
			
			if (name.endsWith("clip") && name.startsWith("splitcon")) {
				if (seqLength <= 100) {
					splitconClipCount++;
					splitconClipBlockCount[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						splitconClipChrMatch++;
					}
				} else {
					splitconClipCountO1++;
					splitconClipBlockCountO1[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						splitconClipChrMatchO1++;
					}
				}
			} else if (name.startsWith("splitcon")) {
				if (seqLength <= 100) {
					splitconCount++;
					splitconBlockCount[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						splitconChrMatch++;
					}
				} else {
					splitconCountO1++;
					splitconBlockCountO1[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						splitconChrMatchO1++;
					}
					
				}
			} else if (name.startsWith("chr") || name.startsWith("GL")) {
				if (seqLength <= 100) {
					regularCount++;
					regularBlockCount[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						regularChrMatch++;
					}
				} else {
					regularCountO1++;
					regularBlockCountO1[bestRec.getBlockCount()]++;
					if (name.contains(bestRec.getReference())) {
						regularChrMatchO1++;
					}
					
				}
			} else {
				System.out.println("unknown name type: " + name);
			}
		}
		
		System.out.println("splitconCount: " + splitconCount + ", splitconChrMatch: " + splitconChrMatch + ", bc: " + Arrays.toString(splitconBlockCount));
		System.out.println("splitconCountO1: " + splitconCountO1 + ", splitconChrMatchO1: " + splitconChrMatchO1 + ", bc: " + Arrays.toString(splitconBlockCountO1));
		System.out.println("splitconClipCount: " + splitconClipCount + ", splitconClipChrMatch: " + splitconClipChrMatch + ", bc: " + Arrays.toString(splitconClipBlockCount));
		System.out.println("splitconClipCountO1: " + splitconClipCountO1 + ", splitconClipChrMatchO1: " + splitconClipChrMatchO1 + ", bc: " + Arrays.toString(splitconClipBlockCountO1));
		System.out.println("regularCount: " + regularCount + ", regularChrMatch: " + regularChrMatch + ", bc: " + Arrays.toString(regularBlockCount));
		System.out.println("regularCountO1: " + regularCountO1 + ", regularChrMatch: " + regularChrMatchO1 + ", bc: " + Arrays.toString(regularBlockCountO1));
		
	}

}
