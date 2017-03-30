/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.hdf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.ScalarDS;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5File;
import ncsa.hdf.object.h5.H5ScalarDS;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.Messages;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.model.Chromosome;

public class PileupHDF {
	
	private final String hdfFileName;
	private H5File hdfFile;
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private int fileId =-1;
	private final boolean useHDFObject;
	
	public PileupHDF(String name, boolean create, boolean useHDFObject) throws Exception {
		this.hdfFileName = name;
		this.useHDFObject = useHDFObject;
		
		if (create) {
			createHDFFile();
		} else {
			instantiateHDFFile();				
		}		 
	}

	private synchronized void instantiateHDFFile() throws Exception {
		
		if (useHDFObject) {
			 FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
	
		     if (fileFormat == null) {
		         throw new QPileupException("Cannot find HDF5 FileFormat.");
		     }
	
		     hdfFile = (H5File)fileFormat.createFile(hdfFileName, FileFormat.FILE_CREATE_OPEN);
		} 		
	}

	public synchronized void open() throws Exception {
		if (fileId == -1) {
			if (useHDFObject) {
				fileId =  this.hdfFile.open();
			} else {
				fileId = H5.H5Fopen(hdfFileName, HDF5Constants.H5F_ACC_RDWR,
						HDF5Constants.H5P_DEFAULT);
			}
			if (fileId == -1) {
				throw new QPileupException("FILE_EXCEPTION");
			}		
		} 
	}
	
	public synchronized void close() throws Exception {
		if (fileId != -1) {
			if (useHDFObject) {
				this.hdfFile.close();
			} else {
				H5.H5Fclose(fileId);
			}
		} 
		fileId = -1;
	}	
	
	public synchronized long getFileSize() {
		if (useHDFObject) {
			return hdfFile.length();
		} else {
			return new File(hdfFileName).length();
		}
	}
	
	public synchronized File getFile() {
		return new File(this.hdfFileName);
	}
	
	public synchronized int getFileId() {
		return this.fileId;
	}
	
	public synchronized String getHDFFileName() {
		return this.hdfFileName;
	}

	public synchronized boolean useHDFObject() {
		return this.useHDFObject;
	}
	
	public synchronized List<Chromosome> getChromosomeLengths() throws Exception {
		List<Chromosome> chromosomes = new ArrayList<>();
		List<String> groupList = getRootGroupMembers();	

		for (String group: groupList) {
			if (!group.equals("/metadata")) {
				String name = group.replace("/", "");
				int length = getGroupIntegerAttribute(group);				
				Chromosome chr = new Chromosome(name, length);
				chromosomes.add(chr);
			}			
		}
		
		//Collections.sort(chromosomes);		

		return chromosomes;
	}	

	public synchronized int getGroupIntegerAttribute(String fullName) throws Exception {
		return useHDFObject ? getIntegerAttributeByGroup(fullName) : getH5IntegerAttributebyGroup(fullName);
	}	

	public synchronized String bootstrapReferenceGroup(String name, int datasetLength) throws Exception {
		logger.info("Creating group for chromosome/contig " + name);
		if (useHDFObject) {			
			Group group = createGroup(name, "root");
			//create length attribute
			createGroupLengthAttribute(group, datasetLength);
			return group.getFullName();
		} else {
			String groupName = createH5Group(name);
			createH5IntegerAttribute(groupName, null, "length", datasetLength);
			logger.info("Finished creating group for chromosome/contig " + name);
			return groupName;
		}		
	}
	
	public synchronized String getHDFHeader() throws Exception{
		
	    	StringBuffer sb = new StringBuffer();
	    	sb.append("## DATE=" + PileupUtil.getCurrentDate() + "\n");
	    	sb.append("## VERSION_BOOTSTRAP=" + getVersionMessage() + "\n");
	    	sb.append("## VERSION_FILE=" +getVersionFileMessage() + "\n");
	    	sb.append("## HDF=" +getFile().getAbsolutePath() + "\n"); 	
	    	
	    	MetadataReferenceDS referenceDS = new MetadataReferenceDS(this, "");
	    	sb.append(referenceDS.getMetadata());
	    	
	    	MetadataRecordDS metaDS = new MetadataRecordDS(this);
	    	sb.append("## INFO=BAMS_ADDED:" +metaDS.getAttribute("bams_added") + "\n"); 
	    	sb.append("## INFO=LOW_READ_COUNT:" +metaDS.getAttribute("low_read_count") + "\n"); 
	    	sb.append("## INFO=MIN_NONREF_PERCENT:" +metaDS.getAttribute("non_reference_threshold") + "\n"); 
	    	sb.append(metaDS.getMetadata());
	    	
			return sb.toString();
	}

	
	//===============================Access methods==================================//
	private synchronized void createHDFFile() throws Exception {	
		logger.info("Creating HDF file");
		if (useHDFObject) {
			createHDF();
		} else {
			createH5HDF();
		}
	}

