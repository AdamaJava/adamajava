package org.qcmg.fasta;

import java.io.File;
import java.io.IOException;
import org.qcmg.record.RecordReader;

public final class FastaReader extends RecordReader<FastaRecord> {
	private static final String ID_PREFIX = ">";
	private static final String HEADER_PREFIX = "#";
		
	public FastaReader(File file) throws IOException {
		super(file, HEADER_PREFIX);
	}

	@Override
	public FastaRecord readRecord(String line) throws Exception {
		String id = line;
		String seq = bin.readLine();
		
		//id start with <
		if ( ! id.startsWith(ID_PREFIX)) {
			throw new Exception("Bad id format: " + id);
		}
		
		//seq should not start with < 
		if (seq.startsWith(ID_PREFIX)) {
			throw new Exception("Bad sequence format: " + seq);
		}
				
		return new FastaRecord(id, seq);
	}
}
