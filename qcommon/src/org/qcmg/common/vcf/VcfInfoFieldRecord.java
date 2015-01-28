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
	public void setField(String key, String value){
		int existingKeyIndex = line.indexOf(key); 
		if (existingKeyIndex == -1) {
			// nothing to replace - have at it
			addField(key, value);
			
		} else {
			// replace existing key (and potentially value)
			// if there is no value, don't need to do anything...
			removeField(key);
			addField(key, value);
		}
		
//		field.put(key, value == null ? Constants.NULL_STRING : value);
	}

	/**
	 * This method will add the supplied key and value to the underlying string buffer with an '=' as the seperator.
	 * No check is performed to ensure that an existing entry with the same key is present.
	 * Please use {@link #setField(String, String)} if this is the behaviour you want.
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
	
	public void removeField(String key){
		int index = line.indexOf(key);
				if (index > -1) {
		
			int scIndex = line.indexOf(Constants.SEMI_COLON_STRING, index);
			String kv = line.substring(index, (scIndex == -1 ? line.length() : scIndex) );
			
			// check to see if there was a semi colon preceding the key
			int scOffset = 0;
			if ((index - 1) > -1) {
				if (line.charAt(index - 1) == Constants.SEMI_COLON) {
					scOffset = 1;
				}
			}
			int endScOffset = 0;
			if (index == 0 && scIndex > -1) {
				// we are first entry - if scIndex is > -1, add 1 t
				endScOffset = 1;
			}
			
			line.delete(index - scOffset, (index + kv.length() + endScOffset));
		
		}
		

	}
	/**
	 * re-orginize info column string
	 */
	@Override
	public String toString(){
		 
		int index = line.indexOf(Constants.MISSING_DATA_STRING + Constants.SEMI_COLON); 
		if(index < 0)	 
			index =  line.indexOf( Constants.SEMI_COLON  + Constants.MISSING_DATA_STRING);
		
		if(index >= 0)
			line.delete(index, index+2);
		
		if(line.length() == 0)
			return Constants.MISSING_DATA_STRING;
		
		return line.toString();

	}
 
}
