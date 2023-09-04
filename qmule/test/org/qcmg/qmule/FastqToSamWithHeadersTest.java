package org.qcmg.qmule;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picard.PicardException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static htsjdk.samtools.SAMUtils.MAX_PHRED_SCORE;
import static org.junit.Assert.*;

public class FastqToSamWithHeadersTest {


    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final FastqToSamWithHeaders fastqToSam = new FastqToSamWithHeaders();
    private static File newTempFile(final String filename) throws IOException {
        return newTempFile(filename, ".tmp");
    }
    private static File newTempFile(final String filename, String suffix) throws IOException {
        final File file = File.createTempFile(filename, suffix);
        file.deleteOnExit();
        return file;
    }

    private static FastqReader freader1;
    private static FastqReader freader2;

    static {
        try {
            freader1 = new FastqReader(newTempFile("dummyFile"));
            freader2 = new FastqReader(newTempFile("dummyFile"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createSAMRecordNoAdditionalHeader() {
        FastqRecord fqRec = new FastqRecord("basename","ACGT", "", "????");
        SAMRecord samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, true, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals("basename", samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertTrue(samRec.getReadPairedFlag());
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));

        samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, false, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals("basename", samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertFalse(samRec.getReadPairedFlag());
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));
    }

    @Test
    public void createSAMRecordAdditionalHeaderNoTrimming() {
        String basename = "basename";
        String pairing = "/1";
        String additionalHeader = " 1.2.ACGTTGCA";
        FastqRecord fqRec = new FastqRecord(basename + pairing + additionalHeader, "ACGT", "", "????");
        SAMRecord samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, true, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals(basename, samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertTrue(samRec.getReadPairedFlag());
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertEquals(pairing + additionalHeader, samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));
    }

    @Test
    public void createSAMRecordAdditionalHeader() {
        String basename = "basename";
        String pairing = "/1";
        String additionalHeader = " 1.2.ACGTTGCA";
        String trimmedBases = "aaaaAAAA+????????";
        FastqRecord fqRec = new FastqRecord(basename + pairing + additionalHeader  + " TB:" + trimmedBases, "ACGT", "", "????");
        SAMRecord samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, true, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals(basename, samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertTrue(samRec.getReadPairedFlag());
        assertEquals(trimmedBases, samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertEquals(pairing + additionalHeader, samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));
    }

    @Test
    public void readPairNameOk() {
        assertEquals("aa", fastqToSam.getBaseName("aa/1", "aa/2" , freader1, freader2));
        assertEquals("aa", fastqToSam.getBaseName("aa", "aa", freader1, freader2));
        assertEquals("aa/bb", fastqToSam.getBaseName("aa/bb", "aa/bb" , freader1, freader2));
        assertEquals("aa/bb/", fastqToSam.getBaseName("aa/bb/", "aa/bb/" , freader1, freader2));
        assertEquals("aa/bb", fastqToSam.getBaseName("aa/bb/1", "aa/bb/2" , freader1, freader2));
        assertEquals("aa/bb/cc/dd/ee/ff", fastqToSam.getBaseName("aa/bb/cc/dd/ee/ff/1", "aa/bb/cc/dd/ee/ff/2" , freader1, freader2));
        assertEquals("///", fastqToSam.getBaseName("////1", "////2" , freader1, freader2));
        assertEquals("/", fastqToSam.getBaseName("/", "/" , freader1, freader2));
        assertEquals("////", fastqToSam.getBaseName("////", "////", freader1, freader2));
        assertEquals("/aa", fastqToSam.getBaseName("/aa", "/aa" , freader1, freader2));
        assertEquals("aa/", fastqToSam.getBaseName("aa/", "aa/" , freader1, freader2));
        assertEquals("ab/c", fastqToSam.getBaseName("ab/c", "ab/c", freader1, freader2));
    }

    @Test
    public void readPairNamesBad() {
        try {
            fastqToSam.getBaseName("", "" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa/1", "bb/2" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa", "bb" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa/1", "aa" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa", "aa/2" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa/1", "aa/1" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
        try {
            fastqToSam.getBaseName("aa/2", "aa/2" , freader1, freader2);
            Assert.fail("Should have thrown an exception");
        } catch (PicardException ignored) {}
    }

    @Test
    public void testSequentialFiles() throws Exception {
        File singleEnd = testFolder.newFile("single_end_R1_001.fastq");
        File singleEnd2 = testFolder.newFile("single_end_R1_002.fastq");
        File pairedEnd1 = testFolder.newFile("paired_end_R1_001.fastq");
        File pairedEnd12 = testFolder.newFile("paired_end_R1_002.fastq");
        File pairedEnd2 = testFolder.newFile("paired_end_R2_001.fastq");
        File pairedEnd22 = testFolder.newFile("paired_end_R2_002.fastq");

        assertEquals(2, FastqToSamWithHeaders.getSequentialFileList(singleEnd).size());
        assertEquals(2, FastqToSamWithHeaders.getSequentialFileList(pairedEnd1).size());
        assertEquals(2, FastqToSamWithHeaders.getSequentialFileList(pairedEnd2).size());

        populateFile(Arrays.asList(singleEnd, singleEnd2, pairedEnd1, pairedEnd12, pairedEnd2, pairedEnd22), Arrays.asList("@FAKE0001 Original version has PHRED scores from 93 to 0 inclusive (in that order)",
                "ACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACG",
                "+",
                "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));

        File singleEnd1Output = testFolder.newFile("singleEnd1.sam");
        File singleEnd2Output = testFolder.newFile("singleEnd2.sam");
        File pairedEnd1Output = testFolder.newFile("pairedEnd1.sam");
        File pairedEnd2Output = testFolder.newFile("pairedEnd2.sam");
        convertFileAndVerifyRecordCount(1, singleEnd, null, singleEnd1Output, FastqQualityFormat.Illumina, true, false);
        convertFileAndVerifyRecordCount(2, singleEnd, null, singleEnd2Output,FastqQualityFormat.Illumina, true, true);
        convertFileAndVerifyRecordCount(2, pairedEnd1, pairedEnd2, pairedEnd1Output, FastqQualityFormat.Illumina, true, false);
        convertFileAndVerifyRecordCount(4, pairedEnd1, pairedEnd2, pairedEnd2Output, FastqQualityFormat.Illumina, true, true);
    }

    private void populateFile(List<File> files, List<String> data) {
        for (File f : files) {
            try (FileWriter fw = new FileWriter(f)) {
                for (String s : data) {
                    fw.write(s + "\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testEmptyFastq() throws IOException {
        final File emptyFastq = testFolder.newFile("empty.fastq");
        final File emptyFastqSam = testFolder.newFile("empty.fastq.sam");
        convertFile(emptyFastq, null, emptyFastqSam, FastqQualityFormat.Illumina, false, false, false, false);
    }

    private void convertFile(final File fastq1,
                             final File fastq2,
                             final File outputFile,
                             final FastqQualityFormat version,
                             final boolean permissiveFormat,
                             final boolean useSequentialFastqs,
                             final boolean allowEmptyFastq) throws IOException {
        convertFile(fastq1, fastq2, outputFile, version, permissiveFormat, useSequentialFastqs, allowEmptyFastq, true);
    }
    private void convertFile(final File fastq1,
                             final File fastq2,
                             final File outputFile,
                             final FastqQualityFormat version,
                             final boolean permissiveFormat,
                             final boolean useSequentialFastqs,
                             final boolean allowEmptyFastq,
                             final boolean expectSuccess) throws IOException {

        final List<String> args = new ArrayList<>();

        args.add("FASTQ=" + fastq1.getAbsolutePath());
        args.add("OUTPUT=" + outputFile.getAbsolutePath());
        args.add("QUALITY_FORMAT=" + version);
        args.add("READ_GROUP_NAME=rg");
        args.add("SAMPLE_NAME=s1");

        if (fastq2 != null) args.add("FASTQ2=" + fastq2.getAbsolutePath());
        if (permissiveFormat) args.add("ALLOW_AND_IGNORE_EMPTY_LINES=true");
        if (useSequentialFastqs) args.add("USE_SEQUENTIAL_FASTQS=true");
        if (allowEmptyFastq) args.add("ALLOW_EMPTY_FASTQ=true");
        int exitStatus = 1;
        try {
            exitStatus = new FastqToSamWithHeaders().instanceMain(args.toArray(new String[]{}));
            if ( ! expectSuccess) {
                Assert.fail("Should have thrown a PicardException");
            }
        } catch (Exception ignored) {}
        assertEquals(expectSuccess ? 0 : 1, exitStatus);
    }

    private void convertFileAndVerifyRecordCount(final int expectedCount,
                                                 final File fastqFilename1,
                                                 final File fastqFilename2,
                                                 final File outputFile,
                                                 final FastqQualityFormat version,
                                                 final boolean permissiveFormat,
                                                 final boolean useSequentialFastqs) throws IOException {

        convertFile(fastqFilename1, fastqFilename2, outputFile, version, permissiveFormat, useSequentialFastqs, false);
        final SamReader samReader = SamReaderFactory.makeDefault().open(outputFile);
        final SAMRecordIterator iterator = samReader.iterator();
        int actualCount = 0;
        while (iterator.hasNext()) {
            iterator.next();
            actualCount++;
        }
        samReader.close();
        Assert.assertEquals(expectedCount, actualCount);
    }

    @Test
    public void runWithNoArgs() {
        int exitStatus = new FastqToSamWithHeaders().instanceMain(new String[]{});
        assertEquals(1, exitStatus);
    }
}
