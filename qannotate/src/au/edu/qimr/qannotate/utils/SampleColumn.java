package au.edu.qimr.qannotate.utils;

import java.io.File;
import java.util.function.Function;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;


public class SampleColumn {
	
	private final int test_column ; //can't be -1 since will "+1"
	private final int control_column ;
	private final String test_Sample;
	private final String control_Sample;
	private final String test_bamID;
	private final String control_bamID;
	private final QLogger logger = QLoggerFactory.getLogger(SampleColumn.class);
	
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
		
		String[][] sampleIds = getPossibleSampleId( header);
		this.control_bamID = sampleIds[0][1];
		this.test_bamID = sampleIds[1][1];	
		String controlS = (control == null)? sampleIds[0][0] :  control;
		String testS = (test == null)? sampleIds[1][0] :  test;
		
		logger.info("control sample: " + controlS);
		logger.info("test sample: " + testS);
		
		int tc = -2, cc = -2;				
	    final String[] samples = header.getSampleId();			    
	    
		//incase both point into same column
		for (int i = 0; i < samples.length; i++) { 	
			if( ( controlS != null && samples[i].equalsIgnoreCase(controlS )) ||  //match sampleid
					samples[i].equalsIgnoreCase(sampleIds[0][1]) ||  //match bamid
					samples[i].equalsIgnoreCase(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE.substring(2)) ) {
				cc = i + 1;										 				
			}
			
			if( ( testS != null &&  samples[i].equalsIgnoreCase(testS) )|| 
					samples[i].equalsIgnoreCase(sampleIds[1][1]) || 
					samples[i].equalsIgnoreCase(VcfHeaderUtils.STANDARD_TEST_SAMPLE.substring(2)) ) { 
				tc = i + 1;
			}
		}
		
		
		if ( tc <= 0 && cc <= 0 ) {
			throw new RuntimeException("can't find test sample id  " + test + ", or control sample id" + control + " from vcf header");
		} else if (cc <= 0) {
			/*
			 * update to equal test
			 */
			logger.info("Setting cc to equal tc");
			cc = tc;
		} else if (tc <= 0) {
			/*
			 * update to equal test
			 */
			logger.info("Setting tc to equal cc");
			tc = cc;
		}

		//only keep uuid eg.  
		int ss = controlS == null? 0 : Math.max(controlS.indexOf("#"), controlS.indexOf(":"));			
		this.control_Sample = (ss > 0)?  controlS.substring(ss+1) : controlS;
		
		ss = testS == null? 0 : Math.max(testS.indexOf("#"), testS.indexOf(":"));				
		this.test_Sample = (ss > 0)?  testS.substring(ss+1) : testS;
		
		this.test_column = tc; 
		this.control_column = cc; 
	}		

	/**
	 * 
	 * @param header : vcfheader
	 * @return two dimension array {{controlSample, controlBamId}, {testSample, testBamId}}
	 */
	private String[][] getPossibleSampleId(  VcfHeader header){
		//List<String> ids = new ArrayList<String>();
		String[][] ids = new String[2][2];
		String[][] temp = new String[2][6];
		VcfHeaderRecord re; 
		
		temp[0][0] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE)) != null? re.getMetaValue() : null;
		temp[0][1] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE_1)) != null? re.getMetaValue() : null;	 
		temp[0][2] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_BAMID)) != null? re.getMetaValue() : null;	 
		temp[0][5] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_BAMID_1)) != null? re.getMetaValue() : null;			
		temp[0][3] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_BAM )) != null? re.getMetaValue() : null;
		temp[0][4] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_BAM_1)) != null? re.getMetaValue() : null;
		temp[1][0] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_SAMPLE)) != null? re.getMetaValue() : null;
		temp[1][1] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_SAMPLE_1)) != null? re.getMetaValue() : null;
		temp[1][2] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_BAMID)) != null? re.getMetaValue() : null;
		temp[1][5] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_BAMID_1)) != null? re.getMetaValue() : null;
		temp[1][3] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_BAM)) != null? re.getMetaValue() : null;
		temp[1][4] = (re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_BAM_1)) != null? re.getMetaValue() : null;
		
		
		Function<String, String> removeBam = (String s) -> 
			(null != s && s.endsWith(".bam")) ? s.substring(0, (s.length() - 4)) : s;	
			
		for(int i = 0; i < temp.length; i++)
			for(int j = 0; j < temp[i].length; j ++)
				if( StringUtils.isMissingDtaString(temp[i][j])   || temp[i][j].equalsIgnoreCase("null")) temp[i][j] = null; 
					
		ids[0][0] = temp[0][0] != null ? temp[0][0] : temp[0][1]; 	//sample id
		ids[0][1] = temp[0][2] != null ? temp[0][2] : temp[0][5];	//bamid				
		if(ids[0][1] == null){
			String bam = temp[0][3] != null ? temp[0][3] : temp[0][4];
			ids[0][1] = (bam == null) ? null :  removeBam.apply(new File(bam).getName());				
		}
			
		ids[1][0] = temp[1][0] != null ? temp[1][0] : temp[1][1];   //sample id
		ids[1][1] = temp[1][2] != null ? temp[1][2] : temp[1][5];	//bamid		
		if(ids[1][1] == null){
			String bam = temp[1][3] != null ? temp[1][3] : temp[1][4];
			ids[1][1] = (bam == null) ? null : removeBam.apply( new File(bam).getName());				
		}
 
		return ids;
	
	}	
	
	public String getTestBamId(){ return test_bamID;}
	public String getControlBamId(){ return control_bamID; }		
	public int getTestSampleColumn(){ return test_column; }		
	public int getControlSampleColumn(){ return control_column; }		
	public String getTestSample(){ return test_Sample; }		
	public String getControlSample(){ 	return control_Sample; }

	static public String getDonorId(VcfHeader header){
		VcfHeaderRecord re = header.firstMatchedRecord(VcfHeaderUtils.STANDARD_DONOR_ID);
		String id =  (re == null)? null: re.getMetaValue() ;
		
		if(id == null )
			id = (re = header.firstMatchedRecord("##1:qDonorId")) == null ? null: re.getMetaValue() ;
		
		return id;	 
		 
	}
}

