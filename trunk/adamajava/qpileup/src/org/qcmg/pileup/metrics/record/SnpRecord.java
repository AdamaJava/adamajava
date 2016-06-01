/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.pileup.PileupConstants;


public class SnpRecord extends MetricRecord {	
	
	char referenceBase;
	String id;
	long totalReferenceBases = 0;
	long totalNonReferenceBases = 0;
	private String baseCountString;
	private long highNonRef = 0;
	String analysisId;
	private char dbSnpStrand;
	private char altBase;
	private String mutation;
	private String dccBaseCountString;	
	private String comparisonFileAnnotation = new String();
	private GenotypeEnum dbSnpGenotype;
	private String dbSnpId;
	private GenotypeEnum normalGenotype;
	private GenotypeEnum tumourGenotype;
	private int dbSNPAltLengh;
	private int nearestNeighbour;
	private String germdb;
	private long totalBases;
	private boolean strandBias = false;
	private boolean inMismapRegion = false;
	private String compareSnpString = "";
	private int altBaseCount;
	protected final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	private String dbSNPMaf;
	private int totalPatients;
	
	public SnpRecord(String chromosome, int position, char refBase, int totalReads) {
		super(PileupConstants.METRIC_SNP,chromosome, position, 1, totalReads);
	}
	
	public SnpRecord(String chromosome, int position, char refBase, String baseCountString, char altBase, String analysisId, int totalReads, int totalBases, int totalPatients) {
		super(PileupConstants.METRIC_SNP,chromosome, position, 1, totalReads);
		this.referenceBase= refBase;
		this.baseCountString = baseCountString;
		this.analysisId = analysisId;
		this.altBase = altBase;
		this.totalBases = totalBases;
		this.mutation = referenceBase + ">" + altBase;	
		this.nearestNeighbour = 0;
		this.totalPatients = totalPatients;
	}



	public char getReferenceBase() {
		return referenceBase;
	}
	
	public void setReferenceBase(char referenceBase) {
		this.referenceBase = referenceBase;
	}
	
	public String getChromosome() {
		return chromosome;
	}
	
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	
	public Integer getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	
	public String getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(String analysisId) {
		this.analysisId = analysisId;
	}

	public char getDbSnpStrand() {
		return dbSnpStrand;
	}

	public void setDbSnpStrand(char dbSnpStrand) {
		this.dbSnpStrand = dbSnpStrand;
	}

	public String getMutation() {
		return mutation;
	}

	public void setMutation(String mutation) {
		this.mutation = mutation;
	}

	public String getDccBaseCountString() {
		return dccBaseCountString;
	}

	public void setDccBaseCountString(String dccBaseCountString) {
		this.dccBaseCountString = dccBaseCountString;
	}

	public String getComparisonFileAnnotation() {
		return comparisonFileAnnotation;
	}

	public void setComparisonFileAnnotation(String comparisonFileAnnotation) {
		this.comparisonFileAnnotation = comparisonFileAnnotation;
	}

	public GenotypeEnum getDbSnpGenotype() {
		return dbSnpGenotype;
	}

	public void setDbSnpGenotype(GenotypeEnum dbSnpGenotype) {
		this.dbSnpGenotype = dbSnpGenotype;
	}

	public String getDbSnpId() {
		return dbSnpId;
	}

	public void setDbSnpId(String dbSnpId) {
		this.dbSnpId = dbSnpId;
	}

	public GenotypeEnum getNormalGenotype() {
		return normalGenotype;
	}

	public void setNormalGenotype(GenotypeEnum normalGenotype) {
		this.normalGenotype = normalGenotype;
	}

	public GenotypeEnum getTumourGenotype() {
		return tumourGenotype;
	}

	public void setTumourGenotype(GenotypeEnum tumourGenotype) {
		this.tumourGenotype = tumourGenotype;
	}

	public long getHighNonRef() {
		return highNonRef;
	}

	public char getAltBase() {
		return altBase;
	}

	public long getTotalReferenceBases() {
		return totalReferenceBases;
	}

	public void setTotalReferenceBases(long totalReferenceBases) {
		this.totalReferenceBases = totalReferenceBases;
	}

