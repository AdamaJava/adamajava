package org.qcmg.common.dcc;


import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.TabTokenizer;

import static org.junit.Assert.*;

public class DccConsequenceTest {

    @Test
    public void testGetMafName() {
        Assert.assertNull(DccConsequence.getMafName(null, null, -1));
        Assert.assertNull(DccConsequence.getMafName("", null, -1));
        Assert.assertNull(DccConsequence.getMafName("", MutationType.INS, -1));
        Assert.assertNull(DccConsequence.getMafName("", MutationType.DEL, -1));
        Assert.assertNull(DccConsequence.getMafName("testing", MutationType.SNP, -1));
        assertEquals("Nonsense_Mutation", DccConsequence.getMafName("stop_gained", MutationType.SNP, -1));
        assertEquals("Nonsense_Mutation", DccConsequence.getMafName("stop_gained", MutationType.INS, -1));
        assertEquals("Nonsense_Mutation", DccConsequence.getMafName("stop_gained", MutationType.DEL, -1));
    }

    @Test
    public void testGetMafNameWrongType() {
        Assert.assertNull(DccConsequence.getMafName("frameshift_variant", MutationType.SNP, -1));

        Assert.assertNull(DccConsequence.getMafName("SYNONYMOUS_CODING", MutationType.INS, -1));
        Assert.assertNull(DccConsequence.getMafName("SYNONYMOUS_CODING", MutationType.DEL, -1));
        assertEquals("Silent", DccConsequence.getMafName("SYNONYMOUS_CODING", MutationType.SNP, -1));
    }

    @Test
    public void testGetMafNameWithMutationType() {
        Assert.assertNull(DccConsequence.getMafName("frameshift_variant", MutationType.INS, 1));
        Assert.assertNull(DccConsequence.getMafName("frameshift_variant", MutationType.DEL, 1));
        assertEquals("Frame_Shift_Ins", DccConsequence.getMafName("frameshift_variant", MutationType.INS, 2));
        assertEquals("Frame_Shift_Ins", DccConsequence.getMafName("frameshift_variant", MutationType.DEL, 2));
        assertEquals("Frame_Shift_Del", DccConsequence.getMafName("frameshift_variant", MutationType.INS, 3));
        assertEquals("Frame_Shift_Del", DccConsequence.getMafName("frameshift_variant", MutationType.DEL, 3));
        Assert.assertNull(DccConsequence.getMafName("frameshift_variant", MutationType.INS, 4));
        Assert.assertNull(DccConsequence.getMafName("frameshift_variant", MutationType.DEL, 4));
    }

    @Test
    public void testPassesMafNameFilter() {
        assertFalse(DccConsequence.passesMafNameFilter(null));
        assertFalse(DccConsequence.passesMafNameFilter(""));
        assertFalse(DccConsequence.passesMafNameFilter("hello"));

        // from Karin's email - the following must pass
        assertTrue(DccConsequence.passesMafNameFilter("Frame_Shift_Del"));
        assertTrue(DccConsequence.passesMafNameFilter("Frame_Shift_Ins"));
        assertTrue(DccConsequence.passesMafNameFilter("In_Frame_Del"));
        assertTrue(DccConsequence.passesMafNameFilter("In_Frame_Ins"));
        assertTrue(DccConsequence.passesMafNameFilter("Missense_Mutation"));
        assertTrue(DccConsequence.passesMafNameFilter("Nonsense_Mutation"));
        assertTrue(DccConsequence.passesMafNameFilter("Silent"));
        assertTrue(DccConsequence.passesMafNameFilter("Splice_Site"));
        assertTrue(DccConsequence.passesMafNameFilter("Nonstop_Mutation"));

        // remaining ones..
        assertFalse(DccConsequence.passesMafNameFilter("RNA"));
        assertFalse(DccConsequence.passesMafNameFilter("Intron"));
        assertFalse(DccConsequence.passesMafNameFilter("IGR1"));
        assertFalse(DccConsequence.passesMafNameFilter("5'Flank"));
        assertFalse(DccConsequence.passesMafNameFilter("5'UTR"));
        assertFalse(DccConsequence.passesMafNameFilter("3'Flank"));
        assertFalse(DccConsequence.passesMafNameFilter("3'UTR"));
    }

