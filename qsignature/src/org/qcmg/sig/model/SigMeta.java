package org.qcmg.sig.model;

public class SigMeta {
	
	private final String md5sum;
	private final int positionsCount;
	private final int baseQ;
	private final int mappingQ;
	
	public SigMeta(String md, int posCount, int bq, int mq) {
		this.md5sum = md;
		this.positionsCount = posCount;
		this.baseQ = bq;
		this.mappingQ = mq;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + baseQ;
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
				+ mappingQ + "]";
	}
	
	public boolean isValid() {
		return md5sum != null && md5sum.length() > 0 && positionsCount > 0;
	}
	

}
