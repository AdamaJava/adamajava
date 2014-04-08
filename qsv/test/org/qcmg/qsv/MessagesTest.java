package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessagesTest {

    @Test
    public void testGetMessageString() {
        String message = Messages.getMessage("LOG_OPTION");
        assertEquals("Name of log file. Will be written to the output directory.", message);
    }

    @Test
    public void testGetVersionMessage() throws Exception {
        String name = Messages.getVersionMessage();
        assertEquals("null, version null", name);
    }

}
