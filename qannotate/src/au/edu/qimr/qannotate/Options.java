/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.qannotate;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import au.edu.qimr.qannotate.modes.HomopolymersMode;
import au.edu.qimr.qannotate.modes.IndelConfidenceMode;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/*
 * parse command line to options.
 */
public class Options {
    public enum MODE {dbsnp, germline, snpeff, confidence, vcf2maf, cadd, indelconfidence, trf, hom, ccm, make_valid, overlap, vcf2maftmp}

    protected static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
    protected static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
    protected static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
    protected static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");

    private static final String test = "test";
    private static final String control = "control";
    private final String commandLine;

    private final Options.MODE mode;
    private final OptionParser parser;
    private final String outputFileName;
    private final String inputFileName;
    private final String[] databaseFiles;
    private final String logFileName;
    private final String logLevel;

    //vcf2maf
    private final String testSample;
    private final String controlSample;

    private final int bufferSize; //trf
    private final int gap; //cadd

    private final Integer miunCutoff;
    private final Integer minCutoff;
    private final Float minPercentage;
    private final Integer nnsCount;
    private final Integer mrCount;
    private final Integer controlCoverageCutoff;
    private final Integer controlCoverageCutoffForSomatic;
    private final Integer testCoverageCutoff;
    private final Float mrPercentage;

    private final List<String> filtersToIgnore;

    //Vcf2maf
    private final String center;
    private final String sequencer;
    private final String outputDir;
    private final String donorId;

    //snpeff
    private final String configFileName;
    private final String summaryFileName;

    //hom
    private final Integer homopolymerWindow;
    private final Integer homopolymerCutoff;
    private final Integer homopolymerReportWindow;

    /**
     * check command line and store arguments and option information
     */
    public Options(final String[] args) throws IOException {
        parser = new OptionParser();
        OptionSet options = parseArgs(args);

        if (options.has("mode")) {
            String m = ((String) options.valueOf("mode")).toLowerCase();
            this.mode = MODE.valueOf(m); //already checked the validation of mode
        } else {
            this.mode = null;
        }

        if (options.has("h") || options.has("help")) {
            displayHelp(mode);
            System.exit(0);
        }

        //parse parameters
        commandLine = Messages.reconstructCommandLine(args);

        //log, input and output are compulsory
        inputFileName = (String) options.valueOf("i");
        outputFileName = (String) options.valueOf("output");
        logFileName = (String) options.valueOf("log");
        logLevel = (String) options.valueOf("loglevel");

        if (null == inputFileName) {
            displayHelp(mode);
            System.exit(0);
        }

        //
        List<String> dbList = (List<String>) options.valuesOf("d");
        databaseFiles = dbList.toArray(new String[dbList.size()]);

        if (MODE.dbsnp == mode && dbList.isEmpty()) {
            displayHelp(mode);
            System.exit(0);
        }

        gap = (options.has("gap")) ? (int) options.valueOf("gap") : 1000;  //CADD default is 1000
        bufferSize = (options.has("buffer")) ? (Integer) options.valueOf("buffer") : 0; //TRF default is 0

        miunCutoff = ((Integer) options.valueOf("miunCutoff"));
        minCutoff = ((Integer) options.valueOf("minCutoff"));
        minPercentage = ((Float) options.valueOf("minPercentage"));
        nnsCount = ((Integer) options.valueOf("nnsCounts"));
        mrCount = ((Integer) options.valueOf("mrCounts"));
        controlCoverageCutoff = ((Integer) options.valueOf("controlCoverageCutoff"));
        controlCoverageCutoffForSomatic = ((Integer) options.valueOf("controlCoverageCutoffForSomatic"));
        testCoverageCutoff = ((Integer) options.valueOf("testCoverageCutoff"));
        mrPercentage = ((Float) options.valueOf("mrPercentage"));
        filtersToIgnore = (List<String>) options.valuesOf("filtersToIgnore");

        //vcf2maf
        outputDir = (options.has("outdir")) ? (String) options.valueOf("outdir") : null;
        center = (options.has("center")) ? (String) options.valueOf("center") : null;
        sequencer = (options.has("sequencer")) ? (String) options.valueOf("sequencer") : null;
        donorId = (options.has("donor")) ? (String) options.valueOf("donor") : null;

        //vcf2maf confidence
        testSample = (options.has(test)) ? (String) options.valueOf(test) : null;
        controlSample = (options.has(control)) ? (String) options.valueOf(control) : null;

        //snpeff
        configFileName = (options.has("config")) ? (String) options.valueOf("config") :
                (MODE.snpeff == mode ? new File(databaseFiles[0]).getParent() + "/snpEff.config" : null);
        summaryFileName = (options.has("summaryFile")) ? (String) options.valueOf("summaryFile") :
                (mode == MODE.snpeff ? outputFileName + ".snpEff_summary.html" : null);

        //homoplymers
        homopolymerWindow = (Integer) options.valueOf("homWindow");
        homopolymerCutoff = (Integer) options.valueOf("homCutoff");
        homopolymerReportWindow = (Integer) options.valueOf("homReportWindow");

        checkIO();    //not yet complete         	
    }

