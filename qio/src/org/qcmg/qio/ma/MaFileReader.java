/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qio.ma;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.qcmg.common.util.Constants;
import org.qcmg.qio.record.RecordReader;

public final class MaFileReader extends RecordReader<MaRecord> {
	private static final String HEADER_PREFIX = Constants.HASH_STRING; 
	private static final Pattern mappingPattern = Pattern.compile("[_.:()]+");
	
	public MaFileReader(File file) throws IOException {
		super(file, HEADER_PREFIX);
	}

	@Override
	/**
	 * it has to read two line to construct one record
	 */
	public MaRecord getRecord(String line) {
				
		String defLine = line;
		
		//read sequence
		try {
			String sequence = bin.readLine();
			return  new MaRecord(parseDefLine(defLine), sequence);    
		} catch (IOException e) {
			e.printStackTrace();
			return null; 
		}
	}
	
    MaDefLine parseDefLine(final String value) {
        if (!value.startsWith(">")) {
        	throw new IllegalArgumentException("Missing \">\" prefix for defLine: " + value);
        }

        String rawValue = value.substring(1);

        String[] params = rawValue.split(Constants.COMMA_STRING);
        		//commaDelimitedPattern.split(rawValue);
        if (1 > params.length) {
        	throw new IllegalArgumentException("Bad defLine format: " + rawValue);
        }

        String key = params[0];
        String[] indices = key.split("_");  
        if (4 != indices.length) {
        	throw new IllegalArgumentException("Bad defLine ID: " + key);
        }

        String panel = indices[0];
        String x = indices[1];
        String y = indices[2];
        String type = indices[3];

        String readName = panel + "_" + x + "_" + y;
        MaDirection direction = MaDirection.getDirection(type);

        Vector<MaMapping> mappings = new Vector<MaMapping>();
        for (int i = 1; i < params.length; i++) {
            mappings.add(parseMapping(params[i]));
        }

        return new MaDefLine(readName, direction, mappings);
    }	
	
    private MaMapping parseMapping(final String value) {
        String[] params = mappingPattern.split( value );  //value.split("_.:()");

        if (7 != params.length) {
        	throw new IllegalArgumentException("Bad mapping format: " + value);
        }

        int length = Integer.parseInt(params[3].trim());
        int possibleMismatches = Integer.parseInt(params[4].trim());
        int seedStart = Integer.parseInt(params[5].trim());

        String chromosome = params[0].trim();
        String location = params[1].trim();
        int mismatchCount = Integer.parseInt(params[2].trim());
        String quality = params[6].trim();

        return new MaMapping(chromosome, location, mismatchCount, length, possibleMismatches, seedStart, quality);
    }
}




