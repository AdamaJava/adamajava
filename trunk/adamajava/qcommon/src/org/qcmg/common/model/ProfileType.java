package org.qcmg.common.model;

public enum ProfileType {
	GFF("GFF"), 
	FASTA("FASTA"), 
	QUAL("QUAL"), 
	BAM("BAM"),
	XML(""),
	MA("MA"),
	FASTQ("FASTQ");
	
	private String reportName;
	
	private ProfileType(String name) {
		this.reportName = name;
	}
	
	public String getReportName() {
		return reportName;
	}
}
