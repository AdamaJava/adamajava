package au.edu.qimr.qannotate.nanno;

import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.Options;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.qio.vcf.VcfFileReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Annotate {

    static final List<String> SEARCH_TERM_VARIETIES = Arrays.asList(">", "->", "-->", "/");

    // Define the set of standard GRCh38 contigs (no "chr" prefix version)
    private static final Set<String> STANDARD_GRCH38_CONTIGS = Set.of(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "X", "Y", "MT", "M"
    );

    static Comparator<String[]> CUSTOM_COMPARATOR;
    static QLogger logger;

    private int exitStatus;

    private String inputFile;
    private String outputFile;
    private String jsonInputs;

    private QExec exec;

    public int engage() throws Exception {

        /*
         * parse the json file into an AnnotationInputs object
         */
        AnnotationInputs ais = AnnotateUtils.getInputs(jsonInputs);
        logger.info("Number of annotation source threads to use: " + ais.getAnnotationSourceThreadCount());
        /*
         * create a comparator that will be used to sort the annotation fields for output
         */
        CUSTOM_COMPARATOR = AnnotateUtils.createComparatorFromList(Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.toList()));
        logger.info("Custom comparator created");
        /*
         * check headers that have been supplied in the json inputs file
         */
        int headersOK = AnnotateUtils.checkHeaders(ais);
        if (headersOK == 1) {
            logger.error("Headers have been checked - not OK!!!");
            System.exit(headersOK);
        }
        logger.info("Headers have been checked - OK");

        List<AnnotationSource> annotationSources = new ArrayList<>();
        AnnotateUtils.populateAnnotationSources(ais, annotationSources);
        logger.info("annotationSources have been loaded (size: " + annotationSources.size() + ")");
        annotationSources.forEach(as -> logger.info(as.toString()));

        CountDownLatch consumerLatch = new CountDownLatch(1);
        Queue<ChrPositionAnnotations> queue = new ConcurrentLinkedQueue<>();


        ExecutorService executor = Executors.newFixedThreadPool(Math.max(ais.getAnnotationSourceThreadCount(), 1) + 1);    // need an extra thread for the consumer, and at least 1 other thread
        executor.execute(new Consumer(queue, outputFile, consumerLatch, ais, exec));
        logger.info("ExecutorService has been setup");

        ChrPosition lastCP = null;
        try (
            VcfFileReader reader = new VcfFileReader(inputFile)) {
            logger.info("VcfFileReader has been setup");
            int vcfCount = 0;
            int nonStandardContigCount = 0;
            for (VcfRecord vcf : reader) {
                vcfCount++;

                ChrPosition thisVcfsCP = vcf.getChrPositionRefAlt();
                logger.debug("thisVcfsCP: " + thisVcfsCP.toIGVString());

                boolean isStandardContig = isStandardContig(thisVcfsCP);
                if (isStandardContig) {


                    /*
                     * check that this CP is "after" the last CP
                     */
                    int compare = null != lastCP ? ((ChrPositionRefAlt) thisVcfsCP).compareTo((ChrPositionRefAlt) lastCP) : 0;
                    if (compare < 0) {
                        throw new IllegalArgumentException("Incorrect order of vcf records in input vcf file! this vcf: " + thisVcfsCP.toIGVString() + ", last vcf: " + lastCP.toIGVString());
                    }


                    String alt = ((ChrPositionRefAlt) thisVcfsCP).getAlt();
                    String gatkAD = VcfUtils.getFormatField(vcf.getFormatFields(), "AD", 0);
                    String gatkGT = VcfUtils.getFormatField(vcf.getFormatFields(), "GT", 0);

                    if (alt.contains(",")) {
                        logger.info("alt has comma: " + thisVcfsCP);
                        /*
                         * split record, create new ChrPositions for each
                         */
                        String[] altArray = alt.split(",");
                        List<VcfRecord> splitVcfs = new ArrayList<>();
                        for (String thisAlt : altArray) {
                            if (thisAlt.equals("*")) {
                                /*
                                 * ignore
                                 */
                            } else {
                                VcfRecord newVcf = VcfUtils.cloneWithNewAlt(vcf, thisAlt);
                                splitVcfs.add(newVcf);
                            }
                        }
                        if (splitVcfs.size() > 1) {
                            /*
                             * sort
                             */
                            splitVcfs.sort(null);
                        }
                        for (VcfRecord splitVcf : splitVcfs) {
                            List<String> annotations = new ArrayList<>(getAnnotationsForPosition(splitVcf.getChrPositionRefAlt(), annotationSources, executor));
                            queue.add(new ChrPositionAnnotations(splitVcf.getChrPositionRefAlt(), annotations, gatkAD, gatkGT, alt));
                        }

                    } else {

                        logger.debug("about to get annotations for: " + thisVcfsCP.toIGVString());
                        List<String> annotations = getAnnotationsForPosition(thisVcfsCP, annotationSources, executor);
                        logger.debug("got annotations for: " + thisVcfsCP.toIGVString() + " - adding to queue");
                        queue.add(new ChrPositionAnnotations(thisVcfsCP, annotations, gatkAD, gatkGT, alt));

                    }

                    lastCP = thisVcfsCP;
                } else {
                    nonStandardContigCount++;
                }
            }

            logger.info("# of vcf records: " + vcfCount + ", # of non-standard contigs: " + nonStandardContigCount);
        } finally {
            /*
             * count down the count down latch
             */
            consumerLatch.countDown();
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.MINUTES);
        logger.info("ExecutorService has been shutdown");
        return exitStatus;
    }


    private static List<String> getAnnotationsForPosition(ChrPosition cp, List<AnnotationSource> annotationSources, Executor executor) {
        long contigAndPosition = ((ChrPositionUtils.convertContigAndPositionToLong(cp.getChromosome().startsWith("chr") ? cp.getChromosome().substring(3) : cp.getChromosome(), cp.getStartPosition())));
        return annotationSources.stream()
                .map(source -> CompletableFuture.supplyAsync(() ->
                        source.getAnnotation(contigAndPosition, cp), executor))
                .map(CompletableFuture::join).collect(Collectors.toList());
    }

    public static class ChrPositionAnnotations {

        public List<String> getAnnotations() {
            return annotations;
        }

        ChrPosition cp;
        List<String> annotations;
        String gatkAD;
        String gatkGT;
        String originalAlt;

        public ChrPositionAnnotations(ChrPosition cp, List<String> annotations, String gatkAD, String gatkGT, String originalAlt) {
            super();
            this.cp = cp;
            this.annotations = annotations;
            this.gatkAD = gatkAD;
            this.gatkGT = gatkGT;
            this.originalAlt = originalAlt;
        }

        public String toStringMinusAnnotations() {
            return ((ChrPositionRefAlt) cp).toTabSeperatedString() + "\t" + originalAlt + "\t" + gatkGT + "\t" + gatkAD;
        }

    }

    public static class Consumer implements Runnable {

        private final Queue<ChrPositionAnnotations> queue;
        private final boolean includeSearchTerm;
        private final CountDownLatch latch;
        private final RecordWriter<String> writer;
        private final String additionalEmptyValues;

        public Consumer(Queue<ChrPositionAnnotations> queue, String outputFile, CountDownLatch latch, AnnotationInputs ais, QExec exec) throws IOException {
            this.queue = queue;
            this.latch = latch;
            includeSearchTerm = ais.isIncludeSearchTerm();
            additionalEmptyValues = AnnotateUtils.generateAdditionalEmptyValues(ais);
            List<String> headers = AnnotateUtils.generateHeaders(ais, exec);

            writer = new RecordWriter<>(new File(outputFile));
            for (String h : headers) {
                writer.addHeader(h);
            }
        }

        @Override
        public void run() {
            logger.info("Consumer thread is a go!");
            try {

                while (true) {

                    final ChrPositionAnnotations rec = queue.poll();
                    if (null != rec) {

                        processRecord(rec);

                    } else {
                        if (latch.getCount() == 0) {
                            break;
                        }
                        // sleep and try again
                        try {
                            Thread.sleep(20);
                        } catch (final InterruptedException e) {
                            logger.error("InterruptedException caught in Consumer sleep: " + e.getLocalizedMessage());
                            throw e;
                        }
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
                logger.error("Exception caught in Consumer class: " + e.getCause().getMessage());
            } finally {
                logger.info("Consumer: shutting down");
                /*
                 * close writer
                 */
                try {
                    writer.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void processRecord(final ChrPositionAnnotations recAndAnnotations) throws IOException {

            List<String> annotations = recAndAnnotations.getAnnotations();
            logger.debug("annotations.size(): " + annotations.size());

            /*
             * collect entries in annotations lists into map
             */
            List<String> singleAnnotations = AnnotateUtils.convertAnnotations(annotations);
            logger.debug("singleAnnotations.size(): " + singleAnnotations.size());


            String searchTerm = "";
            if (includeSearchTerm) {
                String hgvsC = AnnotateUtils.getAnnotationFromList(singleAnnotations, "hgvs.c").orElse(null);
                String hgvsP = AnnotateUtils.getAnnotationFromList(singleAnnotations, "hgvs.p").orElse(null);
                searchTerm = AnnotateUtils.getSearchTerm(hgvsC, hgvsP);
            }
            /*
             * sort and write out to file
             */
            String annotationString = singleAnnotations.stream().map(s -> s.split("=", 2)).sorted(CUSTOM_COMPARATOR).map(a -> a[1]).collect(Collectors.joining("\t"));

            logger.debug("annotationString: " + annotationString);

            writer.add(recAndAnnotations.toStringMinusAnnotations() + "\t" + annotationString + additionalEmptyValues + (includeSearchTerm ? "\t" + searchTerm : ""));
        }
    }

    public static void main(String[] args) {
        final Annotate sp = new Annotate();
        int exitStatus = 0;
        try {
            exitStatus = sp.setup(args);
        } catch (final Exception e) {
            exitStatus = 1;
            if (null != logger) {
                logger.error("Exception caught whilst running Annotate:", e);
            } else {
                System.err.println("Exception caught whilst running Annotate");
            }
            e.printStackTrace();
        }

        if (null != logger) {
            logger.logFinalExecutionStats(exitStatus);
        }
        System.exit(exitStatus);
    }

    protected int setup(String [] args) throws Exception {
        int returnStatus = 1;
        if (null == args || args.length == 0) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
            System.exit(1);
        }
        final Options options = new Options(args);

        System.out.println("options.getInputFileName: " + options.getInputFileName());
        System.out.println("options.getOutputFileName: " + options.getOutputFileName());
        System.out.println("options.getConfigFileName: " + options.getConfigFileName());
        if (null == options.getInputFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getOutputFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getLogFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getConfigFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else {
            // configure logging
            String logFile = options.getLogFileName();
            logger = QLoggerFactory.getLogger(Annotate.class, logFile, options.getLogLevel());
            exec = logger.logInitialExecutionStats("Annotate", Annotate.class.getPackage().getImplementationVersion(), args);
            outputFile = options.getOutputFileName();
            inputFile = options.getInputFileName();
            jsonInputs = options.getConfigFileName();

            return engage();
        }

        return returnStatus;
    }

}
