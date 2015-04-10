package org.qcmg.qmule;
 

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.log.QLogger;

public class UniqCADDLib {
	
	static void countUniqPosition(String input_gzip_file,   String output_gzip_file) throws IOException{
		

//    	 InputStream inputStream = new GZIPInputStream(new FileInputStream(input_gzip_file));
    	
    	try( BufferedReader in = new BufferedReader( new InputStreamReader(new GZIPInputStream(new FileInputStream(input_gzip_file))));
    			GZIPOutputStream output = new GZIPOutputStream( new FileOutputStream(output_gzip_file))){
    		
        	int num = 0;
        	 HashSet<String> uniqPos = new HashSet<String>();
        	 String line; 
        	 while ((line = in.readLine()) != null){
        		 if(line.startsWith("#")){   
        			 output.write((line+"\n").getBytes() );
        			 continue;
        		 }
        		num ++;
        		String[] eles = line.split("\\t");
        		
//				String pos = String.format("s% s% s% s%",  eles[0],eles[1],eles[2],  eles[3]);
        		String pos =   eles[0]  + ":" +eles[1]+ ":" +eles[2]+ ":" +  eles[3] ;
				//System.out.println(line + "\n" +pos);
				if(!uniqPos.contains(pos)){
					uniqPos.add(pos);
					output.write((line+"\n").getBytes() );
				}
        	  }
        	 System.out.println(String.format("There are  total %d records including %d unique positions in file:\n %s",num,   uniqPos.size(),  input_gzip_file));
        	 
    	}
		
	}	
	
	
	public static void main(String[] args) {
		try{
  
			 long startTime = System.currentTimeMillis();
			 countUniqPosition(args[0],args[1]);
				 
			  long endTime = System.currentTimeMillis();
			  String time = QLogger.getRunTime(startTime, endTime);	  
			  System.out.println("run Time is " + time);
			
		}catch(Exception e){
			e.printStackTrace();
		 
			System.err.println("Usage: java -cp qmule-0.1pre.jar ReadPartGZFile <input GZ file> <line number>");
			
		}
		
	}
}
