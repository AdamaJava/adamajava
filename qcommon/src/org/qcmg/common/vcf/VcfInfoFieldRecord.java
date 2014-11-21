package org.qcmg.common.vcf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VcfInfoFieldRecord {
	static final String NULL = "NULL";
	
	Map<String,String> field = new HashMap<String,String>();	
	
	public VcfInfoFieldRecord(String line){
		String[] infos = line.trim().split(";");			
		for(String str: infos){
			int index = str.indexOf("=");			 
			if(index >= 0)
				field.put(str.substring(0,index), str.substring(index));
			else
				field.put(str,NULL);			
		}
	}
	
	public void setfield(String key, String value){
		if(value == null)
			field.put(key, NULL);
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
		
		Iterator<String> it = field.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			if(str != "") str += ";";
			str += key;
			str += (field.get(key) == VcfInfoFieldRecord.NULL ? "": "=" + field.get(key));			
		}
		
		return str;
	}

 

 
}
