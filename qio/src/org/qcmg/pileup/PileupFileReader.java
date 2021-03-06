/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import org.qcmg.common.util.FileUtils;

@Deprecated
public final class PileupFileReader implements Closeable, Iterable<String> {
//	public final class PileupFileReader implements Closeable, Iterable<PileupRecord> {
    private final File file;
    private final InputStream inputStream;

    public PileupFileReader(final File file) throws Exception {
        this.file = file;
        boolean isGzip = FileUtils.isInputGZip( file);  
        inputStream =  (isGzip) ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);    
        
    	}

    @Override
    public Iterator<String> iterator() {
        return getRecordIterator();
    }
    	

    public PileupRecordIterator getRecordIterator() {
        return new PileupRecordIterator(inputStream);
    	}
    
    	@Override
    public void close() throws IOException {
    		inputStream.close();
    	}

    public File getFile() {
        return file;
    	}
}
