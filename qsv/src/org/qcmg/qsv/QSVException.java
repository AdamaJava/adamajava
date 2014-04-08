/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

public class QSVException extends Exception {
    private static final long serialVersionUID = -7765139841201954800L;

    public QSVException(final String identifier) {
        super(Messages.getMessage(identifier));
    }
    public QSVException(final String identifier, final String argument) {
        super(Messages.getMessage(identifier, argument));
    }
    public QSVException(final String identifier, final String argument1, final String argument2) {
        super(Messages.getMessage(identifier, argument1, argument2));
    }
}
