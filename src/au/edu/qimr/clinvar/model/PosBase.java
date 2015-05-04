package au.edu.qimr.clinvar.model;

public class PosBase {
	
	private final int pos;
	private final char base;
	
	public PosBase(int pos, char base) {
		this.pos = pos;
		this.base = base;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + base;
		result = prime * result + pos;
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
		PosBase other = (PosBase) obj;
		if (base != other.base)
			return false;
		if (pos != other.pos)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PosBase [pos=" + pos + ", base=" + base + "]";
	}

	public int getPos() {
		return pos;
	}

	public char getBase() {
		return base;
	}
}
