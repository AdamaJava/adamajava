package org.qcmg.sig.util;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntShortHashMap;
import org.apache.commons.math3.util.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.PositionRange;
import org.qcmg.common.util.ChrPositionCache;
import org.qcmg.qio.illumina.IlluminaRecord;
import org.qcmg.sig.CompareTest;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.model.SigMeta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;


public class SignatureUtilTest {


    private Path tempFile;
    private Map<String, List<PositionRange>> testMap;
    private Map<String, List<PositionRange>> blockedPositions;


    @org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	public static final List<String> BAM_HEADER = Arrays.asList("##fileformat=VCFv4.2",
			"##datetime=2016-08-18T14:37:22.871",
			"##program=SignatureGeneratorBespoke",
			"##version=1.0 (1230)",
			"##java_version=1.8.0_71",
			"##run_by_os=Linux",
			"##run_by_user=oliverH",
			"##positions=/software/genomeinfo/configs/qsignature/qsignature_positions.txt",
			"##positions_md5sum=d18c99f481afbe04294d11deeb418890",
			"##positions_count=1456203",
			"##filter_base_quality=10",
			"##filter_mapping_quality=10",
			"##illumina_array_design=/software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt",
			"##cmd_line=SignatureGeneratorBespoke -i /software/genomeinfo/configs/qsignature/qsignature_positions.txt -illumina /software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt -i /mnt/lustre/home/oliverH/qbammerge/dodgy_sample_merge/dodgy_exome.bam -log /mnt/lustre/home/oliverH/qsignature/bespoke/siggenbes.log -d /mnt/lustre/home/oliverH/qsignature/bespoke/",
			"##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">",
			"##input=/mnt/lustre/home/oliverH/qbammerge/dodgy_sample_merge/dodgy_exome.bam",
			"##rg1=fc17fe15-6c1a-42aa-9270-0787d84c8001",
			"##rg2=14989e3c-e669-46c2-866d-a8c504679743",
			"#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
	
	public static final List<String> BAM_HEADER_OLD_SKOOL = Arrays.asList("##fileformat=VCFv4.2",
			"##fileDate=20171206",
			"##qUUID=389f2b7f-68f9-4393-8053-76ba54fb16ce",
			"##bam_file=/mnt/lustre/working/genomeinfo/sample/f/9/f92864a0-42c8-4f11-895e-5766e1e008c7/aligned_read_group_set/b141eb5f-b556-4d3b-a2e3-b7ea39d56e00.bam",
			"##snp_file=/software/genomeinfo/configs/qsignature/qsignature_positions.txt",
			"##FILTER=<ID=MAPPING_QUALITY,Description=\"Mapping quality < 10\">",
			"##FILTER=<ID=BASE_QUALITY,Description=\"Base quality < 10\">",
			"##INFO=<ID=NOVELCOV,Number=-1,Type=String,Description=\"bases at position from reads with novel starts\">",
			"##INFO=<ID=FULLCOV,Number=-1,Type=String,Description=\"all bases at position\">",
			"#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO");
	
	public static final List<String> SNP_CHIP_HEADER = Arrays.asList("##fileformat=VCFv4.2",
			"##datetime=2018-04-17T16:03:13.028",
			"##program=SignatureGeneratorBespoke",
			"##version=1.0 (2477)",
			"##java_version=1.8.0_152",
			"##run_by_os=Linux",
			"##run_by_user=oliverH",
			"##positions=/software/genomeinfo/configs/qsignature/qsignature_positions.txt",
			"##positions_md5sum=d18c99f481afbe04294d11deeb418890",
			"##positions_count=1456203",
			"##filter_gc_score=0.7",
			"##illumina_array_design=/software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt",
			"##cmd_line=SignatureGeneratorBespoke -i /mnt/lustre/working/genomeinfo/data/20180309_KNArrays/202047900199_R05C01.array.txt -d /mnt/backedup/home/oliverH/qsignature -snpPositions /software/genomeinfo/configs/qsignature/qsignature_positions.txt -illuminaArraysDesign /software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt -log /mnt/backedup/home/oliverH/qsignature/202047900199_R05C01.array.txt.qsig.log",
			"##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">",
			"##input=/mnt/lustre/working/genomeinfo/data/20180309_KNArrays/202047900199_R05C01.array.txt",
			"#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");


    @Before
    public void setUp() {
        testMap = new HashMap<>();
        blockedPositions = new HashMap<>();

    }

    @After
    public void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    @Test
    public void testPositionNotBlocked() {
        // Given
        String chrPosString = "chr1\t12345";
        List<PositionRange> ranges = Arrays.asList(
                new PositionRange(1000, 2000),
                new PositionRange(5000, 6000)
        );
        blockedPositions.put("chr1", ranges);

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertFalse("Should not continue when position is not blocked", shouldContinue);
    }

    @Test
    public void testPositionIsBlocked() {
        // Given
        String chrPosString = "chr1\t1500";
        List<PositionRange> ranges = Arrays.asList(
                new PositionRange(1000, 2000),
                new PositionRange(5000, 6000)
        );
        blockedPositions.put("chr1", ranges);

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertTrue("Should continue when position is blocked", shouldContinue);
    }

    @Test
    public void testPositionAtRangeBoundary() {
        // Given - test position at start of range
        String chrPosString1 = "chr2\t1000";
        List<PositionRange> ranges = List.of(new PositionRange(1000, 2000));
        blockedPositions.put("chr2", ranges);

        // When
        boolean shouldContinue1 = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString1.indexOf('\t');
            String chr = chrPosString1.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString1, tabIndex + 1, chrPosString1.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue1 = true;
                }
            }
        }

        // Given - test position at end of range
        String chrPosString2 = "chr2\t2000";
        boolean shouldContinue2 = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString2.indexOf('\t');
            String chr = chrPosString2.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString2, tabIndex + 1, chrPosString2.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue2 = true;
                }
            }
        }

        // Then
        assertTrue("Should continue when position is at start of blocked range", shouldContinue1);
        assertTrue("Should continue when position is at end of blocked range", shouldContinue2);
    }

    @Test
    public void testMultipleRangesOneBlocked() {
        // Given
        String chrPosString = "chr3\t5500";
        List<PositionRange> ranges = Arrays.asList(
                new PositionRange(1000, 2000),
                new PositionRange(3000, 4000),
                new PositionRange(5000, 6000)
        );
        blockedPositions.put("chr3", ranges);

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertTrue("Should continue when position matches one of multiple ranges", shouldContinue);
    }

    @Test
    public void testDifferentChromosomes() {
        // Given
        String chrPosString = "chrX\t1500";
        List<PositionRange> ranges = List.of(new PositionRange(1000, 2000));
        blockedPositions.put("chr1", ranges);

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertFalse("Should not continue when chromosome is not in blocked list", shouldContinue);
    }

    @Test
    public void testEmptyRangeList() {
        // Given
        String chrPosString = "chr1\t12345";
        blockedPositions.put("chr1", new ArrayList<>());

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertFalse("Should not continue when range list is empty", shouldContinue);
    }

    @Test(expected = NumberFormatException.class)
    public void testInvalidPosition() {
        // Given
        String chrPosString = "chr1\tabc";
        List<PositionRange> ranges = List.of(new PositionRange(1000, 2000));
        blockedPositions.put("chr1", ranges);

        // When
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                // This should throw NumberFormatException
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
            }
        }

        // Then - expect NumberFormatException
    }

    @Test
    public void testChrPositionStringParsing() {
        // Given
        String chrPosString = "chr15\t123456789";
        List<PositionRange> ranges = List.of(new PositionRange(123456700, 123456800));
        blockedPositions.put("chr15", ranges);

        // When
        boolean shouldContinue = false;
        if (null != blockedPositions) {
            int tabIndex = chrPosString.indexOf('\t');
            String chr = chrPosString.substring(0, tabIndex);
            List<PositionRange> list = blockedPositions.get(chr);
            if (null != list) {
                int pos = Integer.parseInt(chrPosString, tabIndex + 1, chrPosString.length(), 10);
                boolean blocked = list.stream().anyMatch(r -> r.containsPosition(pos));
                if (blocked) {
                    shouldContinue = true;
                }
            }
        }

        // Then
        assertEquals("Should correctly parse chromosome", "chr15", chrPosString.substring(0, chrPosString.indexOf('\t')));
        assertEquals("Should correctly parse position", 123456789,
                Integer.parseInt(chrPosString, chrPosString.indexOf('\t') + 1, chrPosString.length(), 10));
        assertTrue("Should continue when large position is in blocked range", shouldContinue);
    }



    @Test
	public void testGetPatientFromFile() {
		assertEquals("AAAA_1234", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_1234/SNP_array/1234.txt")));
		assertEquals("AAAA_9999", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_9999/SNP_array/5555.txt")));
		assertEquals("AAAA_333", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_333/SNP_array/5555.txt")));
		assertEquals("UNKNOWN", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_33/SNP_array/5555.txt")));
	}
	
	@Test
	public void getGenotypesAsByte() throws IOException {
		List<String> trad1Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER_OLD_SKOOL);
		File testVcf = testFolder.newFile("test.vcf");
		String evenData = "\tcnvi0147523\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:55,T:0,N:0,TOTAL:55;NOVELCOV=A:0,C:0,G:49,T:0,N:0,TOTAL:49\t";
		String oddData = "\tcnvi0147523\tG\t.\t.\t.\tFULLCOV=A:20,C:0,G:35,T:0,N:0,TOTAL:55;NOVELCOV=A:20,C:0,G:29,T:0,N:0,TOTAL:49\t";
		for (int i = 0 ; i < 10 ; i++ ) {
			trad1Data.add("chr1\t" + i + (i % 2 == 0 ? evenData : oddData));
		}
		CompareTest.writeDataToFile(trad1Data, testVcf);
		TIntByteHashMap byteMap = SignatureUtil.loadSignatureRatiosFloatGenotypeNew(testVcf);
		assertEquals(10, byteMap.size());
		/*
		 * values in map will be either HOM_G or HET_AG
		 */
		
		
		/*
		 * need to use ChrPositionCache.getStringIndex to get the index in the cache for the position.
		 * This test used to just use 0-9, but if the test was run after other tests that put entries into the cache, then it would fail.
		 * This fix should remove the 'Flaky' nature of this test 
		 */
		for (int i = 0 ; i < 10 ; i++ ) {
			assertEquals(i % 2 == 0 ? SignatureUtil.HOM_G : SignatureUtil.HET_AG, byteMap.get(ChrPositionCache.getStringIndex("chr1\t" + i)));
		}
	}
	
	@Test
	public void doesOldStyleHeaderReturnASigMeta() {
		//TabbedHeader h = new TabbedHeader(BAM_HEADER_OLD_SKOOL);
		Optional<Pair<SigMeta, Map<String, String>>> optional = SignatureUtil.getSigMetaAndRGsFromHeader(BAM_HEADER_OLD_SKOOL);
        assertTrue(optional.isPresent());
		SigMeta sm = optional.get().getKey();
        assertFalse(sm.isValid());
	}
	
	@Test
	public void doContigsStartWithDigit() {
        assertTrue(SignatureUtil.doContigsStartWithDigit(List.of("1")));
        assertFalse(SignatureUtil.doContigsStartWithDigit(List.of("c1")));
        assertFalse(SignatureUtil.doContigsStartWithDigit(List.of("chr1")));
        assertTrue(SignatureUtil.doContigsStartWithDigit(Arrays.asList("chr1", "2")));
        assertFalse(SignatureUtil.doContigsStartWithDigit(Arrays.asList("chr1", "X2")));
	}
	
	@Test
	public void getSigMetaEmptyHeader() {
		assertEquals(Optional.empty(), SignatureUtil.getSigMetaAndRGsFromHeader(null));
	}
	
	@Test
	public void getSigMetaBam() {
		Optional<Pair<SigMeta, Map<String, String>>> o =SignatureUtil.getSigMetaAndRGsFromHeader(BAM_HEADER);
        assertTrue(o.isPresent());

        assertTrue(o.get().getFirst().isValid());			// valid SigMeta
        assertFalse(o.get().getSecond().isEmpty());	// non-empty rg map
		assertEquals(2, o.get().getSecond().size());				// non-empty rg map

        assertTrue(o.get().getSecond().containsKey("rg1"));
        assertTrue(o.get().getSecond().containsKey("rg2"));
		assertEquals("fc17fe15-6c1a-42aa-9270-0787d84c8001", o.get().getSecond().get("rg1"));
		assertEquals("14989e3c-e669-46c2-866d-a8c504679743", o.get().getSecond().get("rg2"));
	}
	
	@Test
	public void getSigMetaSnpChip() {
		Optional<Pair<SigMeta, Map<String, String>>> o =SignatureUtil.getSigMetaAndRGsFromHeader(SNP_CHIP_HEADER);
        assertTrue(o.isPresent());

        assertTrue(o.get().getFirst().isValid());			// valid SigMeta
        assertTrue(o.get().getSecond().isEmpty());	// non-empty rg map
		assertEquals(0, o.get().getSecond().size());				// non-empty rg map
	}
	
	@Test
	public void canSigMEtasBeCompared() {
		Optional<Pair<SigMeta, Map<String, String>>> o =SignatureUtil.getSigMetaAndRGsFromHeader(SNP_CHIP_HEADER);
        assertTrue(o.isPresent());
		SigMeta snpChpSM = o.get().getFirst();
		
		o =SignatureUtil.getSigMetaAndRGsFromHeader(BAM_HEADER);
        assertTrue(o.isPresent());
		SigMeta bamSM = o.get().getFirst();

        assertTrue(SigMeta.suitableForComparison(snpChpSM, bamSM));
        assertTrue(SigMeta.suitableForComparison(bamSM, snpChpSM));
	}
	
	@Test
	public void testGetPatternFromString() {
		try {
			SignatureUtil.getPatternFromString(null, null);
			Assert.fail("SHOULD have thrown an iae");
		} catch (IllegalArgumentException iae) {}
		try {
			SignatureUtil.getPatternFromString("blah", null);
			Assert.fail("SHOULD have thrown an iae");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(SignatureUtil.UNKNOWN, SignatureUtil.getPatternFromString("foo", "bar"));
		
		assertEquals("-ND_", SignatureUtil.getPatternFromString("-[A-Z]{2}_", "GGGG-ABMJ-26-ND_5760640025_R02C01.txt"));
		assertEquals("GGGG-ABMJ-20110329-26-ND", SignatureUtil.getPatternFromString("[A-Z]{4}-[A-Z]{4}-[0-9]{8}-[0-9]{2}-[A-Z]{2}", "GGGG-ABMJ-20110329-26-ND_5760640025_R02C01.txt"));
	}
	
	@Test
	public void wtfExomes() {
		List<String> oeso_0031CovArray = Arrays.asList("A:26,C:0,G:0,T:0,N:0,TOTAL:26;NOVELCOV",
"A:0,C:27,G:0,T:0,N:0,TOTAL:27;NOVELCOV",
"A:0,C:17,G:0,T:12,N:0,TOTAL:29;NOVELCOV",
"A:0,C:16,G:0,T:19,N:0,TOTAL:35;NOVELCOV",
"A:0,C:0,G:24,T:0,N:0,TOTAL:24;NOVELCOV",
"A:0,C:0,G:42,T:0,N:0,TOTAL:42;NOVELCOV",
"A:0,C:0,G:45,T:0,N:0,TOTAL:45;NOVELCOV",
"A:20,C:0,G:11,T:0,N:0,TOTAL:31;NOVELCOV",
"A:0,C:0,G:36,T:0,N:0,TOTAL:36;NOVELCOV",
"A:0,C:29,G:0,T:15,N:0,TOTAL:44;NOVELCOV",
"A:0,C:0,G:41,T:0,N:0,TOTAL:41;NOVELCOV",
"A:0,C:39,G:0,T:0,N:0,TOTAL:39;NOVELCOV",
"A:0,C:26,G:0,T:0,N:0,TOTAL:26;NOVELCOV",
"A:0,C:0,G:1,T:35,N:0,TOTAL:36;NOVELCOV",
"A:0,C:43,G:0,T:0,N:0,TOTAL:43;NOVELCOV",
"A:13,C:1,G:11,T:0,N:0,TOTAL:25;NOVELCOV",
"A:0,C:0,G:40,T:0,N:0,TOTAL:40;NOVELCOV");
		
		int [] indicies = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
		List<Optional<float[]>> oeso_0031Floats = oeso_0031CovArray.stream().map(SignatureUtil::getValuesFromCoverageStringFloat).toList();
		List<Byte> oeso_0031Shorts = oeso_0031Floats.stream().map(of -> SignatureUtil.getCodedGenotypeAsByte(of.get())).toList();
		oeso_0031Shorts.forEach(System.out::println);
		
		List<String> oeso_0050CovArray = Arrays.asList("A:25,C:0,G:31,T:0,N:0,TOTAL:56;NOVELCOV",
"A:0,C:21,G:0,T:0,N:0,TOTAL:21;NOVELCOV",
"A:0,C:97,G:0,T:101,N:0,TOTAL:198;NOVELCOV",
"A:0,C:162,G:0,T:0,N:0,TOTAL:162;NOVELCOV",
"A:0,C:0,G:109,T:0,N:0,TOTAL:109;NOVELCOV",
"A:0,C:0,G:20,T:8,N:0,TOTAL:28;NOVELCOV",
"A:0,C:42,G:45,T:0,N:0,TOTAL:87;NOVELCOV",
"A:15,C:0,G:15,T:0,N:0,TOTAL:30;NOVELCOV",
"A:0,C:0,G:323,T:0,N:0,TOTAL:323;NOVELCOV",
"A:0,C:39,G:0,T:32,N:0,TOTAL:71;NOVELCOV",
"A:0,C:0,G:139,T:1,N:0,TOTAL:140;NOVELCOV",
"A:0,C:35,G:0,T:27,N:0,TOTAL:62;NOVELCOV",
"A:0,C:32,G:0,T:0,N:0,TOTAL:32;NOVELCOV",
"A:0,C:0,G:40,T:0,N:0,TOTAL:40;NOVELCOV",
"A:0,C:101,G:0,T:0,N:0,TOTAL:101;NOVELCOV",
"A:22,C:0,G:19,T:0,N:0,TOTAL:41;NOVELCOV",
"A:18,C:0,G:10,T:0,N:0,TOTAL:28;NOVELCOV");
		
		List<Optional<float[]>> oeso_0050Floats = oeso_0050CovArray.stream().map(SignatureUtil::getValuesFromCoverageStringFloat).toList();
		List<Byte> oeso_0050Shorts = oeso_0050Floats.stream().map(of -> SignatureUtil.getCodedGenotypeAsByte(of.get())).toList();
		oeso_0050Shorts.forEach(System.out::println);
		
		List<String> pn400007CovArray = Arrays.asList("A:70,C:1,G:0,T:0,N:0,TOTAL:71;NOVELCOV",
"A:0,C:84,G:0,T:0,N:0,TOTAL:84;NOVELCOV",
"A:0,C:41,G:0,T:34,N:0,TOTAL:75;NOVELCOV",
"A:0,C:44,G:0,T:0,N:0,TOTAL:44;NOVELCOV",
"A:0,C:0,G:45,T:0,N:0,TOTAL:45;NOVELCOV",
"A:0,C:1,G:48,T:0,N:0,TOTAL:49;NOVELCOV",
"A:0,C:0,G:40,T:0,N:0,TOTAL:40;NOVELCOV",
"A:38,C:0,G:29,T:0,N:0,TOTAL:67;NOVELCOV",
"A:0,C:0,G:43,T:0,N:0,TOTAL:43;NOVELCOV",
"A:0,C:0,G:3,T:55,N:0,TOTAL:58;NOVELCOV",
"A:0,C:0,G:57,T:0,N:0,TOTAL:57;NOVELCOV",
"A:0,C:0,G:0,T:44,N:0,TOTAL:44;NOVELCOV",
"A:0,C:41,G:0,T:0,N:0,TOTAL:41;NOVELCOV",
"A:0,C:0,G:47,T:0,N:0,TOTAL:47;NOVELCOV",
"A:0,C:49,G:0,T:0,N:0,TOTAL:49;NOVELCOV",
"A:36,C:1,G:0,T:0,N:0,TOTAL:37;NOVELCOV",
"A:0,C:0,G:50,T:0,N:0,TOTAL:50;NOVELCOV");
		
		List<Optional<float[]>> pn400007Floats = pn400007CovArray.stream().map(SignatureUtil::getValuesFromCoverageStringFloat).toList();
		List<Byte> pn400007Shorts = pn400007Floats.stream().map(of -> SignatureUtil.getCodedGenotypeAsByte(of.get())).toList();
		pn400007Shorts.forEach(System.out::println);
		
		TIntShortHashMap oeso_0031Map = new TIntShortHashMap();
		TIntShortHashMap oeso_0050Map = new TIntShortHashMap();
		TIntShortHashMap pn400007Map = new TIntShortHashMap();
		for (int i : indicies) {
			oeso_0031Map.put(i, oeso_0031Shorts.get(i -1));
			oeso_0050Map.put(i, oeso_0050Shorts.get(i -1));
			pn400007Map.put(i, pn400007Shorts.get(i -1));
		}
		
		Comparison c = ComparisonUtil.compareRatiosUsingSnpsFloat(oeso_0031Map, oeso_0050Map, new File("oeso_0031"), new File("oeso_0050"));
		System.out.println("c: " + c);
		Comparison c1 = ComparisonUtil.compareRatiosUsingSnpsFloat(oeso_0031Map, pn400007Map, new File("oeso_0031"), new File("pn400007"));
		System.out.println("c1: " + c1);
	}
	
	@Test
	public void getBespokeArray() {
		try {
			 SignatureUtil.decipherCoverageStringBespoke(null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			SignatureUtil.decipherCoverageStringBespoke("");
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("1"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("1,2,3,4"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("1-2-3,4"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("1-2-3-4-"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("1---3-4-"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("-----"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("-"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("--"));
		Assert.assertEquals(Optional.empty(), SignatureUtil.decipherCoverageStringBespoke("-1-"));
		
		Assert.assertArrayEquals(new int[]{1,2,3,4}, SignatureUtil.decipherCoverageStringBespoke("1-2-3-4").get());
		Assert.assertArrayEquals(new int[]{1000,100,10,1}, SignatureUtil.decipherCoverageStringBespoke("1000-100-10-1").get());
		Assert.assertArrayEquals(new int[]{1000,1000,1000,1000}, SignatureUtil.decipherCoverageStringBespoke("1000-1000-1000-1000").get());
		Assert.assertArrayEquals(new int[]{2,1000,20,10}, SignatureUtil.decipherCoverageStringBespoke("2-1000-20-10").get());
	}
	
	@Test
	public void bespokeGenotype() throws IOException {
		File f = testFolder.newFile();
		createBamVcfFile(f);
		
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p = SignatureUtil.loadSignatureGenotype(f, 10, 10);
		assertEquals(7, p.getSecond().get("all").size());
		assertEquals(3, p.getSecond().get("rg1").size());
		assertEquals(2, p.getSecond().get("rg2").size());
		
		/*
		 * adjust the rg min coverage value
		 * Note that the minCov value will be used first for the total value before the rg specific value is used for the individual rgs
		 */
		 p = SignatureUtil.loadSignatureGenotype(f, 10, 5);
		assertEquals(7, p.getSecond().get("all").size());
		assertEquals(7, p.getSecond().get("rg1").size());
		assertEquals(5, p.getSecond().get("rg2").size());
	}
	
	@Test
	public void getVAFDist() throws IOException {
		File f = testFolder.newFile();
		try (FileWriter w = new FileWriter(f)){
			// add header
			for (String s : BAM_HEADER) {
				w.write(s + "\n");
			}
			for (short i = 0 ; i < 10 ; i++) {
				w.write("chr1	10295" + i + "	cnvi0120648	T	.	.	.	FULLCOV=A:0,C:0,G:0,T:35,N:0,TOTAL:35;NOVELCOV=A:0,C:0,G:0,T:32,N:0,TOTAL:32\n");
			}
			for (short i = 0 ; i < 10 ; i++) {
				w.write("chr2	10295" + i + "	cnvi0120648	T	.	.	.	FULLCOV=A:0,C:20,G:0,T:20,N:0,TOTAL:40;NOVELCOV=A:0,C:0,G:0,T:32,N:0,TOTAL:32\n");
			}
			for (short i = 0 ; i < 10 ; i++) {
				w.write("chr3	10295" + i + "	cnvi0120648	T	.	.	.	FULLCOV=A:0,C:25,G:25,T:0,N:0,TOTAL:50;NOVELCOV=A:0,C:0,G:0,T:32,N:0,TOTAL:32\n");
			}
		}
		
		Map<Short, int[]> dist = SignatureUtil.getVariantAlleleFractionDistribution(f, 20);
		for (short s = 0 ; s <=100 ; s++) {
			switch (s) {
			case 0, 50, 100:
				Assert.assertArrayEquals(new int[]{10,0}, dist.get(s)); break;
                default :
				Assert.assertArrayEquals(null, dist.get(s));
			}
		}
	}
	
	@Test
	public void compareBams() throws IOException {
		File f1 = testFolder.newFile();
		File f2 = testFolder.newFile();
		createBamVcfFile(f1);
		createBamVcfFile(f2);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p1 = SignatureUtil.loadSignatureGenotype(f1,10,10);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p2 = SignatureUtil.loadSignatureGenotype(f2,10,10);
		Pair<SigMeta, TIntByteHashMap> ratios1 = new Pair<>(p1.getKey(), p1.getSecond().get("all"));
		Pair<SigMeta, TIntByteHashMap> ratios2 = new Pair<>(p2.getKey(), p2.getSecond().get("all"));
		Comparison comp = ComparisonUtil.compareRatiosUsingSnpsFloat(ratios1.getValue(), ratios2.getValue(), f1, f2);
		assertEquals(1.0, comp.getScore(), 0.0001d);
		assertEquals(7, comp.getOverlapCoverage());	// 7 of the 11 have coverage over 10
	}
	@Test
	public void compareSnpChips() throws IOException {
		File f1 = testFolder.newFile();
		File f2 = testFolder.newFile();
		createSnpChipVcfFile(f1);
		createSnpChipVcfFile(f2);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p1 = SignatureUtil.loadSignatureGenotype(f1,10,10);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p2 = SignatureUtil.loadSignatureGenotype(f2,10,10);
		Pair<SigMeta, TIntByteHashMap> ratios1 = new Pair<>(p1.getKey(), p1.getSecond().get("all"));
		Pair<SigMeta, TIntByteHashMap> ratios2 = new Pair<>(p2.getKey(), p2.getSecond().get("all"));
		Comparison comp = ComparisonUtil.compareRatiosUsingSnpsFloat(ratios1.getValue(), ratios2.getValue(), f1, f2);
		assertEquals(1.0, comp.getScore(), 0.0001d);
		assertEquals(11, comp.getOverlapCoverage());	// 7 of the 11 have coverage over 10
	}
	@Test
	public void compareSnpChipAndBam() throws IOException {
		File f1 = testFolder.newFile();
		File f2 = testFolder.newFile();
		createBamVcfFile(f1);
		createSnpChipVcfFile(f2);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p1 = SignatureUtil.loadSignatureGenotype(f1,10,10);
		Pair<SigMeta, TMap<String, TIntByteHashMap>> p2 = SignatureUtil.loadSignatureGenotype(f2,10,10);
		Pair<SigMeta, TIntByteHashMap> ratios1 = new Pair<>(p1.getKey(), p1.getSecond().get("all"));
		Pair<SigMeta, TIntByteHashMap> ratios2 = new Pair<>(p2.getKey(), p2.getSecond().get("all"));
		Comparison comp = ComparisonUtil.compareRatiosUsingSnpsFloat(ratios1.getValue(), ratios2.getValue(), f1, f2);
		assertEquals(0.0, comp.getScore(), 0.0001d);
		assertEquals(7, comp.getOverlapCoverage());	// 7 of the 11 have coverage over 10
	}
	
	@Test
	public void getVAF() {
		int [] counts = new int[] {10,0,0,0,10};
		assertEquals(0.0f, SignatureUtil.getVAF(counts, "A"), 0.0001);
		assertEquals(1.0f, SignatureUtil.getVAF(counts, "C"), 0.0001);
		assertEquals(1.0f, SignatureUtil.getVAF(counts, "G"), 0.0001);
		assertEquals(1.0f, SignatureUtil.getVAF(counts, "T"), 0.0001);
		
		counts = new int[] {10,10,0,0,20};
		assertEquals(0.5f, SignatureUtil.getVAF(counts, "A"), 0.0001);
		assertEquals(0.5f, SignatureUtil.getVAF(counts, "C"), 0.0001);
		assertEquals(1.0f, SignatureUtil.getVAF(counts, "G"), 0.0001);
		assertEquals(1.0f, SignatureUtil.getVAF(counts, "T"), 0.0001);
	}
	
	@Test
	public void getShortFromFloat() {
		assertEquals(100, SignatureUtil.getFloatAsShort(1f));
		assertEquals(10000, SignatureUtil.getFloatAsShort(100f));
		assertEquals(5055, SignatureUtil.getFloatAsShort(50.55f));
	}
	
	@Test
	public void genotypeAsByte() {
		/*
		 * AAs and BBs
		 */
		//10000 or HOM_A
		assertEquals(SignatureUtil.HOM_A, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.91f,0.0f,0.0f,0.09f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.91f, 0.0f, 0.0f, 0.09f})));
		//100000
		assertEquals(SignatureUtil.HOM_C, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.01f,0.99f,0.0f,0.00f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.01f, 0.99f, 0.0f, 0.00f})));
		//1000000
		assertEquals(SignatureUtil.HOM_G, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.01f,0.09f,0.909f,0.00f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.01f, 0.09f, 0.909f, 0.00f})));
		//10000000
		assertEquals(SignatureUtil.HOM_T, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.0f,0.0f,1.0f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.0f, 0.0f, 1.0f})));
		
		/*
		 * ABs
		 */
		assertEquals(SignatureUtil.HET_AC, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f,0.5f,0.0f,0.0f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f, 0.5f, 0.0f, 0.0f})));
		assertEquals(SignatureUtil.HET_AT, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f,0.0f,0.0f,0.4f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f, 0.0f, 0.0f, 0.4f})));
		assertEquals(SignatureUtil.HET_CT, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.4f,0.0f,0.4f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.4f, 0.0f, 0.4f})));
		assertEquals(SignatureUtil.HET_CG, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.4f,0.6f,0.0f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.4f, 0.6f, 0.0f})));
		assertEquals(SignatureUtil.HET_GT, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.0f,0.59f,0.59f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.0f, 0.59f, 0.59f})));
		assertEquals(SignatureUtil.HET_AG, SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f,0.0f,0.5f,0.0f}));
        assertTrue(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f, 0.0f, 0.5f, 0.0f})));
		
		/*
		 * invalid
		 */
		// this is 3 lots of 0.32 - invalid
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.32f, 0.32f, 0.32f, 0.0f})));
		
		
		assertEquals("1", Integer.toBinaryString(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f,0.0f,0.0f,0.0f})));
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f, 0.0f, 0.0f, 0.0f})));
		assertEquals("1111", Integer.toBinaryString(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f,0.5f,0.5f,0.5f})));
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.5f, 0.5f, 0.5f, 0.5f})));
		assertEquals("1000", Integer.toBinaryString(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.0f,0.0f,0.4f})));
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.0f, 0.0f, 0.4f})));
		assertEquals("10", Integer.toBinaryString(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.4f,0.0f,0.0f})));
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.4f, 0.0f, 0.0f})));
		assertEquals("100", Integer.toBinaryString(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f,0.0f,0.59f,0.0f})));
        assertFalse(SignatureUtil.isCodedGenotypeValid(SignatureUtil.getCodedGenotypeAsByte(new float[]{0.0f, 0.0f, 0.59f, 0.0f})));
	}
	
	@Test
	public void testGetCoverageStringFromCharsAndInts() {
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('\u0000', '\u0000', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'A', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('X', 'Y', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('Z', 'Z', 0, 0));

        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 0).contains("A:1,C:0,G:0,T:0,N:0,TOTAL:1"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 1).contains("A:1,C:1,G:0,T:0,N:0,TOTAL:2"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 2, 1).contains("A:2,C:1,G:0,T:0,N:0,TOTAL:3"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 2, 2).contains("A:2,C:2,G:0,T:0,N:0,TOTAL:4"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 299, 2).contains("A:299,C:2,G:0,T:0,N:0,TOTAL:301"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 201).contains("A:1,C:201,G:0,T:0,N:0,TOTAL:202"));
        assertTrue(SignatureUtil.getCoverageStringFromCharsAndInts('A', 'A', 1, 200).contains("A:201,C:0,G:0,T:0,N:0,TOTAL:201"));
	}
	
	@Test
	public void testGetExcludesFromFile() throws IOException {
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile(null).size());
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile("").size());
		
		File excludesFile = testFolder.newFile("excludesFile");
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		try (FileWriter writer = new FileWriter(excludesFile)){
			writer.write("excludesFile1");
		}
		
		assertEquals(1, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile1", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).getFirst());
		
		try (FileWriter writer = new FileWriter(excludesFile, true)){
			writer.write("excludesFile2");
		}
		assertEquals(1, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile1excludesFile2", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).getFirst());
		
		try (FileWriter writer = new FileWriter(excludesFile, true)){
			for (int i = 3 ; i <= 10 ; i++) {
				writer.append("\nexcludesFile").append(String.valueOf(i));
			}
		}
		
		assertEquals(9, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile10", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).get(8));
	}
	
	@Test
	public void testRemoveExcludedFilesFromList() {
        assertNull(SignatureUtil.removeExcludedFilesFromList(null, null));
		assertEquals(0, SignatureUtil.removeExcludedFilesFromList(new ArrayList<>(), null).size());
		assertEquals(0, SignatureUtil.removeExcludedFilesFromList(new ArrayList<>(), new ArrayList<>()).size());
		List<File> files = new ArrayList<>();
		files.add(new File("file1"));
		assertEquals(1, SignatureUtil.removeExcludedFilesFromList(files, null).size());
		files.add(new File("file2"));
		List<String> excludes = new ArrayList<>();
		assertEquals(2, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file2"));
		excludes.add("file3");
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file3"));
		excludes.add("file4");
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file4"));
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
	}
	
	public static void writeSignatureFile(File signatureFile, String data) throws IOException {
		try (FileWriter writer = new FileWriter(signatureFile)){
			// add header
			writer.write("##test vcf file for use in SignatureUtilTest\n#CHROM POS     ID        REF ALT    QUAL FILTER INFO\n");
			// add data
			writer.write(data);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString() {
		SignatureUtil.decipherCoverageString(null);
	}
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString2() {
		SignatureUtil.decipherCoverageString("");
	}
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString3() {
		SignatureUtil.decipherCoverageString("blah-dee-blah");
	}
	
	@Test
	public void validCoverageString() {
		String coverage = "FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[0]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[1]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[2]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[3]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[4]);
		coverage = "FULLCOV=A:1,C:2,G:3,T:4,N:0,TOTAL:10;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
		assertEquals(1, SignatureUtil.decipherCoverageString(coverage)[0]);
		assertEquals(2, SignatureUtil.decipherCoverageString(coverage)[1]);
		assertEquals(3, SignatureUtil.decipherCoverageString(coverage)[2]);
		assertEquals(4, SignatureUtil.decipherCoverageString(coverage)[3]);
		assertEquals(10, SignatureUtil.decipherCoverageString(coverage)[4]);
	}
	
	@Test
	public void unambiguousIlluminaRecord() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		 
		illRecParams[20] = "[A/C]";	// snp
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[22] = "TOP";	// strand
		illRecParams[30] = "0.5";	// b-allele freq
		
		testResults(illRecParams, 0,1);
		
		illRecParams[20] = "[A/G]";	// snp
		testResults(illRecParams, 0,2);
		
		illRecParams[20] = "[T/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,3);
		illRecParams[20] = "[T/G]";	// snp
		testResults(illRecParams, 2,3);
	}
	
	@Test
	public void ambiguousIlluminaRecordAB() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,2);
	}
	
	@Test
	public void ambiguousIlluminaRecordAA() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "A";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1);
	}
	
	@Test
	public void ambiguousIlluminaRecordBB() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "B";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1);
	}

    @Test
    public void testLoadBlockListIntoMap_ValidData() throws IOException {
        String content = """
                chr1\t100\t200
                chr2\t300\t400\textra_column
                chr1\t500\t600
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertTrue(testMap.containsKey("chr1"));
        assertTrue(testMap.containsKey("chr2"));

        List<PositionRange> chr1Ranges = testMap.get("chr1");
        assertEquals(2, chr1Ranges.size());
        assertEquals(new PositionRange(100, 200), chr1Ranges.get(0));
        assertEquals(new PositionRange(500, 600), chr1Ranges.get(1));

        List<PositionRange> chr2Ranges = testMap.get("chr2");
        assertEquals(1, chr2Ranges.size());
        assertEquals(new PositionRange(300, 400), chr2Ranges.getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_EmptyFile() throws IOException {
        Path file = createTempFileWithContent("");

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertTrue(testMap.isEmpty());
    }

    @Test
    public void testLoadBlockListIntoMap_CommentsAndEmptyLines() throws IOException {
        String content = """
                # This is a comment
                
                chr1\t100\t200
                # Another comment
                
                chr2\t300\t400
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertEquals(1, testMap.get("chr1").size());
        assertEquals(1, testMap.get("chr2").size());
    }

    @Test
    public void testLoadBlockListIntoMap_MalformedLines() throws IOException {
        String content = """
                chr1\t100\t200
                malformed_line
                chr2
                chr3\t300
                chr4\t400\t500
                chr5\tinvalid\t600
                chr6\t700\tinvalid
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertTrue(testMap.containsKey("chr1"));
        assertTrue(testMap.containsKey("chr4"));
        assertEquals(new PositionRange(100, 200), testMap.get("chr1").getFirst());
        assertEquals(new PositionRange(400, 500), testMap.get("chr4").getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_LineEndingWithTab() throws IOException {
        String content = """
                chr1\t100\t
                chr2\t200\t300
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(1, testMap.size());
        assertTrue(testMap.containsKey("chr2"));
        assertEquals(new PositionRange(200, 300), testMap.get("chr2").getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_NumberFormatException() throws IOException {
        String content = """
                chr1\t100\t200
                chr2\tabc\t300
                chr3\t400\txyz
                chr4\t500\t600
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertTrue(testMap.containsKey("chr1"));
        assertTrue(testMap.containsKey("chr4"));
        assertEquals(new PositionRange(100, 200), testMap.get("chr1").getFirst());
        assertEquals(new PositionRange(500, 600), testMap.get("chr4").getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_ExistingMapWithData() throws IOException {
        // Pre-populate the map
        testMap.put("chr0", new ArrayList<>());
        testMap.get("chr0").add(new PositionRange(1, 10));

        String content = "chr1\t100\t200\n";
        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertTrue(testMap.containsKey("chr0"));
        assertTrue(testMap.containsKey("chr1"));
        assertEquals(new PositionRange(1, 10), testMap.get("chr0").getFirst());
        assertEquals(new PositionRange(100, 200), testMap.get("chr1").getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_SameContigMultipleRanges() throws IOException {
        String content = """
                chr1\t100\t200
                chr1\t300\t400
                chr1\t500\t600
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(1, testMap.size());
        List<PositionRange> ranges = testMap.get("chr1");
        assertEquals(3, ranges.size());
        assertEquals(new PositionRange(100, 200), ranges.get(0));
        assertEquals(new PositionRange(300, 400), ranges.get(1));
        assertEquals(new PositionRange(500, 600), ranges.get(2));
    }

    @Test
    public void testLoadBlockListIntoMap_WithExtraColumns() throws IOException {
        String content = """
                chr1\t100\t200\textra1\textra2
                chr2\t300\t400\textra
                """;

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        assertEquals(2, testMap.size());
        assertEquals(new PositionRange(100, 200), testMap.get("chr1").getFirst());
        assertEquals(new PositionRange(300, 400), testMap.get("chr2").getFirst());
    }

    @Test
    public void testLoadBlockListIntoMap_NonExistentFile() {
        // This test verifies that the method handles IOException gracefully
        // The method should not throw an exception but should log the error
        SignatureUtil.loadBlockListIntoMap("non_existent_file.txt", testMap);

        // Map should remain empty
        assertTrue(testMap.isEmpty());
    }

    private Path createTempFileWithContent(String content) throws IOException {
        tempFile = Files.createTempFile("blocklist", ".txt");
        Files.writeString(tempFile, content);
        return tempFile;
    }


    @Test
    public void testLoadBlockListIntoMap_WhitespaceInValues() throws IOException {
        String content = """
                chr1\t100\t200
                 chr2 \t 300 \t 400\s
                """; // This should fail parsing due to spaces

        Path file = createTempFileWithContent(content);

        SignatureUtil.loadBlockListIntoMap(file.toString(), testMap);

        // Only chr1 should be parsed successfully due to whitespace in chr2 line
        assertEquals(1, testMap.size());
        assertTrue(testMap.containsKey("chr1"));
        assertEquals(new PositionRange(100, 200), testMap.get("chr1").getFirst());
    }


    private void testResults(String [] illRecParams, int ... positionsWithCoverage) {
		int coverageValue = 20 / positionsWithCoverage.length;
		
		IlluminaRecord illRec = new IlluminaRecord(illRecParams);
		String infoField = SignatureUtil.getCoverageStringForIlluminaRecord(illRec, new String[8], 20);
		int [] coverages = SignatureUtil.decipherCoverageString(infoField);
		
		for (int i = 0 ; i < coverages.length -1 ; i++) {
			if (Arrays.binarySearch(positionsWithCoverage, i) >= 0) {
				assertEquals(coverageValue, coverages[i]);
			} else {
				assertEquals(0, coverages[i]);
			}
		}
		assertEquals(20, coverages[4]);
	}
	
	private void createSnpChipVcfFile(File f) throws IOException {
		try (FileWriter w = new FileWriter(f)){
			// add header
			for (String s : SNP_CHIP_HEADER) {
				w.write(s+"\n");
			}
			w.write("chr1\t47851\trs3131972\tA\t.\t.\t.\tQAF=t:0-0-22-0\n");
			w.write("chr1\t50251\trs4970383\tC\t.\t.\t.\tQAF=t:0-37-0-0\n");
			w.write("chr1\t51938\trs7537756\tA\t.\t.\t.\tQAF=t:17-0-13-0\n");
			w.write("chr1\t52651\trs13302982\tA\t.\t.\t.\tQAF=t:0-0-25-0\n");
			w.write("chr1\t64251\trs3748597\tT\t.\t.\t.\tQAF=t:0-25-0-0\n");
			w.write("chr1\t98222\trs3935066\tG\t.\t.\t.\tQAF=t:19-0-0-0\n");
			w.write("chr1\t99236\trs28561399\tG\t.\t.\t.\tQAF=t:0-0-21-0\n");
			w.write("chr1\t101095\trs2341354\tA\t.\t.\t.\tQAF=t:12-0-9-0\n");
			w.write("chr1\t102954\trs2465136\tT\t.\t.\t.\tQAF=t:0-12-0-15\n");
			w.write("chr1\t104813\trs7526076\tA\t.\t.\t.\tQAF=t:16-0-11-0\n");
			w.write("chr1\t106222\trs9442372\tA\t.\t.\t.\tQAF=t:18-0-12-0\n");
		}
	}
	private void createBamVcfFile(File f) throws IOException {
		try (FileWriter w = new FileWriter(f)){
			// add header
			for (String s : BAM_HEADER) {
				w.write(s+"\n");
			}
			w.write("chr1	47851\t.\tC\t.\t.\t.\tQAF=t:0-12-0-0,rg1:0-9-0-0,rg2:0-3-0-0\n");
			w.write("chr1	50251\t.\tT\t.\t.\t.\tQAF=t:0-0-0-11,rg1:0-0-0-9,rg2:0-0-0-2\n");
			w.write("chr1\t51938	.\tT\t.\t.\t.\tQAF=t:0-0-0-9,rg1:0-0-0-5,rg2:0-0-0-4\n");
			w.write("chr1\t52651	.\tT\t.\t.\t.\tQAF=t:0-0-0-3,rg1:0-0-0-1,rg2:0-0-0-2\n");
			w.write("chr1\t64251	.\tA\t.\t.\t.\tQAF=t:9-0-0-0,rg1:5-0-0-0,rg2:4-0-0-0\n");
			w.write("chr1\t98222	.\tC\t.\t.\t.\tQAF=t:0-12-0-0,rg1:0-5-0-0,rg2:0-7-0-0\n");
			w.write("chr1\t99236	.\tT\t.\t.\t.\tQAF=t:0-0-0-22,rg1:0-0-0-12,rg2:0-0-0-10\n");
			w.write("chr1\t101095	.\tT\t.\t.\t.\tQAF=t:0-0-0-10,rg1:0-0-0-5,rg2:0-0-0-5\n");
			w.write("chr1\t102954	.\tT\t.\t.\t.\tQAF=t:0-0-1-64,rg1:0-0-0-36,rg2:0-0-1-28\n");
			w.write("chr1\t104813	.\tG\t.\t.\t.\tQAF=t:0-1-17-0,rg1:0-1-10-0,rg2:0-0-7-0\n");
			w.write("chr1\t106222	.\tT\t.\t.\t.\tQAF=t:0-0-0-4,rg1:0-0-0-1,rg2:0-0-0-3\n");
		}
	}
}
