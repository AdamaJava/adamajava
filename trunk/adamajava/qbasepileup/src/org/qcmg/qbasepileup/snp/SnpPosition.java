/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.picard.ReferenceUtils;
import org.qcmg.qbasepileup.QBasePileupException;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class SnpPosition {
	
	public static final char TAB = '\t';
	
	
	private final ChrPosition chrPos;
	private final String fullChromosome;
	private final String inputLine;
	
	private byte[] referenceBases;
	private byte[] altBases;
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end) {
		this(name, chromosome, start, end, null);
	}
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end, String line) {
		super();
		this.chrPos = new ChrPosition(chromosome, start, end, name);
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
		this.inputLine = line;
	}
	
	public String getName() {
		return chrPos.getName();
	}

	public String getChromosome() {
		return  chrPos.getChromosome();
	}
	public int getStart() {
		return chrPos.getPosition();
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
		return true;
	}

	public String toTabString() {
		return getName() + TAB + getChromosome() + TAB + getStart() + TAB + getEnd();		
	}
	
	public String toOutputColumnsString() {
		return toTabString() + TAB +  String.valueOf(referenceBases) + TAB + String.valueOf(altBases);
	}
	
	@Override
	public String toString() {
		return getChromosome() + ":" + getStart() + "-" + getEnd();
	}

	public void retrieveReferenceBases(File refFile) throws QBasePileupException, IOException {
//		public void retrieveReferenceBases(IndexedFastaSequenceFile indexedFasta, FastaSequenceIndex index) throws QBasePileupException, IOException {
		
		
		referenceBases = ReferenceUtils.getReferenceBases(refFile, fullChromosome,  getStart(), getEnd());	
		
//		if (index.hasIndexEntry(fullChromosome)) {
//			referenceBases = indexedFasta.getSubsequenceAt(fullChromosome, getStart() , getEnd() ).getBases();	
////		} else if (fullChromosome.equals("chrM")) {
//////			fullChromosome = "chrMT";
////			retrieveReferenceBases(indexedFasta, index);
//		} else {
//			throw new QBasePileupException("UNKNOWN_CHROMOSOME", fullChromosome);
//		}
		
	}

	public String getInputLine() {
		return this.inputLine;
	}
}
