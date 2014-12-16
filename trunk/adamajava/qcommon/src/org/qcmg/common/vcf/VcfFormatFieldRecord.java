package org.qcmg.common.vcf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.qcmg.common.util.Constants;

public class VcfFormatFieldRecord {
//	static final String NULL = "NULL";
	
	//add . if missing value;
	final Map<String,String> field = new HashMap<>();	
 
//	final String sampleId; //eg. Control, Test
	String sample = null;
	
	/**
	 * parse line into <key, value> pairs. eg. "CONF;NNS=5" will be parsed to <CONF,NULL>, <NNS,5>
	 * @param line
	 */
	//public VcfFormatFieldRecord(String format, String sample, String id){
	public VcfFormatFieldRecord(String format, String sample){	
		final String[] keys = format.split(Constants.COLON_STRING);	
		
		for(int i = 0; i < keys.length; i ++)
			field.put(keys[i], Constants.MISSING_DATA_STRING);
		
//		sampleId = id;
		this.sample = sample;
		if(sample == null) return;
	
		final String[] values= sample.split(Constants.COLON_STRING);
		for(int i = 0; i < field.size(); i ++) 
			if(values.length > i && values[i] != null)
				field.put(keys[i], values[i] );
		 
	}
	

	
	
	public String getfield(String key){
		return field.get(key);
	}
	
	
	@Override
	public String toString(){
		if(sample != null)
			return sample;
		
		sample = "";
		final Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			sample += (sample != "")? Constants.SEMI_COLON_STRING : "";
			sample += field.get(key);
		}
		
		return sample;
	}

 

 
}
