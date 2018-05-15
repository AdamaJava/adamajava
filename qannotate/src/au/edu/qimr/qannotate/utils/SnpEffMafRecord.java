/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.utils;


import java.util.Arrays;
import java.util.stream.Collectors;

import org.qcmg.common.util.Constants;
 

public class SnpEffMafRecord {	
	public static final String Unknown = "unknown";
	public static final String UnTest = "untested";
	public static final String novel = "novel";
	public static final String none = "none";
	public static final String Null = Constants.NULL_STRING;
	public static final String Zero = "0";	
	public static final String Version = "#version 2.4.1";
	public static final String center = "QIMR_Berghofer";
	
	private final String[] values = new String[MafElement.values().length];
	
	
	public SnpEffMafRecord(){
	 
		for(MafElement ele: MafElement.values()){
			int no = ele.getColumnNumber() - 1;
			values[no] = ele.getDefaultValue();
		}	
	}
	
	//all 58 set methods
	public String getMafLine() {
		return Arrays.stream(values).collect(Collectors.joining(Constants.TAB_STRING));
	}
	
	public String getMafLine(boolean hasACSNP) {
		if( hasACSNP)
			return getMafLine();
		
		return Arrays.stream(Arrays.copyOfRange(values, 0, 62) ).collect(Collectors.joining(Constants.TAB_STRING));
	}
		
	public void setColumnValue(MafElement ele, String value) {
		int no = ele.getColumnNumber();
		values[no-1] = value;  
	}
	
	public String getColumnValue(MafElement ele) {
		int no = ele.getColumnNumber();
		return values[no-1];
	}
	
	public String getColumnValue(int no) {
		if(no < 0 || no > MafElement.values().length)
			throw new ArrayIndexOutOfBoundsException(no + " is out of snp maf record column size");
		
		return values[no-1];
	}
	
	public static String getSnpEffMafHeaderline(){
		return Arrays.stream(MafElement.values()).map(MafElement::name).collect(Collectors.joining(Constants.TAB_STRING));
	}
	
	/**
	 * 
	 * @param hasACSNP
	 * @return header line excludes note of ACSNP if hasACSNP is false; 
	 */
	public static String getSnpEffMafHeaderline(boolean hasACSNP){
		if( hasACSNP)
			return getSnpEffMafHeaderline();
		
		// Arrays.stream(MafElement.values()).filter( e -> e.getColumnNumber() < 63 ).map(MafElement::name).forEach( e->System.out.println(e) );
		return Arrays.stream(MafElement.values()).filter( e -> e.getColumnNumber() < 63 ).map(MafElement::name).collect(Collectors.joining(Constants.TAB_STRING));
	}	

}
 