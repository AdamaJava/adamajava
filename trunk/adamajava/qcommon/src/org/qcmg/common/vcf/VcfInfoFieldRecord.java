package org.qcmg.common.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfInfoFieldRecord {
	
	Map<String,String> field = new LinkedHashMap<>();	
	
	/**
	 * parse line into <key, value> pairs. eg. "CONF;NNS=5" will be parsed to <CONF,NULL>, <NNS,5>
	 * @param line
	 * @throws Exception 
	 */
	public VcfInfoFieldRecord(String line) {
		//incase for some testing data
		if (StringUtils.isNullOrEmpty(line)){
			field.clear();
			return;
		}
		
		final String[] infos = line.trim().split(Constants.SEMI_COLON_STRING);			
		for (final String str: infos) {
			final int index = str.indexOf(Constants.EQ_STRING);			 
			if(index > 0) {
				field.put(str.substring(0,index), str.substring(index+1));
			} else if (index == 0) {
				throw new IllegalArgumentException(String.format("missing short key for value %s in INFO string: %s", str, line));
			} else {	// -ve
				field.put(str,Constants.NULL_STRING);
			}
		}
	}
	
	public void setfield(String key, String value){
		field.put(key, value == null ? Constants.NULL_STRING : value);
	}
	
	public String getfield(String key){
		return field.get(key);
	}
	
	public void removefield(String key){
		field.remove(key);
	}
	
	@Override
	public String toString(){
		String str = Constants.EMPTY_STRING;
		
		final Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			if( ! Constants.EMPTY_STRING.equals(str)) {
				str +=  Constants.SEMI_COLON;
			}
			str += key;
			str += (field.get(key) == Constants.NULL_STRING ? Constants.EMPTY_STRING :  Constants.EQ + field.get(key));			
		}
		
		return Constants.EMPTY_STRING.equals(str) ? Constants.MISSING_DATA_STRING : str;
	}
 
}
