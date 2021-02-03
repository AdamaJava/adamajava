/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.ma;

public final class MaRecord {
    private final MaDefLine defLine;
    private final String readSequence;

    public MaRecord(final MaDefLine recordDefLine,
            final String recordReadSequence) {
        defLine = recordDefLine;
        readSequence = recordReadSequence;
    }

    public MaDefLine getDefLine() {
        return defLine;
    }

    public String getReadSequence() {
        return readSequence;
    }
}
