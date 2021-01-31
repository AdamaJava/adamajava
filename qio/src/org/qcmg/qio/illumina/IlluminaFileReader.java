/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.illumina;

 
import java.io.File;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.RecordReader;

public final class IlluminaFileReader extends RecordReader<IlluminaRecord> {
	public static final String HEADER_LINE = "[Header]";
	public static final String DATA_LINE = "[Data]";
	
	public IlluminaFileReader(File file) throws IOException {
		super(file, DEFAULT_BUFFER_SIZE, HEADER_LINE, DEFAULT_CHARSET);
	}
	
	@Override
    public String readHeaderAndReturnFirstNonHeaderLine(CharSequence headerPrefix ) throws IOException {
    	String nextLine = bin.readLine();
    	
    	//empty file
    	if( nextLine == null ) return null;   	
    	
    	//check the first header line
    	if(headerPrefix == null || !nextLine.startsWith(headerPrefix+"") ) return nextLine;
    	    	
		//reader header, hence file pointer to first line after header
		while (null != nextLine && !nextLine.startsWith(DATA_LINE) ) {				
			headerLines.add(nextLine);
			//reset current read line
			nextLine = bin.readLine();
		} 
		
		//add [Data] into header
		headerLines.add(nextLine);
		// next line is still header....		
		headerLines.add(bin.readLine());
		
		nextLine = bin.readLine();	
		return nextLine;
    }
 
	@Override
	public IlluminaRecord getRecord(String line) {
		String[] dataArray = TabTokenizer.tokenize(line);
		
		// raw Illumina data has 32 fields... and the first one is an integer
		if (dataArray.length != 32) {
			throw new IllegalArgumentException("Bad Illumina data format - expecting 32 fields but saw " + dataArray.length + ":\n " + line);
		}
		
		return new IlluminaRecord(  dataArray );
	}

}
