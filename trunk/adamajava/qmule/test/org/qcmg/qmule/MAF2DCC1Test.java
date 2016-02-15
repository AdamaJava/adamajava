package org.qcmg.qmule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.tab.TabbedRecord;

public class MAF2DCC1Test {
	
	private File snpMafFile;
	private File snpDccFile;
	private File indelMafFile;
	private File indelDccFile;
	private File outputFile;
	private MAF2DCC1 test;
	private final String DCCHEADER = "analysis_id	analyzed_sample_id	mutation_id	mutation_type	chromosome	chromosome_start	chromosome_end	chromosome_strand	refsnp_allele	refsnp_strand	reference_genome_allele	control_genotype	tumour_genotype	mutation	expressed_allele	quality_score	probability	read_count	is_annotated	validation_status	validation_platform	xref_ensembl_var_id	note	QCMGflag	ND	TD	NNS	FlankSeq";
	private final String MAFHEADER = "Hugo_Symbol	Entrez_Gene_Id	Center	NCBI_Build	Chromosome	Start_Position	End_Position	Strand	Variant_Classification	Variant_Type	Reference_Allele	Tumor_Seq_Allele1	Tumor_Seq_Allele2	dbSNP_RS	dbSNP_Val_Status	Tumor_Sample_Barcode	Matched_Norm_Sample_Barcode	Match_Norm_Seq_Allele1	Match_Norm_Seq_Allele2	Tumor_Validation_Allele1	Tumor_Validation_Allele2	Match_Norm_Validation_Allele1	Match_Norm_Validation_Allele2	Verification_Status	Validation_Status	Mutation_Status	Sequencing_Phase	Sequence_Source	Validation_Method	Score	BAM_File	Sequencer	QCMG_Flag	ND	TD	Canonical_Transcript_Id	Canonical_AA_Change	Canonical_Base_Change	Alternate_Transcript_Id	Alternate_AA_Change	Alternate_Base_Change	Confidence	CPG	Gff3_Bait	Novel_Starts";
			
	private static String FILE_SEPARATOR = System.getProperty("file.separator");
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {
		snpMafFile = createMafFile("snp", testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "snp.maf");
		indelMafFile = createMafFile("indel", testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "indel.maf");
		snpDccFile = createDccFile("snp", testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "snp.dcc1");
		indelDccFile = createDccFile("indel", testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "indel.dcc1");
		outputFile = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.dcc1");
		String[] args = {"-i", indelMafFile.getAbsolutePath(), "-i", indelDccFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath(), "-mode", "indel", "-log", testFolder.newFile().getAbsolutePath()};
		
		test = new MAF2DCC1();
		test.setup(args);
	}
	
	@After
	public void tearDown() {
		snpDccFile.delete();
		snpMafFile.delete();
		indelMafFile.delete();
		indelDccFile.delete();
		outputFile.delete();
		snpDccFile = null;
		snpMafFile = null;
		indelDccFile = null;
		indelMafFile = null;
	}
	
	@Test
	public void testRunSnpMode() throws Exception {
		String[] args = {"-i", snpMafFile.getAbsolutePath(), "-i", snpDccFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath(), "-mode", "snp", "-log", testFolder.newFile().getAbsolutePath()};
		
		MAF2DCC1 test = new MAF2DCC1();
		test.setup(args);
		assertEquals(0, test.annotate());
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
		assertEquals(1, test.getInputMafRecordCount());
	}
	
	@Test
	public void testRunIndelMode() throws Exception {
		
		assertEquals(0, test.annotate());
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
		assertEquals(2, test.getInputMafRecordCount());
	}	
	
	@Test
	public void testGoodOptions() throws Exception {
		String log  = testFolder.newFile().getAbsolutePath();
		String[] args = {"-i", indelMafFile.getAbsolutePath(), "-i", indelDccFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath(), "-mode", "indel", "-log", log};
		MAF2DCC1 test = new MAF2DCC1();
		test.setup(args);
		assertEquals(indelMafFile, test.getMafFile());
		assertEquals(1, test.getDccFiles().size());
		assertEquals(indelDccFile, test.getDccFiles().get(0));
		assertEquals(outputFile, test.getOutputDccFile());
		assertEquals("indel", test.getMode());
		assertEquals(log, test.getLogFile());
	}
	
