/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.fasta;

import java.io.File;
import org.qcmg.record.RecordReader;

/**
 * create file reader for fasta file format, each record cross two lines, id and sequence. 
 * @author christix
 *
 */
public final class FastaReader extends RecordReader<FastaRecord> {
	private static final String HEADER_PREFIX = "#";
		
	public FastaReader(File file) throws Exception {
		super(file, HEADER_PREFIX);
	}

	@Override
	/**
	 * it has to read two line to construct one record
	 */
	public FastaRecord getRecord(String line) throws Exception {
		String id = line;
		String seq = bin.readLine();
						
		return new FastaRecord(id, seq);
	}
}
