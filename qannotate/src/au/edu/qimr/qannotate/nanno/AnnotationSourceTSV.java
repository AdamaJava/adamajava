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
		headerNameAndPosition = getHeaderNameAndPositions(fieldNames, headerLines);
	}
	
	public static Map<String, Integer> getHeaderNameAndPositions(String fieldNames, List<String> headerLines) {
		/*
		 * have already checked fieldNAmes and headerLines - they are neither null nor empty
		 */
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
		
		return getHeaderNameAndPositions(fieldNames, header);
	}
	
	public static Map<String, Integer> getHeaderNameAndPositions(String fieldNames, String header) {
		Map<String, Integer> namePositions = new HashMap<>();
		
		System.out.println("header: " + header);
		
		for (String s : fieldNames.split(",")) {
			namePositions.put(s, StringUtils.getCount(header.substring(0, header.indexOf(s)), '\t'));
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
	
	public static int[] getFieldPositionsFromString(String sFieldPositions) {
		return Arrays.stream(sFieldPositions.split(",")).mapToInt(Integer::parseInt).map(i -> i - 1).sorted().toArray();
	}
	
	
	public static String extractFieldsFromRecord(String record, Map<String, Integer> fields) {
		String dataToReturn = "";
		String [] recordArray = TabTokenizer.tokenize(record);
		for (Entry<String, Integer> entry : fields.entrySet()) {
			dataToReturn += (dataToReturn.length() > 0 ? FIELD_DELIMITER_TAB : "") + entry.getKey() + "=" + recordArray[entry.getValue()];
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
