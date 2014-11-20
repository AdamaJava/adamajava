/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VCFHeader implements Iterable<String>{
	public static final String CURRENT_FILE_VERSION = "##fileformat=VCFv4.2";
	
	List<String> headers = new ArrayList<>();
	List<String> infos = new ArrayList<>();		
	List<String> filters = new ArrayList<>();
	List<String> formats = new ArrayList<>();
	
	String finalHeaderLine=VCFHeaderUtils.STANDARD_FINAL_HEADER_LINE;

	public VCFHeader(final List<String> headerRecords) {
		headers.add(0, CURRENT_FILE_VERSION );	//default is version 4.2
		
		for (String record : headerRecords) {
			record = record.trim();
			if(record.toLowerCase().startsWith(VCFHeaderUtils.STANDARD_FILE_VERSION.toLowerCase())) 
				headers.add(0, record);			 	 				
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.STANDARD_FILE_DATE.toLowerCase())) 
				headers.add(1, record);	
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.STANDARD_UUID_LINE.toLowerCase())) 
				headers.add(2, record);	
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.STANDARD_SOURCE_LINE.toLowerCase()))  
				headers.add(3, record);			 
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.HEADER_LINE_INFO.toLowerCase())) 
				infos.add(record);				 
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.HEADER_LINE_FILTER.toLowerCase()))  
				filters.add(record);				 
			 else if(record.toLowerCase().startsWith(VCFHeaderUtils.HEADER_LINE_FORMAT.toLowerCase())) 
				formats.add(record);				 
			 else if(record.toUpperCase().startsWith(VCFHeaderUtils.STANDARD_FINAL_HEADER_LINE)) 
				finalHeaderLine = record;
			 else if(record.startsWith("#"))
				headers.add(record)	;
			 else 
				 headers.add("##" + record);	
	        }
	 }

	@Override
	public Iterator<String> iterator() {
		final List<String> records = new ArrayList<>();
		
		records.addAll(headers);
		records.addAll(infos);
		records.addAll(filters);
		records.addAll(formats);
		records.add(finalHeaderLine);
		
		return records.iterator();
	}


}
