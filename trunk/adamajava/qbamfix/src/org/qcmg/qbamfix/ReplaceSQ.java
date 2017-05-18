/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

public class ReplaceSQ {
	private boolean UR_updated = false;

	ReplaceSQ(SAMSequenceDictionary dic, String refFile) throws Exception{
		
		HashMap<String, Integer> refmap = getRefMap(refFile);
		for(SAMSequenceRecord seq : dic.getSequences()){
			String name = seq.getSequenceName(); 
			if(!refmap.containsKey(name))
				throw new Exception("the reference " + name + " isn't listed on reference file: " + refFile);
				
			if(refmap.get(name ) != seq.getSequenceLength() ) 
					throw new Exception(
							String.format("the length of reference %s is %d, but the one listed on reference file: %s is %d ",
							name, seq.getSequenceLength(), refFile, refmap.get(name)));
			
			if(seq.getAttribute("UR") == null){
				seq.setAttribute("UR", refFile);
				UR_updated = true;;
			}
			
		}
		 
	}
	public boolean HasNewUR(){
		
		return UR_updated;
	}
	private HashMap<String, Integer> getRefMap(String reffile) throws IOException{
	
		HashMap<String, Integer> refmap = new HashMap<>();		
		String key = null;
		int value = 0; 
		String line;
	
		try (BufferedReader reader = new BufferedReader(new FileReader(reffile))) {
	        while ( ( line = reader.readLine()) != null) {
		        	if(line.startsWith(">")){
		        		//record pre contig to map
		        		if (key != null) {
		        			refmap.put(key, value);
		        		}
		        		//start current contig
		        		key = line.replace(">", "");
		        		value = 0;
		        	} else {
			        	value += line.length();
		        	}
	        }
	       
	        //record the last one
	        if (key != null) {
    				refmap.put(key, value);
	        }
	    }
	
		return refmap;
	}

}
