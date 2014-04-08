/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import java.util.ArrayList;
import java.util.List;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.QPileupException;

public class MetadataRecordDS extends MetadataDS {

	protected QLogger logger = QLoggerFactory.getLogger(getClass());
	private Integer lowReadCount;
	private Integer nonreferenceThreshold;
	private Integer bamsAdded;
	private static String DATASETNAME = "record";
	final static String COMMA_DELIMITER = PileupConstants.COMMA_DELIMITER;
	
	public MetadataRecordDS(PileupHDF hdf) {
		super(hdf, DATASETNAME);
		this.lowReadCount = null;
		this.nonreferenceThreshold = null;
		this.bamsAdded = null;
	}
	
	public MetadataRecordDS(PileupHDF hdf, Integer lowReadCount, Integer nonreferenceThreshold, Integer bamsAdded) {
		super(hdf, DATASETNAME);
		this.lowReadCount = lowReadCount;
		this.nonreferenceThreshold = nonreferenceThreshold;
		this.bamsAdded = bamsAdded;
	}
	
	@Override 
	public void create() throws Exception {
		if (hdf.useHDFObject()) {
			hdf.createGroup(DATASETNAME, "root");
		} else {
			
			hdf.createH5Group(GROUPNAME);
		}
		createDataset();
		hdf.createMetadataAttributes(fullName, lowReadCount, nonreferenceThreshold, bamsAdded);
		logger.info("Bootstrapped in HDF: LowReadCount is below: " + lowReadCount);
    	logger.info("Bootstrapped in HDF: Threshold for high nonreference is above: " + nonreferenceThreshold + " percent");
	}

	public void instantiate() throws Exception {
		records = hdf.getMetadataRecords(fullName);	
		bamsAdded = getAttribute("bams_added");
		lowReadCount = getAttribute("low_read_count");
		nonreferenceThreshold = getAttribute("non_reference_threshold");
	}
	
	@Override
	public String getMetadata() throws Exception {
		StringBuffer sb = new StringBuffer();
		records = (String[]) hdf.readDatasetBlock(fullName, 0, -1);
			
		for (String r: records) {
			sb.append(((r) + "\n"));
		}
		return sb.toString();
	}
	
	public void updateFirstMember(int index, String mode, String runTime) throws Exception {
		String[] data = records[index].split(COMMA_DELIMITER);
		data[2] = "RUNTIME:" + runTime;
		String out = "";
		for (int i=0; i<data.length; i++) {
			out += data[i];
			if (i != data.length - 1) {
				out += ",";
			}
		}
		records[index] = out;
		hdf.writeDatasetBlock(fullName, 0, records.length, records);
	}
	

	public String getMemberString(String date, String mode, String bam, String recordCount, String runTime, String reference) {
		
		String s = new String();
		if (mode.equals("bootstrap") || mode.equals("merge")) {
			s = "## METADATA=MODE:" + mode + COMMA_DELIMITER + "DATE:" + date + COMMA_DELIMITER + "RUNTIME:" + runTime + COMMA_DELIMITER + "HDF:" + hdf.getHDFFileName() + COMMA_DELIMITER+ "REFERENCE:"+reference+"\n";			
		} else {
			s = "## METADATA=MODE:" + mode + COMMA_DELIMITER + "DATE:" + date + COMMA_DELIMITER+ "RUNTIME:" + runTime + COMMA_DELIMITER + "HDF:" + hdf.getHDFFileName() + COMMA_DELIMITER + "FILE:" + bam + COMMA_DELIMITER + "RECORDS:" + recordCount + "\n";		
		}
		
		return s;
		
	}
	
	public void writeMember(String date, String currentTime, String mode, String bam, String recordCount, String runTime, String reference) throws Exception {
					
		String[] oldRecords = (String[]) hdf.readDatasetBlock(fullName, 0, -1);

		int newLength = oldRecords.length + 1;
		records = new String[newLength];
		hdf.extendStringDatasetBlock(fullName, 0, newLength);
		for (int i=0; i<records.length; i++) {
			if (i == records.length -1) {
				addDatasetMember(i, getMemberString(date, mode, bam, recordCount, runTime, reference));
			} else {
				records[i] = oldRecords[i];
			}
		}		
		hdf.writeDatasetBlock(fullName, 0, records.length, records);
	}
	
	public void checkBams(boolean isBamOverride, List<String> bamFiles, String mode) throws Exception {
		records = (String[]) hdf.readDatasetBlock(fullName, 0, -1);
		this.bamsAdded = 0;
		for (String bamFile : bamFiles) {
			
			if (previouslyAdded(bamFile, mode)) {
				//if allowing override, log it, otherwise throw exception
				displayWarning(isBamOverride, bamFile, mode);				
			}
			if (mode.equals("add")) {
				bamsAdded++;
			}
		}
		logger.info("Total bams to add: " + bamsAdded);
		
	}
	
