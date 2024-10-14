/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.ResourceBundle;

/**
 * Class used to lookup messages from this package's message bundles.
 */
final class Messages {
	
	/** The Constant messages. */
	static final ResourceBundle messages = ResourceBundle
			.getBundle("org.qcmg.motif.messages");

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
	 */
	static String getVersionMessage() {
		return getProgramName() + ", version " + getProgramVersion();
	}

}
