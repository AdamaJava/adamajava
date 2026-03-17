package au.edu.qimr.qannotate.nanno;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import au.edu.qimr.qannotate.nanno.AnnotationSourceSnpEffVCF;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.qio.record.StringFileReader;

public class AnnotationSourceSnpEffVCFTest {

	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void extractFieldsFromInfoField() {
		String info = "AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=46;ExcessHet=0.0000;FS=2.561;MLEAC=2;MLEAF=1.00;MQ=60.00;MQRankSum=0.00;QD=17.47;ReadPosRankSum=0.00;SOR=0.521;ANN=A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000282670.7|protein_coding|5/5|c.*115G>A|||||115|,A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A|||||6971|,A|downstream_gene_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000448074.1|processed_transcript||n.*1991G>A|||||1991|,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A||||||,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000489510.1|retained_intron|2/2|n.568G>A||||||";
		assertEquals("effect=3_prime_UTR_variant", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("effect"), "", "A"));
		assertEquals("annotation=3_prime_UTR_variant", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("annotation"), "", "A"));
		assertEquals("cdna_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("cdna_position"), "", "A"));
		assertEquals("cds_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("cds_position"), "", "A"));
		assertEquals("distance_to_feature=115", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("distance_to_feature"), "", "A"));
		assertEquals("feature_type=transcript", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("feature_type"), "", "A"));
		assertEquals("rank=5/5", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("rank"), "", "A"));
		assertEquals("hgvs.c=c.*115G>A", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("hgvs.c"), "", "A"));
	}
	
	@Test
	public void extractFieldsFromInfoFieldWrongAlt() {
		String info = "AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=46;ExcessHet=0.0000;FS=2.561;MLEAC=2;MLEAF=1.00;MQ=60.00;MQRankSum=0.00;QD=17.47;ReadPosRankSum=0.00;SOR=0.521;ANN=A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000282670.7|protein_coding|5/5|c.*115G>A|||||115|,A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A|||||6971|,A|downstream_gene_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000448074.1|processed_transcript||n.*1991G>A|||||1991|,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A||||||,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000489510.1|retained_intron|2/2|n.568G>A||||||";
		assertEquals("", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("effect"), "", "T"));
		assertEquals("effect=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("effect"), "effect=", "T"));
		assertEquals("hgvs.c=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("hgvs.c"), "hgvs.c=", "C"));
	}
	
	@Test
	public void getWorstConsequence() {
		String info = "AC=2;AF=1.00;AN=2;DP=43;ExcessHet=0.0000;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=25.36;SOR=0.739;ANN=C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||,C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000477976.5|retained_intron|6/16|n.2335+4C>G||||||,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000469563.1|retained_intron||n.*4468C>G|||||4468|,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000487214.1|processed_transcript||n.*648C>G|||||648|";
		String alt = "C";
		assertEquals("C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||", AnnotationSourceSnpEffVCF.getWorstConsequence(info, alt));
	}
	
	@Test
	public void extractFieldsFromInfoField2() {
		String info = "AC=2;AF=1.00;AN=2;DP=43;ExcessHet=0.0000;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=25.36;SOR=0.739;ANN=C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||,C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000477976.5|retained_intron|6/16|n.2335+4C>G||||||,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000469563.1|retained_intron||n.*4468C>G|||||4468|,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000487214.1|processed_transcript||n.*648C>G|||||648|";
		String alt = "C";
		
		assertEquals("cdna_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("cdna_position"), ".", alt));
	}

	@Test
	public void getWorstConsequenceMultipleGenesCommaDelimited() {
		String info = "AC=2;AF=1.00;AN=2;ANN="
				+ "G|missense_variant|MODERATE|GENE1|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|synonymous_variant|LOW|GENE1|ID1|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE2|ID2|transcript|TR3|protein_coding|1/1|c.1A>G|||||10|,"
				+ "T|synonymous_variant|LOW|GENE3|ID3|transcript|TR4|protein_coding|1/1|c.1A>T|||||10|";

		String alt = "G";
		String expected = "G|missense_variant|MODERATE|GENE1|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE2|ID2|transcript|TR3|protein_coding|1/1|c.1A>G|||||10|";

		assertEquals(expected, AnnotationSourceSnpEffVCF.getWorstConsequence(info, alt));
	}

	@Test
	public void getAnnotationMultiGene() throws Exception {
		File vcf = testFolder.newFile("snpeff.vcf");

		String line = "chr1\t100\t.\tA\tG,T\t.\t.\tANN=G|downstream_gene_variant|MODIFIER|NADK|ENSG00000008130.15|transcript|ENST00000341426.9|protein_coding||c.*4307G>C|||||2635|," +
				"G|intergenic_region|MODIFIER|CDK11A-NADK|ENSG00000008128.23-ENSG00000008130.15|intergenic_region|ENSG00000008128.23-ENSG00000008130.15|||n.1748597C>G||||||";

		Files.write(vcf.toPath(), List.of(line), StandardCharsets.UTF_8);

		try (StringFileReader reader = new StringFileReader(vcf)) {
			AnnotationSourceSnpEffVCF source = new AnnotationSourceSnpEffVCF(
					reader,
					1,  // chrPositionInRecord (1-based)
					2,  // positionPositionInRecord (1-based)
					4,  // refPositionInFile (1-based)
					5,  // altPositionInFile (1-based)
					"effect,gene_name",
					true
			);

			ChrPositionRefAlt cp = new ChrPositionRefAlt("chr1", 100, 100, "A", "G");
			long cpAsLong = ChrPositionUtils.convertContigAndPositionToLong("1", 100);

			String first = source.getAnnotation(cpAsLong, cp);
			String second = source.getAnnotation(cpAsLong, cp);

			assertEquals("effect=downstream_gene_variant|intergenic_region\tgene_name=NADK|CDK11A-NADK", first);
			assertEquals("effect=downstream_gene_variant|intergenic_region\tgene_name=NADK|CDK11A-NADK", second);
		}
	}

	@Test
	public void getAnnotationUsesSameLogicForCurrentAndNext() throws Exception {
		File vcf = testFolder.newFile("snpeff.vcf");

		String line = "chr1\t100\t.\tA\tG,T\t.\t.\tANN="
				+ "G|missense_variant|MODERATE|GENE|ID|transcript|TR|protein_coding|1/1|c.1A>G|||||10|,"
				+ "T|synonymous_variant|LOW|GENE|ID|transcript|TR|protein_coding|1/1|c.1A>T|||||10|";

		Files.write(vcf.toPath(), List.of(line), StandardCharsets.UTF_8);

		try (StringFileReader reader = new StringFileReader(vcf)) {
			AnnotationSourceSnpEffVCF source = new AnnotationSourceSnpEffVCF(
					reader,
					1,  // chrPositionInRecord (1-based)
					2,  // positionPositionInRecord (1-based)
					4,  // refPositionInFile (1-based)
					5,  // altPositionInFile (1-based)
					"effect",
					true
			);

			ChrPositionRefAlt cp = new ChrPositionRefAlt("chr1", 100, 100, "A", "T");
			long cpAsLong = ChrPositionUtils.convertContigAndPositionToLong("1", 100);

			String first = source.getAnnotation(cpAsLong, cp);
			String second = source.getAnnotation(cpAsLong, cp);

			assertEquals("effect=synonymous_variant", first);
			assertEquals("effect=synonymous_variant", second);

			cp = new ChrPositionRefAlt("chr1", 100, 100, "A", "G");

			first = source.getAnnotation(cpAsLong, cp);
			second = source.getAnnotation(cpAsLong, cp);

			assertEquals("effect=missense_variant", first);
			assertEquals("effect=missense_variant", second);
		}
	}

	@Test
	public void getAnnotationNoMatchReturnsEmpty() throws Exception {
		File vcf = testFolder.newFile("snpeff-no-match.vcf");

		String line = "chr1\t100\t.\tA\tG,T\t.\t.\tANN="
				+ "G|missense_variant|MODERATE|GENE|ID|transcript|TR|protein_coding|1/1|c.1A>G|||||10|,"
				+ "T|synonymous_variant|LOW|GENE|ID|transcript|TR|protein_coding|1/1|c.1A>T|||||10|";

		Files.write(vcf.toPath(), List.of(line), StandardCharsets.UTF_8);

		try (StringFileReader reader = new StringFileReader(vcf)) {
			AnnotationSourceSnpEffVCF source = new AnnotationSourceSnpEffVCF(
					reader,
					1,  // chrPositionInRecord (1-based)
					2,  // positionPositionInRecord (1-based)
					4,  // refPositionInFile (1-based)
					5,  // altPositionInFile (1-based)
					"effect",
					true
			);

			// same position, but alt does not exist in the record
			ChrPositionRefAlt cp = new ChrPositionRefAlt("chr1", 100, 100, "A", "C");
			long cpAsLong = ChrPositionUtils.convertContigAndPositionToLong("1", 100);

			assertEquals("effect=", source.getAnnotation(cpAsLong, cp));
		}
	}

	@Test
	public void getAnnotationNoPositionMatchReturnsEmpty() throws Exception {
		File vcf = testFolder.newFile("snpeff-no-pos.vcf");

		String line = "chr1\t100\t.\tA\tG\t.\t.\tANN="
				+ "G|missense_variant|MODERATE|GENE|ID|transcript|TR|protein_coding|1/1|c.1A>G|||||10|";

		Files.write(vcf.toPath(), List.of(line), StandardCharsets.UTF_8);

		try (StringFileReader reader = new StringFileReader(vcf)) {
			AnnotationSourceSnpEffVCF source = new AnnotationSourceSnpEffVCF(
					reader,
					1,  // chrPositionInRecord (1-based)
					2,  // positionPositionInRecord (1-based)
					4,  // refPositionInFile (1-based)
					5,  // altPositionInFile (1-based)
					"effect",
					true
			);

			// different position (no match)
			ChrPositionRefAlt cp = new ChrPositionRefAlt("chr1", 101, 101, "A", "G");
			long cpAsLong = ChrPositionUtils.convertContigAndPositionToLong("1", 101);

			assertEquals("effect=", source.getAnnotation(cpAsLong, cp));
		}
	}

	@Test
	public void getWorstConsequenceSkipsDuplicateGeneUsesFirst() {
		String info = "ANN="
				+ "G|missense_variant|MODERATE|GENE1|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE1|ID1|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|";

		String alt = "G";
		String expected = "G|missense_variant|MODERATE|GENE1|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|";

		assertEquals(expected, AnnotationSourceSnpEffVCF.getWorstConsequence(info, alt));
	}

	@Test
	public void getWorstConsequenceSkipsEmptyGene() {
		String info = "ANN="
				+ "G|missense_variant|MODERATE||ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE2|ID2|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|";

		String alt = "G";
		String expected = "G|stop_gained|HIGH|GENE2|ID2|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|";

		assertEquals(expected, AnnotationSourceSnpEffVCF.getWorstConsequence(info, alt));
	}

	@Test
	public void extractFieldsFromInfoFieldMultiGeneAltNotPresent() {
		String info = "ANN="
				+ "G|missense_variant|MODERATE|GENE1|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE2|ID2|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|";

		assertEquals("effect=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("effect"), "effect=", "T"));
	}

	@Test
	public void extractFieldsFromInfoFieldMultiGeneOrderIsStable() {
		// SnpEff already orders consequences by severity; we preserve first-seen order.
		String info = "ANN="
				+ "G|missense_variant|MODERATE|GENE_B|ID1|transcript|TR1|protein_coding|1/1|c.1A>G|||||10|,"
				+ "G|stop_gained|HIGH|GENE_A|ID2|transcript|TR2|protein_coding|1/1|c.1A>G|||||10|";

		String result = AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, List.of("effect", "gene_name"), "effect=\tgene_name=", "G");

		assertEquals("effect=missense_variant|stop_gained\tgene_name=GENE_B|GENE_A", result);
	}

}
