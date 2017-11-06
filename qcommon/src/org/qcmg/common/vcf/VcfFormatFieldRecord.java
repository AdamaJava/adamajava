/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.vcf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfFormatFieldRecord {
	
final Map<String, String> map;
	
	/**
	 * 
	 * @param format  Format column string. eg. "GT:GD:AC"
	 * @param sample  one of the Sample column String. eg.  "0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]"
	 */
	public VcfFormatFieldRecord(String format, String sample){
		if (null == format) {
			throw new IllegalArgumentException("format argument passed to VcfFormatFieldRecord ctor can not be null");
		}
		if (null == sample) {
			throw new IllegalArgumentException("sample argument passed to VcfFormatFieldRecord ctor can not be null");
		}
			
		final String[] kk = format.split(Constants.COLON_STRING);	
		final String[] vv= sample.split(Constants.COLON_STRING);
		map = new LinkedHashMap<>(kk.length * 2);
		
		for(int i = 0; i < kk.length; i ++) {
			if ( ! StringUtils.isNullOrEmpty(kk[i])) {
				map.put(kk[i], vv.length > i ? vv[i] : Constants.MISSING_DATA_STRING);
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
		if (null == key) 
			throw new IllegalArgumentException("null key passed to getField");
		
		String value = map.get(key);
		return (null != value && value.isEmpty()) ? Constants.MISSING_DATA_STRING: value;
	}
	
	/**
	 * replace or set new field to foramt and sample column
	 * @param key : new field will append/replace to format column
	 * @param value: new field value will append/replace to sample column
	 */
	public void setField(String key, String value){
		if (StringUtils.isNullOrEmpty(key))  
			throw new IllegalArgumentException("null or empty key passed to getField");
		
		map.put(key, (StringUtils.isNullOrEmpty(value)) ? Constants.MISSING_DATA_STRING : value);
	}	
	
	/**
	 * 
	 * @return Format column String. eg.  GT:GQ:DP:HQ
	 */
	public String getFormatColumnString(){
		return map.keySet().stream().collect(Collectors.joining(Constants.COLON_STRING));
	}
	/**
	 * 
	 * @return true if sampleColumn is null;  or each value is ".", null or  empty string only
	 */
	public boolean isMissingSample(){
		List<String> uniqueValues = map.values().stream().distinct().collect(Collectors.toList());
		return (uniqueValues.size() == 1 &&  (uniqueValues.get(0).equals(Constants.MISSING_DATA_STRING)));
	}

	/**
	 * @return return sample column String followed Format column pattern: eg.  0|0:48:1:51,51
	 */
	public String getSampleColumnString(){
		if (map == null || map.isEmpty()) return null;
		return map.values().stream().collect(Collectors.joining(Constants.COLON_STRING));
	}
	
	@Override
	public String toString(){
		return getFormatColumnString() + "\t" + getSampleColumnString();
	}
}
