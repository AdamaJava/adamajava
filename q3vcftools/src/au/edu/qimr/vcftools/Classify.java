package au.edu.qimr.vcftools;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.vcf.VcfFileReader;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;
import static org.qcmg.common.util.Constants.TAB;


public class Classify {

    private static QLogger logger;
    private String[] vcfFiles;
    private String outputDirectory;
    private String goldStandard;
    private boolean vcfOutput;
    private int exitStatus;
    private boolean somatic;
    private boolean germline;

    private final Map<ChrPosition, ChrPositionRefAlt> roguePositions = new HashMap<>(1024 * 64);
    private final List<VcfRecord> roguePositionsMatches = new ArrayList<>(1024 * 64);

    protected int engage() throws IOException {

        logger.info("about to load vcf files");
        loadVcfs();
        examineMatches();
//		writeOutput();
        return exitStatus;
    }

    private void writeOutput(List<ChrPositionRefAlt> recs, String output) throws FileNotFoundException {
        recs.sort(new ChrPositionComparator());

        logger.info("writing output");


        try (PrintStream ps = new PrintStream(new FileOutputStream(output))) {
            /*
             * put input files along with their positions into the file header
             */
            int j = 1;
            for (String s : vcfFiles) {
                ps.println("##" + j++ + ": vcf file: " + s);
            }
            if (null != goldStandard) {
                ps.println("##: gold standard file: " + goldStandard);
            }


            String header = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE;
            if (vcfOutput) {
                ps.println(VcfHeaderUtils.CURRENT_FILE_FORMAT);
            }
            ps.println(header);
            for (ChrPositionRefAlt cp : recs) {
                ps.println(ChrPositionUtils.toVcfString(cp, MISSING_DATA_STRING, null, null, MISSING_DATA_STRING, MISSING_DATA_STRING, MISSING_DATA_STRING));
//                ps.println(cp.getChromosome() + TAB + cp.getStartPosition() + "\t.\t" + cp.getRef() + TAB + cp.getAlt() + "\t.\t.\t.");
            }
        }

        logger.info("writing output- DONE");
    }

    private void examineMatches() {
        logger.info("Found " + roguePositionsMatches.size() + " in vcf file out of a potential " + roguePositions.size());
        int germlineCount = 0;
        int somaticCount = 0;
        int pass = 0;
        Map<String, List<VcfRecord>> filterDistSom = new HashMap<>();
        Map<String, List<VcfRecord>> filterDistGerm = new HashMap<>();
        for (VcfRecord v : roguePositionsMatches) {
            if (VcfUtils.isRecordSomatic(v)) {
                somaticCount++;
                filterDistSom.computeIfAbsent(getFilter(v), f -> new ArrayList<>()).add(v);
            } else {
                germlineCount++;
                filterDistGerm.computeIfAbsent(getFilter(v), f -> new ArrayList<>()).add(v);
            }
            if (VcfUtils.isRecordAPass(v)) {
                pass++;
            }
        }
        logger.info("pass: " + pass + ", somaticCount: " + somaticCount + ", germlineCount: " + germlineCount);
        Comparator<Entry<String, List<VcfRecord>>> comp = Comparator.comparingInt(e -> e.getValue().size());
        filterDistSom.entrySet().stream().sorted(comp).forEach(e -> logger.info("somatic, filter: " + e.getKey() + ", count: " + e.getValue().size() + ", v: " + e.getValue().getFirst().toSimpleString()));
        filterDistGerm.entrySet().stream().sorted(comp).forEach(e -> logger.info("germline, filter: " + e.getKey() + ", count: " + e.getValue().size() + ", v: " + e.getValue().getFirst().toSimpleString()));
    }

    public static String getFilter(VcfRecord v) {
        String filter = v.getFilter();
        if (StringUtils.isNullOrEmptyOrMissingData(filter)) {
            /*
             * look to see if filter is in the format column
             */
            List<String> formatFields = v.getFormatFields();
            if (null != formatFields && !formatFields.isEmpty()) {
                if (formatFields.getFirst().contains(VcfHeaderUtils.FORMAT_FILTER)) {

                    /*
                     * get filters for all format values
                     */
                    Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(formatFields);
                    String[] filterArray = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
                    if (null != filterArray && filterArray.length > 0) {
                        filter = String.join(Constants.COMMA_STRING, filterArray);
                    }
                }
            }
        }
        return filter;
    }

