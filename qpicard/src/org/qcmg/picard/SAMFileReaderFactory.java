/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class SAMFileReaderFactory {
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMFileReaderFactory.class);
	 
	public static SAMFileReader createSAMFileReader(final String bamFile) {
		return createSAMFileReader(new File(bamFile));
	}
	
	/**
	 * Will check to bam header to see if bwa was used to align this bam.
	 * If it was, we will use the SILENT validation stringency
	 * otherwise (and default) is to use STRICT
	 * 
	 * @param bamFile
	 * @return
	 */
	public static SAMFileReader createSAMFileReader(final File bamFile) {
//		ValidationStringency original = SAMFileReader.getDefaultValidationStringency();
//		SAMFileReader reader = null;
//		try {
//			SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
//			reader = new SAMFileReader(bamFile);
//		} finally {
//			SAMFileReader.setDefaultValidationStringency(original);
//		}
//		boolean setToSilent = false;
//		try {
//			SAMFileHeader header = reader.getFileHeader();
//			for (SAMProgramRecord pg : header.getProgramRecords()) {
//				if ("bwa".equalsIgnoreCase(pg.getProgramName())) { 
//					setToSilent = true;
//					break;
//				}
//			}	
//		} finally {
//			reader.close();
//		}
//		ValidationStringency vs = setToSilent ?  ValidationStringency.SILENT : ValidationStringency.STRICT;
		
		// if we have bwa as a program record in the bam header, setup a reader that has a strict/lenient validation stringency
		// otherwise use strict
		
		// default to SILENT
		return createSAMFileReader(bamFile, ValidationStringency.SILENT); 
	}
	
	/**
	 * Will check to bam header to see if bwa was used to align this bam.
	 * If it was, we will use the SILENT validation stringency
	 * otherwise (and default) is to use STRICT
	 * 
	 * @param bamFile
	 * @return
	 */
	public static SAMFileReader createSAMFileReader(final File bamFile, final File indexFile) {
//		ValidationStringency original = SAMFileReader.getDefaultValidationStringency();
//		SAMFileReader reader = null;
//		try {
//			SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
//			reader = new SAMFileReader(bamFile, indexFile);
//		} finally {
//			SAMFileReader.setDefaultValidationStringency(original);
//		}
//		boolean bwaAligned = false;
//		try {
//			SAMFileHeader header = reader.getFileHeader();
//			for (SAMProgramRecord pg : header.getProgramRecords()) {
//				if ("bwa".equalsIgnoreCase(pg.getProgramName())) {
//					bwaAligned = true;
//					break;
//				}
//			}
//		} finally {
//			reader.close();
//		}
//		ValidationStringency vs = ValidationStringency.STRICT;
//		if (bwaAligned) vs = ValidationStringency.SILENT;
		
		// if we have bwa as a program record in the bam header, setup a reader that has a strict/lenient validation stringency
		// otherwise use strict
		
		// default to SILENT 
		
		return createSAMFileReader(bamFile, indexFile, ValidationStringency.SILENT);
	}
	
	public static SAMFileReader createSAMFileReader(final String bamFile, final String stringency) {
		return createSAMFileReader(new File(bamFile), stringency);
	}
	
	public static SAMFileReader createSAMFileReader(final File bamFile, final String stringency) {
				 
			if ("lenient".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.LENIENT);
			} else if ("silent".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.SILENT);
			} else if ("strict".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile, ValidationStringency.STRICT);
			} 
			
			return createSAMFileReader(bamFile);
	}
	
	
	public static SAMFileReader createSAMFileReader(final String bamFile, final ValidationStringency stringency) {
		return createSAMFileReader(new File(bamFile), stringency);
	}
	
	/**
	 * Creates and returns a SAMFileReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SAMFileReader reader for this bam file with this validation stringency
	 */
	public static SAMFileReader createSAMFileReader(final File bamFile, final ValidationStringency stringency) {
		if (null != stringency) {
			ValidationStringency original = SAMFileReader.getDefaultValidationStringency();
			logger.debug("Setting Validation Stringency on SAMFileReader to: " + stringency.toString());
			try {
				SAMFileReader.setDefaultValidationStringency(stringency);
				return new SAMFileReader(bamFile);
			} finally {
				// restore previous stringency level
				SAMFileReader.setDefaultValidationStringency(original);
			}
		}
		
		return createSAMFileReader(bamFile);
	}
	
	/**
	 * Creates and returns a SAMFileReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SAMFileReader reader for this bam file with this validation stringency
	 */
	public static SAMFileReader createSAMFileReader(final File bamFile,final File indexFile, final ValidationStringency stringency) {
		if (null != stringency) {
			ValidationStringency original = SAMFileReader.getDefaultValidationStringency();
			logger.debug("Setting Validation Stringency on SAMFileReader to: " + stringency.toString());
			try {
				SAMFileReader.setDefaultValidationStringency(stringency);
				return new SAMFileReader(bamFile, indexFile);
			} finally {
				// restore previous stringency level
				SAMFileReader.setDefaultValidationStringency(original);
			}
		}
		
		return createSAMFileReader(bamFile, indexFile);
	}
}
