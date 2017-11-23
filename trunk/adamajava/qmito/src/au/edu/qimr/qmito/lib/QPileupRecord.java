/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito.lib;

import java.util.List;
import java.util.Map;

public class QPileupRecord {

	PositionElement position;
	Map<String, StrandElement> forwardElementMap;
	Map<String, StrandElement> reverseElementMap;	
	
    //default delimiter is tab
	static final String DELIMITER = "\t";

	public QPileupRecord(PositionElement position, 
			Map<String, StrandElement> forwardElementMap, Map<String, StrandElement> reverseElementMap) {
		this.position = position;
		this.forwardElementMap = forwardElementMap;
		this.reverseElementMap = reverseElementMap;
	}
	
	public String getPositionString() {
		StringBuilder sb = new StringBuilder();
		sb.append(position.getChr() ).append(DELIMITER);
		sb.append(position.getPosition()).append(DELIMITER);
		sb.append(position.getBase()).append(DELIMITER);
		return sb.toString();
	}

	public Map<String, StrandElement> getForwardElementMap() {
		return forwardElementMap;
	}

	public void setForwardElementMap(Map<String, StrandElement> forwardElementMap) {
		this.forwardElementMap = forwardElementMap;
	}

	public Map<String, StrandElement> getReverseElementMap() {
		return reverseElementMap;
	}

	public void setReverseElementMap(Map<String, StrandElement> reverseElementMap) {
		this.reverseElementMap = reverseElementMap;
	}
	
	public int getTotalBases(boolean includeForward, boolean includeReverse) {
		int total = 0;

		if (includeForward) {
			total += forwardElementMap.get("baseA").getIntElementValue(0);
			total += forwardElementMap.get("baseC").getIntElementValue(0);
			total += forwardElementMap.get("baseT").getIntElementValue(0);
			total += forwardElementMap.get("baseG").getIntElementValue(0);
			total += forwardElementMap.get("baseN").getIntElementValue(0);
		}
		
		if (includeReverse) {
			total += reverseElementMap.get("baseA").getIntElementValue(0);
			total += reverseElementMap.get("baseC").getIntElementValue(0);
			total += reverseElementMap.get("baseT").getIntElementValue(0);
			total += reverseElementMap.get("baseG").getIntElementValue(0);
			total += reverseElementMap.get("baseN").getIntElementValue(0);		
		}
		
		return total;
	}
	
	public int getTotalReads(boolean includeForward, boolean includeReverse) {
		int total = 0;

		if (includeForward) {
			total += forwardElementMap.get("referenceNo").getIntElementValue(0);
			total += forwardElementMap.get("nonreferenceNo").getIntElementValue(0);
			total += forwardElementMap.get("cigarD").getIntElementValue(0);
		}
		
		if (includeReverse) {
			total += reverseElementMap.get("referenceNo").getIntElementValue(0);
			total += reverseElementMap.get("nonreferenceNo").getIntElementValue(0);
			total += reverseElementMap.get("cigarD").getIntElementValue(0);	
		}
		
		return total;
	}
	
	public long getForwardElement(String name) {
		
		StrandElement e = forwardElementMap.get(name);
		if (e.isLong()) {
			return e.getLongElementValue(0);
		} else {
			return e.getIntElementValue(0);
		}		
	}
	
	public long getReverseElement(String name) {
		StrandElement e = reverseElementMap.get(name);
		
		if (e.isLong()) {
			return e.getLongElementValue(0);
		} else {
			return e.getIntElementValue(0);
		}		
	}
	
	public long getTotalElement(String name) {
		return getForwardElement(name) + getReverseElement(name);		
	}
 	
	public String getForwardElementString(List<StrandEnum> elements) {
		return getElementString(forwardElementMap, elements);
	}
	
	public String getReverseElementString(List<StrandEnum> elements) {
		return getElementString(reverseElementMap, elements);
	}
	
	private String getElementString(Map<String, StrandElement> elementMap, List<StrandEnum> elements) {
		StringBuilder sb = new StringBuilder();		
		StrandEnum[] enums = StrandEnum.values();

		int count = 0;
		for (int i=0; i<enums.length; i++) {
			if (elements != null) {   //???
				
				if (elements.contains(enums[i])) {			
					sb.append(elementMap.get(enums[i].toString()).getStrandElementMember(0));	
					
					
					if (count != elements.size() -1) {
						sb.append(DELIMITER);
					}
					if (elements.size() == 1) {
						sb.append(DELIMITER);
					}
					count++;
				}				
			} else {
				sb.append(elementMap.get(enums[i].toString()).getStrandElementMember(0));	
				
				if (i != enums.length -1) {
					sb.append(DELIMITER);
				}
				if (enums.length == 1) {
					sb.append(DELIMITER);
				}
			}
		}
		return sb.toString();
	}
	
	public String getStrandRecordString() {
		StringBuilder sb = new StringBuilder();		
		sb.append(getForwardElementString(null)).append(DELIMITER);
		sb.append(getReverseElementString(null));		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getPositionString());
		sb.append(getForwardElementString(null));
		sb.append(getReverseElementString(null));
		sb.append("\n");
		return sb.toString();
	}

	public long getElementCount(String base) {
		return getForwardElement(base) + getReverseElement(base);
	}	
}
