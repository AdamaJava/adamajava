package org.qcmg.qio.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.qio.ma.MAMappingParameters;

public class MAMappingParametersTest
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
    public final void createMAMappingParameters()
        throws Exception
    {
        MAMappingParameters p = new MAMappingParameters(3,2,4);

        assertTrue(3 == p.getLength());
        assertTrue(2 == p.getPossibleMismatches());
        assertTrue(4 == p.getSeedStart());
    }

}
