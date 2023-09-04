package org.qcmg.qmule;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.IOUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picard.PicardException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.qcmg.qmule.SamToFastqWithHeaders.getTrimmedBaseDetailsFromTag;

public class SamToFastqWithHeadersTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void getPositionOfCaseChange() {
        assertEquals(-1, SamToFastqWithHeaders.getPositionOfCaseChange(null));
        assertEquals(-1, SamToFastqWithHeaders.getPositionOfCaseChange(""));
        assertEquals(0, SamToFastqWithHeaders.getPositionOfCaseChange("ACBD"));
        assertEquals(0, SamToFastqWithHeaders.getPositionOfCaseChange("abcd"));
        assertEquals(4, SamToFastqWithHeaders.getPositionOfCaseChange("abcdABCD"));
        assertEquals(4, SamToFastqWithHeaders.getPositionOfCaseChange("ABCDxyz"));
        assertEquals(1, SamToFastqWithHeaders.getPositionOfCaseChange("xYZ"));
        assertEquals(1, SamToFastqWithHeaders.getPositionOfCaseChange("Xyz"));
        assertEquals(2, SamToFastqWithHeaders.getPositionOfCaseChange("xyZ"));
        assertEquals(2, SamToFastqWithHeaders.getPositionOfCaseChange("XYz"));
    }

    @Test
    public void getTrimmedBasesFromTag() {
        assertArrayEquals(new String[]{}, getTrimmedBaseDetailsFromTag(null));
        assertArrayEquals(new String[]{}, getTrimmedBaseDetailsFromTag(""));
        assertArrayEquals(new String[]{"","","ABCD","!!!!"}, getTrimmedBaseDetailsFromTag("ABCD+!!!!"));
        assertArrayEquals(new String[]{"abcd","!!!!","",""}, getTrimmedBaseDetailsFromTag("abcd+!!!!"));
        assertArrayEquals(new String[]{"abcd","!!!!","XYZ","%%%"}, getTrimmedBaseDetailsFromTag("abcdXYZ+!!!!%%%"));
        assertArrayEquals(new String[]{"xyz","%%%","ABCD","!!!!"}, getTrimmedBaseDetailsFromTag("ABCDxyz+!!!!%%%"));
    }

    @Test
    public void getFastqFromSam() {
        SAMRecord record = new SAMRecord(null);
        record.setReadName("ERR194147.1758538");

        // first read
        record.setReadBases("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT".getBytes());
        record.setBaseQualityString("???????????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setFlags(99);
        record.setMappingQuality(60);
        record.setCigarString("97M");
        record.setInferredInsertSize(330);
        record.setAttribute("OQ", "???????????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setAttribute("MQ", 60);
        record.setAttribute("MC", "3S98M");
        record.setAttribute("ZH", "/1 ACGTACGT");
        record.setAttribute("ZT", "GCGA+???'");

        FastqRecord fq = SamToFastqWithHeaders.getFastqRecordFromSamRecord(record, 1, 0, null, null, 0, null, true, null);
        assertEquals("ERR194147.1758538/1 ACGTACGT", fq.getReadName());
        assertEquals("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT" + "GCGA", fq.getReadString());
        assertEquals("???????????????????5???????????????????????????????????????&5?????????+??55?????????????????????????'", fq.getBaseQualityString());
    }

    @Test
    public void getFastqFromSamOQ() {
        SAMRecord record = new SAMRecord(null);
        record.setReadName("ERR194147.1758538");

        // first read
        record.setReadBases("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT".getBytes());
        record.setBaseQualityString("???????????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setFlags(99);
        record.setMappingQuality(60);
        record.setCigarString("97M");
        record.setInferredInsertSize(330);
        record.setAttribute("OQ", "55555??????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setAttribute("MQ", 60);
        record.setAttribute("MC", "3S98M");
        record.setAttribute("ZH", "/1 ACGTACGT");
        record.setAttribute("ZT", "GCGA+???'");

        FastqRecord fq = SamToFastqWithHeaders.getFastqRecordFromSamRecord(record, 1, 0, null, null, 0, null, true, null);
        assertEquals("ERR194147.1758538/1 ACGTACGT", fq.getReadName());
        assertEquals("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT" + "GCGA", fq.getReadString());
        assertEquals("55555??????????????5???????????????????????????????????????&5?????????+??55?????????????????????????'", fq.getBaseQualityString());
    }

    @Test
    public void getFastqFromSamReverse() {
        SAMRecord record = new SAMRecord(null);
        record.setReadName("ERR194147.421820475");

        // first read
        record.setReadBases("TTCTTTGGCCTAATGACATGGCTATTAGTGCACAAGGAAATGGTCAAAAATGGGAAGAAATGTAGGTCACAAAATATTGCACAAAGCTATACTTACTT".getBytes());
        record.setBaseQualityString("??????????????????????????????????????????????????????????????????????????????????????????????????");
        record.setFlags(83);
        record.setMappingQuality(60);
        record.setCigarString("98M");
        record.setInferredInsertSize(-323);
        record.setAttribute("OQ", "??????????????????????????????????????????????????????????????????????????????????????????????????");
        record.setAttribute("MQ", 60);
        record.setAttribute("MC", "3S98M");
        record.setAttribute("ZH", "/1 XYZ.123.25");
        record.setAttribute("ZT", "CTG+???");

        FastqRecord fq = SamToFastqWithHeaders.getFastqRecordFromSamRecord(record, 1, 0, null, null, 0, null, true, null);
        assertEquals("ERR194147.421820475/1 XYZ.123.25", fq.getReadName());
        assertEquals("AAGTAAGTATAGCTTTGTGCAATATTTTGTGACCTACATTTCTTCCCATTTTTGACCATTTCCTTGTGCACTAATAGCCATGTCATTAGGCCAAAGAACTG", fq.getReadString());
        assertEquals("?????????????????????????????????????????????????????????????????????????????????????????????????????", fq.getBaseQualityString());
    }
    @Test
    public void getFastqFromSamDodgyTag() {
        SAMRecord record = new SAMRecord(null);
        record.setReadName("ERR194147.1758538");

        // first read
        record.setReadBases("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT".getBytes());
        record.setBaseQualityString("???????????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setFlags(99);
        record.setMappingQuality(60);
        record.setCigarString("97M");
        record.setInferredInsertSize(330);
        record.setAttribute("OQ", "???????????????????5???????????????????????????????????????&5?????????+??55??????????????????????");
        record.setAttribute("MQ", 60);
        record.setAttribute("MC", "3S98M");
        record.setAttribute("ZH", "/1 foo bar");
        record.setAttribute("ZT", "GCGA+???'");

        FastqRecord fq = SamToFastqWithHeaders.getFastqRecordFromSamRecord(record, 1, 0, null, null, 0, null, true, null);
        assertEquals("ERR194147.1758538/1 foo bar", fq.getReadName());
        assertEquals("AAATGAGGGAAGAAAAGAGTTAAATGCATGTTGATTCCAAGCCCCCGCCTGCCGGGGGGACAGCGGGAGGTTGGAGCACGCAGCCCTGGTGCCTGGT" + "GCGA", fq.getReadString());
        assertEquals("???????????????????5???????????????????????????????????????&5?????????+??55?????????????????????????'", fq.getBaseQualityString());
    }

    @Test
    public void testMissingRgFileOutputPerRg() throws IOException {
        File inputSam = testFolder.newFile("testMissingRgFileOutputPerRg.sam");
        File outputDir = testFolder.newFolder();
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, false);
    }

    @Test
    public void groupedUnpairedMate() throws IOException {
        File inputSam = testFolder.newFile("groupedUnpairedMate.sam");
        File outputDir = testFolder.newFolder();

        populateSamFile(inputSam, Arrays.asList("@HD\tVN:1.0\tSO:unsorted",
                "@RG\tID:rg1\tSM:s1\tPU:rg1\tPL:ILLUMINA",
                "@RG\tID:rg2\tSM:s2\tPU:rg2\tPL:ILLUMINA",
                "foo:record:1\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg1",
                "foo:record:1\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg1",
                "foo:record:2\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg1",
                "foo:record:3\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg1",
                "bar:record:1\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg2",
                "bar:record:2\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg2"));
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, false);

        populateSamFile(inputSam, Arrays.asList("@HD\tVN:1.0\tSO:unsorted",
                "@RG\tID:rg1\tSM:s1\tPU:rg1\tPL:ILLUMINA",
                "@RG\tID:rg2\tSM:s2\tPU:rg2\tPL:ILLUMINA",
                "foo:record:1\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg1",
                "foo:record:1\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg1",
                "foo:record:2\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg1",
                "foo:record:2\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg1",
                "bar:record:1\t77\t*\t0\t0\t*\t*\t0\t0\tAAAAAAAAAAAAA\t1111111111111\tRG:Z:rg2",
                "bar:record:1\t141\t*\t0\t0\t*\t*\t0\t0\tCCCCCCCCCCCCC\t2222222222222\tRG:Z:rg2"));
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, true);
        File[] fastqFiles = outputDir.listFiles((dir, file) -> file.startsWith("rg1"));
        assert fastqFiles != null;
        assertEquals(2, fastqFiles.length);
        boolean arraySorted = fastqFiles[0].getName().endsWith("1.fastq");
        verifyFastq(arraySorted ? fastqFiles[0] : fastqFiles[1], arraySorted ? fastqFiles[1] : fastqFiles[0], inputSam, "rg1");

        fastqFiles = outputDir.listFiles((dir, file) -> file.startsWith("rg2"));
        assert fastqFiles != null;
        assertEquals(2, fastqFiles.length);
        arraySorted = fastqFiles[0].getName().endsWith("1.fastq");
        verifyFastq(arraySorted ? fastqFiles[0] : fastqFiles[1], arraySorted ? fastqFiles[1] : fastqFiles[0], inputSam, "rg2");

        File outputFastq = testFolder.newFile("groupedUnpairedMate.fastq");
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "FASTQ=" + outputFastq.getAbsolutePath(),
                "INTERLEAVE=true"
        }, true);
        final Set<String> outputHeaderSet = createFastqReadHeaderSet(outputFastq);
        // Create map of mate pairs from SAM records
        final Map<String,MatePair> map = createSamMatePairsMap(inputSam, null) ;
        Assert.assertEquals(map.size() * 2, outputHeaderSet.size());

        // Ensure that each mate of each pair in SAM file is in the correct fastq pair file
        for (final Map.Entry<String,MatePair> entry : map.entrySet() ) {
            final MatePair mpair = entry.getValue();
            Assert.assertNotNull(mpair.mate1); // ensure we have two mates
            Assert.assertNotNull(mpair.mate2);
            Assert.assertEquals(mpair.mate1.getReadName(),mpair.mate2.getReadName());
            final String readName = mpair.mate1.getReadName() ;
            Assert.assertTrue(outputHeaderSet.contains(readName + "/1")); // ensure mate is in correct file
            Assert.assertTrue(outputHeaderSet.contains(readName + "/2"));
        }
    }

    @Test
    public void firstMateAtStartLastMateAtEnd() throws IOException {
        File inputSam = testFolder.newFile("groupedUnpairedMate.sam");
        File outputDir = testFolder.newFolder();
        populateSamFile(inputSam, Arrays.asList("@HD	VN:1.0	SO:unsorted",
                "@RG	ID:rg1	SM:s1	PU:blah     PL:ILLUMINA",
        "bar:record:1	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1",
        "bar:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1",
        "bar:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1",
        "bar:record:3	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1",
        "bar:record:3	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1",
        "bar:record:4	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1",
        "bar:record:4	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1",
        "bar:record:5	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1",
        "bar:record:5	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1",
        "bar:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1"));
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, true);
        File[] fastqFiles = outputDir.listFiles((dir, file) -> file.endsWith(".fastq"));
        assert fastqFiles != null;
        assertEquals(2, fastqFiles.length);
        boolean arraySorted = fastqFiles[0].getName().endsWith("1.fastq");
        verifyFastq(arraySorted ? fastqFiles[0] : fastqFiles[1], arraySorted ? fastqFiles[1] : fastqFiles[0], inputSam);

        File outputFastq = testFolder.newFile("groupedUnpairedMate.fastq");
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "FASTQ=" + outputFastq.getAbsolutePath(),
                "INTERLEAVE=true"
        }, true);
        final Set<String> outputHeaderSet = createFastqReadHeaderSet(outputFastq);
        // Create map of mate pairs from SAM records
        final Map<String,MatePair> map = createSamMatePairsMap(inputSam, null) ;
        Assert.assertEquals(map.size() * 2, outputHeaderSet.size());

        // Ensure that each mate of each pair in SAM file is in the correct fastq pair file
        for (final Map.Entry<String,MatePair> entry : map.entrySet() ) {
            final MatePair mpair = entry.getValue();
            Assert.assertNotNull(mpair.mate1); // ensure we have two mates
            Assert.assertNotNull(mpair.mate2);
            Assert.assertEquals(mpair.mate1.getReadName(),mpair.mate2.getReadName());
            final String readName = mpair.mate1.getReadName() ;
            Assert.assertTrue(outputHeaderSet.contains(readName + "/1")); // ensure mate is in correct file
            Assert.assertTrue(outputHeaderSet.contains(readName + "/2"));
        }
    }

    @Test
    public void trimmedHeaders() throws IOException {
        File inputOrigSam = testFolder.newFile("trimmedHeadersOrig.sam");
        File inputSam = testFolder.newFile("trimmedHeaders.sam");
        File outputOrigDir = testFolder.newFolder();
        File outputDir = testFolder.newFolder();
        populateSamFile(inputOrigSam, Arrays.asList("@HD	VN:1.0	SO:queryname",
                "@RG	ID:rg1	SM:s1	PU:rg1	PL:ILLUMINA",
                "@RG	ID:rg2	SM:s2	PU:rg2	PL:ILLUMINA",
                "foo:record:1	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:3	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:3	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "bar:record:1	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111"));
        populateSamFile(inputSam, Arrays.asList("@HD	VN:1.0	SO:queryname",
                "@RG	ID:rg1	SM:s1	PU:rg1	PL:ILLUMINA",
                "@RG	ID:rg2	SM:s2	PU:rg2	PL:ILLUMINA",
                "foo:record:1	77	*	0	0	*	*	0	0	AAAAAAAAA	111111111	RG:Z:rg1	ZT:Z:AAAA+1111\tCR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:1	141	*	0	0	*	*	0	0	CCCCCCCCC	222222222	RG:Z:rg1	CR:Z:AAAAA	ZT:Z:cccc+2222\tUR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222\tZH:Z::3/1",
                "foo:record	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT\tZH:Z::3/2	CY:Z:11111	UY:Z:222",
                "bar:record:1	77	*	0	0	*	*	0	0	AAAAAAAAA	111111111	RG:Z:rg2	CR:Z:CCCCC	ZT:Z:aaaa+1111\tUR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCC	2222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	ZT:Z:CCC+222\tUY:Z:111",
                "bar:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111"));
        convertFile(new String[]{
                "INPUT=" + inputOrigSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputOrigDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, true);
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, true);
        File[] fastqOrigFiles = outputOrigDir.listFiles((dir, file) -> file.endsWith(".fastq"));
        File[] fastqFiles = outputDir.listFiles((dir, file) -> file.endsWith(".fastq"));

        /*
        loop through each one, calculating md5, then compare for equality
         */
        assert fastqOrigFiles != null;
        Map<String, String> mapOrig = Arrays.stream(fastqOrigFiles).collect(Collectors.toMap(File::getName, f -> {
            try {
                return new BigInteger(1, MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get(f.getPath())))).toString(16);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }));
        assert fastqFiles != null;
        Map<String, String> map = Arrays.stream(fastqFiles).collect(Collectors.toMap(File::getName, f -> {
            try {
                return new BigInteger(1, MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get(f.getPath())))).toString(16);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }));

        for (Map.Entry<String, String> entry : mapOrig.entrySet()) {
            String md5 = map.get(entry.getKey());
            assertEquals(md5, entry.getValue());
        }
    }

    @Test
    public void groupedLastPairMatesFlipped() throws IOException {
        File inputSam = testFolder.newFile("groupedUnpairedMate.sam");
        File outputDir = testFolder.newFolder();
        populateSamFile(inputSam, Arrays.asList("@HD	VN:1.0	SO:queryname",
        "@RG	ID:rg1	SM:s1	PU:rg1	PL:ILLUMINA",
                "@RG	ID:rg2	SM:s2	PU:rg2	PL:ILLUMINA",
                "foo:record:1	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:3	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "foo:record:3	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg1	CR:Z:AAAAA	UR:Z:TTT	CY:Z:11111	UY:Z:222",
                "bar:record:1	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:1	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:2	141	*	0	0	*	*	0	0	CCCCCCCCCCCCC	2222222222222	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111",
                "bar:record:2	77	*	0	0	*	*	0	0	AAAAAAAAAAAAA	1111111111111	RG:Z:rg2	CR:Z:CCCCC	UR:Z:GGG	CY:Z:22222	UY:Z:111"));
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "OUTPUT_DIR=" + outputDir.getAbsolutePath() + "/",
                "OUTPUT_PER_RG=true"
        }, true);
        File[] fastqFiles = outputDir.listFiles((dir, file) -> file.startsWith("rg1"));
        assert fastqFiles != null;
        assertEquals(2, fastqFiles.length);
        boolean arraySorted = fastqFiles[0].getName().endsWith("1.fastq");
        verifyFastq(arraySorted ? fastqFiles[0] : fastqFiles[1], arraySorted ? fastqFiles[1] : fastqFiles[0], inputSam, "rg1");

        fastqFiles = outputDir.listFiles((dir, file) -> file.startsWith("rg2"));
        assert fastqFiles != null;
        assertEquals(2, fastqFiles.length);
        arraySorted = fastqFiles[0].getName().endsWith("1.fastq");
        verifyFastq(arraySorted ? fastqFiles[0] : fastqFiles[1], arraySorted ? fastqFiles[1] : fastqFiles[0], inputSam, "rg2");

        File outputFastq = testFolder.newFile("groupedUnpairedMate.fastq");
        convertFile(new String[]{
                "INPUT=" + inputSam.getAbsolutePath(),
                "FASTQ=" + outputFastq.getAbsolutePath(),
                "INTERLEAVE=true"
        }, true);
        final Set<String> outputHeaderSet = createFastqReadHeaderSet(outputFastq);
        // Create map of mate pairs from SAM records
        final Map<String,MatePair> map = createSamMatePairsMap(inputSam, null) ;
        Assert.assertEquals(map.size() * 2, outputHeaderSet.size());

        // Ensure that each mate of each pair in SAM file is in the correct fastq pair file
        for (final Map.Entry<String,MatePair> entry : map.entrySet() ) {
            final MatePair mpair = entry.getValue();
            Assert.assertNotNull(mpair.mate1); // ensure we have two mates
            Assert.assertNotNull(mpair.mate2);
            Assert.assertEquals(mpair.mate1.getReadName(),mpair.mate2.getReadName());
            final String readName = mpair.mate1.getReadName() ;
            Assert.assertTrue(outputHeaderSet.contains(readName + "/1")); // ensure mate is in correct file
            Assert.assertTrue(outputHeaderSet.contains(readName + "/2"));
        }
    }

    private void convertFile(final String [] args, boolean expectSuccess) {
        int exitStatus = 1;
        try {
            exitStatus = new SamToFastqWithHeaders().instanceMain(args);
            if ( ! expectSuccess) {
                Assert.fail("Should have thrown a PicardException");
            }
        } catch (Exception ignored) {System.out.println("ignored exception: " + ignored);}
        assertEquals(expectSuccess ? 0 : 1, exitStatus);
    }

    private void populateSamFile(File sam, List<String> data) {
        try (FileWriter fw = new FileWriter(sam)) {
            for (String s : data) {
                fw.write(s + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyFastq(final File pair1File, final File pair2File, final File samFile) throws IOException {
        verifyFastq(pair1File, pair2File,  samFile, null);
    }
    private void verifyFastq(final File pair1File, final File pair2File, final File samFile, String readGroup) throws IOException {
        // Check that paired fastq files are same size
        final Set<String> outputHeaderSet1 = createFastqReadHeaderSet(pair1File);
        final Set<String> outputHeaderSet2 = createFastqReadHeaderSet(pair2File);
        Assert.assertEquals(outputHeaderSet1.size(), outputHeaderSet2.size());

        // Create map of mate pairs from SAM records
        final Map<String,MatePair> map = createSamMatePairsMap(samFile, readGroup) ;
        Assert.assertEquals(map.size(), outputHeaderSet2.size());

        // Ensure that each mate of each pair in SAM file is in the correct fastq pair file
        for (final Map.Entry<String,MatePair> entry : map.entrySet() ) {
            final MatePair mpair = entry.getValue();
            Assert.assertNotNull(mpair.mate1); // ensure we have two mates
            Assert.assertNotNull(mpair.mate2);
            Assert.assertEquals(mpair.mate1.getReadName(),mpair.mate2.getReadName());
            final String readName = mpair.mate1.getReadName() ;
            Assert.assertTrue(outputHeaderSet1.contains(readName + "/1")); // ensure mate is in correct file
            Assert.assertTrue(outputHeaderSet2.contains(readName + "/2"));
        }
    }

    private Map<String,MatePair> createSamMatePairsMap(final File samFile, final String readGroup) throws IOException {
        IOUtil.assertFileIsReadable(samFile);
        final SamReader reader = SamReaderFactory.makeDefault().open(samFile);

        final Map<String,MatePair> map = new LinkedHashMap<>();
        for (final SAMRecord record : reader ) {
            if (null == readGroup || record.getReadGroup().getReadGroupId().equals(readGroup)) {
                MatePair mpair = map.get(record.getReadName());
                if (mpair == null) {
                    mpair = new MatePair();
                    map.put(record.getReadName(), mpair);
                }
                mpair.add(record);
            }
        }
        reader.close();
        return map;
    }

    protected static Set<String> createFastqReadHeaderSet(final File file) {
        final Set<String> set = new HashSet<>();
        try (final FastqReader freader = new FastqReader(file)) {
            while (freader.hasNext()) {
                final FastqRecord frec = freader.next();
                set.add(frec.getReadName());
            }
        }
        return set ;
    }

    static class MatePair {
        SAMRecord mate1 ;
        SAMRecord mate2 ;
        void add(final SAMRecord record) {
            if (!record.getReadPairedFlag()) throw new PicardException("Record "+record.getReadName()+" is not paired");
            if (record.getFirstOfPairFlag()) {
                if (mate1 != null) throw new PicardException("Mate 1 already set for record: "+record.getReadName());
                mate1 = record ;
            }
            else if (record.getSecondOfPairFlag()) {
                if (mate2 != null) throw new PicardException("Mate 2 already set for record: "+record.getReadName());
                mate2 = record ;
            }
            else throw new PicardException("Neither FirstOfPairFlag or SecondOfPairFlag is set for a paired record");
        }
    }
}