	@Test(expected=QMuleException.class)
	public void testOutputFileExistsThrowsException() throws Exception {
		String log  = testFolder.newFile().getAbsolutePath();
		outputFile = testFolder.newFile("test");
		String[] args = {"-i", indelMafFile.getAbsolutePath(), "-i", indelDccFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath(), "-mode", "indel", "-log", log};
		assertTrue(outputFile.exists());
		MAF2DCC1 test = new MAF2DCC1();
		test.setup(args);		
	}
	
	@Test(expected=QMuleException.class)
	public void testUnknownModeThrowsException() throws Exception {
		String[] args = {"-i", indelMafFile.getAbsolutePath(), "-i", indelDccFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath(), "-mode", "idel", "-log", testFolder.newFile().getAbsolutePath()};
		MAF2DCC1 test = new MAF2DCC1();
		test.setup(args);		
	}
	
	@Test
	public void testMatchChrPos() {
		ChrPosition maf = new ChrRangePosition("chr1", 1, 2);
		ChrPosition dcc = new ChrRangePosition("chr1", 1, 2);
		
		assertTrue(test.match(maf, dcc));
		dcc = new ChrRangePosition("chr1", 1, 3);
		assertFalse(test.match(maf, dcc));
		dcc = new ChrRangePosition("chr1", 2, 2);
		assertFalse(test.match(maf, dcc));
		dcc = new ChrRangePosition("chr1", 1, 1);
		assertFalse(test.match(maf, dcc));
	}
	
	@Test
	public void testMatchingMutation() {
		assertTrue(test.matchingMutation("SNP", "1"));
		assertTrue(test.matchingMutation("INS", "2"));
		assertTrue(test.matchingMutation("DEL", "3"));
		assertFalse(test.matchingMutation("SNP", "3"));
		assertFalse(test.matchingMutation("INS", "1"));
		assertFalse(test.matchingMutation("DEL", "2"));
	}
	
	@Test
	public void testMatchRecordsSnpMode() {
		int[] indexes = {0, 1, 2, 3, 4, 5};
		test.setMafColumnIndexes(indexes);
		test.setDccColumnIndexes(indexes);
		test.setMode("snp");
		TabbedRecord maf = new TabbedRecord();
		TabbedRecord dcc = new TabbedRecord();
		maf.setData("chr1\t1\t2\tSNP");
		dcc.setData("chr1\t1\t2\t1");
		assertTrue(test.matchOtherColumns(maf, dcc));
		dcc.setData("chr1\t1\t2\t4");
		assertFalse(test.matchOtherColumns(maf, dcc));
	}
	
	@Test
	public void testMatchRecordsIndelMode() {
		int[] indexes = {0, 1, 2, 3, 4, 5};
		test.setMafColumnIndexes(indexes);
		test.setDccColumnIndexes(indexes);
		test.setMode("indel");
		TabbedRecord maf = new TabbedRecord();
		TabbedRecord dcc = new TabbedRecord();
		maf.setData("chr1\t1\t2\tINS\t-\tA");
		dcc.setData("chr1\t1\t2\t2\t-\tA");
		assertTrue(test.matchOtherColumns(maf, dcc));
		dcc.setData("chr1\t1\t2\t1\t-\tA");
		assertFalse(test.matchOtherColumns(maf, dcc));
	}
	
	@Test
	public void testRecordInMaf() throws QMuleException {
		int[] indexes = {0, 1, 2, 3, 4, 5};
		test.setMafColumnIndexes(indexes);
		test.setDccColumnIndexes(indexes);
		test.setMode("indel");
		TabbedRecord maf = new TabbedRecord();
		TabbedRecord dcc = new TabbedRecord();
		maf.setData("chr1\t1\t2\tINS\t-\tA");
		dcc.setData("chr1\t1\t2\t2\t-\tA");
		List<TabbedRecord> listOfRecords = new ArrayList<>();
		listOfRecords.add(maf);
		Map<ChrPosition, List<TabbedRecord>> mafs = new HashMap<>();
		ChrPosition c = new ChrPositionName("chr1", 1, 2, "a");
		mafs.put(c, listOfRecords);
		test.setMafRecords(mafs);
		assertTrue(test.recordInMaf(c, dcc));		
	}
	
