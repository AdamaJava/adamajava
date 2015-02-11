package org.qcmg.common.vcf.header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;

/**
 * Represents the header of a vcf file.
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * @author  Christina XU
 *
 */

public class VcfHeader implements Iterable<VcfHeader.Record> {
	
	public static final String ID = "ID";
	public static final String NUMBER = "Number";
	public static final String TYPE = "Type";	
	public static final String DESCRIPTION = "Description";	
	public static final String SOURCE = "Source";
	public static final String VERSION = "Version";
	public static final String FORMAT = "FORMAT";
	public static final String TOOL = "Tool";
	public static final String DATE = "Date";
	public static final String COMMAND_LINE = "CL";
	
	
	//default first 4 lines as standard
	Record version = null;
	Record fileDate = null;	
	Record uuid = null;
	Record preuuid = null;
	Record source = null;
	Record chromLine = null;
		
	final FormattedRecords infoRecords;
	final FormattedRecords formatRecords;
	final FormattedRecords filterRecords;
	final List<QPGRecord> qpgRecords;
	
	final SimpleRecords metaRecords;
	final SimpleRecords otherRecords;
	
	public static class Record {
		private final String data;
		public Record(String data) {
			this.data = data;
		}
		public String getData() {
			return data;
		}
		@Override
		public final String toString() {
			return data;
		}
		@Override
		public final int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((data == null) ? 0 : data.hashCode());
			return result;
		}
		@Override
		public final boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Record other = (Record) obj;
			if (data == null) {
				if (other.data != null)
					return false;
			} else if (!data.equals(other.data))
				return false;
			return true;
		}
	}
	
	public static abstract class FormattedRecord extends Record {
		public FormattedRecord(String data) {
			super(data);
			parseData();
		}
		protected String id;
		
		public String getId() {
			return id;
		}
		private static String getStringFromArray(String [] array, String string) {
			for (String arr : array) {
				if (arr.toUpperCase().startsWith(string.toUpperCase())) return arr;
			}
			return null;
		}
		protected void parseData() {
			final int start = getData().indexOf('<');
			final int end = getData().lastIndexOf('>');
			
			if (start == -1 || end == -1) {
				throw new IllegalArgumentException("string passed to FormatRecord.parseData() doesn't contain < and >  : " + getData());
			}
			
			String [] params = getData().substring(start + 1, end).split(Constants.COMMA_STRING);
			if (null == params || params.length == 0) {
				throw new IllegalArgumentException("string passed to FormatRecord.parseData doesn't contain < and >  : " + getData());
			}
			
			String idString = getStringFromArray(params, ID);
			this.id =  StringUtils.isNullOrEmpty(idString) ? null : idString.substring(ID.length() + 1);
		}
	}
	
	private static  class FormatRecord extends FormattedRecord {
		public FormatRecord(String data) {
			super(data);
		}
	}
	
	private static  class FilterRecord extends FormattedRecord {
		public FilterRecord(String data) {
			super(data);
		}
	}
	
	private static  class InfoRecord extends FormattedRecord {
		
		public InfoRecord(String data) {
			super(data);
		}
	}
	public static  class QPGRecord extends FormattedRecord implements Comparable<QPGRecord> {
		private final int order;
		
		public QPGRecord(String data) {
			super(data);
			order = Integer.parseInt(this.id);
		}
		
		public int getOrder() {
			return order;
		}
		public String getTool() {
			String [] params = getData().split(Constants.COMMA_STRING);
			return StringUtils.getValueFromKey(StringUtils.getStringFromArray(params, TOOL), TOOL);
		}
		public String getCommandLine() {
			String [] params = getData().split(Constants.COMMA_STRING);
			String clWithAngledBracket = StringUtils.getValueFromKey(StringUtils.getStringFromArray(params, COMMAND_LINE), COMMAND_LINE);
			if (clWithAngledBracket.endsWith(">")) {
				return clWithAngledBracket.substring(0, clWithAngledBracket.length() -1);
			}
			return clWithAngledBracket;
		}
		public String getDate() {
			String [] params = getData().split(Constants.COMMA_STRING);
			return StringUtils.getValueFromKey(StringUtils.getStringFromArray(params, DATE), DATE);
		}
		public String getVersion() {
			String [] params = getData().split(Constants.COMMA_STRING);
			return StringUtils.getValueFromKey(StringUtils.getStringFromArray(params, VERSION), VERSION);
		}

		@Override
		public int compareTo(QPGRecord o) {
			return Integer.compare(o.order, this.order);
		}
	}
	
	private static class FormattedRecords {
		private final Map<String, FormattedRecord> lines = new HashMap<>(4);
		public void add(FormattedRecord record) {
			lines.put(record.getId(), record);
		}
		public FormattedRecord get(String key) {
			return lines.get(key);
		}
		public Map<String, FormattedRecord> getAll() {
			return lines;
		}
	}
	
	private static class SimpleRecords  {
		private final List<Record> lines = new ArrayList<>(2);
		public void add(Record record) {
			lines.add(record);
		}
		public List<Record> getRecords() {
			return lines;
		}
	}
	
	public VcfHeader(){
		metaRecords = new SimpleRecords();
		otherRecords = new SimpleRecords();
		formatRecords = new FormattedRecords();
		infoRecords = new FormattedRecords();
		filterRecords = new FormattedRecords();
		
		qpgRecords = new ArrayList<>(2);
	}
	
	public void parseHeaderLine(String line) {
		
		if (StringUtils.isNullOrEmpty(line)) {
			throw new IllegalArgumentException("null or empty string passed to VcfHeaderUtils.parseHeaderLine: " + line);
		}
		
		line = line.trim().replaceAll("\n", "");
		// get type of header line
		Record record = null;
		
		// Is this an Info line?
		if (line.toUpperCase().startsWith(MetaType.FORMAT.toString()) )  {
			record = new FormatRecord(line);
			addFormat(record);
		} else if (line.toUpperCase().startsWith(MetaType.FILTER.toString()) ) {  
			record = new FilterRecord(line);
			addFilter(record);
		} else if (line.toUpperCase().startsWith(MetaType.INFO.toString()) )  {
			record = new InfoRecord(line);
			addInfo(record);
		} else if (line.toUpperCase().startsWith(MetaType.QPG.toString().toUpperCase()) ) {  
			record = new QPGRecord(line);
			addQPG((QPGRecord) record);
		} else if (line.toUpperCase().startsWith(MetaType.CHROM.toString())) { 
			chromLine = new Record(line);
		} else if (line.startsWith(VcfHeaderUtils.STANDARD_SOURCE_LINE)) {
			source = new Record(line);
		} else if (line.startsWith(VcfHeaderUtils.STANDARD_FILE_VERSION)) {
			version = new Record(line);
		} else if (line.startsWith(VcfHeaderUtils.STANDARD_FILE_DATE)) {
			fileDate = new Record(line);
		} else if (line.startsWith(VcfHeaderUtils.STANDARD_UUID_LINE)) {
			uuid = new Record(line);
		} else {
			if( ! line.startsWith(MetaType.OTHER.toString())) {
				throw new IllegalArgumentException("can't convert String into VcfHeaderRecord since missing \"##\" at the begin of line: " + line);
			}
			record = new Record(line);
			if (line.indexOf('=') >= 0) {
				metaRecords.add(record);
			} else {
				otherRecords.add(record);
			}
		}
	}
	
	public void addInfo(Record rec) {
		infoRecords.add((FormattedRecord) rec);
	}
	public void addFormat(Record rec) {
		formatRecords.add((FormattedRecord) rec);
	}
	public void addFilter(Record rec) {
		filterRecords.add((FormattedRecord) rec);
	}

	public VcfHeader(final List<String> headerRecords)   {
		this();	
		
		for (String record : headerRecords)  {
			parseHeaderLine(record);
		}
		
		//in case missing header line
		if(chromLine == null) {
			chromLine = new Record(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
		}
	 }
	
	//only meta data header line can be replaced
	public void replace(String record) {
		if (StringUtils.isNullOrEmpty(record)) {
			throw new IllegalArgumentException("null or empty record passed to VcfHeader.replace()");
		}
		if ( ! record.startsWith(Constants.HASH_STRING)) {
			throw new IllegalArgumentException("invalid record passed to VcfHeader.replace(): " + record);
		}
		// see if supplied record contains an "ID=" string - if so
		int index = record.indexOf(Constants.EQ);
		if (index >= 0) {
			String id = record.substring(0, index);
		
			// check to see if this record already exists in the meta collection
			Iterator<Record> iter = metaRecords.getRecords().iterator();
			while (iter.hasNext()) {
				Record rec = iter.next();
				String recId = rec.getData().substring(0, rec.getData().indexOf(Constants.EQ));
	 			if (recId.equals(id)) {
	 				iter.remove();
	 			}
			}
		}
		
//		if(record.type.equals(MetaType.META)){
//			final Iterator<VcfHeaderRecord> it = meta.iterator();
//			while(it.hasNext()) {
//				if(it.next().getId().equalsIgnoreCase(record.getId())) {
//					it.remove();
//				}
//			}
//		}
		
		//others go to add method directly
		parseHeaderLine(record);
	}
	
//	public VcfHeader.Record get(MetaType type, final String key) throws Exception{
//		
//		//remove "=" and space
//		String id = ( key.endsWith("=") )? key.substring(0,key.length() - 1) : key;
//		id = id.replaceAll(" ", "");
// 		
//		SimpleRecords records = null;
//		switch (type) {
//			case FORMAT:
//				return formatRecords.get(id);
//			case FILTER:
//				return filterRecords.get(id);
//			case INFO:
//				return infoRecords.get(id);
//			case CHROM:
//				return chromLine;
//			case META:
//				records = metaRecords;
//				break;
//			case OTHER:
//				records = otherRecords;
//				break;
//			default:
//				throw new Exception(" can't retrive vcf header record by (metaTyp, id): (" + type.name() + ", " + id +").");
//		}
//		
//		if (null != records) {
//			for (Record r : records.getRecords()) {
//				if (r.getData().contains(key)) {
//					return r;
//				}
//			}
//		}
//		 					 
//		return null;	 
//	}
	
	public String[] getSampleId() {
		if(chromLine == null)
			throw new RuntimeException("missing vcf header line, eg. " + VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
		
		final String[] column = chromLine.toString().trim().split(Constants.TAB+"");
		
		if(column[8].equalsIgnoreCase(FORMAT) && column.length > 9)
			return Arrays.copyOfRange(column, 9, column.length);
		
		return null;
		
	}
	
	public List<QPGRecord> getqPGLines() {
		Collections.sort(qpgRecords);
		return qpgRecords;
	}
 
//	public void add(VcfHeaderRecord record)  {
//		if (record.type.equals(MetaType.FILTER)  ) { 
//			vcfFilterById.put(record.getId(), (VcfHeaderFilter) record );
//		} else if (record.type.equals(MetaType.FORMAT)  ) { 
//			vcfFormatById.put(record.getId(), (VcfHeaderFormat) record );
//		} else if (record.type.equals(MetaType.INFO)  ) {
//			vcfInfoById.put(record.getId(), (VcfHeaderInfo) record);	
//		} else if (record.type.equals(MetaType.QPG)  ) {
//			qPGLines.add((VcfHeaderQPG) record); 	
//		} else if (record.type.equals(MetaType.CHROM) ) {
//			chromLine = record;
//		} else if (record.type.equals(MetaType.OTHER ) ) {
//			others.add(record);
//		} else if (record.type.equals(MetaType.META)) {	
//			if (record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_FILE_VERSION)){
//				version = record;				
//			} else if (record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_FILE_DATE)){
//				fileDate = record;
//			} else if (record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_UUID_LINE )){
//				uuid = record;			 
//			} else if (record.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_SOURCE_LINE )){
//				source = record; 
//			} else if (record.getId().equalsIgnoreCase(VcfHeaderUtils.PREVIOUS_UUID_LINE )){
//				preuuid = record;
//			} else { 
//				meta.add(record);		
//			}	
//		} else {
//			throw new IllegalArgumentException("invalid or duplicated Vcf header record: " + record.toString());
//		}
//
//	}

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
	public Map<String, FormattedRecord> getInfoRecords() {
		return infoRecords.getAll();
	}
	/**
	 * Get all VcfInfo entries
	 * @return
	 */
	public Map<String, FormattedRecord> getFormatRecords() {
		return formatRecords.getAll();
	}
	public Map<String, FormattedRecord> getFilterRecords() {
		return filterRecords.getAll();
	}
	public List<Record> getMetaRecords() {
		return metaRecords.getRecords();
	}
	public List<Record> getOtherRecords() {
		return otherRecords.getRecords();
	}

	/**
	 * Get Info type for a given ID
	 * @param id
	 * @return
	 */
	public FormattedRecord getVcfInfo(String id) {		 
		return infoRecords.get(id);
	}
	
	public FormatRecord getVcfFormat(String id) {		 
		return (FormatRecord) formatRecords.get(id);
	}
	
	public FilterRecord getVcfFilter(String id) {	
		return (FilterRecord) filterRecords.get(id);
	}
	
	public Record getUUID() {
		return uuid;
	}
	public Record getFileVersion() {
		return version;
	}
	public Record getFileDate() {
		return fileDate;
	}
	public Record getSource() {
		return source;
	}
	public Record getChrom() {
		return chromLine;
	} 
	
	
	/**
 	 * it will keep same for version, date, uuid and source if all parameters are null
	 * @param updateVersion: replace version with new value
	 * @param updateDate: replace fileDate with new value
	 * @param updateuuid: replace uuid with new value, push previous uuid to "preuuid"
	 * @param source: replace source with new value
	 * @throws Exception 
	 */
//	public void updateHeader(String updateVersion, String updateDate, String updateUuid, String updateSource) {
//		if (updateVersion != null) {
//			version = new Record(updateVersion);
//		}
//		if (updateDate != null) {
//			fileDate = new Record(updateDate);		
//		}
//		if (updateSource != null) {
//			source = new Record(updateSource);
//		}
//		if (updateUuid != null) {
//			uuid = new Record(updateUuid);	
//		}
//	}
	
	boolean containsQIMRDetails() {
		return (null != uuid && ! qpgRecords.isEmpty() );
	}
//	public void updateHeader(VcfHeaderRecord updateVersion, VcfHeaderRecord updateDate, 
//			VcfHeaderRecord updateUuid, VcfHeaderRecord updateSource) throws Exception{
//		if(updateVersion != null) version = updateVersion;
//		if(updateDate != null) fileDate = updateDate;		
//		if(updateSource != null) source = updateSource;
//		if(updateUuid != null) uuid = updateUuid;	
//		
//	}
	/**
	 * return (internally) sorted vcf header iterator
	 */
	@Override	
	public Iterator<Record> iterator() {
		final List<Record> records = new ArrayList<>();
		if (version != null)  {
			records.add(version);
		}
		
		if (fileDate != null) {  
			records.add(fileDate);
		}
		
		// add in a blank line if we have existing data
		if ( ! records.isEmpty() && containsQIMRDetails()) {
			records.add(new Record("##"));
		}
	 
		if (uuid != null) {  
			records.add(uuid);
		}
	 
		if (source != null)  {
			records.add(source);
		}
		
		// want these sorted
		Collections.sort(qpgRecords);
		for (Record record : qpgRecords)  {
			records.add(record);
		}
		
		for (Record record : metaRecords.getRecords()){  
			records.add(record);
		}
		 
		for (Record record : otherRecords.getRecords()) {
			if( !record.toString().equals( "##"))
				records.add(record);
		}
		
		// add in a blank line if we have existing data
		if ( ! records.isEmpty() && containsQIMRDetails()) {
			records.add(new Record("##"));
		}
		 
		for (FormattedRecord record : infoRecords.getAll().values()) {
			records.add(record);
		}
		 
		for (FormattedRecord record : filterRecords.getAll().values()) {
			records.add(record);
		}
		 
		for (FormattedRecord record : formatRecords.getAll().values()){ 
			records.add(record);	 
		}
		 
		if (chromLine != null) {
			records.add(chromLine);
		}
		
		return records.iterator();
	}

	public void addFilterLine(String id, String description) {
		addFilter(new FilterRecord(	"##FILTER=<ID=" + id + ",Description=\"" + description + "\">"));
	} 
	public void addFormatLine(String id, String number2, String type, String description) {
		addFormat(new FormatRecord(	"##FORMAT=<ID=" + id + ",Number=" + number2 + ",Type=" + type + ",Description=\"" + description + "\">"));
	} 

	public void addInfoLine(String id, String number, String type, String description) {
		addInfo(new InfoRecord(	"##INFO=<ID=" + id + ",Number=" + number + ",Type=" + type + ",Description=\"" + description + "\">"));
	}


	public void addQPGLine(int i, String tool, String version2, String commandLine, String date) {
		addQPG(new QPGRecord("##QPG=<ID=" + i + 
				Constants.COMMA + TOOL + Constants.EQ + tool +				
				Constants.COMMA + VERSION + Constants.EQ + version2 +
				Constants.COMMA + DATE + Constants.EQ + date +
				Constants.COMMA + COMMAND_LINE + Constants.EQ + "\"" + commandLine + "\"" +
				">"));
	} 
	public void addQPG(QPGRecord rec) {
		qpgRecords.add(rec);
	}
}
