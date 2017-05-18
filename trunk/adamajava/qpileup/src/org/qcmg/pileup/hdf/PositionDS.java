/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.model.PositionElement;

import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5Datatype;


public class PositionDS  {	
	
	private long[] positions;
	private int[] bases;
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private int chunk;
	private int numEntries;
	private final PileupHDF hdf;
	private final String datasetName;	
	private final String fullName;
	private final String reference;
 
	public PositionDS(PileupHDF hdf, String group, int totalEntries,  int chunkSize, int blockSize) {
		this.numEntries = totalEntries;
		this.hdf = hdf;
		this.datasetName = "position";
		this.reference = group.replace("/", "");
		this.positions = new long[blockSize];
		this.fullName = group + "/" + datasetName;
		this.bases = new int[blockSize];	
		this.chunk = chunkSize;
	}
	
	public PositionDS(PileupHDF hdf, String group) {
		this.hdf = hdf;
		this.datasetName = "position";
		this.fullName = group + "/" + datasetName;
		this.reference = group.replace("/", "");
	}

	public void addMember(int index, long position, char base) {
		if (base == 'R') {
			base = 'N';
		}
		positions[index] = position;
		bases[index] = base;
	}
	
	public synchronized void createDataset() throws Exception {
		long[] chunks = {chunk};

		logger.info("Creating position dataset: " + fullName + " with chunk of " + chunks[0]);
		if (hdf.useHDFObject()) {
			Group group = hdf.createGroup("position", reference);			
			//position information
			hdf.createScalarDS(group, numEntries, "position", chunk, Datatype.CLASS_INTEGER, 8, positions);
			//base information
			hdf.createScalarDS(group, numEntries, "reference", chunk,  Datatype.CLASS_INTEGER, 4, bases);
		} else {
			String groupName = "/" + reference + "/" + "position";
			hdf.createH5Group(groupName);
			//position information
			hdf.createH5ScalarDS(groupName, numEntries, "position", chunk, Datatype.CLASS_INTEGER, 8, positions);
			//base information
			hdf.createH5ScalarDS(groupName, numEntries, "reference", chunk,  Datatype.CLASS_INTEGER, 4, bases);
		}
	}

	public Datatype[] createMemberDatatypes() {
		return  new Datatype[] {
				new H5Datatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, -1),
				new H5Datatype(Datatype.CLASS_CHAR, 8, Datatype.NATIVE, -1) };
	}

	public void readDatasetBlock(int startIndex, int totalToRead) throws Exception {
		positions = (long[]) hdf.readDatasetBlock(fullName + "/position", startIndex, totalToRead);
		bases = (int[]) hdf.readDatasetBlock(fullName + "/reference", startIndex, totalToRead);	
	}

	public int getDatasetLength() {
		return this.positions.length;
	}

	public PositionElement getPositionElement(int index) {
		return new PositionElement(reference, positions[index], (char) bases[index]);
	}
}
