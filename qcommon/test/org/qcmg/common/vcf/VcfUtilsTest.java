package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class VcfUtilsTest {

    private static final VcfFileMeta TWO_CALLER_TWO_SAMPLE;
    private static final VcfFileMeta TWO_CALLER_ONE_SAMPLE;
    private static final VcfFileMeta ONE_CALLER_TWO_SAMPLE;

    static {
        VcfHeader header = new VcfHeader();
        header.addOrReplace("##1:qControlBamUUID=control-bam-uuid");
        header.addOrReplace("##1:qTestBamUUID=test-bam-uuid");
        header.addOrReplace("##2:qControlBamUUID=control-bam-uuid");
        header.addOrReplace("##2:qTestBamUUID=test-bam-uuid");
        header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	control-bam-uuid_1	test-bam-uuid_1	control-bam-uuid_2	test-bam-uuid_2");
        TWO_CALLER_TWO_SAMPLE = new VcfFileMeta(header);

        header = new VcfHeader();
        header.addOrReplace("##1:qTestBamUUID=test-bam-uuid");
        header.addOrReplace("##2:qTestBamUUID=test-bam-uuid");
        header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	test-bam-uuid_1	test-bam-uuid_2");
        TWO_CALLER_ONE_SAMPLE = new VcfFileMeta(header);

        header = new VcfHeader();
        header.addOrReplace("##1:qControlBamUUID=control-bam-uuid");
        header.addOrReplace("##1:qTestBamUUID=test-bam-uuid");
        header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	control-bam-uuid	test-bam-uuid");
        ONE_CALLER_TWO_SAMPLE = new VcfFileMeta(header);
    }


    @Test
    public void getAlleles() {
        String gt = "0/0";
        String ref = "R";
        String alt = "A";
        String[] alleles = VcfUtils.getAlleles(gt, ref, alt);
        assertEquals(2, alleles.length);
        assertEquals("R", alleles[0]);
        assertEquals("R", alleles[1]);

        gt = "0/1";
        alleles = VcfUtils.getAlleles(gt, ref, alt);
        assertEquals(2, alleles.length);
        assertEquals("R", alleles[0]);
        assertEquals("A", alleles[1]);

        gt = "1/1";
        alleles = VcfUtils.getAlleles(gt, ref, alt);
        assertEquals(2, alleles.length);
        assertEquals("A", alleles[0]);
        assertEquals("A", alleles[1]);

        alt = "A,B";
        alleles = VcfUtils.getAlleles(gt, ref, alt);
        assertEquals(2, alleles.length);
        assertEquals("A", alleles[0]);
        assertEquals("A", alleles[1]);

        gt = "1/2";
        alleles = VcfUtils.getAlleles(gt, ref, alt);
        assertEquals(2, alleles.length);
        assertEquals("A", alleles[0]);
        assertEquals("B", alleles[1]);
    }

    @Test
    public void decomposeOABS() {
        String oabs = "A0[0]33[35.73];G6[30.5]2[34]";
        Map<String, Integer> m = VcfUtils.getAllelicCoverage(oabs);
        assertEquals(2, m.size());
        assertEquals(33, m.get("A").intValue());
        assertEquals(8, m.get("G").intValue());

        oabs = "AB10[0]33[35.73];GH26[30.5]12[34]";
        m = VcfUtils.getAllelicCoverage(oabs);
        assertEquals(2, m.size());
        assertEquals(43, m.get("AB").intValue());
        assertEquals(38, m.get("GH").intValue());
    }

    @Test
    public void decomposeEOR() {
        String oabs = "A0[]33[];G6[]2[]";
        Map<String, Integer> m = VcfUtils.getAllelicCoverage(oabs);
        assertEquals(2, m.size());
        assertEquals(33, m.get("A").intValue());
        assertEquals(8, m.get("G").intValue());

        oabs = "AB10[]33[];GH26[]12[]";
        m = VcfUtils.getAllelicCoverage(oabs);
        assertEquals(2, m.size());
        assertEquals(43, m.get("AB").intValue());
        assertEquals(38, m.get("GH").intValue());
    }

    @Test
    public void decomposeEORWithStrand() {
        String eor = "A0[]33[];G6[]2[]";
        Map<String, int[]> m = VcfUtils.getAllelicCoverageWithStrand(eor);
        assertEquals(2, m.size());
        assertArrayEquals(new int[]{0, 33}, m.get("A"));
        assertArrayEquals(new int[]{6, 2}, m.get("G"));

        eor = "AB10[]33[];GH26[]12[]";
        m = VcfUtils.getAllelicCoverageWithStrand(eor);
        assertEquals(2, m.size());
        assertArrayEquals(new int[]{10, 33}, m.get("AB"));
        assertArrayEquals(new int[]{26, 12}, m.get("GH"));
    }

    @Test
    public void mergeGATKControlOnly() {
        String s = "chr1\t13550\t.\tG\tA\t367.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.943;ClippingRankSum=0.411;DP=25;FS=36.217;MLEAC=1;MLEAF=0.500;MQ=27.62;MQRankSum=3.312;QD=14.71;ReadPosRankSum=1.670;SOR=3.894\tGT:AD:DP:GQ:PL\t0/1:11,14:25:99:396,0,223";
        VcfRecord vcf = new VcfRecord(s.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(vcf, null);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(2));
    }

    @Test
    public void mergeGATKControlOnlyRealLife() {
        String s = "chr1\t881627\trs2272757\tG\tA\t164.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-2.799;ClippingRankSum=1.555;DB;DP=18;FS=7.375;MLEAC=1;MLEAF=0.500;MQ=59.94;MQ0=0;MQRankSum=0.400;QD=9.15;ReadPosRankSum=0.755;SOR=0.044\tGT:AD:DP:GQ:PL\t0/1:10,8:18:99:193,0,370";
        VcfRecord vcf = new VcfRecord(s.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(vcf, null);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(2));
        assertTrue(mergedVcf.getFormatFields().get(1).contains("0/1"));
    }

    @Test
    public void calculateAD() {
        assertEquals("0,1", VcfUtils.calculateAD("0,1", "1/1", "1/1"));
        assertEquals("0,0,1", VcfUtils.calculateAD("0,1", "2/2", "1/1"));
        assertEquals("0,0,1", VcfUtils.calculateAD("0,1", "0/2", "0/1"));
        assertEquals("0,100,50", VcfUtils.calculateAD("0,100,50", "1/2", "1/2"));
        assertEquals("0,0,100", VcfUtils.calculateAD("0,100", "2/2", "1/1"));
        assertEquals("0,0,100,50", VcfUtils.calculateAD("0,100,50", "2/3", "1/2"));
        assertEquals("0,100,0,50", VcfUtils.calculateAD("0,100,50", "3/3", "2/2"));
    }

    @Test
    public void mergeGATKRealLife() {
        String s1 = "chr4	49123053	.	G	A	800.77	.	AC=1;AF=0.500;AN=2;BaseQRankSum=-0.287;ClippingRankSum=0.000;DP=147;ExcessHet=3.0103;FS=13.746;MLEAC=1;MLEAF=0.500;MQ=49.35;MQRankSum=1.081;QD=5.89;ReadPosRankSum=0.743;SOR=3.371	GT:AD:DP:GQ:PL	0/1:108,28:136:99:829,0,4491";
        String s2 = "chr4	49123053	.	G	T	544.77	.	AC=1;AF=0.500;AN=2;BaseQRankSum=1.033;ClippingRankSum=0.000;DP=72;ExcessHet=3.0103;FS=13.528;MLEAC=1;MLEAF=0.500;MQ=47.67;MQRankSum=1.416;QD=11.35;ReadPosRankSum=0.807;SOR=3.456	GT:AD:DP:GQ:PL	0/1:33,15:48:99:573,0,2744";
        VcfRecord vcf1 = new VcfRecord(s1.split("\t"));
        VcfRecord vcf2 = new VcfRecord(s2.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(vcf1, vcf2);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("A,T", mergedVcf.getAlt());
        assertEquals("GT:AD:DP:GQ:QL", mergedVcf.getFormatFields().get(0));
        assertEquals("0/1:108,28:136:99:800.77", mergedVcf.getFormatFields().get(1));
        assertEquals("0/2:33,0,15:48:99:544.77", mergedVcf.getFormatFields().get(2));
    }

    @Test
    public void mergeGATKTestOnly() {
        String s = "chr1\t13550\t.\tG\tA\t367.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.943;ClippingRankSum=0.411;DP=25;FS=36.217;MLEAC=1;MLEAF=0.500;MQ=27.62;MQRankSum=3.312;QD=14.71;ReadPosRankSum=1.670;SOR=3.894\tGT:AD:DP:GQ:PL\t0/1:11,14:25:99:396,0,223";
        VcfRecord vcf = new VcfRecord(s.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(null, vcf);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(1));
    }

    @Test
    public void mergeGATKSameAlts() {
        String s = "chr1\t13838\t.\tC\tT\t49.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124\tGT:AD:DP:GQ:PL\t0/1:10,3:13:78:78,0,370";
        VcfRecord cVcf = new VcfRecord(s.split("\t"));
        s = "chr1\t13838\t.\tC\tT\t157.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.817;ClippingRankSum=-0.696;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=26.49;MQRankSum=-1.545;QD=12.14;ReadPosRankSum=0.000;SOR=1.179\tGT:AD:DP:GQ:PL\t0/1:8,5:13:99:186,0,330";
        VcfRecord tVcf = new VcfRecord(s.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(cVcf, tVcf);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getQualString());
        assertEquals("BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124", mergedVcf.getInfo());
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("0/1:10,3:13:78:49.77", mergedVcf.getFormatFields().get(1));
        assertEquals("0/1:8,5:13:99:157.77", mergedVcf.getFormatFields().get(2));
        assertNull(mergedVcf.getInfoRecord().getField("AC"));
        assertNull(mergedVcf.getInfoRecord().getField("AF"));
        assertNull(mergedVcf.getInfoRecord().getField("MLEAC"));
        assertNull(mergedVcf.getInfoRecord().getField("MLEAF"));
    }

    @Test
    public void mergeGATKDiffAlts() {
        String s = "chr1\t13838\t.\tC\tT\t49.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124\tGT:AD:DP:GQ:PL\t0/1:10,3:13:78:78,0,370";
        VcfRecord cVcf = new VcfRecord(s.split("\t"));
        s = "chr1\t13838\t.\tC\tG\t157.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.817;ClippingRankSum=-0.696;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=26.49;MQRankSum=-1.545;QD=12.14;ReadPosRankSum=0.000;SOR=1.179\tGT:AD:DP:GQ:PL\t0/1:8,5:13:99:186,0,330";
        VcfRecord tVcf = new VcfRecord(s.split("\t"));
        Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(cVcf, tVcf);
        assertTrue(oMergedVcf.isPresent());
        VcfRecord mergedVcf = oMergedVcf.get();
        assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getQualString());
        assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getInfo());
        assertEquals(3, mergedVcf.getFormatFields().size());
        assertEquals("0/1:10,3:13:78:49.77", mergedVcf.getFormatFields().get(1));
        assertEquals("0/2:8,0,5:13:99:157.77", mergedVcf.getFormatFields().get(2));
    }

    @Test
    public void removeGATKINFOFelds() {
        String s = "chrY	2672735	.	ATT	A	123.86	COVN8;NNS	GATKINFO;NIOC=0;SSOI=1.000;SVTYPE=DEL;END=2672737;IN=1	GT:GD:ACINDEL	0/1:ATT/A:1,1,1,1[1,0],1[1],0,0,1	.";
        VcfRecord vcf = new VcfRecord(s.split("\t"));
        VcfUtils.removeElementsFromInfoField(vcf);
        assertEquals("GT:GD:ACINDEL	0/1:ATT/A:1,1,1,1[1,0],1[1],0,0,1	.", vcf.getFormatFieldStrings());

        s = "chrY	2672735	.	ATT	A	123.86	COVN8;NNS	GATKINFO;NIOC=0;SSOI=1.000;SVTYPE=DEL;END=2672737;IN=1;AN=1	GT:GD:ACINDEL	0/1:ATT/A:1,1,1,1[1,0],1[1],0,0,1	.";
        vcf = new VcfRecord(s.split("\t"));
        VcfUtils.removeElementsFromInfoField(vcf);
        assertEquals("GATKINFO;NIOC=0;SSOI=1.000;SVTYPE=DEL;END=2672737;IN=1", vcf.getInfo());

        s = "chrY	2672735	.	ATT	A	123.86	COVN8;NNS	GATKINFO;NIOC=0;SSOI=1.000;SVTYPE=DEL;END=2672737;IN=1;AN=1	GT:GD:ACINDEL	0/1:ATT/A:1,1,1,1[1,0],1[1],0,0,1	.";
        vcf = new VcfRecord(s.split("\t"));
        VcfUtils.removeElementsFromInfoField(vcf, "AC");
        assertEquals("GATKINFO;NIOC=0;SSOI=1.000;SVTYPE=DEL;END=2672737;IN=1;AN=1", vcf.getInfo());
    }

    @Test
    public void getAlleleDistMap() {
        String ac = "A7[41.29],9[38.56],C14[38.43],12[37.83]";
        Map<String, Integer> m = VcfUtils.getAllelicCoverageFromAC(ac);
        assertEquals(2, m.size());
        assertEquals(16, m.get("A").intValue());
        assertEquals(26, m.get("C").intValue());

        ac = "A7[],9[],C14[],12[],G1[],10[],T100[],0[]";
        m = VcfUtils.getAllelicCoverageFromAC(ac);
        assertEquals(4, m.size());
        assertEquals(16, m.get("A").intValue());
        assertEquals(26, m.get("C").intValue());
        assertEquals(11, m.get("G").intValue());
        assertEquals(100, m.get("T").intValue());
    }

    @Test
    public void createVcf() {
        VcfRecord r = VcfUtils.createVcfRecord(new ChrPointPosition("1", 1), "id", "ref", "alt");
        assertEquals("1", r.getChromosome());
        assertEquals(1, r.getPosition());
        assertEquals(1 + r.getRef().length() - 1, r.getChrPosition().getEndPosition());
        assertEquals("id", r.getId());
        assertEquals("ref", r.getRef());
        assertEquals("alt", r.getAlt());

        r = VcfUtils.createVcfRecord(new ChrPointPosition("1", 1), "id", "A", "alt");
        assertEquals("1", r.getChromosome());
        assertEquals(1, r.getPosition());
        assertEquals(1 + r.getRef().length() - 1, r.getChrPosition().getEndPosition());
        assertEquals("id", r.getId());
        assertEquals("A", r.getRef());
        assertEquals("alt", r.getAlt());

        r = VcfUtils.createVcfRecord(new ChrRangePosition("1", 1, 2), "id", "AA", "alt");
        assertEquals("1", r.getChromosome());
        assertEquals(1, r.getPosition());
        assertEquals(1 + r.getRef().length() - 1, r.getChrPosition().getEndPosition());
        assertEquals("id", r.getId());
        assertEquals("AA", r.getRef());
        assertEquals("alt", r.getAlt());

        r = VcfUtils.createVcfRecord(new ChrRangePosition("1", 1, 2), "id", "AAA", "alt");
        assertEquals("1", r.getChromosome());
        assertEquals(1, r.getPosition());
        assertEquals(1 + r.getRef().length() - 1, r.getChrPosition().getEndPosition());
        assertEquals("id", r.getId());
        assertEquals("AAA", r.getRef());
        assertEquals("alt", r.getAlt());
    }

    @Test
    public void getOABSDetails() {
        Map<String, int[]> map = VcfUtils.getAllelicCoverageWithStrand("A1[10]0[0]");
        assertEquals(1, map.size());
        assertEquals(1, map.get("A")[0]);
        assertEquals(0, map.get("A")[1]);

        map = VcfUtils.getAllelicCoverageWithStrand("A1[10]0[0];B0[0]10[2]");
        assertEquals(2, map.size());
        assertEquals(1, map.get("A")[0]);
        assertEquals(0, map.get("A")[1]);
        assertEquals(0, map.get("B")[0]);
        assertEquals(10, map.get("B")[1]);

        map = VcfUtils.getAllelicCoverageWithStrand("A1[10]0[0];B0[0]10[2];C12[44]21[33]");
        assertEquals(3, map.size());
        assertEquals(1, map.get("A")[0]);
        assertEquals(0, map.get("A")[1]);
        assertEquals(0, map.get("B")[0]);
        assertEquals(10, map.get("B")[1]);
        assertEquals(12, map.get("C")[0]);
        assertEquals(21, map.get("C")[1]);
    }

    @Test
    public void getFormatField() {
        assertNull(VcfUtils.getFormatField((List<String>) null, null, 0));
        assertNull(VcfUtils.getFormatField((List<String>) null, "", 0));
        assertNull(VcfUtils.getFormatField(List.of(""), "", 0));
        assertNull(VcfUtils.getFormatField(List.of("s"), "s", 0));
        assertEquals("hello?", VcfUtils.getFormatField(Arrays.asList("s", "hello?"), "s", 0));
        assertNull(VcfUtils.getFormatField(Arrays.asList("s", "hello?"), "s", 1));
        assertNull(VcfUtils.getFormatField(Arrays.asList("s", "hello?"), "t", 1));
        assertNull(VcfUtils.getFormatField(Arrays.asList("s", "hello?"), "t", 0));
        assertEquals("there", VcfUtils.getFormatField(Arrays.asList("s:t", "hello?:there"), "t", 0));
        assertNull(VcfUtils.getFormatField(Arrays.asList("s:t", "hello?:there"), "t", 1));
        assertEquals("again", VcfUtils.getFormatField(Arrays.asList("s:t", "hello?:there", ":again"), "t", 1));
        assertEquals("", VcfUtils.getFormatField(Arrays.asList("s:t", "hello?:there", ":again"), "s", 1));
    }


    @Test
    public void getGEFromGATKVcf() {
        VcfRecord r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "1/1:0,45:45:99:1848,135,0"});
        assertEquals(GenotypeEnum.TT, VcfUtils.getGEFromGATKVCFRec(r));
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "0/0:0,45:45:99:1848,135,0"});
        assertEquals(GenotypeEnum.GG, VcfUtils.getGEFromGATKVCFRec(r));
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "0/1:0,45:45:99:1848,135,0"});
        assertEquals(GenotypeEnum.GT, VcfUtils.getGEFromGATKVCFRec(r));
    }

    @Test
    public void getGTData() {
        VcfRecord r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "1/1:0,45:45:99:1848,135,0"});
        assertEquals("1/1", VcfUtils.getGenotypeFromGATKVCFRecord(r));
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "0/1:0,45:45:99:1848,135,0"});
        assertEquals("0/1", VcfUtils.getGenotypeFromGATKVCFRecord(r));
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "0/0:0,45:45:99:1848,135,0"});
        assertEquals("0/0", VcfUtils.getGenotypeFromGATKVCFRecord(r));
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", "1/2:0,45:45:99:1848,135,0"});
        assertEquals("1/2", VcfUtils.getGenotypeFromGATKVCFRecord(r));
    }

    @Test
    public void getGTDataDuffRealLifeData() {
        VcfRecord r = new VcfRecord(new String[]{"GL000222.1", "80278", ".", "C", "A", "427.77", ".", "AC=1;AF=0.500;AN=2;BaseQRankSum=0GL000222.1", "80426", ".", "C", "A", "579.77", ".", "AC=1;AF=0.500;AN=2;BaseQRankSum=1.845;ClippingRankSum=0.329;DP=81;FS=46.164;MLEAC=1;MLEAF=0.500;MQ=35.11;MQ0=0;MQRankSum=4.453;QD=7.16;ReadPosRankSum=1.477;SOR=5.994", "GT:AD:DP:GQ:PL", "0/1:62,19:81:99:608,0,2576"});
        try {
            VcfUtils.getGenotypeFromGATKVCFRecord(r);
            Assert.fail("Should have thrown an IAE");
        } catch (IllegalArgumentException ignored) {
        }

        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT:AD:DP:GQ:PL", ""});
        try {
            VcfUtils.getGenotypeFromGATKVCFRecord(r);
            Assert.fail("Should have thrown an IAE");
        } catch (IllegalArgumentException ignored) {
        }

        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT", "0/"});
        try {
            VcfUtils.getGenotypeFromGATKVCFRecord(r);
            Assert.fail("Should have thrown an IAE");
        } catch (IllegalArgumentException ignored) {
        }
        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GG", "0/1"});
        try {
            VcfUtils.getGenotypeFromGATKVCFRecord(r);
            Assert.fail("Should have thrown an IAE");
        } catch (IllegalArgumentException ignored) {
        }

        r = new VcfRecord(new String[]{"GL000192.1", "228788", ".", "G", "T", "1819.77", ".", "AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038", "GT", "0/1"});
        assertEquals("0/1", VcfUtils.getGenotypeFromGATKVCFRecord(r));
    }

    @Test
    public void isCS() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "B"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "B"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "A", "BB"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB"});
        assertTrue(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,C"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,CC"});
        assertTrue(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,CC,D"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,CC,DDD"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,CC,DDDDD"});
        assertFalse(VcfUtils.isCompoundSnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AA", "BB,CC,DD"});
        assertTrue(VcfUtils.isCompoundSnp(rec));
    }

    @Test
    public void individualFilterFileds() {
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(null, null));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{}, null));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{}, new short[]{}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(null, new short[]{}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS"}, new short[]{0}));
        assertTrue(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS"}, new short[]{1}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS"}, new short[]{2}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASSE"}, new short[]{1}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"pass"}, new short[]{1}));
        assertTrue(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS", "FAIL"}, new short[]{1}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS", "FAIL"}, new short[]{2}));
        assertFalse(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS", "FAIL"}, new short[]{1, 2}));
        assertTrue(VcfUtils.areTheseFilterFieldsAPass(new String[]{"PASS", "PASS"}, new short[]{1, 2}));
    }

    @Test
    public void shortcut() {
        assertTrue(VcfUtils.getShortCutPassFail(null).isPresent());
        assertEquals(false, VcfUtils.getShortCutPassFail(null).get());
        assertTrue(VcfUtils.getShortCutPassFail(new String[]{}).isPresent());
        assertEquals(false, VcfUtils.getShortCutPassFail(new String[]{}).get());
        assertTrue(VcfUtils.getShortCutPassFail(new String[]{"FAIL"}).isPresent());
        assertEquals(false, VcfUtils.getShortCutPassFail(new String[]{"FAIL"}).get());
        assertTrue(VcfUtils.getShortCutPassFail(new String[]{"PASS"}).isPresent());
        assertEquals(true, VcfUtils.getShortCutPassFail(new String[]{"PASS"}).get());
        assertFalse(VcfUtils.getShortCutPassFail(new String[]{"PASS", "FAIL"}).isPresent());
        assertFalse(VcfUtils.getShortCutPassFail(new String[]{"PASS", "FAIL"}).isPresent());
        assertFalse(VcfUtils.getShortCutPassFail(new String[]{"FAIL", "PASS"}).isPresent());
        assertFalse(VcfUtils.getShortCutPassFail(new String[]{"FAIL", "PASS", "PASS"}).isPresent());
        assertFalse(VcfUtils.getShortCutPassFail(new String[]{"FAIL", "PASS", "FAIL"}).isPresent());
        assertTrue(VcfUtils.getShortCutPassFail(new String[]{"FAIL", "FAIL", "FAIL"}).isPresent());
        assertEquals(false, VcfUtils.getShortCutPassFail(new String[]{"FAIL", "FAIL", "FAIL"}).get());
        assertTrue(VcfUtils.getShortCutPassFail(new String[]{"PASS", "PASS", "PASS"}).isPresent());
        assertEquals(true, VcfUtils.getShortCutPassFail(new String[]{"PASS", "PASS", "PASS"}).get());
    }

    @Test
    public void getConfidence() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});
        assertNull(VcfUtils.getConfidence(rec));
        rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.LOW);
        assertEquals("LOW", VcfUtils.getConfidence(rec));
        rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.HIGH);
        assertEquals("HIGH", VcfUtils.getConfidence(rec));
        rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.ZERO);
        assertEquals("ZERO", VcfUtils.getConfidence(rec));
    }

    @Test
    public void getConfidenceMergedRec() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});
        assertNull(VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=HIGH_1,ZERO_2");
        assertEquals("HIGH_1,ZERO_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=HIGH_1,HIGH_2");
        assertEquals("HIGH_1,HIGH_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=LOW_1,HIGH_2");
        assertEquals("LOW_1,HIGH_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=ZERO_1,HIGH_2");
        assertEquals("ZERO_1,HIGH_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=LOW_1,LOW_2");
        assertEquals("LOW_1,LOW_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=ZERO_1,LOW_2");
        assertEquals("ZERO_1,LOW_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=LOW_1,ZERO_2");
        assertEquals("LOW_1,ZERO_2", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=LOW_2,ZERO_1");
        assertEquals("LOW_2,ZERO_1", VcfUtils.getConfidence(rec));
        rec.setInfo("CONF=ZERO_2,ZERO_1");
        assertEquals("ZERO_2,ZERO_1", VcfUtils.getConfidence(rec));
    }


    @Test
    public void getMapOfFFs() {
        String ff = "AB:CD:EF\t.:.:.";
        Map<String, String[]> m = VcfUtils.getFormatFieldsAsMap(ff);
        assertEquals(3, m.size());
        assertEquals(1, m.get("AB").length);
        assertEquals(1, m.get("CD").length);
        assertEquals(1, m.get("EF").length);
        assertEquals(".", m.get("AB")[0]);
        assertEquals(".", m.get("CD")[0]);
        assertEquals(".", m.get("EF")[0]);

        ff = "AB:CD:EF\t.:.:.\tThis:is:thebest";
        m = VcfUtils.getFormatFieldsAsMap(ff);
        assertEquals(3, m.size());
        assertEquals(2, m.get("AB").length);
        assertEquals(2, m.get("CD").length);
        assertEquals(2, m.get("EF").length);
        assertEquals(".", m.get("AB")[0]);
        assertEquals(".", m.get("CD")[0]);
        assertEquals(".", m.get("EF")[0]);
        assertEquals("This", m.get("AB")[1]);
        assertEquals("is", m.get("CD")[1]);
        assertEquals("thebest", m.get("EF")[1]);
    }


    @Test
    public void mapToList() {
        Map<String, String[]> map = new HashMap<>();

        map.put("AB", new String[]{".", "This"});
        map.put("CD", new String[]{".", "is"});
        map.put("EF", new String[]{".", "thebest"});

        List<String> l = VcfUtils.convertFFMapToList(map);
        assertEquals(3, l.size());
        assertEquals("AB:CD:EF\t.:.:.\tThis:is:thebest", String.join(Constants.TAB_STRING, l));
    }

    @Test
    public void mapToListWithGT() {
        Map<String, String[]> map = new HashMap<>();

        map.put("AB", new String[]{".", "This"});
        map.put("CD", new String[]{".", "is"});
        map.put("GT", new String[]{".", "REALLY"});
        map.put("EF", new String[]{".", "thebest"});

        List<String> l = VcfUtils.convertFFMapToList(map);
        assertEquals(3, l.size());
        assertEquals("GT:AB:CD:EF\t.:.:.:.\tREALLY:This:is:thebest", String.join(Constants.TAB_STRING, l));
    }

    @Test
    public void isRecordSomatic() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});
        assertFalse(VcfUtils.isRecordSomatic(rec));

        rec.setInfo(VcfHeaderUtils.INFO_SOMATIC);
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1");
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1,2");
        assertTrue(VcfUtils.isRecordSomatic(rec));

        rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1,2");
        assertTrue(VcfUtils.isRecordSomatic(rec));

        rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2;SOMATIC");
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2");
        assertFalse(VcfUtils.isRecordSomatic(rec));

        /*
         * needs to be set in ff
         */
        rec.setFormatFields(Arrays.asList("INF", ".", "."));
        assertFalse(VcfUtils.isRecordSomatic(rec));
        rec.setFormatFields(Arrays.asList("INF", ".", "SOMATIC"));
        assertFalse(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("FLANK=ACCCTGGAAGA;IN=1");
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("FLANK=ACCCTGGAAGA;IN=2");
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2");
        rec.setFormatFields(Arrays.asList("INF", ".", "SOMATIC", ".", "."));
        assertFalse(VcfUtils.isRecordSomatic(rec));
        rec.setFormatFields(Arrays.asList("INF", ".", "SOMATIC", ".", "SOMATIC"));
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setFormatFields(Arrays.asList("INF", "SOMATIC", "SOMATIC", ".", "SOMATIC"));
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setFormatFields(Arrays.asList("INF", "SOMATIC", "SOMATIC", "SOMATIC", "SOMATIC"));
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setFormatFields(Arrays.asList("INF", ".", "SOMATIC", ".", "SOMATIC", "."));
        assertTrue(VcfUtils.isRecordSomatic(rec));
        rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2,3");
        assertFalse(VcfUtils.isRecordSomatic(rec));
    }

    @Test
    public void callerCount() {
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount(null));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount(""));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("."));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("Hello"));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("there"));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("in=1"));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("in=1,2"));
        assertEquals(OptionalInt.empty(), VcfUtils.getCallerCount("in=2"));
        assertEquals(OptionalInt.of(1), VcfUtils.getCallerCount("IN=1"));
        assertEquals(OptionalInt.of(1), VcfUtils.getCallerCount("IN=2"));
        assertEquals(OptionalInt.of(1), VcfUtils.getCallerCount("IN=8"));
        assertEquals(OptionalInt.of(2), VcfUtils.getCallerCount("IN=1,2"));
        assertEquals(OptionalInt.of(3), VcfUtils.getCallerCount("IN=1,2,3"));
        assertEquals(OptionalInt.of(3), VcfUtils.getCallerCount("IN=4,5,6"));
    }

    @Test
    public void hasRecordBeenMerged() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});
        assertFalse(VcfUtils.isMergedRecord(rec));

        rec.setInfo(".");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("SOMATIC");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("THIS_SHOULD_BE_FALSE_IN=1,2");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("THIS_SHOULD_BE_TRUE;IN=1,2");
        assertTrue(VcfUtils.isMergedRecord(rec));
        rec.setInfo("IN=1");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1");
        assertFalse(VcfUtils.isMergedRecord(rec));
        rec.setInfo("FLANK=TGTCCATTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.212;ClippingRankSum=1.855;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=21.57;MQ0=0;MQRankSum=-0.533;QD=10.83;ReadPosRankSum=0.696;SOR=1.402;IN=1,2");
        assertTrue(VcfUtils.isMergedRecord(rec));
    }

    @Test
    public void testMissingDataInFormatField() {
        VcfRecord r = new VcfRecord(new String[]{"chr1", "52924633", "rs12072217", "C", "T", "671.77", "NCIT", "AC=1;AF=0.500;AN=2;BaseQRankSum=0.655;ClippingRankSum=-1.179;DB;DP=33;FS=1.363;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-1.067;QD=20.36;ReadPosRankSum=-0.655;SOR=0.990", "GT:AD:DP:GQ:PL", "0/1:12,21:33:99:700,0,339"});
        VcfUtils.addMissingDataToFormatFields(r, 2);
        assertEquals(3, r.getFormatFields().size());
        assertEquals("0/1:12,21:33:99:700,0,339", r.getFormatFields().get(1));
        assertEquals("./.:.:.:.:.", r.getFormatFields().get(2));
    }

    @Test
    public void controlMissingDataInFormatField() {
        VcfRecord r = new VcfRecord(new String[]{"chr1", "52924633", "rs12072217", "C", "T", "671.77", "NCIT", "AC=1;AF=0.500;AN=2;BaseQRankSum=0.655;ClippingRankSum=-1.179;DB;DP=33;FS=1.363;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-1.067;QD=20.36;ReadPosRankSum=-0.655;SOR=0.990", "GT:AD:DP:GQ:PL", "0/1:12,21:33:99:700,0,339"});
        VcfUtils.addMissingDataToFormatFields(r, 1);
        assertEquals(3, r.getFormatFields().size());
        assertEquals("0/1:12,21:33:99:700,0,339", r.getFormatFields().get(2));
        assertEquals("./.:.:.:.:.", r.getFormatFields().get(1));
    }

    @Test
    public void missingDataToFormatField() {
        try {
            VcfUtils.addMissingDataToFormatFields(null, 0);
            Assert.fail("Should have thrown an illegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});

        //VcfUtils.createVcfRecord("1", 1, "A");
        VcfUtils.addMissingDataToFormatFields(rec, 1);
        assertEquals(0, rec.getFormatFields().size());

        // add in an empty list for the ff
        List<String> ff = new ArrayList<>();
        ff.add("header info here");
        ff.add("first bit of data");
        rec.setFormatFields(ff);

        try {
            VcfUtils.addMissingDataToFormatFields(rec, 0);
            Assert.fail("Should have thrown an illegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            VcfUtils.addMissingDataToFormatFields(rec, 10);
            Assert.fail("Should have thrown an illegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        VcfUtils.addMissingDataToFormatFields(rec, 1);
        ff = rec.getFormatFields();
        assertEquals(3, ff.size());
        assertEquals("header info here", ff.get(0));
        assertEquals(".", ff.get(1));
        assertEquals("first bit of data", ff.get(2));

        VcfUtils.addMissingDataToFormatFields(rec, 3);
        ff = rec.getFormatFields();
        assertEquals(4, ff.size());
        assertEquals("header info here", ff.get(0));
        assertEquals(".", ff.get(1));
        assertEquals("first bit of data", ff.get(2));
        assertEquals(".", ff.get(3));

        VcfUtils.addMissingDataToFormatFields(rec, 1);
        ff = rec.getFormatFields();
        assertEquals(5, ff.size());
        assertEquals("header info here", ff.get(0));
        assertEquals(".", ff.get(1));
        assertEquals(".", ff.get(2));
        assertEquals("first bit of data", ff.get(3));
        assertEquals(".", ff.get(4));
    }

    @Test
    public void missingDataAgain() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "."});//VcfUtils.createVcfRecord("1", 1, "A");
        VcfUtils.addMissingDataToFormatFields(rec, 1);
        assertEquals(0, rec.getFormatFields().size());

        // add in an empty list for the ff
        List<String> ff = new ArrayList<>();
        ff.add("AC:DC:12:3");
        ff.add("0/1:1/1:45,45,:xyz");
        rec.setFormatFields(ff);

        VcfUtils.addMissingDataToFormatFields(rec, 1);
        ff = rec.getFormatFields();
        assertEquals(3, ff.size());
        assertEquals("AC:DC:12:3", ff.get(0));
        assertEquals(".:.:.:.", ff.get(1));
    }

    @Test
    public void testCalculateGenotypeEnum() {

        assertNull(VcfUtils.calculateGenotypeEnum(null, '\u0000', '\u0000'));
        assertNull(VcfUtils.calculateGenotypeEnum("", '\u0000', '\u0000'));
        assertNull(VcfUtils.calculateGenotypeEnum("", 'X', 'Y'));
        assertNull(VcfUtils.calculateGenotypeEnum("0/1", 'X', 'Y'));

        assertEquals(GenotypeEnum.AA, VcfUtils.calculateGenotypeEnum("0/0", 'A', 'C'));
        assertEquals(GenotypeEnum.CC, VcfUtils.calculateGenotypeEnum("1/1", 'A', 'C'));
        assertEquals(GenotypeEnum.AC, VcfUtils.calculateGenotypeEnum("0/1", 'A', 'C'));

        assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/0", 'G', 'G'));
        assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/1", 'G', 'G'));
        assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("1/1", 'G', 'G'));
    }

    @Test
    public void getGTStringWithCommas() {
        assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas(null, '\u0000', null));
        assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas("", '\u0000', null));
        assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas("A", 'C', null));
        assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A", 'C', GenotypeEnum.CC));
        assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.CC));
        assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'T', GenotypeEnum.TT));
        assertEquals("0/1", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.AC));
        assertEquals("1/2", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.AG));
        assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.CG));
        assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CG));
        assertEquals("1/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AT));
        assertEquals("0/1", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AC));
        assertEquals("1/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AG));
        assertEquals("1/1", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AA));
        assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CC));
        assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CG));
        assertEquals("0/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CT));
        assertEquals("2/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.GG));
        assertEquals("2/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.GT));
        assertEquals("3/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.TT));
    }

    @Test
    public void updateAltString() {
        assertEquals("C", VcfUtils.getUpdateAltString("CT", "CT", "C"));
    }

    @Test
    public void updateAltStringRealData() {
        assertEquals("CTT", VcfUtils.getUpdateAltString("CTTT", "CT", "C"));
        assertEquals("CT", VcfUtils.getUpdateAltString("CTTT", "CTT", "C"));
        assertEquals("CTTTT", VcfUtils.getUpdateAltString("CTTT", "C", "CT"));
        assertEquals("C", VcfUtils.getUpdateAltString("CTTT", "CTTT", "C"));

        assertEquals("T", VcfUtils.getUpdateAltString("TAAA", "TAAA", "T"));
        assertEquals("TAA", VcfUtils.getUpdateAltString("TAAA", "TA", "T"));
        assertEquals("TAAAA", VcfUtils.getUpdateAltString("TAAA", "T", "TA"));

        assertEquals("A", VcfUtils.getUpdateAltString("ATTTG", "ATTTG", "A"));
        assertEquals("TTTTG", VcfUtils.getUpdateAltString("ATTTG", "A", "T"));
        assertEquals("CTTTG", VcfUtils.getUpdateAltString("ATTTG", "A", "C"));
		
		/*
		 * r7	151921032	.	CAT	C	.	.	END=151921034	
09:52:23.555 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTT	C	.	.	END=151921036	
09:52:23.555 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTTG	C	.	.	END=151921037	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTTGT	C	.	.	END=151921038	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATT	C	.	.	END=151921035	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CA	C	.	.	END=151921033	
		 */
        assertEquals("CGT", VcfUtils.getUpdateAltString("CATTTGT", "CATTT", "C"));
        assertEquals("CT", VcfUtils.getUpdateAltString("CATTTGT", "CATTTG", "C"));
        assertEquals("C", VcfUtils.getUpdateAltString("CATTTGT", "CATTTGT", "C"));
        assertEquals("CTGT", VcfUtils.getUpdateAltString("CATTTGT", "CATT", "C"));
        assertEquals("CTTTGT", VcfUtils.getUpdateAltString("CATTTGT", "CA", "C"));
        assertEquals("CTTGT", VcfUtils.getUpdateAltString("CATTTGT", "CAT", "C"));
    }

    @Test
    public void isRecordAMnp() {

        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", null});
        //		VcfUtils.createVcfRecord("1", 1, "A");
        assertFalse(VcfUtils.isRecordAMnp(rec));

        rec = VcfUtils.resetAllele(rec, "A");
        assertFalse(VcfUtils.isRecordAMnp(rec));

        rec = new VcfRecord(new String[]{"1", "1", ".", "AC", null});
        //VcfUtils.createVcfRecord("1", 1, "AC");
        rec = VcfUtils.resetAllele(rec, "A");
        assertFalse(VcfUtils.isRecordAMnp(rec));

        rec = VcfUtils.resetAllele(rec, "ACG");
        assertFalse(VcfUtils.isRecordAMnp(rec));

        rec = new VcfRecord(new String[]{"1", "1", ".", "G", null});
        //VcfUtils.createVcfRecord("1", 1, "G");
        rec = VcfUtils.resetAllele(rec, "G");
        assertFalse(VcfUtils.isRecordAMnp(rec));        // ref == alt
        rec = new VcfRecord(new String[]{"1", "1", ".", "CG", null});
        //VcfUtils.createVcfRecord("1", 1, "CG");
        rec = VcfUtils.resetAllele(rec, "GA");
        assertTrue(VcfUtils.isRecordAMnp(rec));

        rec = new VcfRecord(new String[]{"1", "1", ".", "CGTTT", null});
        //VcfUtils.createVcfRecord("1", 1, "CGTTT");
        rec = VcfUtils.resetAllele(rec, "GANNN");
        assertTrue(VcfUtils.isRecordAMnp(rec));
    }

    @Test
    public void isRecordAMnpCheckIndels() {

        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", null});
        //VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
        assertFalse(VcfUtils.isRecordAMnp(rec));

        rec = VcfUtils.resetAllele(rec, "A,AACCACC");
        assertFalse(VcfUtils.isRecordAMnp(rec));
    }

    @Test
    public void isRecordASnpOrMnp() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "A", "G"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "A", "C,G"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "A", "C,G,T"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AC", "CG,GA,TT"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "AC", "TT"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "ACGT", "TGCA"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));
        rec = new VcfRecord(new String[]{"1", "1", ".", "ACGT", "TGCA,VVVV"});
        assertTrue(VcfUtils.isRecordASnpOrMnp(rec));

        /*
         * and now the nays
         */
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "A", "TG"})));
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "AG", "C"})));
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "A", "C,CT"})));
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "A", "CG,T"})));
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "AG", "C,GT"})));
        assertFalse(VcfUtils.isRecordASnpOrMnp(new VcfRecord(new String[]{"1", "1", ".", "AG", "CC,GT,T"})));
    }

    @Test
    public void testAdditionalSampleFF() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", "."});
        //VcfUtils.createVcfRecord("1", 1, "");
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));

        assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));

        // now add another sample with the same ffs
        VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:6,3:9:62:62,0,150"));

        assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));
        assertEquals("1/1:6,3:9:62:62,0,150", rec.getFormatFields().get(2));

        // and now one a sample with some extra info
        VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL:OH", "1/1:6,3:9:62:62,0,150:blah"));
        assertEquals("GT:AD:DP:GQ:OH:PL", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:.:62,0,150", rec.getFormatFields().get(1));
        assertEquals("1/1:6,3:9:62:.:62,0,150", rec.getFormatFields().get(2));
        assertEquals("1/1:6,3:9:62:blah:62,0,150", rec.getFormatFields().get(3));

        // start afresh
        rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", "."});
        //VcfUtils.createVcfRecord("1", 1, "");
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
        VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("AB:DP:OH", "anythinghere:0:blah"));
        assertEquals("GT:AB:AD:DP:GQ:OH:PL", rec.getFormatFields().get(0));
        assertEquals("0/1:.:6,3:9:62:.:62,0,150", rec.getFormatFields().get(1));
        assertEquals("./.:anythinghere:.:0:.:blah:.", rec.getFormatFields().get(2));
    }

    @Test
    public void additionalSamplesRealLife() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "3418618", ".", "G", "A", ".", ".", "FLANK=CCGGCACCTCC;DB;DP=3;FS=0.000;MQ=60.00;MQ0=0;QD=32.74;SOR=2.833;SOMATIC;IN=1,2"
                , "GT:DP:FT:MR:NNS:OABS:AD:GQ:INF"
                , "0/1:20:.:14:14:A3[23]11[24.18];G2[35]4[32]:.:.:."
                , "0/0:7:COVT:.:.:A0[0]2[23.5];G2[36]3[35]:.:.:."
                , "0/0:6:MIN:A3[23]11[24.18];G2[35]4[32]:.:.:SOMATIC"});
        VcfUtils.addAdditionalSamplesToFormatField(rec, Arrays.asList("GT:AD:DP:FT:GQ:INF:MR:NNS:OABS", "1/1:0,3:3:SBIASALT;COVT:9:SOMATIC:2:2:A0[0]2[23.5];G2[36]3[35]"));
        assertEquals(5, rec.getFormatFields().size());

    }

    @Test
    public void additionalSamples() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", "."});
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:GD:AC:MR:NNS", "0/1:A/C:A38[31.42],32[25],C11[27.64],5[36.6]:16:16", "0/1:A/C:A75[31.96],57[29.32],C12[35.25],6[38]:18:16"));
        VcfUtils.addAdditionalSamplesToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL:GD:AC:MR:NNS", "0/1:2,2:4:69:72,0,69:A/C:A101[29.56],51[27.63],C30[30.83],21[37.29],G1[12],0[0]:51:44", ".:.:.:.:.:.:A191[31.2],147[27.37],C70[30.29],92[37.47],T0[0],1[37]:162:101"));
        assertEquals(5, rec.getFormatFields().size());

    }

    @Test
    public void testAdditionalSampleFFRealLifeData() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:0,22:22:75:1124,75,0"));
        VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:GQ:PL", "1/1:6:86,6,0"));

        assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
        assertEquals("1/1:0,22:22:75:1124,75,0", rec.getFormatFields().get(1));
        assertEquals("1/1:.:.:6:86,6,0", rec.getFormatFields().get(2));
    }

    @Test
    public void isPassSingleSample() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:FT", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:ANYTHING_ELSE???"));
        assertFalse(VcfUtils.isRecordAPass(rec));
    }

    @Test
    public void isPassSingleSampleMerged() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        rec.setInfo("IN=1,2");        // tells us its merged
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:PASS"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "0/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS", "1/2:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
    }

    @Test
    public void isPassControlTestMerged() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        rec.setInfo("IN=1,2");        // tells us its merged
        rec.setFormatFields(Arrays.asList("GT:FT", "0/0:MIN", "1/1:MIN", "0/0:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/0:.", "1/1:PASS", "0/0:PASS", "1/1:PASS"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/0:PASS", "1/1:PASS", "0/0:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "1/1:PASS:SOMATIC", "0/0:PASS:.", "1/1:PASS:SOMATIC"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "1/1:COV:.", "0/0:PASS:.", "1/1:COV:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "1/1:COV:.", "0/0:COV:.", "1/1:PASS:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "1/1:PASS:.", "0/0:PASS:.", "1/1:PASS:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "./.:.:.", "0/1:PASS:.", "./.:.:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.", "./.:.:.", "0/1:MIN:.", "./.:.:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:MIUN:.", "./.:.:.", "0/1:PASS:.", "./.:.:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:MIUN:.", "./.:.:.", "0/1:SBIASALT:.", "./.:.:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:.:.", "./.:.:.", "0/1:.:.", "./.:.:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS", "1/1:PASS", "0/1:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS", "1/1:PASS", "./.:NCIG", "./.:NCIG"));
        assertFalse(VcfUtils.isRecordAPass(rec));
    }

    @Test
    public void isPassFirstCallerOnly() {
        /*
         * chr1    9440005 rs28612519      A       G       .       .       FLANK=GGCATGTTGGC;IN=1;DB;VAF=0.5849;GERM=G:13:237:250:0;HOM=2,AGCCAGGCATaTTGGCTCACA;EFF=intergenic_region(MODIFIER||||||||||1) GT:AD:CCC:CCM:DP:EOR:FF:FT:INF:NNS:OABS ./.:.:.:4:.:.:.:COV:.:.:.       1/1:0,3:.:4:3:.:.:COV;SBIASCOV;NNS;MR:SOMATIC:3:G3[39.67]0[0]   ./.:.:.:1:.:.:.:COV:.:.:.       ./.:.:.:1:.:.:.:COV:.:.:.
         * chr1    13049064        rs200246942     G       T       .       .       FLANK=ACATGTTGAAG;IN=1;DB;GERM=T:129:133:262:0;HOM=2,GGGCAACATGgTGAAGCCTGG     GT:AD:CCC:CCM:DP:EOR:FF:FT:INF:NNS:OABS 0/1:42,4:Germline:22:46:G1[]1[]:G6;T3:MR:.:4:G25[35.48]17[40.53];T2[26.5]2[41]  0/0:75,4:ReferenceNoVariant:22:79:G0[]2[]:G10:PASS:.:.:G23[39.48]52[38.87];T0[0]4[41]   ./.:.:.:1:.:.:.:COV:.:.:.       ./.:.:.:1:.:.:.:COV:.:.:.
         */
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "9440005", "rs28612519", "A", "G", ".", ".", "FLANK=GGCATGTTGGC;IN=1;DB;VAF=0.5849;GERM=G:13:237:250:0;HOM=2,AGCCAGGCATaTTGGCTCACA",
                "GT:AD:CCC:CCM:DP:EOR:FF:FT:INF:NNS:OABS",
                "./.:.:.:4:.:.:.:COV:.:.:.",
                "1/1:0,3:.:4:3:.:.:COV;SBIASCOV;NNS;MR:SOMATIC:3:G3[39.67]0[0]",
                "./.:.:.:1:.:.:.:COV:.:.:.",
                "./.:.:.:1:.:.:.:COV:.:.:."});
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec = new VcfRecord(new String[]{"chr1", "13049064", "rs200246942", "G", "T", ".", ".", "FLANK=ACATGTTGAAG;IN=1;DB;GERM=T:129:133:262:0;HOM=2,GGGCAACATGgTGAAGCCTGG",
                "GT:AD:CCC:CCM:DP:EOR:FF:FT:INF:NNS:OABS",
                "0/1:42,4:Germline:22:46:G1[]1[]:G6;T3:PASS:.:4:G25[35.48]17[40.53];T2[26.5]2[41]",
                "0/0:75,4:ReferenceNoVariant:22:79:G0[]2[]:G10:PASS:.:.:G23[39.48]52[38.87];T0[0]4[41]",
                "./.:.:.:1:.:.:.:COV:.:.:.",
                "./.:.:.:1:.:.:.:COV:.:.:."});
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
    }

    @Test
    public void isPassRealLife() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "13302", "rs180734498", "C", "T", ".", ".", "FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                "0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
                "0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                "0/0:.:LOH:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));

        rec = new VcfRecord(new String[]{"chr1", "13418", ".", "G", "A", ".", ".", "FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));

        rec = new VcfRecord(new String[]{"chr1", "13418", ".", "G", "A", ".", ".", "FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:.:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:.:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));

        rec = new VcfRecord(new String[]{"chr1", "13418", ".", "G", "A", ".", ".", "FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:22:54:MIN:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
    }

    @Test
    public void isPassRealLifeOS() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "13302", "rs180734498", "C", "T", ".", ".", "FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                "0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
                "0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                "0/0:.:LOH:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));

        rec = new VcfRecord(new String[]{"chr1", "13418", ".", "G", "A", ".", ".", "FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/0:.:LOH:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));

        rec = new VcfRecord(new String[]{"chr1", "13418", ".", "G", "A", ".", ".", "FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                "0/0:.:Germline:22:54:SBIASALT:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/1:.:LOH:22:159:PASS:.:SOMATIC:6:6:A4[39.5]2[42];G81[38.6]72[37.33]",
                "0/0:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                "0/1:.:LOH:22:159:PASS:.:SOMATIC:6:6:A4[39.5]2[42];G81[38.6]72[37.33]"});
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));
    }

    @Test
    public void isPassFourSamplesCompoundNP() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "AA", "CC"});
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN", "1/1:MIN", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:COV", "1/1:PASS", "./.:COV", "./.:COV"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:MIN", "./.:COV", "./.:COV"));
        rec.setInfo("IN=1");
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        assertFalse(VcfUtils.isRecordAPass(rec));
    }

    @Test
    public void isPassFourSamplesSinlgeNP() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "C"});
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN", "1/1:MIN", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:MIN", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS", "1/1:PASS", "1/1:MIN"));
        assertTrue(VcfUtils.isRecordAPass(rec));                        // germline, so only looking at control columns
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));    // germline, so only looking at control columns
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS", "1/1:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/0:PASS", "0/1:PASS", "0/0:PASS", "0/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/0:PASS", "0/1:PASS", "0/0:COV", "0/1:PASS"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:PASS:.", "0/1:COV:SOMATIC", "0/0:PASS:.", "0/1:PASS:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:PASS:.", "0/1:COV:SOMATIC", "0/0:PASS:.", "0/1:PASS:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:PASS:.", "0/1:COV:.", "0/0:PASS:.", "0/1:PASS:SOMATIC"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:PASS:.", "0/1:PASS:.", "0/0:PASS:.", "0/1:COV:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:PASS:.", "0/1:COV:.", "0/0:PASS:.", "0/1:COV:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/0:COV:.", "0/1:PASS:.", "0/0:PASS:.", "0/1:PASS:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_TWO_SAMPLE));
    }

    @Test
    public void isPassTwoSamples() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:."));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:."));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));        // germline and so only control need to be PASS
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));    // both callers need to be PASS
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:PASS"));
        assertFalse(VcfUtils.isRecordAPass(rec));
        assertFalse(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertFalse(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "0/1:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS", "1/2:PASS"));
        assertTrue(VcfUtils.isRecordAPass(rec));
        assertTrue(VcfUtils.isRecordAPass(rec, ONE_CALLER_TWO_SAMPLE));
        assertTrue(VcfUtils.isRecordAPass(rec, TWO_CALLER_ONE_SAMPLE));
    }

    @Test
    public void keepGatkGQAndQualScores() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        rec.setQualString("100");
        rec.setFormatFields(Arrays.asList("GT:FT:GQ:PL", "1/1:MIN:40:1,2,3"));
        rec.appendInfo("AN=1");
        rec.appendInfo("AC=1.000");
        rec.appendInfo("AF=1.000");
        rec.appendInfo("MLEAF=2.000");
        rec.appendInfo("MLEAC=3.000");

        /*
         * should remove PL and all the info fields
         */
        VcfUtils.prepareGATKVcfForMerge(rec);
        assertEquals(".", rec.getInfo());
        assertEquals(".", rec.getQualString());
        Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(rec.getFormatFieldStrings());
        assertEquals(6, ffMap.size());
        assertFalse(ffMap.containsKey("PL"));
        assertTrue(ffMap.containsKey("DP"));
        assertEquals(".", ffMap.get("DP")[0]);
        assertTrue(ffMap.containsKey("AD"));
        assertEquals(".", ffMap.get("AD")[0]);
        assertTrue(ffMap.containsKey("GT"));
        assertEquals("1/1", ffMap.get("GT")[0]);
        assertTrue(ffMap.containsKey("GQ"));
        assertEquals("40", ffMap.get("GQ")[0]);
        assertTrue(ffMap.containsKey("FT"));
        assertEquals("MIN", ffMap.get("FT")[0]);
        assertTrue(ffMap.containsKey("QL"));
        assertEquals("100", ffMap.get("QL")[0]);
    }

    @Test
    public void samplesPass() {
        assertFalse(VcfUtils.samplesPass(null, null, null, null, null));
        assertFalse(VcfUtils.samplesPass("", "", "", "", ""));
        assertFalse(VcfUtils.samplesPass(".", ".", ".", ".", "."));
    }

    @Test
    public void isPassControlTestOldSkool() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN", "1/1:MIN"));
        assertFalse(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:."));
        assertFalse(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:."));
        assertFalse(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.", "1/1:PASS"));
        assertFalse(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "1/1:PASS"));
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS", "0/1:PASS"));
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));
        rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS", "1/2:PASS"));
        assertTrue(VcfUtils.isRecordAPassOldSkool(rec));
    }

    @Test
    public void getAD() {
        assertEquals(".", VcfUtils.getAD(null, null, null));
        assertEquals(".", VcfUtils.getAD(null, null, ""));
        assertEquals(".", VcfUtils.getAD(null, null, "."));
        assertEquals(".", VcfUtils.getAD(null, null, "asdasfg"));
        assertEquals("20,0", VcfUtils.getAD("A", "G", "A10[10]10[20]"));
        assertEquals("20,0", VcfUtils.getAD("A", "C", "A10[10]10[20]"));
        assertEquals("20,0", VcfUtils.getAD("A", "T", "A10[10]10[20]"));

        assertEquals("20,12", VcfUtils.getAD("A", "T", "A10[10]10[20];T1[]11[]"));
        assertEquals("12,20", VcfUtils.getAD("T", "A", "A10[10]10[20];T1[]11[]"));
        assertEquals("12,0", VcfUtils.getAD("T", "C", "A10[10]10[20];T1[]11[]"));

        assertEquals("12,20", VcfUtils.getAD("T", "A", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
        assertEquals("12,20,55", VcfUtils.getAD("T", "A,C", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
        assertEquals("0,20,55", VcfUtils.getAD("G", "A,C", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
    }

    @Test
    public void getADCompundSnp() {
        assertEquals("20,0", VcfUtils.getAD("AC", "GT", "AC10[10]10[20]"));
        assertEquals("20,7", VcfUtils.getAD("AC", "GT", "AC10[10]10[20];GT4[7]3[6]"));
        assertEquals("7,20", VcfUtils.getAD("GT", "AC", "AC10[10]10[20];GT4[7]3[6]"));
        assertEquals("7,0", VcfUtils.getAD("GT", "AA", "AC10[10]10[20];GT4[7]3[6]"));
        assertEquals("20,0", VcfUtils.getAD("AC", "GG", "AC10[10]10[20];GT4[7]3[6]"));
        assertEquals("0,0", VcfUtils.getAD("GG", "TT", "AC10[10]10[20];GT4[7]3[6]"));
        assertEquals("0,2", VcfUtils.getAD("GG", "TT", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
        assertEquals("0,2,20", VcfUtils.getAD("GG", "TT,AC", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
        assertEquals("0,2,20,7", VcfUtils.getAD("GG", "TT,AC,GT", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
    }

    @Test
    public void add5BPToFormat() {
        VcfRecord rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5"));
        assertEquals("5BP", rec.getFormatFields().get(0));
        assertEquals("5", rec.getFormatFields().get(1));

        rec = new VcfRecord(new String[]{"chr1", "1066816", ".", "A", "."});
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:0,22:22:75:1124,75,0", "1/1:0,22:22:75:1124,75,0"));
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5"));
        assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
        assertEquals("1/1:0,22:22:75:1124,75,0", rec.getFormatFields().get(1));
        /*
         * need to add 2 columns - 1 for each sample
         */
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5", "."));
        assertEquals("GT:AD:DP:GQ:PL:5BP", rec.getFormatFields().get(0));
        assertEquals("1/1:0,22:22:75:1124,75,0:5", rec.getFormatFields().get(1));
        assertEquals("1/1:0,22:22:75:1124,75,0:.", rec.getFormatFields().get(2));
    }

    @Test
    public void addFormatFields() {
        VcfRecord rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", "."});
        //VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));

        List<String> newStuff = new ArrayList<>();
        newStuff.add("GT");
        newStuff.add("blah");

        VcfUtils.addFormatFieldsToVcf(rec, newStuff);

        assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));

        newStuff = new ArrayList<>();
        newStuff.add("QT");
        newStuff.add("blah");

        VcfUtils.addFormatFieldsToVcf(rec, newStuff);

        assertEquals("GT:AD:DP:GQ:PL:QT", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:62,0,150:blah", rec.getFormatFields().get(1));

        // and again
        rec = new VcfRecord(new String[]{"1", "1", ".", "ACCACCACC", "."});
        //VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
        VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));

        newStuff = new ArrayList<>();
        newStuff.add("GT:GD:AC");
        newStuff.add("0/1:A/C:A10[12.5],2[33],C20[1],30[2]");

        VcfUtils.addFormatFieldsToVcf(rec, newStuff);

        assertEquals("GT:AD:DP:GQ:PL:GD:AC", rec.getFormatFields().get(0));
        assertEquals("0/1:6,3:9:62:62,0,150:A/C:A10[12.5],2[33],C20[1],30[2]", rec.getFormatFields().get(1));
    }

    @Test
    public void minWithAM() {
        assertTrue(VcfUtils.mutationInNormal(1, 10, 5, 0));
        assertTrue(VcfUtils.mutationInNormal(1, 20, 5, 0));
        assertFalse(VcfUtils.mutationInNormal(1, 30, 5, 0));
        assertFalse(VcfUtils.mutationInNormal(1, 30, 5, 1));
        assertFalse(VcfUtils.mutationInNormal(1, 30, 5, 2));
        assertFalse(VcfUtils.mutationInNormal(1, 30, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(3, 300, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(3, 100, 3, 2));
        assertFalse(VcfUtils.mutationInNormal(1, 30, 3, 2));
        assertFalse(VcfUtils.mutationInNormal(2, 67, 3, 2));
        assertTrue(VcfUtils.mutationInNormal(2, 66, 3, 2));
        assertTrue(VcfUtils.mutationInNormal(2, 50, 3, 2));
    }

    @Test
    public void getFFAsMap() {
        Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(Arrays.asList("GT:AD:CCC:CCM:DP:FT:GQ:INF:QL", "0/0:.:Reference:14:.:PASS:.:NCIG:.", "1/1:0,120:SomaticNoReference:14:120:PASS:99:SOMATIC:5348.77"));
        assertEquals(9, ffMap.size());
        assertArrayEquals(new String[]{".", "5348.77"}, ffMap.get(VcfHeaderUtils.FORMAT_QL));
    }

    @Test
    public void min() {
        assertFalse(VcfUtils.mutationInNormal(0, 0, 0, 0));
        assertFalse(VcfUtils.mutationInNormal(0, 10, 1, 2));
        assertFalse(VcfUtils.mutationInNormal(1, 10, 1, 2));
        assertTrue(VcfUtils.mutationInNormal(2, 10, 1, 2));
        assertFalse(VcfUtils.mutationInNormal(2, 10, 1, 3));
        assertFalse(VcfUtils.mutationInNormal(2, 10, 25, 2));
        assertFalse(VcfUtils.mutationInNormal(2, 10, 25, 3));
        assertTrue(VcfUtils.mutationInNormal(2, 10, 20, 2));

        assertFalse(VcfUtils.mutationInNormal(1, 24, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(3, 63, 5, 3));        // 3.15 = 0.05 * 63
        assertTrue(VcfUtils.mutationInNormal(4, 79, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(4, 99, 5, 3));        // 4.95 = 0.05 * 99

        assertFalse(VcfUtils.mutationInNormal(1, 30, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(1, 10, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(1, 10, 5, 1));
        assertFalse(VcfUtils.mutationInNormal(2, 10, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(2, 10, 5, 2));
        assertTrue(VcfUtils.mutationInNormal(3, 10, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(4, 10, 5, 3));

        assertFalse(VcfUtils.mutationInNormal(1, 100, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(2, 100, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(3, 100, 5, 3));
        assertFalse(VcfUtils.mutationInNormal(4, 100, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(5, 100, 5, 3));
        assertTrue(VcfUtils.mutationInNormal(6, 100, 5, 3));
    }
}
