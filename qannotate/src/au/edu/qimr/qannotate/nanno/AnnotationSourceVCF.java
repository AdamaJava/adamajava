package au.edu.qimr.qannotate.nanno;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.string.StringUtils;
import org.qcmg.qio.record.RecordReader;

public class AnnotationSourceVCF extends AnnotationSource {
	
	public static final String FIELD_DELIMITER_SEMI_COLON = ";";
	
	
	
	List<String> annotationFields;
	String emptyRecordResult;

	public AnnotationSourceVCF(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord,
			int refPositionInFile, int altPositionInFile, String fieldNames, boolean chrStartsWithChr) {
		super(reader, chrPositionInRecord, positionPositionInRecord, refPositionInFile, altPositionInFile, chrStartsWithChr);
		// TODO Auto-generated constructor stub
		
		if (StringUtils.isNullOrEmpty(fieldNames)) {
			throw new IllegalArgumentException("Null or empty fieldNames parameter passed to AnnotationSourceVCF ctor");
		}
		/*
		 * should check to ensure the header contains the request field names
		 */
		
		annotationFields = Arrays.stream(fieldNames.split(",")).collect(Collectors.toList());
		emptyRecordResult = getEmptyRecordReturnValue(fieldNames);
	}

	@Override
	public String annotationToReturn(String [] record) {
		if (null == record || record.length == 0) {
			return emptyRecordResult;
		}
		/*
		 * dealing with a vcf file and assuming that the required annotation fields are in the INFO field
		 * so get that and go from there.
		 */
		String info = record[7];
		
		/*
		 * entries in the INFO field are delimited by ';'
		 */
		return extractFieldsFromInfoField(info, annotationFields, emptyRecordResult);
	}
	
	
	public static String extractFieldsFromInfoField(String info, List<String> fields, String emptyInfoFieldResult) {
		if (StringUtils.isNullOrEmptyOrMissingData(info)) {
			return emptyInfoFieldResult;
		}
		StringBuilder dataToReturn = new StringBuilder();
		for (String af : fields) {
			if ( ! StringUtils.isNullOrEmpty(af)) {
				int start = info.indexOf(af + "=");
				if (start > -1) {
					int end = info.indexOf(FIELD_DELIMITER_SEMI_COLON, start);
					if (end == -1) {
						dataToReturn.append((!dataToReturn.isEmpty()) ? FIELD_DELIMITER_TAB + info.substring(start) : info.substring(start));
					} else {
						dataToReturn.append((!dataToReturn.isEmpty()) ? FIELD_DELIMITER_TAB + info.substring(start, end) : info.substring(start, end));
					}
				} else {
					dataToReturn.append((!dataToReturn.isEmpty()) ? FIELD_DELIMITER_TAB + af + "=" : af + "=");
				}
			}
		}
		return (dataToReturn.isEmpty()) ? emptyInfoFieldResult : dataToReturn.toString();
	}
	
	@Override
	public void close() throws IOException {
		if (null != reader) {
			reader.close();
		}
		
	}

	

}