	public synchronized List<String> getRootGroupMembers() throws Exception {
		List<String> groupList = new ArrayList<String>();
		if (useHDFObject) {
			getRootMembers(groupList);
		} else {
			getH5RootMembers(groupList);
		}
		return groupList;
	}

	
	public synchronized int getIntegerAttribute(String datasetName, String attributeName) throws Exception {
		int length = -1;
		if (useHDFObject) {			
			length = getIntegerAttribute(datasetName, attributeName, length);			
		} else {
			length = getH5IntegerAttribute(datasetName, attributeName);		
		}

        if (length == -1) {
        	throw new Exception("Could not read length attribute for dataset " + datasetName);
        }
		
		return length;
	}
	

	public synchronized Object readDatasetBlock(String datasetName, int startIndex, int size) throws Exception {	
		Object results;
		if (useHDFObject) {
			results = readScalarDatasetBlock(datasetName, startIndex, size);
		} else {		
			results = readH5ScalarDSBlock(datasetName, startIndex, size);
		}
		
		return results;
	}	

	public synchronized String[] getMetadataRecords(String fullName) throws Exception {
		String[] records = null;
		if (useHDFObject) {
			records = (String[]) readDatasetBlock(fullName, 0, -1);
		} else {
			records = (String[]) readH5ScalarDSBlock(fullName, 0, -1);
		}
		return records;	
	}
	
	public synchronized void writeDatasetBlock(String name, int startIndex, int length, Object array) throws Exception {
		if (useHDFObject) {
			writeScalarDSBlock(name, startIndex, length, array);
		} else {			
			writeH5ScalarDSBlock(name, startIndex, length, array);
		}			
	}

	public synchronized void createMetadataAttributes(String datasetName, Integer lowReadCount, Integer nonreferenceThreshold, Integer bamsAdded) throws Exception {

		if (useHDFObject) {
			Dataset dataset = (Dataset) hdfFile.get(datasetName);		
			createIntegerAttribute(dataset, "low_read_count", lowReadCount);
			createIntegerAttribute(dataset, "non_reference_threshold", nonreferenceThreshold);	
			createIntegerAttribute(dataset, "bams_added", bamsAdded);	
			dataset.getMetadata();
		} else {
			createH5IntegerAttribute(null, datasetName, "low_read_count", lowReadCount);
			createH5IntegerAttribute(null, datasetName, "non_reference_threshold", nonreferenceThreshold);
			createH5IntegerAttribute(null, datasetName, "bams_added", bamsAdded);
		}
	}

	private synchronized void createIntegerAttribute(Dataset dataset, String name, Integer value) throws Exception {
		long[] attrDims = {1}; // 1D of size 1
		long[] values = {value};
		Datatype dtype = hdfFile.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
	    Attribute attr = new Attribute(name, dtype, attrDims);
	    attr.setValue(values); // set the attribute value
	    //attach the attribute to the dataset
	    dataset.writeMetadata(attr);
	}

