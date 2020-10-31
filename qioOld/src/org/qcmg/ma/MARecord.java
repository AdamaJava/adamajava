/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ma;

public final class MARecord {
    private final MADefLine defLine;
    private final String readSequence;

    public MARecord(final MADefLine recordDefLine,
            final String recordReadSequence) {
        defLine = recordDefLine;
        readSequence = recordReadSequence;
    }

    public MADefLine getDefLine() {
        return defLine;
    }

    public String getReadSequence() {
        return readSequence;
    }
}
