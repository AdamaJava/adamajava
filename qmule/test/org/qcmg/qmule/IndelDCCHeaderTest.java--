package org.qcmg.qmule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IndelDCCHeaderTest {
	
	File tumourBam;
	File normalBam;
	File somaticFile;
	File somaticOutputFile;
	File germlineFile;
	File germlineOutputFile;
	IndelDCCHeader id;
	private static String FILE_SEPARATOR = System.getProperty("file.separator");
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		tumourBam = createBamFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "tumor.bam", "tumourId");
		normalBam = createBamFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "normal.bam", "normalId");
		somaticFile = createDCCFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "input.dcc1", 13, "3d9d495c-94f7-46a4-9301-7dcbad7285d1");
		somaticOutputFile = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.dcc1");
		germlineFile = createDCCFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "germ.input.dcc1", 13, "2d9d495c-94f7-46a4-9301-7dcbad7285d1");
		germlineOutputFile = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "germ.output.dcc1");
		id = new IndelDCCHeader();
	}
	

	@After
	public void tearDown() {
		tumourBam.delete();
		normalBam.delete();
		germlineFile.delete();
		germlineOutputFile.delete();
		somaticOutputFile.delete();
		somaticFile.delete();
		tumourBam = null;
		normalBam = null;
		germlineFile = null;
		germlineOutputFile =  null;
		somaticFile = null;
		somaticOutputFile = null;
		id = null;
	}
	
	@Test
	public void testGoodOptions() throws Exception {
		IndelDCCHeader id = new IndelDCCHeader();
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", somaticFile.getAbsolutePath(), "-i", germlineFile.getAbsolutePath(), "--tumour", tumourBam.getAbsolutePath(), "--normal", normalBam.getAbsolutePath(), "--output", somaticOutputFile.getAbsolutePath(), "--output", germlineOutputFile.getAbsolutePath(), "--mode", "gatk"};
		id.setup(args);
		assertEquals(tumourBam.getAbsolutePath(), id.getTumourBam().getAbsolutePath());
		assertEquals(normalBam.getAbsolutePath(), id.getNormalBam().getAbsolutePath());
		assertEquals(somaticFile.getAbsolutePath(), id.getSomaticFile().getAbsolutePath());
		assertEquals(germlineFile.getAbsolutePath(), id.getGermlineFile().getAbsolutePath());
		assertEquals(somaticOutputFile.getAbsolutePath(), id.getSomaticOutputFile().getAbsolutePath());
		assertEquals(germlineOutputFile.getAbsolutePath(), id.getGermlineOutputFile().getAbsolutePath());
		assertEquals("gatk", id.getMode());
	}
	
	@Test
	public void testAnnotate() throws Exception {
		
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", somaticFile.getAbsolutePath(), "-i", germlineFile.getAbsolutePath(), "--tumour", tumourBam.getAbsolutePath(), "--normal", normalBam.getAbsolutePath(), "--output", somaticOutputFile.getAbsolutePath(), "--output", germlineOutputFile.getAbsolutePath(), "--mode", "gatk"};
		
		id.setup(args);
		assertFalse(somaticOutputFile.exists());
		assertFalse(germlineOutputFile.exists());
		id.annotate();
		assertTrue(somaticOutputFile.exists());
		assertTrue(germlineOutputFile.exists());
		
		assertAnnotationCorrect(somaticOutputFile, "tumourId");
		assertAnnotationCorrect(germlineOutputFile, "normalId");
		
	}
	
	private void assertAnnotationCorrect(File outputFile, String sampleId) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(outputFile));
		
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			count++;
			if (count == 1 || count == 15) {
				assertTrue(line.contains("3d9d495c_94f7_46a4_9301_7dcbad7285d1"));
			}
			if (count == 52) {
				String[] results = line.split("\t");
				assertEquals("3d9d495c_94f7_46a4_9301_7dcbad7285d1", results[0]);
				assertEquals(sampleId, results[1]);
				assertEquals("3d9d495c_94f7_46a4_9301_7dcbad7285d1_" + sampleId + "_ind1", results[2]);
			}
			if (count == 15) {
				assertTrue(line.startsWith("#Q_DCCMETA"));
			}
			if (count == 25) {
				assertTrue(line.startsWith("#Q_LIMSMETA_TEST"));
			}
			if (count == 38) {
				assertTrue(line.startsWith("#Q_LIMSMETA_CONTROL"));
			}
			
		}
		reader.close();
		
	}


	@Test
	public void testIsCorrectUuidFormat() {
		assertTrue(id.isCorrectUuidFormat("3d9d495c_94f7_46a4_9301_7dcbad7285d1"));
		assertFalse(id.isCorrectUuidFormat("3d9d495c-94f7_46a4_9301_7dcbad7285d1"));
		assertFalse(id.isCorrectUuidFormat("3d9d495c_94f7_46a4_9301_7dcbad7285d"));
	}
	
	@Test
	public void testReplaceAnalysisIdInLine() {
		String uuid = "3d9d495c_94f7_46a4_9301_7dcbad7285d1";
		String tumour = "tumourId_added";
		String normal = "normalId_added";
		id.setUuid(uuid);
		id.setTumourSampleId(tumour);
		id.setNormalSampleId(normal);
		String line = "id\tsecond\tthird_ind1";
		String[] results = id.replaceIdsInLine(line, false).split("\t");
		assertEquals(uuid, results[0]);
		assertEquals(tumour, results[1]);
		assertEquals(uuid + "_" + tumour + "_ind1" , results[2]);
		
		results = id.replaceIdsInLine(line, true).split("\t");
		assertEquals(uuid, results[0]);
		assertEquals(normal, results[1]);
		assertEquals(uuid + "_" + normal + "_ind1" , results[2]);
	}
	
	@Test
	public void testCheckForUUid() throws Exception {
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", somaticFile.getAbsolutePath(), "-i", germlineFile.getAbsolutePath(), "--tumour", tumourBam.getAbsolutePath(), "--normal", normalBam.getAbsolutePath(), "--output", somaticOutputFile.getAbsolutePath(), "--output", germlineOutputFile.getAbsolutePath(), "--mode", "gatk"};
		id.setup(args);
		assertFalse(id.isCompleteHeaderPresent());
		assertFalse(id.isQexecPresent());
		assertEquals(0, id.getQexec().size());
		id.checkForUUid();
		assertFalse(id.isCompleteHeaderPresent());
		assertEquals(14, id.getQexec().size());
		assertTrue(id.isQexecPresent());
	}
	
	@Test(expected=QMuleException.class)
	public void testCheckForUUidThrowsException() throws Exception {
		somaticFile = createDCCFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "input.dcc1", 12, "3d9d495c-94f7-46a4-9301-7dcbad7285d1");
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", somaticFile.getAbsolutePath(), "-i", germlineFile.getAbsolutePath(), "--tumour", tumourBam.getAbsolutePath(), "--normal", normalBam.getAbsolutePath(), "--output", somaticOutputFile.getAbsolutePath(), "--output", germlineOutputFile.getAbsolutePath(), "--mode", "gatk"};
		
		id.setup(args);
		assertFalse(id.isCompleteHeaderPresent());
		assertFalse(id.isQexecPresent());
		assertEquals(0, id.getQexec().size());
		id.checkForUUid();
		assertFalse(id.isCompleteHeaderPresent());
		assertEquals(14, id.getQexec().size());
		assertTrue(id.isQexecPresent());
	}
	
	private File createDCCFile(String fileName, int qexecLength, String uuid) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));
		w.write("#Q_EXEC	Uuid	"+uuid +"\n");
		for (int i=1; i<=qexecLength; i++) {
			w.write("#Q_EXEC\n");
		}
		w.write("analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq\n");
		w.write("id\ttest\ttest_ind1\t2\tchr1\t85\t86\t1\t-999\t-999\t-\t-999\tT\t->T\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--\n");
		w.close();
		return new File(fileName);
	}

	private File createBamFile(String fileName, String sampleID) throws IOException {
		 final List<String> data = new ArrayList<String>();
	        data.add("@HD	VN:1.0	GO:none	SO:coordinate");
	        data.add("@SQ	SN:chr1	LN:249250621	");
	        data.add("@SQ	SN:chr4	LN:191154276	");
	        data.add("@SQ	SN:chr7	LN:159138663	");
	        data.add("@SQ	SN:chrX	LN:155270560	");
	        data.add("@SQ	SN:chrY	LN:59373566	");
	        data.add("@SQ	SN:chr19	LN:59128983	");
	        data.add("@SQ	SN:GL000191.1	LN:106433	");
	        data.add("@SQ	SN:GL000211.1	LN:166566	");
	        data.add("@SQ	SN:chrMT	LN:16569	");
	        data.add("@RG	ID:20120817075934728	PL:ILLUMINA	PU:lane_7	LB:Library_20120726_B	zc:6:/mnt/seq_results/icgc_pancreatic/APGI_1992/seq_mapped/120804_SN7001240_0063_AC0VM1ACXX.lane_7.nobc.bam	SM:Colo-829");
	        data.add("@CO	CN:QCMG	QN:qlimsmeta	Aligner=bwa	Capture Kit=NoCapture	Donor=test	Failed QC=0	Library Protocol=Illumina TruSEQ Multiplexed Manual	Material=1:DNA	Project=test_project	Reference Genome File=/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa	Sample="+sampleID+"	Sample Code=4:Normal control (other site)	Sequencing Platform=HiSeq	Species Reference Genome=Homo sapiens (GRCh37_ICGC_standard_v2)");
	        
	        BufferedWriter out;
	        out = new BufferedWriter(new FileWriter(fileName));
	        for (final String line : data) {
	            out.write(line + "\n");
	        }
	        out.close();
	        return new File(fileName);		
	}

	
	

}
