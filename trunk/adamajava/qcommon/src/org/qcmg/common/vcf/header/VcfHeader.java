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

/**
 * Represents the header of a vcf file.
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * @author  Christina XU
 *
 */

public class VcfHeader implements Iterable<VcfHeaderRecord> {
		

	public static final String HEADER_LINE_FILTER = "##FILTER";
	public static final String HEADER_LINE_INFO = "##INFO";
	public static final String HEADER_LINE_FORMAT = "##FORMAT";	
	
	public static final String CURRENT_FILE_FORMAT = "##fileformat=VCFv4.3";
	public static final String STANDARD_FILE_FORMAT = "##fileformat"; 
	public static final String STANDARD_FILE_DATE = "##fileDate";
	public static final String STANDARD_SOURCE_LINE = "##qSource";
	public static final String STANDARD_UUID_LINE = "##qUUID";	
	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	
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
			boolean isSameId = (record.getId() == null && re.getId() == null) ? true : 
				(re.getId() == null)? false: re.getId().equals(record.getId()); 
			 
			if( re.getMetaKey().equals(record.getMetaKey()) && isSameId   ){ 				
				isExist = true;
				if(isReplace) ite.remove(); 
			}			
		}	
		
		//add record if not exist; replace record if exist and isReplace == true; 
		if(!isExist || isReplace ) stack.add( record );			 
	}
		

	/**
	 * append a new INFO line or replace the existing PG line with same order
	 */
	public void addInfo(String id, String number, String type, String description)  { 
		replaceRecord( idRecords,  new VcfHeaderRecord( HEADER_LINE_INFO, id,number,type,description), true);
	}
	
	/**
	 * append a new FORMAT line or replace the existing PG line with same order
	 */
	public void addFormat(String id, String number, String type, String description) { 
		replaceRecord( idRecords, new VcfHeaderRecord( HEADER_LINE_FORMAT, id,number,type,description),true );
	}
	
	/**
	 * append a new FILTER line or replace the existing PG line with same order
	 */
	public void addFilter(String id, String description) { 
		replaceRecord( idRecords,  new VcfHeaderRecord( HEADER_LINE_FILTER, id, null, null,description), true );			
	}
	
	public void  addOrReplace(VcfHeaderRecord rec){ addOrReplace( rec ,true); }
	public void  addOrReplace(VcfHeaderRecord rec, boolean isReplace){  
		if(rec.getId() != null)
			replaceRecord( idRecords, (VcfHeaderRecord)rec, isReplace);
		else if(isReplace)
			replaceRecord( metaRecords, rec, true);
		else
			metaRecords.add(rec);	
	} 
	
	public void  addOrReplace(String line) { addOrReplace( line, true); 	}
	/**
	 * check the unique id which is ID/ORDER for FORMAT, FILTER,INFO,qPG line, otherwise check the Key of meta-information line.
	 *  Add input header line if not exists, otherwise replace or append  existing header line with same unique id. 
	 * @param line: must start with "##" except CHROM line
	 * @param isReplace: replace the existing line if true; 
	 * otherwise discard the input record in case of FORMAT, FILTER,INFO,qPG line; or append key-value pair meta-information line
	 * 
	 */
	public void  addOrReplace( String line, boolean isReplace ) {

		if(StringUtils.isNullOrEmptyOrMissingData(line))
			return; 
		
		VcfHeaderRecord re = new VcfHeaderRecord(line.trim().replaceAll("\n", ""));
		if(re.getMetaKey().startsWith(VcfHeader.STANDARD_FINAL_HEADER_LINE) && re.getId() == null){
			chromLine = (isReplace || chromLine == null)? new VcfHeaderRecord(line) : chromLine;
		}else if(re.getId() != null){
			replaceRecord( idRecords,re , isReplace );			
		}else if(isReplace) 
			replaceRecord( metaRecords, new VcfHeaderRecord(line), true);
		else metaRecords.add(new VcfHeaderRecord(line));
				
	}

	public VcfHeader(){}	
	
	/**
	 * read whole list of string into vcf header
	 * @param headerRecords
	 */
	public VcfHeader(final List<String> headerRecords){	
		if(headerRecords == null) return;
		
		headerRecords.forEach( r -> {
			try{
				addOrReplace(r, false);
			}catch(IllegalArgumentException e){
				System.err.println(e.getMessage());
			}
			
		});
			
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
	
	public List<VcfHeaderRecord> getFormatRecords() {	return getRecords(idRecords, HEADER_LINE_FORMAT); }
	public VcfHeaderRecord getFormatRecord(String id){ 	return getRecord(idRecords, HEADER_LINE_FORMAT, id); }
	
	public List<VcfHeaderRecord> getFilterRecords() { return getRecords(idRecords, HEADER_LINE_FILTER); }
	public VcfHeaderRecord getFilterRecord(String id) { return getRecord(idRecords, HEADER_LINE_FILTER, id); }

	public List<VcfHeaderRecord> getInfoRecords() { return getRecords(idRecords, HEADER_LINE_INFO); }
	public VcfHeaderRecord getInfoRecord(String id) {  return getRecord(idRecords, HEADER_LINE_INFO, id); }
	
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
	public VcfHeaderRecord getIDRecord(String key, String id) { 	
		key = key.startsWith(Constants.DOUBLE_HASH) ? key : Constants.DOUBLE_HASH + key; 
		return getRecord(idRecords, key, id);			
	}
	
	public List<VcfHeaderRecord> getAllMetaRecords() { return metaRecords ; }	
	public VcfHeaderRecord firstMatchedRecord(String key){
		List<VcfHeaderRecord> list = getRecords(key);
		return (list.isEmpty() )? null : list.get(0);
	}
	public VcfHeaderRecord getUUID() {   return getRecord( metaRecords,STANDARD_UUID_LINE , null ); }	
	public VcfHeaderRecord getFileDate() { return getRecord( metaRecords, STANDARD_FILE_DATE , null ); }
	public VcfHeaderRecord getSource() {	return getRecord( metaRecords, STANDARD_SOURCE_LINE , null ); }
	
	//get newest vcf format version if missing
	public VcfHeaderRecord getFileFormat() {  
		VcfHeaderRecord fv = getRecord( metaRecords, STANDARD_FILE_FORMAT , null ); 
		return (fv == null)? new VcfHeaderRecord(CURRENT_FILE_FORMAT) :fv ; 		
	}
	//in case missing header line
	public VcfHeaderRecord getChrom() { return (chromLine == null) ?  new VcfHeaderRecord(STANDARD_FINAL_HEADER_LINE) : chromLine; } 
	
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
		metaRecords.stream().filter(r -> !r.getMetaKey().equals(STANDARD_FILE_FORMAT) && !r.getMetaKey().equals(STANDARD_FILE_DATE) 
				).forEach(r -> records.add(r));
		
		//add Filter, Info and Format records
		getFilterRecords().stream().sorted().forEach(r -> records.add(r)); 
		getInfoRecords().stream().sorted().forEach(r -> records.add(r));
		getFormatRecords().stream().sorted().forEach(r -> records.add(r));
		
		//add remaining structured header line: ##Key=<ID=, ... >
		idRecords.stream().filter(r -> !r.getMetaKey().equals(HEADER_LINE_FILTER) &&
				!r.getMetaKey().equals(HEADER_LINE_FORMAT) && 
				!r.getMetaKey().equals(HEADER_LINE_INFO) ).sorted().forEach(r -> records.add(r));		
		
		records.add(getChrom()); 		
		return records.iterator();
	}

		
}
