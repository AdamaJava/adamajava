/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.chrconv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChrConvRecordIterator implements Iterator<ChromosomeConversionRecord>{

	private final BufferedReader reader;
    private ChromosomeConversionRecord next;

    public ChrConvRecordIterator(final InputStream stream) {
	        InputStreamReader streamReader = new InputStreamReader(stream);
	        reader = new BufferedReader(streamReader);
        	readNext();
	    }

    @Override
    public boolean hasNext() {
        return null != next;
    }

    @Override
    public ChromosomeConversionRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        ChromosomeConversionRecord result = next;
        readNext();
        return result;
    }

    private void readNext() {
        try {
            next = ChrConvSerializer.nextRecord(reader);
        } catch (NoSuchElementException e) {
        	e.printStackTrace();
            throw e;
        } catch (Exception ex) {
        	ex.printStackTrace();
        	next = null;
        	throw new NoSuchElementException(ex.getMessage());
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
