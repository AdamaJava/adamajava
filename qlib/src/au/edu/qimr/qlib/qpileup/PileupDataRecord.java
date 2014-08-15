/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

import net.sf.samtools.SAMRecord;
import au.edu.qimr.qlib.qpileup.PileupConstants;

public class PileupDataRecord {

	public static String headings;
	private Integer position;
	private int referenceNo = 0;
	private int nonReferenceNo = 0;
	private int highNonReference = 0;
	private int lowReadCount = 0;
	private int baseA = 0;
	private int baseC = 0;
	private int baseG = 0;
	private int baseT = 0;
	private int baseN = 0;
	private long aQual = 0;
	private long cQual = 0;
	private long gQual = 0;
	private long tQual = 0;
	private long nQual = 0;
	private long mapQual = 0;
	private int startAll = 0;
	private int startNondup = 0;
	private int stopAll = 0;
	private int dupCount = 0;
	private int mateUnmapped = 0;
	private int cigarI = 0;	
	private int cigarD = 0;
	private int cigarDStart = 0;
	private int cigarS = 0;
	private int cigarSStart = 0;
	private int cigarH = 0;
	private int cigarHStart = 0;
	private int cigarN = 0;
	private int cigarNStart = 0;
	private String reference;
	boolean isReverse = false;
	
	final static String DELIMITER = PileupConstants.DELIMITER;
	
	public PileupDataRecord()  {
		
	}
	
	public PileupDataRecord(Integer position) {
		this.position = position;
	}
	
	public PileupDataRecord(Integer position, boolean isReverse) {
		this.position = position;
		this.isReverse = isReverse;
	}

	public Integer getPosition() {
		return this.position;
	}	

	public void setPosition(Integer position2) {
		this.position = position2;
		
	}
	public int getNonReferenceNo() {
		return nonReferenceNo;
	}

	public void setNonReferenceNo(int nonReferenceNo) {
		this.nonReferenceNo = nonReferenceNo;
	}

	public int getHighNonReference() {
		return highNonReference;
	}

	public void setHighNonReference(int highNonReference) {
		this.highNonReference = highNonReference;
	}
	
	public int getBaseA() {
		return baseA;
	}

	public void setBaseA(int baseA) {
		this.baseA = baseA;
	}

	public int getBaseC() {
		return baseC;
	}

	public void setBaseC(int baseC) {
		this.baseC = baseC;
	}

	public int getBaseG() {
		return baseG;
	}

	public void setBaseG(int baseG) {
		this.baseG = baseG;
	}

	public int getBaseT() {
		return baseT;
	}

	public void setBaseT(int baseT) {
		this.baseT = baseT;
	}

	public int getBaseN() {
		return baseN;
	}

	public void setBaseN(int baseN) {
		this.baseN = baseN;
	}

	public long getaQual() {
		return aQual;
	}

	public void setaQual(long aQual) {
		this.aQual = aQual;
	}

	public long getcQual() {
		return cQual;
	}

	public void setcQual(long cQual) {
		this.cQual = cQual;
	}

	public long getgQual() {
		return gQual;
	}

	public void setgQual(long gQual) {
		this.gQual = gQual;
	}

	public long gettQual() {
		return tQual;
	}

	public void settQual(long tQual) {
		this.tQual = tQual;
	}

	public long getnQual() {
		return nQual;
	}

	public void setnQual(long nQual) {
		this.nQual = nQual;
	}

	public long getMapQual() {
		return mapQual;
	}

	public void setMapQual(long mapQual) {
		this.mapQual = mapQual;
	}

	public int getStartAll() {
		return startAll;
	}

	public void setStartAll(int startAll) {
		this.startAll = startAll;
	}

	public int getStartNondup() {
		return startNondup;
	}

	public void setStartNondup(int startNondup) {
		this.startNondup = startNondup;
	}

	public int getStopAll() {
		return stopAll;
	}

	public void setStopAll(int stopAll) {
		this.stopAll = stopAll;
	}

	public long getCigarI() {
		return cigarI;
	}

	public void setCigarI(int cigarI) {
		this.cigarI = cigarI;
	}

	public int getCigarD() {
		return cigarD;
	}

	public void setCigarD(int cigarD) {
		this.cigarD = cigarD;
	}

	public int getCigarS() {
		return cigarS;
	}

	public void setCigarS(int cigarS) {
		this.cigarS = cigarS;
	}

	public int getCigarH() {
		return cigarH;
	}

	public void setCigarH(int cigarH) {
		this.cigarH = cigarH;
	}

	public long getCigarN() {
		return cigarN;
	}

	public void setCigarN(int cigarN) {
		this.cigarN = cigarN;
	}

	public int getDupCount() {
		return dupCount;
	}

	public void setDupCount(int dupCount) {
		this.dupCount = dupCount;
	}
	
	public int getLowReadCount() {
		return this.lowReadCount;
	}

	public void setLowReadCount(int lowReadCount) {
		this.lowReadCount = lowReadCount;
	}

