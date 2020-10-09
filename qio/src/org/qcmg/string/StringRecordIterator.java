/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.string;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class StringRecordIterator implements Iterator<String> {
    private final BufferedReader reader;
    private String next;
    private final String headerDiscriminator;

    public StringRecordIterator(final InputStream stream, String headerDiscriminator) {
    	this.headerDiscriminator = headerDiscriminator;
        InputStreamReader streamReader = new InputStreamReader(stream);
        reader = new BufferedReader(streamReader);
        readNext();
    }

    public boolean hasNext() {
        return null != next;
    }

    public String next() {
        if (!hasNext()) throw new NoSuchElementException();
    	String result = next;
    	readNext();
        return result;
    }

    private void readNext() {
        try {
    		next = StringSerializer.nextRecord(reader, headerDiscriminator);
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
