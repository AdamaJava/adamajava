/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;


//? need further test whether upcase or lowcase
public enum ProfileType {
	
	BAM("bam"),
	XML("xml"),
	FASTQ("fastq"),
	FASTA("fasta"), 	
	VCF("vcf"),

	//below only used on qprofiler1.0
	GFF("gff"), 
	MA("ma"),
	QUAL("qual"), 
	FA("fa");	// genome files - hopefully...	

	private String reportName;	
	private ProfileType(String name) { this.reportName = name; }	
	public String getReportName() { return reportName; }	 	
}
