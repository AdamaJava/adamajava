/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.IOException;

import htsjdk.samtools.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMWriterFactory;


public class ReadsAppend {
	File[] inputs;
	File output;
	 
	
	ReadsAppend(File output, File[] inputs ) throws Exception{
		this.output = output;
		this.inputs =   inputs; 
		merging();
	}
	
	/**
	 * retrieve the CS and CQ value from BAM record to output csfasta or qual file
	 * @throws Exception 
	 */
	void merging() throws Exception {
		System.out.println("start time : " + getTime()); 
		
		List<SamReader> readers = new ArrayList<>();	
		for (File f: inputs) {
			readers.add( SAMFileReaderFactory.createSAMFileReader(f));
		}
		
		SAMFileHeader header = readers.get(0).getFileHeader().clone();			
		SAMWriterFactory factory = new SAMWriterFactory(header, true, output,2000000 );
        try (SAMFileWriter writer = factory.getWriter()) {
    	   for (SamReader reader : readers) {
	        	for (SAMRecord record : reader) {
	        		writer.addAlignment(record);
	        	}
	        	reader.close();
    	   }
        	
        }
    	factory.renameIndex();	//try already closed writer	
		System.out.println("end time : " + getTime());
		System.exit(0);
	}

	
	private String getTime(){
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter=  new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
		return "[" + formatter.format(currentDate.getTime()) + "]";		
	}
	public static void main(final String[] args) throws IOException, InterruptedException {
  
    	try{
    		if (args.length < 2)
    			throw new Exception("missing inputs or outputs name");
    		
    		File output = new File(args[0]);
    		File[] inputs = new File[args.length - 1];
    		for (int i = 1; i < args.length; i++) {
    			inputs[i - 1] = new File(args[i]);
    			System.out.println(inputs[i-1].toString());
    		}
    		new ReadsAppend(output,  inputs );
    		System.exit(0);
    	} catch (Exception e) {
    		System.err.println(e.getMessage());
			e.printStackTrace();
    		Thread.sleep(1);
    		System.out.println("usage: qmule org.qcmg.qmule.ReadsAppend <output SAM/BAM> <input SAMs/BAMs>");
    		System.exit(1);
    	}
		
	}
}
