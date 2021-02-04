/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.vcf;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.qio.record.RecordReader;



/**
 * Contains an `InputStream` so remember to call close() or use in try-with-resources
 */
public final class VcfFileReader extends RecordReader<VcfRecord> {
	private static final String HEADER_PREFIX = Constants.HASH_STRING;
   
    private VcfHeader header;
    
    public VcfFileReader(final String file) throws IOException {
    	this(new File(file));
    }

    public VcfFileReader(final File file) throws IOException {
    	super(file, HEADER_PREFIX);     
    	header = new VcfHeader(getHeader());       
    }

	public VcfHeader getVcfHeader() {
		return header;
	}

	@Override
	public VcfRecord getRecord(String line) {
		final String[] params = TabTokenizer.tokenize(line);
		final int arrayLength = params.length; 
		if (8 > arrayLength) {
			throw new IllegalArgumentException("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		
		return new VcfRecord(params);		
	}
	
}
