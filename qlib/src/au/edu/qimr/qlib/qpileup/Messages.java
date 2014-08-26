/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class Messages {
	static final ResourceBundle messages = ResourceBundle
			.getBundle("au.edu.qimr.qlib.qpileup.messages");
	static final String ERROR_PREFIX = getProgramName() + ": ";
	static final String USAGE = getMessage("USAGE");

	public static String getMessage(final String identifier) {
		return messages.getString(identifier);
	}

	static String getMessage(final String identifier, final String argument) {
		final String message = Messages.getMessage(identifier);
		Object[] arguments = { argument };
		return MessageFormat.format(message, arguments);
	}

	static String getMessage(final String identifier, final String arg1,
			final String arg2) {
		final String message = Messages.getMessage(identifier);
		Object[] arguments = { arg1, arg2 };
		return MessageFormat.format(message, arguments);
	}

	static String getMessage(final String identifier, final String arg1,
			final String arg2, final String arg3) {
		final String message = Messages.getMessage(identifier);
		Object[] arguments = { arg1, arg2, arg3 };
		return MessageFormat.format(message, arguments);
	}

	static String getMessage(final String identifier, final Object[] arguments) {
		final String message = Messages.getMessage(identifier);
		return MessageFormat.format(message, arguments);
	}

	static String getProgramName() {
		return Messages.class.getPackage().getImplementationTitle();
	}
}
