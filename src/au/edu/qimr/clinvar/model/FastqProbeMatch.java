package au.edu.qimr.clinvar.model;

import net.sf.picard.fastq.FastqRecord;

public class FastqProbeMatch {

	private final int id;
	private final FastqRecord read1;
	private final FastqRecord read2;
	private  Probe read1Probe;
	private Probe read2Probe;
	private int read1EditDistance;
	private int read2EditDistance;
	
	public FastqProbeMatch(int id, FastqRecord read1, FastqRecord read2) {
		this.id = id;
		this.read1 = read1;
		this.read2 = read2;
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

	public Probe getRead1Probe() {
		return read1Probe;
	}

	public Probe getRead2Probe() {
		return read2Probe;
	}

	public int getRead1EditDistance() {
		return read1EditDistance;
	}

	public int getRead2EditDistance() {
		return read2EditDistance;
	}

	public void setRead1Probe(Probe read1Probe) {
		this.read1Probe = read1Probe;
	}

	public void setRead2Probe(Probe read2Probe) {
		this.read2Probe = read2Probe;
	}

	public void setRead1EditDistance(int read1EditDistance) {
		this.read1EditDistance = read1EditDistance;
	}

	public void setRead2EditDistance(int read2EditDistance) {
		this.read2EditDistance = read2EditDistance;
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
	
}
