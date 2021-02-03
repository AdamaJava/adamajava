package org.qcmg.qio.ma;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.qio.ma.MaDefLine;
import org.qcmg.qio.ma.MaDirection;
import org.qcmg.qio.ma.MaRecord;

public class MaRecordTest
{

    @Test
    public final void createMARecord()
        throws Exception
    {
        MaDefLine defLine = new MaDefLine("14_443_3", MaDirection.F3);
        MaRecord record = new MaRecord(defLine, "value");

        assertTrue(defLine == record.getDefLine());
        assertTrue(record.getReadSequence().equals("value"));
    }
}
