package au.edu.qimr.clinvar.model;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

public class RawFragment implements Comparable<RawFragment> {
	
	private final int id;
	private final String fragment;
	private int count;
	private final TIntArrayList overlapDistribution = new TIntArrayList();
	
	public RawFragment(int id,String sequence, int count, int overlap) {
		this.id = id;
		this.fragment = sequence;
		this.count = count;
		addOverlap(overlap, count);
	}
	
	public void addCount(int count) {
		this.count += count;
	}
	public void addOverlap(int overlap, int count) {
		int[] array = new int[count];
		Arrays.fill(array, overlap);
		this.overlapDistribution.add(array);
	}
	
	public String getSequence() {
		return fragment;
	}
	
	public int getCount() {
		return count;
	}
	
	public TIntArrayList getOverlapDistribution() {
		return overlapDistribution;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fragment == null) ? 0 : fragment.hashCode());
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
		RawFragment other = (RawFragment) obj;
		if (fragment == null) {
			if (other.fragment != null)
				return false;
		} else if (!fragment.equals(other.fragment))
			return false;
		return true;
	}

	@Override
	public int compareTo(RawFragment b) {
		return  b.count - count;
	}

	public int getId() {
		return id;
	}
}
