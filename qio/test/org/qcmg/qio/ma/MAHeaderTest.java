package org.qcmg.ma;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class MAHeaderTest
{
    @Before
    public void before()
    {
    }

    @After
    public void after()
    {
    }

    @Test
    public final void create()
        throws Exception
    {
        Vector<String> headerRecords = new Vector<String>();
        headerRecords.add("#firstline");
        headerRecords.add("#secondline");
        headerRecords.add("#thirdline");

        MAHeader header = new MAHeader(headerRecords);

        Iterator<String> iter = header.iterator();

        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#firstline"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#secondline"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#thirdline"));
        assertFalse(iter.hasNext());
    }

}
