package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class MessagesTest {

    @Test
    public void testGetMessageString() {
    	try {
    		Messages.getMessage("LOG_OPTION");  
        	fail("exception is not throw, but there is no more log option");
    	}catch(Exception e) {}
       
    }

    @Test
    public void testGetVersionMessage() throws Exception {
        String name = Messages.getVersionMessage();
        assertEquals("null, version null", name);
    }

}
