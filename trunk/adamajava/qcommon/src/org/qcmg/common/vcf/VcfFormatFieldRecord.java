package org.qcmg.common.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.qcmg.common.util.Constants;

public class VcfFormatFieldRecord {
//	static final String NULL = "NULL";
	
	//add . if missing value;
	final Map<String,String> field = new LinkedHashMap<>();	
 
//	final String sampleId; //eg. Control, Test
//	String sample = null;
	
 
	
	/**
	 * 
	 * @param format  Format column string. eg. "GT:GD:AC"
	 * @param sample  one of the Sample column String. eg.  "0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]"
	 */
	public VcfFormatFieldRecord(String format, String sample){	
		final String[] keys = format.split(Constants.COLON_STRING);	
		
		for(int i = 0; i < keys.length; i ++)
			field.put(keys[i], Constants.MISSING_DATA_STRING);
	
		final String[] values= sample.split(Constants.COLON_STRING);
		for(int i = 0; i < field.size(); i ++) 
			if(values.length > i ) // && values[i] != null)
				field.put(keys[i], values[i] );
			else
				field.put(keys[i], Constants.MISSING_DATA+"");
	}
	

	public String getfield(String key){
		try{
			return field.get(key);
		}catch(final NullPointerException e){
			return null;
		}
	}
	
	
	/**
	 * 
	 * @return Format column String. eg.  GT:GQ:DP:HQ, it will auto remove ":." at the end of string for missing key values
	 */
	public String getFormatColumnString(){
		String str = "";
		final Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			str += (str != "")? Constants.COLON_STRING : "";
			str += key;
		}
		
		return (str == "")? null: str;
	}
	

	/**
	 * @return return sample column String followed Format column pattern: eg.  0|0:48:1:51,51
	 */
	@Override
	public String toString(){
		
		String sample = "";
		final Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			sample += (sample != "")? Constants.COLON_STRING : "";
			sample += field.get(key);
		}
		
		sample = sample.trim();
		final int last = sample.lastIndexOf(Constants.COLON_STRING);
		if( last >= 0 && sample.substring(last+1).equals(Constants.MISSING_DATA_STRING))
			sample = sample.substring(0, last);
		
		return (sample == "")? null: sample;
	}

 

 
}
