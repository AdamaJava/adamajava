package org.qcmg.sig.model;

public class SigMeta {
	
	private final String md5sum;
	private final int positionsCount;
	private final int baseQ;
	private final int mappingQ;
	private final float gcScore;
	
	public SigMeta(String md, int posCount, int bq, int mq, float gc) {
		this.md5sum = md;
		this.positionsCount = posCount;
		this.baseQ = bq;
		this.mappingQ = mq;
		this.gcScore = gc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + baseQ;
		result = prime * result + Float.floatToIntBits(gcScore);
		result = prime * result + mappingQ;
		result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
		result = prime * result + positionsCount;
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
		SigMeta other = (SigMeta) obj;
		if (baseQ != other.baseQ)
			return false;
		if (Float.floatToIntBits(gcScore) != Float.floatToIntBits(other.gcScore))
			return false;
		if (mappingQ != other.mappingQ)
			return false;
		if (md5sum == null) {
			if (other.md5sum != null)
				return false;
		} else if (!md5sum.equals(other.md5sum))
			return false;
		if (positionsCount != other.positionsCount)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SigMeta [md5sum=" + md5sum + ", positionsCount="
				+ positionsCount + ", baseQ=" + baseQ + ", mappingQ="
				+ mappingQ + ", gcScore=" + gcScore + "]";
	}
	
	public boolean isValid() {
		return md5sum != null && md5sum.length() > 0 && positionsCount > 0;
	}
	
	/**
	 * suitable for comparison will return true if both SigMeta objects are valid AND
	 * baseQ and mappingQ are both > -1 and equal
	 * OR gcScores are both > 0 and equal
	 * OR baseQ and mappingQ are > 0 and gcScore is == -1
	 * OR baseQ and mappingQ are == -1 and gcScore is > 0
	 * clear???
	 * @param sm1
	 * @param sm2
	 * @return
	 */
	public static boolean suitableForComparison(SigMeta sm1, SigMeta sm2) {
		return sm1.isValid() && sm2.isValid()
				&& sm1.md5sum.equals(sm2.md5sum)
				&& sm1.positionsCount == sm2.positionsCount
				&& (isBamVsBamComparisonValid(sm1, sm2)
				|| isSnpChipVsSnpChipComparisonValid(sm1, sm2)
				|| isBamVsSnpChipComparisonValid(sm1, sm2)
				);
	}
	
	public static boolean isBam(SigMeta sm) {
		return sm.baseQ >= 0 && sm.mappingQ >= 0;
	}
	public static boolean isSnpChip(SigMeta sm) {
		return sm.gcScore >= 0f;
	}
	public static boolean isBamVsBamComparisonValid(SigMeta sm1, SigMeta sm2) {
		return isBam(sm1) && isBam(sm2) && sm1.baseQ == sm2.baseQ && sm1.mappingQ == sm2.mappingQ;
	}
	public static boolean isSnpChipVsSnpChipComparisonValid(SigMeta sm1, SigMeta sm2) {
		return isSnpChip(sm1) && isSnpChip(sm2) && Float.floatToIntBits(sm1.gcScore) == Float.floatToIntBits(sm2.gcScore);
	}
	public static boolean isBamVsSnpChipComparisonValid(SigMeta sm1, SigMeta sm2) {
		return (isBam(sm1) && isSnpChip(sm2)) || (isBam(sm2) && isSnpChip(sm1));  
	}

}
