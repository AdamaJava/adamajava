package org.qcmg.qio.ma;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.qio.ma.MaDefLine;
import org.qcmg.qio.ma.MaDirection;

public class MaDefLineTest
{
    @Test
    public final void create() throws Exception
    {
        MaDefLine defLine = new MaDefLine("12_444_3", MaDirection.F3);

        assertFalse(defLine.getMappingIterator().hasNext());
        assertFalse(defLine.iterator().hasNext());
        assertTrue(defLine.getReadName().equals("12_444_3"));
        assertTrue(MaDirection.F3 == defLine.getDirection());
        assertFalse(defLine.hasMappings());
        assertTrue(0 == defLine.getNumberMappings());
    }

}
