package au.edu.qimr.clinvar.model;

import net.sf.picard.fastq.FastqRecord;

public class FastqProbeMatch {

	private final int id;
	private final FastqRecord read1;
	private final FastqRecord read2;
	private  Probe read1Probe;
	private Probe read2Probe;
	
	private final MatchScore score;
	
	private int expectedReadOverlapLength;
	private int overlapBasicEditDistance;
	private int overlapLevenshteinEditDistance;
	private int slide;
	
	private String fragment;
//	private int read1EditDistance = 0;
//	private int read2EditDistance = 0;
	
	public FastqProbeMatch(int id, FastqRecord read1, FastqRecord read2) {
		this.id = id;
		this.read1 = read1;
		this.read2 = read2;
		score = new MatchScore();
	}

	public int getId() {
		return id;
	}

	public FastqRecord getRead1() {
		return read1;
	}

	public FastqRecord getRead2() {
		return read2;
	}
	
	public int getCombnedReadLength() {
		return read1.getReadString().length() + read2.getReadString().length();
	}

	public Probe getRead1Probe() {
		return read1Probe;
	}

	public Probe getRead2Probe() {
		return read2Probe;
	}

	public int getRead1EditDistance() {
		return score.getRead1EditDistance();
	}

	public int getRead2EditDistance() {
		return score.getRead2EditDistance();
	}

	public void setRead1Probe(Probe read1Probe, int distance) {
		this.read1Probe = read1Probe;
		this.score.setRead1EditDistance(distance);
	}

	public void setRead2Probe(Probe read2Probe, int distance) {
		this.read2Probe = read2Probe;
		this.score.setRead2EditDistance(distance);
	}

//	public void setRead1EditDistance(int read1EditDistance) {
//		this.score.setRead1EditDistance(read1EditDistance);
////		this.read1EditDistance = read1EditDistance;
//	}

//	public void setRead2EditDistance(int read2EditDistance) {
//		this.score.setRead2EditDistance(read2EditDistance);
////		this.read2EditDistance = read2EditDistance;
//	}
	
	public MatchScore getScore() {
		return score;
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
		FastqProbeMatch other = (FastqProbeMatch) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public int getOverlapLevenshteinEditDistance() {
		return overlapLevenshteinEditDistance;
	}

	public void setOverlapLevenshteinEditDistance(int overlapEditDistance) {
		this.overlapLevenshteinEditDistance = overlapEditDistance;
	}
	public int getOverlapBasicEditDistance() {
		return overlapBasicEditDistance;
	}
	
	public void setOverlapBasicEditDistance(int overlapEditDistance) {
		this.overlapBasicEditDistance = overlapEditDistance;
	}

	public int getExpectedReadOverlapLength() {
		return expectedReadOverlapLength;
	}

	public void setExpectedReadOverlapLength(int expectedReadOverlapLength) {
		this.expectedReadOverlapLength = expectedReadOverlapLength;
	}

	public void setSlideValue(int slideValue) {
		this.slide = slideValue;
	}
	public int getSlideValue() {
		return slide;
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}
	
}
