package au.edu.qimr.qannotate.nanno;

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
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import static org.junit.Assert.*;


public class AnnotateTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
  	
	@Test
	public void jsonInputs() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.vcf");
		createJsonInputs(inputJson, annotationSource, "blah", false, 3, 4, true);
		
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
        assertTrue(ais != null);
        assert ais != null;
        assertEquals(1, ais.getInputs().size());
		
		List<AnnotationSource> sources = new ArrayList<>();
		AnnotateUtils.populateAnnotationSources(ais, sources);
		assertEquals(1, sources.size());
		
		String annotation = sources.getFirst().getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 95813205), new ChrPositionRefAlt("chr1", 95813205, 95813205, "C", "T"));
		assertEquals("blah=", annotation);
	}
	
	@Test
	public void jsonInputsTSVMissingHeader() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.tsv");
		createJsonInputs(inputJson, annotationSource, "blah", false, 3, 4, true);
		
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
	public void jsonInputsShouldHandleValidInputs() throws IOException {
		// Given valid inputs, the method should process them without throwing exceptions
		File validInputJson = testFolder.newFile("valid_inputs.json");
		File validAnnotationSource = testFolder.newFile("valid_annotation.vcf");
		createJsonInputs(validInputJson, validAnnotationSource, "valid", false, 3, 4, true);

		// When
		AnnotationInputs ais = AnnotateUtils.getInputs(validInputJson.getAbsolutePath());

		// Then
		Assert.assertNotNull(ais);
		Assert.assertEquals(1, ais.getInputs().size());

		List<AnnotationSource> sources = new ArrayList<>();
		AnnotateUtils.populateAnnotationSources(ais, sources);
		assertEquals(1, sources.size());

		String annotation = sources.getFirst().getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 95813205), new ChrPositionRefAlt("chr1", 95813205, 95813205, "C", "T"));
		Assert.assertEquals("valid=", annotation);
	}

	@Test
	public void jsonInputsTSV() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		File annotationSource = testFolder.newFile("annotation.tsv");
		createJsonInputs(inputJson, annotationSource, "aaref,HGVSc_VEP,HGVSp_VEP", false, 3, 4, false);
		createAnnotationFile(annotationSource, true);
		List<AnnotationSource> sources = new ArrayList<>(2);
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		AnnotateUtils.populateAnnotationSources(ais, sources);
		assertEquals(1, sources.size());
		String annotation = sources.getFirst().getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 655652), new ChrPositionRefAlt("chr1", 655652, 655652, "A", "T"));
		assertEquals("HGVSc_VEP=c.1A>C\tHGVSp_VEP=p.Met1?\taaref=M", annotation);
	}
	
	@Test
	public void endToEnd() throws IOException {
		File inputVcf = testFolder.newFile("input.vcf");
		File annotationSource = testFolder.newFile("annotation.tsv");
		File inputJson = testFolder.newFile("inputs.json");
		File outputFile = testFolder.newFile("output.tsv");
		File logFile = testFolder.newFile("endToEnd.log");
		
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
		createJsonInputs(inputJson, annotationSource, "aaref", false, 3, 4, false);
		
		int exitValue = executeTest(inputVcf, inputJson, outputFile, logFile);
		assertEquals(1, exitValue);
		
		/*
		 * matching annotations but incorrect order
		 */
		createVcf(inputVcf, Arrays.asList(
				"chr1	655652	rs11165387	A	T	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.",
				"chr1	655650	rs5015226	A	C	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:."));

		exitValue = executeTest(inputVcf, inputJson, outputFile, null);
		assertEquals(1, exitValue);
		
		/*
		 * should work and annotate
		 */
		createVcf(inputVcf, Arrays.asList(
				"chr1	655650	rs11165387	A	C	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.",
        		"chr1	655652	rs5015226	A	T	.	.	.	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:."));

		exitValue = executeTest(inputVcf, inputJson, outputFile, null);
		assertEquals(0, exitValue);
		
		/*
		 * check output file
		 */
		List<String> lines = Files.readAllLines(Paths.get(outputFile.getAbsolutePath()));
		assertEquals(true, lines.contains("chr1	655650	A	C	C	0/1	17,15	M				"));
		assertEquals(true, lines.contains("chr1	655652	A	T	T	1/1	0,46	M				"));
		
	}

	@Test
	public void endToEndSnpEff() throws IOException {
		File inputVcf = testFolder.newFile("input.vcf");
		File snpEffAnnotationSource = testFolder.newFile("snpEff_annotation.vcf");
		File inputJson = testFolder.newFile("inputs.json");
		File outputFile = testFolder.newFile("output.tsv");
		File logFile = testFolder.newFile("endToEnd.log");

		createVcf(inputVcf, Arrays.asList(
				"chr1    94641   .       G       C       229.02  HardFiltered    AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=14;ExcessHet=0.0000;FS=3.358;MLEAC=2;MLEAF=1.00;MQ=37.05;MQRankSum=1.30;QD=17.62;ReadPosRankSum=0.695;SOR=2.093;ANN=C|intergenic_region|MODIFIER|OR4F5-OR4F29|ENSG00000186092.7-ENSG00000284733.2|intergenic_region|ENSG00000186092.7-ENSG00000284733.2|||n.94641G>C||||||       GT:AD:DP:GQ:PL  1/1:6,7:13:21:243,21,0".replaceAll("\\s+", "\t"),
				"chr1    94819   .       CTTTTCT C       48.60   PASS    AC=1;AF=0.500;AN=2;BaseQRankSum=-1.528e+00;DP=26;ExcessHet=0.0000;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=46.27;MQRankSum=-1.450e+00;QD=2.03;ReadPosRankSum=0.969;SOR=1.071     GT:AD:DP:GQ:PL  0/1:20,4:24:56:56,0,144".replaceAll("\\s+", "\t"),
				"chr1    94824   .       CT      *,C     67      PASS    AC=1,1;AF=0.500,0.500;AN=2;BaseQRankSum=-9.270e-01;DP=26;ExcessHet=0.0000;FS=7.521;MLEAC=1,1;MLEAF=0.500,0.500;MQ=46.27;MQRankSum=-2.726e+00;QD=3.53;ReadPosRankSum=-1.118e+00;SOR=2.546        GT:AD:DP:GQ:PL  1/2:11,4,4:19:64:118,77,297,64,0,165".replaceAll("\\s+", "\t"),
				"chr1    889689  .       G       A,C     679.1   PASS    AC=1,1;AF=0.500,0.500;AN=2;BaseQRankSum=-2.586e+00;DP=63;ExcessHet=0.0000;FS=2.623;MLEAC=1,1;MLEAF=0.500,0.500;MQ=52.67;MQRankSum=-1.696e+00;QD=15.43;ReadPosRankSum=-9.520e-01;SOR=0.984;ANN=A|intergenic_region|MODIFIER|OR4F16-SAMD11|ENSG00000284662.2-ENSG00000187634.13|intergenic_region|ENSG00000284662.2-ENSG00000187634.13|||n.889689G>A||||||,C|intergenic_region|MODIFIER|OR4F16-SAMD11|ENSG00000284662.2-ENSG00000187634.13|intergenic_region|ENSG00000284662.2-ENSG00000187634.13|||n.889689G>C||||||     GT:AD:DP:GQ:PL  1/2:22,4,18:44:99:696,580,775,165,0,111".replaceAll("\\s+", "\t"),
				"chr1    1130186 .       CAAAAAA C,CAAAAAAAAAAAAAA       125.02  HardFiltered    AC=1,1;AF=0.500,0.500;AN=2;DP=17;ExcessHet=0.0000;FS=0.000;MLEAC=1,1;MLEAF=0.500,0.500;MQ=59.72;QD=25.00;SOR=3.611;ANN=C|intergenic_region|MODIFIER|C1orf159-TTLL10|ENSG00000131591.18-ENSG00000162571.14|intergenic_region|ENSG00000131591.18-ENSG00000162571.14|||n.1130187_1130192delAAAAAA||||||,CAAAAAAAAAAAAAA|intergenic_region|MODIFIER|C1orf159-TTLL10|ENSG00000131591.18-ENSG00000162571.14|intergenic_region|ENSG00000131591.18-ENSG00000162571.14|||n.1130192_1130193insAAAAAAAA||||||      GT:AD:DP:GQ:PL  1/2:0,3,2:5:67:142,74,232,72,0,67".replaceAll("\\s+", "\t")));
		/*
		 * snp eff annotation file next
		 */
		createSnpEFfAnnotationFile(snpEffAnnotationSource, true);
		/*
		 * json inputs - need annotationSource deets
		 */
		createJsonInputs(inputJson, snpEffAnnotationSource, "gene_name,feature_id,feature_type,effect,cdna_position,cds_position,protein_position,putative_impact,hgvs.c,hgvs.p", true, 4, 5, true);

		int exitValue = executeTest(inputVcf, inputJson, outputFile, logFile);
		assertEquals(0, exitValue);


		/*
		 * check output file
		 */
		List<String> lines = Files.readAllLines(Paths.get(outputFile.getAbsolutePath()));
		assertEquals("chr1\t94641\tG\tC\tC\t1/1\t6,7\tOR4F5-OR4F29\tENSG00000186092.7-ENSG00000284733.2\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.94641G>C\t\t\t\t\t\"GENE\"+(\"94641G>C\"|\"94641G->C\"|\"94641G-->C\"|\"94641G/C\")", lines.get(lines.size() - 7));
		assertEquals("chr1\t94819\tCTTTTCT\tC\tC\t0/1\t20,4\tOR4F5-OR4F29\tENSG00000186092.7-ENSG00000284733.2\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.94820_94825delTTTTCT\t\t\t\t\t", lines.get(lines.size() - 6));
		assertEquals("chr1\t94824\tCT\tC\t*,C\t1/2\t11,4,4\tOR4F5-OR4F29\tENSG00000186092.7-ENSG00000284733.2\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.94825delT\t\t\t\t\t", lines.get(lines.size() - 5));
		assertEquals("chr1\t889689\tG\tA\tA,C\t1/2\t22,4,18\tOR4F16-SAMD11\tENSG00000284662.2-ENSG00000187634.13\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.889689G>A\t\t\t\t\t\"GENE\"+(\"889689G>A\"|\"889689G->A\"|\"889689G-->A\"|\"889689G/A\")", lines.get(lines.size() - 4));
		assertEquals("chr1\t889689\tG\tC\tA,C\t1/2\t22,4,18\tOR4F16-SAMD11\tENSG00000284662.2-ENSG00000187634.13\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.889689G>C\t\t\t\t\t\"GENE\"+(\"889689G>C\"|\"889689G->C\"|\"889689G-->C\"|\"889689G/C\")", lines.get(lines.size() - 3));
		assertEquals("chr1\t1130186\tCAAAAAA\tC\tC,CAAAAAAAAAAAAAA\t1/2\t0,3,2\tC1orf159-TTLL10\tENSG00000131591.18-ENSG00000162571.14\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.1130187_1130192delAAAAAA\t\t\t\t\t", lines.get(lines.size() - 2));
		assertEquals("chr1\t1130186\tCAAAAAA\tCAAAAAAAAAAAAAA\tC,CAAAAAAAAAAAAAA\t1/2\t0,3,2\tC1orf159-TTLL10\tENSG00000131591.18-ENSG00000162571.14\tintergenic_region\tintergenic_region\t\t\t\tMODIFIER\tn.1130192_1130193insAAAAAAAA\t\t\t\t\t", lines.get(lines.size() - 1));
	}
	
	private int executeTest(File inputVcf, File inputJson, File outputFile, File log) throws IOException {

		if (null == log) {
			log = testFolder.newFile();
		}
	    //run vcf2maf
		try {
		      final String command = "--input " +  inputVcf.getAbsolutePath() + " --loglevel DEBUG --log " + log.getAbsolutePath() + " --config " + inputJson.getAbsolutePath()
		      	+ " --output " + outputFile.getAbsolutePath();
		      Executor exec = new Executor(command, "au.edu.qimr.qannotate.nanno.Annotate");
		      return exec.getErrCode() ;
		} catch (Exception e) {
			e.printStackTrace();
			fail("failed during executeTest!");
		}  
	      
	    return 1;
	}
	
	public static void createJsonInputs(File jsonFile, File annotationFile, String annotationFields, boolean snpEffAnnotationFile, int refPos, int altPos, boolean chrStartsWithChr) throws IOException {
		List<String> data = Arrays.asList(
				"{",
  "\"outputFieldOrder\": \"" + annotationFields + "\",",
  "\"additionalEmptyFields\": \"test1,test2,test3\",",
  "\"includeSearchTerm\": true,",
  "\"annotationSourceThreadCount\": 1,",
  "\"inputs\": [{",
    "\"file\": \"" + annotationFile.getAbsolutePath() + "\",",
	"\"snpEffVcf\": " + snpEffAnnotationFile + ",",
	"\"chrStartsWithChr\": " + chrStartsWithChr + ",",
    "\"chrIndex\": 1,",
    "\"positionIndex\": 2,",
    "\"refIndex\": " + refPos + ",",
    "\"altIndex\": " + altPos + ",",
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

	static void createSnpEFfAnnotationFile(File f, boolean addHeader) throws IOException {
		String header = "#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO    FORMAT  78cf87bd-7c1d-4808-911c-70c3f7a98534".replaceAll("\\s+", "\t");
		List<String> data = Arrays.asList(
				"chr1    94641   .       G       C       229.02  HardFiltered    AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=14;ExcessHet=0.0000;FS=3.358;MLEAC=2;MLEAF=1.00;MQ=37.05;MQRankSum=1.30;QD=17.62;ReadPosRankSum=0.695;SOR=2.093;ANN=C|intergenic_region|MODIFIER|OR4F5-OR4F29|ENSG00000186092.7-ENSG00000284733.2|intergenic_region|ENSG00000186092.7-ENSG00000284733.2|||n.94641G>C||||||       GT:AD:DP:GQ:PL  1/1:6,7:13:21:243,21,0".replaceAll("\\s+", "\t"),
				"chr1    94819   .       CTTTTCT C       48.6    PASS    AC=1;AF=0.500;AN=2;BaseQRankSum=-1.528e+00;DP=26;ExcessHet=0.0000;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=46.27;MQRankSum=-1.450e+00;QD=2.03;ReadPosRankSum=0.969;SOR=1.071;ANN=C|intergenic_region|MODIFIER|OR4F5-OR4F29|ENSG00000186092.7-ENSG00000284733.2|intergenic_region|ENSG00000186092.7-ENSG00000284733.2|||n.94820_94825delTTTTCT||||||      GT:AD:DP:GQ:PL  0/1:20,4:24:56:56,0,144".replaceAll("\\s+", "\t"),
				"chr1    94824   .       CT      *,C     67.0    PASS    AC=1,1;AF=0.500,0.500;AN=2;BaseQRankSum=-9.270e-01;DP=26;ExcessHet=0.0000;FS=7.521;MLEAC=1,1;MLEAF=0.500,0.500;MQ=46.27;MQRankSum=-2.726e+00;QD=3.53;ReadPosRankSum=-1.118e+00;SOR=2.546;ANN=C|intergenic_region|MODIFIER|OR4F5-OR4F29|ENSG00000186092.7-ENSG00000284733.2|intergenic_region|ENSG00000186092.7-ENSG00000284733.2|||n.94825delT||||||    GT:AD:DP:GQ:PL  1/2:11,4,4:19:64:118,77,297,64,0,165".replaceAll("\\s+", "\t"),
				"chr1    889689  .       G       A,C     679.1   PASS    AC=1,1;AF=0.500,0.500;AN=2;BaseQRankSum=-2.586e+00;DP=63;ExcessHet=0.0000;FS=2.623;MLEAC=1,1;MLEAF=0.500,0.500;MQ=52.67;MQRankSum=-1.696e+00;QD=15.43;ReadPosRankSum=-9.520e-01;SOR=0.984;ANN=A|intergenic_region|MODIFIER|OR4F16-SAMD11|ENSG00000284662.2-ENSG00000187634.13|intergenic_region|ENSG00000284662.2-ENSG00000187634.13|||n.889689G>A||||||,C|intergenic_region|MODIFIER|OR4F16-SAMD11|ENSG00000284662.2-ENSG00000187634.13|intergenic_region|ENSG00000284662.2-ENSG00000187634.13|||n.889689G>C||||||     GT:AD:DP:GQ:PL  1/2:22,4,18:44:99:696,580,775,165,0,111".replaceAll("\\s+", "\t"),
				"chr1    1130186 .       CAAAAAA C,CAAAAAAAAAAAAAA       125.02  HardFiltered    AC=1,1;AF=0.500,0.500;AN=2;DP=17;ExcessHet=0.0000;FS=0.000;MLEAC=1,1;MLEAF=0.500,0.500;MQ=59.72;QD=25.00;SOR=3.611;ANN=C|intergenic_region|MODIFIER|C1orf159-TTLL10|ENSG00000131591.18-ENSG00000162571.14|intergenic_region|ENSG00000131591.18-ENSG00000162571.14|||n.1130187_1130192delAAAAAA||||||,CAAAAAAAAAAAAAA|intergenic_region|MODIFIER|C1orf159-TTLL10|ENSG00000131591.18-ENSG00000162571.14|intergenic_region|ENSG00000131591.18-ENSG00000162571.14|||n.1130192_1130193insAAAAAAAA||||||      GT:AD:DP:GQ:PL  1/2:0,3,2:5:67:142,74,232,72,0,67".replaceAll("\\s+", "\t"));

		try (BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
			if (addHeader) {
				out.write(header + "\n");
			}
			for (String line : data) {
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