	public long getTotalNonReferenceBases() {
		return totalNonReferenceBases;
	}

	public void setTotalNonReferenceBases(long totalNonReferenceBases) {
		this.totalNonReferenceBases = totalNonReferenceBases;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public int getNearestNeighbour() {
		return nearestNeighbour;
	}
	
	public boolean isStrandBias() {
		return strandBias;
	}

	public boolean isInMismapRegion() {
		return inMismapRegion;
	}

	public void setNearestNeighbour(int nearestNeighbour) {		
		this.nearestNeighbour = nearestNeighbour;
		getNearestNeighbourDistance();
	}
	
	public String getNearestNeighbourDistance() {
		if (nearestNeighbour != 0) {		
			return new String("" + (Math.abs(position - nearestNeighbour)));
		}
		return new String();
	}
	
	public String getGermdb() {
		return germdb;
	}

	public void setGermdb(String germdb) {
		this.germdb = germdb;
	}	

	private String getEstimatedGMAF() {
		DecimalFormat df = new DecimalFormat("##.##");
		if (highNonRef > 0) {
			return df.format((double) highNonRef / (double) totalPatients * 100 /2);
		}
		return new String("0.0");
	}	

	private String getAlleleFrequencyString() {
		DecimalFormat dc = new DecimalFormat("##.##");
		return dc.format(getAlleleFrequency());
	}

	private String getChromosomeNumber() {
		return chromosome.replace("chr", "");
	}

	private String getXRef() {
		return 1 == getStatus() ? dbSnpId : "-999";
	}

	private String getValidPlatform() {
		return "-888";
	}

	private String getValidStatus() {		
		return "2";			
	}

	private int getStatus() {
		return null != dbSnpId ? 1 : 2;
	}

	public void setHighNonRef(long highNonRef) {
		this.highNonRef  = highNonRef;		
	}
	
	public String getBaseCountString() {
		return baseCountString;
	}

	public void setBaseCountString(String baseCountString) {
		this.baseCountString = baseCountString;
	}

	public void setAltBase(char altBase) {
		this.altBase = altBase;	
	}

	public void setDCCBaseCountString(String dccBaseCountString) {
		this.dccBaseCountString = dccBaseCountString;
	}

	public String getKey() {
		return chromosome + "," + position;
	}

	public ChrRangePosition getChrPosition() {
		return new ChrRangePosition(chromosome, position, position);
	}

	public void setDbSnpAltLength(int dbSNPAltLengh) {
		this.dbSNPAltLengh = dbSNPAltLengh;		
	}

	public int getDbSnpAltLength() {
		return this.dbSNPAltLengh;
	}

	public void setTotalBases(long totalBases) {
		this.totalBases = totalBases;		
	}

	public void setStrandBias(boolean b) {
		this.strandBias  = true;		
	}

	public void setInMismapRegion(boolean inMismapRegion) {
		this.inMismapRegion  = inMismapRegion;		
	}
	
	public void setAltBaseCount(int altBases) {
		this.altBaseCount = altBases;		
	}

	public void setdbSNPMAF(String s) {
		this.dbSNPMaf = s;		
	}
	
	public int getAltBaseCount() {
		return altBaseCount;
	}

	public double getAlleleFrequency() {
		if (altBaseCount > 0 && totalBases > 0) {
			
			return ((double) altBaseCount / (double) totalBases) * 100;
		} else {
			return 0;
		}
	}
	
	public String getNotes(boolean isDCC) {
		StringBuilder sb = new StringBuilder();
		List<String> notes = new ArrayList<String>();
		
		if (dbSnpId != null && !isDCC) {			
			notes.add("dbSNP=" + dbSnpId);
		}
		if (dbSNPMaf != null) {
			notes.add(dbSNPMaf);
		}
		if (germdb != null) {
			notes.add(germdb);
		}
		if  (!comparisonFileAnnotation.equals("")) {
			notes.add(comparisonFileAnnotation);
		}
		if  (nearestNeighbour != 0) {
			notes.add("NN:" + nearestNeighbour);
		}
		if (strandBias) {
			notes.add("SBIAS");
		} 
		if (inMismapRegion) {
			notes.add("MISMAP");
		} 

		for (int i=0; i<notes.size(); i++) {
			sb.append(notes.get(i));
			if (i != notes.size() -1 ) {
				sb.append(";");
			}
		}
		return sb.toString(); 
	}


	public String toTabString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.chromosome + TAB_DELIMITER + this.position + TAB_DELIMITER + this.referenceBase + TAB_DELIMITER + this.altBase 
				+ TAB_DELIMITER + totalReferenceBases + TAB_DELIMITER + totalNonReferenceBases);
		sb.append(TAB_DELIMITER + altBaseCount);
		sb.append(TAB_DELIMITER);
		sb.append(baseCountString);
		sb.append(dccBaseCountString + TAB_DELIMITER + getNotes(false) + TAB_DELIMITER + highNonRef + TAB_DELIMITER + getEstimatedGMAF() + TAB_DELIMITER + "");
		sb.append(TAB_DELIMITER + getAlleleFrequencyString() + TAB_DELIMITER + compareSnpString); 
		sb.append("\n");
		return sb.toString();
	}

	public String toDCCString() {		
		StringBuilder sb = new StringBuilder();		
		
		sb.append(this.analysisId).append(TAB_DELIMITER);
		sb.append(".").append(TAB_DELIMITER);
		sb.append(this.id).append(TAB_DELIMITER);
		sb.append("1").append(TAB_DELIMITER);
		sb.append(getChromosomeNumber()).append(TAB_DELIMITER);
		sb.append(this.position).append(TAB_DELIMITER);
		sb.append(this.position).append(TAB_DELIMITER);
		sb.append("1").append(TAB_DELIMITER);
		sb.append(null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888").append(TAB_DELIMITER);
		sb.append(('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))).append(TAB_DELIMITER);
		sb.append(referenceBase).append(TAB_DELIMITER);
		sb.append(null != normalGenotype ? normalGenotype.getDisplayString() : "--").append(TAB_DELIMITER);
		sb.append(null != tumourGenotype ? tumourGenotype.getDisplayString() : "--").append(TAB_DELIMITER);
		sb.append(mutation).append(TAB_DELIMITER);
		sb.append("-999").append(TAB_DELIMITER);
		sb.append("-999").append(TAB_DELIMITER);
		sb.append("-999").append(TAB_DELIMITER);
		sb.append(totalBases).append(TAB_DELIMITER);
		sb.append(getStatus()).append(TAB_DELIMITER);
		sb.append(getValidStatus()).append(TAB_DELIMITER);
		sb.append(getValidPlatform()).append(TAB_DELIMITER);
		sb.append(getXRef()).append(TAB_DELIMITER);		
		sb.append("-999").append(TAB_DELIMITER);		
		sb.append(getNotes(true)).append(TAB_DELIMITER);
		sb.append(dccBaseCountString).append(TAB_DELIMITER).append(TAB_DELIMITER).append(TAB_DELIMITER).append(TAB_DELIMITER);
		sb.append(highNonRef).append(TAB_DELIMITER);
		sb.append(getEstimatedGMAF()).append(TAB_DELIMITER);
		sb.append("").append(TAB_DELIMITER);
		sb.append(getAlleleFrequencyString()).append(TAB_DELIMITER);		
		sb.append(compareSnpString).append(TAB_DELIMITER);
		sb.append("\n");
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.chromosome + TAB_DELIMITER + this.position + TAB_DELIMITER + this.referenceBase + TAB_DELIMITER + this.altBase 
				+ TAB_DELIMITER + totalReferenceBases + TAB_DELIMITER + totalNonReferenceBases);
		sb.append(TAB_DELIMITER);
		sb.append(baseCountString);
		sb.append(TAB_DELIMITER);
		sb.append(highNonRef + TAB_DELIMITER);
		sb.append(dccBaseCountString + TAB_DELIMITER);
		sb.append(normalGenotype  + TAB_DELIMITER);
		sb.append(altBaseCount + TAB_DELIMITER + isStrandBias());
		sb.append("\n");
		
		return sb.toString();
	}

}
