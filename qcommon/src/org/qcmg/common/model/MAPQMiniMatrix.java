package org.qcmg.common.model;

public class MAPQMiniMatrix implements Comparable<MAPQMiniMatrix> {
	
	private Integer mapQ;
	private Integer value;
	
	
	public MAPQMiniMatrix() {}	// default constructor
	
	public MAPQMiniMatrix(Integer mapQ) {
		this.mapQ = mapQ;
	}
	
	public MAPQMiniMatrix(Integer mapQ, Integer value) {
		this.mapQ = mapQ;
		this.value = value;
	}
	
	public Integer getMapQ() {
		return mapQ;
	}
	public void setMapQ(Integer mapQ) {
		this.mapQ = mapQ;
	}

	public Integer getValue() {
		return value;
	}
	public void setValue(Integer value) {
		this.value = value;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// Object method overrides
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mapQ == null) ? 0 : mapQ.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		MAPQMiniMatrix other = (MAPQMiniMatrix) obj;
		if (mapQ == null) {
			if (other.mapQ != null)
				return false;
		} else if (!mapQ.equals(other.mapQ))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MAPQMiniMatrix [mapQ=" + mapQ + ", value=" + value + "]";
	}

	@Override
	public int compareTo(MAPQMiniMatrix o) {
		if (0 != mapQ.compareTo(o.mapQ))
			return mapQ.compareTo(o.mapQ);
		if (null != value && null != o.value)
			return value.compareTo(o.value);
		return 0;
	}
}
