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

	private String[] fieldNames;
	private int[] fieldPositions;

	public AnnotationSourceTSV(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord,
			int refPositionInFile, int altPositionInFile, String fieldNames, boolean chrStartsWithChr) {
		super(reader, chrPositionInRecord, positionPositionInRecord, refPositionInFile, altPositionInFile, chrStartsWithChr);
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
		// precompute arrays for fast extraction, preserving user-requested field order
		String[] requestedFields = fieldNames.split(",");
		this.fieldNames = requestedFields;
		this.fieldPositions = new int[requestedFields.length];
		for (int i = 0; i < requestedFields.length; i++) {
			this.fieldPositions[i] = headerNameAndPosition.get(requestedFields[i]);
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
			header = headerLines.getFirst();
		} else if (headerLines.size() > 1) {
			/*
			 * going to assume that the last line contains the header line
			 */
			header = headerLines.getLast();
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
	public String annotationToReturn(String[] record) {
		if (null == record || record.length == 0) {
			return emptyRecordResult;
		}
		
		/*
		 * entries in the INFO field are delimited by ';'
		 */
		return extractFieldsFromRecord(record, fieldNames, fieldPositions);
	}
	
	public static String extractFieldsFromRecord(String[] record, String[] fieldNames, int[] fieldPositions) {
		StringBuilder dataToReturn = new StringBuilder();
		int recordLength = null != record ? record.length : 0;
		if (recordLength > 0 && null != fieldNames && null != fieldPositions) {
			for (int i = 0; i < fieldNames.length; i++) {
				int pos = fieldPositions[i];
				if (recordLength > pos) {
					dataToReturn.append((!dataToReturn.isEmpty()) ? FIELD_DELIMITER_TAB : "")
							.append(fieldNames[i]).append("=").append(record[pos]);
				}
			}
		}
		return dataToReturn.toString();
	}
	
	@Override
	public void close() throws IOException {
		if (null != reader) {
			reader.close();
		}
	}

}
