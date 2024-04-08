package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.common.util.SnpUtils;

public class QSnpRecordTest {

    @Test
    public void testAddAnnotation() {
        QSnpRecord rec = new QSnpRecord("chr1", 12345, "ACGT", "TGCA");
        rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
        assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());

        // try adding again
        rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
        assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());

        // and now something else
        rec.getVcfRecord().addFilter(SnpUtils.LESS_THAN_8_READS_TUMOUR);
        assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_TUMOUR, rec.getAnnotation());
    }
}
