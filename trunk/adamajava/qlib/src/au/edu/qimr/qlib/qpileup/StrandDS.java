/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

import java.util.HashMap;
import java.util.Map;

import htsjdk.samtools.SAMSequenceRecord;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;


/**
 * this class borrowed idea from org.qcmg.qpileup.hdf.StrandDS.java. Here we moved all hdf related functions and methods. 
 * @author christix
 *
 */
public class StrandDS {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());	

	private SAMSequenceRecord reference;
	private String direction;
	private boolean isReverse = false;
	private Map<String, StrandElement> elementMap;

	/**
	 * Set up an empty map where pileup dataset from indicated strand will be stored for all reference base 
	 * @param reference: a SAMSequenceRecord
	 * @param isReverse: a flag to  indicate whether stored base information are from reverse strand or not.   
	 */
	public StrandDS(SAMSequenceRecord reference, boolean isReverse ) {
		this.reference = reference;
		this.isReverse = isReverse;
		getDirection();
		this.elementMap = setupMap(reference.getSequenceLength());		
	}
	
	private Map<String, StrandElement> setupSubElementMap(Integer datasetLength, StrandEnum[] memberNames) {
		Map<String,StrandElement> map = new HashMap<String, StrandElement>();
		
		for (int i=0; i<memberNames.length; i++) {	
			if (memberNames[i].toString().toLowerCase().contains("qual")) {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), true));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), datasetLength, true));
				}
			} else {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), false));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), datasetLength, false));
				}		
			}
		}
		return map;
	}

	private Map<String, StrandElement> setupMap(Integer datasetLength) {
		Map<String,StrandElement> map = new HashMap<String, StrandElement>();
		StrandEnum[] memberNames = StrandEnum.values();
		
		for (int i=0; i<memberNames.length; i++) {
			if (i>=StrandEnum.LONG_INDEX_START && i <= StrandEnum.LONG_INDEX_END) {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), true));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), datasetLength, true));
				}
			} else {
				if (datasetLength == null) {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), false));
				} else {
					map.put(memberNames[i].toString(), new StrandElement(memberNames[i].toString(), datasetLength, false));
				}
			}	
		}
		return map;
	}

	public String getDirection() {
		this.direction = "forward";
		if (isReverse) {
			this.direction = "reverse";
		}
		return direction;
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

	public Map<String, StrandElement> getElementsMap() {
		return this.elementMap;
		
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

	public void getPileupRecord(int index) {
		// TODO Auto-generated method stub
		
	}

	public Map<String, StrandElement> getStrandElementMap(int i,
			StrandEnum[] strandElements) {
		Map<String, StrandElement> map = setupSubElementMap(1, strandElements);
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
