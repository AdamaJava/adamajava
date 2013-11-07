/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.dbsnp;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;

public final class DbsnpSerializer {

	public static Dbsnp130Record nextRecord(final BufferedReader reader) throws Exception, IOException {
		Dbsnp130Record result = null;
		String line = reader.readLine();
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static Dbsnp130Record parseRecord(final String line) throws Exception {
		String[] params = TabTokenizer.tokenize(line);
		if (2 > params.length) {
			throw new Exception("Bad dbSNP130 format");
		}
		Dbsnp130Record result = new Dbsnp130Record();
		result.setChromosome(params[0]);
		// convert the 0-based dbSNP format into 1-based, to be consistant with
		// the rest of our tools
		result.setChromosomePosition(Integer.parseInt(params[1]) + 1);
		if (2 < params.length) {
			result.setRefSnp(params[2]);
		}
		if (3 < params.length) {
			result.setStrand(params[3]);
		}
		if (4 < params.length) {
			// occasionally get bits of sequence here - in this case, leave
			// blank
			result.setRefGenome(params[4].length() > 10 ? null : params[4]);
		}
		if (5 < params.length) {
			result.setVariant(params[5]);
		}
		return result;
	}

}
