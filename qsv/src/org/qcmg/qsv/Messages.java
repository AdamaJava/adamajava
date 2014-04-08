/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;


import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Class to parse any messages the program needs to display
 */
public final class Messages {
	static final ResourceBundle messages = ResourceBundle.getBundle("org.qcmg.qsv.messages");
	static final String ERROR_PREFIX = getProgramName() + ": ";
	static final String USAGE = getMessage("USAGE");

	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @return the message
	 */
	public static String getMessage(final String identifier) {
		return messages.getString(identifier);
	}
	
	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @param argument the argument
	 * @return the message
	 */
	static String getMessage(final String identifier, final String argument) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }
	
	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @param argument1 the message argument1
	 * @param argument2 the message argument2
	 * @return the message
	 */
	static String getMessage(final String identifier, final String argument1, final String argument2) {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {argument1, argument2};
        return MessageFormat.format(message, arguments);
    }

	/**
	 * Gets the program name.
	 *
	 * @return the program name
	 */
	static String getProgramName() {
		return Messages.class.getPackage().getImplementationTitle();
	}

	/**
	 * Gets the program version.
	 *
	 * @return the program version
	 */
	static String getProgramVersion() {
		return Messages.class.getPackage().getImplementationVersion();
	}

	/**
	 * Gets the version message.
	 *
	 * @return the version message
	 * @throws Exception the exception
	 */
	public static String getVersionMessage() throws Exception {
		return getProgramName() + ", version " + getProgramVersion();
	}
}
