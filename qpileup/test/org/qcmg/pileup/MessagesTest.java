package org.qcmg.pileup;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.pileup.Messages;

public class MessagesTest {
	
    @Test
    @Ignore
    //LOG_OPTION identifier is not used anywhere
    public void testGetMessageString() {
        String message = Messages.getMessage("LOG_OPTION");
        assertEquals("File where log output will be directed (must have write permissions)", message);
    }

    @Test
    public void testGetVersionMessage() throws Exception {
        String name = Messages.getVersionMessage();
        assertEquals("null, version null", name);
    }

}
