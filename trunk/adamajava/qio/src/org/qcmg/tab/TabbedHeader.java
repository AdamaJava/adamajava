/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.tab;

import java.util.Iterator;
import java.util.Vector;

public final class TabbedHeader implements Iterable<String> {
    private final Vector<String> records = new Vector<String>();

    public TabbedHeader(final Vector<String> headerRecords) {
        for (final String record : headerRecords) {
            records.add(record);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return records.iterator();
    }

}
