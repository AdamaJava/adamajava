package org.qcmg.common.vcf;

import java.util.LinkedHashMap;
import java.util.Map;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfFormatFieldRecord {
	
	//add . if missing value;
	final Map<String,String> field = new LinkedHashMap<>(12);	
	
	/**
	 * 
	 * @param format  Format column string. eg. "GT:GD:AC"
	 * @param sample  one of the Sample column String. eg.  "0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]"
	 */
	public VcfFormatFieldRecord(String format, String sample){
		if (null == format) {
			throw new IllegalArgumentException("format argument passed to VcfFormatFieldRecord cstrt can not be null");
		}
		if (null == sample) {
			throw new IllegalArgumentException("sample argument passed to VcfFormatFieldRecord cstrt can not be null");
		}
		final String[] keys = format.split(Constants.COLON_STRING);	
		
		for(int i = 0; i < keys.length; i ++) {
			field.put(keys[i], Constants.MISSING_DATA_STRING);
		}
	
		final String[] values= sample.split(Constants.COLON_STRING);
		for(int i = 0; i < field.size(); i ++) {
			if(values.length > i ) {// && values[i] != null)
				field.put(keys[i], values[i] );
			} else {
				field.put(keys[i], Constants.MISSING_DATA_STRING);
			}
		}
	}
	/**
	 * 
	 * @param key
	 * @return the value of the key; 
	 * @return "." if the key exists but value is null or empty;
	 * @return null if the key is not exists
	 */
	public String getField(String key){
		if (null == key) {
			throw new IllegalArgumentException("null key passed to getField");
		}
		
		if( ! field.containsKey(key))
			return null; 
		
		if(StringUtils.isNullOrEmpty(field.get(key)))
			return Constants.MISSING_DATA_STRING;
		
		return field.get(key);
	}
	
	/**
	 * 
	 * @return Format column String. eg.  GT:GQ:DP:HQ
	 */
	public String getFormatColumnString(){
		String str = "";
		for (String key : field.keySet()) {
			if (str.length() > 0) {
				str += Constants.COLON;
			}
			str += key;
		}
		return (StringUtils.isNullOrEmpty(str)) ? null: str;
	}

	/**
	 * @return return sample column String followed Format column pattern: eg.  0|0:48:1:51,51
	 */
	@Override
	public String toString(){
		String sample = "";
		for (String value : field.values()) {
			if (sample.length() > 0) {
				sample += Constants.COLON;
			}
			sample += value;
		}
		
		return (StringUtils.isNullOrEmpty(sample)) ? null: sample;
	}
}
