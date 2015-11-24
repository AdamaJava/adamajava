package au.edu.qimr.clinvar.model;

import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;

public class Fragment implements Comparable<Fragment> {
	
	private final static int MAX_NUMBER_OF_DIFFERENCES = 16;		// increase this if we want to allow reads containing more than 16 bases diff from sequence
	private final static QLogger logger = QLoggerFactory.getLogger(Fragment.class);
	
	private final int id;
	private final String fragment;
	private final int fragmentLength;
	private final ChrPosition bestTiledLocation;
	private  int fsCount;
	private  int rsCount;
	private ChrPosition actualLocation;
	private String [] smithWatermanDiffs;
	
	private final Map<ChrPosition, String [] > possiblePositionSWDiffsMap = new HashMap<>(4);
	
	private long position;
	private int noOfTiles;
	
	public Fragment(int id,String sequence, int fsCount, int rsCount, ChrPosition bestTiledLocation) {
		this.id = id;
		this.fragment = sequence;
		this.fragmentLength = this.fragment.length();
		this.fsCount = fsCount;
		this.rsCount = rsCount;
		this.bestTiledLocation = bestTiledLocation;
	}
	
	public void setForwardStrandCount(int fsCount) {
		this.fsCount = fsCount;
	}
	public void setReverseStrandCount(int rsCount) {
		this.rsCount = rsCount;
	}
	
	public void setSWDiffs(String [] diffs) {
		this.smithWatermanDiffs = diffs;
	}
	public String [] getSmithWatermanDiffs() {
		return smithWatermanDiffs;
	}
	public String [] getSmithWatermanDiffs(ChrPosition cp) {
		return possiblePositionSWDiffsMap.get(cp);
	}
	public Map<ChrPosition, String [] > getSmithWatermanDiffsMap() {
		return possiblePositionSWDiffsMap;
	}
	
	public void addPossiblePosition(ChrPosition cp, String [] seDiffs) {
		possiblePositionSWDiffsMap.put(cp, seDiffs);
	}
	/**
	 * Clears and populates the possivlePositionsSWDiffsMap with the supplied map
	 * @param seDiffs
	 */
	public void addPossiblePositions(Map<ChrPosition, String []> seDiffs) {
		possiblePositionSWDiffsMap.putAll(seDiffs);
	}
	
	public String getSequence() {
		return fragment;
	}
	
	public int getLength() {
		return fragmentLength;
	}
	
//	public String getDifferences() {
//		if ( ! diffs.isEmpty()) {
//			StringBuilder sb =new StringBuilder();
//			for (Entry<PosBase, AtomicInteger> entry : diffs.entrySet()) {
//				sb.append(entry.getKey().getPos()).append(":").append(entry.getKey().getBase()).append(":").append(entry.getValue().get()).append(",");
//			}
//			return sb.toString();
//		} else {
//			return "";
//		}
//	}
	
	public int getRecordCount() {
		return rsCount + fsCount;
//		int totalCount = 0;
//		for (int i : counts) {
//			if (i > 0) {
//				totalCount += i;
//			}
//		}
//		return totalCount;
	}

//	private void updateDiffs(char b, int i) {
//		PosBase pb = new PosBase(i, b);
//		AtomicInteger ai = diffs.get(pb);
//		if (null == ai) {
//			ai = new AtomicInteger(1);
//			diffs.put(pb, ai);
//		} else {
//			ai.incrementAndGet();
//		}
//	}

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
		Fragment other = (Fragment) obj;
		if (fragment == null) {
			if (other.fragment != null)
				return false;
		} else if (!fragment.equals(other.fragment))
			return false;
		return true;
	}

	@Override
	public int compareTo(Fragment b) {
		return  b.getRecordCount() - getRecordCount();
	}

	public int getId() {
		return id;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}
	public void setActualPosition(ChrPosition actualCP) {
		this.actualLocation = actualCP;
	}
	public ChrPosition  getActualPosition() {
		return actualLocation;
	}

	public int getNoOfTiles() {
		return noOfTiles;
	}

	public void setNoOfTiles(int noOfTiles) {
		this.noOfTiles = noOfTiles;
	}
	
//	public void setBestTiledLocation(ChrPosition cp) {
//		this.bestTiledLocation = cp;
//	}
	public ChrPosition getBestTiledLocation() {
		return bestTiledLocation;
	}

	public int getFsCount() {
		return fsCount;
	}

	public int getRsCount() {
		return rsCount;
	}

}
