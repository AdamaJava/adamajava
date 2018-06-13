/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito.lib;

import java.util.HashMap;
import java.util.Map;

import htsjdk.samtools.SAMSequenceRecord;


/**
 * this class borrowed idea from org.qcmg.qpileup.hdf.StrandDS.java. Here we moved all hdf related functions and methods. 
 * @author christix
 *
 */
public class StrandDS {
	
	private final Map<String, StrandElement> elementMap;

	/**
	 * Set up an empty map where pileup dataset from indicated strand will be stored for all reference base 
	 * @param reference: a SAMSequenceRecord
	 * @param isReverse: a flag to  indicate whether stored base information are from reverse strand or not.   
	 */
	public StrandDS(SAMSequenceRecord reference, boolean isReverse ) {
		this.elementMap = setupMap(reference.getSequenceLength());		
	}
	

	private Map<String, StrandElement> setupMap(Integer datasetLength) {
		Map<String,StrandElement> map = new HashMap<>();
		StrandEnum[] memberNames = StrandEnum.values();
		
		for (int i=0; i<memberNames.length; i++) {
			if (i>=StrandEnum.LONG_INDEX_START && i <= StrandEnum.LONG_INDEX_END) {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement(true));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(datasetLength, true));
				}
			} else {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement( false));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(datasetLength, false));
				}
			}	
		}
		return map;
	}

	public void finalizeMetrics(int size, boolean isRemove, NonReferenceRecord nonRefRecord) {
		
		for (int i=0; i < size; i++) {
			
			nonRefRecord.defineNonReferenceMetrics(i);

			if (nonRefRecord.isLowReadCount()) {	
				if (isRemove) {
					elementMap.get("lowRead").removeElement(i, 1);
				} else {
					elementMap.get("lowRead").addElement(i, 1);
				}
			} else {				
				if (nonRefRecord.isHighNonRef()) {
					if (isRemove) {
						elementMap.get("highNonreference").removeElement(i, 1);
					} else {
						elementMap.get("highNonreference").addElement(i, 1);
					}
				}			
			}
		}
	}

	public void modifyStrandDS(PileupDataRecord record, int index, boolean isRemove) throws Exception {
		
		for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {	
			String name = entry.getKey();
			StrandElement element = entry.getValue();
			

			if (element.isLong()) {
				long longNo = record.getLongMember(name);
				 
 				if (isRemove) {
					element.removeElement(index, longNo);
				} else {
					element.addElement(index, longNo);
				} 
			} else {
				int intNo = record.getIntMember(name);
				if (isRemove) {
					element.removeElement(index, intNo);
				} else {			
					element.addElement(index, intNo);
				}
			}	
 
		}		
	}

	public Map<String, StrandElement> getStrandElementMap(int i) {
		Map<String, StrandElement> map = setupMap(1);
		for (Map.Entry<String, StrandElement> entry: map.entrySet()) {	
			String name = entry.getKey();			
			StrandElement element = entry.getValue();
			//get the value for the master element map
			StrandElement masterElement = elementMap.get(name);
			
			if (element.isLong()) {
				element.addElement(0, masterElement.getLongElementValue(i));
			} else {
				element.addElement(0, masterElement.getIntElementValue(i));
			}	
		}
		return map;	
	}

}
