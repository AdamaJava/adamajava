package au.edu.qimr.qannotate.nanno;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.AbstractMap.SimpleEntry;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.RecordReader;

public class AnnotationSourceSnpEffVCF extends AnnotationSource {
	
	public static final String FIELD_DELIMITER_SEMI_COLON = ";";
	
	public static final Map<String, Integer> SNP_EFF_ANNOTATION_FIELDS_AND_POSITIONS = Stream.of(
			new SimpleEntry<>("alt", 0),
			new SimpleEntry<>("annotation", 1),
			new SimpleEntry<>("effect", 1),	// annotation is also known as effect
			new SimpleEntry<>("putative_impact", 2),
			new SimpleEntry<>("gene_name", 3),
			new SimpleEntry<>("gene_id", 4),
			new SimpleEntry<>("feature_type", 5),
			new SimpleEntry<>("feature_id", 6),
			new SimpleEntry<>("transcript_biotype", 7),
			new SimpleEntry<>("rank", 8),
			new SimpleEntry<>("hgvs.c", 9),
			new SimpleEntry<>("hgvs.p", 10),
			new SimpleEntry<>("cdna_position", 11),
			new SimpleEntry<>("cds_position", 12),
			new SimpleEntry<>("protein_position", 13),
			new SimpleEntry<>("distance_to_feature", 14),
			new SimpleEntry<>("errors", 15),
			new SimpleEntry<>("warnings", 15),
			new SimpleEntry<>("information", 15)).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
	
	
	
	List<String> annotationFields;
	String emptyRecordResult;

