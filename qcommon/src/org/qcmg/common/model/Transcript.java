/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.List;

public class Transcript implements Comparable<Transcript> {
	
	private final String id;
	private final String type;	// could be enum...
	private final String gene;
	private final List<ChrPosition> exons = new ArrayList<>(2);
	private final List<ChrPosition> cdss = new ArrayList<>(2);
	private final String contig;
	private int start = Integer.MAX_VALUE;
	private int end = Integer.MIN_VALUE;
	
	public Transcript(String id, String type, String contig, String gene) {
		this.id = id;
		this.type = type;
		this.contig = contig;
		this.gene = gene;
	}
	
	public void addExon(ChrPosition exon) {
		exons.add(exon);
		setStartStop(exon);
	}
	public void addCDS(ChrPosition cds) {
		cdss.add(cds);
		setStartStop(cds);
	}
	
	public String getGene() {
		return gene;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	public String getId() {
		return id;
	}
	public String getType() {
		return type;
	}
	
	private void setStartStop(ChrPosition cp) {
//		if (start > cp.getPosition()) {
//			start = cp.getPosition();
//		}
		
		if (start > cp.getStartPosition()) {
			start = cp.getStartPosition();
		}		
		
		if (end <  cp.getEndPosition()) {
			end = cp.getEndPosition();
		}
	}

	public List<ChrPosition> getExons() {
		return exons;
	}
	public List<ChrPosition> getCDSs() {
		return cdss;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transcript other = (Transcript) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int compareTo(Transcript o) {
		return start - o.start;
	}

	public String getContig() {
		return contig;
	}

	@Override
	public String toString() {
		return "Transcript [id=" + id + ", type=" + type + ", gene=" + gene
				+ ", exons=" + exons + ", cdss=" + cdss + ", contig=" + contig
				+ ", start=" + start + ", end=" + end + "]";
	}

	
}
