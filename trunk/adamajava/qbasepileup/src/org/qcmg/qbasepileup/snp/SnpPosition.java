/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.io.IOException;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.qcmg.qbasepileup.QBasePileupException;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class SnpPosition {
	
	public static final char TAB = '\t';
	
	private String chromosome;
	private Integer start;
	private Integer end;
	private int length;
	private String name;
	private byte[] referenceBases;
	private String fullChromosome;
	private byte[] altBases;
	private String inputLine;
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end) {
		this(name, chromosome, start, end, null);
	}
	
	public SnpPosition(String name, String chromosome, Integer start, Integer end, String line) {
		super();
		this.chromosome = chromosome;
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
		this.start = start;		
		this.end = end;
		this.length = end - start + 1;
		this.name = name;	
		this.inputLine = line;
	}
	
	/**
	 * Only called by test classes
	 * 
	 * @param line
	 * @param columns
	 * @throws QBasePileupException
	 */
	public SnpPosition(String line, Integer[] columns) throws QBasePileupException {
		super();
		try {
			String[] values = line.split("\t");
			this.name = values[0];
			if (columns.length == 1) {			
				String range = values[columns[0]];
				this.chromosome = range.split(":")[0];
				String pos = range.split(":")[1];
				this.start = new Integer(pos.split("-")[0].trim());
				this.end = new Integer(pos.split("-")[1].trim());	
			} else {
				this.chromosome = values[columns[0]];
				this.start = new Integer(values[columns[1]].trim());
				this.end = new Integer(values[columns[2]].trim());
			}	
			this.length = end-start + 1;
		} catch (Exception e) {
			throw new QBasePileupException("POSITION_ERROR", line);
		}
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
	}
	
	public String getName() {
		return name;
	}

	public String getChromosome() {
		return chromosome;
	}
	public int getStart() {
		return start.intValue();
	}
	public byte[] getAltBases() {
		return altBases;
	}

	public void setAltBases(byte[] altBases) {
		this.altBases = altBases;
	}

	public int getEnd() {
		return end.intValue();
	}
	public int getLength() {
		return length;
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
		result = prime * result
				+ ((chromosome == null) ? 0 : chromosome.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
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
		if (chromosome == null) {
			if (other.chromosome != null)
				return false;
		} else if (!chromosome.equals(other.chromosome))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	public String toTabString() {
		return name + TAB + chromosome + TAB + start + TAB + end;		
	}
	
	public String toOutputColumnsString() {
		return name + TAB + chromosome + TAB + start + TAB 
				+ end + TAB +  String.valueOf(referenceBases) + TAB + String.valueOf(altBases);
//		return inputLine.split("\t")[0] + "\t" + chromosome + "\t" + start + "\t" 
//		+ end + "\t" + new String(referenceBases) + "\t" + new String(altBases);				
	}
	
	@Override
	public String toString() {
		return chromosome + ":" + start + "-" + end;
	}

	public void retrieveReferenceBases(IndexedFastaSequenceFile indexedFasta, FastaSequenceIndex index) throws QBasePileupException, IOException {
		
		if (index.hasIndexEntry(fullChromosome)) {
			referenceBases = indexedFasta.getSubsequenceAt(fullChromosome, start, end).getBases();	
		} else if (fullChromosome.equals("chrM")) {
			fullChromosome = "chrMT";
			retrieveReferenceBases(indexedFasta, index);
		} else {
			throw new QBasePileupException("UNKNOWN_CHROMOSOME", fullChromosome);
		}
		
	}

	public String getInputLine() {
		return this.inputLine;
	}
}
