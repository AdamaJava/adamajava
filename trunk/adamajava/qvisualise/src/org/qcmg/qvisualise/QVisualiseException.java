/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise;

public final class QVisualiseException extends Exception {

	private static final long serialVersionUID = 7725210877580617098L;

	public QVisualiseException(final String identifier) {
		super(Messages.getMessage(identifier));
	}

	public QVisualiseException(final String identifier, final String argument) {
		super(Messages.getMessage(identifier, argument));
	}

	public QVisualiseException(final String identifier, final String arg1, final String arg2) {
		super(Messages.getMessage(identifier, arg1, arg2));
	}

	public QVisualiseException(final String identifier, final String arg1, final String arg2, final String arg3) {
		super(Messages.getMessage(identifier, arg1, arg2, arg3));
	}

	public QVisualiseException(final String identifier, final Object[] arguments) {
		super(Messages.getMessage(identifier, arguments));
	}
}