    public OptionSet parseArgs(final String[] args) {
        parser.allowsUnrecognizedOptions();
        parser.acceptsAll(asList("v", "version"), VERSION_DESCRIPTION);
        parser.accepts("mode", Messages.getMessage("MODE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("mode");
        parser.acceptsAll(asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll(asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll(asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf");
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("nnsCounts", "Number of novel starts (NNS) required to be High Confidence").withRequiredArg().ofType(Integer.class)
                .describedAs("numberOfNovelStarts");
        parser.accepts("mrCounts", "Number of mutant reads (MR) required to be High Confidence").withRequiredArg().ofType(Integer.class)
                .describedAs("numberOfMutantReads");
        parser.accepts("miunCutoff", "Number of failed filter reads (FF) containing the alt, that will result in a MIUN (Mutation In Unfiltered Normal) in the filter field").withRequiredArg().ofType(Integer.class)
                .describedAs("miunCutoff");
        parser.accepts("minCutoff", "Minimum number of reads containing the alt, that will result in a MIN (Mutation In Normal) in the filter field. Used in conjunction with minPercentage").withRequiredArg().ofType(Integer.class)
                .describedAs("minCutoff");
        parser.accepts("minPercentage", "Percentage of reads containing the alt, that will result in a MIN (Mutation In Normal) in the filter field. Used in conjunction with minCutoff").withRequiredArg().ofType(Float.class)
                .describedAs("minPercentage");
        parser.accepts("controlCoverageCutoff", "Minimum coverage (DP format field) value to gain a PASS for control samples").withRequiredArg().ofType(Integer.class)
                .describedAs("controlCoverageCutoff");
        parser.accepts("controlCoverageCutoffForSomatic", "Minimum coverage (DP format field) value to gain a PASS for control samples when the call is a SOMATIC one").withRequiredArg().ofType(Integer.class)
                .describedAs("controlCoverageCutoffForSomatic");
        parser.accepts("testCoverageCutoff", "Minimum coverage (DP format field) value to gain a PASS for test samples").withRequiredArg().ofType(Integer.class)
                .describedAs("testCoverageCutoff");
        parser.accepts("mrPercentage", "Number of mutant reads (MR) required to be High Confidence as a percentage").withRequiredArg().ofType(Float.class)
                .describedAs("numberOfMutantReadsPercentage");
        parser.accepts("homWindow", "check for homoplymers inside window size on both sides of variants. Default value is " + HomopolymersMode.DEFAULT_WINDOW).withRequiredArg().ofType(Integer.class).describedAs("homopolymer window size");
        parser.accepts("homReportWindow", "Cutoff value for number of homoplymer bases within the window. Default value is " + HomopolymersMode.HOMOPOLYMER_CUTOFF).withRequiredArg().ofType(Integer.class).describedAs("homopolymer cutoff value");
        parser.accepts("homCutoff", "Cutoff value for number of homoplymer bases within the window. Default value is " + IndelConfidenceMode.DEFAULT_HOMN).withRequiredArg().ofType(Integer.class).describedAs("homopolymer cutoff value");

        OptionSet options = parser.parse(args);
        if (options.has("v") || options.has("version")) {
            System.err.println("qannotate: Current version is " + getVersion());
            System.exit(0);
        }

        Options.MODE mm = null;
        if (options.has("mode")) {
            final String m = ((String) options.valueOf("mode")).toLowerCase();
            try {
                mm = MODE.valueOf(m);
            } catch (IllegalArgumentException | NullPointerException e) {
                System.err.println("invalid mode specified: " + m);
                System.exit(1);
            }
        }

        if (mm == null) {
            /*
             * used by nanno.Annotate
             */
            parser.accepts("config", Messages.getMessage("NANNO_CONF_FILE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("config file");
        } else {

            if (mm.equals(MODE.confidence) || mm.equals(MODE.vcf2maf)) {
                parser.accepts(test, Messages.getMessage("TUMOUR_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("testSample");
                parser.accepts(control, Messages.getMessage("NORMAL_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("controlSample");
            } else {
                parser.acceptsAll(asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file");
            }

            if (mm.equals(MODE.snpeff)) {
                parser.accepts("config", Messages.getMessage("CONF_FILE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("config file");
                parser.accepts("summaryFile", Messages.getMessage("SUMMARY_FILE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("stat output");
            }

            if (mm.equals(MODE.trf))
                parser.accepts("buffer", "check TRF region on both sides of indel within this nominated size").withRequiredArg().ofType(Integer.class);//.describedAs("integer");

            if (mm.equals(MODE.cadd))
                parser.accepts("gap", "adjacent variants size").withRequiredArg().ofType(String.class).describedAs("gap size");

            if (mm.equals(MODE.vcf2maf)) {
                parser.accepts("outdir", Messages.getMessage("MAF_OUTPUT_DIRECTORY_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output file location");
                parser.accepts("donor", Messages.getMessage("DONOR_ID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("donor id");
                parser.accepts("center", "Genome sequencing center").withRequiredArg().ofType(String.class).describedAs("center");
                parser.accepts("sequencer", Messages.getMessage("SEQUENCER_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("Sequencer");
            }
        }

        return parser.parse(args);
    }

    public String getVersion() {
        return Options.class.getPackage().getImplementationVersion();
    }

    public String getPGName() {
        return Messages.getProgramName();
    }

    /**
     * check input and output files
     * @return true if input file readable and output file writable
     * @throws IOException
     */
    protected void checkIO() throws IOException {
        List<File> inputs = new ArrayList<>();
        List<File> outputs = new ArrayList<>();

        inputs.add(new File(inputFileName));
        if (databaseFiles != null) {
            for (String name : databaseFiles) {
                inputs.add(new File(name));
            }
        }
        if (getConfigFileName() != null) {
            /*
             * don't add config file if its already in the inputs list
             */
            if (!inputs.contains(new File(getConfigFileName()))) {
                inputs.add(new File(getConfigFileName()));
            }
        }

        if (outputFileName != null) {
            outputs.add(new File(outputFileName));
        }
        if (getSummaryFileName() != null) {
            outputs.add(new File(getSummaryFileName()));
        }

        //check output
        for (File out : outputs) {
            //out.getParentFile() maybe null if file name string exclude path eg. out = "ok.txt"
            File parent = out.getAbsoluteFile().getParentFile();
            if ((out.exists() && !out.canWrite()) || (!out.exists() && !parent.canWrite())) {
                throw new IllegalArgumentException(Messages.getMessage("OUTPUT_ERR_DESCRIPTION", out.getName()));
            }
        }
        //check inputs
        for (File fin : inputs) {
            if (!fin.exists()) {
                throw new IllegalArgumentException(Messages.getMessage("NONEXIST_INPUT_FILE", fin.getPath()));
            } else if (!fin.canRead()) {
                throw new IllegalArgumentException(Messages.getMessage("UNREAD_INPUT_FILE", fin.getPath()));
            }
        }
        //check whether file unique
        inputs.addAll(outputs);
        for (int i = inputs.size() - 1; i > 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (inputs.get(i).getCanonicalFile().equals(inputs.get(j).getCanonicalFile())) {
                    throw new IllegalArgumentException("below command line values are point to same file: \n\t" + inputs.get(i) + "\n\t" + inputs.get(j));
                }
            }
        }
    }

    public String getLogFileName() {
        return logFileName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public String getDatabaseFileName() {
        return null != databaseFiles && databaseFiles.length > 0 ? databaseFiles[0] : null;
    }

    public String[] getDatabaseFiles() {
        return (mode.equals(MODE.cadd) || mode.equals(MODE.overlap)) ? databaseFiles : null;
    }

    public MODE getMode() {
        return mode;
    }

    private void displayHelp(MODE mode) throws IOException {
        String mess = Messages.getMessage("USAGE");
        if (mode != null) {
            switch (mode) {
                case dbsnp:
                    mess = Messages.getMessage("DBSNP_USAGE");
                    break;
                case germline:
                    mess = Messages.getMessage("GERMLINE_USAGE");
                    break;
                case trf:
                    mess = Messages.getMessage("TRF_USAGE");
                    break;
                case snpeff:
                    mess = Messages.getMessage("SNPEFF_USAGE");
                    break;
                case confidence:
                    mess = Messages.getMessage("CONFIDENCE_USAGE");
                    break;
                case cadd:
                    mess = Messages.getMessage("CADD_USAGE");
                    break;
                case indelconfidence:
                    mess = Messages.getMessage("INDELCONFIDENCE_USAGE");
                    break;
                case vcf2maf:
                    mess = Messages.getMessage("VCF2MAF_USAGE");
                    break;
                case hom:
                    mess = Messages.getMessage("HOM_USAGE");
                    break;
                case make_valid:
                    mess = Messages.getMessage("MAKE_VALID_USAGE");
                    break;
                case overlap:
                    mess = Messages.getMessage("OVERLAP_USAGE");
                    break;
                default:
                    break;
            }
        } else {
            /*
             * mode is null, so could be missing, or could be nanno mode
             */
            mess = Messages.getMessage("USAGE");
            mess += "\nOR\n";
            mess += Messages.getMessage("NANNO_USAGE");
        }

        System.out.println(mess);
        parser.printHelpOn(System.err);
    }

    //vcf2maf confidence
    public String getTestSample() {
        return testSample;
    }

    public String getControlSample() {
        return controlSample;
    }

    //vcf2maf
    public String getCenter() {
        return (mode == (MODE.vcf2maf)) ? center : null;
    }

    public String getSequencer() {
        return (mode == (MODE.vcf2maf)) ? sequencer : null;
    }

    public String getOutputDir() {
        return (mode == (MODE.vcf2maf)) ? outputDir : null;
    }

    public String getDonorId() {
        return (mode == (MODE.vcf2maf)) ? donorId : null;
    }

    //snpEff
    public String getSummaryFileName() {
        return (mode == MODE.snpeff) ? summaryFileName : null;
    }

    public String getGenesFileName() {
        return (mode == MODE.snpeff) ? outputFileName + ".snpEff_genes.txt" : null;
    }

    public String getConfigFileName() {
        return (MODE.snpeff == mode || null == mode) ? configFileName : null;
    }

    public int getBufferSize() {
        return (mode == MODE.trf) ? bufferSize : -1;
    } //trf

    public int getGapSize() {
        return (mode == MODE.trf) ? gap : -1;
    } //cadd

    //hom
    public Optional<Integer> getHomoplymersWindow() {
        return Optional.ofNullable(homopolymerWindow);
    }

    public Optional<Integer> getHomoplymersCutoff() {
        return Optional.ofNullable(homopolymerCutoff);
    }
    public Optional<Integer> getHomoplymersReportWindow() {
        return Optional.ofNullable(homopolymerReportWindow);
    }

    public Optional<Integer> getNNSCount() {
        return Optional.ofNullable(nnsCount);
    }

    public Optional<Float> getMRPercentage() {
        return Optional.ofNullable(mrPercentage);
    }

    public Optional<Integer> getMRCount() {
        return Optional.ofNullable(mrCount);
    }

    public List<String> getFiltersToIgnore() {
        return filtersToIgnore;
    }

    public Optional<Integer> getControlCutoff() {
        return Optional.ofNullable(controlCoverageCutoff);
    }

    public Optional<Integer> getControlCutoffForSomatic() {
        return Optional.ofNullable(controlCoverageCutoffForSomatic);
    }

    public Optional<Integer> getTestCutoff() {
        return Optional.ofNullable(testCoverageCutoff);
    }

    public Optional<Integer> getMIUNCutoff() {
        return Optional.ofNullable(miunCutoff);
    }

    public Optional<Integer> getMINCutoff() {
        return Optional.ofNullable(minCutoff);
    }

    public Optional<Float> getMINPercentage() {
        return Optional.ofNullable(minPercentage);
    }

}
