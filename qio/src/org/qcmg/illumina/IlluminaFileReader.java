/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.illumina;

 
import java.io.File;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.record.RecordReader;

public final class IlluminaFileReader extends RecordReader<IlluminaRecord> {
	public static final String HEADER_LINE = "[Header]";
	public static final String DATA_LINE = "[Data]";
	
	public IlluminaFileReader(File file) throws IOException {
		super(file, DEFAULT_BUFFER_SIZE, HEADER_LINE, DEFAULT_CHARSET);
	}
	
	@Override
    public String readHeader(CharSequence headerPrefix ) throws IOException{
    	String nextLine = bin.readLine();
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
	public IlluminaRecord getRecord(String line) throws Exception {
		String[] dataArray = TabTokenizer.tokenize(line);
		
		// raw Illumina data has 32 fields... and the first one is an integer
		if (dataArray.length != 32) throw new Exception("Bad Illumina data format - expecting 32 fields but saw " + dataArray.length);
		
		return new IlluminaRecord(  dataArray );
	}

}
