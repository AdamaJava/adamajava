package org.qcmg.qsv.softclip;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;

public class FindClipClustersMTTest {
	
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getClipPositions() throws QSVException {
		Map<Integer, Breakpoint> leftPositions = new HashMap<>();
		Map<Integer, Breakpoint> rightPositions = new HashMap<>();
		Map<Integer, List<UnmappedRead>> splitReads = new HashMap<>();
		
		List<String> clips = getClipData();
		FindClipClustersMT.getClipPositions(true, leftPositions, rightPositions, splitReads, clips, new Chromosome("chr10", 100000000), true,  20, 50);
		assertEquals(1, leftPositions.size());
		assertEquals(1, rightPositions.size());
		assertEquals(0, splitReads.size());
		assertEquals(5, leftPositions.get(89712341).getClipsSize());
		assertEquals(5, rightPositions.get(89700299).getClipsSize());	// duplicate entry in the list
		
		/*
		 * reset and add split read
		 */
		leftPositions = new HashMap<>();
		rightPositions = new HashMap<>();
		splitReads = new HashMap<>();
		
		clips.add("unmapped,readname,chr10,12345,AAAACCCCGGGGTTTT");
		
		FindClipClustersMT.getClipPositions(true, leftPositions, rightPositions, splitReads, clips, new Chromosome("chr10", 100000000), true,  20, 50);
		assertEquals(1, leftPositions.size());
		assertEquals(1, rightPositions.size());
		assertEquals(1, splitReads.size());
		
		for (Entry<Integer, Breakpoint> entry : leftPositions.entrySet()) {
			System.out.println("leftPositions key: " + entry.getKey() + ", value: " + entry.getValue());
		}
		for (Entry<Integer, Breakpoint> entry : rightPositions.entrySet()) {
			System.out.println("rightPositions key: " + entry.getKey() + ", value: " + entry.getValue());
		}
		
		assertEquals(5, leftPositions.get(89712341).getClipsSize());
		assertEquals(5, rightPositions.get(89700299).getClipsSize());	// duplicate entry in the list
		assertEquals(1, splitReads.get(12345).size());
	}
	
	@Test
	public void amalgamateBreakpoints() throws QSVException {
		Map<Integer, Breakpoint> leftPositions = new HashMap<>();
		Map<Integer, Breakpoint> rightPositions = new HashMap<>();
		TreeMap<Integer, List<UnmappedRead>> splitReads = new TreeMap<>();
		
		List<String> clips = getClipData();
		clips.add("unmapped,readname,chr10,89700600,AAAACCCCGGGGTTTT");
		clips.add("unmapped,readname,chr10,89700601,AAACCCCGGGGTTTT");
		clips.add("unmapped,readname,chr10,89700602,AACCCCGGGGTTTT");
		
		clips.add("unmapped,readname,chr10,89712641,AAAACCCCGGGGTTTT");
		clips.add("unmapped,readname,chr10,89712642,AAACCCCGGGGTTTT");
		clips.add("unmapped,readname,chr10,89712643,AACCCCGGGGTTTT");
		
		Chromosome c = new Chromosome("chr10", 100000000);
		FindClipClustersMT.getClipPositions(true, leftPositions, rightPositions, splitReads, clips, c, true,  20, 50);
		
		List<Breakpoint> amalgamatedBPs = FindClipClustersMT.amalgamateBreakpoints(500, c, leftPositions, rightPositions, splitReads);
		assertEquals(2, amalgamatedBPs.size());
		assertEquals(0, amalgamatedBPs.get(0).getSplitReadsSize());
		assertEquals(0, amalgamatedBPs.get(1).getSplitReadsSize());
	}
	
	@Test
	public void loadClipsFromFile() throws IOException {
		File folder = testFolder.newFolder();
		File clipFile = writeSoftClipFiles(folder);
		List<String> data = FindClipClustersMT.loadClipDataFromFile(clipFile);
		assertEquals(11, data.size());
		assertEquals(true, data.get(0).startsWith("HWI-ST1240:47:D12NAACXX:6:1213:16584:89700:20120608110941621"));
		assertEquals(true, data.get(10).startsWith("HWI-ST1240:47:D12NAACXX:6:2115:13249:50856:20120608110941621"));
	}
	
	
	private File writeSoftClipFiles(File softClipDir) throws IOException {
		File clipFile = new File(softClipDir + QSVUtil.getFileSeparator() + "TD.chr10.clip");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(clipFile));) {
			for (String s : getClipData()) {
				writer.write(s + "\n");
			}
		}
		return clipFile;
	}
	
	private List<String> getClipData() {
		List<String> l = new ArrayList<>(12);
		l.add("HWI-ST1240:47:D12NAACXX:6:1213:16584:89700:20120608110941621,chr10,89700299,-,right,TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGG,TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:4:2208:21187:49506:20120608092353631,chr10,89700299,-,right,GTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAG,GTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:1:2312:16545:35061:20120608115535190,chr10,89700299,-,right,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:8:1115:9658:72817:20120608020343585,chr10,89700299,-,right,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA");
		l.add("HWI-ST1240:47:D12NAACXX:5:2308:21263:96155:20120607102754932,chr10,89712341,+,left,AGTCATATAATCTCTTTGTGTAAGAGATTTTACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA,AGTCATATAATCTCTTTGTGTAAGAGATTTTACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA");
		l.add("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906:20120607102754932,chr10,89712341,+,left,TCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA,TCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA");
		l.add("HWI-ST1240:47:D12NAACXX:5:2313:2867:30879:20120607102754932,chr10,89712341,+,left,AGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA,AGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA");
		l.add("HWI-ST1240:47:D12NAACXX:6:1112:4715:61725:20120608110941621,chr10,89712341,+,left,TCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATA,TCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATA");
		l.add("HWI-ST1240:47:D12NAACXX:6:2115:13249:50856:20120608110941621,chr10,89712341,+,left,ATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTC,ATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTC");
		return l;
	}

}
