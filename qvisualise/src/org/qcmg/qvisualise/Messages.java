/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Messages {
	
    static final ResourceBundle messages = ResourceBundle.getBundle("org.qcmg.qvisualise.messages");
    static final String COLON = ": ";
    static final String ERROR_PREFIX = Messages.getProgramName() + COLON;
    static final String USAGE = Messages.getMessage("USAGE");
    static final String INSUFFICIENT_ARGUMENTS = Messages.getMessage("INSUFFICIENT_ARGUMENTS");
    static final String TOO_MANY_ARGUMENTS = Messages.getMessage("TOO_MANY_ARGUMENTS");
    static final String UNSUPPORTED_FILE_TYPE = Messages.getMessage("UNSUPPORTED_FILE_TYPE");
//   public static final String BAD_READS_DESCRIPTION = Messages.getMessage("BAD_READS_DESCRIPTION");
//   public static final String BAD_QUALS_DESCRIPTION = Messages.getMessage("BAD_QUALS_DESCRIPTION");
//   public static final String LINE_LENGTH_DESCRIPTION = Messages.getMessage("LINE_LENGTH_DESCRIPTION");
//   public static final String TAG_MD_DESCRIPTION = Messages.getMessage("TAG_MD_DESCRIPTION");

    public static String getMessage(final String identifier) {
        return messages.getString(identifier);
    }

    static String getMessage(final String identifier, final String argument) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }

    static String getMessage(final String identifier, final String arg1, final String arg2) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {arg1, arg2};
        return MessageFormat.format(message, arguments);
    }

    static String getMessage(final String identifier, final String arg1, final String arg2, final String arg3) {
    	final String message = Messages.getMessage(identifier);
    	Object[] arguments = {arg1, arg2, arg3};
    	return MessageFormat.format(message, arguments);
    }

    static String getMessage(final String identifier, final Object[] arguments) {
        final String message = Messages.getMessage(identifier);
        return MessageFormat.format(message, arguments);
    }

    static String getProgramName() {
        return Messages.class.getPackage().getImplementationTitle();
    }

    static String getProgramVersion() {
        return Messages.class.getPackage().getImplementationVersion();
    }

    static String getVersionMessage() throws Exception {
    	return getProgramName() + ", version " + getProgramVersion();
    }
}
