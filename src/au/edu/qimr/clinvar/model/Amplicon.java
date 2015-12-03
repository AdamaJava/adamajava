package au.edu.qimr.clinvar.model;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;

public class Amplicon implements Comparable<Amplicon> {
	
	private final static QLogger logger = QLoggerFactory.getLogger(Amplicon.class);
	
	private final int id;
	private final ChrPosition fragmentPosition;
	
	public Amplicon(int id, ChrPosition fcp) {
		this.id = id;
		this.fragmentPosition = fcp;
	}
	
//	public void addPossiblePosition(ChrPosition cp) {
//		potentialBedPositions.add(cp);
//	}

	public int getId() {
		return id;
	}
	
	public ChrPosition getFragmentPosition() {
		return fragmentPosition;
	}
//	public ChrPosition getExpandedFragmentPosition() {
//		return expandedFragmentPosition;
//	}

//	public void setBestPosition(ChrPosition actualCP) {
//		this.bestBedPosition = actualCP;
//	}

//	public ChrPosition getBestPosition() {
//		return bestBedPosition;
//	}

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
		return fragmentPosition.compareTo(o.fragmentPosition);
	}

	@Override
	public String toString() {
		return "Amplicon [id=" + id + ", fragmentPosition=" + fragmentPosition.toIGVString()
				+ "]";
	}

}
