package au.edu.qimr.vcf.header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.qcmg.vcf.VCFHeaderUtils;

import au.edu.qimr.vcf.header.VcfHeaderRecord.MetaType;

/**
 * Represents the header of a vcf file.
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * @author pablocingolani
 *
 */
public class VcfHeader implements Iterable<VcfHeaderRecord> {	
	//default first 4 lines as standard
	VcfHeaderRecord version = null;
	VcfHeaderRecord fileDate = null;	
	VcfHeaderRecord uuid = null;
	VcfHeaderRecord preuuid = null;
	VcfHeaderRecord source = null;
		
	final HashMap<String, VcfHeaderInfo> vcfInfoById;
	final HashMap<String, VcfHeaderFormat> vcfFormatById;
	final HashMap<String, VcfHeaderFilter> vcfFilterById;
	final ArrayList<VcfHeaderRecord> meta;
	final ArrayList<VcfHeaderRecord> others;
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

	public VcfHeader(final Vector<String> headerRecords) throws Exception {
		this();		
		for (String record : headerRecords) 
			add(new VcfHeaderRecord(record));
	 }
	
	//only meta data header line can be replaced
	public void replace(VcfHeaderRecord record)throws Exception {
		
		if(record.type.equals(MetaType.META)){
			for(VcfHeaderRecord re: meta)
				if(re.getId().equalsIgnoreCase(record.getId()))
					meta.remove(record);
		}
		
		add(record);
	}

	/**
	 * Add a VCF INFO header definition
	 * @param vcfInfo
	 * @throws Exception 
	 */
	public void add(VcfHeaderRecord record) throws Exception {
		if(record.type.equals(MetaType.FILTER) || !vcfFilterById.containsKey(record.getId()))
			vcfFilterById.put(record.getId(), (VcfHeaderFilter) record.getRecord());
		else if(record.type.equals(MetaType.FORMAT) || !vcfFormatById.containsKey(record.getId()))
			vcfFormatById.put(record.getId(), (VcfHeaderFormat) record.getRecord());
		else if(record.type.equals(MetaType.INFO) || !vcfInfoById.containsKey(record.getId()))
			vcfInfoById.put(record.getId(), (VcfHeaderInfo) record.getRecord());
		else if(record.type.equals(MetaType.CHROM) )
			chromLine = record;
		else if(record.type.equals(MetaType.OTHER ) )
			others.add(record);
		else if(record.type.equals(MetaType.META)){
			//make sure first four header lines are reserved for purpose
			String id = record.getId();
			if(id.equalsIgnoreCase(VCFHeaderUtils.STANDARD_FILE_VERSION)){
				version = record;				
			}else if(record.getId().equalsIgnoreCase(VCFHeaderUtils.STANDARD_FILE_DATE)){
				fileDate = record;
			}else if(record.getId().equalsIgnoreCase(VCFHeaderUtils.STANDARD_UUID_LINE )){
				uuid = record;			 
			}else if(record.getId().equalsIgnoreCase(VCFHeaderUtils.STANDARD_SOURCE_LINE )){
				source = record; 
			} else 
				meta.add(record);			
		}else
			throw new Exception("invalid or duplicated Vcf header record: " + record.toString());
	}

	/**
	 * Add line to header (can add many lines)
	 * @return
	 * @throws Exception 
	 */
	public void addLine(String newHeaderLine) throws Exception {
		add(new VcfHeaderRecord(newHeaderLine));		 
	}

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
	/**
	 * it will keep same for version, date, uuid and source if all parameters are null
	 * @param updateVersion: replace version with new value
	 * @param updateDate: replace fileDate with new value
	 * @param updateuuid: replace uuid with new value, push previous uuid to "preuuid"
	 * @param source: replace source with new value
	 */
	public void updateHeader(VcfHeaderRecord updateVersion, VcfHeaderRecord updateDate, 
			VcfHeaderRecord updateUuid, VcfHeaderRecord updateSource){
		if(updateVersion != null) version = updateVersion;
		if(updateDate != null) fileDate = updateVersion;		
		if(updateSource != null) version = updateSource;
		if(updateUuid != null){ 
			preuuid = new VcfHeaderRecord(VCFHeaderUtils.PREVIOUS_UUID_LINE + uuid.description); 
			uuid = updateUuid;			 
		}
	}
	/**
	 * return sorted vcf header iterator
	 */
	@Override	
	public Iterator<VcfHeaderRecord> iterator() {
		final Vector<VcfHeaderRecord> records = new Vector<VcfHeaderRecord>();
		if(version != null) records.add(version);
		if(fileDate != null) records.add(fileDate);
		if(uuid != null) records.add(uuid);
		if(preuuid != null) records.add(preuuid);
		if(source != null) records.add(source);
								
		for(VcfHeaderRecord record : meta) records.add(record);
		for(VcfHeaderRecord record : others) records.add(record);
		for(VcfHeaderRecord record : vcfInfoById.values()) records.add(record);
		for(VcfHeaderRecord record : vcfFormatById.values()) records.add(record);
		for(VcfHeaderRecord record : vcfFormatById.values()) records.add(record);	
		records.add(chromLine);		
		return records.iterator();
	} 



}
