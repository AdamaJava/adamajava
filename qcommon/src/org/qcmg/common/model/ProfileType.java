/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;


//? need further test whether upcase or lowcase
public enum ProfileType {
	gff("gff"), 
	FASTA("fasta"), 
	QUAL("qual"), 
	bam("bam"),
	XML("XML"),
	MA("MA"),
	FA("FA"),	// genome files - hopefully...
	FASTQ("FASTQ"),
	VCF("vcf");
	
	 
	private String reportName;	
	private ProfileType(String name) { this.reportName = name; }	
	public String getReportName() { return reportName; }
	
	 
	
}
