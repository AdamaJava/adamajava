/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.genesymbol;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class GeneSymbolRecordIterator implements Iterator<GeneSymbolRecord> {
    private final BufferedReader reader;
    private GeneSymbolRecord next;

    public GeneSymbolRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public GeneSymbolRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        GeneSymbolRecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = GeneSymbolSerializer.nextRecord(reader);
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
