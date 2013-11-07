/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
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
