package org.qcmg.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MAMappingTest
{
    @Before
    public final void before()
    {
    }

    @After
    public final void after()
    {
    }

    @Test
    public final void createMAMapping()
        throws Exception
    {
        MAMappingParameters mp = new MAMappingParameters(3,2,4);
        MAMapping mm = new MAMapping("chromo1", "location1", 3, mp, "qv");

        assertTrue(mm.getChromosome().equals("chromo1"));
        assertTrue(mm.getLocation().equals("location1"));
        assertTrue(3 == mm.getMismatchCount());
        assertTrue(mp == mm.getParameters());
        assertTrue(mm.getQuality().equals("qv"));
    }

}
