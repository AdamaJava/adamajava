/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import htsjdk.samtools.BAMIndex;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;

public class SAMOrBAMWriterFactory {
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMOrBAMWriterFactory.class);
	
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
	
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads){
		this(header, preSort, output,tmpDir, ramReads, true);
	}
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, boolean createIndex){
		this(header, preSort, output,tmpDir, ramReads, createIndex, false);
	}
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, boolean createIndex, boolean useAsyncIO){
		this(header, preSort, output,tmpDir, ramReads, createIndex, useAsyncIO, -1);
	}
	
	public SAMOrBAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, boolean createIndex, boolean useAsyncIO, int asyncOutputBufferSize){
		SAMFileWriterFactory factory = new SAMFileWriterFactory();   
		if (ramReads > 0) {
			htsjdk.samtools.SAMFileWriterImpl.setDefaultMaxRecordsInRam( ramReads );
		}
		
		index = createIndex && header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate);
		
		factory.setCreateIndex(index);
		
		/*
		 * set async setting
		 */
		factory.setUseAsyncIo(useAsyncIO);
		if (useAsyncIO && asyncOutputBufferSize > 0) {
			factory.setAsyncOutputBufferSize(asyncOutputBufferSize);
		}
		
		if (tmpDir != null) {
			factory.setTempDirectory(tmpDir);
		}
		
		this.output = output;		
		
		writer = factory.makeSAMOrBAMWriter(header, preSort, output);  
	}
	
	public SAMFileWriter getWriter(){
		
		return writer;
	}
	public void closeWriter(){
		writer.close();
		if (index) {
			try {
				RenameFile.renameIndex(output);
			} catch(IOException e) {
				logMessage = "IOEXception caught whilst trying to move file";
				logger.error(logMessage, e);
			}
		}
	}
	
	public String getLogMessage(){
		return logMessage;
	}
}
