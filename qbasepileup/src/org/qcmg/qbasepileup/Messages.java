/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class Messages {
	static final ResourceBundle messages = ResourceBundle.getBundle("org.qcmg.qbasepileup.messages");
	static final String ERROR_PREFIX = getProgramName() + ": ";
	static final String USAGE = getMessage("USAGE");

	public static String getMessage(final String identifier) {
		return messages.getString(identifier);
	}
	
	static String getMessage(final String identifier, final String argument) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }
	
	static String getMessage(final String identifier, final String argument1, final String argument2) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {argument1, argument2};
        return MessageFormat.format(message, arguments);
    }

	static String getProgramName() {
		return Messages.class.getPackage().getImplementationTitle();
	}

	static String getProgramVersion() {
		return Messages.class.getPackage().getImplementationVersion();
	}

	public static String getVersionMessage() throws Exception {
		return getProgramName() + ", version " + getProgramVersion();
	}
}
