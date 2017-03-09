package org.qcmg.common.vcf.header;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfHeaderRecord implements Comparable<VcfHeaderRecord> {
	
	public static final String ID = "ID";
	public static final String NUMBER = "Number";
	public static final String TYPE = "Type";	
	public static final String DESCRIPTION = "Description";	
	public static final String SOURCE = "Source";
	public static final String FORMAT = "FORMAT";	
	public static final Pattern PATTERN = Pattern.compile("=(\\s*)\"(.*?)\"(\\s*),");
	
	private final String value;
	private final String key;
	private final String id; 
	private final List<Pair<String,String>> pairs; // =  new ArrayList<Pair<String,String>>();
	
	/**
	 * Create a new vcf Header Record
	 * @param line: vcf header line follow ##key=value pattern 
	 */
	public VcfHeaderRecord(String line) {
		if (StringUtils.isNullOrEmpty(line) || ! (line = line.trim()).startsWith("#") ) 
			throw new IllegalArgumentException("input String is null, empty or missing leading \"#\":\n" + line);

		if(line.startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE) ){			
			this.key = line.trim();
			this.value = null; 	
			this.id = null;
			this.pairs = null;
			return;
		}
		
		int index = line.indexOf( Constants.EQ ) ;	
		if (index < 3 || !line.startsWith("##"))
			throw new IllegalArgumentException("can't convert String didn't follow \"##<key>=<value>\" pattern :\n" + line);
	
		this.key =  line.substring(0, index).trim(); 
		if (key.contains(" "))
			throw new IllegalArgumentException("can't convert String into VcfHeaderRecord since contains spacd on key: " + key);
		
		
		String vstr = line.substring(index +1).trim();		
		this.pairs = getPairs(vstr);
		
		Pair<String, String> pid = null; 
		if (pairs != null ){
			vstr = "<";
			for (Pair<String, String> p : pairs){ 
				vstr += p.getLeft() +  ((p.getRight() == null) ? "": "=" + p.getRight() ) + Constants.COMMA;
			}			
			vstr = vstr.substring(0, vstr.length()-1) + ">";
			for (Pair<String, String> p : pairs) {
				if(p.getKey().equals(ID)){ 
					pid = p; break; 
				}
			}
		}		
		this.value = vstr;
		this.id = (pid == null)? null : (String) pid.getRight();				
	}

	/**
	 * Create a new vcf Header Record
	 * @param key: a string can't be null
	 * @param value: a description string,  accept null incase of #Chrom line
	 */
	public VcfHeaderRecord(String key, String value) { 
		this( ( key==null? "" : key )+ Constants.EQ + (value == null? "" : value));
	}
	
	/***
	 *  eg. ##INFO=<ID=id,Number=number,Type=type,Description="description">
	 * @param prefix: ##INFO, ##FORMAT or ##FILTER
	 * @param id
	 * @param number
	 * @param type
	 * @param description: a description string. 
	 */
	public VcfHeaderRecord(String prefix, String id, String number, String type, String description){
		this(prefix + "=<ID=" + id +  
				(number == null ? "" : Constants.COMMA + NUMBER + Constants.EQ + number) +				
				(type == null ? "" :  Constants.COMMA + TYPE + Constants.EQ + type) + 
				Constants.COMMA + DESCRIPTION + Constants.EQ + parseDescription(description) + ">");
		
	}
	
	
	@Override
	public String toString() { 
		return  (value == null) ? key : key + Constants.EQ + value ; 
	}		

	@Override
	public  int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + key.hashCode();
		result += result * prime + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	@Override
	public  boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		VcfHeaderRecord other = (VcfHeaderRecord) obj;
		//key will never be null
		if(key.equals(other.key))
			//value.equals(other.value); incase value == null
			return  other.hashCode() == this.hashCode(); 				
		
		return false;
	}
		
	@Override
	public int compareTo(VcfHeaderRecord o) {	
		int diff = getMetaKey().compareTo(o.getMetaKey());			
		if(diff != 0) return diff;
		
		if(id != null){
			boolean isNumeric = getId().chars().allMatch( Character::isDigit ) && o.getId().chars().allMatch( Character::isDigit );
			if(isNumeric)					 
				return Integer.compare( Integer.parseInt(o.getId()) ,Integer.parseInt(getId()));
		
			return	o.getId().compareTo(getId());	
		}	
		
		return 0; 
	}
	
	/**
	 * 
	 * @return the key string which is the string before "=" mark of ##key=value; 
	 */
	public String getMetaKey(){ return key; }
	
	/**
	 * 
	 * @return the meta value string  which is the string after "=" mark of ##key=value; 
	 */
	public String getMetaValue(){ return value; }	
	
	/**
	 * 
	 * @return the ID of the structured meta-information header line, eg. ##Key=<ID=id, ...>
	 * return null, if ID is not exists.
	 */
	public String getId(){ return id;} 	
	
	
	/**
	 * 
	 * @param pairKey
	 * @return the related pair value of specified pair key, eg.  ##Key=<ID=id,pairkey1=pairValue1, pairkey2=pairValue2, ...>
	 * return null if the specified pairKey or pairValue is not exists.
	 */
	public String getSubFieldValue(String pairKey){ 
		if(pairs != null) {
			for(Pair<String, String> pair : pairs) {
				if(pair.getLeft().equalsIgnoreCase( pairKey)  ) {				 
					return pair.getRight(); 
				}
			}
		}
		return null; 		
	}
	public List<Pair<String,String>> getSubFields(){ return pairs; }	
	
	/**
	 * format input string to a proper description string
	 * @param str: an input string
	 * @return a trimmed string enclosed by double quotation. 
	 */
	static String parseDescription(String str){
		String str1 = str.trim();
		String str2 = "";
		str2 += (str1.startsWith("\""))? str1 : "\"" + str1;
		str2 += (str1.endsWith("\""))? "" : "\"";
		return str2;
	}

	static private List<Pair<String,String>> getPairs(String sValue){
		
		if( !sValue.startsWith("<")  || !sValue.endsWith(">")) 
			return null; 
			
		List<Pair<String,String>> values =  new ArrayList<Pair<String,String>>();
		//remove space around = 			
		String subLine = sValue.substring( 1, sValue.length()-1).trim()+Constants.COMMA;
				 
		List<String> quotStr = new ArrayList<String>();
		Matcher matcher = PATTERN.matcher(subLine);
 		while(matcher.find()){
 			String mstr = matcher.group();
 			mstr = mstr.substring(mstr.indexOf("\"")+1, mstr.lastIndexOf("\""));
 			quotStr.add(  mstr.trim()  );
 		} 
 			 		
 		String[] quotKeys = PATTERN.split(subLine);	
 		for(int i = 0; i < quotKeys.length; i++){  
 			//get sub field key value pair
 			String[] subs = quotKeys[i].split(",");   		
 			for(int j = 0; j < subs.length; j++){
 				subs[j] = subs[j].trim();	 	 				
 				String key = null, value = null;
 				if(quotStr.size() > i &&   j == subs.length-1 ) //the key bf quot string
 				{	key = subs[j].replace("=", "").trim();
 					value = "\"" + quotStr.get(i) + "\"";
 				}else{  //non quot string sub field
	 				int index = subs[j].indexOf(Constants.EQ);
	 				if(index > 0){ key = subs[j].substring(0,index).trim(); value = subs[j].substring(index+1).trim();}
	 				else key = subs[j];	 					
 				}	
 				if( key.equals("") &&  value.equals("")) continue;				
 				values.add(Pair.of( key, value));
 			}			 
 		}	
 		
 		if(values.size() == 0) return null; 
					 
		Pair<String, String> pid = null; 
		for (Pair<String, String> p : values) {
			if (p.getKey().equals(ID)){ 
				pid = p; break; 
			}
		}
		
		if(!values.get(0).equals( pid )){
			values.remove( pid );
			values.add(0, pid );
		} 
				
		return values; 
	}

}
