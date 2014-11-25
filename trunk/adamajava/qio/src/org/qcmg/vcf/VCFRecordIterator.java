/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.qcmg.common.vcf.VCFRecord;

public final class VCFRecordIterator implements Iterator<VCFRecord> {
    private final BufferedReader reader;
    private VCFRecord next;

    public VCFRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public VCFRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        VCFRecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = VCFSerializer.nextRecord(reader);
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
