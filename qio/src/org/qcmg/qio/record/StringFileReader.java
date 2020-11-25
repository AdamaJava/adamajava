package org.qcmg.qio.record;

import java.io.File;
import java.io.IOException;
import org.qcmg.common.util.Constants;

public class StringFileReader  extends RecordReader<String> {
	private static final String HEADER_PREFIX = Constants.HASH_STRING;
	
 
	public StringFileReader(File file) throws IOException {
		super(file, HEADER_PREFIX);
	}

	public StringFileReader(File file, CharSequence headerPrefix) throws IOException {
		super(file, headerPrefix);
	}
	
	public StringFileReader(File file, int bufferSize) throws IOException {
		super(file, bufferSize);
	}

	@Override
	/**
	 * return input self even it is null
	 */
	public String getRecord(String line) {
		return line;
	}	
}