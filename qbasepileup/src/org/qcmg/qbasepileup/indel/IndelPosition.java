/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;


import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qbasepileup.QBasePileupUtil;


public class IndelPosition {
	
	final String chromosome;
	Integer start;
	Integer end;
	int length;
	private final String fullChromosome;
	public static final String DEL = "DEL";
	public static final String INS = "INS";
	public static final String CTX = "CTX";
	private final String inputString;
	private final String mutationType;
	private int tdColumn = -1;
	private int ndColumn = -1;
	private int qcmgFlagColumn = -1;
	private final boolean isGermline;
	private String motif;
	QLogger logger = QLoggerFactory.getLogger(IndelPosition.class);
	
	public IndelPosition(String line, boolean isGermline, int[] dccColumns) {
		String[] values = line.split("\t");

		String mut = switch (values[3]) {
			case "2" -> INS;
			case "3" -> DEL;
			case "4" -> CTX;
			default -> values[3];
		};
		this.qcmgFlagColumn = dccColumns[0];
		this.ndColumn = dccColumns[1];
		this.tdColumn = dccColumns[2];
		this.chromosome = values[4];			
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
		this.mutationType = mut;
		this.isGermline = isGermline;
		this.start = Integer.valueOf(values[5]);
		
		this.end = Integer.valueOf(values[6]);
		this.inputString = line;
		this.motif = getMotif(values[dccColumns[3]], values[dccColumns[4]]);
		this.length = motif.length();		
	}

	private String getMotif(String referenceBases, String tumourBases) {			
		//use tumour bases
		if (isInsertion()) {
			return tumourBases;
		} else if (isDeletion()){
			//reference base since missing in tumour
			return referenceBases;
		} else {
			return "";
		}
	}

	public IndelPosition(String chromosome, Integer start, Integer end, String mutationType, String indelFileType, String line, boolean isGermline) {
		super();
		this.chromosome = chromosome;
		this.fullChromosome = QBasePileupUtil.getFullChromosome(chromosome);
		this.mutationType = mutationType;
		this.isGermline = isGermline;

		if (indelFileType.equals("pindel") || indelFileType.equals("strelka")) {
			
			if (mutationType.equals(INS) || mutationType.equals(CTX)) {
				this.start = start;
				this.end = end;
				this.length = end - start;
			} else if (mutationType.equals(DEL)) {
				//pindel deletions always bracket the deletion so they
				//will never be length 1; min length = 2
				this.start = start + 1;
				this.end = end - 1;
				this.length = end -start + 1;
			}			
		}

		this.inputString = line;
	}	
	
	public String getMutationType() {
		return mutationType;
	}

	public boolean isGermline() {
		return isGermline;
	}

	public void setMotif(String motif) {
		this.motif = motif;
		this.length = motif.length();
	}

	public String getChromosome() {
		return chromosome;
	}
	public int getStart() {
		return start;
	}
	public Integer getStartObject() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	public int getLength() {
		return length;
	}
	
	public String getFullChromosome() {
		return fullChromosome;
	}
	
	@Override
    public boolean equals(final Object o) {
	       
        if (!(o instanceof IndelPosition other)) return false;

		if (chromosome.equals(other.getChromosome())) {
        	if (start.equals(other.getStartObject())) {
        		return end.equals(other.getEnd());
        	} else {
        		return start.equals(other.getStartObject());
        	}
        } else {
        	return chromosome.equals(other.getChromosome());
        }
    }
	    
    @Override
    public int hashCode() {
       return 31*chromosome.hashCode() + start.hashCode() + end.hashCode();
    }

	@Override
	public String toString() {
		return chromosome + ":" + start + "-" + end + ":" + mutationType + ":Length" + length;
	}

	public boolean isDeletion() {
		return DEL.equals(mutationType);
	}	

	public int getTdColumn() {
		return tdColumn;
	}

	public int getNdColumn() {
		return ndColumn;
	}

	public boolean isInsertion() {
		return INS.equals(mutationType);
	}
	public boolean isComplex() {
		return CTX.equals(mutationType);
	}

	public String getInputString() {
		return this.inputString;
	}

	public int getQCMGFlagColumn() {
		return qcmgFlagColumn;
	}

	public byte[] getMotif() {
		return this.motif.getBytes();
	}

	public boolean isSomatic() {
		return ! isGermline;
	}

}
