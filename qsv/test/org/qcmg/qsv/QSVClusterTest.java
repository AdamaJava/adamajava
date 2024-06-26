package org.qcmg.qsv;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.softclip.Breakpoint;
import org.qcmg.qsv.softclip.Clip;
import org.qcmg.qsv.softclip.SoftClipCluster;
import org.qcmg.qsv.splitread.SplitReadContig;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;

public class QSVClusterTest {
	
	private QSVCluster record;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
    @Test
    public void testFindGermline() throws IOException, Exception {
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "germline", testFolder.newFolder(), "chr7", "chr7", true, false);    	
    	record.findGermline();    	
    	assertTrue(record.isGermline());
    	
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", testFolder.newFolder(), "chr7", "chr7", false, false);    	
    	record.findGermline();    	
    	assertFalse(record.isGermline());
    }
    
    @Test 
    public void testFindClipOverlap() throws IOException, Exception {
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "germline", testFolder.newFolder(), "chr10", "chr10", true, false);    	
    	assertEquals(1, record.getClipRecords().size());
    	assertTrue(record.findClipOverlap(TestUtil.setUpClipRecord("chr10", "chr10", false, false)));
    	assertEquals(2, record.getClipRecords().size());
    }
    
    @Test
    public void testFindClusterOverlapIsFalse() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.newFolder(), "chr7", "chr7");
    	record = new QSVCluster(cluster, false,  "id");
    	boolean test = record.findClusterOverlap(TestUtil.setUpClipRecord("chr10", "chr10", false, false));
    	assertFalse(test);
    }
    
    @Test
    public void testFindClusterOverlapIsTrue() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	record = new QSVCluster(cluster, false,  "id");
    	boolean test = record.findClusterOverlap(TestUtil.setUpClipRecord("chr10", "chr10", false, false));
    	assertTrue(test);
    }
    
    @Test
    public void testGetOverlapWithCategory1() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	record = new QSVCluster(cluster, false,  "id"); 	
