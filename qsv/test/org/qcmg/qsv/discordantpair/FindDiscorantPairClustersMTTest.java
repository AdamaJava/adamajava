package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.report.SVCountReport;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.TestUtil;

public class FindDiscorantPairClustersMTTest {

	private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
	private  File tumorBam;
	private  File normalBam;
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
		if (null == tumorBam) {
			tumorBam = TestUtil.createBamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		}
		if (null == normalBam) {
			normalBam = TestUtil.createBamFile(testFolder.newFile("normal.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		}
		tumor = TestUtil.getQSVParameters(testFolder.getRoot(), normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "pair");
		normal = TestUtil.getQSVParameters(testFolder.getRoot(), normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), false, "pair"); 
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
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_TD_AAC");
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_ND_AAC");
		findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "TD");
		compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "ND");
		//germline

		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader,
				compareReader, tumor, normal, countReport, query, true);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
		executorService.shutdown();
		countDownLatch.await();        
		int germline = f.get().get("germline").size();
		assertEquals(1, germline);
	}

	@Test
	public void testRunTumorWithSomatic() throws InterruptedException, ExecutionException, IOException {
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_TD_AAC");
		findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "TD");
		compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "ND");
		//germline
		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader,
				compareReader, tumor, normal, countReport, query, true);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<Map<String, List<DiscordantPairCluster>>> f = executorService.submit(findClusters);
		executorService.shutdown();
		countDownLatch.await();        
		int germline = f.get().get("somatic").size();
		assertEquals(1, germline);
	}

	@Test
	public void testRunNormal() throws InterruptedException, ExecutionException, IOException {
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_ND_AAC");
		findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "TD");
		compareReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "ND");
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
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.Cxx, "chr4-chr15_xxx_test_TD_AAC");
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.Cxx, "chr4-chr15_xxx_test_ND_AAC");

		findReader = new MatePairsReader(PairGroup.valueOf("Cxx"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "TD");
		compareReader = new MatePairsReader(PairGroup.valueOf("Cxx"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "ND");
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
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_TD_AAC");
		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, "", true);
		findReader = new MatePairsReader(PairGroup.valueOf("AAC"), matePairDir.getAbsolutePath() + FILE_SEPARATOR, "test", "TD");
		List<MatePair> pairs = TestUtil.readInMatePairs(new File(matePairDir.getAbsolutePath() + FILE_SEPARATOR + "AAC" + FILE_SEPARATOR + "chr7_xxx_test_TD_AAC"));

		assertEquals(6, pairs.size());
		List<DiscordantPairCluster> list = findClusters.findClusters(pairs);

		assertEquals(1, list.size());
		assertEquals(6, list.getFirst().getClusterMatePairs().size());
	}

	@Test
	public void testClassifyGermlineCluster() throws IOException, Exception {
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_ND_AAC");
		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, "", true); 
		List<MatePair> pairs = TestUtil.readInMatePairs(new File(matePairDir.getAbsolutePath() + FILE_SEPARATOR + "AAC" + FILE_SEPARATOR + "chr7_xxx_test_ND_AAC"));
		findClusters.classifyClusters(Arrays.asList(TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.getRoot(), "chr7", "chr7")), pairs);

		assertEquals(1, findClusters.getClustersMap().get("germline").size());	
	}

	@Test
	public void testClassifySomaticCluster() throws IOException, Exception {
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_ND_AAC");
		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, findReader, compareReader, tumor, normal, countReport, query, true); 

		findClusters.classifyClusters(Arrays.asList(TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.getRoot(), "chr7", "chr7")), Arrays.asList(new MatePair("722_126_792:20110412030837875,chr4,100,200,Cxx,129,false,722_126_792:20110412030837875,chr15,300,400,Cxx,65,false,F2F1\n")));
		assertEquals(1, findClusters.getClustersMap().get("somatic").size());	
	}

	@Test
	public void testClassifyNormalGermlineCluster() throws IOException, Exception {
		TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPARATOR, PairClassification.AAC, "chr7_xxx_test_ND_AAC");
		findClusters = new FindDiscordantPairClustersMT(PairGroup.AAC, countDownLatch, compareReader, findReader, normal, tumor, countReport, query, true); 

		findClusters.classifyClusters(Arrays.asList(TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder.getRoot(), "chr7", "chr7")), Collections.emptyList());

		assertEquals(1, findClusters.getClustersMap().get("normal-germline").size());	
	}

	@Ignore // hard coded paths....
	public void findDiscordantPairsABB() throws Exception {
		String pathToABBDir = "/Users/oliverh/development/sf/adamajava/qsv/bin/resources/";
		PairGroup zp = PairGroup.ABB;
		String pairQuery = "and(Cigar_M > 35, MD_mismatch < 3, Flag_DuplicateRead == false)";
		MatePairsReader normalReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.CONTROL_SAMPLE);
		MatePairsReader tumourReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.DISEASE_SAMPLE);
		
		normal.setUpperInsertSize(1010);
		tumor.setUpperInsertSize(1037);
		
		Callable<Map<String, List<DiscordantPairCluster>>> normalWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, normalReader, tumourReader, normal, tumor, countReport, pairQuery, true);
		Callable<Map<String, List<DiscordantPairCluster>>> tumourWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, tumourReader, normalReader, tumor, normal, countReport, pairQuery, true);
		
		Map<String,List<DiscordantPairCluster>> nClusters = normalWorker.call();
		Map<String,List<DiscordantPairCluster>> tClusters = tumourWorker.call();
		
		System.out.println("no of nClusters: " + nClusters.size());
		System.out.println("no of tClusters: " + tClusters.size());
		
		for (Entry<String, List<DiscordantPairCluster>> entry : nClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
		}
		for (Entry<String, List<DiscordantPairCluster>> entry : tClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
		}
	}
	
	@Test // too slow for daily use - but still very useful.....
	public void findDiscordantPairsAAC() throws Exception {
		String pathToABBDir = "/Users/oliverh/development/sf/adamajava/qsv/bin/resources/";
		PairGroup zp = PairGroup.AAC;
		String pairQuery = "and(Cigar_M > 35, MD_mismatch < 3, Flag_DuplicateRead == false)";
		MatePairsReader normalReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.CONTROL_SAMPLE);
		MatePairsReader tumourReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.DISEASE_SAMPLE);
		
		normal.setUpperInsertSize(1010);
		tumor.setUpperInsertSize(1037);
		
		Callable<Map<String, List<DiscordantPairCluster>>> normalWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, normalReader, tumourReader, normal, tumor, countReport, pairQuery, true);
		Callable<Map<String, List<DiscordantPairCluster>>> tumourWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, tumourReader, normalReader, tumor, normal, countReport, pairQuery, true);
		
		Map<String,List<DiscordantPairCluster>> nClusters = normalWorker.call();
		Map<String,List<DiscordantPairCluster>> tClusters = tumourWorker.call();
		
		System.out.println("no of nClusters: " + nClusters.size());
		System.out.println("no of tClusters: " + tClusters.size());
		
		for (Entry<String, List<DiscordantPairCluster>> entry : nClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
			
			switch (entry.getKey()) {
			case "normal-germline": assertEquals(1976, entry.getValue().size()); break;
			case "germline": assertEquals(2162, entry.getValue().size()); break;
			case "somatic": assertEquals(412, entry.getValue().size()); break;
			}
		}
		for (Entry<String, List<DiscordantPairCluster>> entry : tClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
			switch (entry.getKey()) {
			case "normal-germline": assertEquals(1976, entry.getValue().size()); break;
			case "germline": assertEquals(2162, entry.getValue().size()); break;
			case "somatic": assertEquals(412, entry.getValue().size()); break;
			}
		}
		
	}
	@Ignore	// too slow for daily use - but still very useful.....
	public void findDiscordantPairsABA() throws Exception {
		String pathToABBDir = "/Users/oliverh/development/sf/adamajava/qsv/bin/resources/";
		PairGroup zp = PairGroup.ABA;
		String pairQuery = "and(Cigar_M > 35, MD_mismatch < 3, Flag_DuplicateRead == false)";
		MatePairsReader normalReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.CONTROL_SAMPLE);
		MatePairsReader tumourReader = new MatePairsReader(zp, pathToABBDir, "OESO_0384", QSVConstants.DISEASE_SAMPLE);
		
		normal.setUpperInsertSize(1010);
		tumor.setUpperInsertSize(1037);
		
		Callable<Map<String, List<DiscordantPairCluster>>> normalWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, normalReader, tumourReader, normal, tumor, countReport, pairQuery, true);
		Callable<Map<String, List<DiscordantPairCluster>>> tumourWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, tumourReader, normalReader, tumor, normal, countReport, pairQuery, true);
		
		Map<String,List<DiscordantPairCluster>> nClusters = normalWorker.call();
		Map<String,List<DiscordantPairCluster>> tClusters = tumourWorker.call();
		
		System.out.println("no of nClusters: " + nClusters.size());
		System.out.println("no of tClusters: " + tClusters.size());
		
		for (Entry<String, List<DiscordantPairCluster>> entry : nClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
		}
		for (Entry<String, List<DiscordantPairCluster>> entry : tClusters.entrySet()) {
			System.out.println("entry key: " + entry.getKey() + ", no of DiscordantPairClusters: " + entry.getValue().size());
		}
	}

}