	public AnnotationSourceSnpEffVCF(RecordReader<String> reader, int chrPositionInRecord, int positionPositionInRecord,
			int refPositionInFile, int altPositionInFile, String fieldNames) {
		super(reader, chrPositionInRecord, positionPositionInRecord, refPositionInFile, altPositionInFile);
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
	public String getAnnotation(ChrPosition requestedCp) {

		logger.debug(reader.getFile().getName() + ":  requestedCp is " + (null != requestedCp ? requestedCp.toIGVString() : null) + ", currentCP: " + (null != currentCP ? currentCP.toIGVString() : null) + ", nextCP: " + (null != nextCP ? nextCP.toIGVString() : null));

		/*
		 * check to see if the records we currently have stored are a match
		 */
		if ( areCPsEqual(requestedCp, currentCP) == 0) {

			/*
			 * we match on position
			 * lets see if there are any records that match on ref and alt
			 */
			for (String rec : currentRecords) {
				String [] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
				String recRef = recArray[refPositionInFile];
				String recAlt = recArray[altPositionInFile];

				if (recAlt.contains(",")) {
					String [] recAltArray = recAlt.split(",");
					for (String recAltValue : recAltArray) {
						if (((ChrPositionRefAlt)requestedCp).getRef().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAltValue)) {
							return annotationToReturnWithAlt(rec, recAltValue);
						}
					}
				} else {
					if (((ChrPositionRefAlt)requestedCp).getRef().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAlt)) {
						return annotationToReturnWithAlt(rec, recAlt);
					}
				}
			}

		} else if (null != nextCP && areCPsEqual(requestedCp, nextCP) < 0) {
			/*
			 * requestedCp is "less than" next CP
			 * return empty list here
			 */
		} else {
			logger.debug(reader.getFile().getName() + ": getting next record. requestedCp: " + (null != requestedCp ? requestedCp.toIGVString() : null) + ", currentCP: " + (null != currentCP ? currentCP.toIGVString() : null));
			getNextRecord(requestedCp);
			if ( areCPsEqual(requestedCp, currentCP) == 0) {
				/*
				 * we match on position
				 * lets see if there are any records that match on ref and alt
				 */
				for (String rec : currentRecords) {
					String [] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
					String recRef = recArray[refPositionInFile];
					String recAlt = recArray[altPositionInFile];

					if (recAlt.contains(",")) {
						String [] recAltArray = recAlt.split(",");
						for (String recAltValue : recAltArray) {
							if (((ChrPositionRefAlt)requestedCp).getRef().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAltValue)) {
								return annotationToReturnWithAlt(rec, recAltValue);
							}
						}
					} else {
						if (((ChrPositionRefAlt)requestedCp).getRef().equals(recRef) && ((ChrPositionRefAlt)requestedCp).getAlt().equals(recAlt)) {
							return annotationToReturnWithAlt(rec, recAlt);
						}
					}
				}
			} else {
				/*
				 * requestedCP and currentCP are not equal
				 */
			}
		}
		return annotationToReturn(null);
	}

	@Override
	public String annotationToReturn(String record) {
		if (null == record) {
			return emptyRecordResult;
		}
		/*
		 * dealing with a vcf file and assuming that the required annotation fields are in the INFO field
		 * so get that and go from there.
		 */
		String [] recordArray = record.split("\t"); 
		String info = recordArray[7];
		String alt = recordArray[4];
		
		/*
		 * entries in the INFO field are delimited by ';'
		 */
		logger.debug("looking for annotations in info field: " + info + ", with alt: " + alt);
		return extractFieldsFromInfoField(info, annotationFields, emptyRecordResult, alt);
	}
	public String annotationToReturnWithAlt(String record, String alt) {
		if (null == record) {
			return emptyRecordResult;
		}
		/*
		 * dealing with a vcf file and assuming that the required annotation fields are in the INFO field
		 * so get that and go from there.
		 */
		String [] recordArray = record.split("\t");
		String info = recordArray[7];

		/*
		 * entries in the INFO field are delimited by ';'
		 */
		logger.debug("looking for annotations in info field: " + info + ", with alt: " + alt);
		return extractFieldsFromInfoField(info, annotationFields, emptyRecordResult, alt);
	}
	
	
	public static String extractFieldsFromInfoField(String info, List<String> fields, String emptyInfoFieldResult, String alt) {
		if (StringUtils.isNullOrEmptyOrMissingData(info)) {
			return emptyInfoFieldResult;
		}
		
		String dataToReturn = "";
		String worstConsequence = getWorstConsequence(info, alt);
		
		/*
		 * if we didn't have a match on alt, return the empty result
		 */
		if (StringUtils.isNullOrEmpty(worstConsequence)) {
			return emptyInfoFieldResult;
		}
		
		/*
		 * we have our consequence
		 * split by pipe and then get our fields
		 */
		String [] consequenceArray = TabTokenizer.tokenize(worstConsequence, '|');

		for (String af : fields) {
			if ( ! StringUtils.isNullOrEmpty(af)) {
				
				/*
				 * get position from map
				 */
				String aflc = af.toLowerCase();
				Integer arrayPosition = SNP_EFF_ANNOTATION_FIELDS_AND_POSITIONS.get(aflc);
				if (null != arrayPosition && arrayPosition >= 0 && arrayPosition < consequenceArray.length) {
					/*
					 * good
					 */
					String annotation = consequenceArray[arrayPosition];
					dataToReturn += ! dataToReturn.isEmpty() ? FIELD_DELIMITER_TAB + af + "=" + annotation : af + "=" + annotation;
				} else {
//					System.out.println("Could not find field [" + af + "] in SNP_EFF_ANNOTATION_FIELDS_AND_POSITIONS map!");
//					System.out.println("arrayPosition.intValue(): " + arrayPosition.intValue() + ", consequenceArray.length: " + consequenceArray.length);
				}
				
			}
		}
		return dataToReturn.isEmpty() ? emptyInfoFieldResult : dataToReturn;
	}

	/**
	 * @param info
	 * @param alt
	 * @return
	 */
	public static String getWorstConsequence(String info, String alt) {
		/*
		 * SnpEff annotations are in the following format:
		 * ANN=|||||||,|||||||,||||||||
		 * ie. a comma separated (ordered) list of consequences, which in turn are pipe delimited and contain the following columns:
		 * alt|effect|Putative_impact|
		 * 
		 *  
		 *  
		 *  snpEff sorts consequences as follows:
		 *  Effect sort order. When multiple effects are reported, SnpEff sorts the effects the following way:

		 *	Putative impact: Effects having higher putative impact are first.
		 *	Effect type: Effects assumed to be more deleterious effects first.
		 *	Canonical transcript before non-canonical.
		 *	Marker genomic coordinates (e.g. genes starting before first).
		 *
		 * 
		 */
		
		/*
		 * first get the consequence corresponding to this alt
		 * There will most likely be more than 1
		 * Pick the first one as that is the one with the highest effect as decreed by snpEff
		 */
		int annoIndex = info.indexOf("ANN=");
		int end = info.indexOf(FIELD_DELIMITER_SEMI_COLON, annoIndex);
		String ann = info.substring(annoIndex + 4, end == -1 ? info.length() : end);
		
		
		String [] annArray = ann.split(",");
		String worstConsequence = "";
		for (String aa : annArray) {
			if (aa.startsWith(alt)) {
				worstConsequence = aa;
				break;
			}
		}
		return worstConsequence;
	}
	
	@Override
	public void close() throws IOException {
		if (null != reader) {
			reader.close();
		}
	}
	
}
