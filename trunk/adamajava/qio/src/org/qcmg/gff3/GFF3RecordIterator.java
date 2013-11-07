/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.gff3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class GFF3RecordIterator implements Iterator<GFF3Record> {
    private final BufferedReader reader;
    private GFF3Record next;

    public GFF3RecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public GFF3Record next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        GFF3Record result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = GFF3Serializer.nextRecord(reader);
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
