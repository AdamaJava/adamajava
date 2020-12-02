package org.qcmg.qio.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.qio.ma.MaMapping;

public class MAMappingTest
{
    @Test
    public final void createMAMapping() {
       // MaMappingParameters mp = new MAMappingParameters();
        MaMapping mm = new MaMapping("chromo1", "location1", 3, 3,2,4, "qv");

        assertTrue(mm.getChromosome().equals("chromo1"));
        assertTrue(mm.getLocation().equals("location1"));
        assertTrue(3 == mm.getMismatchCount());
    //    assertTrue(mp == mm.getParameters());
        assertTrue(mm.getQuality().equals("qv"));
    }

}
