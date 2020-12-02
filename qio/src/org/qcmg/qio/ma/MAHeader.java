/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ma;

import java.util.Iterator;
import java.util.Vector;

public final class MAHeader implements Iterable<String> {
    private final Vector<String> records = new Vector<String>();

    public MAHeader(final Vector<String> headerRecords) {
        for (final String record : headerRecords) {
            records.add(record);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return records.iterator();
    }

}
