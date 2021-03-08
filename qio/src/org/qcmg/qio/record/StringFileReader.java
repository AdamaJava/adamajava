package org.qcmg.qio.record;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.qcmg.common.util.Constants;

public class StringFileReader  extends RecordReader<String> {
	private static final String HEADER_PREFIX = Constants.HASH_STRING;
	
 
	public StringFileReader(File file) throws IOException {
		super(file, DEFAULT_BUFFER_SIZE, HEADER_PREFIX, DEFAULT_CHARSET); 
	}

	public StringFileReader(File file, CharSequence headerPrefix) throws IOException {
	 
		super(file, DEFAULT_BUFFER_SIZE, headerPrefix, DEFAULT_CHARSET); 
	}
	
	public StringFileReader(File file, int bufferSize) throws IOException {
		super(file, bufferSize, HEADER_PREFIX, DEFAULT_CHARSET); 
	}
	
	public StringFileReader(final File file, int bufferSize, CharSequence headerPrefix, Charset charset) throws IOException {
		super(file, bufferSize, headerPrefix, charset); 
	}	
	

	@Override
	/**
	 * return input self even it is null
	 */
	public String getRecord(String line) {
		return line;
	}	
}