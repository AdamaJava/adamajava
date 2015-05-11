package au.edu.qimr.clinvar.model;

public class IntPair implements Comparable<IntPair>{
	
	private  int int1;
	private  int int2;
	
	public IntPair() {}
	
	public IntPair(int int1, int int2) {
		this.int1 = int1;
		this.int2 = int2;
	}
	
	public void setInt1(int int1) {
		this.int1 = int1;
	}
	public void setInt2(int int2) {
		this.int2 = int2;
	}
	public int getInt1() {
		return int1;
	}
	public int getInt2() {
		return int2;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + int1;
		result = prime * result + int2;
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
		IntPair other = (IntPair) obj;
		if (int1 != other.int1)
			return false;
		if (int2 != other.int2)
			return false;
		return true;
	}
	
	@Override
	public int compareTo(IntPair o) {
		return (int1 + int2) - (o.int1 + o.int2);
	}
	@Override
	public String toString() {
		return "IntPair [int1=" + int1
				+ ", int2=" + int2 + "]";
	}

}
