/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.io.IOException;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.qcmg.qbasepileup.QBasePileupException;
import org.qcmg.qbasepileup.QBasePileupUtil;

public class SnpPosition {
	
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
//	public void setEnd(int end) {
//		this.end = end;
//	}
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
    public boolean equals(final Object o) {
	       
        if (!(o instanceof SnpPosition)) return false;
        
        final SnpPosition other = (SnpPosition) o;
        
        if (chromosome.equals(other.getChromosome())) {
        	if (start.equals(other.getStart())) {
        		return end.equals(other.getEnd());
        	} else {
        		return start.equals(other.getStart());
        	}
        } else {
        	return chromosome.equals(other.getChromosome());
        }
    }
	    
    @Override
    public int hashCode() {
       return 31*chromosome.hashCode() + start.hashCode() + end.hashCode();
    }

	public String toTabString() {
		return name + "\t" + chromosome + "\t" + start + "\t" + end;		
	}
	
	public String toOutputColumnsString() {
		return inputLine.split("\t")[0] + "\t" + chromosome + "\t" + start + "\t" 
				+ end + "\t" + new String(referenceBases) + "\t" + new String(altBases);				
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
