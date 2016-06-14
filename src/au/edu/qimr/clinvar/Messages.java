/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package au.edu.qimr.clinvar;

import java.text.MessageFormat;
import java.util.ResourceBundle;

final class Messages {
	
    static final ResourceBundle messages = ResourceBundle.getBundle("au.edu.qimr.clinvar.messages");
    static final String COLON = ": ";
    static final String ERROR_PREFIX = Messages.getProgramName() + COLON;
    static final String USAGE =	Messages.getMessage("USAGE");
    static final String USAGE2 =	Messages.getMessage("USAGE2");

    static String getMessage(final String identifier) {
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
