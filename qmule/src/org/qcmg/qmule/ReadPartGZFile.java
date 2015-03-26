/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.lang.Math;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.util.FileUtils;
import org.qcmg.vcf.VCFSerializer;

import net.sf.samtools.BAMIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public class ReadPartGZFile {
	
	static InputStream getInputStream(File input_gzip_file) throws FileNotFoundException, IOException{
		InputStream inputStream;		
	       if (FileUtils.isFileGZip(input_gzip_file)) {
	        	GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(input_gzip_file));
	        	try(InputStreamReader streamReader = new InputStreamReader(gzis)){
		        	 inputStream = new GZIPInputStream(new FileInputStream(input_gzip_file));
	        	}
	    	} else {
		        FileInputStream stream = new FileInputStream(input_gzip_file);
		        try(InputStreamReader streamReader = new InputStreamReader(stream)){	         
		        	BufferedReader in = new BufferedReader(streamReader);
		        	inputStream = new FileInputStream(input_gzip_file);
		        }
	    	}
		return inputStream;
	}
	
	ReadPartGZFile(File input_gzip_file, int no) throws Exception{
		
	  InputStream inputStream = getInputStream(input_gzip_file);

       try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream) )){
	       int num = 0;
	       String line; 
			while( (line = reader.readLine() ) != null){
				if( ++num > no) break;
				System.out.println(line);
			} 
       }
//       TabixReader tabix=new TabixReader("knownGene.txt.gz");
       
	}
	static void countLines(File input_gzip_file) throws FileNotFoundException, IOException, InterruptedException{
		  long startTime = System.currentTimeMillis();
		  long num = 0;	
		  InputStream inputStream = getInputStream(input_gzip_file);		   
		  try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream) )){			 
				String line;
				while( (line = reader.readLine() ) != null)
					num ++;
		  }
			
		  long endTime = System.currentTimeMillis();
		  String time = QLogger.getRunTime(startTime, endTime);	  
		  System.out.println(String.format("Read file: %s\nLine number: %d\nTime: %s", input_gzip_file.getAbsoluteFile(), num, time));
	  
	}

	public static void main(String[] args) {
		try{
			File input = new File(args[0]);
			int no = Integer.parseInt(args[1]);
			
			if(no > 0)
				new ReadPartGZFile(input, no );	
			else
				countLines(input);
			
		}catch(Exception e){
			e.printStackTrace();
			//System.out.println(e.printStackTrace(););
			System.err.println("Usage: java -cp qmule-0.1pre.jar ReadPartGZFile <input GZ file> <line number>");
			
		}
		
	}
}