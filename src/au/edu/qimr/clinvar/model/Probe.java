package au.edu.qimr.clinvar.model;

public class Probe {
	
	private final int id;
	private final String dlsoSeq;
	private final String dlsoSeqRC;
	private final String ulsoSeq;
	private final String ulsoSeqRC;
	
	public Probe(int id, String dlsoSeq, String dlsoSeqRC, String ulsoSeq, String ulsoSeqRC) {
		this.id = id;
		this.dlsoSeq = dlsoSeq;
		this.dlsoSeqRC = dlsoSeqRC;
		this.ulsoSeq = ulsoSeq;
		this.ulsoSeqRC = ulsoSeqRC;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dlsoSeq == null) ? 0 : dlsoSeq.hashCode());
		result = prime * result + id;
		result = prime * result + ((ulsoSeq == null) ? 0 : ulsoSeq.hashCode());
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
		Probe other = (Probe) obj;
		if (dlsoSeq == null) {
			if (other.dlsoSeq != null)
				return false;
		} else if (!dlsoSeq.equals(other.dlsoSeq))
			return false;
		if (id != other.id)
			return false;
		if (ulsoSeq == null) {
			if (other.ulsoSeq != null)
				return false;
		} else if (!ulsoSeq.equals(other.ulsoSeq))
			return false;
		return true;
	}

	public int getId() {
		return id;
	}

	public String getDlsoSeq() {
		return dlsoSeq;
	}

	public String getDlsoSeqRC() {
		return dlsoSeqRC;
	}

	public String getUlsoSeq() {
		return ulsoSeq;
	}

	public String getUlsoSeqRC() {
		return ulsoSeqRC;
	}

	@Override
	public String toString() {
		return "Probe [id=" + id + ", dlsoSeq=" + dlsoSeq + ", dlsoSeqRC="
				+ dlsoSeqRC + ", ulsoSeq=" + ulsoSeq + ", ulsoSeqRC="
				+ ulsoSeqRC + "]";
	}

}
