/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.io.File;

import org.qcmg.common.model.ProfileType;

public class ProfileTypeUtils {
	
	private static final String GFF_EXTENSION = "gff";
	private static final String GFF_EXTENSION_3 = "gff3";
	private static final String FASTA_EXTENSION = "csfasta";
	private static final String QUAL_EXTENSION = "qual";
	private static final String BAM_EXTENSION = "bam";
	private static final String SAM_EXTENSION = "sam";
	private static final String XML_EXTENSION = "xml";
	private static final String MA_EXTENSION = "ma";
	private static final String FASTQ_EXTENSION = "fastq";
	private static final String FQ_EXTENSION = "fq";
	private static final String FASTQ_GZ_EXTENSION = "fastq.gz";
	private static final String FQ_GZ_EXTENSION = "fq.gz";
	private static final String FA_EXTENSION = "fa";
	private static final String GZ_EXTENSION = "gz";
	private static final String VCF_EXTENSION = "vcf";
	private static final String VCF_GZ_EXTENSION = "vcf.gz";
	
	public static ProfileType getType(String s) {
		String ext = null;
//		final String s = f.getName();/
	    int i = s.lastIndexOf('.');
	    
	    if ( i > 0 &&  i < s.length() - 1) {
	    	ext = s.substring(i+1).toLowerCase();
	    }
//	    if ( ! f.isDirectory() && i > 0 &&  i < s.length() - 1) {
//	    	ext = s.substring(i+1).toLowerCase();
//	    }
	    
	    
	    // if ext is equal to gz, then find the previous extension
	    if (GZ_EXTENSION.equals(ext)) {
	    	i = s.lastIndexOf('.', i -1);
	    	ext = s.substring(i+1).toLowerCase();
	    }
	    	    
	    if (BAM_EXTENSION.equals(ext) || SAM_EXTENSION.equals(ext))
	    	return ProfileType.BAM;
	    if (FASTA_EXTENSION.equals(ext))
	    	return ProfileType.FASTA;
	    if (QUAL_EXTENSION.equals(ext))
	    	return ProfileType.QUAL;
	    if (XML_EXTENSION.equals(ext))
	    	return ProfileType.XML;
	    if (GFF_EXTENSION.equals(ext) || GFF_EXTENSION_3.equals(ext))
	    	return ProfileType.GFF;
	    if (MA_EXTENSION.equals(ext))
	    	return ProfileType.MA;
	    if (FASTQ_EXTENSION.equals(ext) || FQ_EXTENSION.equals(ext))
	    	return ProfileType.FASTQ;
	    if (FASTQ_GZ_EXTENSION.equals(ext) || FQ_GZ_EXTENSION.equals(ext))
	    	return ProfileType.FASTQ;
	    if (FA_EXTENSION.equals(ext))
	    	return ProfileType.FA;	    
	    if(VCF_EXTENSION.equals(ext) || VCF_GZ_EXTENSION.equals(ext)) 
	    	return ProfileType.VCF; 
	    
	    throw new IllegalArgumentException("Unsupported file type "+ ext);
	}

}
