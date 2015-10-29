package org.qcmg.common.vcf.header;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfHeaderSampleFormat {
	
	public static void setSampleId(VcfHeader header, String id, boolean isTest){
		
		
	}
	
	
	/**
	 * 
	 * @param header: a VcfHeader
	 * @param id: sample id
	 * @param isTest: if true set id to first sample column , otherwise set to Control column which is second sample column
	 */
	public static void addSampleId(VcfHeader header, String id, boolean isTest){
		if (null == header) {
			throw new IllegalArgumentException("null vcf header object passed to VcfHeaderUtils.addQPGLineToHeader");
		}
		
		String[] exsitIds = header.getSampleId();
		if(isTest){
			header.replace(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=" + id);
			if(exsitIds == null)
				exsitIds = new String[] {id};
			else
				exsitIds[0] = id;						
		}else{
			header.replace(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=" + id);
			if(exsitIds == null)
				exsitIds = new String[] {null, id};
			else if(exsitIds.length == 1)
				exsitIds = new String[] {exsitIds[0], id};
			else
				exsitIds[1] = id;			
		}
		
	   String str = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + exsitIds[0];
	   for(int i = 1 ; i < exsitIds.length; i ++)
		   str += Constants.TAB + id; 
		 
		header.parseHeaderLine(str);		
	}
	
	

	private int test_column = 1; //can't be -1 since will "+1"
	private int control_column = 0;
	private String test_Sample ;
	private String control_Sample ; 
	
	public VcfHeaderSampleFormat( VcfHeader header){
		this(null, null, header);
	}
	
	/**
	 * it retrive the sample column number. eg. if the second column after "FORMAT" is for the sample named "testSample", then it will report "2" to related variable
	 * @param testSample:   testSample column name located after "FORMAT" column, put null here if vcf header already exisit qControlSample
	 * @param controlSample:  controlSample column name located after "FORMAT" column, put null here if vcf header already exisit qTestSample
	 * @param header: if null, it will point to this class's header; 
	 */		
	public VcfHeaderSampleFormat(String test, String control, VcfHeader header){

		if (header == null)
			   throw new RuntimeException(" invalid header to null!");
		
//		String cs = control, ts = test; 
		for (final VcfHeader.Record hr : header.getMetaRecords()) 
			if(control == null && hr.getData().indexOf(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE) != -1)
				control =  StringUtils.getValueFromKey(hr.getData(), VcfHeaderUtils.STANDARD_CONTROL_SAMPLE);
			else if (test == null &&  hr.getData().indexOf(VcfHeaderUtils.STANDARD_TEST_SAMPLE) != -1) 
				test = StringUtils.getValueFromKey(hr.getData(), VcfHeaderUtils.STANDARD_TEST_SAMPLE);
		
		control_Sample =control;
		test_Sample = test;		
							
//	    if(test == null || control == null)
//		   throw new RuntimeException(" Missing qControlSample or qTestSample  from VcfHeader; please specify on command line!");
	    
	   int tc = -1, cc = -1;					    
	   final String[] samples = header.getSampleId();	
	   if(test != null && samples != null) 
		   for(int i = 0; i < samples.length; i++){ 
				if(samples[i].equalsIgnoreCase(test))
					tc = i + 1;
			if(tc >= 0)
				test_column = tc; 	
	   }
	   
	   if(control != null && samples != null) 
		   for(int i = 0; i < samples.length; i++){ 
				if(samples[i].equalsIgnoreCase(test))
					cc = i + 1;
			if(cc >= 0)
				control_column = cc; 	
	   }
	   
 		
		if( tc <= 0 || cc <= 0 )
			throw new RuntimeException("can't find test sample id  " + test + ", or normal sample id from vcf header line: " + control );
	}
	
	public int getTestSampleColumn(){
		return test_column;
	}
	
	public int getControlSampleColumn(){
		return control_column;
	}
	
	public String getTestSample(){
		return test_Sample;
	}
	
	public String getControlSample(){
		return control_Sample;
	}

	
}