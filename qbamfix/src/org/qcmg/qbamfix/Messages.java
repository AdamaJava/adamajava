/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Class used to lookup messages from this package's message bundles.
 */
final class Messages {
	
	/** The Constant messages. */
	static final ResourceBundle messages = ResourceBundle
			.getBundle("org.qcmg.qbamfix.messages");

	/** The Constant ERROR_PREFIX. */
	static final String ERROR_PREFIX = getProgramName() + ": ";

	/** The Constant USAGE. */
	static final String USAGE = getMessage("USAGE");

	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @return the message
	 */
	static String getMessage(final String identifier) {
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
		Object[] arguments = { argument };
		return MessageFormat.format(message, arguments);
	}

	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @param arg1 the arg1
	 * @param arg2 the arg2
	 * @return the message
	 */
	static String getMessage(final String identifier, final String arg1,
			final String arg2) {
		final String message = Messages.getMessage(identifier);
		Object[] arguments = { arg1, arg2 };
		return MessageFormat.format(message, arguments);
	}

	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @param arg1 the arg1
	 * @param arg2 the arg2
	 * @param arg3 the arg3
	 * @return the message
	 */
	static String getMessage(final String identifier, final String arg1,
			final String arg2, final String arg3) {
		final String message = Messages.getMessage(identifier);
		Object[] arguments = { arg1, arg2, arg3 };
		return MessageFormat.format(message, arguments);
	}

	/**
	 * Gets the message.
	 *
	 * @param identifier the identifier
	 * @param arguments the arguments
	 * @return the message
	 */
	static String getMessage(final String identifier, final Object[] arguments) {
		final String message = Messages.getMessage(identifier);
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
	static String getVersionMessage() throws Exception {
		return getProgramName() + ", version " + getProgramVersion();
	}

	/**
	 * Reconstruct command line.
	 *
	 * @param args the args
	 * @return the string
	 */
	static String reconstructCommandLine(final String[] args) {
		String result = getProgramName() + " ";
		for (final String arg : args) {
			result += arg + " ";
		}
		return result;
	}

}
