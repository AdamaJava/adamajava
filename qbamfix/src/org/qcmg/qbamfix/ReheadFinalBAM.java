/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.picard.RnameFile;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.picard.HeaderUtils;

import htsjdk.samtools.BAMIndex;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

public class ReheadFinalBAM {
	
	SAMFileHeader newHeader = null;
	
	//just for unit test
	public ReheadFinalBAM(){}

	public ReheadFinalBAM(SAMFileHeader header, NewOptions options, QLogger logger ) throws Exception{
//		SAMFileHeader header = reader.getFileHeader().clone();				
		//get donor information from CO qlimsmeta line
		String donor = getDonor(header.getComments());
		String SMvalue = matchDonor(donor, header.getReadGroups());
		//created new header if SM value in RG line isn't match the donor
	    if( SMvalue.length() > 0 ){
	    	SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(options.getInputFileName()),options.getValidation() );  	    	
	    	logger.info("According qlimsmeta line SM tag value " + SMvalue + " are replaced to " + donor);
	    	refinalBAM(reader, donor, new File( options.getOutputFileName()) );
		    reader.close();					    	
	    }
	    
	}
	

	 void refinalBAM(SamReader reader,String donor, File output) throws Exception{
		
		//replace header
		SAMFileHeader header = reader.getFileHeader().clone();		
		List<SAMReadGroupRecord> rglist = new ArrayList<SAMReadGroupRecord>();
		
		for(SAMReadGroupRecord re :  header.getReadGroups()){
			if(! re.getSample().equalsIgnoreCase(donor)){
				re.setSample(donor);
			}
			rglist.add(re);
		}
		header.setReadGroups(rglist);
		
		//append reads to output 
		SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, true, output,2000000 );
        SAMFileWriter writer = factory.getWriter();
    	for( SAMRecord record : reader)
    		 writer.addAlignment(record);
    	factory.closeWriter();
    	reader.close();
 
	}
	
	 String matchDonor(String donor, List<SAMReadGroupRecord> readGroups){
		String sm = "";
		for(SAMReadGroupRecord re :  readGroups){
			if(! re.getSample().equalsIgnoreCase(donor))				
				sm = sm + re.getSample() + ",";
		}
		
		//remore last ","
		if(sm.length() > 0)
			sm = sm.substring(0, sm.length()-1);
		
		return sm;
	} 
	
   String getDonor( List<String> COs) throws Exception{
    	String donor = null;
    	for(String str : COs){
    		if(str.contains("QN:qlimsmeta")){
    			
    			String[] array = str.split("Donor=");
    			array = array[1].split("\\s+");
    			donor = array[0];
    		}
    	}
    	
    	if(donor.length() > 0)
    		return donor;
    	else
    		throw new Exception("can't find related donor information on CO qlimsmeta line: " + donor);
    	 
    }
    
	
	
}