	public synchronized void extendStringDatasetBlock(String datasetName, int startIndex, int newLength) throws Exception {
		
		int datasetId = -1, dataspaceId = -1, filespaceId = -1;
		long[] newDims = { newLength };
		if (useHDFObject) {
			Dataset dataset = (Dataset) hdfFile.get(datasetName);
			datasetId = dataset.open();
		} else {
			datasetId = H5.H5Dopen(fileId, datasetName, HDF5Constants.H5P_DEFAULT);
		}
		filespaceId = H5.H5Dget_space(datasetId);
		dataspaceId = H5.H5Screate_simple(1, newDims, null);
		H5.H5Dset_extent(datasetId, newDims);
        H5.H5Fflush(datasetId, HDF5Constants.H5F_SCOPE_GLOBAL);
		
		H5.H5Sclose(dataspaceId);
		H5.H5Sclose(filespaceId);
		H5.H5Dclose(datasetId);
	}
	
	
	//================================HDF-Object methods=========================================//
	private synchronized void createHDF() throws QPileupException, Exception,
			HDF5LibraryException {
		File file = new File(hdfFileName);
		if (file.exists()) {
			file.delete();
		}

		FileFormat fileFormat = FileFormat
				.getFileFormat(FileFormat.FILE_TYPE_HDF5);

		if (fileFormat == null) {
			throw new QPileupException("Cannot find HDF5 FileFormat.");
		}

		hdfFile = (H5File) fileFormat.createFile(hdfFileName,
				FileFormat.FILE_CREATE_OPEN);

		// Check for error condition and report.
		if (hdfFile == null) {
			throw new QPileupException("Failed to create file: " + hdfFileName);
		}

		int fileId = hdfFile.open();
		H5.H5Fflush(fileId, HDF5Constants.H5F_SCOPE_GLOBAL);
		close();
	}