    @Test
    public void testPassesMafNameFilterMultipleNames() {

        // the following must pass
        assertTrue(DccConsequence.passesMafNameFilter("Frame_Shift_Del;RNA"));
        assertTrue(DccConsequence.passesMafNameFilter("RNA;Frame_Shift_Ins"));
        assertTrue(DccConsequence.passesMafNameFilter("RNA;In_Frame_Del;RNA"));
        assertTrue(DccConsequence.passesMafNameFilter("Nonstop_Mutation;Splice_Site"));

        // remaining ones..
        assertFalse(DccConsequence.passesMafNameFilter("RNA;Intron;RNA"));
        assertFalse(DccConsequence.passesMafNameFilter("Intron;IGR1"));
        assertFalse(DccConsequence.passesMafNameFilter("5'UTR;3'UTR"));
        assertFalse(DccConsequence.passesMafNameFilter("3'Flank;3'UTR"));
    }

    @Test
    public void testConsequenceOrder() {

        assertNull(DccConsequence.getWorstCaseConsequence(MutationType.SNP, null));
        assertNull(DccConsequence.getWorstCaseConsequence(MutationType.SNP, ""));
        assertNull(DccConsequence.getWorstCaseConsequence(MutationType.SNP, "test"));
        assertEquals("NON_SYNONYMOUS_CODING", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING"));

        // SPLICE_SITE--STOP_GAINED and STOP_GAINED are the worst - should trump everything
        assertEquals("SPLICE_SITE--STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "SPLICE_SITE--STOP_GAINED"));
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "STOP_GAINED"));
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "STOP_GAINED", "SYNONYMOUS_CODING"));

        //and when they are both in there, the first one in the list should get used
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "STOP_GAINED", "SPLICE_SITE--STOP_GAINED"));

        assertEquals("INTRONIC", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "INTRONIC", "WITHIN_NON_CODING_GENE", "INTRONIC", "WITHIN_NON_CODING_GENE", "INTRONIC", "INTRONIC", "INTRONIC"));
    }

    @Test
    public void testConsequenceOrderWITHIN_NON_CODING_GENE() {

        // new consequence type to be added to list
        // should be above SILENT

        assertEquals("WITHIN_NON_CODING_GENE", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "WITHIN_NON_CODING_GENE"));
        assertEquals("SYNONYMOUS_CODING", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "SYNONYMOUS_CODING"));
        assertEquals("SYNONYMOUS_CODING", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "WITHIN_NON_CODING_GENE", "SYNONYMOUS_CODING"));
        assertEquals("WITHIN_NON_CODING_GENE", DccConsequence.getWorstCaseConsequence(MutationType.INS, "WITHIN_NON_CODING_GENE", "SYNONYMOUS_CODING"));
        assertEquals("NON_SYNONYMOUS_CODING", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "WITHIN_NON_CODING_GENE", "NON_SYNONYMOUS_CODING"));

    }

    @Test
    public void testConsequenceOrder2() {
        String consequences = "upstream_gene_variant,missense_variant,3_prime_UTR_variant,non_coding_exon_variant,downstream_gene_variant";
        String[] consequencesArray = TabTokenizer.tokenize(consequences, ',');

        assertEquals("missense_variant", DccConsequence.getWorstCaseConsequence(MutationType.SNP, consequencesArray));

        // SPLICE_SITE--STOP_GAINED and STOP_GAINED are the worst - should trump everything
        assertEquals("SPLICE_SITE--STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "SPLICE_SITE--STOP_GAINED"));
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "STOP_GAINED"));
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "NON_SYNONYMOUS_CODING", "STOP_GAINED", "SYNONYMOUS_CODING"));

        //and when they are both in there, the first one in the list should get used
        assertEquals("STOP_GAINED", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "STOP_GAINED", "SPLICE_SITE--STOP_GAINED"));

        assertEquals("INTRONIC", DccConsequence.getWorstCaseConsequence(MutationType.SNP, "INTRONIC", "WITHIN_NON_CODING_GENE", "INTRONIC", "WITHIN_NON_CODING_GENE", "INTRONIC", "INTRONIC", "INTRONIC"));
    }

    @Test
    public void testRealLifeData() {
        String consequences = "upstream_gene_variant,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation";
        String[] consequencesArray = TabTokenizer.tokenize(consequences, ',');

        assertEquals("intron_variant", DccConsequence.getWorstCaseConsequence(MutationType.INS, consequencesArray));
        assertEquals("intron_variant", DccConsequence.getWorstCaseConsequence(MutationType.DEL, consequencesArray));
        assertEquals("intron_variant", DccConsequence.getWorstCaseConsequence(MutationType.SNP, consequencesArray));
    }
}
