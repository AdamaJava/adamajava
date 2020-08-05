/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import htsjdk.samtools.CRAMFileReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class CramFileReaderFactory {
	private static final ValidationStringency DefaultStringency = ValidationStringency.SILENT;	
	private static final QLogger logger = QLoggerFactory.getLogger(CramFileReaderFactory.class);	
	
	/**
	 *
	 * Create a CRAMFileReader from a CRAM file and the supplied reference file.
	 * 
	 * @param cramFile - CRAM file to open
	 * @param reference - a source of reference sequences
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CRAMFileReader createCRAMFileReader(final File cramFile,  final File reference) throws FileNotFoundException, IOException {					 		
		return createCRAMFileReader(cramFile, null, reference,   DefaultStringency);
	}
	
	/**
	 *
	 * Create a CRAMFileReader from an input stream and optional index stream using the supplied reference source.
	 * 
	 * @param cramFile - CRAM file to open
	 * @param indexFile - index file to be used for random access
	 * @param reference - a source of reference sequences
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CRAMFileReader createCRAMFileReader(final File cramFile, final File indexFile, final File reference) throws FileNotFoundException, IOException {				

		return createCRAMFileReader( cramFile, indexFile, reference, DefaultStringency);		 
	}
	

	
	/**
	 * main constructor
	 * Create a CRAMFileReader from an input stream and optional index stream using the supplied reference source and validation stringency.
	 * 
	 * @param cramFile - CRAM file to open
	 * @param indexFile - index file to be used for random access
	 * @param reference - a source of reference sequences
	 * @param stringency - Validation stringency to be used when reading
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CRAMFileReader createCRAMFileReader(final File cramFile, final File indexFile, final File reference,  
			final ValidationStringency stringency ) throws FileNotFoundException, IOException {				

		ReferenceSource rs = new ReferenceSource(reference);		
		ValidationStringency str = (null != stringency) ? stringency : DefaultStringency;
		logger.debug("Setting Validation Stringency on CRAMFileReader to: " +  str.name());	
						 
		return new CRAMFileReader(new FileInputStream(cramFile), indexFile, rs, str);
		 
	}
	
	public static CRAMFileReader createCRAMFileReader(final File cramFile, final File index, final File reference, final String stringency) throws FileNotFoundException, IOException {
		ValidationStringency str = DefaultStringency;		 
		if ("lenient".equalsIgnoreCase(stringency)) {
			str =  ValidationStringency.LENIENT ;
		} else if ("strict".equalsIgnoreCase(stringency)) {
			str =  ValidationStringency.STRICT ;		 
		} 		
		return createCRAMFileReader(cramFile, null, reference,  str );
	}
	
	public static CRAMFileReader createCRAMFileReader(final File cramFile, final File reference, final String stringency) throws FileNotFoundException, IOException {
 	
		return createCRAMFileReader(cramFile, null, reference, stringency);
	}
	
	

}
