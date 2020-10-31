/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class TabbedRecordIterator implements Iterator<TabbedRecord> {
    private final BufferedReader reader;
    private TabbedRecord next;

    public TabbedRecordIterator(final InputStream stream) {
	        InputStreamReader streamReader = new InputStreamReader(stream);
	        reader = new BufferedReader(streamReader);
	        readNext();
    	}

    public boolean hasNext() {
        return null != next;
    	}

    public TabbedRecord next() {
        if (!hasNext()) throw new NoSuchElementException();
        
        	TabbedRecord result = next;
        	readNext();
        return result;
    	}

    private void readNext() {
        try {
            		next = TabbedSerializer.nextRecord(reader);
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
