/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.io.File;

import org.qcmg.common.model.ProfileType;

public class ProfileTypeUtils {
	
	private static final String GFF_EXTENTION = "gff";
	private static final String GFF_EXTENTION_3 = "gff3";
	private static final String FASTA_EXTENTION = "csfasta";
	private static final String QUAL_EXTENTION = "qual";
	private static final String BAM_EXTENTION = "bam";
	private static final String SAM_EXTENTION = "sam";
	private static final String XML_EXTENTION = "xml";
	private static final String MA_EXTENTION = "ma";
	private static final String FASTQ_EXTENTION = "fastq";
	
	public static ProfileType getType(File f) throws Exception {
		String ext = null;
		final String s = f.getName();
	    final int i = s.lastIndexOf('.');
	    
	    if ( ! f.isDirectory() && i > 0 &&  i < s.length() - 1)
		    	ext = s.substring(i+1).toLowerCase();
	    
	    if (BAM_EXTENTION.equals(ext) || SAM_EXTENTION.equals(ext))
	    	return ProfileType.BAM;
	    if (FASTA_EXTENTION.equals(ext))
	    	return ProfileType.FASTA;
	    if (QUAL_EXTENTION.equals(ext))
	    	return ProfileType.QUAL;
	    if (XML_EXTENTION.equals(ext))
	    	return ProfileType.XML;
	    if (GFF_EXTENTION.equals(ext) || GFF_EXTENTION_3.equals(ext))
	    	return ProfileType.GFF;
	    if (MA_EXTENTION.equals(ext))
	    	return ProfileType.MA;
	    if (FASTQ_EXTENTION.equals(ext))
	    	return ProfileType.FASTQ;
	    
	    throw new Exception("Unsupported file type "+ ext);
	}

}
