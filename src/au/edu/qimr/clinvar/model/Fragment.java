package au.edu.qimr.clinvar.model;

import gnu.trove.list.array.TIntArrayList;

import org.qcmg.common.model.ChrPosition;

public class Fragment implements Comparable<Fragment> {
	
	private final int id;
	private final String fragment;
//	private final int fragmentLength;
	private final  ChrPosition bestTiledLocation;
	
	private final TIntArrayList fsReadPositions = new TIntArrayList();
	private final TIntArrayList rsReadPositions = new TIntArrayList();
//	private final List<StringBuilder> fsHeaders = new ArrayList<>(2);
//	private final List<StringBuilder> rsHeaders = new ArrayList<>(2);
	
	private ChrPosition actualLocation;
	private String [] smithWatermanDiffs;
	private final TIntArrayList overlapDistribution;
	
	public Fragment(int id,String sequence, TIntArrayList fsHeaders, TIntArrayList rsHeaders, ChrPosition bestTiledLocation, TIntArrayList overlapDist) {
//		public Fragment(int id,String sequence, List<StringBuilder> fsHeaders, List<StringBuilder> rsHeaders, ChrPosition bestTiledLocation, TIntArrayList overlapDist) {
		this.id = id;
		this.fragment = sequence;
//		this.fragmentLength = null != this.fragment ? this.fragment.length() : 0;
		this.fsReadPositions.addAll(fsHeaders);
		this.rsReadPositions.addAll(rsHeaders);
//		this.fsHeaders.addAll(fsHeaders);
//		this.rsHeaders.addAll(rsHeaders);
		this.bestTiledLocation = bestTiledLocation;
		this.overlapDistribution = overlapDist;
	}
	
	public void setForwardStrandCount(TIntArrayList headers) {
		this.fsReadPositions.addAll(headers);
	}
	public void setReverseStrandCount(TIntArrayList rsHead) {
		this.rsReadPositions.addAll(rsHead);
	}
//	public void setForwardStrandCount(List<StringBuilder> headers) {
//		this.fsHeaders.addAll(headers);
//	}
//	public void setReverseStrandCount(List<StringBuilder> rsHead) {
//		this.rsHeaders.addAll(rsHead);
//	}
	
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
	
	public int getRecordCount() {
		return getFsCount() + getRsCount();
	}
	
	public boolean onBothStrands() {
		return getFsCount() > 0 && getRsCount() > 0;
	}


	@Override
	public int compareTo(Fragment b) {
		return  b.getRecordCount() - getRecordCount();
	}

	public int getId() {
		return id;
	}

	public void setActualPosition(ChrPosition actualCP) {
		this.actualLocation = actualCP;
	}
	public ChrPosition  getActualPosition() {
		return actualLocation;
	}

	public ChrPosition getBestTiledLocation() {
		return bestTiledLocation;
	}

	public int getFsCount() {
		return fsReadPositions.size() ;
	}
//	public List<StringBuilder> getFsHeaders() {
//		return fsHeaders;
//	}
//	public List<StringBuilder> getRsHeaders() {
//		return rsHeaders;
//	}

	public int getRsCount() {
		return rsReadPositions.size() ;
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
		Fragment other = (Fragment) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
