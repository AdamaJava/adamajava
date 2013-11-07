/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.pileup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class PileupRecordIterator implements Iterator<String> {
//	public final class PileupRecordIterator implements Iterator<PileupRecord> {
    private final BufferedReader reader;
    private String next;
//    private PileupRecord next;

    public PileupRecordIterator(final InputStream stream) {
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String result = next;
        readNext();
        return result;
    }
//    public PileupRecord next() {
//    	if (!hasNext()) {
//    		throw new NoSuchElementException();
//    	}
//    	PileupRecord result = next;
//    	readNext();
//    	return result;
//    }

    private void readNext() {
        try {
            next = PileupSerializer.nextRecord(reader);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
