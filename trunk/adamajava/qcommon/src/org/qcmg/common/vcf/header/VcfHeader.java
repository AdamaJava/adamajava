/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.vcf.header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

/**
 * Represents the header of a vcf file.
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * @author  Christina XU
 *
 */

public class VcfHeader implements Iterable<VcfHeaderRecord> {
	//deal with special for the vcf chrom header line
	VcfHeaderRecord chromLine = null;
 	
	//store all record with formate ##key=<ID=id,...>, eg. qPG, Format, info and filter
	private final List<VcfHeaderRecord> idRecords = new ArrayList<VcfHeaderRecord>();

	//store all record follow patter ##key=value
	private final List<VcfHeaderRecord> metaRecords = new ArrayList<VcfHeaderRecord>(); 
		
	//add input record if  no exists record with same key and if; 
	//otherwise replace the existing one for true isReplace else discard input record
	private <T extends VcfHeaderRecord>  void replaceRecord(List<T> stack,  T record,  boolean isReplace){		
		boolean isExist = false;

		Iterator<T> ite = stack.iterator();
		while(ite.hasNext()){
			T re = ite.next();
			boolean isSameId = (record.getId() == null && re.getId() == null) || ((re.getId() == null)? false: re.getId().equals(record.getId()));
			 
			if( isSameId && re.getMetaKey().equals(record.getMetaKey())){ 				
				isExist = true;
				if(isReplace) ite.remove(); 
			}			
		}	
		
		//add record if not exist; replace record if exist and isReplace == true; 
		if( ! isExist || isReplace ) stack.add( record );			 
	}
		
	/**
	 * append a new INFO line or replace the existing one with same ID. 
	 * @param id: an unique value for FORMAT Meta-information line
	 * @param number: an Integer that describes the number of values that can be included with the INFO field
	 * @param type: Possible Types are: Integer, Float, Flag, Character, and String.
	 * @param description: a description string
	 */
	public void addInfo(String id, String number, String type, String description)  { 
		replaceRecord( idRecords,  new VcfHeaderRecord( VcfHeaderUtils.HEADER_LINE_INFO, id,number,type,description), true);
	}
	

	/**
	 * append a new FORMAT line or replace the existing one with same ID.
	 * @param id: an unique value for FORMAT Meta-information line
	 * @param number: an Integer that describes the number of values that can be included with the INFO field
	 * @param type: Possible Types are: Integer, Float, Flag, Character, and String.
	 * @param description: a description string
	 */
	public void addFormat(String id, String number, String type, String description) { 
		replaceRecord( idRecords, new VcfHeaderRecord( VcfHeaderUtils.HEADER_LINE_FORMAT, id,number,type,description),true );
	}
	

	/**
	 * append a new FILTER header line or replace the existing one with same id
	 * @param id: an unique value for FILTER Meta-information line
	 * @param description: a description string 
	 */
	public void addFilter(String id, String description) { 
		replaceRecord( idRecords,  new VcfHeaderRecord( VcfHeaderUtils.HEADER_LINE_FILTER, id, null, null,description), true );			
	}
 
	/**
	 * retrieve the key and id value from input record, then determine whether the header contains existing record with same key and id. 
	 * Add input header record if not exists, otherwise replace existing header record. 
	 * @param rec: a vcf header record
	 */
	public void  addOrReplace(VcfHeaderRecord rec){ addOrReplace( rec ,true); }
	
