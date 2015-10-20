/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.util.EnumSet;

import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class SAMFileReaderFactory {
	EnumSet<Option> defultOption = SamReaderFactory.Option.DEFAULTS;
	static ValidationStringency DefaultStringency = ValidationStringency.SILENT;
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMFileReaderFactory.class);
	 
//	public static SamReader createSAMFileReader(final String bamFile) {
//	//	return createSAMFileReader(new File(bamFile));
//		
//		return createSAMFileReader(new File(bamFile), null, ValidationStringency.SILENT, null);
//	}
	
	/**
	 * Will check to bam header to see if bwa was used to align this bam.
	 * If it was, we will use the SILENT validation stringency
	 * otherwise (and default) is to use STRICT
	 * 
	 * @param bamFile
	 * @return
	 */
	public static SamReader createSAMFileReader(final File bamFile) {
		return createSAMFileReader(bamFile,null, ValidationStringency.SILENT, null); 
	}
	
	/**
	 * Will check to bam header to see if bwa was used to align this bam.
	 * If it was, we will use the SILENT validation stringency
	 * otherwise (and default) is to use STRICT
	 * 
	 * @param bamFile
	 * @return
	 */
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile) { 	
		return createSAMFileReader(bamFile, indexFile, null, null);
	}
	
//	public static SamReader createSAMFileReader(final String bamFile, final String stringency) {
//		return createSAMFileReader(new File(bamFile), stringency);
//	}
	
	public static SamReader createSAMFileReader(final File bamFile, final String stringency) {
				 
			if ("lenient".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.LENIENT);
			} else if ("silent".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.SILENT);
			} else if ("strict".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.STRICT);
			} 
			
			return createSAMFileReader(bamFile);
	}
	
	
//	public static SamReader createSAMFileReader(final String bamFile, final ValidationStringency stringency) {
//		return createSAMFileReader(new File(bamFile), stringency);
//	}
	
	/**
	 * Creates and returns a SamReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SamReader reader for this bam file with this validation stringency
	 */
	public static SamReader createSAMFileReader(final File bamFile, final ValidationStringency stringency) {
		
		 return createSAMFileReader(bamFile, null, stringency, null);
	}
	
	/**
	 * Creates and returns a SamReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SamReader reader for this bam file with this validation stringency
	 */
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile, final ValidationStringency stringency) {
		return createSAMFileReader(bamFile, indexFile, stringency, null);
	}
	
	public static SamReader createSAMFileReader(final File bamFile, final SamReaderFactory.Option... options) {
		return createSAMFileReader(bamFile, null, null, options);
	}
	
	
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile,  final ValidationStringency stringency, final Option...options ) {				

		SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();		
		
		if( options != null && options.length > 0)
			samReaderFactory = SamReaderFactory.makeDefault().enable(options);
		
		
		// only apply validationStringency(stringency) rather than static method setDefaultValidationStringency(stringency) for instance don't apply 
		//you can only call the static method before create the instanct 
		if (null != stringency) 
			samReaderFactory.validationStringency(stringency);
		else
			samReaderFactory.validationStringency(DefaultStringency);		 
		logger.debug("Setting Validation Stringency on SamReader to: " + samReaderFactory.validationStringency().name());	
			
		
		SamInputResource resource = SamInputResource.of(bamFile);
		if(indexFile != null)
			resource = SamInputResource.of(bamFile).index(indexFile);
		
		return samReaderFactory.open(resource);
	}



}
