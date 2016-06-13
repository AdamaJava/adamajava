/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.util.FileUtils;

public final class TabbedFileReader implements Closeable, Iterable<TabbedRecord> {
    private final File file;
    private final InputStream inputStream;
    private final TabbedHeader header;

    public TabbedFileReader(final File file) throws IOException {
        this.file = file;
        
        if (FileUtils.isFileGZip(file)) {
        	
        		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file), 65536);
	        try {
		        	InputStreamReader streamReader = new InputStreamReader(gzis);
		        	BufferedReader in = new BufferedReader(streamReader);
		        	header = TabbedSerializer.readHeader(in);
	        	} finally {
	        		gzis.close();
	        	}
		        
	        	// setup the input stream to read the file contents
	        	inputStream = new GZIPInputStream(new FileInputStream(file), 65536);
        	} else {
	            	FileInputStream stream = new FileInputStream(file);
	        try {
		        	InputStreamReader streamReader = new InputStreamReader(stream);
		        	BufferedReader in = new BufferedReader(streamReader);
		
		        	header = TabbedSerializer.readHeader(in);
	        	} finally {
	        		stream.close();
	        	}
		        
		        inputStream = new FileInputStream(file);
        	}
    }
    
    public TabbedHeader getHeader() {
        return header;
    }

    @Override
    public Iterator<TabbedRecord> iterator() {
        return getRecordIterator();
    }

    public TabbedRecordIterator getRecordIterator() {
        return new TabbedRecordIterator(inputStream);
    }

    @Override
    public void close() throws IOException {
    	inputStream.close();
    }

    public File getFile() {
        return file;
    }
}
