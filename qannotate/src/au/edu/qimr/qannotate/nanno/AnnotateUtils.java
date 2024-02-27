package au.edu.qimr.qannotate.nanno;

import au.edu.qimr.qannotate.nanno.AnnotationInputs.AnnotationInput;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qio.record.StringFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotateUtils {

    public static final QLogger logger = QLoggerFactory.getLogger(AnnotateUtils.class);

    public static AnnotationInputs getInputs(String file) throws IOException {
        //read json file data to String
        byte[] jsonData = Files.readAllBytes(Paths.get(file));
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //convert json string to object

        return objectMapper.readValue(jsonData, AnnotationInputs.class);
    }

    public static Comparator<String[]> createComparatorFromList(final List<String> sortedList) {
        return (o1, o2) -> {
            final int index1 = sortedList.indexOf(o1[0]);
            if (index1 == -1) return 1;
            final int index2 = sortedList.indexOf(o2[0]);
            if (index2 == -1) return -1;
            return index1 - index2;
        };
    }

    /**
     * @param ais
     * @param annotationSources
     * @throws IOException
     */
    public static void populateAnnotationSources(AnnotationInputs ais, List<AnnotationSource> annotationSources) throws IOException {
        for (AnnotationInput ai : ais.getInputs()) {
            String fileName = ai.getFile();
            String fieldNames = ai.getFields();

            logger.info("fileName: " + fileName + ", positions: " + ai.getChrIndex() + ", " + ai.getPositionIndex() + ", " + ai.getRefIndex() + ", " + ai.getAltIndex() + ", fieldNames: " + fieldNames);

            if (ai.isSnpEffVcf()) {
                annotationSources.add(new AnnotationSourceSnpEffVCF(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames, ai.isChrStartsWithChr()));
            } else if (fileName.contains("vcf")) {
                annotationSources.add(new AnnotationSourceVCF(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames, ai.isChrStartsWithChr()));
            } else {
                annotationSources.add(new AnnotationSourceTSV(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames, ai.isChrStartsWithChr()));
            }
        }
    }

    public static int checkHeaders(AnnotationInputs ais) {
        List<String> annotationFields = ais.getInputs().stream().map(AnnotationInput::getFields).toList();
        boolean headersValid = AnnotateUtils.isOrderedHeaderListValid(ais.getOutputFieldOrder(), annotationFields.toArray(new String[]{}));

        if (!headersValid) {
            System.err.println("headers are not valid! OrderedHeader: " + ais.getOutputFieldOrder() + "\nAnnotation fields: " + (ais.getInputs().stream().map(AnnotationInput::getFields)).collect(Collectors.joining(",")));
            return 1;
        }
        return 0;
    }

    /**
     * Checks if the ordered list of headers is valid.
     *
     * @param sortedHeader                The sorted header string.
     * @param fieldsFromAnnotationSources The fields from annotation sources.
     * @return Returns true if the ordered header list is valid, false otherwise.
     */
    public static boolean isOrderedHeaderListValid(String sortedHeader, String... fieldsFromAnnotationSources) {
        if (StringUtils.isNullOrEmpty(sortedHeader)) {
            /*
             * empty or null sorted header - not valid
             */
            logger.error("sortedHeader is null or empty");
            return false;
        }
        if (null == fieldsFromAnnotationSources || fieldsFromAnnotationSources.length == 0) {
            /*
             * empty or null annotation fields - not valid
             */
            logger.error("fieldsFromAnnotationSources is null or length is 0");
            return false;
        }

        Set<String> sortedHeaderSet = Arrays.stream(sortedHeader.split(",")).collect(Collectors.toSet());
        Set<String> fieldsFromAnnotationSourcesSet = Arrays.stream(String.join(",", fieldsFromAnnotationSources).split(",")).collect(Collectors.toSet());

        for (String s : sortedHeaderSet) {
            if (!fieldsFromAnnotationSourcesSet.contains(s)) {
                logger.error(s + " in header but not found in any data source!");
            }
        }
        for (String s : fieldsFromAnnotationSourcesSet) {
            if (!sortedHeaderSet.contains(s)) {
                logger.error(s + " in data source but not found in header!");
            }
        }

        return sortedHeaderSet.containsAll(fieldsFromAnnotationSourcesSet) && fieldsFromAnnotationSourcesSet.containsAll(sortedHeaderSet);
    }

    public static String getEmptyHeaderValues(int count) {
        if (count <= 0) {
            return "";
        }
        return org.apache.commons.lang3.StringUtils.repeat("\t", count);
    }

    public static int countOccurrences(String s, String t) {
        return org.apache.commons.lang3.StringUtils.countMatches(s, t);
    }

    /**
     * Create a PubMed search term using the hgvsC and hgvsP values
     *
     * @param hgvsC
     * @param hgvsP
     * @return
     */
    public static String getSearchTerm(String hgvsC, String hgvsP) {
        String st = "";

        /*
         * check the optionals - if they are both not present, no need to proceed
         */
        if ((hgvsC == null && hgvsP == null)) {
            return st;
        }

        if (hgvsC != null && !hgvsC.isEmpty()) {

            /*
             * need to check that the string contains the dot ('.') and the gt sign ('>')
             */
            int dotIndex = hgvsC.indexOf('.');
            int gtIndex = hgvsC.indexOf('>');
            if (dotIndex > -1 && gtIndex > -1) {

                /*
                 * split value into required parts
                 */
                String firstPart = hgvsC.substring(dotIndex + 1, gtIndex);
                String secondPart = hgvsC.substring(gtIndex + 1);

                st += Annotate.SEARCH_TERM_VARIETIES.stream().map(s -> "\"" + firstPart + s + secondPart + "\"").collect(Collectors.joining("|"));
            }
        }

        if (hgvsP != null && !hgvsP.isEmpty()) {
            if (!st.isEmpty()) {
                /*
                 * we must have hgvs.c data - so add bar
                 */
                st += "|";
            }
            st += "\"" + hgvsP.substring(hgvsP.indexOf('.') + 1) + "\"";
        }

        if (!st.isEmpty()) {
            return "\"GENE\"+(" + st + ")";
        }
        return st;
    }

    /**
     * Splits the strings in the supplied list by tab, and flattens them to a single list
     */
    public static List<String> convertAnnotations(List<String> manyAnnotations) {
        if (null != manyAnnotations) {
            return manyAnnotations.stream().flatMap(s -> java.util.Arrays.stream(TabTokenizer.tokenize(s))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * get the requiredAnnotation value from the list of annotations
     * return null if not present
     *
     * @param listOfAnnotations
     * @param requiredAnnotation
     * @return
     */
    public static Optional<String> getAnnotationFromList(List<String> listOfAnnotations, String requiredAnnotation) {

        if (null != listOfAnnotations && !StringUtils.isNullOrEmpty(requiredAnnotation)) {
            for (String anno : listOfAnnotations) {
                if (anno.startsWith(requiredAnnotation)) {
                    return Optional.of(anno.substring(requiredAnnotation.length() + 1));        // don't forget the equals sign
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves the AD (allele depth) values for split VCF records.
     *
     * @param altArray An array of alternate alleles.
     * @param gatkAD   The GATK AD field containing comma-separated allele depth values.
     * @return A map of alternate alleles to their corresponding AD values.
     */
    public static Map<String, String> getADForSplitVcfRecords(String[] altArray, String gatkAD) {

        Map<String, String> altToADMap = new HashMap<>(4);
        String[] gatkADArray = gatkAD.split(",");
        /*
         * should have 1 more in the gatkADArray than the altArray
         */
        if (altArray.length == gatkADArray.length - 1) {
            for (int i = 0; i < altArray.length; i++) {
                altToADMap.put(altArray[i], gatkADArray[0] + "," + gatkADArray[i + 1]);
            }
        }

        return altToADMap;
    }

    public static List<String> generateHeaders(AnnotationInputs ais, QExec exec) {
        List<String> headers = new ArrayList<>();
        if (null != exec) {
            headers.add("##" + exec.getStartTime().toLogString());
            headers.add("##" + exec.getUuid().toLogString());
            headers.add("##" + exec.getHost().toLogString());
            headers.add("##" + exec.getRunBy().toLogString());
            headers.add("##" + exec.getJavaVersion().toLogString());
            headers.add("##" + exec.getToolName().toLogString());
            headers.add("##" + exec.getToolVersion().toLogString());
            headers.add("##" + exec.getCommandLine().toLogString());
        }
        if (null != ais && null != ais.getInputs()) {

            for (AnnotationInput ai : ais.getInputs()) {
                headers.add("##file:fields\t" + ai.getFile() + ":" + ai.getFields());
            }

            String emptyHeaders = ais.getAdditionalEmptyFields();
            String[] emptyHeadersArray = StringUtils.isNullOrEmpty(emptyHeaders) ? new String[]{} : emptyHeaders.split(",");
            String fieldOrder = ais.getOutputFieldOrder();
            String[] fieldOrderArray = StringUtils.isNullOrEmpty(fieldOrder) ? new String[]{} : fieldOrder.split(",");

            String header = "#chr\tposition\tref\talt\toriginal_alt\tGATK_GT\tGATK_AD\t" + String.join("\t", fieldOrderArray);
            if (emptyHeadersArray.length > 0) {
                header += "\t" + String.join("\t", emptyHeadersArray);
            }

            boolean includeSearchTerm = ais.isIncludeSearchTerm();
            header += (includeSearchTerm ? "\tsearchTerm" : "");
            headers.add(header);
        }

        return headers;
    }

    public static String generateAdditionalEmptyValues(AnnotationInputs ais) {
        String emptyHeaders = ais.getAdditionalEmptyFields();

        if (StringUtils.isNullOrEmpty(emptyHeaders)) {
            return "";
        } else {
            return getEmptyHeaderValues(org.apache.commons.lang3.StringUtils.countMatches(emptyHeaders, ",") + 1);
        }
    }

}
