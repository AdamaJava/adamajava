package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.report.SVCountReport;
import org.qcmg.qsv.util.TestUtil;

public class FindDiscorantPairClustersMTTest {

    private static final String FILE_SEPERATOR = System.getProperty("file.separator");
    private File tumorBam;
    private File normalBam;
    private MatePairsReader findReader;
    private MatePairsReader compareReader;
    private FindDiscordantPairClustersMT findClusters = null;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private QSVParameters tumor = null;
    private QSVParameters normal = null;
	private File matePairDir;
	private SVCountReport countReport;
	private final String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	

    @Before
    public void setUp() throws Exception {
    	tumorBam = TestUtil.createBamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
        normalBam = TestUtil.createBamFile(testFolder.newFile("normal.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);

        tumor = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "pair");
        normal = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), false, "pair"); 
        matePairDir = testFolder.newFolder("matepair"); 
        countReport = new SVCountReport(testFolder.newFile("report"), "test");
    }

    @After
    public void tearDown() {
        findClusters = null;
        tumor = null;
        normal = null;
        findReader = null;
        compareReader = null;
        countReport = null;
    }

    @Test
    public void testRunTumorWithGermline() throws InterruptedException, ExecutionException, IOException {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_TD_AAC");
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_ND_AAC");
        findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "TD");
        compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "ND");
        //germline
        
        findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader,
                compareReader, tumor, normal, countReport, query, true);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
        executorService.shutdown();
        countDownLatch.await();        
        Integer germline = f.get().get("germline").size();
        assertEquals(new Integer(1), germline);
    }
    
    @Test
    public void testRunTumorWithSomatic() throws InterruptedException, ExecutionException, IOException {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_TD_AAC");
        findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "TD");
        compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "ND");
        //germline
        findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader,
                compareReader, tumor, normal, countReport, query, true);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
        executorService.shutdown();
        countDownLatch.await();        
        Integer germline = f.get().get("somatic").size();
        assertEquals(new Integer(1), germline);
    }

    @Test
    public void testRunNormal() throws InterruptedException, ExecutionException, IOException {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_ND_AAC");
    	findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "TD");
        compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "ND");
        findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, compareReader,
                findReader, normal, tumor, countReport, query, true);
       
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
        executorService.shutdown();
        countDownLatch.await();
    
        assertEquals(1, f.get().get("normal-germline").size());
    }

    @Test
    public void testFindCxxClusters() throws Exception {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.Cxx, "chr4-chr15_test_TD_AAC");
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.Cxx, "chr4-chr15_test_ND_AAC");
        
    	findReader = new MatePairsReader(PairGroup.valueOf("Cxx"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "TD");
        compareReader = new MatePairsReader(PairGroup.valueOf("Cxx"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "ND");
        findClusters = new FindDiscordantPairClustersMT(PairGroup.Cxx,
                countDownLatch, findReader,
                compareReader, tumor,normal, countReport, query, true);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
        executorService.shutdown();
        countDownLatch.await();

        assertEquals(1, f.get().get("germline").size());
    }

    @Test
    public void testFindClusters() throws Exception {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_TD_AAC");
        findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, "", true);
        findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPERATOR, "test", "TD");
        List<MatePair> pairs = TestUtil.readInMatePairs(new File(matePairDir.getAbsolutePath() + FILE_SEPERATOR + "AAC" + FILE_SEPERATOR + "chr7_test_TD_AAC"));
        
        assertEquals(6, pairs.size());
        List<DiscordantPairCluster> list = findClusters.findClusters(pairs);

        assertEquals(1, list.size());
        assertEquals(6, list.get(0).getClusterMatePairs().size());
    }
    
    @Test
    public void testClassifyGermlineCluster() throws IOException, Exception {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_ND_AAC");
    	findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, "", true); 
    	List<MatePair> pairs = TestUtil.readInMatePairs(new File(matePairDir.getAbsolutePath() + FILE_SEPERATOR + "AAC" + FILE_SEPERATOR + "chr7_test_ND_AAC"));
    	List<DiscordantPairCluster> records = new ArrayList<DiscordantPairCluster>();
    	DiscordantPairCluster cluster = TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder, "chr7", "chr7");
    	records.add(cluster);
    	findClusters.classifyClusters(records, pairs, "type");
    	
    	assertEquals(1, findClusters.getClustersMap().get("germline").size());	
    }
    
    @Test
    public void testClassifySomaticCluster() throws IOException, Exception {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_ND_AAC");
    	findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, query, true); 
    	List<MatePair> pairs = TestUtil.readInMatePairs(new File(matePairDir.getAbsolutePath() + FILE_SEPERATOR + "AAC" + FILE_SEPERATOR + "chr7_test_ND_AAC"));
    	
    	List<MatePair> pairs2 = new ArrayList<MatePair>();
//    	MatePair p = pairs.get(0);
    	MatePair mp = new MatePair("722_126_792:20110412030837875,chr4,100,200,Cxx,129,false,722_126_792:20110412030837875,chr15,300,400,Cxx,65,false,F2F1\n");
//    	p.getLeftMate().setStart(100);
//    	p.getLeftMate().setEnd(200);
//    	p.getRightMate().setStart(300);
//    	p.getRightMate().setEnd(400);
//    	pairs2.add(p);
    	pairs2.add(mp);
    	
    	List<DiscordantPairCluster> records = new ArrayList<DiscordantPairCluster>();
    	DiscordantPairCluster cluster = TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder, "chr7", "chr7");
    	records.add(cluster);
    	findClusters.classifyClusters(records, pairs2, "type");
    	
    	assertEquals(1, findClusters.getClustersMap().get("somatic").size());	
    }
    
    @Test
    public void testClassifyNormalGermlineCluster() throws IOException, Exception {
    	TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, "chr7_test_ND_AAC");
    	findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, compareReader, findReader, normal, tumor, countReport, query, true); 

    	List<DiscordantPairCluster> records = new ArrayList<DiscordantPairCluster>();
    	DiscordantPairCluster cluster = TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder, "chr7", "chr7");
    	records.add(cluster);
    	findClusters.classifyClusters(records, new ArrayList<MatePair>(), "type");
    	
    	assertEquals(1, findClusters.getClustersMap().get("normal-germline").size());	
    }

}
