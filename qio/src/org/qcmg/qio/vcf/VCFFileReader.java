/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.vcf;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.qio.record.RecordReader;

/**
 * Contains an `InputStream` so remember to call close() or use in try-with-resources
 */
public final class VCFFileReader extends RecordReader<VcfRecord> {

	private final VcfHeader header;
	
	public VCFFileReader(File file, int bufferSize) throws IOException {
		super(file, bufferSize);
		header = new VcfHeader(headerLines);
	}
	
    public VCFFileReader(File file ) throws IOException {
		this(file,1048576);		
	}
    
    public VCFFileReader(final String file) throws IOException  {
		this (new File(file));
}   
    
    
	@Override
	public VcfRecord getRecord(final String line) {
		final String[] params = TabTokenizer.tokenize(line);
		final int arrayLength = params.length; 
		if (8 > arrayLength) {
			throw new IllegalArgumentException("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		
		return new VcfRecord(params);
	}	
	
	
	public VcfHeader getVcfHeader() {
		return header;
	}
 
}
