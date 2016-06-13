/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import htsjdk.samtools.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;


public class ReadsAppend {
	File[] inputs;
	File output;
	HashMap<Integer, PrintWriter> outFast = new HashMap<Integer, PrintWriter>();  
	HashMap<Integer, PrintWriter> outQual = new HashMap<Integer, PrintWriter>();
	 
	
	ReadsAppend(File output, File[] inputs ) throws Exception{
		this.output = output;
		this.inputs =   inputs; 
	//	printHeader(null);   	
		
		merging();
	}
	
	/**
	 * retrive the CS and CQ value from BAM record to output csfasta or qual file
	 * @throws Exception 
	 */
	void merging() throws Exception{	
		System.out.println("start time : " + getTime()); 
		//ArrayList<SAMFileReader> readers = new ArrayList<SAMFileReader>();	
		
		
		ArrayList<SamReader> readers = new ArrayList<SamReader>();	
		for (File f: inputs) 
			readers.add( SAMFileReaderFactory.createSAMFileReader(f));
		
		SAMFileHeader header = readers.get(0).getFileHeader().clone();	
		
		SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, true, output,2000000 );
        SAMFileWriter writer = factory.getWriter();
        
        for( SamReader reader : readers){
        	for( SAMRecord record : reader)
        		writer.addAlignment(record);
        	reader.close();
        }
        
        
    	
    	factory.closeWriter();		
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
    		if(args.length < 2)
    			throw new Exception("missing inputs or outputs name");
    		
    		File output = new File(args[0]);
    		File[] inputs = new File[args.length-1];
    		for (int i = 1; i < args.length; i++)
    			inputs[i-1] = new File(args[i])   ;

    		System.out.println(inputs.toString() );
    		
    		ReadsAppend myAppend = new ReadsAppend(output,  inputs );

    		System.exit(0);
    	}catch(Exception e){
    		System.err.println(e.toString());
    		Thread.sleep(1);
    		System.out.println("usage: qmule org.qcmg.qmule.ReadsAppend <output SAM/BAM> <input SAMs/BAMs>");
    		System.exit(1);
    	}
		
	}
}
