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
import java.util.*;

import org.qcmg.common.log.QLogger;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMWriterFactory;


import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

public class FixBAM {
	
	/**
	 * 0:  only output read which Seq and QUAL are same length; 
	 * >0, only report reads with specified length
	 * <0, output reads without check the Seq length
	 */
	private final int seqLength;   
	private final File input;
	private final File output;
	private final File tmpDir;
	private final QLogger log;
	SAMFileHeader header;
	ValidationStringency validation;
	
	//constructor for unit test only
	FixBAM(File input, File output,File tmpDir, QLogger logFile,  int length) throws IOException{
		this.output = output;
		this.input = input;
		this.tmpDir = tmpDir;
		this.log = logFile;
		this.seqLength = length;
		this.validation = ValidationStringency.SILENT;
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(input, null, ValidationStringency.SILENT)) {
			this.header = reader.getFileHeader().clone();
		}
	}
	
	FixBAM(SAMFileHeader header, String readsFileName, String outputName, 
			String tmpdir, QLogger logFile,  int readLength, ValidationStringency validation) throws Exception {
		if (header == null) {
			throw new Exception(FixBAM.class + " Exception: SMAFileHeader is null ");
		}
		if (outputName == null) {
			throw new Exception(FixBAM.class + " Exception: outputName is null ");
		}
		if (readsFileName == null) {
			throw new Exception(FixBAM.class + " Exception: readsFileName is null ");
		}
		if (logFile == null) {
			throw new Exception(FixBAM.class + " Exception: logFile is null ");
		}



		this.header = header;
		this.output = new File(outputName);
		this.input = new File(readsFileName);
		this.tmpDir = tmpdir != null ? new File(tmpdir) : new File(outputName).getParentFile();
		this.log = logFile;
		this.validation = Objects.requireNonNullElse(validation, ValidationStringency.LENIENT);
		log.info("set validation to " + this.validation);

		this.seqLength = readLength;
		if (seqLength > 0) {
			log.info("length value is " + seqLength + " : output reads with seq length " + seqLength);
		} else if (seqLength == 0) {
			log.info("length value is " + seqLength + " : output reads which seq length is equal to base quality string length");
		} else {
			log.info("length value is " + seqLength + " : ignore reads seq length");
		}
		
	 	//filter out all reads with unqualified Seq length
		File tmpBAM = File.createTempFile(output.getName(), ".tmp.bam", tmpDir);
		List<SAMRecord> badReads = firstFilterRun(tmpBAM);

		//check whether there are any mate of bad Reads left on the temp BAM
		Map<String, Integer> badMates = checkMate(badReads, tmpBAM); //new HashMap<String, Integer>();
		
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
			throw new Exception("Exception occurred during renaming: " + tmp.getPath());
		}
		
		
		//rename index files
		try{
			if(output.getName().endsWith("sam"))
				Files.move(Porg[1], Pdes[1], StandardCopyOption.REPLACE_EXISTING);			
			new File(tmpbai).delete();	
			log.info("renamed tmp bai to " + outbai);
		}catch(Exception e){
			log.error("Exception occurred during renaming: " + tmpbai);
		}
		
	}
		
	/**
	 * 
	 * @param reads: a list of record which didn't qualified on the firstFilterRun
	 * @param inBAM: a bam file created by firstFilterRun, in which all satisfied records are stored
	 * @return 
	 * @throws IOException
	 */
	Map<String, Integer> checkMate(List<SAMRecord> reads, File inBAM) throws IOException {
		
		Map<String, Integer> badMateID = new HashMap<>();
		SamReader tmpReader = SAMFileReaderFactory.createSAMFileReader(inBAM,  null, validation);
		if ( ! header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate)) {
			tmpReader.close();
			throw new IllegalArgumentException("currently we can only work for output BAM with sorted by coordinate ");
		}
		
		for (SAMRecord re: reads) {
			if (re.getReadPairedFlag() &&   tmpReader.queryMate(re) != null) {
				badMateID.put(re.getReadName(), 0);
			}
		}
		
		tmpReader.close();
		log.info("number of bad mate reads is " + badMateID.size());

		return badMateID;
	}
	
	/**
	 * 
	 * @param output: all qualified records will be stored here
	 * @return list: unqualified records will be return  
	 * @throws IOException 
	 */
	List<SAMRecord> firstFilterRun(File output) throws IOException {
		
        List<SAMRecord> badReads = new ArrayList<>();       
        String id = header.getReadGroups().get(0).getId();  
        long numOfInput = 0;
        long numOfOutput = 0;
        boolean ok2add;		
		
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(input, null,  validation)) {
			boolean preSort = reader.getFileHeader().getSortOrder().equals(header.getSortOrder());			
			SAMWriterFactory factory = new SAMWriterFactory(header, preSort, output, tmpDir, 2000000, true);
			
			try (SAMFileWriter writer = factory.getWriter()) {
		    	for (SAMRecord record : reader) {
		    		record.setAttribute("RG", id );	  
		    		if (seqLength > 0) {
						ok2add = (record.getReadLength() == seqLength);
					} else if(seqLength == 0) {
						ok2add = (record.getReadLength() == record.getBaseQualityString().length());
					} else {
						ok2add = true;
					}
		    		
		    		if (ok2add) {
		    			writer.addAlignment(record);	
		    			numOfOutput ++;
		    		} else {
		    			badReads.add(record);
		    		}
		    		numOfInput ++;
		    	}
			}	
			factory.renameIndex(); //try already closed writer
			log.info("created a temp BAM: " + output.getPath());
			if (factory.getLogMessage() != null) {
				log.info(factory.getLogMessage());
			}
		}
       	log.info("number of reads from input at first time is " + numOfInput);
        log.info("number of good reads outputted at first time is " + numOfOutput);
        log.info("number of bad reads detected at first time is " + badReads.size());
        
        return badReads;
	}
		
	/**
	 * 
	 * @param badIDs: a list of bad reads id stored in hash table
	 * @param inBAM: input bam usually it is the tmp file created by first filter run
	 * @throws IOException
	 */
	void secondFilterRun(Map<String, Integer>  badIDs, File inBAM)throws IOException{
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(inBAM, null, validation)) {
			//set presort as true since the tmpBAM already sorted. otherwise throw exception
			SAMWriterFactory factory = new SAMWriterFactory(header, true, output, tmpDir, 2000000, true ); 
			try (SAMFileWriter writer = factory.getWriter()){
		        long numOfOutput = 0;
		        long numOfBad = 0;
		    	for (SAMRecord record : reader) {
		    		if (badIDs.containsKey(record.getReadName())) {
						numOfBad ++;
					} else {
		    			writer.addAlignment(record);	
		    			numOfOutput ++;
		    		}		    			
		    	}
		      	log.info("created final output " + output.getPath());
		        log.info( "number of good reads outputted at second time is "+  numOfOutput);
		        log.info("number of discarded bad mate reads at second time is " + numOfBad);
			}
	    	factory.renameIndex(); //try already closed writer 
	    	if (factory.getLogMessage() != null) {
				log.info(factory.getLogMessage());
			}
		}         
    	//delete tmp files  
        inBAM.delete();
    	log.info("deleted temporary output BAM file: " + inBAM.getPath());
        
    	File inBai = new File(inBAM.getPath() + ".bai");
    	inBai.delete();    	
    	log.info("deleted temporary output index file: " + inBai.getPath());

	} 
}
