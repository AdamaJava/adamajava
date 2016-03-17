package au.edu.qimr.qannotate.utils;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class SampleColumn {
	 
		private final int test_column ; //can't be -1 since will "+1"
		private final int control_column ;
		private final String test_Sample ;
		private final String control_Sample ; 
		
		static public SampleColumn getSampleColumn(String test, String control, VcfHeader header){
			
			return new SampleColumn(test,  control, header);
		}
		
		/**
		 * it retrive the sample column number. eg. if the second column after "FORMAT" is for the sample named "testSample", then it will report "2" to related variable
		 * @param testSample:   testSample column name located after "FORMAT" column, put null here if vcf header already exisit qControlSample
		 * @param controlSample:  controlSample column name located after "FORMAT" column, put null here if vcf header already exisit qTestSample
		 * @param header: if null, it will point to this class's header; 
		 */		
		private SampleColumn(String test, String control, VcfHeader header){

			if (header == null)
				   throw new RuntimeException(" invalid header to null!");
			
//			String cs = control, ts = test; 
			for (final VcfHeader.Record hr : header.getMetaRecords()) 
				if(control == null && hr.getData().indexOf(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE) != -1)
					control =  StringUtils.getValueFromKey(hr.getData(), VcfHeaderUtils.STANDARD_CONTROL_SAMPLE);
				else if (test == null &&  hr.getData().indexOf(VcfHeaderUtils.STANDARD_TEST_SAMPLE) != -1) 
					test = StringUtils.getValueFromKey(hr.getData(), VcfHeaderUtils.STANDARD_TEST_SAMPLE);
								
		    if(test == null || control == null)
			   throw new RuntimeException(" Missing qControlSample or qTestSample  from VcfHeader; please specify on command line!");
		    
			int tc = -2, cc = -2;					    
		   final String[] samples = header.getSampleId();			   	   
			//incase both point into same column
			for(int i = 0; i < samples.length; i++){ 
				if(samples[i].equalsIgnoreCase(test) || samples[i].equalsIgnoreCase(VcfHeaderUtils.STANDARD_TEST_SAMPLE.substring(2)))
					tc = i + 1;
				//else if(samples[i].equalsIgnoreCase(controlSample))
				if(samples[i].equalsIgnoreCase(control) || samples[i].equalsIgnoreCase(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE.substring(2)) )
					cc = i + 1;
			}
			
			if( tc <= 0 || cc <= 0 )
				throw new RuntimeException("can't find test sample id  " + test + ", or normal sample id from vcf header line: " + control );

			control_Sample =control;
			test_Sample = test;
			test_column = tc; 
			control_column = cc; 				
			
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
