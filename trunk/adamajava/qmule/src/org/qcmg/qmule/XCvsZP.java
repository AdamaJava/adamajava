/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.lang.Math;

import net.sf.samtools.BAMIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public class XCvsZP {
	
	
	XCvsZP(File input) throws Exception{
		SAMFileReader reader = new SAMFileReader(input);
		
		HashMap<String, Float> matric = countToMatric( reader );
		 
		ArrayList<String> keys = getKeys(matric );
		printMatric(matric, keys);		
		
		reader.close();
	 
	}
	
	ArrayList<String> getKeys( HashMap<String, Float> matric ){
		Set<String> myset = new HashSet<String>();
 
		Iterator<String> itr = matric.keySet().iterator();
		while( itr.hasNext()){
			String key = itr.next().toString();
			String[] zpxc = key.split("_");
			myset.add(zpxc[0]); 
			myset.add(zpxc[1]);			
		}
		ArrayList<String> mylist = new ArrayList<String>(myset);
		Collections.sort(mylist);
		
		
		return mylist;
	}
	
	
	void printMatric( HashMap<String, Float> matric, ArrayList<String> keys ){
		System.out.print("\t\tZP \t(reads_Number/total_number)\n");
		System.out.print("-------------------------------------------------------------------------------------------------------------------------------------------------\n XC\t|" );
		for(int i = 0; i < keys.size(); i ++)
			System.out.print( "\t  " + keys.get(i) + " ");
		
		for(int i = 0; i < keys.size(); i ++){
			System.out.print( "\n\t|" + keys.get(i) + "|\t");
			for(int j = 0; j < keys.size(); j ++){
				String xc_zp = keys.get(i) + "_" + keys.get(j);
				if(matric.containsKey(xc_zp))
					System.out.print(String.format("%.4f\t", matric.get(xc_zp)) );
				else
					System.out.print("-----\t");					
			}
		}		
	}
	
	
	HashMap<String,Float> countToMatric( SAMFileReader reader) throws Exception{
		
		HashMap<String, Long> matric = new HashMap<String, Long>();
		HashMap<String, Float> rateMatric = new HashMap<String, Float>();
		
		long numRead = 0;
		for( SAMRecord record : reader){
			String xc = record.getAttribute("XC").toString();
			String zp = record.getAttribute("ZP").toString();
			String key = xc + "_" + zp;
 
			long value = 1;
			if( matric.containsKey(key))
				value = matric.get(key) + 1;
			
			matric.put(key, value); 
			numRead ++;			
		}		
		
		System.out.println("Total number of reads is " + numRead + "\n");
		
		//convert to float with %.4f formart
		for(Map.Entry<String, Long> set: matric.entrySet()){
			String key = set.getKey();
			int value = Math.round((set.getValue() * 10000 )/ numRead );
			rateMatric.put(key, ((float) value/10000 ));			
		}
		
		return rateMatric;		
	}
	
 

	public static void main(String[] args) throws Exception{
 
		XCvsZP vs = new XCvsZP(new File(args[0]) );		
		
	}
}