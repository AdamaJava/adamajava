/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler;

public final class QProfilerException extends Exception {
	private static final long serialVersionUID = 6412315267141301843L;

	public QProfilerException(final String identifier) {
		super(Messages.getMessage(identifier));
	}

	public QProfilerException(final String identifier, final String argument) {
		super(Messages.getMessage(identifier, argument));
	}

	public QProfilerException(final String identifier, final String arg1, final String arg2) {
		super(Messages.getMessage(identifier, arg1, arg2));
	}

	public QProfilerException(final String identifier, final String arg1, final String arg2, final String arg3) {
		super(Messages.getMessage(identifier, arg1, arg2, arg3));
	}

	public QProfilerException(final String identifier, final Object[] arguments) {
		super(Messages.getMessage(identifier, arguments));
	}
}
