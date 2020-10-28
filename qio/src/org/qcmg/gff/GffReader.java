package org.qcmg.gff;

import java.io.File;
import java.io.IOException;

import org.qcmg.record.RecordReader;

public final class GffReader extends RecordReader<GffRecord> {
	public static final String TAB_DELIMITER = "\t";
	
	public GffReader(File file) throws IOException {
		super(file);
	}

	@Override
	public GffRecord readRecord(String line) throws Exception {
		if (null == line) {
			throw new AssertionError("Record was null");
		}

		String[] fields = line.split(TAB_DELIMITER);

		if (fields.length < 8) {
			throw new Exception("Not enough fields in the Record");
		}
		
		return new GffRecord(fields);		
	}
}
 
