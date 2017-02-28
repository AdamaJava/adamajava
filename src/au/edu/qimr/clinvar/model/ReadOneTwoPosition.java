package au.edu.qimr.clinvar.model;

public class ReadOneTwoPosition {
	
	private final String r1;
	private final String r2;
	private final int position;
	
	public ReadOneTwoPosition(String r1, String r2, int pos) {
		this.r1 = r1;
		this.r2 = r2;
		this.position = pos;
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + position;
		result = prime * result + ((r1 == null) ? 0 : r1.hashCode());
		result = prime * result + ((r2 == null) ? 0 : r2.hashCode());
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
		ReadOneTwoPosition other = (ReadOneTwoPosition) obj;
		if (position != other.position)
			return false;
		if (r1 == null) {
			if (other.r1 != null)
				return false;
		} else if (!r1.equals(other.r1))
			return false;
		if (r2 == null) {
			if (other.r2 != null)
				return false;
		} else if (!r2.equals(other.r2))
			return false;
		return true;
	}



	public String getR1() {
		return r1;
	}

	public String getR2() {
		return r2;
	}

	public int getPosition() {
		return position;
	}

}