	@Test(expected=QMuleException.class)
	public void testRecordInMafThrowsException() throws QMuleException {
		int[] indexes = {0, 1, 2, 3, 4, 5};
		test.setMafColumnIndexes(indexes);
		test.setDccColumnIndexes(indexes);
		test.setMode("indel");
		TabbedRecord maf = new TabbedRecord();
		TabbedRecord dcc = new TabbedRecord();
		maf.setData("chr1\t1\t2\tINS\t-\tA");
		dcc.setData("chr1\t1\t2\t2\t-\tA");
		List<TabbedRecord> listOfRecords = new ArrayList<>();
		listOfRecords.add(maf);
		listOfRecords.add(maf);
//		List<TabbedRecord> listOfRecords2 = new ArrayList<>();
//		listOfRecords2.add(maf);
		Map<ChrPosition, List<TabbedRecord>> mafs = new HashMap<>();
		ChrPosition c = new ChrRangePosition("chr1", 1, 2);
//		ChrPosition c2 = new ChrPosition("chr1", 1, 2);
//		ChrPosition c = new ChrPosition("chr1", 1, 2, "a");
//		ChrPosition c2 = new ChrPosition("chr1", 1, 2, "b");
		mafs.put(c, listOfRecords);
//		mafs.put(c2, listOfRecords2);
		assertEquals(1, mafs.size());
		assertEquals(2, mafs.get( new ChrRangePosition("chr1", 1, 2)).size());
		test.setMafRecords(mafs);
		test.recordInMaf(c, dcc);
	}
	
	@Test
	public void testfindColumnIndexesFromHeaderWithMaf() {
		TabbedRecord rec = new TabbedRecord();
		rec.setData(MAFHEADER);
		int[] cols = test.findColumnIndexesFromHeader(rec);
		assertEquals(4, cols[0]);
		assertEquals(5, cols[1]);
		assertEquals(6, cols[2]);
		assertEquals(9, cols[3]);
		assertEquals(10, cols[4]);
		assertEquals(11, cols[5]);
	}
	
	@Test
	public void testfindColumnIndexesFromHeaderWithDcc() {
		TabbedRecord rec = new TabbedRecord();
		rec.setData(DCCHEADER);
		int[] cols = test.findColumnIndexesFromHeader(rec);
		assertEquals(4, cols[0]);
		assertEquals(5, cols[1]);
		assertEquals(6, cols[2]);
		assertEquals(3, cols[3]);
		assertEquals(10, cols[4]);
		assertEquals(12, cols[5]);
	}
	
	@Test(expected=QMuleException.class)
	public void testMissingColumnIndexThrowsException() throws QMuleException {
		int[] i = {-1};
		test.missingColumnIndex(i);		
	}
	
	@Test
	public void testMissingColumnIndex() throws QMuleException {
		int[] i = {1};
		assertFalse(test.missingColumnIndex(i));		
	}
	
