package au.edu.qimr.panel.model;

import gnu.trove.list.array.TIntArrayList;

import org.qcmg.common.model.ChrPosition;

public class Fragment2 implements Comparable<Fragment2> {
	
	private final int id;
	private final String fragment;
	private ChrPosition cp;
	private boolean actualLocationSet = false;
	private boolean fs = true;
	private final TIntArrayList readPositions = new TIntArrayList();
	
	private String [] smithWatermanDiffs;
	private final TIntArrayList overlapDistribution = new TIntArrayList();
	
	public Fragment2(int id,String sequence) {
		this.id = id;
		this.fragment = sequence;
	}
	
	public void setForwardStrand(boolean fs) {
		this.fs = fs;
	}
	public boolean isForwardStrand() {
		return fs;
	}
	
	public void addPosition(int pos) {
		readPositions.add(pos);
	}
	
	public void setSWDiffs(String [] diffs) {
		this.smithWatermanDiffs = diffs;
	}
	public String [] getSmithWatermanDiffs() {
		return smithWatermanDiffs;
	}
	
	public String getSequence() {
		return fragment;
	}
	
	public int getLength() {
		return fragment.length();
	}

	@Override
	public int compareTo(Fragment2 b) {
		return  b.getRecordCount() - getRecordCount();
	}

	public int getId() {
		return id;
	}

	public void setPosition(ChrPosition cp, boolean actual) {
		this.cp = cp;
		actualLocationSet = actual;
	}
	public ChrPosition  getPosition() {
		return cp;
	}
	public boolean isActualPositionSet() {
		return actualLocationSet;
	}
	
	public int getRecordCount() {
		return readPositions.size();
	}

	public TIntArrayList getOverlapDistribution() {
		return overlapDistribution;
	}

	public void addOverlapDistribution(TIntArrayList overlapDistribution2) {
		overlapDistribution.addAll(overlapDistribution2);
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
		Fragment2 other = (Fragment2) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
