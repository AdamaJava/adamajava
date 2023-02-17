package au.edu.qimr.qannotate.nanno;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.RecordReader;

public class AnnotationSourceTSV extends AnnotationSource {
	
	String emptyRecordResult;
	List<String> headerLines;
	Map<String, Integer> headerNameAndPosition;

	public AnnotationSourceTSV(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord,
			int refPositionInFile, int altPositionInFile, String fieldNames) {
		super(reader, chrPositionInRecord, positionPositionInRecord, refPositionInFile, altPositionInFile);
		// TODO Auto-generated constructor stub
		
		if (StringUtils.isNullOrEmpty(fieldNames)) {
			throw new IllegalArgumentException("Null or empty fieldNames parameter passed to AnnotationSourceVCF ctor");
		}
		/*
		 * should check to ensure the header contains the request field names
		 */
		
		emptyRecordResult = getEmptyRecordReturnValue(fieldNames);
		headerLines = reader.getHeader();
		if (headerLines.isEmpty()) {
			throw new IllegalArgumentException("No headers for AnnotationSourceTSV!");
		}
		
		String headerLine = getLastHeaderLine(headerLines);
		if (StringUtils.isNullOrEmpty(headerLine)) {
			throw new IllegalArgumentException("No headers for AnnotationSourceTSV!");
		}
		
		headerNameAndPosition = getHeaderNameAndPositions(fieldNames, headerLine);
		if (headerNameAndPosition.isEmpty()) {
			throw new IllegalArgumentException("Could not find requested fields (" + fieldNames + ") in header: " + headerLine);
		}
	}
	
	/*
	 * At present, return the last line in the list
	 * May need to make this more sophisticated in future...
	 */
	public static String getLastHeaderLine(List<String> headerLines) {
		String header = "";
		if (headerLines.size() == 1) {
			/*
			 * easy
			 */
			header = headerLines.get(0);
		} else if (headerLines.size() > 1) {
			/*
			 * going to assume that the last line contains the header line
			 */
			header = headerLines.get(headerLines.size() - 1);
		}
		return header;
	}
	
	/*
	 * return an empty map if any of the fields are not in the header
	 */
	public static Map<String, Integer> getHeaderNameAndPositions(String fieldNames, String header) {
		Map<String, Integer> namePositions = new HashMap<>();
		
		System.out.println("header: " + header);
		
		for (String s : fieldNames.split(",")) {
			int indexOf = header.indexOf(s);
			if ( -1 == indexOf) {
				return Collections.emptyMap();
			}
			namePositions.put(s, StringUtils.getCount(header.substring(0, indexOf), '\t'));
		}
		
		return namePositions;
	}

	@Override
	public String annotationToReturn(String record) {
		if (null == record) {
			return emptyRecordResult;
		}
		
		/*
		 * entries in the INFO field are delimited by ';'
		 */
		return extractFieldsFromRecord(record, headerNameAndPosition);
	}
	
	public static String extractFieldsFromRecord(String record, Map<String, Integer> fields) {
		String dataToReturn = "";
		if ( ! StringUtils.isNullOrEmpty(record) && null != fields) {
			String [] recordArray = TabTokenizer.tokenize(record);
			for (Entry<String, Integer> entry : fields.entrySet()) {
				/*
				 * make sure that array length is not shorter than entry value
				 */
				if (recordArray.length > entry.getValue().intValue()) {
					dataToReturn += (dataToReturn.length() > 0 ? FIELD_DELIMITER_TAB : "") + entry.getKey() + "=" + recordArray[entry.getValue().intValue()];
				}
			}
		}
		return dataToReturn;
	}
	
	@Override
	public void close() throws IOException {
		if (null != reader) {
			reader.close();
		}
	}

}
