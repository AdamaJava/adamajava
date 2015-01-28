package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;

public abstract class AbstractMode {
	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	protected final Map<ChrPosition,VcfRecord> positionRecordMap = new HashMap<ChrPosition,VcfRecord>();
	protected VcfHeader header;
	protected String inputUuid;
	
	protected int test_column = -2; //can't be -1 since will "+1"
	protected int control_column = -2;

 	
	protected void inputRecord(File f) throws Exception{
		
        //read record into RAM, meanwhile wipe off the ID field value;
 
        try(VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
           	for(final VcfHeaderRecord hr : header)
        		if(hr.getMetaType().equals(MetaType.META) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_UUID_LINE)){
        			inputUuid = hr.getDescription();
        			break;
        		}
        	
        	//no chr in front of position
			for (final VcfRecord qpr : reader) 
				positionRecordMap.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition(), qpr.getPosition() + qpr.getRef().length() - 1 ), qpr);	
		} 
        
        
	}
 
	protected void retriveDefaultSampleColumn(){
		retriveSampleColumn(null, null, null);
		
	}
	
	/**
	 * it retrive the sample column number. eg. if the second column after "FORMAT" is for the sample named "testSample", then it will report "2" to related variable
	 * @param testSample:   testSample column name located after "FORMAT" column
	 * @param controlSample:  controlSample column name located after "FORMAT" column
	 * @param header: if null, it will point to this class's header; 
	 */
	protected void retriveSampleColumn(String testSample, String controlSample, VcfHeader header){
		if(header == null)
			header = this.header;
		
		for(final VcfHeaderRecord hr : header)
    		if( hr.getMetaType().equals(MetaType.META) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_CONTROLSAMPLE)) 
    				 controlSample = (controlSample == null)? hr.getDescription(): controlSample; 
    		 else if( hr.getMetaType().equals(MetaType.META) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_TESTSAMPLE)) 
    				 testSample = (testSample == null)? hr.getDescription(): testSample; 
	    				 
	   if(testSample == null || controlSample == null)
		   throw new RuntimeException(" Missing qControlSample or qTestSample  from VcfHeader; please speify on command line!");
	   
	   final String[] samples = header.getSampleId();	
	   	   
		//incase both point into same column
		for(int i = 0; i < samples.length; i++) 
			if(samples[i].equalsIgnoreCase(testSample))
				test_column = i + 1;
			else if(samples[i].equalsIgnoreCase(controlSample))
				control_column = i + 1;
				
		if(test_column <= 0 )
			throw new RuntimeException("can't find test sample id from vcf header line: " + testSample);
		if(control_column <= 0  )
			throw new RuntimeException("can't find normal sample id from vcf header line: " + controlSample);	  				 
	    				 		 
	}

	abstract void addAnnotation(String dbfile) throws Exception;
	
	
	protected void writeVCF(File outputFile ) throws Exception {
		 
//		logger.info("Writing VCF output");	 		
		//get Q_EXEC or #Q_DCCMETA  org.qcmg.common.meta.KeyValue.java or org.qcmg.common.meta.QExec.java	
		final List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {			
			for(final VcfHeaderRecord record: header)  writer.addHeader(record.toString());
			for (final ChrPosition position : orderedList) {				
				final VcfRecord record = positionRecordMap.get(position); 
				writer.add( record );				 
			}
		}  
		
		
	}
	
	protected void reheader(String cmd, String inputVcfName) throws Exception{	
//		System.out.println("package: " + Main.class.getPackage());
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();

		//move input uuid into preuuid
		header.replace(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName) );
//		header.add( new VcfHeaderRecord( MetaType.OTHER.toString() + cmd));
		
		header.updateHeader( new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_VERSION),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate ),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid ),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version) );
		
	    if(version == null) version = Constants.NULL_STRING;
	    if(pg == null ) pg = Constants.NULL_STRING;
	    if(cmd == null) cmd = Constants.NULL_STRING;
		
		 VcfHeaderUtils.addQPGLineToHeader(header, pg, version, cmd);
		
	}	
}
