/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

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
	private HashMap<String, Integer> getRefMap(String reffile) throws Exception{
	
		HashMap<String, Integer> refmap = new HashMap<String, Integer>();		
		BufferedReader reader = new BufferedReader(new FileReader(reffile));
		String key = null;
		int value = 0; 
		String line = null;
	
		try {	       
	        while ( ( line = reader.readLine()) != null) {
		        	if(line.startsWith(">")){
	        		//record pre contig to map
	        		if(key != null)
	        			refmap.put(key, value);	        		
	        		//start current contig
	        		key = line.replace(">", "");
	        		value = 0;
	        	}else
		        	value += line.length();
	        }
	       
	        //record the last one
	        if(key != null)
    			refmap.put(key, value);	 
	    } finally {
	        reader.close();
	    }
	
		return refmap;
	}

}
