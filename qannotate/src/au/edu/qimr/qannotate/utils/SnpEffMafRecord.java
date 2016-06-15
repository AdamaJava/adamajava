/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.utils;


import java.util.Arrays;
import java.util.stream.Collectors;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Constants;
 

public class SnpEffMafRecord {	
	
	public static final String Unknown = "Unknown";
	public static final String UnTest = "UnTest";
	public static final String Other = "Other";
	public static final String novel = "novel";
	public static final char positive = '+';
	public static final String none = "none";
	public static final String No = "No";
	public static final String Null = "null";
	public static final String Yes = "Yes";
	public static final String Zero = "0";	
	public static final String Version = "#version 2.4.1";
	public static final String center = "QIMR_Berghofer"; 
	
//	public final static int column = 60;  add column notes
//	public final static int column = 61; 
//	private final String[] maf = new String[column];	
	private final MafElement[] maf = new MafElement[MafElement.values().length];
	private final String[] values = new String[MafElement.values().length];
	
//	public enum MUTATION_STATUS{ 
//		None, Germline,Somatic,LOH,PostTranscriptional, modification,Unknown;
//		@Override
//		public String toString() {
//			switch (this) {
//				case PostTranscriptional:
//					return "Post-transcriptional";			 
//				default:
//					return name();		 		
//			}
//		}
//	};
//	public enum VALIDATION_STATUS { Untested,Inconclusive, Valid,Invalid };
	
	
	public SnpEffMafRecord(){
	 
		for(MafElement ele: MafElement.values()){
			int no = ele.getColumnNumber() - 1;
			maf[no] = ele; 	
			values[no] = ele.getDefaultValue();
		}		
	}
	
	//all 58 set methods
	public String getMafLine() {
		
		return Arrays.stream(values).collect(Collectors.joining(Constants.TAB_STRING));
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
		SnpEffMafRecord maf = new SnpEffMafRecord();
		String str = "";
		for(MafElement ele :  maf.maf)
			str += Constants.TAB + ele.name();
		return str.replaceFirst(Constants.TAB+"", "");
	}

}
 
