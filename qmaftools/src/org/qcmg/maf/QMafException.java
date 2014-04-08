/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

public final class QMafException extends Exception {
	private static final long serialVersionUID = -4575755996356751582L;

	public QMafException(final String identifier) {
		super(Messages.getMessage(identifier));
	}

	public QMafException(final String identifier, final String argument) {
		super(Messages.getMessage(identifier, argument));
	}

	public QMafException(final String identifier, final String arg1, final String arg2) {
		super(Messages.getMessage(identifier, arg1, arg2));
	}

	public QMafException(final String identifier, final String arg1, final String arg2, final String arg3) {
		super(Messages.getMessage(identifier, arg1, arg2, arg3));
	}

	public QMafException(final String identifier, final Object[] arguments) {
		super(Messages.getMessage(identifier, arguments));
	}
}
