package au.edu.qimr.qannotate.nanno;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.record.RecordReader;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.qio.vcf.VcfFileReader;


public class AnnotateTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
  	
	 
	 
//	 @Test
//	 public void annotate() {
//		 
//		VcfRecord r = new VcfRecord(new String[]{"chr1","16534","rs201459529","C","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "T", new String[]{"1","1","1","1"}));
//		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1"));
//		assertEquals(false, GermlineMode.annotateGermlineSnp(r, "C", "A", new String[]{"1","1","1","1"}));
//		assertEquals(false, r.getInfo().contains("GERM=A:1:1:1:1"));
//		
//		r = new VcfRecord(new String[]{"chr1","16534","rs201459529","C","T,A",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "T", new String[]{"1","1","1","1"}));
//		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1"));
//		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "A", new String[]{"1","1","1","1"}));
//		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1,A:1:1:1:1"));
//	 }
	 
//	@Test
//	public void annotateRecords() throws IOException {
//		File annotationFile = testFolder.newFile();
//		createAnnotationFile(annotationFile);
//		
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, null)) {
//			Assert.fail("SHould have thrown an IAE!");
//		} catch (IllegalArgumentException iae) {}
//		
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13"))  {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655650","rs201459529","A","C",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt() );
//			assertEquals("13\tOR4F5", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "14"))  {
//			VcfRecord r = new VcfRecord(new String[]{"1","655650","rs201459529","A","C",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt() );
//			assertEquals("14\tENSG00000186092", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "1"))  {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655650","rs201459529","A","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("1\t", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "1"))  {
//			VcfRecord r = new VcfRecord(new String[]{"1","655650","rs201459529","A","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("1\t", results);
//		}
//		
//		/*
//		 * go beyond end of annotation file
//		 */
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "15")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","65565000","rs201459529","A","C",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("15\t", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "2")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr2","655650","rs201459529","A","C",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("2\t", results);
//		}
//		
//		/*
//		 * multiple annotation entries
//		 */
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655651","rs201459529","A","C",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("13\tOR4F51", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655651","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("13\tOR4F52", results);
//		}
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655651","rs201459529","A","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("13\tOR4F53", results);
//		}
//		/*
//		 * incorrect ref
//		 */
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13")) {
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655651","rs201459529","C","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("13\t", results);
//		}
//	}
	