//    	89700049
//    	89700300
//    	89712340
//    	89712546
    	//cat1
    	assertFalse(record.getOverlap(true, 89700465));
    	assertTrue(record.getOverlap(true, 89700265));
    	assertFalse(record.getOverlap(false, 89700265));
    	assertTrue(record.getOverlap(false, 89712345));
    }
    
    @Test
    public void testGetOverlapWithCategory2() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "2");
    	record = new QSVCluster(cluster, false,  "id");
    	assertFalse(record.getOverlap(true, 89700265));
    	assertTrue(record.getOverlap(true, 89700065));
    	assertFalse(record.getOverlap(false, 89700265));
    	assertTrue(record.getOverlap(false, 89712545));
    }
    
    @Test
    public void testGetOverlapWithCategory3() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "3");
    	record = new QSVCluster(cluster, false,  "id");
    	assertFalse(record.getOverlap(true, 89700465));
    	assertTrue(record.getOverlap(true, 89700265));
    	assertFalse(record.getOverlap(false, 89700265));
    	assertTrue(record.getOverlap(false, 89712545));    
    }
    
    @Test
    public void testGetOverlapWithCategory4() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "4");
    	record = new QSVCluster(cluster, false,  "id");
    	assertFalse(record.getOverlap(true, 89700265));
    	assertTrue(record.getOverlap(true, 89700065));
    	assertFalse(record.getOverlap(false, 89700265));
    	assertTrue(record.getOverlap(false, 89712345));
    }
    
    @Test
    public void testGetGermlineRatio() throws IOException, Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	MatePair m =cluster.getClusterMatePairs().getFirst();
    	record = new QSVCluster(cluster, false,  "id");
    	
    	assertFalse(record.getPotentialGermline());
    	record.getPairRecord().setLowConfidenceNormalMatePairs(1);
    	assertTrue(record.getPotentialGermline());
    }
    
    @Test
    public void testGetDataString() throws Exception {
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	record = new QSVCluster(cluster, false,  "id");
    	record.addQSVClipRecord(TestUtil.setUpClipRecord("chr10", "chr10", false, false));
    	record.setIdParameters("sv1", "test", "testsample");
    	assertEquals(37, record.getDataString("dcc", "TD", "ND", true).split("\t").length);
    	//assertEquals(8,record.getDataString("vcf", "TD", "ND", true).split("\t").length);
    	assertEquals(20,record.getDataString("tab", "TD", "ND", true).split("\t").length);
    	assertEquals(1,record.getDataString("verbose", "TD", "ND", true).split("\t").length);
    	assertEquals(5,record.getDataString("qprimer", "TD", "ND", true).split("\t").length);
    	assertEquals(23,record.getDataString("softclip", "TD", "ND", true).split("\t").length);
    }
    
    @Test
    public void getOverlap() {
    	/*
    	 * must be within 100 of each other
    	 */
    	assertEquals(true, QSVCluster.getOverlap(1, 10));
    	assertEquals(true, QSVCluster.getOverlap(10, 1));
    	assertEquals(true, QSVCluster.getOverlap(10, -1));
    	assertEquals(false, QSVCluster.getOverlap(-1, -1));
    	assertEquals(true, QSVCluster.getOverlap(100, 100));
    	assertEquals(false, QSVCluster.getOverlap(10000, 100));
    	assertEquals(true, QSVCluster.getOverlap(1, 101));
    	assertEquals(false, QSVCluster.getOverlap(1, 102));
    	assertEquals(true, QSVCluster.getOverlap(202, 102));
    	assertEquals(false, QSVCluster.getOverlap(203, 102));
    }
    
    @Test
    public void getClipWithHighestClipCount() throws Exception {
    	/*
    	 * setup clips
    	 */
    	SoftClipCluster clip1 = TestUtil.setUpClipRecord("chr10", "chr10", false, false);
    	Breakpoint b = TestUtil.getBreakpoint(true, true, 10, false);
    	String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10-89700299-false-neg\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
		b.setMateBreakpoint(89712341);
		b.setMateReference("chr10");
		b.setMateStrand(QSVUtil.MINUS);
		b.setBlatRecord(new BLATRecord.Builder(value2.split("\t")).build());
		Collection<Clip> clips = TestUtil.getRightClips(false);
		for (Clip c : clips) {
			b.addNormalClip(c);
		}
    	SoftClipCluster clip2 = new SoftClipCluster(b);
    	assertEquals(11, clip2.getClipCount(false, true));
    	
    	/*
    	 * clip1 has more
    	 */
    	assertEquals(11, clip1.getClipCount(true, true) + clip1.getClipCount(true, false));
    	assertEquals(5, clip2.getClipCount(true, true) + clip2.getClipCount(true, false));
    	
    	assertEquals(clip1, QSVCluster.getSoftClipClusterWithHighestClipCount(Arrays.asList(clip2, clip1)));
    	assertEquals(clip1, QSVCluster.getSoftClipClusterWithHighestClipCount(Arrays.asList(clip1, clip2)));
    }
    
    @Test
    public void testConfidenceLevel() throws IOException, Exception {
    	//3
    	DiscordantPairCluster cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1"); 	
    	record = new QSVCluster(cluster, false,  "id");
    	assertEquals(QSVConstants.LEVEL_LOW, record.getConfidenceLevel());
    	
    	//5
    	record.getPairRecord().setLowConfidenceNormalMatePairs(1);
    	assertEquals(QSVConstants.LEVEL_GERMLINE, record.getConfidenceLevel());    	
    	
    	//1
    	cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	record = new QSVCluster(cluster, false,  "id");
    	SoftClipCluster clip = TestUtil.setUpClipRecord("chr10", "chr10", false, false);
    	record.findClusterOverlap(clip);
    	SplitReadContig contig = createMock(SplitReadContig.class);
    	expect(contig.getIsPotentialSplitRead()).andReturn(true);
    	replay(contig);
    	record.setSplitReadContig(contig);
    	assertEquals("1", record.getConfidenceLevel());  
    	
    	//2    	
    	cluster = TestUtil.setupHiseqCluster("somatic", testFolder.newFolder(), "1");
    	record = new QSVCluster(cluster, false,  "id");
    	record.findClusterOverlap(TestUtil.setUpClipRecord("chr10", "chr10", false, true));
    	assertEquals(QSVConstants.LEVEL_LOW, record.getConfidenceLevel());  
    	
    	//4
    	record = new QSVCluster(TestUtil.setUpClipRecord("chr10", "chr10", false, false), "test");
    	assertEquals(QSVConstants.LEVEL_LOW, record.getConfidenceLevel());
    	
    	//6
    	record = new QSVCluster(TestUtil.setUpClipRecord("chr10", "chr10", false, true), "test");
    	assertEquals(QSVConstants.LEVEL_SINGLE_CLIP, record.getConfidenceLevel());
    }
    
    
    @Test
    public void getCurrentFlankSeqUsingFile() throws IOException, QSVException {
    	
		File refFile = testFolder.newFile("blah.fa");
		File indexFile = testFolder.newFile("blah.fa.fai");
		setupReferenceFile(refFile, indexFile);
		QSVUtil.setupReferenceMap(refFile);
		ConcurrentMap<String, byte[]> referenceMap = QSVUtil.getReferenceMap();
		String reference = "chr25";
		byte[] bases = referenceMap.get(reference);
		String basesString = new String(bases);
		
    	int breakpoint = 200;
    	List<Chromosome> chrs = new ArrayList<>();
    	chrs.add(new Chromosome("chr25", bases.length));
    	
//	    	IndexedFastaSequenceFile isff = new IndexedFastaSequenceFile(refFile);
    	String fs = QSVCluster.getCurrentFlankSeq(referenceMap, reference, breakpoint, chrs);
    	assertEquals(400, fs.length());
    	assertEquals(true, basesString.startsWith(fs));
    	
    	breakpoint = 201;
    	fs = QSVCluster.getCurrentFlankSeq(referenceMap, reference, breakpoint, chrs);
    	assertEquals(401, fs.length());
    	assertEquals(true, basesString.startsWith(fs));
    	
    	breakpoint = 202;
    	fs = QSVCluster.getCurrentFlankSeq(referenceMap, reference, breakpoint, chrs);
    	assertEquals(401, fs.length());
    	assertEquals(false, basesString.startsWith(fs));
    	
    	breakpoint = 203;
    	fs = QSVCluster.getCurrentFlankSeq(referenceMap, reference, breakpoint, chrs);
    	assertEquals(401, fs.length());
    	assertEquals(false, basesString.startsWith(fs));
    	
    	breakpoint = 199;
    	fs = QSVCluster.getCurrentFlankSeq(referenceMap, reference, breakpoint, chrs);
    	assertEquals(399, fs.length());
    	assertEquals(true, basesString.startsWith(fs));
    }
    
    private void setupReferenceFile(File file, File indexFile) throws IOException {
	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(">chr25\n");
			writer.write("GATCACAGGTCTATCACCCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGG\n");
			writer.write("GTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTC\n");
			writer.write("CTGCCTCATCCTATTATTTATCGCACCTACGTTCAATATTACAGGCGAACATACTTACTAAAGTGTGTTA\n");
			writer.write("ATTAATTAATGCTTGTAGGACATAATAATAACAATTGAATGTCTGCACAGCCACTTTCCACACAGACATC\n");
			writer.write("ATAACAAAAAATTTCCACCAAACCCCCCCTCCCCCGCTTCTGGCCACAGCACTTAAACACATCTCTGCCA\n");
			writer.write("AACCCCAAAAACAAAGAACCCTAACACCAGCCTAACCAGATTTCAAATTTTATCTTTTGGCGGTATGCAC\n");
			writer.write("TTTTAACAGTCACCCCCCAACTAACACATTATTTTCCCCTCCCACTCCCATACTACTAATCTCATCAATA\n");
			writer.write("CAACCCCCGCCCATCCTACCCAGCACACACACACCGCTGCTAACCCCATACCCCGAACCAACCAAACCCC\n");
			writer.write("AAAGACACCCCCCACAGTTTATGTAGCTTACCTCCTCAAAGCAATACACTGAAAATGTTTAGACGGGCTC\n");
			writer.write("ACATCACCCCATAAACAAATAGGTTTGGTCCTAGCCTTTCTATTAGCTCTTAGTAAGATTACACATGCAA\n");
			writer.write("GCATCCCCGTTCCAGTGAGTTCACCCTCTAAATCACCACGATCAAAAGGAACAAGCATCAAGCACGCAGC\n");
			writer.write("AATGCAGCTCAAAACGCTTAGCCTAGCCACACCCCCACGGGAAACAGCAGTGATTAACCTTTAGCAATAA\n");
			writer.write("ACGAAAGTTTAACTAAGCTATACTAACCCCAGGGTTGGTCAATTTCGTGCCAGCCACCGCGGTCACACGA\n");
			writer.write("TTAACCCAAGTCAATAGAAGCCGGCGTAAAGAGTGTTTTAGATCACCCCCTCCCCAATAAAGCTAAAACT\n");
			writer.write("CACCTGAGTTGTAAAAAACTCCAGTTGACACAAAATAGACTACGAAAGTGGCTTTAACATATCTGAACAC\n");
			writer.write("ACAATAGCTAAGACCCAAACTGGGATTAGATACCCCACTATGCTTAGCCCTAAACCTCAACAGTTAAATC\n");
			writer.write("AACAAAACTGCTCGCCAGAACACTACGAGCCACAGCTTAAAACTCAAAGGACCTGGCGGTGCTTCATATC\n");
			writer.write("CCTCTAGAGGAGCCTGTTCTGTAATCGATAAACCCCGATCAACCTCACCACCTCTTGCTCAGCCTATATA\n");
			writer.write("CCGCCATCTTCAGCAAACCCTGATGAAGGCTACAAAGTAAGCGCAAGTACCCACGTAAAGACGTTAGGTC\n");
			writer.write("AAGGTGTAGCCCATGAGGTGGCAAGAAATGGGCTACATTTTCTACCCCAGAAAACTACGATAGCCCTTAT\n");
			writer.write("GAAACTTAAGGGTCGAAGGTGGATTTAGCAGTAAACTAAGAGTAGAGTGCTTAGTTGAACAGGGCCCTGA\n");
			writer.write("AGCGCGTACACACCGCCCGTCACCCTCCTCAAGTATACTTCAAAGGACATTTAACTAAAACCCCTACGCA\n");
			writer.write("TTTATATAGAGGAGACAAGTCGTAACATGGTAAGTGTACTGGAAAGTGCACTTGGACGAACCAGAGTGTA\n");
			writer.write("GCTTAACACAAAGCACCCAACTTACACTTAGGAGATTTCAACTTAACTTGACCGCTCTGAGCTAAACCTA\n");
			writer.write("GCCCCAAACCCACTCCACCTTACTACCAGACAACCTTAGCCAAACCATTTACCCAAATAAAGTATAGGCG\n");
			writer.write("ATAGAAATTGAAACCTGGCGCAATAGATATAGTACCGCAAGGGAAAGATGAAAAATTATAACCAAGCATA\n");
			writer.write("ATATAGCAAGGACTAACCCCTATACCTTCTGCATAATGAATTAACTAGAAATAACTTTGCAAGGAGAGCC\n");
			writer.write("AAAGCTAAGACCCCCGAAACCAGACGAGCTACCTAAGAACAGCTAAAAGAGCACACCCGTCTATGTAGCA\n");
			writer.write("AAATAGTGGGAAGATTTATAGGTAGAGGCGACAAACCTACCGAGCCTGGTGATAGCTGGTTGTCCAAGAT\n");
			writer.write("AGAATCTTAGTTCAACTTTAAATTTGCCCACAGAACCCTCTAAATCCCCTTGTAAATTTAACTGTTAGTC\n");
			writer.write("CAAAGAGGAACAGCTCTTTGGACACTAGGAAAAAACCTTGTAGAGAGAGTAAAAAATTTAACACCCATAG\n");
			writer.write("TAGGCCTAAAAGCAGCCACCAATTAAGAAAGCGTTCAAGCTCAACACCCACTACCTAAAAAATCCCAAAC\n");
			writer.write("ATATAACTGAACTCCTCACACCCAATTGGACCAATCTATCACCCTATAGAAGAACTAATGTTAGTATAAG\n");
			writer.write("TAACATGAAAACATTCTCCTCCGCATAAGCCTGCGTCAGATTAAAACACTGAACTGACAATTAACAGCCC\n");
			writer.write("AATATCTACAATCAACCAACAAGTCATTATTACCCTCACTGTCAACCCAACACAGGCATGCTCATAAGGA\n");
			writer.write("AAGGTTAAAAAAAGTAAAAGGAACTCGGCAAATCTTACCCCGCCTGTTTACCAAAAACATCACCTCTAGC\n");
			writer.write("ATCACCAGTATTAGAGGCACCGCCTGCCCAGTGACACATGTTTAACGGCCGCGGTACCCTAACCGTGCAA\n");
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
			writer.write("chr25\t16571\t7\t50\t51\n");
		}
    }
	
}
