/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.ma;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class MARecordIterator implements Iterator<MARecord> {
    private final BufferedReader reader;
    private MARecord next;

    public MARecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    @Override
    public boolean hasNext() {
        return null != next;
    }

    @Override
    public MARecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        MARecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = MASerializer.nextRecord(reader);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception ex) {
            next = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
