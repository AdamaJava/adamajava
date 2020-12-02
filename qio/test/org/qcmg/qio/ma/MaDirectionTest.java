package org.qcmg.qio.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.qio.ma.MaDirection;

public final class MaDirectionTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void createDirection()
        throws Exception
    {
        ExpectedException.none();

        MaDirection f3 = MaDirection.getDirection("F3");
        assertTrue(MaDirection.F3 == f3);

        MaDirection r3 = MaDirection.getDirection("R3");
        assertTrue(MaDirection.R3 == r3);

        MaDirection f5 = MaDirection.getDirection("F5");
        assertTrue(MaDirection.F5 == f5);
    }

    @Test
    public void excludeR5Direction()
        throws Exception
    {
        // R5 is not a valid case at this time
        thrown.expect(Exception.class);
        thrown.expectMessage("Unknown direction type: R5");

        MaDirection.getDirection("R5");
    }

    @Test
    public void invalidDirection()
        throws Exception
    {
        thrown.expect(Exception.class);
        thrown.expectMessage("Unknown direction type: X6");
        MaDirection.getDirection("X6");
    }
}
