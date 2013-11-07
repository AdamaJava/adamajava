/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.dbsnp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;


public final class DbsnpRecordIterator implements Iterator<Dbsnp130Record> {
    private final BufferedReader reader;
    private Dbsnp130Record next;

    public DbsnpRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public Dbsnp130Record next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Dbsnp130Record result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = DbsnpSerializer.nextRecord(reader);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
