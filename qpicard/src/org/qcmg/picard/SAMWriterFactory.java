/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.util.BAMFileUtils;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import static htsjdk.samtools.SamReader.Type.BAM_TYPE;
import static htsjdk.samtools.SamReader.Type.SAM_TYPE;
import static htsjdk.samtools.SamReader.Type.CRAM_TYPE;


public class SAMWriterFactory {
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMWriterFactory.class);
	
	private final File output;
	private final SAMFileWriter writer;
	private final boolean index;
	private String logMessage = null;
	
	public SAMWriterFactory(SAMFileHeader header, boolean preSort, File output) {
		this(header,preSort, output, null, 0,  true);
	}
	
	public SAMWriterFactory(SAMFileHeader header, boolean preSort, File output, boolean createindex) {
		this(header,preSort, output, null, 0,  createindex);
	}
	
	public SAMWriterFactory(SAMFileHeader header, boolean preSort, File output, int RamReads) {
		this(header,preSort, output, null, RamReads,  true);
	}
	public SAMWriterFactory(SAMFileHeader header, boolean preSort, File output, File tmpDir ){
		this(header, preSort, output,tmpDir, 0, true);
	}
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, boolean createindex){
		this(header, preSort, output,tmpDir, 0, createindex);
	}
	
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads){
		this(header, preSort, output,tmpDir, ramReads, true);
	}
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, boolean createIndex){
		this(header, preSort, output,tmpDir, ramReads, createIndex, false);
	}
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, 
			boolean createIndex, boolean useAsyncIO){
		this(header, preSort, output,tmpDir, ramReads, createIndex, useAsyncIO, -1, null);
	}
	
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output, File tmpDir, int ramReads, 
			boolean createIndex, boolean useAsyncIO, int asyncOutputBufferSize ){
		
		this(header, preSort, output, tmpDir, ramReads, createIndex, useAsyncIO, asyncOutputBufferSize, null);
	}

	
	public SAMWriterFactory(SAMFileHeader header,  boolean preSort, File output,File tmpDir, int ramReads, 
			boolean createIndex, boolean useAsyncIO, int asyncOutputBufferSize, File reference ) {
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
		
        final String filename = output.getName();
        
        if (BAM_TYPE.hasValidFileExtension(filename)) {
                 writer = factory.makeBAMWriter(header, preSort, output);                       
        } else if(CRAM_TYPE.hasValidFileExtension(filename)) {   
				File ref = (reference == null)? Defaults.REFERENCE_FASTA: reference;            
                writer = factory.makeCRAMWriter(header, preSort, output, ref);
        } else {
            if (!SAM_TYPE.hasValidFileExtension(filename)) {
                logger.error("Unknown file extension, assuming SAM format when writing file: " + filename);               
            }
            writer = factory.makeSAMWriter(header, preSort, output);
        }
	}
	
	public SAMFileWriter getWriter(){		
		return writer;
	}
	
	/**
	 * picard creates index aln.bai for aln.bam; and aln.cram.bai for aln.cram.
	 * Here we rename aln.bai to aln.bam.bai; and lan.cram.bai to aln.cram.crai.
	 */
	public void renameIndex(){
		
		//writer of BAM/CRAM has to be closed first 
		//the try statement will close writer, so we won't worry about it again
		 
		if (!index) return;
		
		try {				
			//rename index in case of bam
			BAMFileUtils.renameBamIndex(output);
			//rename index in case of cram
			BAMFileUtils.renameCramIndex(output);
		} catch(IOException e) {
			logMessage = "IOEXception caught whilst trying to move index file";
			logger.error(logMessage, e);
		}
		 		
	}
	
	public String getLogMessage(){
		return logMessage;
	}
}
