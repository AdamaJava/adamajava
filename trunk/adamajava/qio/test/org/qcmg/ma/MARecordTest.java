package org.qcmg.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MARecordTest
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
    public final void createMARecord()
        throws Exception
    {
        MADefLine defLine = new MADefLine("14_443_3", MADirection.F3);
        MARecord record = new MARecord(defLine, "value");

        assertTrue(defLine == record.getDefLine());
        assertTrue(record.getReadSequence().equals("value"));
    }

}
