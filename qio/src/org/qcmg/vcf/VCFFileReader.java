/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.BufferedInputStream;
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
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;


/**
 * Contains an `InputStream` so remember to call close() or use in try-with-resources
 */
public final class VCFFileReader implements Closeable, Iterable<VcfRecord> {
    private final File file;
    private final InputStream inputStream;
    private VcfHeader header;

    public VCFFileReader(final File file) throws IOException    {
        this.file = file;
        
        boolean isGzip = FileUtils.isInputGZip( file);        
        try(InputStream stream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), 65536) : new FileInputStream(file);) {
	        	BufferedReader in = new BufferedReader(new InputStreamReader(stream));        
	        	header = VCFSerializer.readHeader(in);        	
        }  
        
        //get a new stream rather than a closed one
        inputStream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), 65536) : new FileInputStream(file);  
        
    }
    public VCFFileReader(final String file) throws IOException  {
    		this (new File(file));
    }    
    /**
     * Constructor initialized by an InputStream. {@code markSupported()} for the supplied InputStream must be
     * {@code true}.
     * <p> 
     * The value of {@code file} for the returned instance is {@code null}.
     * 
     * @param instrm
     * @param headerMaxBytes
     * @throws IOException
     */
    public VCFFileReader(final InputStream instrm, Integer headerMaxBytes) throws IOException {
        
        if ( ! instrm.markSupported()) {
            throw new IOException("The supplied InputStream does not support marking");
        }
        instrm.mark(headerMaxBytes);
        BufferedInputStream bis = new BufferedInputStream(instrm);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        header = VCFSerializer.readHeader(br);
        instrm.reset();
        inputStream = instrm;
        file = null;
    }

    /**
     * Uses a default value of 1048576 (1MB) for {@code headerMaxBytes}
     * 
     * @param instrm
     * @throws IOException
     */
    public VCFFileReader(final InputStream instrm) throws IOException {
        this(instrm, 1048576);
    }    
    

    
    @Override
	public Iterator<VcfRecord> iterator() {
        return getRecordIterator();
    }

    public VCFRecordIterator getRecordIterator() {
        return new VCFRecordIterator(inputStream);
    }

    @Override
	public void close() throws IOException {
        inputStream.close();
    }

    public File getFile() {
        return file;
    }

	public VcfHeader getHeader() {
		return header;
	}
}
