/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.qcmg.common.log.QLogger;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;


import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

public class FixBAM {
	
	/**
	 * 0:  only output read which Seq and QUAL are same length; 
	 * >0, only report reads with specified length
	 * <0, output reads without check the Seq lenght
	 */
	private final int seqLength;   
	private File input;
	private File output;
	private File tmpDir;
	private QLogger log;
	SAMFileHeader header;
	ValidationStringency validation;
	
	//constructor for unit test only
	FixBAM(File input, File output,File tmpDir, QLogger logFile,  int length){
		this.output = output;
		this.input = input;
		this.tmpDir = tmpDir;
		this.log = logFile;
		this.seqLength = length;
		this.validation = ValidationStringency.SILENT;
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(input, ValidationStringency.SILENT);  
		this.header = reader.getFileHeader().clone();
		
	}
	
	FixBAM(SAMFileHeader header, String readsFileName, String outputName, 
			String tmpdir, QLogger logFile,  int readLength, ValidationStringency validation) throws Exception {	
		if(header == null)
			throw new Exception(FixBAM.class.toString() +  " Exception: SMAFileHeader is null ");		
		else
			this.header = header;
		
		if( outputName == null)
			throw new Exception(FixBAM.class.toString() +  " Exception: outputName is null ");			
		else
			output = new File(outputName);
			
		if( readsFileName == null)
			throw new Exception(FixBAM.class.toString() +  " Exception: readsFileName is null ");
		else
			input = new File(readsFileName);
			
		if( tmpdir == null)
			tmpDir = new File(outputName).getParentFile();
		else 
			tmpDir = new File(tmpdir);
		
		if(logFile == null)
			throw new Exception(FixBAM.class.toString() +  " Exception: logFile is null ");
		else
			log = logFile;
		
		if(validation == null)
			this.validation = ValidationStringency.LENIENT;
		else
			this.validation = validation;
		log.info("set validation to " + this.validation);
		
		seqLength = readLength;
		if(seqLength > 0)
			log.info("length value is " + seqLength + " : output reads with seq length "+ seqLength);
		else if(seqLength == 0)
			log.info("length value is " + seqLength + " : output reads which seq length is equal to base quality string length" );
		else
			log.info("length value is " + seqLength + " : ignore reads seq length");
		
	 	//filter out all reads with unqualified Seq length
		File tmpBAM = File.createTempFile(output.getName(), ".tmp.bam", tmpDir);
		//File tmpBAM = new File(outputName + ".tmp.bam");
		List<SAMRecord> badReads = firstFilterRun(tmpBAM);

		//check whether there are any mate of bad Reads left on the temp BAM
		HashMap<String, Integer> badMates = checkMate(badReads, tmpBAM); //new HashMap<String, Integer>();
		
		//create final output BAM and index, delete all tmp files
		//create final sam without index 
	    	if (badMates.size() > 0 || output.getPath().endsWith(".sam")) {
	    		secondFilterRun(badMates, tmpBAM );    		
	    	} else {
	    		tmp2outputBAM(tmpBAM);
	    	}

	}
	 
	void tmp2outputBAM(File tmp) throws Exception{
		String tmpbai = tmp.getPath() + ".bai";
		String outbai = output.getPath() + ".bai";
		
		Path[] Porg = {Paths.get(tmp.getPath()), Paths.get(tmpbai)};
		Path[] Pdes = {Paths.get(output.getPath()), Paths.get(outbai)};
		
		if( ! tmp.exists()){
			throw new Exception( "can't rename file, since file not exist: " + tmp.getPath() );
		}
		//do nothing if both File are instance of same real file
		if( tmp.getPath().equals(output.getPath())) return;
		
		//rename BAM files
		try{
			Files.move(Porg[0], Pdes[0], StandardCopyOption.REPLACE_EXISTING);			
			tmp.delete();	
			log.info("renamed tmp BAM to " + output.getPath());
		}catch(Exception e){
			throw new Exception("Exception occured during renaming: " + tmp.getPath());
		}
		
		
		//rename index files
		try{
			if(output.getName().endsWith("sam"))
				Files.move(Porg[1], Pdes[1], StandardCopyOption.REPLACE_EXISTING);			
			new File(tmpbai).delete();	
			log.info("renamed tmp bai to " + outbai);
		}catch(Exception e){
			log.error("Exception occured during renaming: " + tmpbai);
		}
		
	}
		
