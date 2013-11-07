/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.primerdesignsummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.qcmg.record.Serializer;

public final class PrimerDesignRecordSerializer extends Serializer<PrimerDesignRecord> {
	public PrimerDesignRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line);
		if (5 > params.length) {
			throw new Exception("Bad primer design record format: '" + line
					+ "'");
		}
		String encodedPosition = params[2].trim();
		PrimerPosition primerPosition = parsePrimerPosition(encodedPosition);

		PrimerDesignRecord result = new PrimerDesignRecord();
		result.setSnpId(params[0].trim());
		result.setGene(params[1].trim());
		result.setPosition(primerPosition);
		result.setBaseChange(params[3].trim());
		result.setSnpClass(params[4].trim());
		return result;
	}

	public PrimerPosition parsePrimerPosition(String encodedPosition)
			throws Exception {
		String[] positionParams = colonPattern.split(encodedPosition);
		if (2 != positionParams.length) {
			throw new Exception("Bad primer design record position format: '"
					+ encodedPosition + "'");
		}
		String chromosome = positionParams[0].trim();
		String positionRange = positionParams[1].trim();

		String[] positions = hyphenPattern.split(positionRange);
		if (2 != positions.length) {
			throw new Exception("Bad primer design record position format: '"
					+ encodedPosition + "'");
		}
		int start = Integer.parseInt(positions[0]);
		int end = Integer.parseInt(positions[1]);

		PrimerPosition primerPosition = new PrimerPosition();
		primerPosition.setChromosome(chromosome);
		primerPosition.setStart(start);
		primerPosition.setEnd(end);
		return primerPosition;
	}

    public String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(HASH)) {
			line = reader.readLine();
		}
		return line;
	}

	public String serialise(PrimerDesignRecord record) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrimerDesignRecord parseRecord(BufferedReader reader)
			throws Exception {
		String line = nextNonheaderLine(reader);
		PrimerDesignRecord result = null;
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}
}
