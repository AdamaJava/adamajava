package au.edu.qimr.clinvar.model;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;

public class Probe implements Comparable<Probe>{
	
	private static QLogger logger = QLoggerFactory.getLogger(Probe.class);
	
	private final int id;
	private final String dlsoSeq;
	private final String dlsoSeqRC;
	private final String ulsoSeq;
	private final String ulsoSeqRC;
	private final String subseq;
	
	private final String name;
	
	private final boolean forwardStrand;
	
	private final int primer1Start;
	private final int primer1End;
	private final int primer2Start;
	private final int primer2End;
	private final int subseqStart;
	private final int subseqEnd;
	
	private final ChrPosition cp;
	
	public Probe(int id, String dlsoSeq, String dlsoSeqRC, String ulsoSeq, String ulsoSeqRC, int p1Start, int p1End, int p2Start, int p2End, String subseq, int ssStart, int ssEnd, String chr, boolean forwardStrand, String name) {
		this.id = id;
		this.dlsoSeq = dlsoSeq;
		this.dlsoSeqRC = dlsoSeqRC;
		this.ulsoSeq = ulsoSeq;
		this.ulsoSeqRC = ulsoSeqRC;
		this.primer1Start = p1Start;
		this.primer1End = p1End;
		this.primer2Start = p2Start;
		this.primer2End = p2End;
		this.subseq = subseq;
		this.subseqStart = ssStart;
		this.subseqEnd = ssEnd;
		this.forwardStrand = forwardStrand;
		this.name = name;
		this.cp = new ChrPosition(chr, primer1Start, primer2End);
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
	
	public int getSubReferencePosition(String refSubString) {
		int offset = subseq.indexOf(refSubString);
		
		if (offset == -1) {
			logger.warn("Probe: " + id + ", could not find refSubString in subseq!!! refSubString: " + refSubString);
			return -1;
		} else {
			return  subseqStart + offset;
		}
	}
	
	public String getReferenceSequence() {
		int start = cp.getPosition();
//		int end = cp.getEndPosition();
		int seqStartPos = start - subseqStart;
		String ref = subseq.substring(seqStartPos, cp.getLength() + seqStartPos);

		// reference sequence is always reported on the +ve strand in the primer xml files
		return ref; 	
	}
	public String getBufferedReferenceSequence() {
		return getBufferedReferenceSequence(10);
	}
	
	public String getBufferedReferenceSequence(int buffer) {
		int start = cp.getPosition();
//		int end = cp.getEndPosition();
		int seqStartPos = Math.max(0, start - subseqStart - buffer);
		int seqEndPos = Math.min(cp.getLength() + seqStartPos + buffer, subseq.length());
		
		// reference sequence is always reported on the +ve strand in the primer xml files
		return subseq.substring(seqStartPos, seqEndPos);
	}
	
	public String getReferenceSequence(int additionalLength) {
		int start = cp.getPosition();
//		int end = cp.getEndPosition();
		int seqStartPos = start - subseqStart;
		String ref = subseq.substring(seqStartPos, cp.getLength() + seqStartPos  + additionalLength);
		
		// reference sequence is always reported on the +ve strand in the primer xml files
		return ref; 	
	}
	
	public int getDlsoPrimerLength() {
		return primer1End - primer1Start + 1;
	}
	public int getUlsoPrimerLength() {
		return primer2End - primer2Start + 1;
	}
	public int getExpectedFragmentLength() {
		return cp.getLength();
//		return primer2End - primer1Start + 1;
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

	@Override
	public int compareTo(Probe o) {
		return id - o.id;
	}

	public String getSubseq() {
		return subseq;
	}

	public boolean reverseComplementSequence() {
		return  forwardStrand;
	}

	public ChrPosition getCp() {
		return cp;
	}
	
	public String getName() {
		return name;
	}

}
