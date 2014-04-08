/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ma;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.Vector;

public final class MASerializer {
    private static final Pattern commaDelimitedPattern = Pattern
            .compile("[,]+");
    private static final Pattern underscoreDelimitedPattern = Pattern
            .compile("[_]+");
    private static final Pattern mappingPattern = Pattern.compile("[_.:()]+");

    public static MAHeader readHeader(final BufferedReader reader)
            throws Exception {
        Vector<String> headerLines = new Vector<String>();
        String line = reader.readLine();
        while (null != line && line.startsWith("#")) {
            headerLines.add(line);
            line = reader.readLine();
        }
        return new MAHeader(headerLines);
    }

    private static String nextNonheaderLine(final BufferedReader reader)
            throws IOException {
        String line = reader.readLine();
        while (null != line && line.startsWith("#")) {
            line = reader.readLine();
        }
        return line;
    }

    public static MARecord nextRecord(final BufferedReader reader)
            throws Exception, IOException {
        MARecord result = null;
        try {
            String defLine = nextNonheaderLine(reader);
            String sequence = reader.readLine();
            if (null != defLine && null != sequence) {
                result = parseRecord(defLine, sequence);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
        return result;
    }

    static MARecord parseRecord(final String defLine, final String sequence)
            throws Exception {
        return new MARecord(parseDefLine(defLine), sequence);
    }

    static MADefLine parseDefLine(final String value) throws Exception {
        if (!value.startsWith(">")) {
            throw new Exception("Missing \">\" prefix for defLine: " + value);
        }

        String rawValue = value.substring(1);

        String[] params = commaDelimitedPattern.split(rawValue);
        if (1 > params.length) {
            throw new Exception("Bad defLine format: " + rawValue);
        }

        String key = params[0];
        String[] indices = underscoreDelimitedPattern.split(key);
        if (4 != indices.length) {
            throw new Exception("Bad defLine ID: " + key);
        }

        String panel = indices[0];
        String x = indices[1];
        String y = indices[2];
        String type = indices[3];

        String readName = panel + "_" + x + "_" + y;
        MADirection direction = MADirection.getDirection(type);

        Vector<MAMapping> mappings = new Vector<MAMapping>();
        for (int i = 1; i < params.length; i++) {
            mappings.add(parseMapping(params[i]));
        }

        return new MADefLine(readName, direction, mappings);
    }

    static MAMapping parseMapping(final String value) throws Exception {
        String[] params = mappingPattern.split(value);

        if (7 != params.length) {
            throw new Exception("Bad mapping format");
        }

        int length = Integer.parseInt(params[3].trim());
        int possibleMismatches = Integer.parseInt(params[4].trim());
        int seedStart = Integer.parseInt(params[5].trim());

        String chromosome = params[0].trim();
        String location = params[1].trim();
        int mismatchCount = Integer.parseInt(params[2].trim());
        MAMappingParameters parameters = new MAMappingParameters(length,
                possibleMismatches, seedStart);
        String quality = params[6].trim();

        return new MAMapping(chromosome, location, mismatchCount, parameters,
                quality);
    }

}
