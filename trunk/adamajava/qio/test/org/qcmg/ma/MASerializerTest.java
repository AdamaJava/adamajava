package org.qcmg.ma;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MASerializerTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public final void before()
    {
    }

    @After
    public final void after()
    {
    }

    @Test
    public final void decodeDefLineNoMappings()
        throws Exception
    {
        String defLine = ">1_8_184_F3";
        MADefLine d = MASerializer.parseDefLine(defLine);

        assertTrue(d.getReadName().equals("1_8_184"));
        assertFalse(d.hasMappings());
    }

    @Test
    public final void decodeDefLineWithMappings()
        throws Exception
    {
        ExpectedException.none();

        String defLine = ">1_8_184_F3,8_-30078837.2:(31.3.0):q4,10_-9547536.2:(27.2.0):q1,18_-46572772.2:(26.2.0):q1,23_16023538.2:(24.2.0):q0";
        MADefLine d = MASerializer.parseDefLine(defLine);

        assertTrue(d.getReadName().equals("1_8_184"));
        assertTrue(d.hasMappings());

        Iterator<MAMapping> iter = d.iterator();

        assertTrue(4 == d.getNumberMappings());

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MAMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("8"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-30078837"));
            assertTrue(31 == mapping.getParameters().getLength());
            assertTrue(3 == mapping.getParameters().getPossibleMismatches());
            assertTrue(0 == mapping.getParameters().getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q4"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MAMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("10"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-9547536"));
            assertTrue(27 == mapping.getParameters().getLength());
            assertTrue(2 == mapping.getParameters().getPossibleMismatches());
            assertTrue(0 == mapping.getParameters().getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q1"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MAMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("18"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-46572772"));
            assertTrue(26 == mapping.getParameters().getLength());
            assertTrue(2 == mapping.getParameters().getPossibleMismatches());
            assertTrue(0 == mapping.getParameters().getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q1"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MAMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("23"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("16023538"));
            assertTrue(24 == mapping.getParameters().getLength());
            assertTrue(2 == mapping.getParameters().getPossibleMismatches());
            assertTrue(0 == mapping.getParameters().getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q0"));
        }

        assertFalse(iter.hasNext());
    }

    @Test
    public final void decodeRecord()
        throws Exception
    {
        ExpectedException.none();

        String defLine = ">1_8_184_F3,8_-30078837.2:(31.3.0):q4,10_-9547536.2:(27.2.0):q1,18_-46572772.2:(26.2.0):q1,23_16023538.2:(24.2.0):q0";
        String sequence = "T1100011201110111121111111111211121.112211122111221";
        MASerializer.parseRecord(defLine, sequence);
    }
}
