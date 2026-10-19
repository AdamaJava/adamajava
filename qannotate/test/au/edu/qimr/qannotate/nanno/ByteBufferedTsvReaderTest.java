package au.edu.qimr.qannotate.nanno;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.qio.record.StringFileReader;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

public class ByteBufferedTsvReaderTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void lineScannerFindsFirstTerminatorAtEveryWordAlignment() {
        byte[] bytes = new byte[64];
        for (int start = 0; start < 16; start++) {
            for (int length = 0; length <= 32; length++) {
                Arrays.fill(bytes, (byte) 'x');
                // Terminators outside the requested slice must be ignored.
                if (start > 0) bytes[start - 1] = '\n';
                bytes[start + length] = '\r';
                assertEquals(start + length, ByteBufferedTsvReader.findLineEnd(bytes, start, start + length));
                for (int offset = 0; offset < length; offset++) {
                    for (byte terminator : new byte[]{'\n', '\r'}) {
                        bytes[start + offset] = terminator;
                        assertEquals(start + offset,
                                ByteBufferedTsvReader.findLineEnd(bytes, start, start + length));
                        bytes[start + offset] = 'x';
                    }
                }
            }
        }
    }

    @Test
    public void lineScannerMatchesScalarSearchForAllBytePairsAndRandomSlices() {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) 'x');
        for (int first = 0; first < 256; first++) {
            for (int second = 0; second < 256; second++) {
                // Include zero bytes, high-bit bytes and the borrow cases after CR/LF.
                bytes[14] = (byte) first;
                bytes[15] = (byte) second;
                assertEquals(scalarLineEnd(bytes, 3, 27), ByteBufferedTsvReader.findLineEnd(bytes, 3, 27));
            }
        }
        Random random = new Random(1817);
        bytes = new byte[1024];
        for (int trial = 0; trial < 10_000; trial++) {
            random.nextBytes(bytes);
            int start = random.nextInt(bytes.length + 1);
            int end = start + random.nextInt(bytes.length - start + 1);
            assertEquals(scalarLineEnd(bytes, start, end), ByteBufferedTsvReader.findLineEnd(bytes, start, end));
        }
    }

    private static int scalarLineEnd(byte[] bytes, int start, int end) {
        while (start < end && bytes[start] != '\n' && bytes[start] != '\r') start++;
        return start;
    }

    @Test
    public void positionGroupsPreserveLookaheadAndEarlierResults() throws Exception {
        String first = "1\t10\tA\tC\tfirst";
        String duplicate = "1\t10\tA\tC\tduplicate";
        String otherAllele = "1\t10\tA\tG\tother";
        String last = "1\t30\tG\tT\tlast";
        for (boolean compressed : new boolean[]{false, true}) {
            for (int size : new int[]{1, 17, 64 * 1024}) {
                File file = writeFile("1\t1\tA\tC\tskipped\n" + first + "\r\n" + duplicate + "\r"
                        + otherAllele + "\n1\t20\tA\tT\tskipped\n" + last, compressed);
                try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, false, size)) {
                    long chr = 1L << 32;
                    assertTrue(reader.readRecordsAt(chr + 5).isEmpty());
                    assertEquals(chr + 10, reader.nextPosition());
                    List<String> firstGroup = reader.readRecordsAt(chr + 10);
                    assertEquals(List.of(first, duplicate, otherAllele), firstGroup);
                    assertTrue(reader.readRecordsAt(chr + 25).isEmpty());
                    assertEquals(chr + 30, reader.nextPosition());
                    assertEquals(List.of(last), reader.readRecordsAt(chr + 30));
                    assertEquals(Long.MAX_VALUE, reader.nextPosition());
                    assertTrue(reader.readRecordsAt(chr + 40).isEmpty());
                    assertEquals(List.of(first, duplicate, otherAllele), firstGroup);
                }
            }
        }
    }

    @Test
    public void sentinelRequestDiscardsRemainingRowsAndStillValidatesPositions() throws Exception {
        for (boolean compressed : new boolean[]{false, true}) {
            File file = writeFile("1\t10\tA\tC\n2\t20\tG\tT", compressed);
            try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, false, 7)) {
                assertEquals((1L << 32) + 10, reader.nextPosition());
                assertTrue(reader.readRecordsAt(-1).isEmpty());
                assertEquals(Long.MAX_VALUE, reader.nextPosition());
                assertTrue(reader.readRecordsAt(-1).isEmpty());
            }
            file = writeFile("1\t10\tA\tC\n2\tx\tG\tT", compressed);
            try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, false, 7)) {
                try {
                    reader.readRecordsAt(-1);
                    fail("Expected sentinel request to validate skipped positions");
                } catch (NumberFormatException expected) {
                    // Skipping must preserve the normal position-validation behavior.
                }
            }
        }
    }

    @Test
    public void annotationsMatchStandardReaderAcrossPositionsAndBufferBoundaries() throws Exception {
        for (boolean compressed : new boolean[]{false, true}) {
            for (boolean chrPrefix : new boolean[]{false, true}) {
                for (String newline : List.of("\n", "\r\n", "\r")) {
                    File file = writeFile(fixture(chrPrefix, newline), compressed);
                    try (AnnotationSource standard = new AnnotationSourceTSV(new StringFileReader(file),
                            1, 2, 4, 5, "empty,vep,AF", chrPrefix);
                         AnnotationSource buffered = new AnnotationSourceTSV(
                                 new ByteBufferedTsvReader(file, chrPrefix, 257),
                                 1, 2, 4, 5, "empty,vep,AF", chrPrefix)) {
                        for (ChrPositionRefAlt variant : queries()) {
                            long position = ChrPositionUtils.convertContigAndPositionToLong(
                                    variant.getChromosome(), variant.getStartPosition());
                            assertEquals("compressed=" + compressed + ", chrPrefix=" + chrPrefix + ", " + variant,
                                    standard.getAnnotation(position, variant), buffered.getAnnotation(position, variant));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void oneByteBufferPreservesHeadersCrLfUtf8AndFinalUnterminatedRow() throws Exception {
        String contents = "##source=référence\r\n#chr\tpos\tref\talt\tvalue\r\n"
                + "chr1\t+0010\tA\tC\tαβ😀\r\nchrX\t2147483647\tG\tT\t";
        for (boolean compressed : new boolean[]{false, true}) {
            File file = writeFile(contents, compressed);
            try (StringFileReader standard = new StringFileReader(file);
                 ByteBufferedTsvReader buffered = new ByteBufferedTsvReader(file, true, 1)) {
                assertEquals(standard.getHeader(), buffered.getHeader());
                List<String> expected = new ArrayList<>();
                List<String> actual = new ArrayList<>();
                standard.forEach(expected::add);
                buffered.forEach(actual::add);
                assertEquals(expected, actual);
                assertEquals(Long.MAX_VALUE, buffered.nextPosition());
                assertTrue(buffered.readRecordsAt(Long.MAX_VALUE - 1).isEmpty());
            }
        }
    }

    @Test
    public void concatenatedGzipMembersPreserveRowsAndUtf8() throws Exception {
        byte[] data = ("#chr\tpos\tref\talt\tvalue\n1\t10\tA\tC\té😀\n2\t20\tG\tT\tlast")
                .getBytes(StandardCharsets.UTF_8);
        int split = 0;
        while (data[split] >= 0) split++;
        split++; // Split the UTF-8 encoding of é between independent gzip members.
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream first = new GZIPOutputStream(compressed)) {
            first.write(data, 0, split);
        }
        try (GZIPOutputStream second = new GZIPOutputStream(compressed)) {
            second.write(data, split, data.length - split);
        }
        File file = folder.newFile();
        Files.write(file.toPath(), compressed.toByteArray());
        try (StringFileReader standard = new StringFileReader(file);
             ByteBufferedTsvReader buffered = new ByteBufferedTsvReader(file, false, 3)) {
            assertEquals(standard.getHeader(), buffered.getHeader());
            List<String> expected = new ArrayList<>();
            List<String> actual = new ArrayList<>();
            standard.forEach(expected::add);
            buffered.forEach(actual::add);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void emptyAndHeaderOnlyStreamsReachEnd() throws Exception {
        for (boolean compressed : new boolean[]{false, true}) {
            for (String contents : List.of("", "#chr\tpos\tref\talt\tvalue", "#header\n")) {
                try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(writeFile(contents, compressed), false, 7)) {
                    assertFalse(reader.iterator().hasNext());
                    assertTrue(reader.readRecordsAt(1L << 32 | 10).isEmpty());
                    assertEquals(Long.MAX_VALUE, reader.nextPosition());
                }
            }
        }
    }

    @Test
    public void inPlaceAndSplitPrefixesPreserveSignedPositionsAndContigChanges() throws Exception {
        List<String> positions = List.of("-2147483648", "-1", "0", "+0010", "2147483647");
        for (boolean compressed : new boolean[]{false, true}) {
            for (boolean chrPrefix : new boolean[]{false, true}) {
                List<String> rows = new ArrayList<>();
                for (String contig : List.of("1", "2", "X", "MT")) {
                    for (String pos : positions) {
                        rows.add((chrPrefix ? "chr" : "") + contig + "\t" + pos + "\tA\tC\tvalue");
                    }
                }
                File file = writeFile(String.join("\n", rows), compressed);
                for (int size : new int[]{1, 2, 7, 17, 64, 1024}) {
                    try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, chrPrefix, size)) {
                        var iterator = reader.iterator();
                        for (String row : rows) {
                            String[] fields = row.split("\t");
                            long expected = ChrPositionUtils.convertContigAndPositionToLong(
                                    fields[0], Integer.parseInt(fields[1]));
                            assertEquals(expected, reader.nextPosition());
                            assertEquals(row, iterator.next());
                        }
                        assertFalse(iterator.hasNext());
                    }
                }
            }
        }
    }

    @Test
    public void invalidPositionsFailInsteadOfSilentlyMatching() throws Exception {
        for (String position : List.of("", "x", "2147483648", "-2147483649", "1x")) {
            File file = writeFile("#chr\tpos\tref\talt\n1\t" + position + "\tA\tC\n", false);
            for (int size : new int[]{7, 1024}) {
                try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, false, size)) {
                    try {
                        reader.nextPosition();
                        fail("Expected invalid position to fail: " + position);
                    } catch (NumberFormatException expected) {
                        // Same failure category as the standard position parser.
                    }
                }
            }
        }
    }

    @Test
    public void truncatedGzipPropagatesReadFailure() throws Exception {
        File file = writeFile("#chr\tpos\tref\talt\n1\t1\tA\tC\n", true);
        byte[] bytes = Files.readAllBytes(file.toPath());
        Files.write(file.toPath(), Arrays.copyOf(bytes, bytes.length - 6));
        try {
            try (ByteBufferedTsvReader reader = new ByteBufferedTsvReader(file, false, 7)) {
                reader.readRecordsAt(ChrPositionUtils.convertContigAndPositionToLong("2", 1));
            }
            fail("Expected truncated gzip to fail");
        } catch (IOException | UncheckedIOException expected) {
            // A read error must propagate even when all data rows are being skipped.
        }
    }

    @Test
    public void configurationOptsInAndUnsupportedLayoutsFallBack() throws Exception {
        File file = writeFile("#chr\tpos\tid\tref\talt\tAF\nchr1\t10\t.\tA\tC\t0.5\n", true);
        for (boolean enabled : new boolean[]{false, true}) {
            AnnotationInputs inputs = inputs(Map.of("file", file.getPath(), "chrIndex", 1, "positionIndex", 2,
                    "refIndex", 4, "altIndex", 5, "chrStartsWithChr", true, "fields", "AF", "byteBuffered", enabled));
            assertEquals(enabled, inputs.getInputs().getFirst().isByteBuffered());
            List<AnnotationSource> sources = new ArrayList<>();
            AnnotateUtils.populateAnnotationSources(inputs, sources);
            try (AnnotationSource source = sources.getFirst()) {
                assertEquals(enabled, source.reader instanceof ByteBufferedTsvReader);
                assertEquals("AF=0.5", source.getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 10),
                        variant("1", 10, "A", "C")));
            }
        }

        File reordered = writeFile("#pos\tchr\tref\talt\tAF\n10\t1\tA\tC\t0.5\n", false);
        List<AnnotationSource> sources = new ArrayList<>();
        AnnotateUtils.populateAnnotationSources(inputs(Map.of("file", reordered.getPath(), "chrIndex", 2,
                "positionIndex", 1, "refIndex", 3, "altIndex", 4, "fields", "AF", "byteBuffered", true)), sources);
        try (AnnotationSource source = sources.getFirst()) {
            assertFalse(source.reader instanceof ByteBufferedTsvReader);
            assertEquals("AF=0.5", source.getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 10),
                    variant("1", 10, "A", "C")));
        }

        File vcf = folder.newFile("annotation.vcf");
        Files.writeString(vcf.toPath(), "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n1\t10\t.\tA\tC\t.\t.\tAF=0.5\n");
        sources = new ArrayList<>();
        AnnotateUtils.populateAnnotationSources(inputs(Map.of("file", vcf.getPath(), "chrIndex", 1,
                "positionIndex", 2, "refIndex", 4, "altIndex", 5, "fields", "AF", "byteBuffered", true)), sources);
        try (AnnotationSource source = sources.getFirst()) {
            assertTrue(source instanceof AnnotationSourceVCF);
            assertEquals("AF=0.5", source.getAnnotation(ChrPositionUtils.convertContigAndPositionToLong("1", 10),
                    variant("1", 10, "A", "C")));
        }
        assertFalse(inputs(Map.of("file", file.getPath())).getInputs().getFirst().isByteBuffered());
    }

    private AnnotationInputs inputs(Map<String, Object> source) throws Exception {
        File json = folder.newFile();
        new ObjectMapper().writeValue(json, Map.of("outputFieldOrder", "AF", "inputs", List.of(source)));
        return AnnotateUtils.getInputs(json.getPath());
    }

    private File writeFile(String contents, boolean compressed) throws IOException {
        File file = folder.newFile();
        try (OutputStream out = compressed ? new GZIPOutputStream(Files.newOutputStream(file.toPath()))
                : Files.newOutputStream(file.toPath())) {
            out.write(contents.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private static String fixture(boolean prefixed, String newline) {
        String chr = prefixed ? "chr" : "";
        List<String> rows = new ArrayList<>();
        rows.add("##source=référence");
        rows.add("#chr\tpos\tid\tref\talt\tAF\tvep\tempty");
        rows.add(chr + "1\t1\t.\tA\tC\t0\t" + "skipped|β|😀".repeat(20_000) + "\t");
        rows.add(chr + "1\t10\t.\tA\tC\t0.1\tfirst\t");
        rows.add(chr + "1\t10\t.\tA\tC\t0.2\tduplicate\t");
        rows.add(chr + "1\t10\t.\tA\tCG\t0.3\tlong-alt\t");
        rows.add(chr + "1\t10\t.\tAT\tC\t0.4\tlong-ref\t");
        rows.add(chr + "1\t20\t.\tA\tC\t0.5\t" + "skipped".repeat(20_000) + "\t");
        rows.add(chr + "1\t30\t.\tA\tC\t0.6\t" + "retained|é|😀=".repeat(10_000) + "\t");
        for (String contig : List.of("2", "10", "X", "Y", "MT")) {
            rows.add(chr + contig + "\t10\t.\tG\tT\t0.7\t" + contig + "|é😀\t");
        }
        return String.join(newline, rows);
    }

    private static List<ChrPositionRefAlt> queries() {
        List<ChrPositionRefAlt> queries = new ArrayList<>();
        queries.add(variant("1", 4, "A", "C"));
        queries.add(variant("1", 10, "A", "C"));
        queries.add(variant("1", 10, "A", "C"));
        queries.add(variant("1", 10, "A", "CG"));
        queries.add(variant("1", 10, "AT", "C"));
        queries.add(variant("1", 10, "A", "G"));
        queries.add(variant("1", 10, "AA", "C"));
        queries.add(variant("1", 11, "A", "C"));
        queries.add(variant("1", 30, "A", "C"));
        queries.add(variant("1", 31, "A", "C"));
        for (String contig : List.of("2", "10", "X", "Y", "MT")) {
            queries.add(variant(contig, 9, "G", "T"));
            queries.add(variant(contig, 10, "G", "T"));
            queries.add(variant(contig, 11, "G", "T"));
        }
        queries.add(variant("MT", 12, "G", "T"));
        return queries;
    }

    private static ChrPositionRefAlt variant(String chr, int pos, String ref, String alt) {
        return new ChrPositionRefAlt(chr, pos, pos + ref.length() - 1, ref, alt);
    }
}
