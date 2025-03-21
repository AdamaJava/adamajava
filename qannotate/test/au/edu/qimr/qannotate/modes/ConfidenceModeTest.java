package au.edu.qimr.qannotate.modes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import static org.junit.Assert.*;


public class ConfidenceModeTest {
    @org.junit.Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public static final VcfFileMeta TWO_SAMPLE_TWO_CALLER_META = new VcfFileMeta(CCMModeTest.createTwoSampleTwoCallerVcf());
    public static final VcfFileMeta TWO_SAMPLE_ONE_CALLER_META = new VcfFileMeta(CCMModeTest.createTwoSampleVcf());
    public static final VcfFileMeta SINGLE_SAMPLE_TWO_CALLER_META = new VcfFileMeta(CCMModeTest.createSingleSampleTwoCallerVcf());

    @Test
    public void testThresholds() {
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 9));
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{10, 9}, 9));
        assertFalse(ConfidenceMode.allValuesAboveThreshold(new int[]{10, 9, 8}, 9));
        assertFalse(ConfidenceMode.allValuesAboveThreshold(new int[]{8, 9, 8}, 9));
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{8, 9, 8}, 8));
    }

    @Test //gt not contain 1, should return 0
    public void testEndsOfReadsWithGtNotContainOne() {

        Map<String, int[]> oabsMap = Map.of("A", new int[]{10, 10}, "C", new int[]{0, 5});
        String[] alts = {"A"};
        String gt = "0/0";

        int result = ConfidenceMode.endsOfReads(alts, gt, oabsMap, "A10[40]10[40];C10[40]0[0]");

        assertEquals(0, result);
    }

    @Test //eor is empty, should return 0
    public void testEndsOfReadsWithEmptyEor() {
        String[] alts = {"A"};
        String gt = "0/1";
        String eor = "";

        int result = ConfidenceMode.endsOfReads(alts, gt, Map.of("A", new int[]{10, 10}, "C", new int[]{0, 5}), eor);

        assertEquals(0, result);
    }

    @Test //alt not found in oabsMap, should return 0
    public void testEndsOfReadsWithAltNotFound() {
        String[] alts = {"A"};
        String gt = "0/1";

        int result = ConfidenceMode.endsOfReads(alts, gt, Map.of("A", new int[]{10, 10}, "C", new int[]{0, 5}), "C10[40]10[40];G10[40]0[0]");

        assertEquals(0, result);
    }

    @Test //middleOfReadCount is less than 5, should return max endOfReadCount
    public void testEndsOfReadsWithMiddleOfReadCountLessThanFive() {
        String[] alts = {"A"};
        String gt = "1/1";

        int result = ConfidenceMode.endsOfReads(alts, gt, Map.of("alt1", new int[]{10, 10}, "alt2", new int[]{0, 5}), "A10[40]10[40];C10[40]0[0]");

        // Assuming max endOfReadCount will be 20
        assertEquals(20, result);
    }

    @Test
    public void testCheckNNS() {

        // Test 1: All NNS above threshold
        StringBuilder sb1 = new StringBuilder();
        ConfidenceMode.checkNNS("3,5", sb1, 1);
        assertEquals("", sb1.toString());

        // Test 2: Some NNS not above threshold
        StringBuilder sb2 = new StringBuilder();
        ConfidenceMode.checkNNS("2,1", sb2, 3);
        assertEquals(VcfHeaderUtils.FILTER_NOVEL_STARTS, sb2.toString());

        // Test 3: All NNS equal to threshold
        StringBuilder sb3 = new StringBuilder();
        ConfidenceMode.checkNNS("2,2", sb3, 2);
        assertEquals("", sb3.toString());

        // Test 4: Null or empty string
        StringBuilder sb4 = new StringBuilder();
        ConfidenceMode.checkNNS("", sb4, 1);
        assertEquals(VcfHeaderUtils.FILTER_NOVEL_STARTS, sb4.toString());

        // Test 5: Some NNS are equals to threshold and some below threshold
        StringBuilder sb5 = new StringBuilder();
        ConfidenceMode.checkNNS("4,2", sb5, 3);
        assertEquals(VcfHeaderUtils.FILTER_NOVEL_STARTS, sb5.toString());
    }

    @Test
    public void testCheckStrandBiasWhenBothStrandsRepresented() {

        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("A", new int[]{10, 10});

        int[] gts = new int[]{1};
        StringBuilder failedFilterStringBuilder = new StringBuilder();
        int sBiasCovPercentage = 5;
        int sBiasAltPercentage = 5;

        ConfidenceMode.checkStrandBias(
                new String[]{"A"}, failedFilterStringBuilder, alleleDist, gts,
                sBiasCovPercentage, sBiasAltPercentage
        );

        assertEquals("", failedFilterStringBuilder.toString());

        alleleDist.put("A", new int[]{10, 0});
        failedFilterStringBuilder = new StringBuilder();
        ConfidenceMode.checkStrandBias(
                new String[]{"A"}, failedFilterStringBuilder, alleleDist, gts,
                sBiasCovPercentage, sBiasAltPercentage
        );
        assertEquals("SBIASCOV", failedFilterStringBuilder.toString());

        alleleDist.put("A", new int[]{0, 10});
        failedFilterStringBuilder = new StringBuilder();
        ConfidenceMode.checkStrandBias(
                new String[]{"A"}, failedFilterStringBuilder, alleleDist, gts,
                sBiasCovPercentage, sBiasAltPercentage
        );
        assertEquals("SBIASCOV", failedFilterStringBuilder.toString());
    }

    @Test
    public void testThresholdsPercentage() {
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 9f));
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 10f));
        assertFalse(ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 10.1f));
        assertTrue(ConfidenceMode.allValuesAboveThreshold(new int[]{10, 11, 23}, 100, 10.0f));
        assertFalse(ConfidenceMode.allValuesAboveThreshold(new int[]{10, 11, 23}, 100, 20.1f));
    }

    @Test
    public void getFieldofInts() {
        assertArrayEquals(new int[]{0}, ConfidenceMode.getFieldOfInts(null));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getFieldOfInts(""));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getFieldOfInts("."));
        assertArrayEquals(new int[]{1}, ConfidenceMode.getFieldOfInts("1"));
        assertArrayEquals(new int[]{10}, ConfidenceMode.getFieldOfInts("10"));
        assertArrayEquals(new int[]{10}, ConfidenceMode.getFieldOfInts("010"));
        assertArrayEquals(new int[]{10, 0}, ConfidenceMode.getFieldOfInts("010,0"));
        assertArrayEquals(new int[]{10, 1}, ConfidenceMode.getFieldOfInts("010,001"));
    }

    @Test
    public void getADs() {
        assertArrayEquals(new int[]{0}, ConfidenceMode.getAltCoveragesFromADField(null));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getAltCoveragesFromADField(""));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getAltCoveragesFromADField("."));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getAltCoveragesFromADField("A"));
        assertArrayEquals(new int[]{0}, ConfidenceMode.getAltCoveragesFromADField("1234"));
        assertArrayEquals(new int[]{2}, ConfidenceMode.getAltCoveragesFromADField("1,2"));
        assertArrayEquals(new int[]{20}, ConfidenceMode.getAltCoveragesFromADField("1,20"));
        assertArrayEquals(new int[]{20}, ConfidenceMode.getAltCoveragesFromADField("XYZ,20"));
        assertArrayEquals(new int[]{20, 1}, ConfidenceMode.getAltCoveragesFromADField("XYZ,20,1"));
        assertArrayEquals(new int[]{20, 1, 3}, ConfidenceMode.getAltCoveragesFromADField("XYZ,20,1,3"));
        assertArrayEquals(new int[]{20, 1, 3, 5}, ConfidenceMode.getAltCoveragesFromADField("XYZ,20,1,3,5"));
        assertArrayEquals(new int[]{20, 1, 3, 5}, ConfidenceMode.getAltCoveragesFromADField("100,20,1,3,5"));
    }

    @Test
    public void checkMin() {
        StringBuilder sb = null;
        ConfidenceMode.checkMIN(null, -1, null, sb, -1, -1f);
        assertNull(sb);
        sb = new StringBuilder();
        ConfidenceMode.checkMIN(null, -1, null, sb, -1, -1f);
        assertEquals("", sb.toString());
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("A", new int[]{10, 10});
        ConfidenceMode.checkMIN(new String[]{"A"}, 20, alleleDist, sb, 1, 5f);
        assertEquals("MIN", sb.toString());

        sb = new StringBuilder();
        ConfidenceMode.checkMIN(new String[]{"A"}, 2000, alleleDist, sb, Integer.MAX_VALUE, 5f);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIN(new String[]{"A"}, 2000, alleleDist, sb, 21, 5f);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIN(new String[]{"A"}, 2000, alleleDist, sb, 20, 5f);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIN(new String[]{"A"}, 200, alleleDist, sb, 2, 3f);
        assertEquals("MIN", sb.toString());

        sb = new StringBuilder();
        ConfidenceMode.checkMIN(new String[]{"C"}, 2000, alleleDist, sb, 20, 5f);
        assertEquals("", sb.toString());
    }

    @Test
    public void minForCompoundSnps() {

        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA10[40]10[40]");
        ConfidenceMode.checkMIN(new String[]{"AA"}, 20, alleleDist, sb, 1, 5f);
        assertEquals("MIN", sb.toString());

        sb = new StringBuilder();
        ConfidenceMode.checkMIN(new String[]{"AC"}, 20, alleleDist, sb, 1, 5f);
        assertEquals("", sb.toString());

        sb = new StringBuilder();
        alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA10[40]10[40];AC1[40]0[0]");
        ConfidenceMode.checkMIN(new String[]{"AC"}, 21, alleleDist, sb, 1, 5f);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIN(new String[]{"AC"}, 21, alleleDist, sb, 2, 3f);
        assertEquals("", sb.toString());
        alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA10[40]10[40];AC1[40]1[10]");
        ConfidenceMode.checkMIN(new String[]{"AC"}, 21, alleleDist, sb, 2, 3f);
        assertEquals("MIN", sb.toString());

        sb = new StringBuilder();
        alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA10[40]10[40];AC1[40]0[0]");
        ConfidenceMode.checkMIN(new String[]{"AC"}, 21, alleleDist, sb, 2, 5f);
        assertEquals("", sb.toString());

        sb = new StringBuilder();
        alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA10[40]10[40];AC1[40]10[40]");
        ConfidenceMode.checkMIN(new String[]{"AC"}, 32, alleleDist, sb, 2, 5f);
        assertEquals("MIN", sb.toString());

        sb = new StringBuilder();
        alleleDist = VcfUtils.getAllelicCoverageWithStrand("AA100[40]10[40];AC1[40]10[40]");
        ConfidenceMode.checkMIN(new String[]{"AC"}, 122, alleleDist, sb, 2, 10f);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIN(new String[]{"AA"}, 122, alleleDist, sb, 2, 10f);
        assertEquals("MIN", sb.toString());

    }

    @Test
    public void checkMIUN() {
        StringBuilder sb = null;
        ConfidenceMode.checkMIUN(null, 0, null, sb, -1, 3, null);
        assertNull(sb);
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 0, "", sb, -1, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A"}, 0, "C1", sb, 1, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A"}, 0, "C1;G2;T3", sb, 1, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A", "C"}, 0, "C1;G2;T3", sb, 2, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A", "C", "G"}, 93, "C1;G2;T3", sb, 2, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A", "C", "G", "T"}, 93, "C1;G2;T3", sb, 2, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A", "C", "G"}, 94, "C1;G2;T3", sb, 2, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"A", "C", "T"}, 0, "C1;G2;T3", sb, 3, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A", "C", "T"}, 100, "C1;G2;T3", sb, 3, 3, null);
        assertEquals("", sb.toString());
        ConfidenceMode.checkMIUN(new String[]{"C"}, 0, "C1;G2;T3", sb, 1, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"G"}, 0, "C1;G2;T3", sb, 2, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"T"}, 0, "C1;G2;T3", sb, 3, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"T"}, 0, "C1;G2;T3", sb, 4, 3, null);
        assertEquals("", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 0, "A3;C8", sb, 2, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A", "C"}, 0, "A3;C8", sb, 2, 3, null);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 90, "A3;C8", sb, 2, 3, null);
        assertEquals("", sb.toString());

        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"AA"}, 90, "AA13;AC8", sb, 2, 3, null);
        assertEquals("MIUN", sb.toString());

