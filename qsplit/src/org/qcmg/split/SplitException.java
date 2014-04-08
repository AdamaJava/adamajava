/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

@SuppressWarnings("serial")
final class SplitException extends Exception {
	SplitException(final String identifier) {
		super(Messages.getMessage(identifier));
	}

	SplitException(final String identifier, final String argument) {
		super(Messages.getMessage(identifier, argument));
	}

	SplitException(final String identifier, final String arg1,
			final String arg2) {
		super(Messages.getMessage(identifier, arg1, arg2));
	}

	SplitException(final String identifier, final String arg1,
			final String arg2, final String arg3) {
		super(Messages.getMessage(identifier, arg1, arg2, arg3));
	}

	SplitException(final String identifier, final Object[] arguments) {
		super(Messages.getMessage(identifier, arguments));
	}
}
