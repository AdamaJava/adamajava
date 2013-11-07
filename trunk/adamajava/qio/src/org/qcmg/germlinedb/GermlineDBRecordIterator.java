/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.germlinedb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class GermlineDBRecordIterator implements Iterator<GermlineDBRecord> {
    private final BufferedReader reader;
    private GermlineDBRecord next;

    public GermlineDBRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public GermlineDBRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        GermlineDBRecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = GermlineDBSerializer.nextRecord(reader);
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