	public void setMateUnmapped(int i) {
		this.mateUnmapped  = i;
		
	}
	
	public int getMateUnmapped() {
		return this.mateUnmapped;		
	}
	
	public int getCigarDStart() {
		return cigarDStart;
	}

	public void setCigarDStart(int cigarDStart) {
		this.cigarDStart = cigarDStart;
	}

	public int getCigarSStart() {
		return cigarSStart;
	}

	public void setCigarSStart(int cigarSStart) {
		this.cigarSStart = cigarSStart;
	}

	public int getCigarHStart() {
		return cigarHStart;
	}

	public void setCigarHStart(int cigarHStart) {
		this.cigarHStart = cigarHStart;
	}

	public int getCigarNStart() {
		return cigarNStart;
	}

	public void setCigarNStart(int cigarNStart) {
		this.cigarNStart = cigarNStart;
	}	
	
	public int totalBaseCount() {
		int count = 0;
		count += baseA + baseC + baseG + baseT;
		return count;
	}
	
	public String getReference() {
		return this.reference;
	}	
	
	public boolean isReverse() {
		return isReverse;
	}

	public void setReverse(boolean isReverse) {
		this.isReverse = isReverse;
	}

	public void checkBase(char base, int qual, char referenceBase, SAMRecord record) throws Exception {
		if (referenceBase != 'h' && referenceBase != '0' && base != 'd') {
			
			if (base != referenceBase) {
				nonReferenceNo += 1;				
			} else {
				referenceNo += 1;
			}
			
			if (base == 'A') {			
				baseA += 1;	
				aQual += qual;							
			} else if (base == 'C') {
				baseC += 1;
				cQual += qual;	
			} else if (base == 'T') {
				baseT += 1;
				tQual += qual;				
			} else if (base == 'G') {
				baseG += 1;
				gQual += qual;	
			}  else if (base == 'N') {
				baseN +=1;
				nQual += qual;	
			} else {
				//ignore as letter is a cigar signifier
			}
		}				
	}

	public int getReferenceNo() {
		return referenceNo;
	}

	public void setReferenceNo(int referenceNo) {
		this.referenceNo = referenceNo;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		StrandEnum[] enums = StrandEnum.values();
		try {
			for (int i=0; i< enums.length; i++) {
				if (i>=StrandEnum.LONG_INDEX_START && i <= StrandEnum.LONG_INDEX_END) {
					sb.append(getLongMember(enums[i].toString()));
				} else {					
					sb.append(getIntMember(enums[i].toString()));
				}
				if (i != enums.length -1) {
					sb.append(DELIMITER);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public int getTotalBases() {
		return  baseA + baseT  + baseC  + baseG  + baseN;
	}

	public int getIntMember(String name) throws Exception {
		StrandEnum strand = (StrandEnum.valueOf(name));
		
		int member = 0;
		switch (strand) {			
			case baseA : member = this.baseA;
			break;
			case baseC:member = this.baseC;
			break;
			case baseG: member = this.baseG;
			break;
			case baseT:member = this.baseT;
			break;
			case baseN:member = this.baseN;
			break;
			case referenceNo:member = this.referenceNo;
			break;
			case nonreferenceNo:member = this.nonReferenceNo;
			break;
			case highNonreference:member = this.highNonReference;
			break;
			case lowRead:member = this.lowReadCount;
			break;
			case startAll:member = this.startAll;
			break;
			case startNondup:member = this.startNondup;
			break;
			case stopAll:member = this.stopAll;
			break;
			case dupCount:member = this.dupCount;
			break;
			case mateUnmapped:member = this.mateUnmapped;
			break;
			case cigarI:member = this.cigarI;
			break;
			case cigarD:member = this.cigarD;
			break;
			case cigarDStart:member = this.cigarDStart;
			break;
			case cigarS:member = this.cigarS;
			break;
			case cigarSStart:member = this.cigarSStart;
			break;
			case cigarH:member = this.cigarH;
			break;
			case cigarHStart:member = this.cigarHStart;
			break;
			case cigarN:member = this.cigarN;
			break;
			case cigarNStart:member = this.cigarNStart;
			break;
			default: member = -1;
		}
		
		if (member< 0) {
			throw new Exception(Messages.getMessage("NEGATIVE_RECORD", name));
		}
		return member;
	}

	public long getLongMember(String name) throws Exception {
		StrandEnum strand = (StrandEnum.valueOf(name));
		
		long member = 0;
		switch (strand) {
			case qualA: member = this.aQual;
			break;
			case qualC:member = this.cQual;
			break;
			case qualG: member = this.gQual;
			break;
			case qualT:member = this.tQual;
			break;
			case qualN:member = this.nQual;
			break;
			case mapQual:member = this.mapQual;
			break;
			default: member = -1;
		}
		if (member< 0) {
			throw new Exception(Messages.getMessage("NEGATIVE_RECORD"));
		}
		return member;
	}

	public void setIsReverse(boolean b) {
		this.isReverse = b;		
	}

}
