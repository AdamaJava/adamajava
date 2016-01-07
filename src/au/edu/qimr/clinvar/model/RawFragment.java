package au.edu.qimr.clinvar.model;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RawFragment implements Comparable<RawFragment> {
	
	private final int id;
	private final String fragment;
	private final List<StringBuilder> readHeaders = new ArrayList<>(2);
//	private int count;
	private final TIntArrayList overlapDistribution = new TIntArrayList();
	
	public RawFragment(int id,String sequence, List<StringBuilder> headers, int overlap) {
//		public RawFragment(int id,String sequence, int count, int overlap) {
		this.id = id;
		this.fragment = sequence;
//		this.count = count;
		addOverlap(overlap, headers);
	}
	
	public void addOverlap(int overlap, List<StringBuilder> headers) {
		int[] array = new int[headers.size()];
		Arrays.fill(array, overlap);
		this.overlapDistribution.add(array);
		this.readHeaders.addAll(headers);
	}
	
	public String getSequence() {
		return fragment;
	}
	
	public List<StringBuilder> getReadHeaders() {
		return readHeaders;
	}
	
	public int getCount() {
		return readHeaders.size();
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
		return  b.readHeaders.size() - this.readHeaders.size();
	}

	public int getId() {
		return id;
	}
}
