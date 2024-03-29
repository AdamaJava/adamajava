/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.picard.ReferenceUtils;
import org.qcmg.qbasepileup.QBasePileupUtil;

import java.io.File;

public class SnpPosition {
	
	public static final char TAB = '\t';
	
	
	private final ChrPosition chrPos;
	private final String name;
	private final String fullChromosome;
	private final String inputLine;
	
	private byte[] referenceBases;
	private byte[] altBases;
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end) {
		this(name, chromosome, start, end, null);
	}
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end, String line) {
		super();
		this.chrPos = new ChrRangePosition(chromosome, start, end);
		this.name = name;
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
		this.inputLine = line;
	}
	
	public String getName() {
		return name;
	}

	public String getChromosome() {
		return  chrPos.getChromosome();
	}
	public int getStart() {
		return chrPos.getStartPosition();
	}
	public byte[] getAltBases() {
		return altBases;
	}

	public void setAltBases(byte[] altBases) {
		this.altBases = altBases;
	}

	public int getEnd() {
		return chrPos.getEndPosition();
	}
	public int getLength() {
		return chrPos.getLength();
	}
	public byte[] getReferenceBases() {
		return referenceBases;
	}
	public String getFullChromosome() {
		return fullChromosome;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chrPos == null) ? 0 : chrPos.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SnpPosition other = (SnpPosition) obj;
		if (chrPos == null) {
			if (other.chrPos != null)
				return false;
		} else if (!chrPos.equals(other.chrPos))
			return false;
		if (name == null) {
			return other.name == null;
		} else return name.equals(other.name);
	}

	public String toTabString() {
		return getName() + TAB + getChromosome() + TAB + getStart() + TAB + getEnd();		
	}
	
	public String toOutputColumnsString() {
		return toTabString() + TAB +  new String(referenceBases) + TAB + new String(altBases);
	}
	
	@Override
	public String toString() {
		return getChromosome() + ":" + getStart() + "-" + getEnd();
	}

	public void retrieveReferenceBases(File refFile) {
		referenceBases = ReferenceUtils.getReferenceBases(refFile, fullChromosome,  getStart(), getEnd());	
	}

	public String getInputLine() {
		return this.inputLine;
	}
}
