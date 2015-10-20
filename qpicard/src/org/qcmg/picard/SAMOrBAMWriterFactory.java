/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import htsjdk.samtools.BAMIndex;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;

public class SAMOrBAMWriterFactory {
	private final File output;
	private final SAMFileWriter writer;
	private final boolean index;
	private String logMessage = null;

	 
	
	public SAMOrBAMWriterFactory(SAMFileHeader header, boolean preSort, File output) {
		this(header,preSort, output, null, 0,  true);
	}
	
	public SAMOrBAMWriterFactory(SAMFileHeader header, boolean preSort, File output, boolean createindex) {
		this(header,preSort, output, null, 0,  createindex);
	}
	
	public SAMOrBAMWriterFactory(SAMFileHeader header, boolean preSort, File output, int RamReads) {
		this(header,preSort, output, null, RamReads,  true);
	}
	public SAMOrBAMWriterFactory(SAMFileHeader header, boolean preSort, File output, File tmpDir ){
		this(header, preSort, output,tmpDir, 0, true);
	}
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, boolean createindex){
		this(header, preSort, output,tmpDir, 0, createindex);
	}
	
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int RamReads){
		this(header, preSort, output,tmpDir, RamReads, true);
	}
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int RamReads, boolean createindex){
		SAMFileWriterFactory factory = new SAMFileWriterFactory();   
		if(RamReads != 0)
			htsjdk.samtools.SAMFileWriterImpl.setDefaultMaxRecordsInRam( RamReads );
		
		index = createindex && header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate);
		
		factory.setCreateIndex(index);	
		
		if(tmpDir != null)
			factory.setTempDirectory(tmpDir);
		
		this.output = output;		
		
		writer = factory.makeSAMOrBAMWriter(header, preSort, output);  
	}
	
	
	public SAMFileWriter getWriter(){
		
		return writer;
	}
	public void closeWriter(){
		writer.close();
 
		if(index)
			renameIndex(output);
	}
	
	/**
	 * rename index, eg. output.bai to output.bam.bai if the related bam named as output.bam
	 * @param bamFile
	 */
	public void renameIndex(File bamFile) {
		String path = bamFile.getPath();
	   	String indexFileBase = bamFile.getPath().endsWith(".bam") ? bamFile.getPath().substring(0, path.lastIndexOf(".")) : path;

	   	if(! indexFileBase.equals(path)){
	        File org = new File(indexFileBase + BAMIndex.BAMIndexSuffix);
	        File des = new File(path + BAMIndex.BAMIndexSuffix);
			
	        if(! org.exists()){
	        	logMessage = "Error: can't rename file, since file not exist: " + org.getPath();
	        	return;
	        }else if(! org.getPath().equals(des.getPath())) {
	        	Path pOrg = Paths.get(org.getPath());
	    		Path pDes = Paths.get(des.getPath());
	        	try{
					Files.move(pOrg, pDes, StandardCopyOption.REPLACE_EXISTING);			
					org.delete();			 
				}catch(Exception e){
					logMessage ="Error: Exception occured during deleting file: " + org.getPath();
				}
	        }
	        logMessage = "Succeed: renamed index file to " + des.getPath();
        }
	}
	
	public String getLogMessage(){
		return logMessage;
	}
}
