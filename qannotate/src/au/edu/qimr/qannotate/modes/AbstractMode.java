package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;

public abstract class AbstractMode {
	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	protected final Map<ChrPosition,VcfRecord> positionRecordMap = new HashMap<ChrPosition,VcfRecord>();
	protected VcfHeader header = null;
	
	private final static QLogger logger = QLoggerFactory.getLogger(Main.class, null,  null);	
	
	/**
	 * 
	 * @param f: read variants from input into RAM hash map
	 * @throws IOException
	 */
	protected void inputRecord(File f) throws IOException{
		
        //read record into RAM, meanwhile wipe off the ID field value;
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
        	//no chr in front of position
			for (final VcfRecord qpr : reader) {
				positionRecordMap.put(qpr.getChrPosition(), qpr);
			}
		} 
        
	}
 
	public class SampleColumn{
		private final int test_column ; //can't be -1 since will "+1"
		private final int control_column ;
		private final String test_Sample ;
		private final String control_Sample ; 
		
		/**
		 * it retrive the sample column number. eg. if the second column after "FORMAT" is for the sample named "testSample", then it will report "2" to related variable
		 * @param testSample:   testSample column name located after "FORMAT" column, put null here if vcf header already exisit qControlSample
		 * @param controlSample:  controlSample column name located after "FORMAT" column, put null here if vcf header already exisit qTestSample
		 * @param header: if null, it will point to this class's header; 
		 */		
		public SampleColumn(String test, String control, VcfHeader header){

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
				if(samples[i].equalsIgnoreCase(test))
					tc = i + 1;
				//else if(samples[i].equalsIgnoreCase(controlSample))
				if(samples[i].equalsIgnoreCase(control))
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

	abstract void addAnnotation(String dbfile) throws Exception;
	

	/**
	 * 
	 * @param outputFile: add annotate variants from RAM hash map intp the output file
	 * @throws IOException
	 */
	protected void writeVCF(File outputFile ) throws IOException {		 
		logger.info("Writing VCF output");	 		
		final List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {			
			for(final VcfHeader.Record record: header)  {
				writer.addHeader(record.toString());
			}
			for (final ChrPosition position : orderedList) {				
				VcfRecord record = positionRecordMap.get(position); 
				writer.add( record );				 
			}
		}  
	}
	

	/**
	 * 
	 * @param cmd: add this cmd string into vcf header
	 * @param inputVcfName: add input file name into vcf header
	 * @throws IOException
	 */
	protected void reheader(String cmd, String inputVcfName) throws IOException {	
 
		if(header == null)
	        try(VCFFileReader reader = new VCFFileReader(inputVcfName)) {
	        	header = reader.getHeader();	
	        	if(header == null)
	        		throw new IOException("can't receive header from vcf file, maybe wrong format: " + inputVcfName);
	        } 	
 
		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);	
			
		String inputUuid = (header.getUUID() == null)? null: new VcfHeaderUtils.SplitMetaRecord(header.getUUID()).getValue();   
		header.replace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING;
	    if(pg == null ) pg = Constants.NULL_STRING;
	    if(cmd == null) cmd = Constants.NULL_STRING;
		VcfHeaderUtils.addQPGLineToHeader(header, pg, version, cmd);
			
	}

}
