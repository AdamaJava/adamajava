package org.qcmg.common.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.qcmg.common.util.Constants;

public class VcfInfoFieldRecord {
//	static final String NULL = "NULL";
	
	Map<String,String> field = new LinkedHashMap<>();	
	
	/**
	 * parse line into <key, value> pairs. eg. "CONF;NNS=5" will be parsed to <CONF,NULL>, <NNS,5>
	 * @param line
	 * @throws Exception 
	 */
	public VcfInfoFieldRecord(String line) throws Exception{
		//incase for some testing data
		if(line == null || line.equals(Constants.EMPTY_STRING)){
			field.clear();
			return;
		}
		
		final String[] infos = line.trim().split(";");			
		for(final String str: infos){
			final int index = str.indexOf("=");			 
			if(index > 0)
				field.put(str.substring(0,index), str.substring(index+1));
			else if(index == 0)
				throw new Exception(String.format("missing short key for value %s in INFO string: %s", str, line));
			else
				field.put(str,Constants.NULL_STRING);			
		}
	}
	
	public void setfield(String key, String value){
		if(value == null)
			field.put(key, Constants.NULL_STRING);
		else
			field.put(key, value);
	}
	
	public String getfield(String key){
		return field.get(key);
	}
	
	public void removefield(String key){
		field.remove(key);
		
	}
	
	@Override
	public String toString(){
		String str = "";
		
		final Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			if(str != "") str +=  Constants.SEMI_COLON_STRING;
			str += key;
			str += (field.get(key) == Constants.NULL_STRING ? "": Constants.EQ + field.get(key));			
		}
		
		if (str == "") return Constants.MISSING_DATA_STRING;
		return str;
	}

 

 
}
