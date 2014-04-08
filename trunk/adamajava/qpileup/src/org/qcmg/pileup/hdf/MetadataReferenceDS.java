/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import java.util.List;

public class MetadataReferenceDS extends MetadataDS {

	private static String DATASETNAME = "reference";
	
	public MetadataReferenceDS(PileupHDF hdf, String referenceFile) {
		super(hdf, DATASETNAME);
		this.referenceFile = referenceFile;
	}
	
	public void setReferenceFile(String referenceFile) {
		this.referenceFile = referenceFile;
	}

	@Override
	public void create() throws Exception {
		createDataset();		
	}

	public String getMemberString(String name, int length) {
		return "## REFERENCE=SEQUENCE:"+ name+",LENGTH:" +length;
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

	public void addRecords(List<String> references) {
		
		records = new String[numEntries + references.size()];
		
		//add file info
		records[0] = "## REFERENCE=FILE:" + referenceFile;
		
		for (int i=0; i<references.size(); i++) {
			records[i+1] = references.get(i);
		}
	}
}
