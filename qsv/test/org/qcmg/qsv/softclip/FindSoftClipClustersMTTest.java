package org.qcmg.qsv.softclip;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;

import gnu.trove.map.hash.TIntObjectHashMap;

public class FindSoftClipClustersMTTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void doesUnaryOperatorWorkWithObjects() {		// yes but is it efficient???
		Integer INT_OBJECT = Integer.valueOf(1);
		INT_OBJECT--;
		assertEquals(0, INT_OBJECT.intValue());
	}
	
	@Test
	public void getProperClipSVs() throws Exception {
		File tumorBam = TestUtil.createHiseqBamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);

        QSVParameters tumor = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true,  "clip");
        QSVParameters normal = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), false,  "clip"); 
        tumor.setReference("file");
        normal.setReference("file");
        Options options = createMock(Options.class);
		expect(options.isSplitRead()).andReturn(false);
		expect(options.isQCMG()).andReturn(true);
		expect(options.singleSided()).andReturn(false);
		expect(options.getConsensusLength()).andReturn(20);	
		expect(options.getClipSize()).andReturn(3);	
		expect(options.getMinInsertSize()).andReturn(50);	
		expect(options.getReference()).andReturn("file");
		expect(options.getAnalysisMode()).andReturn("both");
		expect(options.getPlatform()).andReturn("illumina");
		expect(options.getIncludeTranslocations()).andReturn(true);
		expect(options.allChromosomes()).andReturn(true);
		expect(options.getGffFiles()).andReturn(new ArrayList<String>());
		replay(options);    
        
        File softClipDir = testFolder.newFolder("softclip");
		
		writeSoftClipFiles(softClipDir);		
		
		Map<PairGroup, Map<String,List<DiscordantPairCluster>>> tumorClusterRecords = new HashMap<>();
		Map<String,List<DiscordantPairCluster>> map = new HashMap<>();
		List<DiscordantPairCluster> list = new ArrayList<>();
		list.add(TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.getRoot(), "chr7", "chr7"));
		map.put("chr10:chr10", list);
		tumorClusterRecords.put(PairGroup.AAC, map);
		
		BLAT blat = createMock(BLAT.class);
        Map<String, BLATRecord> expected = new HashMap<>();
        String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10_89712341_true_+\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
        String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10_89700299_false_-\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
        
        expected.put("chr10_89712341_true_+", new BLATRecord.Builder(value.split("\t")).build());
        expected.put("chr10_89700299_false_-", new BLATRecord.Builder(value2.split("\t")).build());
        expect(blat.align(softClipDir.getAbsolutePath() + QSVUtil.getFileSeparator() + ("TD_breakpoint.chr10.fa"), softClipDir.getAbsolutePath() + QSVUtil.getFileSeparator() + ("TD_breakpoint.chr10.psl"))).andReturn(expected);
       
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr7-140189108-chr7-140191044", "TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA", "chr7", "chr7")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr7-140189108-chr7-140191044", "TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA", "chr7", "chr7")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr10-89700299-chr10-89712341", "TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA", "chr10", "chr10")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr10-89700299-chr10-89712341", "TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA", "chr10", "chr10")).andReturn(new ArrayList<BLATRecord>());
        
        replay(blat);
        FindClipClustersMT worker = new FindClipClustersMT(tumor, normal, softClipDir.getAbsolutePath(), tumorClusterRecords,  options, "analysisId", 200, new TIntObjectHashMap<int[]>());
		String key = "";
		List<SoftClipCluster> clusters = new ArrayList<>();
		List<SoftClipCluster> results = worker.getProperClipSVs(key, clusters);
		assertEquals(0, results.size());
	}
		

	@Test
	public void testClipClusterWorker() throws Exception {
		File tumorBam = TestUtil.createHiseqBamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
        QSVParameters tumor = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true,  "clip");
        QSVParameters normal = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), false,  "clip"); 
        tumor.setReference("file");
        normal.setReference("file");
        Options options = createMock(Options.class);
		expect(options.isSplitRead()).andReturn(false);
		expect(options.isQCMG()).andReturn(true);
		expect(options.singleSided()).andReturn(false);
		expect(options.getConsensusLength()).andReturn(20);	
		expect(options.getClipSize()).andReturn(3);	
		expect(options.getMinInsertSize()).andReturn(50);	
		expect(options.getReference()).andReturn("file");
		expect(options.getAnalysisMode()).andReturn("both");
		expect(options.getPlatform()).andReturn("illumina");
		expect(options.getIncludeTranslocations()).andReturn(true);
		expect(options.allChromosomes()).andReturn(true);
		expect(options.getGffFiles()).andReturn(new ArrayList<String>());
		replay(options);    
        
        File softClipDir = testFolder.newFolder("softclip");
		writeSoftClipFiles(softClipDir);		
		
		Map<PairGroup, Map<String,List<DiscordantPairCluster>>> tumorClusterRecords = new HashMap<>();
		Map<String,List<DiscordantPairCluster>> map = new HashMap<>();
		List<DiscordantPairCluster> list = new ArrayList<DiscordantPairCluster>();
		list.add(TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.getRoot(), "chr7", "chr7"));
		map.put("chr10:chr10", list);
		tumorClusterRecords.put(PairGroup.AAC, map);
		
		BLAT blat = createMock(BLAT.class);
        Map<String, BLATRecord> expected = new HashMap<>();
        String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10_89712341_true_+\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
        String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10_89700299_false_-\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
        
        expected.put("chr10_89712341_true_+", new BLATRecord.Builder(value.split("\t")).build());
        expected.put("chr10_89700299_false_-", new BLATRecord.Builder(value2.split("\t")).build());
        expect(blat.align(softClipDir.getAbsolutePath() + QSVUtil.getFileSeparator() + ("TD_breakpoint.chr10.fa"), softClipDir.getAbsolutePath() + QSVUtil.getFileSeparator() + ("TD_breakpoint.chr10.psl"))).andReturn(expected);
       
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr7-140189108-chr7-140191044", "TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA", "chr7", "chr7")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr7-140189108-chr7-140191044", "TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA", "chr7", "chr7")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr10-89700299-chr10-89712341", "TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA", "chr10", "chr10")).andReturn(new ArrayList<BLATRecord>());
        expect(blat.alignConsensus(softClipDir.getAbsolutePath(), "splitcon-chr10-89700299-chr10-89712341", "TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA", "chr10", "chr10")).andReturn(new ArrayList<BLATRecord>());
        
        replay(blat);
		
		FindClipClustersMT worker = new FindClipClustersMT(tumor, normal, softClipDir.getAbsolutePath(), tumorClusterRecords,  options, "analysisId", 200, new TIntObjectHashMap<int[]>());
        worker.execute();
        
        assertEquals(0, worker.getQSVRecordWriter().getGermlineCount().intValue());
	}

	private void writeSoftClipFiles(File softClipDir) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(softClipDir + QSVUtil.getFileSeparator() + "TD.chr10.clip")))) {
			writer.write("HWI-ST1240:47:D12NAACXX:6:1213:16584:89700:20120608110941621,chr10,89700299,-,right,TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGG,TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:4:2208:21187:49506:20120608092353631,chr10,89700299,-,right,GTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAG,GTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:1:2312:16545:35061:20120608115535190,chr10,89700299,-,right,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:8:1115:9658:72817:20120608020343585,chr10,89700299,-,right,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT,TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:5:2308:21263:96155:20120607102754932,chr10,89712341,+,left,AGTCATATAATCTCTTTGTGTAAGAGATTTTACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA,AGTCATATAATCTCTTTGTGTAAGAGATTTTACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906:20120607102754932,chr10,89712341,+,left,TCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA,TCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:5:2313:2867:30879:20120607102754932,chr10,89712341,+,left,AGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA,AGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:6:1112:4715:61725:20120608110941621,chr10,89712341,+,left,TCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATA,TCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATA\n");
			writer.write("HWI-ST1240:47:D12NAACXX:6:2115:13249:50856:20120608110941621,chr10,89712341,+,left,ATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTC,ATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA,AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTC\n");
		}
	}
}
