/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
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
