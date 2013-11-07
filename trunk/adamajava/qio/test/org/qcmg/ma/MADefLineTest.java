package org.qcmg.ma;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MADefLineTest
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
    public final void create()
        throws Exception
    {
        MADefLine defLine = new MADefLine("12_444_3", MADirection.F3);

        assertFalse(defLine.getMappingIterator().hasNext());
        assertFalse(defLine.iterator().hasNext());
        assertTrue(defLine.getReadName().equals("12_444_3"));
        assertTrue(MADirection.F3 == defLine.getDirection());
        assertFalse(defLine.hasMappings());
        assertTrue(0 == defLine.getNumberMappings());
    }

}
