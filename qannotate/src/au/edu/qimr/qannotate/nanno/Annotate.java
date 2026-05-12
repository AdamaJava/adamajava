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

    private static final Set<String> STANDARD_GRCH38_CONTIGS = Set.of(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "X", "Y", "MT", "M"
    );

    /** Number of variants processed per chunk. Controls peak memory: peak ≈ CHUNK_SIZE × N_sources × avg-annotation-bytes. */
    static final int CHUNK_SIZE = 100_000;

    private static final ChrPositionAnnotations POISON_PILL =
            new ChrPositionAnnotations(null, null, "", "", "");

    static Comparator<String[]> CUSTOM_COMPARATOR;
    static QLogger logger;

    private int exitStatus;
    private String inputFile;
    private String outputFile;
    private String jsonInputs;
    private QExec exec;

    record VariantWork(ChrPosition cp, long cpAsLong, String gatkAD, String gatkGT, String originalAlt) {}

    /**
     * Performs a single linear sweep through one annotation source against a
     * sorted chunk of variants. The annotation file cursor advances forward and
     * is never rewound, so repeated calls across chunks work correctly.
     */
    static class SourceSweeper implements Callable<String[]> {

        private final AnnotationSource source;
        private final List<VariantWork> variants;

        SourceSweeper(AnnotationSource source, List<VariantWork> variants) {
            this.source = source;
            this.variants = variants;
        }

        @Override
        public String[] call() {
            String[] results = new String[variants.size()];
            for (int i = 0; i < variants.size(); i++) {
                VariantWork v = variants.get(i);
                results[i] = source.getAnnotation(v.cpAsLong(), v.cp());
            }
            return results;
        }
    }

    public int engage() throws Exception {

        AnnotationInputs ais = AnnotateUtils.getInputs(jsonInputs);
        logger.info("Number of annotation sources: " + ais.getInputs().size());

        CUSTOM_COMPARATOR = AnnotateUtils.createComparatorFromList(
                Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.toList()));
        logger.info("Custom comparator created");

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

        // One thread per source, reused across all chunks.
        ExecutorService sourceExecutor = Executors.newFixedThreadPool(annotationSources.size());

        // Consumer (writer) thread — started once up front so writing overlaps sweeping.
        BlockingQueue<ChrPositionAnnotations> queue = new LinkedBlockingQueue<>(50_000);
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.execute(new Consumer(queue, outputFile, ais, exec));

        ChrPosition lastCP = null;
        int totalVariants = 0;
        int nonStandardContigCount = 0;
        List<VariantWork> chunk = new ArrayList<>(CHUNK_SIZE);

        try (VcfFileReader reader = new VcfFileReader(inputFile)) {
            logger.info("Reading and processing VCF records in chunks of " + CHUNK_SIZE + "...");

            for (VcfRecord vcf : reader) {
                ChrPosition thisVcfsCP = vcf.getChrPositionRefAlt();

                if (!isStandardContig(thisVcfsCP)) {
                    nonStandardContigCount++;
                    continue;
                }

                int compare = null != lastCP
                        ? ((ChrPositionRefAlt) thisVcfsCP).compareTo((ChrPositionRefAlt) lastCP)
                        : 0;
                if (compare < 0) {
                    throw new IllegalArgumentException("Incorrect order of vcf records in input vcf file! this vcf: "
                            + thisVcfsCP.toIGVString() + ", last vcf: " + lastCP.toIGVString());
                }

                String alt = ((ChrPositionRefAlt) thisVcfsCP).getAlt();
                String gatkAD = VcfUtils.getFormatField(vcf.getFormatFields(), "AD", 0);
                String gatkGT = VcfUtils.getFormatField(vcf.getFormatFields(), "GT", 0);

                if (alt.contains(",")) {
                    logger.info("alt has comma: " + thisVcfsCP);
                    String[] altArray = alt.split(",");
                    List<VcfRecord> splitVcfs = new ArrayList<>();
                    for (String thisAlt : altArray) {
                        if (!thisAlt.equals("*")) {
                            splitVcfs.add(VcfUtils.cloneWithNewAlt(vcf, thisAlt));
                        }
                    }
                    if (splitVcfs.size() > 1) splitVcfs.sort(null);
                    for (VcfRecord splitVcf : splitVcfs) {
                        chunk.add(toVariantWork(splitVcf.getChrPositionRefAlt(), gatkAD, gatkGT, alt));
                    }
                } else {
                    chunk.add(toVariantWork(thisVcfsCP, gatkAD, gatkGT, alt));
                }

                lastCP = thisVcfsCP;

                if (chunk.size() >= CHUNK_SIZE) {
                    processChunk(chunk, annotationSources, sourceExecutor, queue);
                    totalVariants += chunk.size();
                    chunk = new ArrayList<>(CHUNK_SIZE);
                }
            }

            // Flush the final partial chunk.
            if (!chunk.isEmpty()) {
                processChunk(chunk, annotationSources, sourceExecutor, queue);
                totalVariants += chunk.size();
            }
        }

        logger.info("VCF processing complete: " + totalVariants + " variants, "
                + nonStandardContigCount + " non-standard contigs skipped");

        queue.put(POISON_PILL);
        sourceExecutor.shutdown();
        consumerExecutor.shutdown();
        consumerExecutor.awaitTermination(60, TimeUnit.MINUTES);
        sourceExecutor.awaitTermination(60, TimeUnit.MINUTES);
        logger.info("Done");
        return exitStatus;
    }

    /**
     * Sweeps all annotation sources in parallel over one chunk of variants,
     * assembles per-variant result arrays, and hands them to the writer queue.
     * <p>
     * Peak live memory while this method runs:
     *   chunk.size() × annotationSources.size() × (avg annotation string bytes)
     * which is independent of total VCF size.
     * <p>
     * The {@link AnnotationSource} instances are stateful iterators over their
     * files; they must not be shared between concurrent calls to this method.
     */
    private static void processChunk(List<VariantWork> chunk,
                                     List<AnnotationSource> annotationSources,
                                     ExecutorService sourceExecutor,
                                     BlockingQueue<ChrPositionAnnotations> queue) throws Exception {

        final int numSources = annotationSources.size();

        // Sweep all sources in parallel over this chunk.
        List<Future<String[]>> futures = new ArrayList<>(numSources);
        for (AnnotationSource source : annotationSources) {
            futures.add(sourceExecutor.submit(new SourceSweeper(source, chunk)));
        }

        // Collect results — String[numSources][chunk.size()].
        String[][] chunkResults = new String[numSources][];
        for (int s = 0; s < numSources; s++) {
            chunkResults[s] = futures.get(s).get();
        }

        // Assemble per-variant annotation arrays and enqueue for writing.
        // After this loop chunkResults goes out of scope and is eligible for GC.
        final int n = chunk.size();
        for (int i = 0; i < n; i++) {
            VariantWork v = chunk.get(i);
            String[] annotations = new String[numSources];
            for (int s = 0; s < numSources; s++) {
                annotations[s] = chunkResults[s][i];
            }
            queue.put(new ChrPositionAnnotations(v.cp(), annotations, v.gatkAD(), v.gatkGT(), v.originalAlt()));
        }
    }

    private static VariantWork toVariantWork(ChrPosition cp, String gatkAD, String gatkGT, String originalAlt) {
        String contig = cp.getChromosome().startsWith("chr") ? cp.getChromosome().substring(3) : cp.getChromosome();
        long cpAsLong = ChrPositionUtils.convertContigAndPositionToLong(contig, cp.getStartPosition());
        return new VariantWork(cp, cpAsLong, gatkAD, gatkGT, originalAlt);
    }

    private boolean isStandardContig(ChrPosition cp) {
        String contig = cp.getChromosome();
        return STANDARD_GRCH38_CONTIGS.contains(contig.startsWith("chr") ? contig.substring(3) : contig);
    }

    public static class ChrPositionAnnotations {

        final ChrPosition cp;
        /** One entry per annotation source; each entry is a tab-separated "key=value\tkey=value…" string. */
        final String[] annotations;
        final String gatkAD;
        final String gatkGT;
        final String originalAlt;

        public ChrPositionAnnotations(ChrPosition cp, String[] annotations, String gatkAD, String gatkGT, String originalAlt) {
            this.cp = cp;
            this.annotations = annotations;
            this.gatkAD = gatkAD;
            this.gatkGT = gatkGT;
            this.originalAlt = originalAlt;
        }

        public String[] getAnnotations() {
            return annotations;
        }

        public String toStringMinusAnnotations() {
            return ((ChrPositionRefAlt) cp).toTabSeperatedString() + "\t" + originalAlt + "\t" + gatkGT + "\t" + gatkAD;
        }
    }

    public static class Consumer implements Runnable {

        private final BlockingQueue<ChrPositionAnnotations> queue;
        private final boolean includeSearchTerm;
        private final RecordWriter<String> writer;
        private final String additionalEmptyValues;

        private final int fieldCount;
        // pre-allocated, reused output buffer — Consumer is single-threaded
        private final String[] outputValues;
        private final StringBuilder sb = new StringBuilder(512);

        // used only during first-record initialisation
        private final Map<String, Integer> fieldPositionMap;
        private boolean indexInitialised = false;
        // annotationToSlot[flatIndex] = output slot (-1 = discard)
        // built once on the first record, never changes
        private int[] annotationToSlot;

        public Consumer(BlockingQueue<ChrPositionAnnotations> queue, String outputFile, AnnotationInputs ais, QExec exec) throws IOException {
            this.queue = queue;
            this.includeSearchTerm = ais.isIncludeSearchTerm();
            this.additionalEmptyValues = AnnotateUtils.generateAdditionalEmptyValues(ais);
            List<String> headers = AnnotateUtils.generateHeaders(ais, exec);
            this.writer = new RecordWriter<>(new File(outputFile));
            for (String h : headers) {
                writer.addHeader(h);
            }

            String[] fieldOrder = ais.getOutputFieldOrder().split(",");
            this.fieldCount = fieldOrder.length;
            this.outputValues = new String[fieldCount];
            Map<String, Integer> map = new HashMap<>(fieldCount * 2);
            for (int i = 0; i < fieldCount; i++) {
                map.put(fieldOrder[i], i);
            }
            this.fieldPositionMap = map;
        }

        @Override
        public void run() {
            logger.info("Consumer thread is a go!");
            try {
                while (true) {
                    final ChrPositionAnnotations rec = queue.take();
                    if (rec == POISON_PILL) {
                        break;
                    }
                    processRecord(rec);
                }
            } catch (final Exception e) {
                e.printStackTrace();
                logger.error("Exception caught in Consumer class", e);
            } finally {
                logger.info("Consumer: shutting down");
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void processRecord(final ChrPositionAnnotations recAndAnnotations) throws IOException {
            final String[] rawAnnotations = recAndAnnotations.getAnnotations();

            // ── First-record initialisation ──────────────────────────────────────────
            // Walk every tab-separated "key=value" token across all source strings and
            // build annotationToSlot[flatIndex] = output slot once, reused every record.
            if (!indexInitialised) {
                // Count total flat tokens to size the array.
                int totalTokens = 0;
                for (String annot : rawAnnotations) {
                    totalTokens++;
                    for (int k = 0; k < annot.length(); k++) {
                        if (annot.charAt(k) == '\t') totalTokens++;
                    }
                }
                annotationToSlot = new int[totalTokens];
                int idx = 0;
                for (String annot : rawAnnotations) {
                    int start = 0;
                    final int len = annot.length();
                    while (true) {
                        int tab = annot.indexOf('\t', start);
                        int end = tab < 0 ? len : tab;
                        int eq = annot.indexOf('=', start);
                        String key = (eq >= 0 && eq < end) ? annot.substring(start, eq) : annot.substring(start, end);
                        Integer slot = fieldPositionMap.get(key);
                        annotationToSlot[idx++] = slot != null ? slot : -1;
                        if (tab < 0) break;
                        start = tab + 1;
                    }
                }
                indexInitialised = true;
            }

            // ── Hot path — no List/stream allocation ─────────────────────────────────
            Arrays.fill(outputValues, "");
            int flatIdx = 0;
            for (String annot : rawAnnotations) {
                int start = 0;
                final int len = annot.length();
                while (true) {
                    int tab = annot.indexOf('\t', start);
                    int end = tab < 0 ? len : tab;
                    int slot = annotationToSlot[flatIdx++];
                    if (slot >= 0) {
                        int eq = annot.indexOf('=', start);
                        outputValues[slot] = (eq >= 0 && eq < end) ? annot.substring(eq + 1, end) : "";
                    }
                    if (tab < 0) break;
                    start = tab + 1;
                }
            }

            String searchTerm = "";
            if (includeSearchTerm) {
                String hgvsC = getFromOutputValues("hgvs.c");
                String hgvsP = getFromOutputValues("hgvs.p");
                searchTerm = AnnotateUtils.getSearchTerm(
                        hgvsC.isEmpty() ? null : hgvsC,
                        hgvsP.isEmpty() ? null : hgvsP);
            }

            sb.setLength(0);
            sb.append(recAndAnnotations.toStringMinusAnnotations()).append('\t');
            for (int i = 0; i < fieldCount; i++) {
                if (i > 0) sb.append('\t');
                sb.append(outputValues[i]);
            }
            sb.append(additionalEmptyValues);
            if (includeSearchTerm) sb.append('\t').append(searchTerm);

            writer.add(sb.toString());
        }

        private String getFromOutputValues(String fieldName) {
            Integer slot = fieldPositionMap.get(fieldName);
            return (slot != null && outputValues[slot] != null) ? outputValues[slot] : "";
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

    protected int setup(String[] args) throws Exception {
        int returnStatus = 1;
        if (null == args || args.length == 0) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
            System.exit(1);
        }
        final Options options = new Options(args);

        if (null == options.getInputFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getOutputFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getLogFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else if (null == options.getConfigFileName()) {
            System.err.println(Messages.getMessage("NANNO_USAGE"));
        } else {
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

