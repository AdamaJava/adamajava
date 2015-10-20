/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.qcmg.common.log.QLogger;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.fastq.*;
import org.qcmg.picard.SAMFileReaderFactory;

public class FixHeader {
	SAMFileHeader header = null;
	
	public FixHeader(NewOptions options, QLogger logger ) throws Exception{
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(options.getInputFileName()),options.getValidation() );  
		header = reader.getFileHeader().clone();
		reader.close();
		
		String OldTxtRG = getTxtRG( header );				 
		header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
		logger.info("set sort order to coordinate");

		//check SQ & add UR if not exists 
		if(options.getReferenceFile() != null){
			ReplaceSQ newSQ = new ReplaceSQ(header.getSequenceDictionary(), options.getReferenceFile());
			if(newSQ.HasNewUR()) 
				logger.info("add field to SQ line UR:" + options.getReferenceFile());
			else
				logger.info("existing SQ lines are checked and consistance to the reference file");			
		}
		
		//check the RG 	
		List<SAMReadGroupRecord> rglist = header.getReadGroups();		
		if(rglist.size() == 0){
			header.setReadGroups(
					new ReplaceRG(null, options.getRGinfo(),  options.getInputFileName()).getReadGroupList());	
			logger.info("create RG line: " +getTxtRG(header.clone()));
		}else if(rglist.size() == 1){ 
			header.setReadGroups(
					new ReplaceRG(rglist.get(0), options.getRGinfo(), options.getInputFileName()).getReadGroupList());		
			logger.info("replace RG line to: " +getTxtRG(header.clone()));
			header.addComment("previous RG line was " + OldTxtRG);
			logger.info("add old RG line to CO: " + OldTxtRG);
		}else
			throw new Exception("multi RG lines exist in the input BAM. Current tool can't fix this sort of BAM.");	 
	}
	
	private String getTxtRG(SAMFileHeader he){
		String [] txtHeader = he.getTextHeader().split("\\n");
		for (String str: txtHeader){
			if( str.contains("@RG")  )
				return str;
		}
		
		return null;		
	}

	public SAMFileHeader getHeader(){
		
		return header;
	}

}
