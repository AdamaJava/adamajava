/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qannotate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

import au.edu.qimr.qannotate.modes.CCMMode;
import au.edu.qimr.qannotate.modes.CaddMode;
import au.edu.qimr.qannotate.modes.ConfidenceMode;
import au.edu.qimr.qannotate.modes.DbsnpMode;
import au.edu.qimr.qannotate.modes.GermlineMode;
import au.edu.qimr.qannotate.modes.HomopolymersMode;
import au.edu.qimr.qannotate.modes.IndelConfidenceMode;
import au.edu.qimr.qannotate.modes.MakeValidMode;
import au.edu.qimr.qannotate.modes.OverlapMode;
import au.edu.qimr.qannotate.modes.SnpEffMode;
import au.edu.qimr.qannotate.modes.TandemRepeatMode;
import au.edu.qimr.qannotate.modes.Vcf2maf;

public class Main {

    private static QLogger logger;

    public static void main(final String[] args) {

        try {
            final Options options = new Options(args);
            logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(), options.getLogLevel());
            logger.logInitialExecutionStats(options.getPGName(), options.getVersion(), args);

            List<Options.MODE> modes = options.getModes();
            if (modes.isEmpty()) {
                throw new IllegalArgumentException("No mode was specified on the commandline - please add the \"-mode\" option");
            }
            if (modes.size() > 1 && (modes.contains(Options.MODE.vcf2maf) || modes.contains(Options.MODE.vcf2maftmp))) {
                throw new IllegalArgumentException("Multiple mode runs do not support vcf2maf/vcf2maftmp");
            }

            if (modes.size() == 1) {
                checkOptions(options);
                runMode(options);
            } else {
                runMultipleModes(args, modes, options.getInputFileName(), options.getOutputFileName());
            }

            logger.logFinalExecutionStats(0);

        } catch (Exception e) {
            System.out.println("Exception caught!");
            e.printStackTrace();
            System.err.println(Thread.currentThread().getName() + " " + e + " : " + e.getLocalizedMessage());
            if (null != logger) {
                logger.info(Thread.currentThread().getName() + " " + e + " : " + e.getMessage());
                logger.logFinalExecutionStats(1);
            }
            System.out.println("About to return exit code of 1");
            System.exit(1);
        }
    }

    static void runMultipleModes(String[] args, List<Options.MODE> modes, String inputFile, String outputFile) throws Exception {
        String currentInput = inputFile;
        List<File> tempFiles = new ArrayList<>();
        for (int i = 0 ; i < modes.size() ; i++) {
            boolean finalMode = i == modes.size() - 1;
            String currentOutput;
            if (finalMode) {
                currentOutput = outputFile;
            } else {
                File tmp = File.createTempFile("qannotate-mode-" + i + "-", ".vcf");
                tmp.deleteOnExit();
                tempFiles.add(tmp);
                currentOutput = tmp.getAbsolutePath();
            }
            Options modeOptions = new Options(rewriteArgsForMode(args, modes.get(i), currentInput, currentOutput));
            checkOptions(modeOptions);
            runMode(modeOptions);
            currentInput = currentOutput;
        }
        for (File temp : tempFiles) {
            if (temp.exists() && ! temp.delete()) {
                temp.deleteOnExit();
            }
        }
    }

    static String[] rewriteArgsForMode(String[] args, Options.MODE mode, String inputFile, String outputFile) {
        List<String> newArgs = new ArrayList<>();
        for (int i = 0 ; i < args.length ; i++) {
            String arg = args[i];
            if (isOptionWithValue(arg, "mode", "input", "i", "o", "output")) {
                i++;
                continue;
            }
            if (arg.startsWith("--mode=") || arg.startsWith("--input=") || arg.startsWith("-i=")
                    || arg.startsWith("--output=") || arg.startsWith("-o=") || arg.startsWith("-output=")) {
                continue;
            }
            newArgs.add(arg);
        }
        newArgs.add("--mode");
        newArgs.add(mode.name());
        newArgs.add("-i");
        newArgs.add(inputFile);
        newArgs.add("-o");
        newArgs.add(outputFile);
        return newArgs.toArray(new String[0]);
    }

    private static boolean isOptionWithValue(String arg, String... options) {
        for (String option : options) {
            if (arg.equals("-" + option) || arg.equals("--" + option)) {
                return true;
            }
        }
        return false;
    }

    private static void runMode(Options options) throws Exception {
        if (options.getMode() == Options.MODE.dbsnp) {
            new DbsnpMode(options);
        } else if (options.getMode() == Options.MODE.germline) {
            new GermlineMode(options);
        } else if (options.getMode() == Options.MODE.snpeff) {
            new SnpEffMode(options);
        } else if (options.getMode() == Options.MODE.confidence) {
            new ConfidenceMode(options);
        } else if (options.getMode() == Options.MODE.ccm) {
            new CCMMode(options);
        } else if (options.getMode() == Options.MODE.vcf2maf) {
            new Vcf2maf(options);
        } else if (options.getMode() == Options.MODE.cadd) {
            new CaddMode(options);
        } else if (options.getMode() == Options.MODE.indelconfidence) {
            new IndelConfidenceMode(options);
        } else if (options.getMode() == Options.MODE.hom) {
            new HomopolymersMode(options);
        } else if (options.getMode() == Options.MODE.trf) {
            new TandemRepeatMode(options);
        } else if (options.getMode() == Options.MODE.make_valid) {
            new MakeValidMode(options);
        } else if (options.getMode() == Options.MODE.overlap) {
            new OverlapMode(options);
        } else {
            throw new IllegalArgumentException("No valid mode are specified on commandline - please run \"qannotate -help\" to see the list of available modes");
        }
    }

    /**
     * Checks the Options object to see if the minimal options (input, output, database) have been supplied.
     * Using the switch statements fall through process here.
     *
     * Assuming that the individual modes will perform any more specific Options checking internally
     *
     * @param o
     */
    public static void checkOptions(Options o) {
        Options.MODE m = o.getMode();
        switch (m) {
            /*
             * some modes need a database
             */
            case dbsnp:
            case germline:
            case hom:
            case cadd:
            case snpeff:
            case trf:
                if (null == o.getDatabaseFileName()) {
                    throw new IllegalArgumentException("Please supply a reference file using the \"-d\" option");
                }

                /*
                 * most modes need an output and input
                 * there is a break after these checks as the vcf2mafs are a slightly special case
                 */
            case overlap:
            case confidence:
            case indelconfidence:
            case ccm:
            case make_valid:
                if (null == o.getOutputFileName()) {
                    throw new IllegalArgumentException("Please supply an output file using the \"-output\" or \"-o\" option");
                }
                if (null == o.getInputFileName()) {
                    throw new IllegalArgumentException("Please supply an input file using the \"-input\" option");
                }
                break;
            /*
             * apart from the vcf2maf modes which can take either a outdir or output option
             */
            case vcf2maf:
            case vcf2maftmp:
                if (null == o.getInputFileName()) {
                    throw new IllegalArgumentException("Please supply an input file using the \"-input\" option");
                }
                if (null == o.getOutputDir() && null == o.getOutputFileName()) {
                    throw new IllegalArgumentException("Please supply an output dir using the \"-outdir\" option, or an output file using the \"-output\" or \"-o\" option");
                }
        }
    }
}
