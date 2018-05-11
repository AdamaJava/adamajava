package org.qcmg.common.messages;

import java.text.MessageFormat;
import java.util.ResourceBundle;


public class QMessage {
	
	private final ResourceBundle messages;
	private final Class<?> clazz;
    
    
	public QMessage(Class<?> clazz, ResourceBundle messages){ this.clazz = clazz;	this.messages = messages;  }

	
	public String getUsage(){
		return messages.getString("USAGE");
	}
	
    public String getMessage(final String identifier) {
        return messages.getString(identifier);
    }	
		
    public String getMessage(final String identifier, final String ...argument) {
    	final String message = messages.getString(identifier);
    	Object[] arguments = {argument};
    	return MessageFormat.format(message, arguments);
    }

    public String getProgramName() {
        return this.clazz.getPackage().getImplementationTitle();
    }

    public String getProgramVersion() {
        return this.clazz.getPackage().getImplementationVersion();
    }

    public String getVersionMessage() throws Exception {
    	return getProgramName() + ", version " + getProgramVersion();
    }

}