	private void displayWarning(boolean isBamOverride, String bamFile, String mode) throws QPileupException {
		if (isBamOverride) {
			logger.warn("The bam file: " + bamFile + " has previously been used in " + mode + " mode but override option has been chosen");
		} else {
			throw new QPileupException("BAMFILE_ADDED", bamFile, mode);
		}		
	}

	private boolean previouslyAdded(String bamFile, String mode) {
		boolean previouslyAdded = false;
		
		for (String record : records) {
			String[] elements = record.split(",");
			boolean filePresent = false;
			boolean modePresent = false;
			for (String e: elements) {
				if (e.startsWith("FILE")) {
					
					if (e.contains(bamFile)) {
						
						filePresent = true;
					}
				} else if (e.startsWith("## METADATA=MODE")) {
					if (e.contains(mode)) {
						modePresent = true;
					}
				}
			}
			if (modePresent && filePresent) {
				previouslyAdded = true;
				break;
			}
		}
		
		return previouslyAdded;
	}
	
	public void checkHDFMetadata(String[] newRecords, boolean isBamOverride, Integer currentLowReadCount, Integer currentNonRefThreshold, Integer currentBamsAdded) throws QPileupException {
		
		ArrayList<String> finalRecords = new ArrayList<String>();
		
		for (String record: records) {
			finalRecords.add(record);
		}

		//check the info tags: ie low_read_count and non_reference_threshold
		if (lowReadCount == null) {
			lowReadCount = new Integer(currentLowReadCount);
		} else {
			if (!lowReadCount.equals(currentLowReadCount)) {
				throw new QPileupException("READCOUNT_MERGE_ERROR", "" + lowReadCount, "" + currentLowReadCount);
			} 
		}
		
		if (nonreferenceThreshold == null) {
			nonreferenceThreshold = new Integer(currentNonRefThreshold);
		} else {
			if (!nonreferenceThreshold.equals(currentNonRefThreshold)) {
				throw new QPileupException("NONREF_MERGE_ERROR", "" + nonreferenceThreshold, "" + currentNonRefThreshold);
			}
		}
		
		if (bamsAdded == null) {
			bamsAdded = new Integer(currentBamsAdded);
		} else {
			bamsAdded += new Integer(currentBamsAdded);
		}
		
		//check to see if they already exist in the records
			
		for (String newRecord : newRecords) {
			//check add 
			if (newRecord.contains("add")) {
				String file = newRecord.split(",")[4];
				if (recordsContains(file)) {
					displayWarning(isBamOverride, file, "add");
					finalRecords.add(newRecord);
				} else {
					finalRecords.add(newRecord);
				}
			}
			//check reference
			if (newRecord.contains("bootstrap")) {
				finalRecords.add(newRecord);
				String line = newRecord.split(",")[4];
				String ref = line.substring(10, line.length());	
				if (referenceFile == null) {
					referenceFile = ref;						
				} else {
					if (!referenceFile.equals(ref)) {
						throw new QPileupException("REFERENCE_MERGE_ERROR", "" + referenceFile, "" + ref);
					}
				}
			}			
		}
		newRecordsArray(finalRecords);		
	}

	private boolean recordsContains(String file) {
		boolean found = false;
		for (String record : records) {
			if (record.contains(file)) {
				found = true;
				break;
			}
		}
		return found;
	}

	private void newRecordsArray(ArrayList<String> finalRecords) {
		records = new String[finalRecords.size()];
		
		for (int i = 0; i < finalRecords.size(); i++) {
			if (i == 0) {
				String previous = finalRecords.get(i);
				if (previous.endsWith("REFERENCE:\n")) {
				String newString = previous.substring(0, previous.length() - 1) + referenceFile;				
				records[i] = newString;
				} else {
					records[i] = previous;
				}
			} else {
				records[i] = finalRecords.get(i);
			}
		}
	}	

	public Integer getLowReadCount() {
		return lowReadCount;
	}
	
	public Integer getNonreferenceThreshold() {
		return nonreferenceThreshold;
	}
	
	public Integer getBamsAdded() {
		return bamsAdded;
	}

	public void writeAttribute(String attribute) throws NullPointerException, HDF5Exception {
		if (attribute.equals("bams_added")) {
			hdf.modifyH5LengthAttribute(fullName, attribute, bamsAdded);
		}
	}
	

}
