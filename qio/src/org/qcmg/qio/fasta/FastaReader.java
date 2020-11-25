/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qio.fasta;

import java.io.File;
import java.io.IOException;

import org.qcmg.qio.record.RecordReader;

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
	public FastaRecord getRecord(String line) {
		String id = line;
		String seq = null;
		try {
			seq = bin.readLine();			
		} catch (IOException e) {
			e.printStackTrace();			 
		}						
		return new FastaRecord(id, seq);
	}
}
