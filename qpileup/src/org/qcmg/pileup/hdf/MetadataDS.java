/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;


public abstract class MetadataDS {	
	
	protected QLogger logger = QLoggerFactory.getLogger(getClass());
	protected int numEntries;
	protected PileupHDF hdf;
	protected String[] records;
	protected String fullName;
	protected String referenceFile;
	protected int STRING_LENGTH = 500;
	protected String datasetName;
	protected static String GROUPNAME = "metadata";
 
	public MetadataDS(PileupHDF hdf, String name) {
		this.numEntries = 1;
		this.hdf = hdf;
		this.records = new String[numEntries];
		this.datasetName = name;
		this.fullName = "/metadata/" + name;
	}
	
	public abstract void create() throws Exception;
	
	public abstract String getMetadata() throws Exception;
	
	public void addDatasetMember(int index, String member) {
		records[index] = member;		
	}
	
	public void createDataset() throws Exception {
		logger.info("Creating dataset: " + fullName);
		//create metadata group
		if (hdf.useHDFObject()) {
			Group group = hdf.getGroup(fullName);		
			if (records[0] == null) {
				hdf.createScalarDS(group, 0, datasetName, 1, Datatype.CLASS_STRING, STRING_LENGTH, null);				
			} else {
				hdf.createScalarDS(group, records.length, datasetName, 1, Datatype.CLASS_STRING, STRING_LENGTH, records);
			}			
		} else {
			if (records[0] == null) {
				hdf.createH5ScalarDS("/" + GROUPNAME, 1, datasetName, 1, Datatype.CLASS_STRING, STRING_LENGTH, null);
			} else { 
				hdf.createH5ScalarDS("/" + GROUPNAME, records.length, datasetName, 1, Datatype.CLASS_STRING, STRING_LENGTH, records);
			}			
		}		
	}


	public Integer getAttribute(String name) throws Exception {
		return hdf.getIntegerAttribute(fullName, name);
	}

	public String[] getRecords() {
		return records;
	}

	public void setRecords(String [] records) {
		this.records = records;		
	}

	
}