//	@Test
//	public void annotateRecordsWrongOrder() throws IOException {
//		File annotationFile = testFolder.newFile();
//		createAnnotationFileIncorrectOrder(annotationFile);
//		
//		try (RecordReader<String> reader = new StringFileReader(annotationFile);
//				Anno anno = new Anno(reader, 1, 2, 3, 4, "13,14")) {
//			/*
//			 * get chr 1 rec - should work fine
//			 */
//			VcfRecord r = new VcfRecord(new String[]{"chr1","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			String results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\tOR4F5\tENSG00000186092", results);
//			/*
//			 * get chr2 rec - nothing returned as iterator is in chr10
//			 */
//			r = new VcfRecord(new String[]{"chr2","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			/*
//			 * get chr3 rec - nothing returned as iterator is in chr10
//			 */
//			r = new VcfRecord(new String[]{"chr3","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			/*
//			 * get chr4 rec - nothing returned as iterator is in chr10
//			 */
//			r = new VcfRecord(new String[]{"chr4","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			/*
//			 * get chr5 rec - nothing returned as iterator is in chr10
//			 */
//			r = new VcfRecord(new String[]{"chr5","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			/*
//			 * get chr6 rec - nothing returned as iterator is in chr10
//			 */
//			r = new VcfRecord(new String[]{"chr6","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			/*
//			 * get chr7 rec - nothing in annotation file - trick test!
//			 */
//			r = new VcfRecord(new String[]{"chr7","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\t\t", results);
//			
//			/*
//			 * now try and get the chr10 record - should return record
//			 */
//			r = new VcfRecord(new String[]{"chr10","655652","rs201459529","A","G",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
//			results = anno.getAnnotation(r.getChrPositionRefAlt());
//			assertEquals("genename\tEnsembl_geneid\tOR4F5\tENSG00000186092", results);
//		}
//	}
	 
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	static void createVcf(File f) throws IOException {
        List<String> data =Arrays.asList(
        		"##fileformat=VCFv4.2",
        		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,
        		"chr1	95813205	rs11165387	C	T	.	.	FLANK=CTTCGTGTCTT;BaseQRankSum=1.846;ClippingRankSum=-1.363;DP=35;FS=3.217;MQ=60.00;MQRankSum=-1.052;QD=18.76;ReadPosRankSum=-1.432;SOR=0.287;IN=1,2;DB;VAF=0.6175;HOM=0,TCCTTCTTCGtGTCTTTCCTT;EFF=downstream_gene_variant(MODIFIER||3602|||RP11-14O19.1|lincRNA|NON_CODING|ENST00000438093||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.	0/1:16,18:Germline:21:34:.:.:PASS:99:.:.:.:656.77	./.:.:HomozygousLoss:21:.:.:.:PASS:.:NCIG:.:.:.",
        		"chr1	60781981	rs5015226	T	G	.	.	FLANK=ACCAAGTGCCT;DP=53;FS=0.000;MQ=60.00;QD=31.16;SOR=0.730;IN=1,2;DB;HOM=0,CGAAAACCAAgTGCCTGCATT;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:.	1/1:0,53:Germline:34:53:.:.:PASS:99:.:.:.:2418.77	1/1:0,60:Germline:34:60:.:.:PASS:99:.:.:.:2749.77"
        		);
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
            for (String line : data)   out.write(line + "\n");
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
	
//	@Test
//	public void compareNameAndPositions() {
//		assertEquals(0, Anno.compareChromosomeNameAndStartPositions("chr1", 1, "chr1", 1));
//		assertEquals(0, Anno.compareChromosomeNameAndStartPositions("chr1", 1, "1", 1));
//		assertEquals(0, Anno.compareChromosomeNameAndStartPositions("1", 1, "chr1", 1));
//		assertEquals(0, Anno.compareChromosomeNameAndStartPositions("1", 1, "1", 1));
//		assertEquals(-1, Integer.compare(1, 2));
//		assertEquals(-1, Anno.compareChromosomeNameAndStartPositions("1", 1, "1", 2));
//		assertEquals(-1, Anno.compareChromosomeNameAndStartPositions("1", 1, "2", 2));
//		assertEquals(0, Anno.compareChromosomeNameAndStartPositions("2", 2, "2", 2));
//		assertEquals(1, Anno.compareChromosomeNameAndStartPositions("2", 3, "2", 2));
//		assertEquals(1, Anno.compareChromosomeNameAndStartPositions("3", 32, "2", 2));
//	}
	
	@Test
	public void getItemFromList() {
		assertEquals(Optional.empty(), Annotate.getAnnotationFromList(null, null));
		assertEquals(Optional.empty(), Annotate.getAnnotationFromList(new ArrayList<>(), null));
		assertEquals(Optional.empty(), Annotate.getAnnotationFromList(new ArrayList<>(), ""));
		assertEquals(Optional.empty(), Annotate.getAnnotationFromList(Arrays.asList("ANNO_1=1"), ""));
		assertEquals(Optional.empty(), Annotate.getAnnotationFromList(Arrays.asList("ANNO_1=1"), "ANNO_2"));
		assertEquals(Optional.of("1"), Annotate.getAnnotationFromList(Arrays.asList("ANNO_1=1"), "ANNO_1"));
		assertEquals(Optional.of("2"), Annotate.getAnnotationFromList(Arrays.asList("ANNO_1=1","ANNO_2=2"), "ANNO_2"));
		assertEquals(Optional.of("1"), Annotate.getAnnotationFromList(Arrays.asList("ANNO_1=1","ANNO_2=2"), "ANNO_1"));
	}
	
	@Test
	public void convertAnnotationsToSingleList() {
		List<String> annos = new ArrayList<>();
		annos.add("FIELD1=1\tFIELD2=2");
		annos.add("FIELD3=3\tFIELD4=4");
		annos.add("FIELD5=5\tFIELD6=6");
		
		List<String> singleAnnoList = Annotate.convertAnnotations(annos);
		assertEquals(6, singleAnnoList.size());
		assertEquals(true, singleAnnoList.contains("FIELD1=1"));
		
		annos.add("FIELD7=7\tFIELD8=8\tFIELD9=9\tFIELD10=10");
		singleAnnoList = Annotate.convertAnnotations(annos);
		assertEquals(10, singleAnnoList.size());
		assertEquals(true, singleAnnoList.contains("FIELD10=10"));
		
		/*
		 * look at empty and null lists
		 */
		annos = new ArrayList<>();
		singleAnnoList = Annotate.convertAnnotations(annos);
		assertEquals(0, singleAnnoList.size());
		singleAnnoList = Annotate.convertAnnotations(null);
		assertEquals(0, singleAnnoList.size());
		
	}
	
	@Test
	public void getEmptyFieldValues() {
		assertEquals("", Annotate.getEmptyHeaderValues(0));
		assertEquals("\t", Annotate.getEmptyHeaderValues(1));
		assertEquals("\t\t", Annotate.getEmptyHeaderValues(2));
		assertEquals("\t\t\t\t\t\t\t\t\t\t\t\t", Annotate.getEmptyHeaderValues(12));
	}
	
	@Test
	public void getSearchTerm() {
		assertEquals("\"GENE\"+(\"277C>T\"|\"277C->T\"|\"277C-->T\"|\"277C/T\"|\"Arg93Trp\")", Annotate.getSearchTerm(Optional.of("c.277C>T"), Optional.of("p.Arg93Trp")));
		assertEquals("\"GENE\"+(\"277T>C\"|\"277T->C\"|\"277T-->C\"|\"277T/C\"|\"Arg93Trp\")", Annotate.getSearchTerm(Optional.of("c.277T>C"), Optional.of("p.Arg93Trp")));
		assertEquals("", Annotate.getSearchTerm(Optional.empty(), Optional.empty()));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", Annotate.getSearchTerm(Optional.of("c277TC"), Optional.of("p.Arg93Trp")));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", Annotate.getSearchTerm(Optional.of(""), Optional.of("p.Arg93Trp")));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", Annotate.getSearchTerm(Optional.empty(), Optional.of("p.Arg93Trp")));
		assertEquals("", Annotate.getSearchTerm(Optional.of("c277TC"), Optional.empty()));
		assertEquals("", Annotate.getSearchTerm(Optional.of(""), Optional.of("")));
	}
	
}
