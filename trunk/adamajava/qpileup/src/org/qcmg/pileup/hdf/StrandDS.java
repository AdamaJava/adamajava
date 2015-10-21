/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import java.util.HashMap;
import java.util.Map;

import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.model.NonReferenceRecord;
import org.qcmg.pileup.model.PileupDataRecord;
import org.qcmg.pileup.model.StrandElement;
import org.qcmg.pileup.model.StrandEnum;

public class StrandDS {
	
	private final PileupHDF hdf;
	private final QLogger logger = QLoggerFactory.getLogger(getClass());	
	private int chunk;	
	private int datasetLength;
	private String groupName;
	private final String reference;
//	private final String direction;
	private final boolean isReverse;
	private final Map<String, StrandElement> elementMap;

	public StrandDS(PileupHDF hdf, int chunk, int datasetLength, String reference, boolean isReverse) throws Exception {
		this.hdf = hdf;
		this.chunk = chunk;
		this.datasetLength = datasetLength;
		this.reference = reference;	
		this.isReverse = isReverse;
//		this.direction = isReverse ? "reverse" : "forward";
//		getDirection();
		this.groupName = "/"+ reference + "/"+ getDirection();
		this.elementMap = setupMap(null);
	}	
	
	public StrandDS(PileupHDF hdf2, String reference, boolean isReverse) {
		this.hdf= hdf2;
		this.reference = reference;
		this.isReverse = isReverse;
//		this.direction = isReverse ? "reverse" : "forward";
//		getDirection();
		this.groupName = "/"+ reference + "/"+ getDirection(); 
		this.elementMap = setupMap(null);
	}
	
	public StrandDS(PileupHDF hdf2, String reference, boolean isReverse, StrandEnum[] strandElements) {
		this.hdf= hdf2;
		this.reference = reference;
		this.isReverse = isReverse;
//		this.direction = isReverse ? "reverse" : "forward";
//		getDirection();
		this.groupName = "/"+ reference + "/"+ getDirection(); 
		this.elementMap =  setupSubElementMap(null, strandElements);;
	}
	
	public StrandDS(String reference, boolean isReverse, int datasetLength) {
		this.hdf= null;
		this.reference = reference;
		this.isReverse = isReverse;
//		this.direction = isReverse ? "reverse" : "forward";
//		getDirection();
		this.elementMap = setupMap(datasetLength);		
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
//		this.direction = "forward";
//		if (isReverse) {
//			this.direction = "reverse";
//		}
		return isReverse ? "reverse" : "forward";
	}
	
	public void createStrandGroup() throws Exception {
		if (hdf.useHDFObject()) {
			hdf.createGroup(getDirection(), reference);
		} else {
			String groupName = "/" + reference + "/" + getDirection();
			hdf.createH5Group(groupName);
		}
	}

	public void createDatasets() throws Exception {
		
		if (hdf.useHDFObject()) {
			Group group = hdf.getGroup(groupName);

			logger.info("Creating Strand Datasets for chromosome/contig dataset: " + groupName);
			
			for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {
				String name = entry.getKey();
				StrandElement element = entry.getValue();
				
				if (element.isLong()) {
					hdf.createScalarDS(group, datasetLength, name, chunk, Datatype.CLASS_INTEGER, 8, null);
				} else {
					hdf.createScalarDS(group, datasetLength, name, chunk, Datatype.CLASS_INTEGER, 4, null);				
				}
			}	
		} else {
			logger.info("Creating Strand Datasets for chromosome/contig dataset: " + groupName);
			
			for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {
				String name = entry.getKey();
				StrandElement element = entry.getValue();
				
				if (element.isLong()) {
					hdf.createH5ScalarDS(groupName, datasetLength, name, chunk, Datatype.CLASS_INTEGER, 8, null);
				} else {
					hdf.createH5ScalarDS(groupName, datasetLength, name, chunk, Datatype.CLASS_INTEGER, 4, null);				
				}
			}	
		}	
	}

	public synchronized void readDatasetBlock(int startIndex, int size)  {
		
		for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {
			
			String name = entry.getKey();
			
			StrandElement element = entry.getValue();
			
			if (element.isLong()) {				
				element.setLongDataMembers(readLongDSBlock(name, startIndex, size));
			} else {
				element.setIntDataMembers(readIntDSBlock(name, startIndex, size));				
			}			
		}
	}
	
	public synchronized void writeDatasetBlocks(int startIndex, int size, boolean isRemove) throws Exception {
		if (startIndex > -1 && size > 0) {
			
			for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {			
				String name = entry.getKey();
				StrandElement element = entry.getValue();
				
				if (element.isLong()) {
					hdf.writeDatasetBlock(getFullDatasetName(name), startIndex, element.getLongDataMembers().length, element.getLongDataMembers());					
				} else {
					hdf.writeDatasetBlock(getFullDatasetName(name), startIndex, element.getIntDataMembers().length, element.getIntDataMembers());
				}			
			}
		} else {
			logger.warn("Trying to write empty set: " + reference + " " + startIndex + " size: " + size);
		}
	}
	
	private long[] readLongDSBlock(String name, int startIndex, int size)  {
		long[] array;
		try {
			array = (long[]) hdf.readDatasetBlock(getFullDatasetName(name), startIndex, size);
			return array;		
		} catch (Exception e) {
			logger.error("Trying to read set: " + reference + " name " + getFullDatasetName(name) + " " + startIndex + " size: " + size);
			array = new long[0];
			logger.error(PileupUtil.getStrackTrace(e));
			return null;
		}		
	}

	private int[] readIntDSBlock(String name, int startIndex, int size)  {
		int[] array;
		try {
			array = (int[]) hdf.readDatasetBlock(getFullDatasetName(name), startIndex, size);
			return array;		
		} catch (Exception e) {
			logger.error("Trying to read set: " + getFullDatasetName(name) + " " + startIndex + " size: " + size);
			logger.error(PileupUtil.getStrackTrace(e));
			return null;
		}		
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

	private String getFullDatasetName(String name) {
		return groupName + "/" +  name;
	}



	public void modifyStrandDS(PileupDataRecord record, int index, boolean isRemove) throws QPileupException {
		
		for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {	
			String name = entry.getKey();
			StrandElement element = entry.getValue();
			
//			if (element.getName().equals("cigarD") && record.getPosition().intValue() >= 12300 &&  record.getPosition().intValue() <= 12310) {
//				element.addElement(index, 5);
//			} else {
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
//			}
		}		
	}

	public Map<String, StrandElement> getElementsMap() {
		return this.elementMap;
		
	}

	public void mergeDatasets(Map<String, StrandElement> newMap) throws QPileupException {
		for (Map.Entry<String, StrandElement> entry: elementMap.entrySet()) {			
			String key = entry.getKey();
			StrandElement element = entry.getValue();
			element.mergeElement(newMap.get(key));			
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
//
//	public void getPileupRecord(int index) {
//		// TODO Auto-generated method stub
//		
//	}

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