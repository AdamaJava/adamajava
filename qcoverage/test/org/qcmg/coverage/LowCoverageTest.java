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

public class LowCoverageTest {

    String bam;
    String bai;
    String gff1000To1065;


    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public final void before() throws Exception {
        if (null == bam) {
            bam = testFolder.newFile("coverage.bam").getAbsolutePath();
            createCoverageBam(bam, getAACSAMRecords(SortOrder.coordinate), createSamHeaderObject(SortOrder.coordinate));
            bai = bam.replace("bam", "bai");
        }
        if (null == gff1000To1065) {
            File f = testFolder.newFile("gff1");
            gff1000To1065 = f.getAbsolutePath();
            createGFF3File(1000, 1065, f);

        }
    }

    private void createGFF3File(final int start, final int end, File file) throws IOException {
        Gff3Record record = new Gff3Record();
        record.setSeqId("chr1");
        record.setType("chrom");
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
    public void defaultLowCoverageValues() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutputTumour = new File(fname + ".lowcov.tumour12.bed");
        File fOutputControl = new File(fname + ".lowcov.control8.bed");
        String cmd = "--log ./logfile --type low_coverage --input-gff3 " + gff1000To1065 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname;

        Executor exec = execute(cmd);

        assertEquals(0, exec.getErrCode());

        File folder = testFolder.getRoot();
        File[] listOfFiles = folder.listFiles();

        assertTrue(fOutputTumour.exists());
        assertTrue(fOutputControl.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutputTumour))) {
            fileContents = r.lines().toList();
        }
        assertEquals(2, fileContents.size());
        assertEquals("chr1\t999\t1011", fileContents.get(0));
        assertEquals("chr1\t1050\t1065", fileContents.get(1));

        fOutputTumour.delete();

        try (BufferedReader r = new BufferedReader(new FileReader(fOutputControl))) {
            fileContents = r.lines().toList();
        }
        assertEquals(2, fileContents.size());
        assertEquals("chr1\t999\t1007", fileContents.get(0));
        assertEquals("chr1\t1054\t1065", fileContents.get(1));

        fOutputControl.delete();

    }

    @Test
    public void defaultOptionalLowCoverageValues() throws Exception {
        String fname = testFolder.getRoot().getAbsolutePath() + "/output";
        File fOutputTumour = new File(fname + ".lowcov.tumour10.bed");
        File fOutputControl = new File(fname + ".lowcov.control6.bed");
        String cmd = "--log ./logfile --type low_coverage --input-gff3 " + gff1000To1065 + " --input-bam " + bam + " --input-bai " + bai + " --output " + fname + " --low-cov-tumour 10 --low-cov-control 6";

        Executor exec = execute(cmd);

        assertEquals(0, exec.getErrCode());

        File folder = testFolder.getRoot();
        File[] listOfFiles = folder.listFiles();

        assertTrue(fOutputTumour.exists());
        assertTrue(fOutputControl.exists());

        List<String> fileContents;
        try (BufferedReader r = new BufferedReader(new FileReader(fOutputTumour))) {
            fileContents = r.lines().toList();
        }
        assertEquals(2, fileContents.size());
        assertEquals("chr1\t999\t1009", fileContents.get(0));
        assertEquals("chr1\t1052\t1065", fileContents.get(1));
        fOutputTumour.delete();

        try (BufferedReader r = new BufferedReader(new FileReader(fOutputControl))) {
            fileContents = r.lines().toList();
        }
        assertEquals(2, fileContents.size());
        assertEquals("chr1\t999\t1005", fileContents.get(0));
        assertEquals("chr1\t1056\t1065", fileContents.get(1));
        fOutputControl.delete();

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
        rg1.setPlatform("Illumina");
        rg1.setLibrary("0ac984f9-db61-4090-918b-615fd24e6efe");
        SAMReadGroupRecord rg2 = new SAMReadGroupRecord("ZZZ");
        rg2.setPlatform("Illumina");
        rg2.setLibrary("0ac984f9-db61-4090-918b-615fd24e6efa");

        h.setReadGroups(Arrays.asList(rg1, rg2));
        return h;
    }


    public static List<SAMRecord> getAACSAMRecords(SortOrder so) {
        SAMFileHeader h = createSamHeaderObject(so);

        SAMRecord s1 = getSAM(h, "1290_738_1021", 0, "chr1", 1000, 255, "50M", "*", 0, 0, "ACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTAC", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s2 = getSAM(h, "1290_738_1022", 16, "chr1", 1001, 255, "50M", "*", 0, 0, "CATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACA", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s3 = getSAM(h, "1290_738_1023", 0, "chr1", 1002, 255, "50M", "*", 0, 0, "ATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACAT", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s4 = getSAM(h, "1290_738_1023", 0, "chr1", 1003, 255, "50M", "*", 0, 0, "ATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACAT", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s5 = getSAM(h, "1290_738_1024", 0, "chr1", 1004, 255, "50M", "*", 0, 0, "TTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATT", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s6 = getSAM(h, "1290_738_1025", 0, "chr1", 1005, 255, "50M", "*", 0, 0, "TCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTC", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s7 = getSAM(h, "1290_738_1026", 0, "chr1", 1006, 255, "50M", "*", 0, 0, "CCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s8 = getSAM(h, "1290_738_1027", 0, "chr1", 1007, 255, "50M", "*", 0, 0, "CAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGA", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s9 = getSAM(h, "1290_738_1028", 0, "chr1", 1008, 255, "50M", "*", 0, 0, "AAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s10 = getSAM(h, "1290_738_1029", 0, "chr1", 1009, 255, "50M", "*", 0, 0, "AAAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAGG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s11 = getSAM(h, "1290_738_1010", 0, "chr1", 1010, 255, "50M", "*", 0, 0, "AAGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAGGG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s12 = getSAM(h, "1290_738_1011", 0, "chr1", 1011, 255, "50M", "*", 0, 0, "AGTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAGGGA", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s13 = getSAM(h, "1290_738_1012", 0, "chr1", 1012, 255, "50M", "*", 0, 0, "GTCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAGGGAA", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");
        SAMRecord s14 = getSAM(h, "1290_738_1013", 0, "chr1", 1013, 255, "50M", "*", 0, 0, "TCAACCATCCAAGTTTATTCTAAATAGATGTGACGTACATTCGAGGGAAG", "!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDDDDDDD:<3B''DDD!", "ZZ");

        return Arrays.asList(s1, s2, s3,s4,s5,s6,s7,s8,s9,s10,s11,s12,s13, s14);
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
