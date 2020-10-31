/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.string;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;

public final class StringFileReader implements Closeable, Iterable<String> {
    private final File file;
    private final InputStream inputStream;
    private final InputStream fin;
    private final BufferedInputStream in;
    private final StringHeader header;
    private final CharSequence headerDiscriminator;
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    public StringFileReader(final File file, int bufferSize) throws IOException {
    	this(file, bufferSize, Constants.HASH_STRING);
    }
    public StringFileReader(final File file, int bufferSize, CharSequence headerDiscriminator) throws IOException {
        this.file = file;
        this.headerDiscriminator = headerDiscriminator;
        boolean isGzip = FileUtils.isInputGZip( file);
        
        try (InputStream fin = Files.newInputStream(file.toPath());
        	BufferedInputStream in = new BufferedInputStream(fin, DEFAULT_BUFFER_SIZE);
       		InputStream stream = (isGzip) ? new GZIPInputStream(in, bufferSize) : in;
        	BufferedReader bin = new BufferedReader(new InputStreamReader(stream)); ) {
        		
        	header = StringSerializer.readHeader(bin, headerDiscriminator.toString());
        }
        
        //  create a new stream rather a closed one
        fin = Files.newInputStream(file.toPath());
        in = new BufferedInputStream(fin, DEFAULT_BUFFER_SIZE);
        inputStream = (isGzip) ? new GZIPInputStream(in, bufferSize) : in;
    }
    
    public StringFileReader(final File file) throws IOException {
    	this(file, DEFAULT_BUFFER_SIZE);
    }
    
    public StringHeader getHeader() {
        return header;
    }

    @Override
    public Iterator<String> iterator() {
        return getRecordIterator();
    }

    public StringRecordIterator getRecordIterator() {
        return new StringRecordIterator(inputStream, headerDiscriminator.toString());
    }

    @Override
    public void close() throws IOException {
    	try {
    		inputStream.close();
    	} catch (IOException ioe) {
    		// TODO Auto-generated catch block
    		ioe.printStackTrace();
    	} finally {
	    	try {
				in.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
		    	try {
					fin.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    	}
    }

    public File getFile() {
        return file;
    }
}