	/**
	 * 
	 * @param reads: a list of record which didn't qualified on the firstFilterRun
	 * @param inBAM: a bam file created by firstFilterRun, in which all satisfied records are stored
	 * @return 
	 * @throws IOException
	 */
	HashMap<String, Integer> checkMate(List<SAMRecord> reads, File inBAM)throws IOException{
		
		HashMap<String, Integer> badMateID = new HashMap<String, Integer>();
		SamReader tmpreader = SAMFileReaderFactory.createSAMFileReader(inBAM,  validation);  	
		if( ! header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate)){
			tmpreader.close();
			throw new IllegalArgumentException("currently we can only work for output BAM with sorted by coordinate ");
		}
		
		for(SAMRecord re: reads){
			if(re.getReadPairedFlag() &&   tmpreader.queryMate(re) != null){		 
				badMateID.put(re.getReadName(), 0);
			}
		}
		
		tmpreader.close();
		log.info("number of bad mate reads is " + badMateID.size());

		return badMateID;
	}
	
	/**
	 * 
	 * @param output: all qualified records will be stored here
	 * @return list: unqualified records will be return  
	 * @throws IOException 
	 */
	List<SAMRecord> firstFilterRun(File output) throws IOException{
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(input,  validation);  

		SAMOrBAMWriterFactory factory;		
		if(reader.getFileHeader().getSortOrder().equals(header.getSortOrder())) {
			factory = new SAMOrBAMWriterFactory(header, true, output, tmpDir, 2000000, true );
		} else {
        		factory = new SAMOrBAMWriterFactory(header, false, output, tmpDir, 2000000, true);
		}
						 
		SAMFileWriter writer = factory.getWriter();
		
        List<SAMRecord> badReads = new ArrayList<>();
        
        String id = header.getReadGroups().get(0).getId();  
        long NumofInput = 0;
        long NumofOutput = 0;
        boolean ok2add;
	    	for( SAMRecord record : reader){
	    		record.setAttribute("RG", id );
  
	    		if(seqLength > 0)
	    			ok2add = (record.getReadLength() == seqLength); 
	    		else if(seqLength == 0)
	    			ok2add = (record.getReadLength() == record.getBaseQualityString().length());
	    		else
	    			ok2add = true;
	    		
	    		if (ok2add){
	    			writer.addAlignment(record);	
	    			NumofOutput ++;
	    		} else {
	    			badReads.add(record);
	    		}
	    		NumofInput ++;	
		}
	    	//writer.close();
	    	factory.closeWriter();
	    	reader.close();
    	
		log.info("crteated a temp BAM: " + output.getPath());
		if(factory.getLogMessage() != null)
			log.info(factory.getLogMessage());
       	log.info("number of reads from input at first time is " + NumofInput);
        log.info("number of good reads outputed at first time is " + NumofOutput);
        log.info("number of bad reads detected at first time is " + badReads.size());
        
        return badReads;
	}
		
	/**
	 * 
	 * @param badIDs: a list of bad reads id stored in hash table
	 * @param inBAM: input bam usually it is the tmp file created by first filter run
	 * @throws IOException
	 */
	void secondFilterRun(HashMap<String, Integer>  badIDs, File inBAM)throws IOException{
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(inBAM, validation);
		//set presort as true since the tmpBAM already sorted. otherwise throw exception
		SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, true, output, tmpDir, 2000000, true ); 
		SAMFileWriter writer = factory.getWriter();
		
        long NumofOutput = 0;
        long NumofBad = 0;
	    	for( SAMRecord record : reader){	
	    		if(badIDs.containsKey(record.getReadName()))
	    			NumofBad ++;
	    		else{
	    			writer.addAlignment(record);	
	    			NumofOutput ++;
	    		}
	    			
		}
	    	factory.closeWriter();
	    	reader.close();

       	log.info("created final output " + output.getPath());
       	if(factory.getLogMessage() != null)
       		log.info(factory.getLogMessage());
        log.info( "number of good reads outputed at second time is "+  NumofOutput);
        log.info("number of discarded bad mate reads at second time is " + NumofBad); 
        
         
    	//delete tmp files  
        inBAM.delete();
	    	log.info("deleted tmporary output BAM file: " + inBAM.getPath());
	        
	    	File inBai = new File(inBAM.getPath() + ".bai");
	    	inBai.delete();    	
	    	log.info("deleted tmporary output index file: " + inBai.getPath());

	} 
}
