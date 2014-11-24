package org.qcmg.common.vcf.header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;

/**
 * Represents the header of a vcf file.
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * @author  Christina XU
 *
 */

public class VcfHeader implements Iterable<VcfHeaderRecord> {	
	//default first 4 lines as standard
	VcfHeaderRecord version = null;
	VcfHeaderRecord fileDate = null;	
	VcfHeaderRecord uuid = null;
	VcfHeaderRecord preuuid = null;
	VcfHeaderRecord source = null;
		
	final Map<String, VcfHeaderInfo> vcfInfoById;
	final Map<String, VcfHeaderFormat> vcfFormatById;
	final Map<String, VcfHeaderFilter> vcfFilterById;
	final List<VcfHeaderRecord> meta;
	final List<VcfHeaderRecord> others;
	VcfHeaderRecord chromLine = null;
	
	ArrayList<String> sampleNames;
//	private boolean VcfHeaderRecord;
	
	
	public VcfHeader(){
		meta = new ArrayList<VcfHeaderRecord>();
		others = new ArrayList<VcfHeaderRecord>();
		vcfFormatById = new HashMap<String, VcfHeaderFormat>();
		vcfInfoById = new HashMap<String, VcfHeaderInfo>();
		vcfFilterById = new HashMap<String, VcfHeaderFilter>();
		
		//set default version & chrom line to new vcf header
		//headers.add(0, new VcfHeaderRecord(VCFHeaderUtils.CURRENT_FILE_VERSION));	
		//chromLine = VCFHeaderUtils.STANDARD_FINAL_HEADER_LINE;
	}

	public VcfHeader(final List<String> headerRecords) throws Exception {
		this();		
		for (String record : headerRecords) {
			add(new VcfHeaderRecord(record));
		}
	 }
	
	//only meta data header line can be replaced
	public void replace(VcfHeaderRecord record)throws Exception {
		
		if(record.type.equals(MetaType.META)){
			Iterator<VcfHeaderRecord> it = meta.iterator();
			while(it.hasNext()) {
				if(it.next().getId().equalsIgnoreCase(record.getId())) {
					it.remove();
				}
			}
		}
		
		//others go to add method directly
		add(record);
	}
	
	public VcfHeaderRecord get(MetaType type, final String key) throws Exception{
		
		String id = ( key.endsWith("=") )? key.substring(0,key.length() - 1) : key;
 		
		Iterator<VcfHeaderRecord> it;
		switch (type) {
			case FORMAT:
				return vcfFormatById.get(id);
			case FILTER:
				return vcfFilterById.get(id);
			case INFO:
				return vcfInfoById.get(id);
			case CHROM:
				return chromLine;
			case META:
				it = meta.iterator();
				break;
			case OTHER:
			default:
				throw new Exception(" can't retrive vcf header record by (metaTyp, id): (" + type.name() + ", " + id +").");
		}
		 					 
		while(it.hasNext()){
			VcfHeaderRecord re = it.next();
			if( re.getId().equalsIgnoreCase(id)){
				return re; 
			}
		}
										
		return null;	 
	}

 
	public void add(VcfHeaderRecord record) throws Exception {
		if(record.type.equals(MetaType.FILTER)  ) 
			vcfFilterById.put(record.getId(), (VcfHeaderFilter) record.parseRecord() );
		else if(record.type.equals(MetaType.FORMAT)  ) 
			vcfFormatById.put(record.getId(), (VcfHeaderFormat) record.parseRecord() );
		else if(record.type.equals(MetaType.INFO)  )
			vcfInfoById.put(record.getId(), (VcfHeaderInfo) record.parseRecord());				
		else if(record.type.equals(MetaType.CHROM) )
			chromLine = record;
		else if(record.type.equals(MetaType.OTHER ) )
			others.add(record);
		else if(record.type.equals(MetaType.META)){	
			if(record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_FILE_VERSION)){
				version = record;				
			}else if(record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_FILE_DATE)){
				fileDate = record;
			}else if(record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_UUID_LINE )){
				uuid = record;			 
			}else if(record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_SOURCE_LINE )){
				source = record; 
			}else if(record.getId().equalsIgnoreCase(VcfHeaderUtils.PREVIOUS_UUID_LINE )){
				preuuid = record;
			} else{ 
				meta.add(record);		
			}	
		}else
			throw new Exception("invalid or duplicated Vcf header record: " + record.toString());

	}

	/**
	 * Add line to header (can add many lines)
	 * @return
	 * @throws Exception 
	 */
//	public void addLine(String newHeaderLine) throws Exception {
//		add(new VcfHeaderRecord(newHeaderLine));		 
//	}

	/**
	 * Get all VcfInfo entries
	 * @return
	 */
	public Collection<VcfHeaderInfo> getVcfInfo() {
	 
		return vcfInfoById.values();
	}

	/**
	 * Get Info type for a given ID
	 * @param id
	 * @return
	 */
	public VcfHeaderInfo getVcfInfo(String id) {		 
		return vcfInfoById.get(id);
	}
	
	public VcfHeaderFormat getVcfFormat(String id) {		 
		return vcfFormatById.get(id);
	}
	
	public VcfHeaderFilter getVcfFilter(String id) {	
		return vcfFilterById.get(id);
	}	 
	
	
	/**
 	 * it will keep same for version, date, uuid and source if all parameters are null
	 * @param updateVersion: replace version with new value
	 * @param updateDate: replace fileDate with new value
	 * @param updateuuid: replace uuid with new value, push previous uuid to "preuuid"
	 * @param source: replace source with new value
	 * @throws Exception 
	 */
	public void updateHeader(VcfHeaderRecord updateVersion, VcfHeaderRecord updateDate, 
			VcfHeaderRecord updateUuid, VcfHeaderRecord updateSource) throws Exception{
		if(updateVersion != null) version = updateVersion;
		if(updateDate != null) fileDate = updateDate;		
		if(updateSource != null) source = updateSource;
		if(updateUuid != null) uuid = updateUuid;	

	}
	/**
	 * return sorted vcf header iterator
	 */
	@Override	
	public Iterator<VcfHeaderRecord> iterator() {
		final List<VcfHeaderRecord> records = new ArrayList<>();
		if (version != null) {
			records.add(version);
		}
		if (fileDate != null) {
			records.add(fileDate);
		}
		if (uuid != null) {
			records.add(uuid);
		}
		if (source != null) {
			records.add(source);
		}
		
		for (VcfHeaderRecord record : meta) {
			records.add(record);
		}
		for (VcfHeaderRecord record : others) {
			records.add(record);
		}
		for (VcfHeaderRecord record : vcfInfoById.values()) {
			records.add(record);
		}
		for (VcfHeaderRecord record : vcfFilterById.values()) {
			records.add(record);
		}
		for (VcfHeaderRecord record : vcfFormatById.values()) {
			records.add(record);	 
		}
		records.add(chromLine);		
		
		return records.iterator();
	} 



}
