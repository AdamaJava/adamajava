/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.vcf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfFormatFieldRecord {
	
	final List<String> keys;
	final List<String> values ;
	
	/**
	 * create an empty record
	 */
	public VcfFormatFieldRecord( ){
		keys = new ArrayList<>(4);
		values = new ArrayList<>(4);		
	}
	
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
		
		keys = new ArrayList<>(kk.length + 1);
		values = new ArrayList<>(kk.length + 1);
						
		
		for(int i = 0; i < kk.length; i ++) {
			keys.add(kk[i]);
			if(vv.length > i)
				values.add(vv[i]);
			else
				values.add(Constants.MISSING_DATA_STRING);
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
		
		int index = getKeyIndex(key);		
		
		//can't find the key
		if(index == -1) return null;
		
		String value = values.get(index);			
		return (StringUtils.isNullOrEmpty(value))? Constants.MISSING_DATA_STRING: value;
	}
	
	/**
	 * replace or set new field to foramt and sample column
	 * @param key : new field will append/replace to format column
	 * @param value: new field value will append/replace to sample column
	 */
	public void setField(String key, String value){
		setField(-1,   key,   value);
	}	
	
	/**
	 * @param index: add key value to index position, -1 means regardless position
	 * @param key: add/replace this string to format column
	 * @param value: add/replace this string to sample column
	 */
	public void setField(final int index, String key, String value){
		if (StringUtils.isNullOrEmpty(key))  
			throw new IllegalArgumentException("null or empty key passed to getField");
		 
		if(keys.size() != values.size())
			throw new ArrayStoreException("existing format key and value size is not matched!");
				
		value = (StringUtils.isNullOrEmpty(value)) ? Constants.MISSING_DATA_STRING : value; 
		final int order = getKeyIndex( key);
		if(order < 0 && index < 0){
			// add new one to end
			keys.add(key);
			values.add(value);
		}else if(order >= 0 && index >= 0){
			//replace existing one and move to index order
			keys.remove(order);
			values.remove(order);
			keys.add(index, key);
			values.add(index, value);								
		}else if(order < 0 && index >= 0){
			//add new one to index order
			keys.add(index, key);
			values.add(index, value);			
		}else if(order >=0 && index < 0){
			//replace exist pair with new one
			keys.set(order, key);
			values.set(order, value);						
		}
		
	}	
	/**
	 * 
	 * @return Format column String. eg.  GT:GQ:DP:HQ
	 */
	public String getFormatColumnString(){
		return keys.stream().collect(Collectors.joining(Constants.COLON_STRING));
	}
	/**
	 * 
	 * @return true if sampleColumn is null;  or each value is ".", null or  empty string only
	 */
	public boolean isMissingSample(){
				
		if(values == null) return true; 
		
		boolean flag = true;
		for( String v : values) {
			if(!StringUtils.isMissingDtaString(v)){
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * @return return sample column String followed Format column pattern: eg.  0|0:48:1:51,51
	 */
	
	public String getSampleColumnString(){
//		if (values.isEmpty()) return null;
//		return values.stream().collect(Collectors.joining(Constants.COLON_STRING));
		String sample = null;
		for (String value : values) 
			if(StringUtils.isNullOrEmpty(sample))
				sample = value;
			else		 
				sample += Constants.COLON + value;		 
		
		return (StringUtils.isNullOrEmpty(sample)) ? null: sample;
	}
	
	@Override
	public String toString(){
		return getFormatColumnString() + Constants.TAB + getSampleColumnString();
	}
	
	/**
	 * 
	 * @return a list of string with two elements: formatColumnString and sampleColumnString
	 */
	public List<String> toStringList(){		
		List<String> list = new ArrayList<String>();		
		list.add(getFormatColumnString());
		list.add(getSampleColumnString());
		return list; 	
		
	}
	
	/**
	 * 
	 * @param key : format column key
	 * @return the order of exsiting key; return -1 if not exist
	 */
	private int getKeyIndex(String key){
		
		return keys.indexOf(key);
		
		
//		int index = -1;
//		for (int i = 0; i < keys.size(); i ++) {
//			if (keys.get(i).equals(key)){
//				index = i;
//				break;
//			}
//		}
//		return index; 
	}
}
