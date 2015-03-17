/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

import org.qcmg.common.util.FileUtils;
import org.qcmg.vcf.VCFSerializer;

import net.sf.samtools.BAMIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public class ReadPartGZFile {
	
	
	ReadPartGZFile(File input_gzip_file, int no) throws Exception{
		
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

       BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream) );
       int num = 0;
       String line; 

		while( (line = reader.readLine() ) != null){
			if( ++num > no) break;
			System.out.println(line);
		} 

	}

	public static void main(String[] args) {
		try{
			int no = Integer.parseInt(args[1]);
			File input = new File(args[0]);
			ReadPartGZFile vs = new ReadPartGZFile(input, no );	
		}catch(Exception e){
			e.printStackTrace();
			//System.out.println(e.printStackTrace(););
			System.err.println("Usage: java -cp qmule-0.1pre.jar ReadPartGZFile <input GZ file> <line number>");
			
		}
		
	}
}