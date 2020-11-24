/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.unused;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StringHeader implements Iterable<String> {
    private final List<String> records = new ArrayList<>();

    public StringHeader(final List<String> headerRecords) {
		if (null != headerRecords) {
			records.addAll(headerRecords);
		}
    }

    @Override
    public Iterator<String> iterator() {
        return records.iterator();
    }

}
