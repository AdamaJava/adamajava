/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.illumina;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;

public final class IlluminaSerializer {
	private static final String HEADER_LINE = "[Header]";
	private static final String DATA_LINE = "[Data]";
	
	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		// header lines are as follows:
		/*
[Header]
GSGT Version    1.8.4
Processing Date 8/12/2011 8:41 PM
Content         HumanOmni1-Quad_v1-0_H.bpm
Num SNPs        1134514
Total SNPs      1134514
Num Samples     259
Total Samples   260
File    77 of 259
[Data]
SNP Name        Sample ID       Allele1 - Top   Allele2 - Top   GC Score        Sample Name     Sample Group    Sample Index    SNP Index       SNP Aux Allele1 - Forward       Allele2 - Forward       Allele1 - Design        Allele2 - Design        Allele1 - AB    Allele2 - AB    Chr     Position        GT Score        Cluster Sep     SNP     ILMN Strand     Customer Strand Top Genomic Sequence    Theta   R       X       Y
X Raw   Y Raw   B Allele Freq   Log R Ratio
		 */
		
		String line = reader.readLine();
		if (null != line && line.startsWith(HEADER_LINE)) {
			
			// ignore header lines until we hit [DATA]
			line = reader.readLine();
			while ( ! line.startsWith(DATA_LINE)) {
				line = reader.readLine();
			}
			// next line is still header....
			line = reader.readLine();
			line = reader.readLine();
		}
		return line;
	}

	public static IlluminaRecord nextRecord(final BufferedReader reader) throws Exception {
		IlluminaRecord result = null;

		String data = nextNonheaderLine(reader);
		if (null != data ) {
			result = parseRecord(data);
		}

		return result;
	}

	static String[] parseData(final String value) throws Exception {
		String[] dataArray = TabTokenizer.tokenize(value);
		
		// raw Illumina data has 32 fields... and the first one is an integer
		if (dataArray.length != 32) throw new Exception("Bad Illumina data format - expecting 32 fields but saw " + dataArray.length);
		
		return dataArray;
	}
	
	static IlluminaRecord parseRecord(final String record)
			throws Exception {
		return new IlluminaRecord(parseData(record));
	}

}
