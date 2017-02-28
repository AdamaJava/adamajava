package au.edu.qimr.clinvar.model;

public class LongInt {
	
	private final long l;
	private final int i;
	
	public LongInt(long l, int i) {
		this.l = l;
		this.i = i;
	}
	
	public long getLong() {
		return l;
	}
	public int getInt() {
		return i;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i;
		result = prime * result + (int) (l ^ (l >>> 32));
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
		LongInt other = (LongInt) obj;
		if (i != other.i)
			return false;
		if (l != other.l)
			return false;
		return true;
	}

}
