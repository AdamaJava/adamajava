package org.qcmg.record;

import java.io.File;
import java.io.IOException;

public class StringFileReader  extends RecordReader<String> {

	public StringFileReader(File file) throws IOException {
		super(file);
	}

	public StringFileReader(File file, CharSequence headerPrefix) throws IOException {
		super(file, headerPrefix);
	}
	
	public StringFileReader(File file, int bufferSize) throws IOException {
		super(file, bufferSize);
	}

	@Override
	public String getRecord(String line) throws Exception {
		return line;
	}	
}