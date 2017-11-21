/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.StrandBiasRecord;

public class QPileupRecord {
	final static String DELIMITER = PileupConstants.DELIMITER;

	final PositionElement position;
	final Map<String, StrandElement> forwardElementMap;
	final Map<String, StrandElement> reverseElementMap;
	List<Base> bases = new ArrayList<>();

	public QPileupRecord(PositionElement positionElement, Map<String, StrandElement> forwardElementMap, Map<String, StrandElement> reverseElementMap) {
		this.position = positionElement;
		this.forwardElementMap = forwardElementMap;
		this.reverseElementMap = reverseElementMap;
	}
	
	public String getPositionString() {
		StringBuilder sb = new StringBuilder();
		sb.append(position.getChr()).append(DELIMITER);
		sb.append(position.getPosition()).append(DELIMITER);
		sb.append(position.getBase()).append(DELIMITER);
		return sb.toString();
	}
	
	public String getChromosome() {
		return position.getChr();
	}
	
	public char getBase() {
		return position.getBase();
	}
	
	public long getBasePosition() {
		return position.getPosition();
	}	

	public Map<String, StrandElement> getForwardElementMap() {
		return forwardElementMap;
	}

	public Map<String, StrandElement> getReverseElementMap() {
		return reverseElementMap;
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
			if (elements != null) {
				
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

	public String getRecordString(List<StrandEnum> viewElements, List<StrandEnum> groupElements, boolean getForwardElements, boolean getReverseElements) {
		StringBuilder sb = new StringBuilder();
		sb.append(getPositionString());
		if (viewElements.size() == 0 && groupElements.size() == 0) {
			sb.append(getForwardElementString(null)).append(DELIMITER);
			sb.append(getReverseElementString(null));
		} else {
			if (viewElements.size() > 0) {
				sb.append(getForwardElementString(viewElements));
				sb.append(getReverseElementString(viewElements));
			} else {
				if (groupElements.size() > 0) {
					if (getForwardElements && !getReverseElements) {
						sb.append(getForwardElementString(groupElements));
					} else if (getReverseElements && !getForwardElements) {
						sb.append(getReverseElementString(groupElements));
					} else {
						sb.append(getForwardElementString(groupElements)).append(DELIMITER);
    					sb.append(getReverseElementString(groupElements));
					}
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

	public String getTotalBasesString() {
		StringBuilder sb = new StringBuilder();		
		sb.append(getForwardElementString(StrandEnum.getBaseCounts()));
		sb.append(DELIMITER);
		sb.append(getReverseElementString(StrandEnum.getBaseCounts()));
		return sb.toString();
	}

	public Character getAltBase(boolean includeForward, boolean includeReverse) {
		
		Map<Character, Long> baseMap= getNonZeroBaseCountsMap(includeForward, includeReverse); 	
		
		long maxNo = 0;
		TreeSet<Character> chars = new TreeSet<>();		
		
		for (Entry<Character, Long> entry: baseMap.entrySet()) {
			char current = entry.getKey();
			long count = entry.getValue();
			if (count > 0) {
				if (current == position.getBase()) {
				} else {
					if (count > maxNo) {
						maxNo = count;
						chars.clear();
						chars.add(current);					
					} 
					if (count == maxNo && count > 0) {
						chars.add(current);
					}
				}
			}
		}
		if (chars.size() == 1) {
			return chars.first();
		} else {
			long baseQual = 0;
			Character alt= null;
			for (char c: chars ) {				
				double currentQual = getAvgBaseQual(baseMap.get(c),c);
				if (currentQual > baseQual) {
					alt = c;
				}
			}
			return alt;
		}
	}
	
	private void calculateTotalBases(boolean includeForward, boolean includeReverse) {
		bases.add(createBase('A', includeForward, includeReverse));
		bases.add(createBase('C', includeForward, includeReverse));
		bases.add(createBase('T', includeForward, includeReverse));
		bases.add(createBase('G',includeForward, includeReverse));
		bases.add(createBase('N', includeForward, includeReverse));
	}
	
	public Base createBase(char c, boolean includeForward, boolean includeReverse) {
		long forCount = 0;
		long forQual = 0;
		if (includeForward) {
		  forCount =  getForwardElement("base" + c);
		  forQual = getForwardElement("qual" + c);
		}
		
		long revCount = 0;
		long revQual = 0;
		if (includeReverse) {
			revCount =  getReverseElement("base" + c);
		    revQual = getReverseElement("qual" + c);
		}
		return new Base(c, forCount, revCount, forQual, revQual, getStrandAvgBaseQual(forQual, forCount), getStrandAvgBaseQual(revQual, revCount));		
	}

	private Map<Character, Long> getNonZeroBaseCountsMap(boolean includeForward, boolean includeReverse) {
		Map<Character, Long> map = new HashMap<>();	
		
		for (Base b: getTotalBasesList (includeForward, includeReverse)) {
			addNonZeroBase(map, b);
		}		
		return map;
	}

	private List<Base> getTotalBasesList(boolean includeForward, boolean includeReverse) {
		if (bases.size() == 0) {
			calculateTotalBases(includeForward, includeReverse);
		}
		return bases;
	}

	private void addNonZeroBase(Map<Character, Long> map, Base b) {
		long count = getElementCount("base" + b.getBase());
		if (b.getCount() > 0 && b.trueBase()) {
			map.put(new Character(b.getBase()), count);
		}
	}

	private double getAvgBaseQual(long totalBaseCount, Character character) {
		double baseCount = getElementCount("qual" + character);
		if (baseCount > 0 && totalBaseCount > 0) {
			return baseCount/totalBaseCount;
		} else {
			return 0;
		}
	}
	
	private double getStrandAvgBaseQual(long totalQual, long totalBaseCount) {
		if (totalQual > 0 && totalBaseCount > 0) {
			return totalQual/(double)totalBaseCount;
		} else {
			return 0;
		}
	}

	public long getElementCount(String base) {
		return getForwardElement(base) + getReverseElement(base);
	}
	
	public int getTotalAltBases(char altBase) {		
		List<Base> bases = getTotalBasesList(true, true);
		int altBaseCount = 0;
		
		for (int i=0; i< bases.size(); i++) {
			if (bases.get(i).getBase() == altBase) {
				altBaseCount += bases.get(i).getCount() ;
			}
		}
		return altBaseCount;
	}
	
	public String getDCCBaseCountString() {
		StringBuilder sb = new StringBuilder();
		List<Base> bases = getTotalBasesList(true, true);
		for (int i=0; i< bases.size(); i++) {
			sb.append(bases.get(i).getDCCString());
			if (i != bases.size() -1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public GenotypeEnum getGenotypeEnum() {		
		List<Base> bases = getTotalTrueBases(); 	

		if (bases.size() == 1) {			
			return GenotypeEnum.getGenotypeEnum(bases.get(0).getBase(), bases.get(0).getBase());						
		} else if (bases.size() == 2) {
			return GenotypeEnum.getGenotypeEnum(bases.get(0).getBase(), bases.get(1).getBase());						
		} else {
			Collections.sort(bases, new Comparator<Base>(){
				@Override
				public int compare(Base b1, Base b2) {
					int diff = (int) b2.getCount() - (int) b1.getCount();
					if (diff != 0) return diff;
					if (position.getBase() == b2.getBase()) 
						return 1;
					else if (position.getBase() == b1.getBase()) 
						return -1;
					return (int) b2.getTotalQual() - (int)b1.getTotalQual();
				}
			});
			return GenotypeEnum.getGenotypeEnum(bases.get(0).getBase(), bases.get(1).getBase());	
		}
	}

	private List<Base> getTotalTrueBases() {
		List<Base> bases = getTotalBasesList(true, true);
		
		List<Base> trueBases = new ArrayList<>();
		
		for (Base b: bases) {
			if (b.trueBase()) {
				trueBases.add(b);
			}
		}
		return trueBases;
	}
	
	public int getForwardBaseCount(char base) {
		if (base == 'R') {
			base = 'N';
		}
		return (int) getForwardElement("base" + base);
	}
	
	public int getReverseBaseCount(char base) {
		if (base == 'R') {
			base = 'N';
		}
		return (int) getReverseElement("base" + base);
	}

	public boolean inRequiredRegion(int startPos, int endPos) {		
		return (getBasePosition() >= startPos && getBasePosition() <= endPos);	
	}

	public StrandBiasRecord getStrandBiasRecord(int minPercentDifference) {
		String chr = getChromosome();
		int pos = (int) getBasePosition();		
		char base = getBase();
		if (base == 'R') {
			base = 'N';
		}
		if (base != 'N') {
			Character forwardAltBase = getAltBase(true, false);
			Character reverseAltBase = getAltBase(false, true);
			if ((forwardAltBase != null && forwardAltBase != 'N') || (reverseAltBase != null && reverseAltBase != 'N')) {				
				StrandBiasRecord strandRecord = new StrandBiasRecord(chr, pos, base, minPercentDifference);
				if (forwardAltBase != null) {
					strandRecord.addForwardBaseCounts(forwardAltBase, getForwardBaseCount(base), getForwardBaseCount(forwardAltBase), getTotalBases(true, false));
				}
				if (reverseAltBase != null) {
					strandRecord.addReverseBaseCounts(reverseAltBase, getReverseBaseCount(base), getReverseBaseCount(reverseAltBase), getTotalBases(false, true));
				}
				
				if (strandRecord.hasStrandBias() || strandRecord.hasDifferentAltBase()) {					
					return strandRecord;
				}				
			}
		}
		
		return null;
	}

	
}
