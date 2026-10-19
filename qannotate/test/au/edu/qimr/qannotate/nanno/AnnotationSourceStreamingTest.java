package au.edu.qimr.qannotate.nanno;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.qio.record.StringFileReader;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnnotationSourceStreamingTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void wideTsvPreservesAlleleMatchingAndRequestedFieldOrder() throws Exception {
        for (boolean prefixed : new boolean[]{false, true}) {
            File file = folder.newFile();
            List<String> lines = new ArrayList<>();
            lines.add("#chr\tpos\tref\talt\tearly\t" + "unused\t".repeat(100) + "late\tempty");
            for (String contig : List.of("1", "2", "10", "X", "Y", "MT")) {
                String chr = (prefixed ? "chr" : "") + contig;
                for (String[] alleles : new String[][]{{"A", "C"}, {"A", "CG"}, {"AT", "C"}}) {
                    String label = contig + alleles[0] + alleles[1];
                    lines.add(chr + "\t10\t" + alleles[0] + "\t" + alleles[1] + "\t" + label
                            + "\tignored".repeat(100) + "\t" + label + "=late\t");
                }
                // Duplicate annotations must continue to return the first matching record.
                lines.add(chr + "\t10\tA\tC\tduplicate" + "\tignored".repeat(100) + "\tduplicate\t");
            }
            Files.write(file.toPath(), lines);
            try (AnnotationSource source = new AnnotationSourceTSV(new StringFileReader(file),
                    1, 2, 3, 4, "late,empty,early", prefixed)) {
                for (String contig : List.of("1", "2", "10", "X", "Y", "MT")) {
                    assertEquals("late=\tempty=\tearly=", annotation(source, contig, 9, "A", "C"));
                    for (String[] alleles : new String[][]{{"A", "C"}, {"A", "CG"}, {"AT", "C"}}) {
                        String label = contig + alleles[0] + alleles[1];
                        String expected = "late=" + label + "=late\tempty=\tearly=" + label;
                        assertEquals(expected, annotation(source, contig, 10, alleles[0], alleles[1]));
                        // Repeated requests reuse the current position, including across chunks.
                        assertEquals(expected, annotation(source, contig, 10, alleles[0], alleles[1]));
                    }
                    assertEquals("late=\tempty=\tearly=", annotation(source, contig, 10, "A", "G"));
                    assertEquals("late=\tempty=\tearly=", annotation(source, contig, 11, "A", "C"));
                }
                assertEquals("late=\tempty=\tearly=", annotation(source, "MT", 20, "A", "C"));
            }
        }
    }

    @Test
    public void reorderedColumnsAndShortRowsRetainExtractionBehaviour() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), List.of(
                "#alt\tref\tchr\tpos\tfirst\tlast",
                "C\tA\t1\t10\talpha\tomega",
                "CG\tA\t1\t10\tsecond\t",
                "T\tA\t2\t20\tshort"));
        try (AnnotationSource source = new AnnotationSourceTSV(new StringFileReader(file),
                3, 4, 2, 1, "last,first", false)) {
            assertEquals("last=omega\tfirst=alpha", annotation(source, "1", 10, "A", "C"));
            assertEquals("last=\tfirst=second", annotation(source, "1", 10, "A", "CG"));
            assertEquals("last=\tfirst=", annotation(source, "1", 10, "A", "T"));
            assertEquals("first=short", annotation(source, "2", 20, "A", "T"));
        }
    }

    @Test
    public void vcfPayloadExtractionRetainsExactAlleleMatches() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), List.of(
                "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO",
                "chr1\t10\t.\tA\tC\t.\t.\tGENE=first",
                "chr1\t10\t.\tA\tCG\t.\t.\tGENE=second",
                "chr1\t10\t.\tAT\tC\t.\t.\tGENE=third",
                "chr2\t20\t.\tA\tT\t.\t.\tGENE=last"));
        try (AnnotationSource source = new AnnotationSourceVCF(new StringFileReader(file),
                1, 2, 4, 5, "GENE", true)) {
            assertEquals("GENE=first", annotation(source, "1", 10, "A", "C"));
            assertEquals("GENE=second", annotation(source, "1", 10, "A", "CG"));
            assertEquals("GENE=third", annotation(source, "1", 10, "AT", "C"));
            assertEquals("GENE=", annotation(source, "1", 10, "A", "T"));
            assertEquals("GENE=last", annotation(source, "2", 20, "A", "T"));
        }
    }

    private static String annotation(AnnotationSource source, String contig, int position, String ref, String alt) {
        return source.getAnnotation(ChrPositionUtils.convertContigAndPositionToLong(contig, position),
                new ChrPositionRefAlt("chr" + contig, position, position + ref.length() - 1, ref, alt));
    }
}
