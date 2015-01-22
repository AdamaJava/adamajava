package org.qcmg.common.vcf;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfInfoFieldRecord {
	
//	Map<String,String> field = new LinkedHashMap<>();	
	StringBuilder line;
	
	/**
	 * parse line into <key, value> pairs. eg. "CONF;NNS=5" will be parsed to <CONF,NULL>, <NNS,5>
	 * @param line
	 * @throws Exception 
	 */
	public VcfInfoFieldRecord(String line) {
		//incase for some testing data
//		if (StringUtils.isNullOrEmpty(line)){
//			field.clear();
//			return;
//		}
		if (StringUtils.isNullOrEmpty(line)) {
			throw new IllegalArgumentException("Null or empty string passed to VcfInfoFieldRecord ctor");
		}
		this.line = new StringBuilder(line);
		
//		final String[] infos = line.trim().split(Constants.SEMI_COLON_STRING);			
//		for (final String str: infos) {
//			final int index = str.indexOf(Constants.EQ_STRING);			 
//			if(index > 0) {
//				field.put(str.substring(0,index), str.substring(index+1));
//			} else if (index == 0) {
//				throw new IllegalArgumentException(String.format("missing short key for value %s in INFO string: %s", str, line));
//			} else {	// -ve
//				field.put(str,Constants.NULL_STRING);
//			}
//		}
	}
	
	/**
	 * Replace any entry with this key in the info field
	 * @param key
	 * @param value
	 */
	public void setfield(String key, String value){
		int existingKeyIndex = line.indexOf(key); 
		if (existingKeyIndex == -1) {
			// nothing to replace - have at it
			addField(key, value);
			
		} else {
			// replace existing key (and potentially value)
			// if there is no value, don't need to do anything...
			removefield(key);
			addField(key, value);
		}
		
//		field.put(key, value == null ? Constants.NULL_STRING : value);
	}

	/**
	 * This method will add the supplied key and value to the underlying string buffer with an '=' as the seperator.
	 * No check is performed to ensure that an existing entry with the same key is present.
	 * Please use {@link #setfield(String, String)} if this is the behaviour you want.
	 * 
	 * @param key
	 * @param value
	 */
	public void addField(String key, String value) {
		if (line.length() > 0) {
			line.append(Constants.SEMI_COLON);
		}
		
		line.append(key);
		
		if (null != value) {
			line.append(Constants.EQ).append(value);
		}
	}
	
	public String getField(String key){
		int index = line.indexOf(key);
		
		if (index > -1) {
			// get position of semi colon
			int scIndex = line.indexOf(Constants.SEMI_COLON_STRING, index);
			String kv = line.substring(index, (scIndex == -1 ? line.length() : scIndex) );
			String value = StringUtils.getValueFromKey(kv, key, Constants.EQ);
			
			return value != null ? value : Constants.EMPTY_STRING;
		}
		return null;
//		return field.get(key);
	}
	
	public void removefield(String key){
		int index = line.indexOf(key);
		if (index > -1) {
		
			int scIndex = line.indexOf(Constants.SEMI_COLON_STRING, index);
			String kv = line.substring(index, (scIndex == -1 ? line.length() : scIndex) );
			line.delete(index, (index + kv.length()));
		
		}
//		field.remove(key);
	}
	
	@Override
	public String toString(){
		return line.toString();
//		String str = Constants.EMPTY_STRING;
//		
//		final Iterator<String> it = field.keySet().iterator();
//		while(it.hasNext()){
//			final String key = it.next();
//			if( ! Constants.EMPTY_STRING.equals(str)) {
//				str +=  Constants.SEMI_COLON;
//			}
//			str += key;
//			str += (field.get(key) == Constants.NULL_STRING ? Constants.EMPTY_STRING :  Constants.EQ + field.get(key));			
//		}
//		
//		return Constants.EMPTY_STRING.equals(str) ? Constants.MISSING_DATA_STRING : str;
	}
 
}
