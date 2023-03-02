package au.edu.qimr.qannotate.nanno;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.vcf.header.VcfHeaderUtils;


public class AnnotateTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
  	
	@Test
	public void jsonInputs() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.vcf");
		createJsonInputs(inputJson, annotationSource, "blah");
		
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		assertEquals(true, ais != null);
		assertEquals(1, ais.getInputs().size());
		
		List<AnnotationSource> sources = new ArrayList<>();
		AnnotateUtils.populateAnnotationSources(ais, sources);
		assertEquals(1, sources.size());
		
		String annotation = sources.get(0).getAnnotation(new ChrPositionRefAlt("chr1", 95813205, 95813205, "C", "T"));
		assertEquals("blah=", annotation);
	}
	
	@Test
	public void jsonInputsTSVMissingHeader() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.tsv");
		createJsonInputs(inputJson, annotationSource, "blah");
		
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		try {
			AnnotateUtils.populateAnnotationSources(ais, new ArrayList<>());
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			assertEquals(true, iae.getMessage().contains("No headers for AnnotationSourceTSV!"));
		}
		
		/*
		 * now try populating the annotation file - with no header
		 */
		createAnnotationFile(annotationSource);
		try {
			AnnotateUtils.populateAnnotationSources(ais, new ArrayList<>());
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			assertEquals(true, iae.getMessage().contains("No headers for AnnotationSourceTSV!"));
		}
		
		/*
		 * and with a header
		 */
		createAnnotationFile(annotationSource, true);
		try {
			AnnotateUtils.populateAnnotationSources(ais, new ArrayList<>());
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			assertEquals(true, iae.getMessage().contains("Could not find requested fields (blah) in header"));
		}
	}
	
	@Test
	public void jsonInputsTSV() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.tsv");
		createJsonInputs(inputJson, annotationSource, "aaref,HGVSc_VEP,HGVSp_VEP");
		createAnnotationFile(annotationSource, true);
		List<AnnotationSource> sources = new ArrayList<>(2);
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		AnnotateUtils.populateAnnotationSources(ais, sources);
		assertEquals(1, sources.size());
		String annotation = sources.get(0).getAnnotation(new ChrPositionRefAlt("chr1", 655652, 655652, "A", "T"));
		assertEquals("HGVSc_VEP=c.1A>C\tHGVSp_VEP=p.Met1?\taaref=M", annotation);
	}
	
	@Test
	public void endToEnd() throws IOException {
		File inputVcf = testFolder.newFile("input.vcf");
		File annotationSource = testFolder.newFile("annotation.tsv");
		File inputJson = testFolder.newFile("inputs.json");
		File outputFile = testFolder.newFile("output.tsv");
		
		/*
		 * incorrect order and no matching annotations
		 */
		createVcf(inputVcf, Arrays.asList(
				"chr1	95813205	rs11165387	C	T	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.",
        		"chr1	60781981	rs5015226	T	G	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:."));
		/*
		 * annotation file next
		 */
		createAnnotationFile(annotationSource, true);
		/*
		 * json inputs - need annotationSource deets
		 */
		createJsonInputs(inputJson, annotationSource, "aaref");
		
		int exitValue = executeTest(inputVcf, inputJson, outputFile);
		assertEquals(1, exitValue);
		
		/*
		 * matching annotations but incorrect order
		 */
		createVcf(inputVcf, Arrays.asList(
				"chr1	655652	rs11165387	A	T	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.",
				"chr1	655650	rs5015226	A	C	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:."));
		
		exitValue = executeTest(inputVcf, inputJson, outputFile);
		assertEquals(1, exitValue);
		
		/*
		 * should work and annotate
		 */
		createVcf(inputVcf, Arrays.asList(
				"chr1	655650	rs11165387	A	C	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.",
        		"chr1	655652	rs5015226	A	T	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:."));
		
		exitValue = executeTest(inputVcf, inputJson, outputFile);
		assertEquals(0, exitValue);
		
		/*
		 * check output file
		 */
		List<String> lines = Files.readAllLines(Paths.get(outputFile.getAbsolutePath()));
		assertEquals(true, lines.contains("chr1	655650	A	C	17,15	M				"));
		assertEquals(true, lines.contains("chr1	655652	A	T	0,46	M				"));
		
	}
	
	private int executeTest(File inputVcf, File inputJson, File outputFile) {
	      
	    //run vcf2maf
		try {
		      File log = testFolder.newFile();
		      final String command = " au.edu.qimr.qannotate.nanno.Annotate --input " +  inputVcf.getAbsolutePath() + " --loglevel DEBUG  --log " + log.getAbsolutePath() + " -config " + inputJson.getAbsolutePath() 
		      	+ " --output " + outputFile.getAbsolutePath();
		      Executor exec = new Executor(command, "au.edu.qimr.qannotate.nanno.Annotate");
		      return exec.getErrCode() ;
		} catch (Exception e) {
			e.printStackTrace();
			fail("failed during executeTest!");
		}  
	      
	    return 1;	 
	}
	
	public static void createJsonInputs(File jsonFile, File annotationFile, String annotationFields) throws IOException {
		List<String> data = Arrays.asList(
				"{",
  "\"outputFieldOrder\": \"" + annotationFields + "\",",
  "\"additionalEmptyFields\": \"test1,test2,test3\",",
  "\"includeSearchTerm\": true,",
  "\"annotationSourceThreadCount\": 1,",
  "\"inputs\": [{",
    "\"file\": \"" + annotationFile.getAbsolutePath() + "\",",
    "\"chrIndex\": 1,",
    "\"positionIndex\": 2,",
    "\"refIndex\": 3,",
    "\"altIndex\": 4,",
    "\"fields\": \"" + annotationFields + "\"",
  "}]",
"}"
				);
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(jsonFile));) {
			for (String line : data) {
				out.write(line + "\n");
			}
		}
	}
	
	@Test
	public void loadJSONInputs() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		List<String> data = Arrays.asList(
				"{",
  "\"outputFieldOrder\": \"field_1,field_2,field_3\",",
  "\"additionalEmptyFields\": \"test1,test2,test3\",",
  "\"includeSearchTerm\": true,",
  "\"annotationSourceThreadCount\": 3,",
  "\"inputs\": [{",
    "\"file\": \"/file_1.tsv\",",
    "\"chrIndex\": 1,",
    "\"positionIndex\": 2,",
    "\"refIndex\": 3,",
    "\"altIndex\": 4,",
    "\"fields\": \"field_1\"",
  "},",
  "{",
	  "\"file\": \"file_2.eff.vcf\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"snpEffVcf\": true,",
    "\"fields\": \"field_2\"",
  "},",
  "{",
	  "\"file\": \"/file_3.vcf.gz\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"fields\": \"field_3\"",
  "}]",
"}"
				);
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(inputJson));) {
			for (String line : data) {
				out.write(line + "\n");
			}
		}
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		assertEquals(true, ais.isIncludeSearchTerm());
		assertEquals("test1,test2,test3", ais.getAdditionalEmptyFields());
		assertEquals(3, ais.getAnnotationSourceThreadCount());
		assertEquals("field_1,field_2,field_3", ais.getOutputFieldOrder());
		assertEquals(3, ais.getInputs().size());
	}
	 
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	static void createVcf(File f, List<String> records) throws IOException {
        List<String> data =Arrays.asList(
        		"##fileformat=VCFv4.2",
        		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT
//        		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,
//        		"chr1	95813205	rs11165387	C	T	.	.	FLANK=CTTCGTGTCTT;BaseQRankSum=1.846;ClippingRankSum=-1.363;DP=35;FS=3.217;MQ=60.00;MQRankSum=-1.052;QD=18.76;ReadPosRankSum=-1.432;SOR=0.287;IN=1,2;DB;VAF=0.6175;HOM=0,TCCTTCTTCGtGTCTTTCCTT;EFF=downstream_gene_variant(MODIFIER||3602|||RP11-14O19.1|lincRNA|NON_CODING|ENST00000438093||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.	0/1:16,18:Germline:21:34:.:.:PASS:99:.:.:.:656.77	./.:.:HomozygousLoss:21:.:.:.:PASS:.:NCIG:.:.:.",
//        		"chr1	60781981	rs5015226	T	G	.	.	FLANK=ACCAAGTGCCT;DP=53;FS=0.000;MQ=60.00;QD=31.16;SOR=0.730;IN=1,2;DB;HOM=0,CGAAAACCAAgTGCCTGCATT;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:.	1/1:0,53:Germline:34:53:.:.:PASS:99:.:.:.:2418.77	1/1:0,60:Germline:34:60:.:.:PASS:99:.:.:.:2749.77"
        		);
        
        try (BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
            for (String line : data) {
            	out.write(line + "\n");
            }
            for (String line : records) {
            	out.write(line + "\n");
            }
         }
	}
	
	/**
	 * a mini dbSNP vcf file 
	
	 */
	static void createAnnotationFile(File f, boolean addHeader) throws IOException {
		String header = "#chr	pos(1-based)	ref	alt	aaref	aaalt	rs_dbSNP151	hg19_chr	hg19_pos(1-based)	hg18_chr	hg18_pos(1-based)	aapos	genename	Ensembl_geneid	Ensembl_transcriptid	Ensembl_proteinid	Uniprot_acc	Uniprot_entry	HGVSc_ANNOVAR	HGVSp_ANNOVAR	HGVSc_snpEff	HGVSp_snpEff	HGVSc_VEP	HGVSp_VEP	APPRIS	GENCODE_basic	TSL	VEP_canonical	cds_strand	refcodon";
        List<String> data = Arrays.asList(
        		"1	655650	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655651	A	C	M	L	.	1	65565	1	55428	1	OR4F51	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655651	A	G	M	L	.	1	65565	1	55428	1	OR4F52	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F53	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F54	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F55	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
        		"1	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F56	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?"
        		);

        try (BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
        	if (addHeader) {
        		out.write(header + "\n");
        	}
           for (String line : data) {
        	   out.write(line + "\n");
           }
        }
	}	
	static void createAnnotationFile(File f) throws IOException {
		createAnnotationFile(f, false);
	}
	/**
	 * a mini dbSNP vcf file 
	 */
	static void createAnnotationFileIncorrectOrder(File f) throws IOException {
		String header = "#chr	pos(1-based)	ref	alt	aaref	aaalt	rs_dbSNP151	hg19_chr	hg19_pos(1-based)	hg18_chr	hg18_pos(1-based)	aapos	genename	Ensembl_geneid	Ensembl_transcriptid	Ensembl_proteinid	Uniprot_acc	Uniprot_entry	HGVSc_ANNOVAR	HGVSp_ANNOVAR	HGVSc_snpEff	HGVSp_snpEff	HGVSc_VEP	HGVSp_VEP	APPRIS	GENCODE_basic	TSL	VEP_canonical	cds_strand	refcodon";
		List<String> data = Arrays.asList(
				"1	655650	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655651	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655651	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"1	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"10	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"10	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"10	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"10	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"11	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"11	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"11	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"11	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"12	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"12	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"12	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"12	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"2	655651	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"2	655652	A	C	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"2	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"2	655652	A	T	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"3	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"4	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"5	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?",
				"6	655652	A	G	M	L	.	1	65565	1	55428	1	OR4F5	ENSG00000186092	ENST00000641515	ENSP00000493376	A0A2U3U0J3	A0A2U3U0J3_HUMAN	.	.	.	.	c.1A>C	p.Met1?"
				);
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
			out.write(header + "\n");
			for (String line : data) {
				out.write(line + "\n");
			}
		}
	}
	
}
