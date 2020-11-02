/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.qio.pileup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class VerifiedSnpRecordIterator implements Iterator<VerifiedSnpRecord> {
    private final BufferedReader reader;
    private VerifiedSnpRecord next;

    public VerifiedSnpRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public VerifiedSnpRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        VerifiedSnpRecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = VerifiedSnpSerializer.nextRecord(reader);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
