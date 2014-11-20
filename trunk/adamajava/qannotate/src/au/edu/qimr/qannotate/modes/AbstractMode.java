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
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;
import org.qcmg.vcf.VCFHeaderUtils;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.Messages;

public abstract class AbstractMode {
	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	protected final Map<ChrPosition,VCFRecord> positionRecordMap = new HashMap<ChrPosition,VCFRecord>();
	protected VcfHeader header;
//	private QLogger logger;
	
	protected void inputRecord(File f) throws Exception{
		
        //read record into RAM, meanwhile wipe off the ID field value;
 
        try(VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
			for (VCFRecord qpr : reader) {
				positionRecordMap.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
			}
		}  
        
	}

	abstract void addAnnotation(String dbfile) throws Exception;
	
	
	protected void writeVCF(File outputFile ) throws Exception {
		 
//		logger.info("Writing VCF output");	 		
		//get Q_EXEC or #Q_DCCMETA  org.qcmg.common.meta.KeyValue.java or org.qcmg.common.meta.QExec.java	
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {
//			header = reheader(header, cmd);
			
			for(VcfHeaderRecord record: header)  writer.addHeader(record.toString());
			for (ChrPosition position : orderedList) {				
				VCFRecord record = positionRecordMap.get(position); 
				writer.add( record );				 
			}
		}  
		
	}
	
	protected void reheader(String cmd) throws Exception{	
		
		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Messages.getProgramName();
		String fileDate = df.format(Calendar.getInstance().getTime());
		String uuid = QExec.createUUid();
		
		header.add(new VcfHeaderRecord(cmd));
		
		header.updateHeader( new VcfHeaderRecord(VCFHeaderUtils.CURRENT_FILE_VERSION),
				new VcfHeaderRecord(VCFHeaderUtils.STANDARD_FILE_DATE + fileDate ),
				new VcfHeaderRecord(VCFHeaderUtils.STANDARD_UUID_LINE + uuid ),
				new VcfHeaderRecord(VCFHeaderUtils.STANDARD_SOURCE_LINE + pg+"-"+version) );
  
		//return header;
	}	
}
