package org.qcmg.ma;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class MADirectionTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before()
    {
    }

    @After
    public void after()
    {
    }

    @Test
    public void createDirection()
        throws Exception
    {
        ExpectedException.none();

        MADirection f3 = MADirection.getDirection("F3");
        assertTrue(MADirection.F3 == f3);

        MADirection r3 = MADirection.getDirection("R3");
        assertTrue(MADirection.R3 == r3);

        MADirection f5 = MADirection.getDirection("F5");
        assertTrue(MADirection.F5 == f5);
    }

    @Test
    public void excludeR5Direction()
        throws Exception
    {
        // R5 is not a valid case at this time
        thrown.expect(Exception.class);
        thrown.expectMessage("Unknown direction type: R5");

        MADirection.getDirection("R5");
    }

    @Test
    public void invalidDirection()
        throws Exception
    {
        thrown.expect(Exception.class);
        thrown.expectMessage("Unknown direction type: X6");
        MADirection.getDirection("X6");
    }
}