	private synchronized void getRootMembers(List<String> groupList) {
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode) hdfFile.getRootNode()).getUserObject();
		
		List<HObject> groupObjects = root.getMemberList();		
		
		for (HObject group: groupObjects) {	
			groupList.add(group.getFullName());
		}
	}
		
	public synchronized Group getGroup(String groupName) throws Exception {
		Group g = (Group) hdfFile.get(groupName);
		return g;
	}
	
	private synchronized int getIntegerAttributeByGroup(String fullName) throws Exception {
		Group group = (Group) hdfFile.get(fullName);		
		@SuppressWarnings("unchecked")
		List<Object> attrList = group.getMetadata();
		Attribute attr = (Attribute)attrList.get(0);

		//print out attribute value
		int[] attrValue = (int[]) attr.getValue();
		
		return attrValue[0];
	}
	
	private synchronized int getIntegerAttribute(String datasetName, String attributeName,
			int length) throws Exception {
		Dataset dataset = (Dataset) hdfFile.get(datasetName);		
		@SuppressWarnings("unchecked")
		List<Object> attrList = dataset.getMetadata();		

		//print out attribute value
		for (Object obj : attrList) {
			  Attribute attr = (Attribute)obj;
			  String name = attr.getName();
			  if (name.equals(attributeName)) {
				  if (attr.getValue() instanceof long[]) {
					  long[] attrValue = (long[]) attr.getValue();
		    		  length = (int) attrValue[0];
				  } else if (attr.getValue() instanceof int[]) {
					  int[] attrValue = (int[]) attr.getValue();
		    		  length = attrValue[0]; 
				  } else {	        			  
				  }	        		  
			  }
		}
		return length;
	}
	
	public synchronized void createGroupLengthAttribute(Group group, int datasetLength) throws Exception {
		Datatype dtype = hdfFile.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
		long[] attrDims = {1}; // 1D of size two
		long [] length = {datasetLength};
	    Attribute attr = new Attribute("length", dtype, attrDims);
	    attr.setValue(length); // set the attribute value
	
	    //attach the attribute to the dataset
	    group.writeMetadata(attr);
	    
	    //read the attribute into memory
        @SuppressWarnings("unchecked")
		List<Object> attrList = group.getMetadata();
        attr = (Attribute)attrList.get(0);	
	}

	public synchronized Dataset createCompoundDS(String groupName, String datasetName,
			long[] dims, long[] maxdims, long[] chunks, int compression,
			String[] memberNames, Datatype[] memberDataTypes,
			int[] memberSizes, Vector<Object> data) throws Exception {
		
		Dataset compoundDS = null;
		
		Group group = (Group) hdfFile.get(groupName);
		compoundDS = hdfFile.createCompoundDS(datasetName, //dataset name
                group,           //group to make dataset in
                dims,            //dimensions
                maxdims,         //maxdims
                chunks,          //chunks (for expandable dataset) 
                compression,     //compression
                memberNames,     //name of datatype members
                memberDataTypes, //datatypes
                memberSizes,     //member sizes
                data);           //the actual data

		if (compoundDS == null) {
			   logger.error("The compound dataset : " + datasetName + " was not created.");
			   throw new QPileupException("The compound dataset : " + datasetName + " was not created.");
		   }			
		
		return compoundDS;
	}	
	
	public synchronized Datatype createDatatype(int dataType, int objectSize) throws Exception {
		Datatype dtype = null;
		if (useHDFObject) {
			if (dataType == Datatype.CLASS_STRING) {
				dtype = hdfFile.createDatatype(Datatype.CLASS_STRING, objectSize, Datatype.CLASS_STRING, Datatype.CLASS_STRING);
			} else {			
				dtype = hdfFile.createDatatype(Datatype.CLASS_INTEGER, objectSize, Datatype.NATIVE, Datatype.NATIVE);
			}
		} else {
			if (dataType == Datatype.CLASS_STRING) {			
				dtype = new H5Datatype(Datatype.CLASS_STRING, objectSize, Datatype.CLASS_STRING, Datatype.CLASS_STRING);
			} else {			
				dtype = new H5Datatype(Datatype.CLASS_INTEGER, objectSize, Datatype.NATIVE, Datatype.NATIVE);
			}
		}
		
		return dtype;
	}

	public synchronized Dataset createScalarDS(Group group, int datasetLength, String datasetName, int chunkSize, int dataType, int objectSize, Object array) throws Exception {

		if (datasetLength < chunkSize) {
			chunkSize = datasetLength;
		}
		//create dimensions of the object
		long[] dims = {datasetLength};		
		long[] chunks = {chunkSize};	
		long[] maxdims = null;
		if (dataType == Datatype.CLASS_STRING) {
			maxdims = new long[1];
			maxdims[0] = HDF5Constants.H5S_UNLIMITED;	
		}
		
		//create integer datatype
		Datatype dtype = createDatatype(dataType, objectSize);
		Dataset dataset;
		//create the dataset
		
		if (array != null) {			
			dataset = hdfFile.createScalarDS(datasetName, group, dtype, dims, maxdims, chunks, 1, array);
		} else {
			dataset = hdfFile.createScalarDS(datasetName, group, dtype, dims, maxdims, chunks, 1, null);
		}
	    
	    if (dataset == null) {
	    	throw new QPileupException("NULL_ERROR");
	    }

		return dataset;
	}	

	public synchronized Object readScalarDatasetBlock(String datasetName, int startIndex, int size) throws Exception {
		H5ScalarDS ds = (H5ScalarDS) hdfFile.get(datasetName);
		ds.init();
		int rank = ds.getRank();
		long[] selectedDims = ds.getSelectedDims();
		long[] start = ds.getStartDims();
		long[] stride = ds.getStride();
		rank = 1;
		start[0] = startIndex;
		selectedDims[0] = size;
		Object data =  ds.read();
		ds.close(ds.getFID());
		return data;
	}
	
	private synchronized void writeScalarDSBlock(String name, int startIndex, int length,
			Object array) throws Exception {

		Dataset ds = (ScalarDS) hdfFile.get(name);
		ds.init();
		int rank = ds.getRank();
		long[] selectedDims = ds.getSelectedDims();
		long[] start = ds.getStartDims();
		long[] stride = ds.getStride();
		rank = 1;
		start[0] = startIndex;
		selectedDims[0] = length;						
		ds.write(array);

	}
	
	public synchronized Group createGroup(String name, String parentGroupName) throws Exception {
		Group group = null;	

		Group parentGroup = (Group) hdfFile.get(parentGroupName);
		group = hdfFile.createGroup(name, parentGroup);

		return group;		
	}	

	//==========================================H5 methods================================================//
	
	private synchronized void createH5HDF() throws Exception {
		fileId = H5.H5Fcreate(hdfFileName, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		if (fileId == -1) {
			throw new QPileupException("CREATE_ERROR", "HDF file", hdfFileName);
		}
		
		createH5StringAttribute("/", null, "version_file",Messages.getVersionFileMessage(), 10);
		createH5StringAttribute("/", null, "version_bootstrap", Messages.getVersionMessage(), 50);
		H5.H5Fclose(fileId);
		fileId = -1;
	}
	
	public synchronized String createH5Group(String groupName) throws Exception {

		String fullName = "/" + groupName;
		int groupId = H5.H5Gcreate(fileId, fullName, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT); 						
		if (groupId == -1) {
			throw new QPileupException("CREATE_ERROR", "group", groupName);
		}
		H5.H5Gclose(groupId);

		return "/" + groupName;
	}	

	private synchronized void createH5IntegerAttribute(String groupName, String datasetName, String attributeName, int value) throws Exception {

		long[] dims = {1};
		long[] attributeData = {value};
		int dataId = -1;
		int spaceId = -1;
		if (groupName != null) {
			dataId = H5.H5Gopen(fileId, groupName, HDF5Constants.H5P_DEFAULT);
		} else {
			dataId = H5.H5Dopen(fileId, "/" + datasetName, HDF5Constants.H5P_DEFAULT);			
		}		
		spaceId = H5.H5Screate_simple(1, dims, null);
		int attributeId = H5.H5Acreate(dataId, attributeName, HDF5Constants.H5T_STD_I32BE, spaceId, HDF5Constants.H5P_DEFAULT,
				HDF5Constants.H5P_DEFAULT);
		H5.H5Awrite(attributeId, HDF5Constants.H5T_NATIVE_INT, attributeData);
		H5.H5Aclose(attributeId);
		if (spaceId > -1) {
			H5.H5Sclose(spaceId);
		} 
		if (groupName != null) {
			H5.H5Gclose(dataId);
		} else {
			H5.H5Dclose(dataId);
		}

	}
	
	private synchronized void createH5StringAttribute(String groupName, String datasetName, String attributeName, String value, int currentSize) throws Exception {
		long[] dims = {1};
		String[] attribute = {value};
		byte[] bval = PileupUtil.stringToByte(attribute, currentSize);
        Object attrValue = bval;
		int dataId = -1;
		int spaceId = -1;
		if (groupName != null) {			
			dataId = H5.H5Gopen(fileId, groupName, HDF5Constants.H5P_DEFAULT);
		} 
		int typeId = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
		H5.H5Tset_size(typeId,currentSize);
		spaceId = H5.H5Screate_simple(1, dims, null);
		int attributeId = H5.H5Acreate(dataId, attributeName, typeId, spaceId, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		
		int tmptid = typeId;
        int tid = H5.H5Tget_native_type(tmptid);
        H5.H5Tclose(tmptid);
        H5.H5Awrite(attributeId, tid, attrValue);
		H5.H5Aclose(attributeId);
		H5.H5Sclose(spaceId);
		H5.H5Tclose(tid);
		if (groupName != null) {
			H5.H5Gclose(dataId);
		} else {
			H5.H5Dclose(dataId);
		}

	}

	private synchronized void getH5RootMembers(List<String> groupList)
			throws HDF5LibraryException {
		String root = "/";			
		int count = H5.H5Gn_members(fileId, root);
		String[] oname = new String[count];
		int[] otype = new int[count];
		int[] ltype = new int[count];
		long[] orefs = new long[count];
		H5.H5Gget_obj_info_all(fileId, root, oname, otype, ltype, orefs, HDF5Constants.H5_INDEX_NAME);

		// Get type of the object and display its name and type.
		for (int indx = 0; indx < otype.length; indx++) {
			//is a group
			if (otype[indx] == 0 && !oname[indx].equals("metadata")) {
				groupList.add(oname[indx]);
			}
		}
	}	

	synchronized void modifyH5LengthAttribute(String datasetName, String attrName, int value) throws NullPointerException, HDF5Exception {
		long[] dims = {1};
		long[] attributeData = {value};
		
		int dataset_id = H5.H5Dopen(fileId, "/" + datasetName, HDF5Constants.H5P_DEFAULT);
		int dataspace_id = H5.H5Screate_simple(1, dims, null);

		int attribute_id = H5.H5Aopen(dataset_id, attrName, HDF5Constants.H5P_DEFAULT);
		H5.H5Awrite(attribute_id, HDF5Constants.H5T_NATIVE_INT, attributeData);
		H5.H5Aclose(attribute_id);
		H5.H5Sclose(dataspace_id);
		H5.H5Dclose(dataset_id);
	}

	private synchronized int getH5IntegerAttributebyGroup(String fullName)
			throws HDF5LibraryException, HDF5Exception {
		int groupId = H5.H5Gopen(fileId, "/" + fullName, HDF5Constants.H5P_DEFAULT);
		long[] attrData = new long[]{1};
		
		int attributeId = H5.H5Aopen(groupId, "length", HDF5Constants.H5P_DEFAULT);
		H5.H5Aread(attributeId, HDF5Constants.H5T_NATIVE_INT, attrData);
		int length = (int) attrData[0];
		H5.H5Gclose(groupId);
		H5.H5Aclose(attributeId);
		return length;
	}

	public synchronized int getH5IntegerAttribute(String datasetName, String attributeName)
			throws HDF5LibraryException, HDF5Exception {
		int length;

		int datasetId = H5.H5Dopen(fileId, datasetName, HDF5Constants.H5P_DEFAULT);
		long[] attrData = new long[]{1};
		
		int attributeId = H5.H5Aopen(datasetId, attributeName, HDF5Constants.H5P_DEFAULT);
		H5.H5Aread(attributeId, HDF5Constants.H5T_NATIVE_INT, attrData);
		length = (int) attrData[0];
		H5.H5Dclose(datasetId);
		H5.H5Aclose(attributeId);
		return length;
	}	
	
	public synchronized byte[] getH5StringAttribute(String groupName, String attributeName, int size) throws HDF5LibraryException, NullPointerException {

		int groupId = H5.H5Gopen(fileId, groupName, HDF5Constants.H5P_DEFAULT);
		byte[] attrData = new byte[size];
		int attributeId = H5.H5Aopen(groupId, attributeName, HDF5Constants.H5P_DEFAULT);
		int tmptid = H5.H5Aget_type(attributeId);
        int tid = H5.H5Tget_native_type(tmptid);
		H5.H5Aread(attributeId,tid, attrData);
		
		//close everything
		H5.H5Tclose(tmptid);
		H5.H5Tclose(tid);
		H5.H5Aclose(attributeId);
		H5.H5Gclose(groupId);
		return attrData;
	
	}

	private synchronized void writeH5ScalarDSBlock(String datasetName, int startIndex, int length,
			Object array) throws Exception {
		Object tmpData = null;

		long[] start = { startIndex };
		long[] stride = { 1 };
		long[] selectedDims = { length };
		tmpData = array;
		//open the dataset
		int datasetId = H5.H5Dopen(fileId, datasetName, HDF5Constants.H5P_DEFAULT);
		
		//get fileSpaceId for the dataset 
		int fileSpaceId = H5.H5Dget_space(datasetId);
		H5.H5Sselect_hyperslab(fileSpaceId, HDF5Constants.H5S_SELECT_SET,
				start, stride, selectedDims, null);
		//get data type and space ids
		int dataTypeId = H5.H5Dget_type(datasetId);
		int dataSpaceId = H5.H5Screate_simple(1, selectedDims, null);
		int tclass = H5.H5Tget_class(dataTypeId);
		boolean isText = (tclass == HDF5Constants.H5T_STRING);
		
		if (isText) {
			tmpData = PileupUtil.stringToByte((String[]) array, H5.H5Tget_size(dataTypeId));
		}

		//allocate space for the data
		H5.H5Dwrite(datasetId, dataTypeId, dataSpaceId, fileSpaceId,
                 HDF5Constants.H5P_DEFAULT, tmpData);
		
		datasetId = H5.H5Dclose(datasetId);
		H5.H5Sclose(fileSpaceId);
		H5.H5Sclose(dataSpaceId); 
		H5.H5Tclose(dataTypeId); 

	}
	
	public synchronized Object readH5ScalarDSBlock(String datasetName, int startIndex, int size) throws HDF5LibraryException, NullPointerException, HDF5Exception {
		Object theData = null;

		int datasetId = H5.H5Dopen(fileId, datasetName, HDF5Constants.H5P_DEFAULT);	
		int dataspace = H5.H5Dget_space(datasetId);
		//dims to get
		long[] start = { startIndex };
		long[] stride = { 1 };
		long[] selectedDims = new long[1];
		long[] maxDims = new long[1];
		
		//get the whole dataset
		if (size == -1) {	
			H5.H5Sget_simple_extent_dims(dataspace, selectedDims, maxDims);			
			size = (int) selectedDims[0];
		} else {
			selectedDims[0] = size;
		}	
		H5.H5Sclose(dataspace);

		//get fileSpaceId for the dataset 
		int fileSpaceId = H5.H5Dget_space(datasetId);
		
		//get data type and space ids
		int dataTypeId = H5.H5Dget_type(datasetId);
		
		int tclass = H5.H5Tget_class(dataTypeId);

		boolean isText = (tclass == HDF5Constants.H5T_STRING);

		//select the block required
		H5.H5Sselect_hyperslab(fileSpaceId, HDF5Constants.H5S_SELECT_SET,
				start, stride, selectedDims, null);
		int subSpaceId = H5.H5Screate_simple(1, selectedDims, null);		
		
		//allocate space for the data
		theData = H5Datatype.allocateArray(dataTypeId, size);
		H5.H5Dread(datasetId, dataTypeId, subSpaceId, fileSpaceId,
                 HDF5Constants.H5P_DEFAULT, theData);
		H5.H5Sclose(subSpaceId); 
				
		
		if (isText) {
			theData = PileupUtil.byteToString((byte[]) theData, H5.H5Tget_size(dataTypeId));
		}
		
		datasetId = H5.H5Dclose(datasetId);		
		H5.H5Sclose(fileSpaceId);		
		H5.H5Tclose(dataTypeId); 

		return theData;		
	}

	public synchronized void createH5ScalarDS(String groupName, int datasetLength, String datasetName, int chunkSize, int dataType, int datatypeSize, Object array) throws Exception {

		if (datasetLength < chunkSize) {
				chunkSize = datasetLength;
			}
			//create dimensions of the object
			long[] dims = {datasetLength};		
			long[] chunks = {chunkSize};	
			long[] maxdims = null;
			Object theData = array;
			if (dataType == Datatype.CLASS_STRING) {
				maxdims = new long[1];
				maxdims[0] = HDF5Constants.H5S_UNLIMITED;
			}
			String name = groupName +  "/" + datasetName;
			Datatype dtype = createDatatype(dataType, datatypeSize);
			int datatypeId = -1;
			//get the type of the data
			if (dtype.getDatatypeClass() == Datatype.CLASS_STRING) {
				datatypeId = H5.H5Tcopy(HDF5Constants.H5T_C_S1); 
				H5.H5Tset_size(datatypeId, dtype.getDatatypeSize());
				if (theData != null) {
					theData = PileupUtil.stringToByte((String[]) array, H5.H5Tget_size(datatypeId));
				} 
			} else {
				if (datatypeSize == 4) {
					datatypeId = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT32);
                }
                else if (datatypeSize == 8) {
                	datatypeId = H5.H5Tcopy(HDF5Constants.H5T_NATIVE_INT64);
                }
			}
			
			int dataspaceId = H5.H5Screate_simple(1, dims, maxdims);

            // figure out creation properties
            int plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
            
            //set chunking and compression
            H5.H5Pset_layout(plist, HDF5Constants.H5D_CHUNKED);
            H5.H5Pset_chunk(plist, 1, chunks);
            H5.H5Pset_deflate(plist, 1);
            int datasetId = H5.H5Dcreate(fileId, name, datatypeId, dataspaceId, HDF5Constants.H5P_DEFAULT, plist, HDF5Constants.H5P_DEFAULT);
            int fileSpaceId = H5.H5Dget_space(datasetId);
            if (datasetId < 0) {
            	throw new QPileupException("CREATE_ERROR", "dataset", groupName);
            }            
           
            //check if there is anything in the array to write
            if (theData != null) { 
            	H5.H5Dwrite(datasetId, datatypeId, dataspaceId, fileSpaceId, HDF5Constants.H5P_DEFAULT, theData);
            }
            
    		H5.H5Sclose(dataspaceId); 
    		H5.H5Tclose(datatypeId); 
    		H5.H5Dclose(datasetId); 
    		H5.H5Sclose(fileSpaceId);
    		H5.H5Pclose(plist);
	}

	public String getVersionMessage() throws HDF5LibraryException, HDF5Exception {
		String[] array = PileupUtil.byteToString(getH5StringAttribute("/", "version_bootstrap", 50), 50);
		return array[0];
	}

	public String getVersionFileMessage() throws HDF5LibraryException, HDF5Exception {
		String[] array = PileupUtil.byteToString(getH5StringAttribute("/", "version_file", 10), 10);
		return array[0];
	}

	public List<String> getBamFilesInHeader() throws Exception {
		String header = getHDFHeader();
		List<String> bamFiles = new ArrayList<String>();
		if (header != null) {
			String[] lines = header.split("\n");
			for (String line: lines) {
				if (line.startsWith("## METADATA")) {
					String[] values = line.split(",");
					if (values.length >=5) {
						if (values[0].contains("MODE:add")) {
							if (values[4].startsWith("FILE")) {
								bamFiles.add(values[4].replace("FILE:", ""));
							}
						}
					}
				}
			}
		}
		return bamFiles;
	}

}
