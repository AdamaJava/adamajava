package org.qcmg.common.util;


import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Genotype;
import org.qcmg.common.model.GenotypeEnum;

import static org.junit.Assert.*;

public class BaseUtilsTest {

    @Test
    public void testGetComplement() {
        assertEquals('A', BaseUtils.getComplement('T'));
        assertEquals('C', BaseUtils.getComplement('G'));
        assertEquals('G', BaseUtils.getComplement('C'));
        assertEquals('T', BaseUtils.getComplement('A'));
        try {
            BaseUtils.getComplement('.');
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            BaseUtils.getComplement('X');
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            BaseUtils.getComplement('Y');
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            BaseUtils.getComplement('Z');
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            BaseUtils.getComplement('\u0000');
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void encodeDistribution() {
        assertEquals(281479271743489L, BaseUtils.encodeDistribution(1, 1, 1, 1));
        assertEquals("1000000000000000100000000000000010000000000000001", Long.toBinaryString(BaseUtils.encodeDistribution(1, 1, 1, 1)));

        assertEquals(562958543486978L, BaseUtils.encodeDistribution((short) 2, (short) 2, (short) 2, (short) 2));
        assertEquals("10000000000000001000000000000000100000000000000010", Long.toBinaryString(BaseUtils.encodeDistribution(2, 2, 2, 2)));
        assertEquals("11000000000000001100000000000000110000000000000011", Long.toBinaryString(BaseUtils.encodeDistribution(3, 3, 3, 3)));

        assertEquals("1111111111111111111111111111111111111111111111111111111111111111", Long.toBinaryString(BaseUtils.encodeDistribution(65535, 65535, 65535, 65535)));
        assertEquals(Long.MIN_VALUE, BaseUtils.encodeDistribution(65536, 65535, 65535, 65535));
        assertEquals(Long.MIN_VALUE, BaseUtils.encodeDistribution(65536, 0, 0, 0));
        assertEquals(Long.MIN_VALUE, BaseUtils.encodeDistribution(0, 0, 0, 123456));
        assertEquals(0, BaseUtils.encodeDistribution(0, 0, 0, 0));
    }

    @Test
    public void decodeDist() {
        Assert.assertArrayEquals(new int[]{1, 1, 1, 1}, BaseUtils.decodeDistribution(281479271743489L).get());
        assertTrue(BaseUtils.decodeDistribution(0).isPresent());
        assertTrue(BaseUtils.decodeDistribution(Long.MAX_VALUE).isPresent());
        assertFalse(BaseUtils.decodeDistribution(Long.MIN_VALUE).isPresent());
        Assert.assertArrayEquals(new int[]{65535, 65535, 65535, 65535}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(65535, 65535, 65535, 65535)).get());

        Assert.assertArrayEquals(new int[]{0, 0, 0, 0}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(0, 0, 0, 0)).get());
        Assert.assertArrayEquals(new int[]{21, 0, 0, 0}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(21, 0, 0, 0)).get());
        Assert.assertArrayEquals(new int[]{0, 0, 3, 0}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(0, 0, 3, 0)).get());
        Assert.assertArrayEquals(new int[]{21, 15, 7, 1}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(21, 15, 7, 1)).get());
        Assert.assertArrayEquals(new int[]{21, 15, 7, 1000}, BaseUtils.decodeDistribution(BaseUtils.encodeDistribution(21, 15, 7, 1000)).get());

    }

    @Test
    public void testAreGenotypesEqual() {
        Assert.assertFalse(BaseUtils.areGenotypesEqual(null, null));
        try {
            BaseUtils.areGenotypesEqual("", "");
            Assert.fail("Should have throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        Assert.assertTrue(BaseUtils.areGenotypesEqual("AA", "AA"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A", "A/A"));
        Assert.assertFalse(BaseUtils.areGenotypesEqual("A/A", "A/C"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("C/A", "A/C"));
        Assert.assertFalse(BaseUtils.areGenotypesEqual("A/T", "A/C"));
        Assert.assertFalse(BaseUtils.areGenotypesEqual("A/T", "G/C"));
        Assert.assertFalse(BaseUtils.areGenotypesEqual("C/G", "A/C"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("T/T", "TT"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A/A", "A/A/A"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("A/C/G", "A/C/G"));
        Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A/A/T", "A/A/A/T"));
    }

    @Test
    public void testGetGenotype() {
        assertNotEquals(new Genotype('A', 'C'), BaseUtils.getGenotype("AG"));
        assertEquals(new Genotype('A', 'C'), BaseUtils.getGenotype("CA"));
        assertNotEquals(new Genotype('G', 'G', 'G'), BaseUtils.getGenotype("GGG"));
    }

    @Test
    public void testGetGenotypeFromVcf() {
        Assert.assertNull(BaseUtils.getGenotypeFromVcf(null, 'C', 'T'));

        // 0/0, 0/1, 1/1 are valid input strings for this method
        assertEquals(new Genotype('C', 'C'), BaseUtils.getGenotypeFromVcf("0/0", 'C', 'T'));
        assertEquals(new Genotype('C', 'T'), BaseUtils.getGenotypeFromVcf("0/1", 'C', 'T'));
        assertEquals(new Genotype('T', 'T'), BaseUtils.getGenotypeFromVcf("1/1", 'C', 'T'));

        assertEquals(new Genotype('A', 'A'), BaseUtils.getGenotypeFromVcf("0/0", 'A', 'A'));
        assertEquals(new Genotype('A', 'A'), BaseUtils.getGenotypeFromVcf("0/1", 'A', 'A'));
        assertEquals(new Genotype('A', 'A'), BaseUtils.getGenotypeFromVcf("1/1", 'A', 'A'));

        assertEquals(new Genotype('A', 'A'), BaseUtils.getGenotypeFromVcf("0/0", 'A', 'G'));
        assertEquals(new Genotype('A', 'G'), BaseUtils.getGenotypeFromVcf("0/1", 'A', 'G'));
        assertEquals(new Genotype('G', 'G'), BaseUtils.getGenotypeFromVcf("1/1", 'A', 'G'));

    }

    @Test
    public void testIsValid() {
        assertFalse(BaseUtils.isACGT('\u0000'));
        assertTrue(BaseUtils.isACGT('A'));
        assertTrue(BaseUtils.isACGT('C'));
        assertTrue(BaseUtils.isACGT('G'));
        assertTrue(BaseUtils.isACGT('T'));
        assertFalse(BaseUtils.isACGT('N'));
        assertFalse(BaseUtils.isACGT('.'));
        assertFalse(BaseUtils.isACGT('X'));
        assertFalse(BaseUtils.isACGT('Y'));
        assertFalse(BaseUtils.isACGT('Z'));
        assertFalse(BaseUtils.isACGT('a'));
        assertFalse(BaseUtils.isACGT('c'));
        assertFalse(BaseUtils.isACGT('g'));
        assertFalse(BaseUtils.isACGT('t'));
    }

    @Test
    public void testIsValidIncludeDotN() {
        assertFalse(BaseUtils.isACGTNDot('\u0000'));
        assertTrue(BaseUtils.isACGTNDot('A'));
        assertTrue(BaseUtils.isACGTNDot('C'));
        assertTrue(BaseUtils.isACGTNDot('G'));
        assertTrue(BaseUtils.isACGTNDot('T'));
        assertTrue(BaseUtils.isACGTNDot('N'));
        assertTrue(BaseUtils.isACGTNDot('.'));
        assertFalse(BaseUtils.isACGTNDot('X'));
        assertFalse(BaseUtils.isACGTNDot('Y'));
        assertFalse(BaseUtils.isACGTNDot('Z'));
        assertFalse(BaseUtils.isACGTNDot('a'));
        assertFalse(BaseUtils.isACGTNDot('c'));
        assertFalse(BaseUtils.isACGTNDot('g'));
        assertFalse(BaseUtils.isACGTNDot('t'));
        assertFalse(BaseUtils.isACGTNDot('n'));
        assertFalse(BaseUtils.isACGTNDot(','));
    }

    @Test
    public void testIsValidIncludeDotNMR() {
        assertFalse(BaseUtils.isACGTNDotMR('\u0000'));
        assertTrue(BaseUtils.isACGTNDotMR('M'));
        assertTrue(BaseUtils.isACGTNDotMR('R'));
        assertFalse(BaseUtils.isACGTNDotMR('Y'));
        assertFalse(BaseUtils.isACGTNDotMR('Z'));
        assertFalse(BaseUtils.isACGTNDotMR('m'));
        assertFalse(BaseUtils.isACGTNDotMR('r'));
    }

    @Test
    public void testGetGenotypeEnum() {
        // null string returns null
        Assert.assertNull(BaseUtils.getGenotypeEnum(null));
        Assert.assertNull(BaseUtils.getGenotypeEnum("X/Y"));
        Assert.assertNull(BaseUtils.getGenotypeEnum("a/c"));

        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("A/A"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("[A/A]"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("[AA]"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("[AA"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("[A/A"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("A/A]"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("AA]"));
        assertEquals(GenotypeEnum.AA, BaseUtils.getGenotypeEnum("AA"));
    }
}
