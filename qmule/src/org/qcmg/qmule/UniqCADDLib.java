package org.qcmg.qmule;
 

import htsjdk.tribble.readers.TabixReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.log.QLogger;

public class UniqCADDLib {
	
//	static void countUniqPosition(String input_gzip_file,   String output_file) throws IOException{
//		
//
////    	 InputStream inputStream = new GZIPInputStream(new FileInputStream(input_gzip_file));
//    	
//    	try( BufferedReader in = new BufferedReader( new InputStreamReader(new GZIPInputStream(new FileInputStream(input_gzip_file))));
//    			FileWriter writer = new FileWriter(new File(output_file)) ){
////    			GZIPOutputStream output = new GZIPOutputStream( new FileOutputStream(output_file))){
//    		
//        	int num = 0;
//        	 HashSet<String> uniqPos = new HashSet<String>();
//        	 String line; 
//        	 while ((line = in.readLine()) != null){
//        		 if(line.startsWith("#")){   
//        			writer.append(line+"\n");
//        			 continue;
//        		 }
//        		num ++;
//        		String[] eles = line.split("\\t");
//        		
////				String pos = String.format("s% s% s% s%",  eles[0],eles[1],eles[2],  eles[3]);
//        		String pos =   eles[0]  + ":" +eles[1]+ ":" +eles[2]+ ":" +  eles[3] ;
//				//System.out.println(line + "\n" +pos);
//				if(!uniqPos.contains(pos)){
//					uniqPos.add(pos);
//					output.write((line+"\n").getBytes() );
//				}
//        	  }
//        	 System.out.println(String.format("There are  total %d records including %d unique positions in file:\n %s",num,   uniqPos.size(),  input_gzip_file));
//        	 
//    	}
//		
//	}	
	
	static void getUniqPosition(String input_gzip_file,   String output_file) throws IOException{
	
		TabixReader tabix = new TabixReader( input_gzip_file);
		Set<String> chrs = tabix.getChromosomes();
		HashSet<String> uniqVariant = new HashSet<String>();
		long total_uniq = 0; 
		long num = 0;	
		String line;
		System.out.println("total reference number is " + chrs.size() + " from " + input_gzip_file);
		
    	try( FileWriter writer = new FileWriter(new File(output_file)) ){  		
    		//get header line
    		while( (line = tabix.readLine()) != null) 
    			 if(line.startsWith("#"))  writer.append(line+"\n");
    			 else break;
         		 
    		//query each chrome to avoid out off memory
    		for(String str : chrs){
				uniqVariant.clear();
				TabixReader.Iterator it = tabix.query(str);
				 
				while(( line = it.next())!= null){
					String[] eles = line.split("\\t");
					String pos =  eles[1]+ ":" +eles[2]+ ":" +  eles[4] ;
					if(!uniqVariant.contains(pos)){
						uniqVariant.add(pos);
						writer.append(line+"\n");
					} 
					num ++;
				} 
	 			total_uniq += 	uniqVariant.size();
				System.out.println("There are " + uniqVariant.size() + " uniq variants recorded in reference " + str);		
			} 
    	}
		
		System.out.println("Total uniq variants recorded in all reference is " + total_uniq);
		System.out.println("Total records in whole file is " + num);
		
	}
	public static void main(String[] args) {
		try{
  
			 long startTime = System.currentTimeMillis();
			 getUniqPosition(args[0],args[1]);
				 
			  long endTime = System.currentTimeMillis();
			  String time = QLogger.getRunTime(startTime, endTime);	  
			  System.out.println("run Time is " + time);
			
		}catch(Exception e){
			e.printStackTrace();
		 
			System.err.println("Usage: java -cp qmule-0.1pre.jar ReadPartGZFile <input GZ file> <line number>");
			
		}
		
	}
}