	@Test
	public void testAddRecordToMap() throws QMuleException {
		int[] indexes = {0, 1, 2, 3, 4, 5};
		test.setMafColumnIndexes(indexes);
		test.setDccColumnIndexes(indexes);
		test.setMode("indel");
		TabbedRecord maf = new TabbedRecord();
		maf.setData("chr1\t1\t2\tINS\t-\tA");
		test.addToMafRecordMap(maf, 1);
		assertEquals(1, test.getMafRecords().size());
		assertTrue(test.getMafRecords().containsKey(new ChrRangePosition("1", 1, 2)));
//		assertTrue(test.getMafRecords().containsKey(new ChrPosition("1", 1, 2, "" + 1)));
		maf = new TabbedRecord();
		maf.setData("chr1\t1\t2\tINS\t-\tA");
		test.addToMafRecordMap(maf, 2);
		assertEquals(1, test.getMafRecords().size());
		assertEquals(2, test.getMafRecords().get(new ChrRangePosition("1", 1, 2)).size());
//		assertEquals(2, test.getMafRecords().size());
		assertTrue(test.getMafRecords().containsKey(new ChrRangePosition("1", 1, 2)));
//		assertTrue(test.getMafRecords().containsKey(new ChrPosition("1", 1, 2, "" +2)));
	}

	
	private File createDccFile(String type, String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));
		//w.write("analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq\n");
		w.write(DCCHEADER  + "\n");
		if (type.equals("indel")) {
			w.write("aba9fc0c_7f03_417f_b087_2e8ab1a45e42	test	test_ind716	2	chr1	4412134	4412135	1	-999	-999	-0	-999	A	-/A		-999	-999	-999	-999	-999	-999	-999	-999	PASS	--	--	--	--\n");
			w.write("aba9fc0c_7f03_417f_b087_2e8ab1a45e42	test	test_ind2740	3	chr1	12126362	12126362	1	-999	-999	T	-999	-0	T/-		-999	-999	-999	-999	-999	-999	-999	-999	PASS	--	--	--	--\n");
		}
		
		if (type.equals("snp")) {
			w.write("02ebc0c3_3102_4bf0_9c5b_eabcab65414d	ICGC-ABMJ-20120706-01	APGI_1992_SNP_248	1	1	569492	569492	1	C/T	1	T	T/T	C/T	T>C	-999	-999	0.0119695263	106	1	2	-888	rs147253560	-999	MIN	A:1[35],0[0],C:0[0],1[37],T:42[36.71],47[35.89]	C:9[31.33],1[36],T:49[36.61],46[33.41]	5	ATCCCCATACT\n");
			w.write("02ebc0c3_3102_4bf0_9c5b_eabcab65414d	ICGC-ABMJ-20120706-01	APGI_1992_SNP_260	1	1	604271	604271	1	-888	-888	G	G/G	A/G	G>A	-999	-999	0.3973437368	56	2	2	-888	-999	-999	MIN;MR;GERM	A:0[0],1[29],G:20[34.1],19[35.79]	A:2[37.5],2[32],G:30[36.43],22[38]	4	TGGAGAGGAAC");
			}

		w.close();
		return new File(fileName);
	}

	private File createMafFile(String type, String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));
		w.write(MAFHEADER + "\n");	
		if (type.equals("indel")) {
			w.write("Unknown	null	qcmg.uq.edu.au	37	1	4412134	4412135	0	null	INS	-0	A	A	novel	null	QCMG-66-APGI_1992-ICGC-ABMJ-20120706-01	QCMG-66-APGI_1992-ICGC-ABMP-20091203-10-ND	-0	-0	null	null	null	null	null	Unknown	Somatic	null	Unknown	null	null	null	Unknown	PASS;HOMCON_4	0;28;28;0;0;0;0	9;52;52;9;0;0;0;\"4	contiguous	CTAAAAACACaAAAATTAGCT\"	null	null	null	null	null	null	HIGH	--	null	0\n");
			w.write("TNFRSF8	0	qcmg.uq.edu.au	37	1	12126362	12126362	0	Intron	DEL	T	-0	-0	novel	null	QCMG-66-APGI_1992-ICGC-ABMJ-20120706-01	QCMG-66-APGI_1992-ICGC-ABMP-20091203-10-ND	-0	-0	null	null	null	null	null	Unknown	Somatic	null	Unknown	null	null	null	Unknown	PASS;HOMCON_3	0;67;66;0;0;2;0	15;52;49;16;0;1;0;\"3	contiguous	AAGCTCGTTA_TTTAAAAAAA\"	ENST00000263932	-888	-888	null	null	null	HIGH	--	fill\n");
		}
		
		if (type.equals("snp")) {
			w.write("Unknown	0	qcmg.uq.edu.au	37	1	569492	569492	0	RNA	SNP	T	C	T	rs147253560	null	QCMG-66-APGI_1992-ICGC-ABMJ-20120706-01	QCMG-66-APGI_1992-ICGC-ABMP-20091203-10-ND	T	T	null	null	null	null	null	Unknown	Somatic	null	Unknown	null	null	null	Unknown	PASS	A:1[35],0[0],C:0[0],1[37],T:42[36.71],47[35.89]	C:9[31.33],1[36],T:49[36.61],46[33.41]	ENST00000440200	-888	-888	null	null	null	HIGH	ATCCCCATACT	fill\n");																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						
		}

		w.close();
		return new File(fileName);
	}
}
