package au.edu.qimr.clinvar.model;

public class MatchScore implements Comparable<MatchScore>{
	
	private int read1EditDistance = -1;
	private int read2EditDistance = -1;
	
	public void setRead1EditDistance(int read1EditDistance) {
		this.read1EditDistance = read1EditDistance;
	}
	public void setRead2EditDistance(int read2EditDistance) {
		this.read2EditDistance = read2EditDistance;
	}
	public int getRead1EditDistance() {
		return read1EditDistance;
	}
	public int getRead2EditDistance() {
		return read2EditDistance;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + read1EditDistance;
		result = prime * result + read2EditDistance;
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
		MatchScore other = (MatchScore) obj;
		if (read1EditDistance != other.read1EditDistance)
			return false;
		if (read2EditDistance != other.read2EditDistance)
			return false;
		return true;
	}
	
	@Override
	public int compareTo(MatchScore o) {
		return (read1EditDistance + read2EditDistance) - (o.read1EditDistance + o.read2EditDistance);
	}
	@Override
	public String toString() {
		return "MatchScore [read1EditDistance=" + read1EditDistance
				+ ", read2EditDistance=" + read2EditDistance + "]";
	}

}
