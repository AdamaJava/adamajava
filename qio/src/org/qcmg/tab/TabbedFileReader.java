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

import org.qcmg.Utils.IOStreamUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.vcf.VCFSerializer;

public final class TabbedFileReader implements Closeable, Iterable<TabbedRecord> {
    private final File file;
    private final InputStream inputStream;
    private final TabbedHeader header;

    public TabbedFileReader(final File file, int bufferSize) throws IOException {
        this.file = file;
        boolean isGzip = FileUtils.isInputGZip( file);
        try(InputStream stream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), bufferSize) : new FileInputStream(file);) {
        	BufferedReader in = new BufferedReader(new InputStreamReader(stream));        
        	header = TabbedSerializer.readHeader(in);       	
        } 
        
        //  create a new stream rather a closed one
        inputStream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), bufferSize) : new FileInputStream(file);          
    }
    
    public TabbedFileReader(final File file) throws IOException {
    	this(file, 65536);
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
