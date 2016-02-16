package au.edu.qimr.clinvar.model;

import java.util.Comparator;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;

public class Amplicon implements Comparable<Amplicon> {
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	
	private final int id;
	private ChrPosition position;
	private final ChrPosition initialFragmentPosition;
	
	public Amplicon(int id, ChrPosition fcp) {
		this.id = id;
		this.initialFragmentPosition = fcp;
		this.position = fcp;
	}
	
	public void setPosition(ChrPosition cp) {
		this.position = cp;
	}
	
	public int getId() {
		return id;
	}
	
	public ChrPosition getInitialFragmentPosition() {
		return initialFragmentPosition;
	}
	public ChrPosition getPosition() {
		return position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		Amplicon other = (Amplicon) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public int compareTo(Amplicon o) {
		return COMPARATOR.compare(position, o.position);
	}

	@Override
	public String toString() {
		return "Amplicon [id=" + id + ", position=" + position.toIGVString() + "]";
	}
}
