/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.header.VcfHeader;


public final class VCFFileReader implements Closeable, Iterable<VCFRecord> {
    private final File file;
    private final InputStream inputStream;
    private VcfHeader header;

    public VCFFileReader(final File file) throws Exception {
        this.file = file;
               
        if (FileUtils.isFileGZip(file)) {
	        	GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file));
	        try {
		        	InputStreamReader streamReader = new InputStreamReader(gzis);
		        	BufferedReader in = new BufferedReader(streamReader);
		        	header = VCFSerializer.readHeader(in);
	        	} finally {
	        		gzis.close();
	        	}
	        	
	        	// setup the input stream to read the file contents
	        	inputStream = new GZIPInputStream(new FileInputStream(file));
	    	} else {
		        FileInputStream stream = new FileInputStream(file);
		    try {
		        	InputStreamReader streamReader = new InputStreamReader(stream);
		        	BufferedReader in = new BufferedReader(streamReader);
		        
		        	header = VCFSerializer.readHeader(in);
		        } finally {
		        	stream.close();
		        }
		        
	    		inputStream = new FileInputStream(file);
	    	}
//        
//        FileInputStream fileStream = new FileInputStream(file);
//        inputStream = fileStream;
        
    }

    public VCFFileReader(final String file) throws Exception {
    	this(new File(file));
    }
    
    @Override
	public Iterator<VCFRecord> iterator() {
        return getRecordIterator();
    }

    public VCFRecordIterator getRecordIterator() {
        return new VCFRecordIterator(inputStream);
    }

    @Override
	public void close() throws IOException {
    }

    public File getFile() {
        return file;
    }

	public VcfHeader getHeader() {
		return header;
	}
}
