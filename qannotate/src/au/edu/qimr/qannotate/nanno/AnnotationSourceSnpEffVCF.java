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
            new SimpleEntry<>("effect", 1),    // annotation is also known as effect
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
    public String getAnnotation(long requestedCpAsLong, ChrPosition requestedCp) {

        /*
         * check to see if the records we currently have stored are a match
         */
        if (requestedCpAsLong == currentCPAsLong) {

            /*
             * we match on position
             * lets see if there are any records that match on ref and alt
             */
            return getAnnotationsFromRecords(requestedCp);
        } else {
            int matchWithNextCP = Long.compare(requestedCpAsLong, nextCPAsLong);
            if (nextCPAsLong > -1 && matchWithNextCP < 0) {

            } else {

                getNextRecord(requestedCpAsLong, matchWithNextCP);
                if (requestedCpAsLong == currentCPAsLong) {
                    /*
                     * we match on position
                     * lets see if there are any records that match on ref and alt
                     */
                    return getAnnotationsFromRecords(requestedCp);
                }
            }
        }
        return annotationToReturn(null);
    }

    private String getAnnotationsFromRecords(ChrPosition requestedCp){
        if (requestedCp instanceof ChrPositionRefAlt reqCpRefAlt) {
            String reqRef = reqCpRefAlt.getRef();
            String reqAlt = reqCpRefAlt.getAlt();
            for (String rec : currentRecords) {
                String[] recArray = TabTokenizer.tokenize(rec, DEFAULT_DELIMITER);
                String recRef = recArray[refPositionInFile];
                String recAlt = recArray[altPositionInFile];

                if (recAlt.indexOf(',') >= 0) {
                    String[] recAltArray = recAlt.split(",");
                    for (String recAltValue : recAltArray) {
                        if (reqRef.equals(recRef) && reqAlt.equals(recAltValue)) {
                            return annotationToReturnWithAlt(rec, recAltValue);
                        }
                    }
                } else {
                    if (reqRef.equals(recRef) && reqAlt.equals(recAlt)) {
                        return annotationToReturnWithAlt(rec, recAlt);
                    }
                }
            }
        }
        return annotationToReturn(null);
    }

    @Override
    public String annotationToReturn(String[] record) {
        if (null == record) {
            return emptyRecordResult;
        }
        /*
         * dealing with a vcf file and assuming that the required annotation fields are in the INFO field
         * so get that and go from there.
         */
        String info = record[7];
        String alt = record[4];

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
        String[] recordArray = record.split("\t");
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

        StringBuilder dataToReturn = new StringBuilder();
        String worstConsequence = getWorstConsequence(info, alt);

        /*
         * if we didn't have a match on alt, return the empty result
         */
        if (StringUtils.isNullOrEmpty(worstConsequence)) {
            return emptyInfoFieldResult;
        }
        /*
         * we have our consequences (comma-delimited)
         * split by comma into consequences, then by pipe into fields
         */
        String[] consequences = worstConsequence.split(",");

        for (String af : fields) {
            if (!StringUtils.isNullOrEmpty(af)) {

                String aflc = af.toLowerCase();
                Integer arrayPosition = SNP_EFF_ANNOTATION_FIELDS_AND_POSITIONS.get(aflc);

                if (null != arrayPosition) {
                    StringBuilder fieldValues = new StringBuilder();
                    for (String consequence : consequences) {
                        String[] consequenceArray = TabTokenizer.tokenize(consequence, '|');
                        if (arrayPosition >= 0 && arrayPosition < consequenceArray.length) {
                            String annotation = consequenceArray[arrayPosition];
                            fieldValues.append(fieldValues.isEmpty() ? annotation : "|" + annotation);
                        }
                    }
                    dataToReturn.append((!dataToReturn.isEmpty())
                            ? FIELD_DELIMITER_TAB + af + "=" + fieldValues
                            : af + "=" + fieldValues);
                }
            }
        }
        return (dataToReturn.isEmpty()) ? emptyInfoFieldResult : dataToReturn.toString();
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
        if (annoIndex < 0) {
            return "";
        }
        int end = info.indexOf(FIELD_DELIMITER_SEMI_COLON, annoIndex);
        String ann = info.substring(annoIndex + 4, end == -1 ? info.length() : end);


        String[] annArray = ann.split(",");
        Map<String, String> worstByGene = new java.util.LinkedHashMap<>();
        for (String aa : annArray) {
            int pipeIndex = aa.indexOf('|');
            if (pipeIndex <= 0) {
                // Malformed ANN entry or missing allele token; skip
                continue;
            }
            String alleleToken = aa.substring(0, pipeIndex);
            if (alleleToken.equals(alt)) {
                String[] parts = TabTokenizer.tokenize(aa, '|');
                if (parts.length > 3) {
                    String gene = parts[3];
                    if (!StringUtils.isNullOrEmpty(gene) && !worstByGene.containsKey(gene)) {
                        worstByGene.put(gene, aa);
                    }
                }
            }
        }
        return String.join(",", worstByGene.values());
    }

    @Override
    public void close() throws IOException {
        if (null != reader) {
            reader.close();
        }
    }

}
