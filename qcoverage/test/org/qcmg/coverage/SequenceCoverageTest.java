package org.qcmg.coverage;

import htsjdk.samtools.*;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SequenceCoverageTest {

    String bam;
    String bai;
    String gff54000To54025;
    String gff54030To54070;
    String gff54077To54120;
    String gff54000To54026;
    String gff54076To54120;
    String gff54000To54036;
    String gff54050To54120;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public final void before() throws Exception {
        if (null == bam) {
            bam = testFolder.newFile("coverage.bam").getAbsolutePath();
            createCoverageBam(bam, getAACSAMRecords(SortOrder.coordinate), createSamHeaderObject(SortOrder.coordinate));
            bai = bam.replace("bam", "bai");
        }
        if (null == gff54000To54025) {
            File f = testFolder.newFile("gff1");
            gff54000To54025 = f.getAbsolutePath();
            createGFF3File(54000, 54025, f);

            File f2 = testFolder.newFile("gff2");
            gff54030To54070 = f2.getAbsolutePath();
            createGFF3File(54030, 54070, f2);

            File f3 = testFolder.newFile("gff3");
            gff54077To54120 = f3.getAbsolutePath();
            createGFF3File(54077, 54120, f3);

            File f4 = testFolder.newFile("gff4");
            gff54000To54026 = f4.getAbsolutePath();
            createGFF3File(54000, 54026, f4);

            File f5 = testFolder.newFile("gff5");
            gff54076To54120 = f5.getAbsolutePath();
            createGFF3File(54076, 54120, f5);

            File f6 = testFolder.newFile("gff6");
            gff54000To54036 = f6.getAbsolutePath();
            createGFF3File(54000, 54036, f6);

            File f7 = testFolder.newFile("gff7");
            gff54050To54120 = f7.getAbsolutePath();
            createGFF3File(54050, 54120, f7);
        }
    }

    private void createGFF3File(final int start, final int end, File file) throws IOException {
        Gff3Record record = new Gff3Record();
        record.setSeqId("chr1");
        record.setType("exon");
        record.setStart(start);
        record.setEnd(end);
        record.setScore(".");
        record.setSource(".");
        record.setStrand("+");

        try (RecordWriter<Gff3Record> writer = new RecordWriter<>(file)) {
            writer.add(record);
        }
    }

    private Executor execute(final String command) throws Exception {
        return new Executor(command, "org.qcmg.coverage.Coverage");
    }

    @Test
    public void beforeReadsStart() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");
        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54000To54025 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(2, fileContents.size());
        assertEquals("sequence	exon	26	0x", fileContents.get(1));

        fOutput.delete();

    }

    @Test
    public void overlapReadsStart() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");

        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54030To54070 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(2, fileContents.size());
        assertEquals("sequence	exon	41	1x", fileContents.get(1));

        fOutput.delete();
    }


    @Test
    public final void afterRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");


        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54077To54120 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);
        assertEquals(0, exec.getErrCode());

        assertTrue(fOutput.exists());
        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(2, fileContents.size());
        assertEquals("sequence	exon	44	0x", fileContents.get(1));

        fOutput.delete();
    }


    @Test
    public final void overlapBeginningOfRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");


        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54000To54026 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);
        assertEquals(0, exec.getErrCode());

        assertTrue(fOutput.exists());
        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(3, fileContents.size());
        assertEquals("sequence	exon	26	0x", fileContents.get(1));
        assertEquals("sequence	exon	1	1x", fileContents.get(2));

        fOutput.delete();
    }

    @Test
    public final void rightOnEndRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");

        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54076To54120 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(2, fileContents.size());
        assertEquals("sequence	exon	45	0x", fileContents.get(1));

        fOutput.delete();
    }

    @Test
    public final void leftOverlapRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");

        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54000To54036 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(3, fileContents.size());
        assertEquals("sequence	exon	26	0x", fileContents.get(1));
        assertEquals("sequence	exon	11	1x", fileContents.get(2));

        fOutput.delete();
    }

    @Test
    public final void rightOverlapRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");

        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54050To54120 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(3, fileContents.size());
        assertEquals("sequence	exon	50	0x", fileContents.get(1));
        assertEquals("sequence	exon	21	1x", fileContents.get(2));

        fOutput.delete();
    }

    @Test
    public final void subsetRead() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutput = new File(fname + ".txt");

        Executor exec = execute("--log ./logfile --type seq --input-gff3 " + gff54030To54070 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname);

        assertEquals(0, exec.getErrCode());
        assertTrue(fOutput.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutput))) {
            fileContents = r.lines().toList();
        }

        assertEquals(2, fileContents.size());
        assertEquals("sequence	exon	41	1x", fileContents.get(1));

        fOutput.delete();
    }

    public static void createCoverageBam(String outputFileName, List<SAMRecord> recs, SAMFileHeader h) {
        File outputFile = new File(outputFileName);
        SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
        try (SAMFileWriter outputWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(h, true, outputFile)) {
            for (SAMRecord read : recs) {
                outputWriter.addAlignment(read);
            }
        }
    }

    public static SAMFileHeader createSamHeaderObject(SortOrder sort) {
        SAMSequenceDictionary dict = new SAMSequenceDictionary(
                Arrays.asList(new SAMSequenceRecord("chr1", 100000),
                        new SAMSequenceRecord("chr2", 100000),
                        new SAMSequenceRecord("chr3", 100000)));

        SAMFileHeader h = new SAMFileHeader();
        h.setSequenceDictionary(dict);
        h.setSortOrder(sort);

        SAMReadGroupRecord rg1 = new SAMReadGroupRecord("ZZ");
        rg1.setPlatform("SOLiD");
        rg1.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");
        SAMReadGroupRecord rg2 = new SAMReadGroupRecord("ZZZ");
        rg2.setPlatform("SOLiD");
        rg2.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");

        h.setReadGroups(Arrays.asList(rg1, rg2));
        return h;
    }


    public static List<SAMRecord> getAACSAMRecords(SortOrder so) {
        SAMFileHeader h = createSamHeaderObject(so);

        SAMRecord s1 = getSAM(h, "1290_738_1025", 0, "chr1", 54026, 255, "45M5H", "*", 0, 0, "AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s2 = getSAM(h, "2333_755_492", 16, "chr2", 10103, 255, "10H40M", "*", 0, 0, "CACACCACACCCACACACCACACACCACACCCACACCCAC", "!=DD?%+DD<)=DDD<@9)9C:DA.:DD>%%,<?('-,4!", "ZZ");
        SAMRecord s3 = getSAM(h, "1879_282_595", 0, "chr3", 60775, 255, "40M10H", "*", 0, 0, "TCTAAATTTGTTTGATCACATACTCCTTTTCTGGCTAACA", "!DD,*@DDD''DD>5:DD>;DDDD=CDD8%%DA9-DDC0!", "ZZ");

        return Arrays.asList(s1, s2, s3);
    }

    private static SAMRecord getSAM(SAMFileHeader h, String readName, int flags, String chr, int pos, int mapQ, String cigar, String mRef, int mPos, int iSize, String bases, String quals, String md) {
        SAMRecord s1 = new SAMRecord(h);
        s1.setAlignmentStart(pos);
        s1.setCigarString(cigar);
        s1.setBaseQualityString(quals);
        s1.setFlags(flags);
        s1.setMappingQuality(mapQ);
        s1.setInferredInsertSize(iSize);
        s1.setReadName(readName);
        s1.setReferenceName(chr);
        s1.setReadString(bases);
        s1.setAttribute("MD", md);
        s1.setMateReferenceName(mRef);
        s1.setMateAlignmentStart(mPos);
        return s1;
    }

}
