package org.qcmg.coverage;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import org.junit.*;
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that a query can be supplied for each of the coverage types when several coverage
 * types are calculated together, and that the results match those of running the coverage
 * types separately.
 */
public class MultiTypeQueryCoverageTest {
    static String inputBam;
    static String inputBai;
    static String gff3;
    static Path tmpDir;

    @BeforeClass
    public static void setup() throws Exception {
        tmpDir = Files.createTempDirectory(null);
        inputBam = Files.createTempFile(tmpDir, null, ".bam").toString();
        inputBai = inputBam.replace("bam", "bai");
        SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));

        Gff3Record record = new Gff3Record();
        record.setSeqId("chr1");
        record.setType("exon");
        record.setScore(".");
        record.setSource(".");
        record.setStrand("+");
        record.setStart(54000);
        record.setEnd(54120);
        gff3 = tmpDir + "/test.gff3";
        try (RecordWriter<Gff3Record> writer = new RecordWriter<>(new File(gff3))) {
            writer.add(record);
        }
    }

    @AfterClass
    public static void tearDown() {
        tmpDir.toFile().delete();
    }

    /**
     * Runs qcoverage with the given types and query options, writing one output file per type.
     *
     * @param queryArgs the query options and their values, eg. "--query", "ISIZE<50"
     * @return the contents of the output file of each of the types, in the order of the types
     */
    private List<List<String>> run(final String name, final String[] types, final String... queryArgs) throws Exception {
        final List<String> args = new ArrayList<>(Arrays.asList("--log", tmpDir + "/logfile",
                "--input-gff3", gff3, "--input-bam", inputBam, "--input-bai", inputBai));
        for (final String type : types) {
            args.add("--type");
            args.add(type);
        }
        args.addAll(Arrays.asList(queryArgs));
        for (final String type : types) {
            args.add("--output");
            args.add(tmpDir + "/" + name + "." + type);
        }
        assertEquals(0, Coverage.run(args.toArray(new String[0])));

        final List<List<String>> results = new ArrayList<>();
        for (final String type : types) {
            // the low read depth coverage is written in bed format
            final String extension = "low_readdepth".equals(type) ? ".bed" : ".txt";
            final File output = new File(tmpDir + "/" + name + "." + type + extension);
            assertTrue(output.exists());
            results.add(Files.readAllLines(output.toPath()));
            output.delete();
        }
        return results;
    }

    private int runExpectingFailure(final String... extraArgs) {
        final List<String> args = new ArrayList<>(Arrays.asList("--log", tmpDir + "/logfile",
                "--input-gff3", gff3, "--input-bam", inputBam, "--input-bai", inputBai,
                "--output", tmpDir + "/wrong"));
        args.addAll(Arrays.asList(extraArgs));
        return Coverage.run(args.toArray(new String[0]));
    }

    @Test
    public final void differentQueryPerType() throws Exception {
        final List<String> seq = run("seq", new String[] { "seq" }, "--query", "Cigar_M>45").get(0);
        final List<String> phys = run("phys", new String[] { "phys" }, "--query", "ISIZE<50").get(0);
        // ensure that the test would detect the queries being applied to the wrong types
        assertNotEquals(seq, run("seq", new String[] { "seq" }, "--query", "ISIZE<50").get(0));
        assertNotEquals(phys, run("phys", new String[] { "phys" }, "--query", "Cigar_M>45").get(0));

        final List<List<String>> combined = run("combined", new String[] { "seq", "phys" },
                "--sequence-query", "Cigar_M>45", "--physical-query", "ISIZE<50");
        assertEquals(seq, combined.get(0));
        assertEquals(phys, combined.get(1));

        // the order of the types does not affect which query is applied to which type
        final List<List<String>> reordered = run("reordered", new String[] { "phys", "seq" },
                "--sequence-query", "Cigar_M>45", "--physical-query", "ISIZE<50");
        assertEquals(phys, reordered.get(0));
        assertEquals(seq, reordered.get(1));
    }

    @Test
    public final void typeQueryOverridesQuery() throws Exception {
        final List<String> seq = run("seq", new String[] { "seq" }, "--query", "Cigar_M>45").get(0);
        final List<String> phys = run("phys", new String[] { "phys" }, "--query", "ISIZE<50").get(0);

        final List<List<String>> combined = run("combined", new String[] { "seq", "phys" },
                "--query", "Cigar_M>45", "--physical-query", "ISIZE<50");
        assertEquals(seq, combined.get(0));
        assertEquals(phys, combined.get(1));
    }

    @Test
    public final void emptyTypeQueryMeansNoFilter() throws Exception {
        final List<String> seq = run("seq", new String[] { "seq" }).get(0);
        final List<String> phys = run("phys", new String[] { "phys" }, "--query", "ISIZE<50").get(0);

        final List<List<String>> combined = run("combined", new String[] { "seq", "phys" },
                "--query", "ISIZE<50", "--sequence-query", "");
        assertEquals(seq, combined.get(0));
        assertEquals(phys, combined.get(1));
    }

    @Test
    public final void singleQueryUsedForAllTypes() throws Exception {
        final List<String> seq = run("seq", new String[] { "seq" }, "--query", "ISIZE<50").get(0);
        final List<String> phys = run("phys", new String[] { "phys" }, "--query", "ISIZE<50").get(0);

        final List<List<String>> combined = run("combined", new String[] { "seq", "phys" },
                "--query", "ISIZE<50");
        assertEquals(seq, combined.get(0));
        assertEquals(phys, combined.get(1));
    }

    @Test
    public final void lowReadDepthQuery() throws Exception {
        final List<String> seq = run("seq", new String[] { "seq" }, "--query", "Cigar_M>45").get(0);
        final List<String> lowReadDepth = run("lrd", new String[] { "low_readdepth" },
                "--readdepth-cutoff", "1", "--query", "ISIZE<50").get(0);
        assertNotEquals(lowReadDepth, run("lrd", new String[] { "low_readdepth" },
                "--readdepth-cutoff", "1", "--query", "Cigar_M>45").get(0));

        final List<List<String>> combined = run("combined", new String[] { "seq", "low_readdepth" },
                "--readdepth-cutoff", "1", "--query", "Cigar_M>45", "--low-readdepth-query", "ISIZE<50");
        assertEquals(seq, combined.get(0));
        assertEquals(lowReadDepth, combined.get(1));
    }

    @Test
    public final void queryOptionSpecifiedMoreThanOnce() {
        assertNotEquals(0, runExpectingFailure("--type", "seq", "--type", "phys",
                "--query", "ISIZE<50", "--query", "Cigar_M>45"));
    }

    @Test
    public final void typeQueryOptionSpecifiedMoreThanOnce() {
        assertNotEquals(0, runExpectingFailure("--type", "seq", "--type", "phys",
                "--sequence-query", "ISIZE<50", "--sequence-query", "Cigar_M>45"));
    }

    @Test
    public final void typeQueryForTypeNotRequested() {
        assertNotEquals(0, runExpectingFailure("--type", "seq", "--physical-query", "ISIZE<50"));
        assertNotEquals(0, runExpectingFailure("--type", "seq", "--type", "phys",
                "--low-readdepth-query", "ISIZE<50"));
    }
}