	/**
	 * retrieve the key and id value from input line, then determine whether the header contains existing header record with same key and id. 
	 * Add input header line if not exists, otherwise replace existing header line or keep both. 
	 * @param rec: vcf header record
	 * @param isReplace: the existing record will be replace if set to true; Otherwise if set to false, id of record must be unique, 
	 * so the existing one will be kept and input one will be discard; line without id is allowed for multi entry, so both existing one and input line will be kept.
	 */
	public void  addOrReplace(VcfHeaderRecord rec, boolean isReplace){  
		
		if(rec.getMetaKey().startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE) && rec.getId() == null){
			chromLine = (isReplace || chromLine == null)? rec : chromLine;
		}else if(rec.getId() != null)
			replaceRecord( idRecords, rec, isReplace);
		else if(isReplace)
			replaceRecord( metaRecords, rec, true);
		else
			metaRecords.add(rec);	
	} 
	
	
	/**
	 * retrieve the key and id value from input line, then determine whether the header contains existing header line with same key and id. 
	 * Add input header line if not exists, otherwise replace existing header line. 
	 * @param line:a vcf Meta-information line, must follow  ##key=value pattern, except header line start with #CHROM. 
	 */
	public void  addOrReplace(String line) { addOrReplace( line, true); 	}

	/**
	 * retrieve the key and id value from input line, then determine whether the header contains existing header line with same key and id. 
	 * Add input header line if not exists, otherwise replace existing header line or keep both. 
	 * @param line:a vcf Meta-information line, must follow  ##key=value pattern, except header line start with #CHROM. 
	 * @param isReplace: the existing header line will be replace if set to true; Otherwise if set to false, header line contains id must be unique, 
	 * so the existing one will be kept and input one will be discard; line without id is allowed for multi entry, so both existing one and input line will be kept.
	 * 
	 */
	public void  addOrReplace( String line, boolean isReplace ) {

		if(StringUtils.isNullOrEmptyOrMissingData(line))
			return; 
		
		VcfHeaderRecord re = new VcfHeaderRecord(line.trim().replaceAll("\n", ""));		
		 addOrReplace(re,   isReplace);
				
	}
	/**
	 * create an new empty vcf Header during vcf header merge, unit test etc
	 */
	public VcfHeader(){}	
	
 
	/**
	 * create an new vcf header by reading whole list of string
	 * @param headerRecords: a list of vcf header line
	 * @throws Exception for invalid vcf header, eg.the list is null or missing CHROM line 
	 */
	public VcfHeader(final List<String> headerRecords) {	
		if(headerRecords == null || headerRecords.size() == 0) //return;
			throw new IllegalArgumentException("Vcf Header can't null or empty");
		
		headerRecords.forEach( r -> {
			try{
				addOrReplace(r, false);
			}catch(IllegalArgumentException e){
				System.err.println(e.getMessage());
			}			
		});
		
	if(chromLine == null) 
		throw new IllegalArgumentException("Missing or error on #CHROM line on vcf header");
	 
			
	}		 
			
	/**
	 * 
	 * @return sample column string after Format column on vcf final header line "#CHROM ... "
	 */
	public String[] getSampleId() {
		if(chromLine == null || ! chromLine.getMetaKey().contains(VcfHeaderRecord.FORMAT)) {
			return null;
		}
		
		String[] column = chromLine.getMetaKey().split(Constants.TAB + "");
		if(column.length <= 9) {
			return null;
		}
		return Arrays.copyOfRange(column, 9, column.length);
	}
	
	// return matched record or null if no matched one exists. 
	private  <T extends VcfHeaderRecord>  T getRecord(List<T> stack, String key, String id){	
		if(id == null ) 
			return stack.stream().filter(r -> r.getMetaKey().equals(key)  && r.getId() == null).findFirst().orElse(null);
		return stack.stream().filter(r -> r.getMetaKey().equals(key)  && r.getId().equals(id)).findFirst().orElse(null);
	}
	
	//return empty list if not exists
	private  <T extends VcfHeaderRecord>  List<T> getRecords(List<T> stack, String key ){
		List<T> list = stack.stream().filter(r -> r.getMetaKey().equals(key)).sorted().collect(Collectors.toList()); 
		return list;   
	}
	
	/**
	 * 
	 * @return a list of vcf header record follow pattern ##FORMAT=<ID=id,...>
	 */
	public List<VcfHeaderRecord> getFormatRecords() {	return getRecords(idRecords, VcfHeaderUtils.HEADER_LINE_FORMAT); }
	
	/**
	 * 
	 * @param id: specify an id string here
	 * @return a vcf header Record which contains the specified id value: ##FORMAT=<ID=id,...>
	 */
	public VcfHeaderRecord getFormatRecord(String id){ 	return getRecord(idRecords, VcfHeaderUtils.HEADER_LINE_FORMAT, id); }
	
	/**
	 * 
	 * @return a list of vcf header record follow pattern ##FILTER=<ID=id,...>
	 */
	public List<VcfHeaderRecord> getFilterRecords() { return getRecords(idRecords, VcfHeaderUtils.HEADER_LINE_FILTER); }
	/**
	 * 
	 * @return a list of vcf header record follow pattern ##FILTER=<ID=id,...>
	 */
	public List<VcfHeaderRecord> getContigRecords() { return getRecords(idRecords, VcfHeaderUtils.HEADER_LINE_CONTIG); }
	
	/**
	 * 
	 * @param id: specify an id string here
	 * @return a vcf header Record which contains the specified id value: ##FILTER=<ID=id,...>
	 */
	public VcfHeaderRecord getFilterRecord(String id) { return getRecord(idRecords, VcfHeaderUtils.HEADER_LINE_FILTER, id); }

	/**
	 * 
	 * @return a list of vcf header record follow pattern ##INFO=<ID=id,...>
	 */
	public List<VcfHeaderRecord> getInfoRecords() { return getRecords(idRecords, VcfHeaderUtils.HEADER_LINE_INFO); }
	
	/**
	 * 
	 * @param id: specify an id string here
	 * @return a vcf header Record which contains the specified id value: ##INFO=<ID=id,...>
	 */
	public VcfHeaderRecord getInfoRecord(String id) {  return getRecord(idRecords, VcfHeaderUtils.HEADER_LINE_INFO, id); }
	
	/**
	 * @param key: the key string of ##key=<ID...>. 
	 * @return a list if matched records, or an empty list
	 */
	public List<VcfHeaderRecord> getRecords(String key) { 
		key = key.startsWith(Constants.DOUBLE_HASH) ? key : Constants.DOUBLE_HASH + key; 	
		List<VcfHeaderRecord> recs = getRecords(metaRecords, key);		
		List<VcfHeaderRecord> recs1  = getRecords(idRecords, key);			 
		recs.addAll(recs1);
 				
		return  recs;			 		
	}
	
	/**
	 * 
	 * @param key 
	 * @param id 
	 * @return a vcf header record with specified key and id: ##Key=<ID=id,...>
	 */
	public VcfHeaderRecord getIDRecord(String key, String id) { 	
		key = key.startsWith(Constants.DOUBLE_HASH) ? key : Constants.DOUBLE_HASH + key; 
		return getRecord(idRecords, key, id);			
	}
	/**
	 * 
	 * @return a list of meta info header line which is not structured meta info line. eg. qUUID=1234_567
	 */
	public List<VcfHeaderRecord> getAllMetaRecords() { return metaRecords ; }	
	
	/**
	 * 
	 * @param key: a string match the meta info line ##<key>=<value>
	 * @return the first matched VcfHeaderRecord.
	 */
	public VcfHeaderRecord firstMatchedRecord(String key){
		List<VcfHeaderRecord> list = getRecords(key);
		return (list.isEmpty() )? null : list.get(0);
	}
	
	/**
	 * 
	 * @return the vcf header record with contain key of qUUID: ##qUUID=value
	 */
	public VcfHeaderRecord getUUID() {   return getRecord( metaRecords,VcfHeaderUtils.STANDARD_UUID_LINE , null ); }	

	/**
	 * 
	 * @return the vcf header record with file date: ##fileDate=value
	 */
	public VcfHeaderRecord getFileDate() { return getRecord( metaRecords, VcfHeaderUtils.STANDARD_FILE_DATE , null ); }

	/**
	 * 
	 * @return the vcf header record with qSource: ##qSource=value
	 */	
	public VcfHeaderRecord getSource() {	return getRecord( metaRecords, VcfHeaderUtils.STANDARD_SOURCE_LINE , null ); }
	
	
	/**
	 * 
	 * @return the vcf header record with file format: ##fileFormat=value
	 */
	public VcfHeaderRecord getFileFormat() {  
		VcfHeaderRecord fv = getRecord( metaRecords, VcfHeaderUtils.STANDARD_FILE_FORMAT , null ); 
		return (fv == null)? new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_FORMAT) :fv ; 		
	}
	
	
	/**
	 * 
	 * @return the last vcf header line which start with "#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO"
	 */
	public VcfHeaderRecord getChrom() { return (chromLine == null) ?  new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE) : chromLine; } 
	
	/**
	 * return (internally) sorted vcf header iterator
	 */
	@Override	
	public Iterator<VcfHeaderRecord> iterator() {
		final List<VcfHeaderRecord> records = new ArrayList<>();
		records.add(getFileFormat());  	 //vcf header must start with file version	
		
		VcfHeaderRecord re =  getFileDate();
		if ( re != null) {  records.add(re); } //second line	 
		
		//add line with "<key>=<value>"
		metaRecords.stream().filter(r -> !r.getMetaKey().equals(VcfHeaderUtils.STANDARD_FILE_FORMAT) && !r.getMetaKey().equals(VcfHeaderUtils.STANDARD_FILE_DATE) 
				).forEach(r -> records.add(r));
		
		//add Filter, Info and Format records
		getFilterRecords().stream().sorted().forEach(r -> records.add(r)); 
		getInfoRecords().stream().sorted().forEach(r -> records.add(r));
		getFormatRecords().stream().sorted().forEach(r -> records.add(r));
		
		//add remaining structured header line: ##Key=<ID=, ... >
		idRecords.stream().filter(r -> !r.getMetaKey().equals(VcfHeaderUtils.HEADER_LINE_FILTER) &&
				!r.getMetaKey().equals(VcfHeaderUtils.HEADER_LINE_FORMAT) && 
				!r.getMetaKey().equals(VcfHeaderUtils.HEADER_LINE_INFO) ).sorted().forEach(r -> records.add(r));		
		
		records.add(getChrom()); 		
		return records.iterator();
	}

		
}