//
    }

    @Test
    public void testMIUNWithHelpFromUnfiltered() {
           /*
        add some tests with alt in the alleleDist
         */
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 10, "A1", sb, 2, 3, alleleDist);
        assertEquals("", sb.toString());

        alleleDist.put("A", new int[]{1, 0});
        ConfidenceMode.checkMIUN(new String[]{"A"}, 10, "A1", sb, 2, 3, alleleDist);
        assertEquals("MIUN", sb.toString());

        alleleDist.put("A", new int[]{0, 1});
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 10, "A1", sb, 2, 3, alleleDist);
        assertEquals("MIUN", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 10, "C1", sb, 2, 3, alleleDist);
        assertEquals("", sb.toString());
        
    }

    @Test
    public void testCheckMIUN_AllAllelesPassCutoff() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("G", new int[]{2, 2}); // Sum 4
        alleleDist.put("T", new int[]{1, 1}); // Sum 2
        ConfidenceMode.checkMIUN(new String[]{"G", "T"}, 30, "G4;T4", sb, 5, 10, alleleDist);
        assertTrue(sb.toString().contains("MIUN")); // Both "G" and "T" pass the cutoff, MIUN should be added
    }

    @Test
    public void testCheckMIUN_AltsNotInFailedFilter() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("A", new int[]{1, 2});

        ConfidenceMode.checkMIUN(new String[]{"C"}, 10, "T:3;G:5;", sb, 2, 10, alleleDist);

        assertEquals(0, sb.length()); // No matching alternative allele in `failedFilter`, no mutation added
    }


    @Test
    public void testCheckMIUN_EmptyFailedFilterString() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("G", new int[]{0, 0});

        ConfidenceMode.checkMIUN(new String[]{"G"}, 20, "", sb, 5, 10, alleleDist);

        assertEquals(0, sb.length()); // Empty `failedFilter`, no mutation should be added
    }

    @Test
    public void testCheckMIUN_AltExceedsCutoff() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("A", new int[]{3, 3}); // Sum = 6

        ConfidenceMode.checkMIUN(new String[]{"A"}, 20, "A5", sb, 5, 10, alleleDist);

        assertTrue(sb.toString().contains("MIUN")); // Alt exceeds the calculated cutoff, "MIUN" should be added
    }


    @Test
    public void testCheckMIUN_PercentageCutoff() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("A", new int[]{0, 0}); // Sum = 0

        // Total Coverage = 50 + 32 = 82
        // Percentage-based cutoff = 30% = 31.5 -> 32
        ConfidenceMode.checkMIUN(new String[]{"A"}, 50, "A32", sb, 10, 30, alleleDist);

        assertTrue(sb.toString().contains("MIUN")); // Failed filter count meets the percentage cutoff
    }
    @Test
    public void testCheckMIUN_EmptyAlts() {
        StringBuilder sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{}, 10, "T:3;G:5;", sb, 5, 10, new HashMap<>());
        assertEquals(0, sb.length()); // No update should occur with empty `alts` array
    }


    @Test
    public void testCheckMIUN_NullFailedFilter() {
        StringBuilder sb = new StringBuilder();
        ConfidenceMode.checkMIUN(new String[]{"A"}, 10, null, sb, 2, 10, new HashMap<>());
        assertEquals(0, sb.length()); // Null `failedFilter` should result in no modification
    }


    @Test
    public void testCheckMIUN_HandlesMultipleAlts() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("T", new int[]{1, 1}); // Sum = 2
        alleleDist.put("A", new int[]{4, 2}); // Sum = 6

        // Total Coverage = 50 + 10 = 60
        // Hard cutoff = max(5, (10% of 60)) = max(5, 6) = 6
        ConfidenceMode.checkMIUN(new String[]{"T", "A"}, 50, "T3;A4", sb, 5, 10, alleleDist);

        assertTrue(sb.toString().contains("MIUN")); // "A" exceeds the cutoff, so MIUN is added
    }



    @Test
    public void testCheckMIUN_HardCutoff() {
        StringBuilder sb = new StringBuilder();
        Map<String, int[]> alleleDist = new HashMap<>();
        alleleDist.put("T", new int[]{1, 1}); // Sum = 2

        // Total Coverage = 50 + 6 = 56
        // Hard cutoff = max(10, (20% of 56)) = max(10, 11.2) = 11
        ConfidenceMode.checkMIUN(new String[]{"T"}, 50, "T9", sb, 10, 20, alleleDist);

        assertEquals(0, sb.length()); // Failed filter count + alleleDist below the hard cutoff
    }


    @Test
    public void testHOM() {
        StringBuilder sb = null;
        ConfidenceMode.checkHOM(sb, -1, -1);
        assertNull(sb);
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, -1, -1);
        assertEquals("", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, 1, 0);
        assertEquals("HOM", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, 1, 1);
        assertEquals("HOM", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, 1, 2);
        assertEquals("", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, 2, 2);
        assertEquals("HOM", sb.toString());
        sb = new StringBuilder();
        ConfidenceMode.checkHOM(sb, 3, 2);
        assertEquals("HOM", sb.toString());
    }

    @Test
    public void endOfReads() {
        /*
         * chr2    31044381        .       C       A       .       .       FLANK=CACCCAACAAC;BaseQRankSum=-1.769;ClippingRankSum=0.078;DP=38;FS=0.000;MQ=59.43;MQRankSum=-0.078;QD=9.20;ReadPosRankSum=-0.204;SOR=0.315;IN=1,2;HOM=2,CCCCCCACCCaACAACAGTCC GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL   0/0:28,0:Reference:13:28:C2[]2[]:C4:PASS:.:.:.:C4[34.25]24[36.67]:.     0/1:28,12:Somatic:13:40:A1[]1[];C1[]2[]:A6;C4:PASS:.:SOMATIC:12:A1[32]11[33.55];C3[31.33]25[35.04]:.    ./.:.:.:3:.:.:.:PASS:.:NCIG:.:.:.       0/1:23,14:.:3:37:.:.:PASS:99:SOMATIC:.:.:349.77
         */
        VcfRecord r = new VcfRecord(new String[]{"chr2", "31044381", ".", "C", "A", ".", ".", "FLANK=CACCCAACAAC;BaseQRankSum=-1.769;ClippingRankSum=0.078;DP=38;FS=0.000;MQ=59.43;MQRankSum=-0.078;QD=9.20;ReadPosRankSum=-0.204;SOR=0.315;IN=1,2;HOM=2,CCCCCCACCCaACAACAGTCC;"
                , "GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL"
                , "0/0:28,0:Reference:13:28:C2[]2[]:C4:.:.:.:.:C4[34.25]24[36.67]:."
                , "0/1:28,12:Somatic:13:40:A1[]1[];C1[]2[]:A6;C4:.:.:SOMATIC:12:A1[32]11[33.55];C3[31.33]25[35.04]:."
                , "./.:.:.:3:.:.:.:.:.:NCIG:.:.:."
                , "0/1:23,14:.:3:37:.:.:.:99:SOMATIC:.:.:349.77"});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("5BP=2", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIUN() {
        /*
         * chr1	792590	.	C	A	.	.	FLANK=AATTTATTCCC;BaseQRankSum=-1.455;ClippingRankSum=0.000;DP=73;ExcessHet=3.0103;FS=1.318;MQ=55.96;MQRankSum=-6.174;QD=1.23;ReadPosRankSum=1.813;SOR=0.953;IN=1,2;GERM=A:9:0:9:0;HOM=0,TTGATAATTTaTTCCCATTCT;EFF=downstream_gene_variant(MODIFIER||4458|||LINC01128|retained_intron|NON_CODING|ENST00000425657||1),downstream_gene_variant(MODIFIER||4444|||LINC01128|lincRNA|NON_CODING|ENST00000416570||1),downstream_gene_variant(MODIFIER||4444|||LINC01128|lincRNA|NON_CODING|ENST00000448975||1),downstream_gene_variant(MODIFIER||3584|||LINC01128|lincRNA|NON_CODING|ENST00000449005||1),non_coding_exon_variant(MODIFIER|||n.4370C>A||LINC01128|lincRNA|NON_CODING|ENST00000445118|5|1)	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/0:46,1:47:.:A3;C8:PASS:.:.:.:A1[37]0[0];C26[39.62]20[40.15]:.	0/1:61,7:68:C1[]2[]:A9;C3:PASS:.:SOMATIC:6:A6[37.83]1[41];C40[40.17]21[37.86]:.	./.:.:.:.:.:PASS:.:NCIG:.:.:.	0/1:62,11:73:.:.:PASS:99:SOMATIC:.:.:89.77
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "792590", ".", "C", "A", ".", ".", "FLANK=AATTTATTCCC;BaseQRankSum=-1.455;ClippingRankSum=0.000;DP=73;ExcessHet=3.0103;FS=1.318;MQ=55.96;MQRankSum=-6.174;QD=1.23;ReadPosRankSum=1.813;SOR=0.953;IN=1,2;GERM=A:9:0:9:0;HOM=0,TTGATAATTTaTTCCCATTCT",
                "GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:46,1:47:.:A3;C8:.:.:.:.:A1[37]0[0];C26[39.62]20[40.15]:.",
                "0/1:61,7:68:C1[]2[]:A9;C3:.:.:SOMATIC:6:A6[37.83]1[41];C40[40.17]21[37.86]:.",
                "./.:.:.:.:.:.:.:NCIG:.:.:.",
                "0/1:62,11:73:.:.:.:99:SOMATIC:.:.:89.77"});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("MIUN", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void firstCallerOnly() {
        /*
         * chr1    5323163 rs7525806       C       T       .       .       BaseQRankSum=-0.967;ClippingRankSum=0.000;DP=3;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=11.92;ReadPosRankSum=-0.431;SOR=1.179;IN=2;DB;VLD;VAF=0.2567;GERM=T:8:157:165:0;HOM=3,TGGCACTATGcTTTGCATAGA;EFF=intergenic_region(MODIFIER||||||||||1)     GT:AD:CCC:CCM:DP:FT:GQ:INF:QL   0/1:1,2:Germline:21:3:COV;MR:37:.:35.77 ./.:.:HomozygousLoss:21:.:PASS:.:NCIG:.	./.:.:.:1:.:COV:.:.:.   ./.:.:.:1:.:COV:.:.:.
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "5323163", "rs7525806", "C", "T", ".", ".", "BaseQRankSum=-0.967;ClippingRankSum=0.000;DP=3;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=11.92;ReadPosRankSum=-0.431;SOR=1.179;IN=2;DB;VLD;VAF=0.2567;GERM=T:8:157:165:0;HOM=3,TGGCACTATGcTTTGCATAGA;EFF=intergenic_region(MODIFIER||||||||||1)", "GT:AD:CCC:CCM:DP:FT:GQ:INF:QL", "0/1:1,2:Germline:21:3:.:37:.:35.77", "./.:.:HomozygousLoss:21:.:.:.:NCIG:.", "./.:.:.:1:.:.:.:.:.", "./.:.:.:1:.:.:.:.:."});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void secondCallerOnly() {
        /*
         * chr1    5323163 rs7525806       C       T       .       .       BaseQRankSum=-0.967;ClippingRankSum=0.000;DP=3;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=11.92;ReadPosRankSum=-0.431;SOR=1.179;IN=2;DB;VLD;VAF=0.2567;GERM=T:8:157:165:0;HOM=3,TGGCACTATGcTTTGCATAGA;EFF=intergenic_region(MODIFIER||||||||||1)     GT:AD:CCC:CCM:DP:FT:GQ:INF:QL   ./.:.:.:1:.:COV:.:.:.   ./.:.:.:1:.:COV:.:.:.   0/1:1,2:Germline:21:3:COV;MR:37:.:35.77 ./.:.:HomozygousLoss:21:.:PASS:.:NCIG:.
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "5323163", "rs7525806", "C", "T", ".", ".", "BaseQRankSum=-0.967;ClippingRankSum=0.000;DP=3;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=11.92;ReadPosRankSum=-0.431;SOR=1.179;IN=2;DB;VLD;VAF=0.2567;GERM=T:8:157:165:0;HOM=3,TGGCACTATGcTTTGCATAGA;EFF=intergenic_region(MODIFIER||||||||||1)", "GT:AD:CCC:CCM:DP:FT:GQ:INF:QL", "./.:.:.:1:.:.:.:.:.", "./.:.:.:1:.:.:.:.:.", "0/1:1,2:Germline:21:3:.:37:.:35.77", "./.:.:HomozygousLoss:21:.:.:.:NCIG:."});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("COV", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void compoundSnp() {
        /*
         * chr1    14221527        .       TG      CA      .       .       IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)    GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS        1/1:0,22:Germline:34:39:PASS:.:21:CA12[]10[];CG8[]9[];C_2[]0[]  1/1:0,6:Germline:34:50:PASS:.:6:CA3[]3[];CG23[]21[];_G1[]1[]    ./.:.:.:1:.:COV:.:.:.   ./.:.:.:1:.:COV:.:.:.
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "1/1:0,22:Germline:34:39:.:.:21:CA12[]10[];CG8[]9[];C_2[]0[]",
                "1/1:0,6:Germline:34:50:.:.:6:CA3[]3[];CG23[]21[];_G1[]1[]",
                "./.:.:.:1:.:.:.:.:.",
                "./.:.:.:1:.:.:.:.:."});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void compoundSnp2() {
        /*
         * chr1    14221527        .       TG      CA      .       .       IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)
         * GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS
         * 1/1:0,22:Germline:34:39:PASS:.:21:CA12[]10[];CG8[]9[];C_2[]0[]
         * 1/1:0,6:Germline:34:50:PASS:.:6:CA3[]3[];CG23[]21[];_G1[]1[]
         * ./.:.:.:1:.:COV:.:.:.   ./.:.:.:1:.:COV:.:.:.
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "./.:.:.:1:.:.:.:.:.",
                "1/1:0,6:Germline:34:50:.:SOMATIC:6:CA3[]3[];CG23[]21[];_G1[]1[]",
                "./.:.:.:1:.:.:.:.:.",
                "./.:.:.:1:.:.:.:.:."});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("COV", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "0/0:3:.:1:.:.:.:.:TG1[]2[]",
                "1/1:0,6:Germline:34:50:.:SOMATIC:6:CA3[]3[];CG23[]21[];_G1[]1[]",
                "./.:.:.:1:.:.:.:.:.",
                "./.:.:.:1:.:.:.:.:."});
        cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("COV", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "1/1:0,16:Germline:34:50:.:.:16:CA13[]3[];CG23[]21[];_G1[]1[]",
                "./.:.:.:1:.:.:.:.:."});
        cm = new ConfidenceMode(SINGLE_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));

        r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "1/1:0,16:Germline:34:50:.:.:16:CA13[]3[];CG23[]21[];_G1[]1[]",
                "./.:.:.:1:.:.:.:.:."});
        cm = new ConfidenceMode(TWO_SAMPLE_ONE_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));

        r = new VcfRecord(new String[]{"chr1", "14221527", ".", "TG", "CA", ".", ".", "IN=1;HOM=2,TGTAAAACGGtgCTACTAGGCA;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:FT:INF:NNS:OABS",
                "./.:.:.:1:.:.:.:.:.",
                "1/1:0,6:Germline:34:50:.:SOMATIC:6:CA3[]3[];CG23[]21[];_G1[]1[]"});
        cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("COV", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIN() {
        /*
         * chr1	14248	.	T	G	.	.	FLANK=CTGACGGCCCT;BaseQRankSum=-0.422;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=1.906;MQ=26.71;MQRankSum=1.263;QD=2.31;ReadPosRankSum=0.769;SOR=1.282;IN=1,2;GERM=G:205:9:214:0;HOM=2,CCCTGCTGACgGCCCTTCTCT	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/0:24,1:25:T1[]0[]:G8;T42:PASS:.:.:.:G1[41]0[0];T14[31.36]10[38.1]:.	0/1:20,6:26:T2[]1[]:G11;T94:PASS:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.	./.:.:.:.:.:PASS:.:NCIG:.:.:.	0/1:19,6:25:.:.:PASS:86:SOMATIC:.:.:57.77
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "14248", ".", "T", "G", ".", ".", "FLANK=CTGACGGCCCT;BaseQRankSum=-0.422;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=1.906;MQ=26.71;MQRankSum=1.263;QD=2.31;ReadPosRankSum=0.769;SOR=1.282;IN=1,2;GERM=G:205:9:214:0;HOM=2,CCCTGCTGACgGCCCTTCTCT",
                "GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:24,2:26:T1[]0[]:T42:.:.:.:.:G1[41]1[10];T14[31.36]10[38.1]:.",
                "0/1:20,6:26:T2[]1[]:G11;T94:.:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.",
                "./.:.:.:.:.:.:.:NCIG:.:.:.",
                "0/1:19,6:25:.:.:.:86:SOMATIC:.:.:57.77"});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("MIN", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        /*
         * reduce alt coverage in control to below 3% - should change to PASS
         */
        r.setFormatFields(java.util.Arrays.asList("GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:34,1:35:T1[]0[]:T42:.:.:.:.:G1[41]0[0];T14[31.36]20[38.1]:.",
                "0/1:20,6:26:T2[]1[]:G11;T94:.:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.",
                "./.:.:.:.:.:.:.:NCIG:.:.:.",
                "0/1:19,6:25:.:.:.:86:SOMATIC:.:.:57.77"));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIN2() {
        /*
         * chr1	14248	.	T	G	.	.	FLANK=CTGACGGCCCT;BaseQRankSum=-0.422;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=1.906;MQ=26.71;MQRankSum=1.263;QD=2.31;ReadPosRankSum=0.769;SOR=1.282;IN=1,2;GERM=G:205:9:214:0;HOM=2,CCCTGCTGACgGCCCTTCTCT	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/0:24,1:25:T1[]0[]:G8;T42:PASS:.:.:.:G1[41]0[0];T14[31.36]10[38.1]:.	0/1:20,6:26:T2[]1[]:G11;T94:PASS:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.	./.:.:.:.:.:PASS:.:NCIG:.:.:.	0/1:19,6:25:.:.:PASS:86:SOMATIC:.:.:57.77
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "14248", ".", "T", "G", ".", ".", "FLANK=CTGACGGCCCT;BaseQRankSum=-0.422;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=1.906;MQ=26.71;MQRankSum=1.263;QD=2.31;ReadPosRankSum=0.769;SOR=1.282;IN=1,2;GERM=G:205:9:214:0;HOM=2,CCCTGCTGACgGCCCTTCTCT",
                "GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:64,3:67:T1[]0[]:T42:.:.:.:.:G1[41]2[20];T14[31.36]50[38.1]:.",
                "0/1:20,6:26:T2[]1[]:G11;T94:.:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.",
                "./.:.:.:.:.:.:.:NCIG:.:.:.",
                "0/1:19,6:25:.:.:.:86:SOMATIC:.:.:57.77"});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("MIN", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        /*
         * reduce alt coverage in control to below 3% - should change to PASS
         */
        r.setFormatFields(java.util.Arrays.asList(
                "GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:64,2:67:T1[]0[]:T42:.:.:.:.:G1[41]1[20];T14[31.36]50[38.1]:.",
                "0/1:20,6:26:T2[]1[]:G11;T94:.:.:SOMATIC:6:G4[35.25]2[26.5];T11[39.09]9[37.56]:.",
                "./.:.:.:.:.:.:.:NCIG:.:.:.",
                "0/1:19,6:25:.:.:.:86:SOMATIC:.:.:57.77"));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIN3() {
        VcfRecord r = new VcfRecord(new String[]{"chr1", "577154", ".", "A", "T", ".", ".", "FLANK=GGGCATTTTCT;BaseQRankSum=-1.290;ClippingRankSum=0.000;DP=12;ExcessHet=3.0103;FS=0.000;MQ=25.90;MQRankSum=-0.765;QD=7.15;ReadPosRankSum=-0.713;SOR=0.883;IN=1,2;HOM=3,TGATGGGGCAaTTTCTGAAAA;EFF=intron_variant(MODIFIER|||n.170-33718T>A||RP5-857K21.4|lincRNA|NON_CODING|ENST00000440200|1|1)",
                "GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:3,2:Reference:13:5:T1[]1[]:A25;T10:.:.:.:.:A1[41]2[41];T1[37]1[37]:.",
                "0/1:7,6:Somatic:13:13:T1[]2[]:A50;G1;T18:.:.:SOMATIC:5:A4[32.75]3[41];T3[38.33]3[36.67]:.",
                "./.:.:.:3:.:.:.:.:.:NCIG:.:.:.",
                "0/1:7,5:.:3:12:.:.:.:99:SOMATIC:.:.:85.77"});

        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("COV;MIN", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("5BP=3", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIN4() {
        VcfRecord r = new VcfRecord(new String[]{"chr1", "67978295", "rs185498976", "A", "G", ".", ".", "FLANK=TTACAGCCTCT;BaseQRankSum=0.391;ClippingRankSum=0.000;DP=59;ExcessHet=3.0103;FS=7.068;MQ=60.00;MQRankSum=0.000;QD=13.40;ReadPosRankSum=-0.873;SOR=0.757;IN=1,2;DB;VAF=0.005051;HOM=2,GCTCATTACAaCCTCTGCCTC;EFF=intergenic_region(MODIFIER||||||||||1)",
                "GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:29,1:Reference:13:30:A3[]0[]:A1:.:.:.:.:A16[33.44]13[35.31];G1[12]0[0]:.",
                "0/1:32,26:Somatic:13:58:A3[]0[];G0[]3[]:A2;G2:.:.:SOMATIC:25:A20[34.8]12[37.67];G11[32]15[33]:.",
                "./.:.:.:3:.:.:.:.:.:NCIG:.:.:.",
                "0/1:31,28:.:3:59:.:.:.:99:SOMATIC:.:.:790.77"});

        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();

        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void newCoverageCutoffs() {
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 4985568, 4985568), "rs10753395", "A", "C");
        vcf.setInfo("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816");
        vcf.setFormatFields(java.util.Arrays.asList(
                "GT:AD:DP:FT:INF:MR:NNS",
                "0/1:3,5:8:.:.:5:5",
                "0/1:3,5:8:.:.:5:5",
                "0/1:3,5:8:.:.:5:5",
                "0/1:3,5:8:.:.:5:5"));

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();

        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));


        vcf.setFormatFields(java.util.Arrays.asList(
                "GT:AD:DP:FT:INF:MR:NNS",
                "0/1:3,5:8:.:.:5:5",
                "0/1:2,5:7:.:.:5:5",
                "0/1:3,5:8:.:.:5:5",
                "0/1:2,5:7:.:.:5:5"));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();

        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        vcf.setFormatFields(java.util.Arrays.asList(
                "GT:AD:DP:FT:INF:MR:NNS",
                "0/1:2,5:7:.:.:5:5",
                "0/1:2,5:7:.:.:5:5",
                "0/1:3,5:8:.:.:5:5",
                "0/1:3,5:8:.:.:5:5"));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();

        assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeMIUN2() {
        // chr1    16651203        rs79306135      G       A       .       .       FLANK=GTAAAACTGGA;BaseQRankSum=0.325;ClippingRankSum=0.000;DP=58;ExcessHet=3.0103;FS=4.683;MQ=55.10;MQRankSum=-6.669;QD=4.63;ReadPosRankSum=-0.352;SOR=1.425;IN=1,2;DB;VLD;HOM=3,TATATGTAAAgCTGGATTAAT;EFF=downstream_gene_variant(MODIFIER||914|||MST1P2|unprocessed_pseudogene|NON_CODING|ENST00000457982||1),intergenic_region(MODIFIER||||||||||1)  GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL   0/0:21,1:Reference:13:22:.:A1;C1:PASS:.:.:.:A0[0]1[41];G9[40.11]12[39.08]:.     0/1:43,14:Somatic:13:57:A0[]1[];G0[]1[]:A10;G2:PASS:.:SOMATIC:13:A6[40.33]8[37.12];G21[35.67]22[38.91]:.
        //        ./.:.:.:3:.:.:.:PASS:.:NCIG:.:.:.       0/1:45,12:.:3:57:.:.:PASS:99:SOMATIC:.:.:263.77
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1",   16651203, 16651203), "rs79306135", "G", "A");
        vcf.setInfo("FLANK=GTAAAACTGGA;BaseQRankSum=0.325;ClippingRankSum=0.000;DP=58;ExcessHet=3.0103;FS=4.683;MQ=55.10;MQRankSum=-6.669;QD=4.63;ReadPosRankSum=-0.352;SOR=1.425;IN=1,2;DB;VLD;HOM=3,TATATGTAAAgCTGGATTAAT;EFF=downstream_gene_variant(MODIFIER||914|||MST1P2|unprocessed_pseudogene|NON_CODING|ENST00000457982||1),intergenic_region(MODIFIER||||||||||1)");
        vcf.setFormatFields(java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL",
                "0/0:21,1:Reference:13:22:.:A1;C1:.:.:.:.:A0[0]1[41];G9[40.11]12[39.08]:.",
                "0/1:43,14:Somatic:13:57:A0[]1[];G0[]1[]:A10;G2:.:.:SOMATIC:13:A6[40.33]8[37.12];G21[35.67]22[38.91]:.",
                "./.:.:.:3:.:.:.:.:.:NCIG:.:.:.",
                "0/1:45,12:.:3:57:.:.:.:99:SOMATIC:.:.:263.77"));
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("MIUN", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

    }

    @Test
    public void compoundSnpMIUN() {
        /*
        chr1  205931  .       AA      GG      .       .       .       GT:AD:DP:FF:FT:INF:NNS:OABS     0/0:6,0:6:AA47;AT1;A_1;GG3:.:.:.:AA3[]3[]       0/1:20,4:25:AA182;GG2;TA1:.:SOMATIC:4:AA
7[]13[];GA1[]0[];GG0[]4[]
         */
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 205931, 205931), ".", "CA", "GG");
        vcf.setInfo("FLANK=GTAAAACTGGA;BaseQRankSum=0.325;ClippingRankSum=0.000;DP=58;ExcessHet=3.0103;FS=4.683;MQ=55.10;MQRankSum=-6.669;QD=4.63;ReadPosRankSum=-0.352;SOR=1.425;IN=1;DB;VLD;HOM=3,TATATGTAAAgCTGGATTAAT;EFF=downstream_gene_variant(MODIFIER||914|||MST1P2|unprocessed_pseudogene|NON_CODING|ENST00000457982||1),intergenic_region(MODIFIER||||||||||1)");
        vcf.setFormatFields(java.util.Arrays.asList(
                "GT:AD:DP:FF:FT:INF:NNS:OABS",
                "0/0:36,0:36:CA12;GG4;_A2:.:.:.:CA17[]19[];C_1[]0[]",
                "0/1:102,11:114:AA1;CA30;CC1;CT1;C_3;GG50;GT1:.:SOMATIC:10:AA1[]0[];CA61[]41[];GG4[]7[];G_1[]0[];_A0[]2[]"));
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_ONE_CALLER_META);
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("MIUN", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));

    }

    @Test
    public void realLifeFail3() {
        //GL000224.1	34563	.	C	T	.	.	FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=7,GCAATTCTTTtTTTAATGATC;EFF=upstream_gene_variant(MODIFIER||3648|||AL591856.4|miRNA|NON_CODING|ENST00000581903||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/0:.:Reference:13:240:PASS:.:.:.:.:C117[38.72]123[39.17]	0/1:.:Somatic:13:516:.:.:SOMATIC:64:60:C226[39.34]226[39.23];T33[40.76]31[40.16]	0/0:.:Reference:13:240:PASS:.:.:.:.:C117[38.72]123[39.17]	0/1:450,62:Somatic:13:512:.:99:SOMATIC:64:60:C226[39.34]226[39.23];T33[40.76]31[40.16]
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000224.1", 34563, 34563), ".", "C", "T");
        vcf.setInfo("FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=7,GCAATTCTTTtTTTAATGATC");
        List<String> ff = java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                "0/0:240:Reference:13:240:.:.:.:.:C117[38.72]123[39.17]	",
                "0/1:452,64:Somatic:13:516:.:.:SOMATIC:60:C226[39.34]226[39.23];T33[40.76]31[40.16]",
                "0/0:240:Reference:13:240:.:.:.:.:.",
                "0/1:450,62:Somatic:13:512:.:99:SOMATIC:.:.");
        vcf.setFormatFields(ff);
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("HOM", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("HOM", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

        /*
         * set HOM to 5, and we should be all PASSES
         */
        vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000224.1", 34563, 34563), ".", "C", "T");
        vcf.setInfo("FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=5,GCAATTCTTTtTTTAATGATC");
        ff = java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                "0/0:240:Reference:13:240:.:.:.:.:C117[38.72]123[39.17]	",
                "0/1:452,64:Somatic:13:516:.:.:SOMATIC:60:C226[39.34]226[39.23];T33[40.76]31[40.16]",
                "0/0:240:Reference:13:240:.:.:.:.:.",
                "0/1:450,62:Somatic:13:512:.:99:SOMATIC:.:.");
        vcf.setFormatFields(ff);
        cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeFail4() {
        //GL000247.1	152	.	A	G	.	.	FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:27:PASS:.:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:.:Germline:23:76:SBIASALT:.:.:29:25:A43[39.23]4[41];G28[39.39]1[41]	0/1:20,7:Germline:23:27:PASS:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:45,20:Germline:23:65:SBIASALT:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000247.1", 152, 152), ".", "A", "G");
        vcf.setInfo("FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG");
        List<String> ff = java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                "0/1:20,7:Germline:23:27:.:.:.:7:A20[38.1]0[0];G6[40.33]1[41]",
                "0/1:47,29:Germline:23:76:.:.:.:25:A43[39.23]4[41];G28[39.39]1[41]",
                "0/1:20,7:Germline:23:27:.:99:.:.:.",
                "0/1:45,20:Germline:23:65:.:99:.:.:.");
//		 "0/1:20,7:Germline:23:27:.:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]",
//		 "0/1:45,20:Germline:23:65:.:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]");
        vcf.setFormatFields(ff);
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeFail5() {
        //GL000247.1	152	.	A	G	.	.	FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:27:PASS:.:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:.:Germline:23:76:SBIASALT:.:.:29:25:A43[39.23]4[41];G28[39.39]1[41]	0/1:20,7:Germline:23:27:PASS:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:45,20:Germline:23:65:SBIASALT:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000247.1", 152, 152), ".", "A", "G");
        vcf.setInfo("FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG");
        List<String> ff = java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                "0/1:20,7:Germline:23:27:.:.:.:7:A20[38.1]0[0];G6[40.33]1[41]",
                "0/1:47,29:Germline:23:76:.:.:.:25:A43[39.23]4[41];G28[39.39]1[41]",
                "0/1:20,4:Germline:23:27:.:99:.:.:.",
                "0/1:45,20:Germline:23:65:.:99:.:.:.");
//		 "0/1:20,7:Germline:23:27:.:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]",
//		 "0/1:45,20:Germline:23:65:.:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]");
        vcf.setFormatFields(ff);
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("MR", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeFail() {
        //chr1    4985568 rs10753395      A       C       .       PASS_1;PASS_2   FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816   GT:GD:AC:MR:NNS:AD:DP:GQ:PL     0/1:A/C:A8[33.75],11[38.82],C3[42],5[40],A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18,8:26:99:274,0,686      1/1:C/C:A1[37],0[0],C23[38.96],19[41.21],A1[37],0[0],C24[38.88],23[40.26]:42,47:38,42:1,44:45:94:1826,94,0
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 4985568, 4985568), "rs10753395", "A", "C");
        vcf.setFilter("PASS_1;PASS_2");
        vcf.setInfo("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816");
        List<String> ff = java.util.Arrays.asList("GT:GD:AC:MR:NNS:AD:DP:GQ:PL", "0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]:8:8:18:26:99:274,0,686", "0/1:A/C:A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:8:26:99:274,0,686", "1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]:42:38:1,44:45:94:1826,94,0", "1/1:C/C:A1[37],0[0],C24[38.88],23[40.26]:47:42:1,44:45:94:1826,94,0");
        vcf.setFormatFields(ff);
        assertEquals(8, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(1), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
        assertEquals(8, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(2), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
        assertEquals(38, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(3), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
        assertEquals(42, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(4), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
    }

    @Test
    public void realLifeFail2() {
        //chr1	73551390	rs12142252	T	C	.	.	FLANK=ATCAACGGTCT;BaseQRankSum=-1.162;ClippingRankSum=1.214;DP=54;FS=0.000;MQ=60.00;MQRankSum=0.330;QD=17.29;ReadPosRankSum=0.590;SOR=0.596;IN=1,2;DB;VLD;VAF=0.3462;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:22:36:PASS:.:.:7:6:C3[41]4[38.75];T17[40.29]12[35.58]	0/0:.:LOH:22:63:PASS:.:.:.:.:C1[37]1[41];T31[39.03]30[38.87]	0/1:29,25:Germline:23:54:PASS:99:.:7:6:C3[41]4[38.75];T17[40.29]12[35.58]	0/1:61,8:Germline:23:69:.:99:.:2:2:C1[37]1[41];T31[39.03]30[38.87]
        VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 73551390, 73551390), "rs12142252", "T", "C");
        vcf.setInfo("FLANK=ATCAACGGTCT;BaseQRankSum=-1.162;ClippingRankSum=1.214;DP=54;FS=0.000;MQ=60.00;MQRankSum=0.330;QD=17.29;ReadPosRankSum=0.590;SOR=0.596;IN=1,2;DB;VLD;VAF=0.3462;EFF=intergenic_region(MODIFIER||||||||||1)");
        List<String> ff = java.util.Arrays.asList(
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                "0/1:29,7:Germline:22:36:.:.:.:6:C3[41]4[38.75];T17[40.29]12[35.58]",
                "0/0:61:LOH:22:63:.:.:.:.:C1[37]1[41];T31[39.03]30[38.87]",
                "0/1:29,25:Germline:23:54:.:99:.:.:.",
                "0/1:61,8:Germline:23:69:.:99:.:.:.");
        vcf.setFormatFields(ff);
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));    // fails on MR and NNS
    }

    @Test
    public void passHomozygosLoss() {
        /*
         * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
         */
        VcfRecord r = new VcfRecord(new String[]{"GL000225.1", "6859", ".", "T", "G", ".", ".", "FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)"
                , "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS"
                , "0/1:.:Germline:21:24:.:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]"
                , "./.:.:HomozygousLoss:21:14:.:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]"
                , "0/1:5,18:Germline:21:23:.:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]"
                , "./.:3,5:HomozygousLoss:21:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]"});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void noCallInGATK() {
        /*
         * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "696644", ".", "G", "A", ".", ".", "FLANK=AAACAAAAACT;BaseQRankSum=-0.677;ClippingRankSum=-0.710;DP=49;FS=0.000;MQ=25.07;MQRankSum=-0.215;QD=21.19;ReadPosRankSum=0.974;SOR=0.446;IN=1,2;HOM=5,AACTAAAACAaAAACTCCTGA"
                , "GT:AD:DP:FF:FT:GQ:INF:NNS:OABS:QL"
                , "0/0:39,0:39:G17:.:.:.:.:G16[40.25]23[37.22]:."
                , "0/1:5,43:48:.:.:.:SOMATIC:41:A19[39.95]24[37.83];G2[39]3[39.67]:."
                , "0/0:.:.:.:.:.:NCIG:.:.:."
                , "0/1:5,44:49:.:.:3:.:.:.:1038.53"});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void areCoverageChecksWorking() {
        /*
         * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
         */
        VcfRecord r = new VcfRecord(new String[]{"GL000225.1", "6859", ".", "T", "G", ".", ".", "FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)"
                , "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
                , "0/1:8,16:Germline:23:24:.:.:.:15:G5[38]11[38.09];T4[34.25]4[35.5]"
                , "0/1:7,7:Germline:23:14:.:.:.:7:G4[34.25]3[39];T4[38.25]3[33]"
                , "0/1:5,18:Germline:23:23:.:99:.:.:."
                , "0/1:3,5:Germline:23:8:.:99:.:.:."});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void areCoverageChecksWorking2() {
        /*
         * GL000231.1	21863	.	C	T	.	.	FLANK=AATCCTTTCAT;BaseQRankSum=-0.085;ClippingRankSum=-0.751;DP=90;FS=0.832;MQ=50.50;MQRankSum=-3.000;QD=11.64;ReadPosRankSum=0.564;SOR=0.554;IN=1,2;EFF=downstream_gene_variant(MODIFIER||15|||CT867977.1|miRNA|NON_CODING|ENST00000581649||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:56:PASS:.:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:.:Germline:23:38:.:.:.:4:3:C9[37.78]25[36.6];T3[40]1[41]	0/1:50,40:Germline:23:90:PASS:99:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:40,27:Germline:23:67:.:99:.:4:3:C9[37.78]25[36.6];T3[40]1[41]
         */
        VcfRecord r = new VcfRecord(new String[]{"GL000231.1", "21863", ".", "C", "T", ".", ".", "."
                , "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
                , "0/1:45,11:Germline:23:56:.:.:.:9:C16[37.38]29[37.34];T8[34.5]3[39.33]"
                , "0/1:34,4:Germline:23:38:.:.:.:3:C9[37.78]25[36.6];T3[40]1[41]"
                , "0/1:50,40:Germline:23:90:.:99:.:.:."
                , "0/1:40,27:Germline:23:67:.:99:.:.:."});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("NNS;MR", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void ifGermlineJustLookAtControl() {
        /*
         * GL000231.1	21863	.	C	T	.	.	FLANK=AATCCTTTCAT;BaseQRankSum=-0.085;ClippingRankSum=-0.751;DP=90;FS=0.832;MQ=50.50;MQRankSum=-3.000;QD=11.64;ReadPosRankSum=0.564;SOR=0.554;IN=1,2;EFF=downstream_gene_variant(MODIFIER||15|||CT867977.1|miRNA|NON_CODING|ENST00000581649||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:56:PASS:.:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:.:Germline:23:38:.:.:.:4:3:C9[37.78]25[36.6];T3[40]1[41]	0/1:50,40:Germline:23:90:PASS:99:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:40,27:Germline:23:67:.:99:.:4:3:C9[37.78]25[36.6];T3[40]1[41]
         */
        VcfRecord r = new VcfRecord(new String[]{"GL000231.1", "21863", ".", "C", "T", ".", ".", "."
                , "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
                , "0/1:45,11:Germline:23:56:.:.:.:9:C16[37.38]29[37.34];T8[34.5]3[39.33]"
                , "0/1:34,4:Germline:23:38:.:.:.:3:C9[37.78]25[36.6];T3[40]1[41]"
                , "0/1:50,40:Germline:23:90:.:99:.:.:."
                , "0/1:40,27:Germline:23:67:.:99:.:.:."});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("NNS;MR", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confIsSomatic() {
        /*
         * chr1    2245570 rs2843152       C       G       .       .       FLANK=GATGCGAGGAG;DP=4;FS=0.000;MQ=60.00;MQ0=0;QD=17.76;SOR=0.693;IN=1,2;DB;VLD;VAF=0.6276;EFF=downstream_gene_variant(MODIFIER||4012||728|SKI|protein_coding|CODING|ENST00000378536||1),intergenic_region(MODIFIER||||||||||1) GT:AD:DP:FT:GQ:INF:MR:NNS:OABS  .:.:.:.:.:.:.:.:.       1/1:.:4:5BP=1;COVT:.:SOMATIC;GERM=53,185;CONF=SOMATIC;GERM=53,185;ZERO:4:4:G2[35]2[35]  .:.:.:.:.:.:.:.:.       1/1:0,4:4:5BP=1;COVT:12:SOMATIC;GERM=53,185;CONF=SOMATIC;GERM=53,185;ZERO:4:4:G2[35]2[35]
         */
        VcfRecord r = new VcfRecord(new String[]{"chr1", "2245570", "rs2843152", "C", "G", ".", ".", "FLANK=GATGCGAGGAG;DP=4;FS=0.000;MQ=60.00;MQ0=0;QD=17.76;SOR=0.693;IN=1,2;DB;VLD;VAF=0.6276;EFF=downstream_gene_variant(MODIFIER||4012||728|SKI|protein_coding|CODING|ENST00000378536||1),intergenic_region(MODIFIER||||||||||1)"
                , "GT:AD:DP:FT:GQ:INF:MR:NNS:OABS"
                , ".:.:.:.:.:.:.:.:."
                , "1/1:0,4:4:5BP=1;COVT:.:SOMATIC;GERM=53,185:4:4:G2[35]2[35]"
                , ".:.:.:.:.:.:.:.:."
                , "1/1:0,4:4:5BP=1;COVT:12:SOMATIC;GERM=53,185:4:4:G2[35]2[35]"});

        assertTrue(VcfUtils.isRecordSomatic(r));
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(r.getChrPosition(), List.of(r));
        cm.addAnnotation();
        r = cm.positionRecordMap.get(r.getChrPosition()).getFirst();
        assertTrue(r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_INFO).contains("SOMATIC"));
        assertTrue(r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_INFO).contains("GERM=53,185"));
        assertEquals("COV;MR", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV;MR", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeSingle() {
        //chr8	12306635	rs28428895	C	T	.	PASS	FLANK=ACACATACATA;DB;CONF=HIGH;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)	GT:GD:AC:MR:NNS	0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8	0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1
        VcfRecord vcf1 = new VcfRecord(new String[]{"chr8", "12306635", "rs28428895", "C", "T", ".", ".", "FLANK=ACACATACATA;DB;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)", "GT:AC:AD:DP:NNS:FT", "0/1:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:13,8:22:8:.", "0/0:C19[36.11],20[38.45],T1[42],0[0]:39,1:40:1:."});
        VcfRecord vcf2 = new VcfRecord(new String[]{"chr8", "12306635", "rs28428895", "C", "T", "57.77", ".", "SOMATIC;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)", "GT:AD:DP:GQ:PL:GD:AC:MR:NNS:FT", "0/1:17,15,1:33:.:.:C/C:C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]:15:15:MIN", "0/1:4,3:7:86:86,0,133:C/T:C22[36.23],22[36.91],T2[26.5],1[42]:3:2:MR;NNS"});

        ConfidenceMode cm = new ConfidenceMode(SINGLE_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(vcf1.getChrPosition(), java.util.Arrays.asList(vcf1, vcf2));
        cm.addAnnotation();

        vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).getFirst();
        vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
        assertEquals("PASS", vcf1.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf1.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf2.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV;MR", vcf2.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void multipleADValues() {
        /*
         *chr1	63982013	rs12130694	C	G,T	.	.	FLANK=GGAAGGGGAGC;BaseQRankSum=1.437;ClippingRankSum=0.000;DP=21;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=29.74;ReadPosRankSum=-0.087;SOR=4.615;IN=1,2;DB;VAF=[.,0.359];GERM=T:32:48:80:0;HOM=4,GGAAGGGAAGgGGAGCGGGGG	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	2/2:1,0,21:22:T0[]1[]:G4;T6:MR:.:.:18:C1[12]0[0];T18[30.39]3[35]:.	1/2:0,3,8:12:T1[]0[]:A1;G9;T13:SBIASALT;NNS;MR:.:SOMATIC:3,7:A1[12]0[0];G3[12]0[0];T7[19.14]1[32]:.2/2:1,19:20:.:.:PASS:48:.:.:.:594.77	2/2:0,8:8:.:.:PASS:22:.:.:.:119.79
         */
        VcfRecord vcf = new VcfRecord(new String[]{"chr1	", "63982013", "rs12130694", "C", "G,T", ".", ".", "FLANK=GGAAGGGGAGC;BaseQRankSum=1.437;ClippingRankSum=0.000;DP=21;ExcessHet=3.0103;FS=0.000;MQ=60.00;MQRankSum=0.000;QD=29.74;ReadPosRankSum=-0.087;SOR=4.615;IN=1,2;DB;VAF=[.,0.359];GERM=T:32:48:80:0;HOM=4,GGAAGGGAAGgGGAGCGGGGG"
                , "GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL"
                , "2/2:1,0,21:22:T0[]1[]:G4;T6:MR:.:.:18:C1[12]0[0];T18[30.39]3[35]:."
                , "1/2:0,3,8:12:T1[]0[]:A1;G9;T13:SBIASALT;NNS;MR:.:SOMATIC:3,7:A1[12]0[0];G3[12]0[0];T7[19.14]1[32]:."
                , "2/2:1,19:20:.:.:PASS:48:.:.:.:594.77"
                , "2/2:0,8:8:.:.:PASS:22:.:.:.:119.79"});

        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();

        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("SBIASALT;NNS;MR", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged() {
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr8", "12306635", "rs28428895", "C", "T", ".", ".", "FLANK=ACACATACATA;IN=1,2;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)",
                "GT:OABS:NNS:AD:DP:GQ:PL:FT",
                "0/1:C10[39]3[30]G1[11]0[0];T7[41.29]1[42]:8:13,8:22:.:.:.",
                "0/1:C14[38.79]3[30];G1[11]0[0];T11[39.27]4[25.25]:15:17,15:33:.:.:.",
                "0/0:.:.:.:40:.:.:.",
                "0/1:.:.:4,3:47:86:86,0,133:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();
        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();

        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("MR", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLife2() {
        //chr9	126129715	rs57014689	C	A	205.77	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB	GT:AD:DP:GQ:PL:GD:AC:MR:NNS	0/1:6,5:11:99:234,0,331:A/C:A0[0],4[24.25],C243[17.06],65[18.88],G2[7],0[0]:4:4	1/1:1,18:19:46:841,46,0:A/A:A2[7],15[28.73],C179[15.92],121[14.76],G0[0],1[7]:17:16
        //chr9	126129715	rs57014689	C	A	.	PASS	SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185	GT:GD:AC:MR:NNS	0/0:C/C:A0[0],4[24.25],C128[17.64],30[20.9],G2[7],0[0]:4:4	0/1:A/C:A2[7],13[28.23],C96[17.22],54[14.22]:15:15
        VcfRecord vcf1 = new VcfRecord(new String[]{"chr9", "126129715", "rs57014689", "C", "A", "205.77", ".", "AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB",
                "GT:AD:DP:GQ:PL:OABS:NNS:FT",
                "0/1:6,5:11:99:234,0,331:A0[0]4[24.25];C243[17.06]65[18.88];G2[7]0[0]:.:.",
                "1/1:1,18:19:46:841,46,0:A2[7]15[28.73];C179[15.92]121[14.76];G0[0]1[7]:.:."});
        VcfRecord vcf2 = new VcfRecord(new String[]{"chr9", "126129715", "rs57014689", "C", "A", ".", ".", "SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185",
                "GT:AD:DP:OABS:NNS:FT",
                "0/0:158,4:164:A0[0]4[24.25];C128[17.64]30[20.9];G2[7]0[0]:4:.",
                "0/1:150,15:165:A2[7]13[28.23];C96[17.22]54[14.22]:15:."});

        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf1.getChrPosition(), Arrays.asList(vcf1, vcf2));
        cm.addAnnotation();

        vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).getFirst();
        vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
        assertEquals("PASS", vcf1.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf1.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf2.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf2.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged2() {
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr9", "126129715", "rs57014689", "C", "A", ".", ".", "FLANK=CCCCCACACCC;AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;IN=1,2;DB;GERM=5,185",
                "GT:OABS:NNS:AD:DP:GQ:PL:FT:INF",
                "0/0:A0[0]4[24.25];C128[17.64]30[20.9];G2[7]0[0]:4:.:164:.:.:.:.",
                "0/1:A0[0]4[24.25];C243[17.06]65[18.88];G2[7]0[0]:4:6,5:300:99:234,0,331:.:SOMATIC",
                "0/0:.:.:.:.:.:.:.:NCIG",
                "1/1:.:16:1,18:319:46:841,46,0:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged3() {
        /*
         * chr17	76354679       	.      	G      	A      	.      	.      	FLANK=TAGATATAATA;BaseQRankSum=-0.735;ClippingRankSum=-0.385;DP=23;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.175;QD=20.34;ReadPosRankSum=-0.245;SOR=1.061;IN=1,2     	GT:DP:FT:INF:MR:NNS:OABS:AD:GQ 	0/0:36:.:SOMATIC:.:.:A0[0]1[34];G19[34.79]16[35.25]:.:.	0/1:22:.:SOMATIC:16:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:.:. 	.:.:.:.:.:.:A0[0]1[34];G19[34.79]16[35.25]:.:. 	0/1:23:.:.:16:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:6,17:99
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr17", "76354679", ".", "G", "A", ".", ".", "FLANK=TAGATATAATA;BaseQRankSum=-0.735;ClippingRankSum=-0.385;DP=23;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.175;QD=20.34;ReadPosRankSum=-0.245;SOR=1.061;IN=1,2"
                , "GT:DP:FT:INF:NNS:OABS:AD:GQ"
                , "0/0:36:.:.:.:A0[0]1[34];G19[34.79]16[35.25]:36:."
                , "0/1:22:.:SOMATIC:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:6,16:."
                , "0/0:.:.:NCIG:.:.:.:."
                , "0/1:23:.:SOMATIC:.:.:6,17:99"});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged4() {
        /*
         * chr1    	1654058	rs61777495     	C      	T      	.      	.      	FLANK=CTTCATCGAAG;DP=17;FS=0.000;MQ=46.03;MQ0=0;QD=28.60;SOR=4.294;IN=1,2;DB;VLD;VAF=0.6313	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	1/1:.:13:.:.:CONF=ZERO:13:11:T10[39.7]3[32.67] 	1/1:.:10:SBIASALT:.:.:9:8:C0[0]1[28];T9[39.67]0[0]     	1/1:0,17:17:.:57:CONF=ZERO:13:11:T10[39.7]3[32.67]     	0/1:1,10:11:SBIASALT:46:.:9:8:C0[0]1[28];T9[39.67]0[0]
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr1", "1654058", "rs61777495", "C", "T", ".", ".", "FLANK=CTTCATCGAAG;DP=17;FS=0.000;MQ=46.03;MQ0=0;QD=28.60;SOR=4.294;IN=1,2;DB;VLD;VAF=0.6313"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "1/1:0,13:13:.:.:.:11:T10[39.7]3[32.67]"
                , "1/1:1,9:10:.:.:.:8:C0[0]1[28];T9[39.67]0[0]"
                , "1/1:0,17:17:.:57:.:.:."
                , "0/1:1,10:11:.:46:.:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged5() {
        /*
         * chr1   	22332008       	rs56968853     	T      	C      	.      	.      	FLANK=CCCGACTGGGT;BaseQRankSum=-2.031;ClippingRankSum=-0.750;DP=39;FS=11.226;MQ=56.26;MQ0=0;MQRankSum=-4.117;QD=1.40;ReadPosRankSum=-0.457;SOR=1.429;IN=1,2;DB;EFF=synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|121|CELA3A|protein_coding|CODING|ENST00000374663|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|5|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|2|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|7|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|8|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|6|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|4|1),sequence_feature[disulfide_bond](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),upstream_gene_variant(MODIFIER||1935||75|CELA3A|protein_coding|CODING|ENST00000400271||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1647|||RN7SL768P|misc_RNA|NON_CODING|ENST00000584415||1)     	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/1:.:64:.:.:CONF=HIGH:7:7:C2[34]5[34.8];T41[33.2]16[35.19]    	0/1:.:39:.:.:.:7:7:C2[35]5[34];T23[33.39]9[35.44]      	0/0:.:57:MIN:.:SOMATIC;GERM=49,185:.:.:C2[34]5[34.8];T41[33.2]16[35.19]	0/1:32,7:39:.:83:SOMATIC;GERM=49,185;CONF=HIGH:7:7:C2[35]5[34];T23[33.39]9[35.44]
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr1", "22332008", "rs56968853", "T", "C", ".", ".", "FLANK=CCCGACTGGGT;BaseQRankSum=-2.031;ClippingRankSum=-0.750;DP=39;FS=11.226;MQ=56.26;MQ0=0;MQRankSum=-4.117;QD=1.40;ReadPosRankSum=-0.457;SOR=1.429;IN=1,2;DB;EFF=synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|121|CELA3A|protein_coding|CODING|ENST00000374663|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|5|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|2|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|7|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|8|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|6|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|4|1),sequence_feature[disulfide_bond](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),upstream_gene_variant(MODIFIER||1935||75|CELA3A|protein_coding|CODING|ENST00000400271||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1647|||RN7SL768P|misc_RNA|NON_CODING|ENST00000584415||1)"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "0/1:57,7:64:.:.:.:7:C2[34]5[34.8];T41[33.2]16[35.19]"
                , "0/1:32,7:39:.:.:.:7:C2[35]5[34];T23[33.39]9[35.44]"
                , "0/0:.:.:.:.:NCIG:.:."
                , "0/1:32,7:39:.:83:SOMATIC;GERM=49,185:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged6() {
        /*
         * COVN12 filter in normal needs to affect the somatic confidence
         *
         * chr1   	176992676      	rs10798496     	C      	T      	.      	.      	FLANK=TCCAGTTGGCT;BaseQRankSum=0.851;ClippingRankSum=0.676;DP=10;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.336;QD=14.78;ReadPosRankSum=-0.336;SOR=0.250;IN=1,2;DB;VLD;VAF=0.2466	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/0:.:8:COVN12:.:SOMATIC;GERM=117,185:.:.:C0[0]8[39.62]	0/1:.:10:.:.:SOMATIC;GERM=117,185;CONF=HIGH:5:5:C1[32]4[35.5];T2[34]3[38]      	0/0:.:8:COVN12:.:SOMATIC;GERM=117,185:.:.:C0[0]8[39.62]	0/1:5,5:10:.:99:SOMATIC;GERM=117,185;CONF=HIGH:5:5:C1[32]4[35.5];T2[34]3[38]
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr1", "176992676", "rs10798496", "C", "T", ".", ".", "FLANK=TCCAGTTGGCT"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "0/0:8:8:.:.:GERM=117,185:.:C0[0]8[39.62"
                , "0/1:5,5:10:.:.:SOMATIC;GERM=117,185:5:C1[32]4[35.5];T2[34]3[38]"
                , "0/0:.:.:.:.:NCIG:.:."
                , "0/1:5,5:10:.:99:SOMATIC;GERM=117,185:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged7() {
        /*
         * SAN3 filter in normal needs to affect the somatic confidence
         * chr5   	101570491      	rs4703217      	A      	G      	.      	.      	FLANK=GACAAGGAAAG;BaseQRankSum=1.262;ClippingRankSum=1.926;DP=19;FS=0.000;MQ=50.85;MQ0=0;MQRankSum=-2.192;QD=29.86;ReadPosRankSum=-0.332;SOR=1.061;IN=1,2;DB;VLD;VAF=0.1832;EFF=synonymous_variant(LOW|SILENT|aaA/aaG|p.Lys15Lys/c.45A>G|90|AC008948.1|protein_coding|CODING|ENST00000597120|1|1),3_prime_UTR_variant(MODIFIER||2071|c.*2071T>C|724|SLCO4C1|protein_coding|CODING|ENST00000310954|13|1)	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	.:.:.:.:.:SOMATIC;GERM=11,185:.:.:.    	1/1:.:19:.:.:SOMATIC;GERM=11,185;CONF=HIGH:17:17:A1[39]1[39];G7[37.43]10[38.4] 	.:.:.:SAN3:.:SOMATIC;GERM=11,185:.:.:. 	1/1:2,17:19:.:8:SOMATIC;GERM=11,185;CONF=HIGH:17:17:A1[39]1[39];G7[37.43]10[38.4]
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr5", "101570491", "rs4703217", "A", "G", ".", ".", "FLANK=GACAAGGAAAG;BaseQRankSum=1.262;ClippingRankSum=1.926;DP=19;FS=0.000;MQ=50.85;MQ0=0;MQRankSum=-2.192;QD=29.86;ReadPosRankSum=-0.332;SOR=1.061;IN=1,2;DB;VLD;VAF=0.1832;EFF=synonymous_variant(LOW|SILENT|aaA/aaG|p.Lys15Lys/c.45A>G|90|AC008948.1|protein_coding|CODING|ENST00000597120|1|1),3_prime_UTR_variant(MODIFIER||2071|c.*2071T>C|724|SLCO4C1|protein_coding|CODING|ENST00000310954|13|1)"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "./.:.:0:.:.:SOMATIC;GERM=11,185:.:."
                , "1/1:2,17:19:.:.:SOMATIC;GERM=11,185:17:A1[39]1[39];G7[37.43]10[38.4]"
                , "0/0:.:.:.:.:NCIG:.:."
                , "1/1:2,17:19:.:8:SOMATIC;GERM=11,185:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged8() {
        /*
         * MIUN filter in normal needs to affect the somatic confidence
         * chr11  	1016978	rs76461263     	T      	G      	.      	.      	FLANK=GTGTGGTTGGG		GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/0:.:63:MIUN:.:SOMATIC;GERM=40,185:.:.:G1[40]0[0];T35[33.11]27[35.33] 	0/1:.:69:.:.:SOMATIC;GERM=40,185;CONF=HIGH:7:5:G1[35]6[39.67];T32[32.12]30[36] 	0/0:.:62:.:.:SOMATIC;GERM=40,185:.:.:G1[40]0[0];T35[33.11]27[35.33]    	0/1:58,15:73:.:99:SOMATIC;GERM=40,185;CONF=HIGH:7:5:G1[35]6[39.67];T32[32.12]30[36]
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr11", "1016978", "rs76461263", "T", "G", ".", ".", "FLANK=GTGTGGTTGGG"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "0/0:62,1:63:.:.:.;GERM=40,185:.:G1[40]0[0];T35[33.11]27[35.33]"
                , "0/1:62,7:69:.:.:SOMATIC;GERM=40,185:5:G1[35]6[39.67];T32[32.12]30[36]"
                , "0/0:.:.:.:.:NCIG:.:."
                , "0/1:58,15:73:.:99:SOMATIC;GERM=40,185:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void confidenceRealLifeMerged9() {
        /*
         * COVN8 filter in normal needs to affect the somatic confidence
         * chr17  	42254527       	rs7217858      	T      	G      	.      	.      	FLANK=AGGACGCCCCC
         */
        //now try the merged record
        VcfRecord vcf = new VcfRecord(new String[]{"chr17", "42254527", "rs7217858", "T", "G", ".", ".", "FLANK=AGGACGCCCCC"
                , "GT:AD:DP:FT:GQ:INF:NNS:OABS"
                , "0/0:3:3:.:.:SOMATIC;GERM=30,185:.:T2[35]1[35]"
                , "1/1:1,11:12:.:.:SOMATIC;GERM=30,185:10:G5[33.6]6[35.67];T0[0]1[38]"
                , "0/0:.:.:.:.:NCIG:.:."
                , "0/1:1,11:12:.:7:SOMATIC;GERM=30,185:.:."});
        ConfidenceMode cm = new ConfidenceMode();
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
    }

    @Test
    public void realLifeCSMIUN() {
        /*
        chr1  11445731        rs386628485     AG      GC      .       .       IN=1;DB;HOM=0,ACAGAGAGACagAGAGTCAGAG    GT:AD:DP:FF:FT:INF:NNS:OABS     0/0:18,1:20:AC1;AG6;AGC1;AT1;A_1;CC1;GC7:PASS:.:.:AG7[]11[];GC0[]1[];GG0
[]1[];_C1[]0[]  0/1:32,4:36:AA1;AG11;A_1;GA1;GC18;G_1:MR:SOMATIC:4:AG21[]11[];A_1[]0[];GC2[]2[];_C1[]0[]        ./.:.:.:.:COV:.:.:.     ./.:.:.:.:COV:.:.:.
         */
        VcfRecord vcf = new VcfRecord(new String[]{"chr1", "11445731", "rs386628485", "AG", "GC", ".", ".", "IN=1;DB;HOM=0,ACAGAGAGACagAGAGTCAGAG"
                , "GT:AD:DP:FF:FT:INF:NNS:OABS"
                , "0/0:18,1:20:AC1;AG6;AGC1;AT1;A_1;CC1;GC7:.:.:.:AG7[]11[];GC0[]1[];GG0[]1[];_C1[]0[]"
                , "0/1:32,4:36:AA1;AG11;A_1;GA1;GC18;G_1:.:SOMATIC:4:AG21[]11[];A_1[]0[];GC2[]2[];_C1[]0[]"
                , "./.:.:.:.:.:.:.:."
                , "./.:.:.:.:.:.:.:."});
        ConfidenceMode cm = new ConfidenceMode(TWO_SAMPLE_TWO_CALLER_META);
        cm.positionRecordMap.put(vcf.getChrPosition(), List.of(vcf));
        cm.addAnnotation();

        vcf = cm.positionRecordMap.get(vcf.getChrPosition()).getFirst();
        assertEquals("MIUN", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("MR", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
        assertEquals("COV", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));

    }

    @Test
    public void applyMRFilter() {
        assertFalse(ConfidenceMode.applyMutantReadFilter(null, null, -1));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{}, "", -1));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{0, 0}, ".", -1));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{0, 0}, "0", -1));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{0, 0}, "10", 5));
        assertTrue(ConfidenceMode.applyMutantReadFilter(new int[]{0, 1}, "10,0", 5));
        assertTrue(ConfidenceMode.applyMutantReadFilter(new int[]{0, 1}, "10,4", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{0, 1}, "10,5", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{0, 1}, "4,5", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{1, 1}, "10,5", 5));
        assertTrue(ConfidenceMode.applyMutantReadFilter(new int[]{1, 1}, "10,4", 5));
        assertTrue(ConfidenceMode.applyMutantReadFilter(new int[]{1, 1}, "10,4,5", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{1, 2}, "10,5,5", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{1, 2}, "0,5,5", 5));
        assertTrue(ConfidenceMode.applyMutantReadFilter(new int[]{1, 2}, "10,5,4", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{2, 2}, "10,5,6", 5));
        assertFalse(ConfidenceMode.applyMutantReadFilter(new int[]{2, 2}, "10,4,5", 5));
    }

    @Test
    public void covFromFF() {
        assertEquals(0, ConfidenceMode.getCoverageFromFailedFilterString(null));
        assertEquals(0, ConfidenceMode.getCoverageFromFailedFilterString(""));
        assertEquals(0, ConfidenceMode.getCoverageFromFailedFilterString("."));
        assertEquals(0, ConfidenceMode.getCoverageFromFailedFilterString("A0"));
        assertEquals(1, ConfidenceMode.getCoverageFromFailedFilterString("A1"));
        assertEquals(10, ConfidenceMode.getCoverageFromFailedFilterString("A10"));
        assertEquals(100, ConfidenceMode.getCoverageFromFailedFilterString("A100"));
        assertEquals(101, ConfidenceMode.getCoverageFromFailedFilterString("A100;B1"));
        assertEquals(110, ConfidenceMode.getCoverageFromFailedFilterString("A100;B10"));
        assertEquals(113, ConfidenceMode.getCoverageFromFailedFilterString("A100;B10;X3"));
        // alts of more than 1 base too
        assertEquals(1, ConfidenceMode.getCoverageFromFailedFilterString("AA1"));
        assertEquals(2, ConfidenceMode.getCoverageFromFailedFilterString("AA1;B1"));
        assertEquals(107, ConfidenceMode.getCoverageFromFailedFilterString("AA1;B1;CAB105"));
        assertEquals(127, ConfidenceMode.getCoverageFromFailedFilterString("AA1;B1;CAB105;H20"));
    }
}
