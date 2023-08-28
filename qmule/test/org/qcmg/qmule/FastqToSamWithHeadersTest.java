package org.qcmg.qmule;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.Test;

import static htsjdk.samtools.SAMUtils.MAX_PHRED_SCORE;
import static org.junit.Assert.*;

public class FastqToSamWithHeadersTest {

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
        FastqRecord fqRec = new FastqRecord("basename 1.2.ACGTTGCA/1", "ACGT", "", "????");
        SAMRecord samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, true, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals("basename", samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertTrue(samRec.getReadPairedFlag());
        assertNull(samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertEquals(" 1.2.ACGTTGCA/1", samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));
    }

    @Test
    public void createSAMRecordAdditionalHeader() {
        FastqRecord fqRec = new FastqRecord("basename 1.2.ACGTTGCA/1 TB:aaaaAAAA+????????", "ACGT", "", "????");
        SAMRecord samRec = FastqToSamWithHeaders.createSamRecord(null, "basename", fqRec, true, "A", FastqQualityFormat.Standard, 0, MAX_PHRED_SCORE);
        assertEquals("basename", samRec.getReadName());
        assertEquals("ACGT", samRec.getReadString());
        assertEquals("????", samRec.getBaseQualityString());
        assertTrue(samRec.getReadPairedFlag());
        assertEquals("aaaaAAAA+????????", samRec.getAttribute(FastqToSamWithHeaders.ZT_ATTRIBUTE));
        assertEquals(" 1.2.ACGTTGCA/1", samRec.getAttribute(FastqToSamWithHeaders.ZH_ATTRIBUTE));
    }
}
