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
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AnnotateDCCWithGFFRegionTest {	

	private File inputDCC1File;
	private File inputRepeatGFF;
	private File inputGermGFF;
	private File outputFile;
	private AnnotateDCCWithGFFRegions ann;
	private File inputDCCQFile;
	private static String FILE_SEPARATOR = System.getProperty("file.separator");
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();	
	
	@Before
	public void setUp() throws IOException {
		inputDCC1File = createDCCFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "input.dcc1");
		inputDCCQFile = createDCCQFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "input.dccq");
		inputRepeatGFF = createRepeatGFFFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "input.gff3");
		inputGermGFF = createGermGFFFile(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "germ.gff3");
		outputFile = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.dccq");
		ann = new AnnotateDCCWithGFFRegions();
	}	

	@After
	public void tearDown() {
		inputDCC1File.delete();
		outputFile.delete();
		inputDCCQFile.delete();
		outputFile.delete();
		inputRepeatGFF.delete();
		inputGermGFF.delete();
		inputDCC1File = null;
		outputFile = null;
		ann = null;
	}
	
	@Test
	public void testGoodOptions() throws Exception {
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCC1File.getAbsolutePath(), "-i", inputRepeatGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath()};
		ann.setup(args);
		assertEquals(inputDCC1File.getAbsolutePath(), ann.getCmdLineInputFiles()[0]);
		assertEquals(inputRepeatGFF.getAbsolutePath(), ann.getCmdLineInputFiles()[1]);
		assertEquals(outputFile.getAbsolutePath(), ann.getCmdLineOutputFiles()[0]);
		
		//with annotation
		String[] args2 = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCC1File.getAbsolutePath(), "-i", inputRepeatGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath(), "--annotation", "GERM"};
		ann.setup(args2);
		assertEquals("GERM", ann.getAnnotation());
	}
	
	@Test(expected=QMuleException.class)
	public void testBadOptions() throws Exception {
		outputFile = testFolder.newFile();
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCC1File.getAbsolutePath(), "-i", inputRepeatGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath()};
		ann.setup(args);
	}
	
	@Test
	public void testEngageWithDCC1Repeat() throws Exception {
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCC1File.getAbsolutePath(), "-i", inputRepeatGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath()};
		ann.setup(args);
		int exit = ann.engage();
		assertEquals(0, exit);
		assertTrue(outputFile.exists());
		
		BufferedReader reader = new BufferedReader(new FileReader(outputFile));
		
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			count++;
			String[] values = line.split("\t");
			if (count == 2) {
				assertEquals("PASS", values[23]);
			}
			if (count == 3) {
				assertEquals("PASS;Simple_repeat::(CCCTAA)n", values[23]);
			}
		}
		reader.close();
	}
	
	@Test
	public void testEngageWithDCC1GERM() throws Exception {
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCC1File.getAbsolutePath(),				
				"-i", inputGermGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath(), "--annotation", "GERM"};
		ann.setup(args);
		int exit = ann.engage();
		assertEquals(0, exit);
		assertTrue(outputFile.exists());
		assertEquals(1, ann.getOverlapCount());
		assertEquals(1, ann.getNotOverlappingCount());
		BufferedReader reader = new BufferedReader(new FileReader(outputFile));
		
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			count++;
			String[] values = line.split("\t");
			if (count == 2) {
				assertEquals("PASS", values[23]);
			}
			if (count == 3) {
				assertEquals("PASS;GERM", values[23]);
			}
		}
		reader.close();
	}
	
	@Test
	public void testEngageWithDCCQGERM() throws Exception {
		String[] args = {"--log", testFolder.newFile().getAbsolutePath(), "-i", inputDCCQFile.getAbsolutePath(),				
				"-i", inputGermGFF.getAbsolutePath(), "--output", outputFile.getAbsolutePath(), "--annotation", "GERM"};
		ann.setup(args);
		int exit = ann.engage();
		assertEquals(0, exit);
		assertTrue(outputFile.exists());
		assertEquals(3, ann.getOverlapCount());
		assertEquals(3, ann.getNotOverlappingCount());
		BufferedReader reader = new BufferedReader(new FileReader(outputFile));
		
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			count++;
			String[] values = line.split("\t");
			if (count == 2 || count == 3) {
				assertEquals("4", values[1]);
			}
			if (count == 4 || count == 5) {
				assertEquals("2", values[1]);
			}
			if (count == 6 || count == 7) {
				assertEquals("3", values[1]);
			}
			if (count == 2 || count == 4 || count == 6) {
				assertTrue(values[37].contains("GERM"));
			}
			if (count == 3 || count == 5 || count == 7) {
				assertFalse(values[37].contains("GERM"));
			}
		}
		reader.close();
	}
	
	@Test
	public void testParseDCCColumnsWithDCCQ() throws QMuleException {
		Vector<String> headers = new Vector<String>();
		headers.add("mutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation" +
				"\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tND\tTD\tNNS\tconsequence_type\taa_mutation\tcds_mutation" +
				"\tprotein_domain_affected\tgene_affected\ttranscript_affected\tgene_build_version\tnote_s\tgene_symbol\tAll_domains\tAll_domains_type\tAll_domains_description\tChrPosition\tQCMGflag\tFlankSeq");
		ann.parseDCCHeader(headers, "dccq");
		assertEquals(5, ann.getDCC_STRAND_INDEX());
		assertEquals(37, ann.getQCMGFLAG_COLUMN_INDEX());
		assertEquals(8, ann.getREFERENCE_ALLELE_INDEX());
		assertEquals(10, ann.getTUMOUR_ALLELE_INDEX());
		assertEquals(1, ann.getMUTATION_TYPE_INDEX());
	}
	
	@Test
	public void testParseDCCColumnsWithDCC1() throws QMuleException {
		Vector<String> headers = new Vector<String>();
		headers.add("analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele" +
				"\tcontrol_genotype\ttumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq");
		ann.parseDCCHeader(headers, "dcc1");
		assertEquals(7, ann.getDCC_STRAND_INDEX());
		assertEquals(23, ann.getQCMGFLAG_COLUMN_INDEX());
	}
	
	private File createDCCFile(String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));
		w.write("analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq\n");
		w.write("id\ttest\ttest_ind1\t2\tchr1\t85\t86\t1\t-999\t-999\t-\t-999\tT\t->T\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--\n");
		w.write("id\ttest\ttest_ind1\t2\tchr1\t10001\t10002\t1\t-999\t-999\t-\t-999\tT\t->T\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--\n");
		
		w.close();
		return new File(fileName);
	}
	
	private File createRepeatGFFFile(String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));	
		w.write("chr1\thg19.fa.out\tSimple_repeat::(CCCTAA)n\t10001\t10468\t1504\t+\t.\tID=1;Note=(CCCTAA)n;SR_length=6;\n");
		w.close();
		return new File(fileName);
	}
	
	private File createGermGFFFile(String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));	
		w.write("chr1\thg19.fa.out\t.\t10024\t10024\t1504\t+\t.\tReferenceAllele=C;TumourAllele=-;PatientCount=10\n");
		w.write("chr1\thg19.fa.out\t.\t10021\t10022\t1504\t+\t.\tReferenceAllele=-;TumourAllele=T;PatientCount=10\n");
		w.write("chr1\thg19.fa.out\t.\t10001\t10011\t1504\t+\t.\tReferenceAllele=CTAAGTCACC;TumourAllele=-;PatientCount=10\n");
		w.write("chr1\thg19.fa.out\t.\t10001\t10002\t1504\t+\t.\tReferenceAllele=-;TumourAllele=T;PatientCount=10\n");
		w.close();
		return new File(fileName);
	}
	
	private File createDCCQFile(String fileName) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(fileName)));
		w.write("mutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation" +
				"\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tND\tTD\tNNS\tconsequence_type\taa_mutation\tcds_mutation" +
				"\tprotein_domain_affected\tgene_affected\ttranscript_affected\tgene_build_version\tnote_s\tgene_symbol\tAll_domains\tAll_domains_type\tAll_domains_description\tChrPosition\tQCMGflag\tFlankSeq\n");
		w.write("test_ind1\t4\tchr1\t10001\t10011\t1\t-999\t-999\tCTAAGTCACC\t-999\tCCTTCAAGATTCAACCTGAATAAATCGCT\tCTAAGTCACC>CCTTCAAGATTCAACCTGAATAAATCGCT\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");
		w.write("test_ind1\t4\tchr1\t10001\t10010\t1\t-999\t-999\tCTAAGTCACC\t-999\tCCTTCAAGATTCAACCTGAATAAATCGCT\tCTAAGTCACC>CCTTCAAGATTCAACCTGAATAAATCGCT\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");		
		w.write("test_ind1\t2\tchr1\t10021\t10022\t1\t-999\t-999\t-\t-999\tT\t->T\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");
		w.write("test_ind1\t2\tchr1\t10021\t10022\t1\t-999\t-999\t-\t-999\tC\t->C\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");
		w.write("test_ind1\t3\tchr1\t10024\t10024\t1\t-999\t-999\tC\t-999\t-\tC>-\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");
		w.write("test_ind1\t3\tchr1\t10024\t10024\t1\t-999\t-999\tG\t-999\t-\tG>-\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t0;44;26;0;4;1;9\t0;113;76;0;12;0;21;\t--\t-888\t-888\t-888\t-888\t-888\t-888\t70\t-999\t--\t--\t--\t--\tchr1:817120-817130\tPASS;NNS\t--\n");
		
		w.close();
		return new File(fileName);
	}


}
