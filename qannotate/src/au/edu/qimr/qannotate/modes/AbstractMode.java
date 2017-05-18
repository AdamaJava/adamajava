/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
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
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;

abstract class AbstractMode {
	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	final Map<ChrPosition,List<VcfRecord>> positionRecordMap = new HashMap<>();
	VcfHeader header = null;

	private final static QLogger logger = QLoggerFactory.getLogger(AbstractMode.class);	
	
	/**
	 * 
	 * @param f: read variants from input into RAM hash map
	 * @throws IOException
	 */
	void inputRecord(File f) throws IOException{
		
        //read record into RAM, meanwhile wipe off the ID field value;
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
        	//no chr in front of position
			for (final VcfRecord vcf : reader) {
				String chr = IndelUtils.getFullChromosome( vcf.getChromosome() );
				ChrPosition pos = vcf.getChrPosition();
				if ( ! pos.getChromosome().equals(chr)) {
					pos = new ChrRangePosition(chr, pos.getStartPosition(), pos.getEndPosition());
				}
				
				List<VcfRecord> res = positionRecordMap.get(pos); 
				if( res == null){
					res = new ArrayList<VcfRecord>();
					positionRecordMap.put(pos, res);
				}
				res.add(vcf);	
			}
		}
        logger.info("loaded " + positionRecordMap.size() + " vcf entries from " + f.getAbsolutePath());
        
	}

	abstract void addAnnotation(String dbfile) throws Exception;
	
	/**
	 * 
	 * @param outputFile: add annotate variants from RAM hash map intp the output file
	 * @throws IOException
	 */
	void writeVCF(File outputFile ) throws IOException {		 
		logger.info("creating VCF output...");	 		
		final List<ChrPosition> orderedList = new ArrayList<>(positionRecordMap.keySet());
		Collections.sort(orderedList, new ChrPositionComparator());
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {
			for(final VcfHeaderRecord record: header)  {
				writer.addHeader(record.toString());
			}
			long count = 0; 
			for (final ChrPosition position : orderedList)  
				for(  VcfRecord record : positionRecordMap.get(position) ){				
					writer.add( record );	
					count ++;
				}
			logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  count , orderedList.size()));
		}  
	}
	
	static VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName) throws IOException {	
		 
		VcfHeader myHeader = header;  	
 		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		myHeader.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);	
			
		String inputUuid = (myHeader.getUUID() == null)? null:  myHeader.getUUID().getMetaValue(); //new VcfHeaderUtils.SplitMetaRecord(myHeader.getUUID()).getValue();   
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING_UPPER_CASE;
	    if(pg == null ) pg = Constants.NULL_STRING_UPPER_CASE;
	    if(cmd == null) cmd = Constants.NULL_STRING_UPPER_CASE;
		VcfHeaderUtils.addQPGLineToHeader(myHeader, pg, version, cmd);
		
		return myHeader;			
	}
	

	/**
	 * 
	 * @param cmd: add this cmd string into vcf header
	 * @param inputVcfName: add input file name into vcf header
	 * @throws IOException
	 */
	void reheader(String cmd, String inputVcfName) throws IOException {	

		if(header == null)
	        try(VCFFileReader reader = new VCFFileReader(inputVcfName)) {
	        	header = reader.getHeader();	
	        	if(header == null)
	        		throw new IOException("can't receive header from vcf file, maybe wrong format: " + inputVcfName);
	        } 	
 
		header = reheader(header, cmd, inputVcfName);			
	}

}
