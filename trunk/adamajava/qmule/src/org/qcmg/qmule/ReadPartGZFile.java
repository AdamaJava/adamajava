/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import htsjdk.tribble.readers.TabixReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;


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

       
	}
	static void countLines(File input_gzip_file) throws FileNotFoundException, IOException, InterruptedException{
		  HashSet<String> uniqRef = new HashSet();
		  
		  long startTime = System.currentTimeMillis();
		  long num = 0;	
		  InputStream inputStream = getInputStream(input_gzip_file);		   
		  try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream) )){			 
				String line;
				while( (line = reader.readLine() ) != null){
					uniqRef.add(line.split("\\t")[0]);
					num ++;
				}
		  }
		  
		  System.out.println(String.format("Read file: %s\nLine number: %d", input_gzip_file.getAbsoluteFile(), num));	 
		  System.out.println("Uniq reference name are " + uniqRef );

	  
	}
	
	static void countUniqPosition(String input_gzip_file, String indexFile) throws IOException{
	//       TabixReader tabix = new TabixReader( input_gzip_file, indexFile);
		TabixReader tabix = new TabixReader( input_gzip_file);
		Set<String> chrs = tabix.getChromosomes();
		HashSet<String> uniqPos = new HashSet<String>();
		long total_uniq = 0; 
		long num = 0;	
		System.out.println("total reference number is " + chrs.size() + " from " + input_gzip_file);
		for(String str : chrs){
			
			uniqPos.clear();
			TabixReader.Iterator it = tabix.query(str);
			
			
			String line; 
			while(( line = it.next())!= null){
			//	String[] eles = TabTokenizer.tokenize(line, '\t');
			//	uniqPos.add(eles[1]);
			//	uniqPos.add(line.split("\\t")[1]);
				num ++;
			} 
			
			//debug
			System.out.println("There are " + num+ " position recorded in reference "  + str);
			num ++;
			
			
		//	total_uniq += 	uniqPos.size();
		//	System.out.println("There are " + uniqPos.size() + " uniq position recorded in reference " + str);
			
		}
		
//		System.out.println("Total uniq position recorded in all reference is " + total_uniq);
//		System.out.println("Total records in whole file is " + num);
		
	}

	public static void main(String[] args) {
		try{
			long startTime = System.currentTimeMillis();
			File input = new File(args[0]);
			int no = Integer.parseInt(args[1]);
			
			if(no > 0)
				new ReadPartGZFile(input, no );	
			else if (no == 0)
				countUniqPosition(args[0], null);
			else
				countLines(input);
			
			  long endTime = System.currentTimeMillis();
			  String time = QLogger.getRunTime(startTime, endTime);	  
			  System.out.println("run Time is " + time);
			
		}catch(Exception e){
			e.printStackTrace();
			//System.out.println(e.printStackTrace(););
			System.err.println("Usage: java -cp qmule-0.1pre.jar ReadPartGZFile <input GZ file> <line number>");
			
		}
		
	}
}