    /**
     * Loads the VCF files.
     *
     * @throws IOException if an I/O error occurs
     */
    private void loadVcfs() throws IOException {
        int i = 0;
        int index = 0;
        for (String s : vcfFiles) {
            if (index == 0) {
                try (VcfFileReader reader = new VcfFileReader(new File(s))) {
                    for (VcfRecord rec : reader) {
                        if (++i % 1000000 == 0) {
                            logger.info("hit " + i + " entries");
                        }

                        processVcfRecord(rec, somatic, germline, roguePositions);
                    }
                }
                logger.info("input: " + (index + 1) + " has " + i + " entries");
                logger.info("positions size: " + roguePositions.size());
                i = 0;
            } else {
                try (VcfFileReader reader = new VcfFileReader(new File(s))) {
                    for (VcfRecord rec : reader) {
                        if (++i % 1000000 == 0) {
                            logger.info("hit " + i + " entries");
                        }

                        if (roguePositions.containsKey(rec.getChrPosition())) {
                            roguePositionsMatches.add(rec);
                        }
                    }
                }
            }
            index++;
        }
    }



    /**
     * Processes a VCF record.
     *
     * @param rec the VCF record to process
     * @param somatic whether to process somatic records
     * @param germline whether to process germline records
     * @param map the map to add the processed record to
     */
    public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPosition, ChrPositionRefAlt> map) {
        /*
         * only deal with snps and compounds for now
         */
        if (VcfUtils.isRecordASnpOrMnp(rec)) {

            String ref = rec.getRef();
            String alt = rec.getAlt();

            if (ref.length() > 1) {
                /*
                 * split cs into constituent snps
                 */
                for (int z = 0; z < ref.length(); z++) {
                    addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z) + "", alt.charAt(z) + "");
                }
            } else {
                addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt);
            }
        }
    }


    /**
     * Adds a record to the map.
     *
     * @param map the map to add the record to
     * @param chr the chromosome
     * @param start the start position
     * @param end the end position
     * @param ref the reference
     * @param alt the alt allele
     */
    public static void addToMap(Map<ChrPosition, ChrPositionRefAlt> map, String chr, int start, int end, String ref, String alt) {
        if (null != map && null != chr && start >= 0 && end >= start) {
            ChrPosition cp = new ChrPointPosition(chr, start);
            ChrPositionRefAlt cpn = new ChrPositionRefAlt(chr, start, end, ref, alt);
            map.putIfAbsent(cp, cpn);
        }
    }

    /**
     * The main method.
     *
     * @param args the command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {

        Classify qp = new Classify();
        int exitStatus = qp.setup(args);
        if (null != logger) {
            logger.logFinalExecutionStats(exitStatus);
        } else {
            System.err.println("Exit status: " + exitStatus);
        }
        System.exit(exitStatus);
    }

    /**
     * Sets up the classification process.
     *
     * @param args the command line arguments
     * @return the exit status
     * @throws Exception if an error occurs
     */
    private int setup(String[] args) throws Exception {
        int returnStatus = 1;
        Options options = new Options(args);

        if (options.hasHelpOption()) {
            System.err.println(Messages.GOLD_STANDARD_USAGE);
            options.displayHelp();
            returnStatus = 0;
        } else if (options.hasVersionOption()) {
            System.err.println(Messages.getVersionMessage());
            returnStatus = 0;
        } else if (options.getVcfs().length < 1) {
            System.err.println(Messages.GOLD_STANDARD_USAGE);
        } else {
            // configure logging
            String logFile = options.getLog();
            String version = Classify.class.getPackage().getImplementationVersion();
            if (null == version) {
                version = "local";
            }
            logger = QLoggerFactory.getLogger(Classify.class, logFile, options.getLogLevel());
            QExec exec = logger.logInitialExecutionStats("q3vcftools Overlap", version, args);

            // get list of file names
            vcfFiles = options.getVcfs();
            if (vcfFiles.length < 1) {
                throw new Exception("INSUFFICIENT_ARGUMENTS");
            } else {
                // loop through supplied files - check they can be read
                for (String vcfFile : vcfFiles) {
                    if (!FileUtils.canFileBeRead(vcfFile)) {
                        throw new Exception("INPUT_FILE_ERROR: " + vcfFile);
                    }
                }
            }
            vcfOutput = options.hasVcfOutputOption();

            // set output file - if supplied, check that it can be written to
            if (null != options.getOutputFileName()) {
                String optionsOutputFile = options.getOutputFileName();
                if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
                    outputDirectory = optionsOutputFile;
                } else {
                    throw new Exception("OUTPUT_FILE_WRITE_ERROR");
                }

            }

            logger.info("vcf input files: " + Arrays.deepToString(vcfFiles));
            logger.info("outputDirectory: " + outputDirectory);

            somatic = options.hasSomaticOption();
            germline = options.hasGermlineOption();

            if (!somatic && !germline) {
                somatic = true;
                germline = true;
            }
            if (somatic)
                logger.info("Will process somatic records");
            if (germline)
                logger.info("Will process germline records");
            if (vcfOutput)
                logger.info("Will output gold standard as a vcf file");

            options.getGoldStandard().ifPresent(s -> goldStandard = s);

            return engage();
        }
        return returnStatus;
    }

}
