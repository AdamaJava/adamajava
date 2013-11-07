package org.qcmg.illumina;

import org.qcmg.common.string.StringUtils;
import org.qcmg.record.Record;

public class IlluminaRecord implements Record {
	
	private String chr;
	private int start;
	private final String strand;
	private String snpId;
	
	//TODO do we need this field?
	private float GCScore;
	
	private char firstAllele;
	private char secondAllele;
	
	private final char firstAlleleForward;
	private final char secondAlleleForward;
	
	private final char firstAlleleCall;
	private final char secondAlleleCall;
	
	//TODO do we need this field?
	private boolean hom;
	private boolean isSnp;
	private String snp;
	
	private final float logRRatio;
	private final float bAlleleFreq;
	
	private final int rawX;
	private final int rawY;
	
	/**
	 * Constructor that takes in a String array, retrieving pertinent fields from the array to populate the record
	 * 
	 * @param rawIlluminaData String[] representing a line in the raw Illumina data file
	 */
	public IlluminaRecord(String [] rawIlluminaData) {
		// chromosome and position defined in the raw Illumina data file relate to an old version
		// of the genome (hg18), so instead, we use the dbSNP id to get the more recent 
		//(hg19) chromosome and position details from the dbSNP file at a later date
		int length = rawIlluminaData.length;
		snpId = rawIlluminaData[0];
		GCScore = Float.parseFloat(rawIlluminaData[4]);
		firstAlleleForward = rawIlluminaData[10].charAt(0);
		secondAlleleForward = rawIlluminaData[11].charAt(0);
		firstAllele = rawIlluminaData[12].charAt(0);
		secondAllele = rawIlluminaData[13].charAt(0);
		setHom(rawIlluminaData[14].equals(rawIlluminaData[15]));
		chr = rawIlluminaData[16];
		start = Integer.parseInt(rawIlluminaData[17]);
		snp = rawIlluminaData[20];
		rawX = Integer.parseInt(rawIlluminaData[length - 4]);
		rawY = Integer.parseInt(rawIlluminaData[length - 3]);
		bAlleleFreq = Float.parseFloat(rawIlluminaData[length - 2]);
		String logRRatioString = rawIlluminaData[length - 1];
		if (StringUtils.isNullOrEmpty(logRRatioString))
			logRRatioString = "NaN";
		logRRatio = Float.parseFloat(logRRatioString);
		firstAlleleCall = rawIlluminaData[14].charAt(0);
		secondAlleleCall = rawIlluminaData[15].charAt(0);
		strand = rawIlluminaData[22];		// use customer strand rather than illumina strand
//		strand = rawIlluminaData[21];
	}
	
	
	public String getChr() {
		return chr;
	}
	public void setChr(String chr) {
		this.chr = chr;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public String getSnpId() {
		return snpId;
	}
	public void setSnpId(String snpId) {
		this.snpId = snpId;
	}
	public float getGCScore() {
		return GCScore;
	}
	public void setGCScore(float GCScore) {
		this.GCScore = GCScore;
	}
	public char getFirstAllele() {
		return firstAllele;
	}
	public void setFirstAllele(char firstAllele) {
		this.firstAllele = firstAllele;
	}
	public char getSecondAllele() {
		return secondAllele;
	}
	public void setSecondAllele(char secondAllele) {
		this.secondAllele = secondAllele;
	}
	public String getSnp() {
		return snp;
	}
	public void setSnp(String snp) {
		this.snp = snp;
	}
	public void setHom(boolean hom) {
		this.hom = hom;
	}
	public boolean isHom() {
		return hom;
	}

	public void setSnp(boolean isSnp) {
		this.isSnp = isSnp;
	}
	
	public boolean isSnp() {
		return isSnp;
	}

	public float getLogRRatio() {
		return logRRatio;
	}


	public float getbAlleleFreq() {
		return bAlleleFreq;
	}


	public char getFirstAlleleCall() {
		return firstAlleleCall;
	}


	public char getSecondAlleleCall() {
		return secondAlleleCall;
	}

	public int getRawX() {
		return rawX;
	}

	public int getRawY() {
		return rawY;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(GCScore);
		result = prime * result + Float.floatToIntBits(bAlleleFreq);
		result = prime * result + ((chr == null) ? 0 : chr.hashCode());
		result = prime * result + firstAllele;
		result = prime * result + firstAlleleCall;
		result = prime * result + (hom ? 1231 : 1237);
		result = prime * result + (isSnp ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(logRRatio);
		result = prime * result + rawX;
		result = prime * result + rawY;
		result = prime * result + secondAllele;
		result = prime * result + secondAlleleCall;
		result = prime * result + ((snp == null) ? 0 : snp.hashCode());
		result = prime * result + ((snpId == null) ? 0 : snpId.hashCode());
		result = prime * result + start;
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
		IlluminaRecord other = (IlluminaRecord) obj;
		if (Float.floatToIntBits(GCScore) != Float
				.floatToIntBits(other.GCScore))
			return false;
		if (Float.floatToIntBits(bAlleleFreq) != Float
				.floatToIntBits(other.bAlleleFreq))
			return false;
		if (chr == null) {
			if (other.chr != null)
				return false;
		} else if (!chr.equals(other.chr))
			return false;
		if (firstAllele != other.firstAllele)
			return false;
		if (firstAlleleCall != other.firstAlleleCall)
			return false;
		if (hom != other.hom)
			return false;
		if (isSnp != other.isSnp)
			return false;
		if (Float.floatToIntBits(logRRatio) != Float
				.floatToIntBits(other.logRRatio))
			return false;
		if (rawX != other.rawX)
			return false;
		if (rawY != other.rawY)
			return false;
		if (secondAllele != other.secondAllele)
			return false;
		if (secondAlleleCall != other.secondAlleleCall)
			return false;
		if (snp == null) {
			if (other.snp != null)
				return false;
		} else if (!snp.equals(other.snp))
			return false;
		if (snpId == null) {
			if (other.snpId != null)
				return false;
		} else if (!snpId.equals(other.snpId))
			return false;
		if (start != other.start)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "IlluminaRecord [GCScore=" + GCScore + ", bAlleleFreq="
				+ bAlleleFreq + ", chr=" + chr + ", firstAllele=" + firstAllele
				+ ", firstAlleleCall=" + firstAlleleCall + ", hom=" + hom
				+ ", isSnp=" + isSnp + ", logRRatio=" + logRRatio + ", rawX="
				+ rawX + ", rawY=" + rawY + ", secondAllele=" + secondAllele
				+ ", secondAlleleCall=" + secondAlleleCall + ", snp=" + snp
				+ ", snpId=" + snpId + ", start=" + start + "]";
	}


	public String getStrand() {
		return strand;
	}


	public char getFirstAlleleForward() {
		return firstAlleleForward;
	}

	public char getSecondAlleleForward() {
		return secondAlleleForward;
	}


}
