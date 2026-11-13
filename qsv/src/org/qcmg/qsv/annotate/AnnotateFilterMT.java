/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import htsjdk.samtools.*;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMWriterFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.Messages;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.softclip.SoftClipStaticMethods;
import org.qcmg.qsv.util.CustomThreadPoolExecutor;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.SamReaderCache;

import htsjdk.samtools.SAMFileHeader.SortOrder;

/**
 * Class to annotate discordant read pairs and filter
 * for high quality discordant pairs, clips and unmapped reads.
 * <p>
 * One instance per BAM holds that BAM's annotation state, output queues and writer threads.
 * Filtering is done by a pool shared across all BAMs passed to {@link #runAll(List)}, so that
 * once a smaller BAM (typically the control) has been filtered, its threads move on to the
 * remaining chromosomes of the larger one.
 */
public class AnnotateFilterMT implements Runnable {

    /** filter threads contributed to the shared pool for each BAM */
    static final int FILTER_THREADS_PER_BAM = 2;
    private static final int maxRecords = 500000;
    private static final int checkPoint = 100000;
    /**
     * The filtered BAMs are intermediate files that qsv re-reads and never publishes,
     * so favour write speed over file size.
     */
    static final int INTERMEDIATE_BAM_COMPRESSION_LEVEL = 1;

    private static final QLogger staticLogger = QLoggerFactory.getLogger(AnnotateFilterMT.class);

    private final QLogger logger = QLoggerFactory.getLogger(getClass());
    private final int sleepUnit = 10;
    private final File input;
    private final File output;
    private final String query;
    private final QSVParameters parameters;
    private final CountDownLatch mainLatch;
    private final AtomicLong badRecordCount;
    private final AtomicLong goodPairRecordCount = new AtomicLong();
    private final AtomicInteger exitStatus;
    private final File clippedFile;
    private final SAMFileHeader queryNameHeader;
    private final SAMFileHeader coordinateHeader;
    private final String softClipDir;
    private final String clipQuery;
    private final boolean runPair;
    private final boolean runClip;
    private final boolean isSplitRead;
    private final Collection<String> readGroupIds;
    private final AtomicLong goodClipCount = new AtomicLong();
    private final AtomicLong unmappedCount = new AtomicLong();
    private final boolean translocationsOnly;

    // queue to store the satisfied BAM records for discordant pairs
    private final AbstractQueue<SAMRecord> writeQueue = new ConcurrentLinkedQueue<>();
    // queue to store the satisfied BAM records for clips and unmapped reads
    private final AbstractQueue<SAMRecord> writeClipQueue = new ConcurrentLinkedQueue<>();
    // counted down by each writing thread as it exits
    private final CountDownLatch writeLatch = new CountDownLatch(2);
    // counted down once per chromosome group of this BAM that has been filtered (or abandoned);
    // writers finish once it reaches zero and their queue is drained
    private CountDownLatch filterLatch;
    private ExecutorService writeThreads;


    public AnnotateFilterMT(CountDownLatch countDownLatch, QSVParameters parameters, AtomicInteger exitStatus, String softclipDir, Options options) {
        this.softClipDir = softclipDir;
        this.query = options.getPairQuery();
        this.clipQuery = options.getClipQuery();
        logger.info("Mapper: " + parameters.getMapper());
        logger.info("Type of Annotation: " + parameters.getPairingType());
        logger.info("QBamFilter Query to use is: " + this.query);

        this.parameters = parameters;
        this.input = parameters.getInputBamFile();
        this.output = parameters.getFilteredBamFile();
        this.queryNameHeader = parameters.getHeader().clone();
        this.queryNameHeader.setSortOrder(SortOrder.queryname);
        this.coordinateHeader = parameters.getHeader().clone();
        this.coordinateHeader.setSortOrder(SortOrder.coordinate);
        this.readGroupIds = parameters.getReadGroupIds();
        this.translocationsOnly = options.getIncludeTranslocations();
        this.mainLatch = countDownLatch;
        this.badRecordCount = new AtomicLong();
        this.exitStatus = exitStatus;
        this.clippedFile = parameters.getClippedBamFile();
        this.isSplitRead = options.isSplitRead();

        switch (options.getPreprocessMode()) {
            case "both" -> {
                this.runClip = true;
                this.runPair = true;
            }
            case "clip" -> {
                this.runClip = true;
                this.runPair = false;
            }
            case "pair" -> {
                this.runClip = false;
                this.runPair = true;
            }
            default -> {
                this.runClip = false;
                this.runPair = false;
            }
        }
    }

    /**
     * Filters this BAM on its own.
     */
    @Override
    public void run() {
        runAll(List.of(this));
    }

    public static void runAll(List<AnnotateFilterMT> samples) {
        runAll(samples, null);
    }

    /**
     * Filters several BAMs using one shared pool of threads (threads in total, or {@link #FILTER_THREADS_PER_BAM} threads per BAM if null).
     * Each BAM keeps its own writers; threads take chromosome groups from whichever BAM still has work.
     * Errors are recorded in each instance's exit status.
     */
    public static void runAll(List<AnnotateFilterMT> samples, Integer threads) {
        final Thread mainThread = Thread.currentThread();
        final int noOfThreads = null != threads ? threads : FILTER_THREADS_PER_BAM * samples.size();
        final AbstractQueue<FilterTask> taskQueue = createTaskQueue(samples);
        staticLogger.info("Filtering " + samples.size() + " BAM(s) using a shared pool of " + noOfThreads + " threads; chromosome groups to process: " + taskQueue.size());

        ExecutorService filterThreads = null;
        try {
            for (AnnotateFilterMT sample : samples) {
                sample.startWriters(mainThread);
            }

            filterThreads = new CustomThreadPoolExecutor(noOfThreads, samples.getFirst().exitStatus, staticLogger);
            for (int i = 0; i < noOfThreads; i++) {
                filterThreads.execute(new SharedFilterWorker(taskQueue, mainThread));
            }
            filterThreads.shutdown();

            staticLogger.info("waiting for filter threads to finish (max wait will be 100 hours)");
            if ( ! filterThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
                staticLogger.error("Filtering threads did not finish within the time limit");
            }
            if ( ! taskQueue.isEmpty()) {
                for (AnnotateFilterMT sample : samples) {
                    sample.fail("filter threads have completed but " + taskQueue.size() + " chromosome groups were not processed");
                }
            }
        } catch (final Exception e) {
            staticLogger.error("Exception caught whilst filtering: " + QSVUtil.getStrackTrace(e));
            for (AnnotateFilterMT sample : samples) {
                sample.fail(e.getMessage());
            }
        } finally {
            if (null != filterThreads) {
                filterThreads.shutdownNow();
            }
            for (AnnotateFilterMT sample : samples) {
                sample.finish();
            }
        }
    }

    /**
     * @return the number of reference bases that will be read for these chromosomes (ranges)
     */
    public static long totalLength(List<Chromosome> chromosomes) {
        long length = 0;
        for (Chromosome c : chromosomes) {
            length += (long) c.getEndPosition() - c.getStartPosition() + 1;
        }
        return length;
    }

    /*
     * One task per chromosome group per BAM, BAMs interleaved and each longest first,
     * so that the larger groups (and the larger BAM's groups first) are started earliest.
     */
    private static AbstractQueue<FilterTask> createTaskQueue(List<AnnotateFilterMT> samples) {
        List<List<FilterTask>> perSample = new ArrayList<>(samples.size());
        int max = 0;
        for (AnnotateFilterMT sample : samples) {
            List<FilterTask> tasks = sample.parameters.getChromosomes()
                    .entrySet()
                    .stream()
                    /*
                     * longest first (then by name), so that the threads don't end up waiting on one long chromosome at the end.
                     * Each chromosome writes its own clip file, and the shared pair and clip BAMs are sorted, so the order doesn't change the output
                     */
                    .sorted(Comparator.comparingLong((Map.Entry<String, List<Chromosome>> e) -> totalLength(e.getValue())).reversed()
                            .thenComparing(Map.Entry::getKey, new ReferenceNameComparator()))
                    .map(e -> new FilterTask(sample, e.getValue()))
                    .toList();
            sample.filterLatch = new CountDownLatch(tasks.size());
            perSample.add(tasks);
            max = Math.max(max, tasks.size());
        }
        AbstractQueue<FilterTask> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < max; i++) {
            for (List<FilterTask> tasks : perSample) {
                if (i < tasks.size()) {
                    queue.add(tasks.get(i));
                }
            }
        }
        return queue;
    }

    private void startWriters(Thread mainThread) {
        writeThreads = new CustomThreadPoolExecutor(2, exitStatus, logger);
        //paired reads
        writeThreads.execute(new Writing(writeQueue, output, mainThread, filterLatch, writeLatch, queryNameHeader));
        //soft clips
        writeThreads.execute(new Writing(writeClipQueue, clippedFile, mainThread, filterLatch, writeLatch, coordinateHeader));
        writeThreads.shutdown();
    }

    private void fail(String message) {
        logger.error("Filtering failed for " + input.getAbsolutePath() + ": " + message);
        if (exitStatus.intValue() == 0) {
            exitStatus.incrementAndGet();
        }
    }

    /*
     * Wait for this BAM's writers and check the outcome. Always counts down the main latch.
     */
    private void finish() {
        try {
            if (null != filterLatch) {
                // groups that were never processed (e.g. the pool failed to start) would otherwise keep the writers waiting
                while (filterLatch.getCount() > 0) {
                    filterLatch.countDown();
                }
            }
            if (null != writeThreads) {
                if ( ! writeThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
                    logger.error("Writing threads did not finish within the time limit");
                }
            }

            if ( ! writeQueue.isEmpty() || ! writeClipQueue.isEmpty()) {
                throw new Exception(
                        " threads have completed but queue isn't empty  (writeQueue, writeClipQueue ):  "
                                + writeQueue.size() + "," + writeClipQueue.size());
            }
            logger.info("All threads finished for " + input.getAbsolutePath());

            if (exitStatus.intValue() == 0) {
                if (runPair && goodPairRecordCount.longValue() == 0) {
                    throw new QSVException("LOW_FILTER_COUNT", "discordant pair");
                }
                if (runClip && goodClipCount.longValue() == 0) {
                    throw new QSVException("LOW_FILTER_COUNT", "clip");
                }
            }
            logger.info("Finished filtering file: " + input.getAbsolutePath() + " .Discordant Pair records: " +
                    goodPairRecordCount.longValue() + "; Soft clip records: " + goodClipCount.longValue() + "; Unmapped records: " + unmappedCount.intValue());

        } catch (final InterruptedException e) {
            logger.error("Thread was interrupted while waiting for writer threads to terminate: " + QSVUtil.getStrackTrace(e));
            fail(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            logger.error(QSVUtil.getStrackTrace(e));
            fail(e.getMessage());
        } finally {
            if (null != writeThreads) {
                writeThreads.shutdownNow();
            }
            if (null != mainLatch) {
                mainLatch.countDown();
            }
        }
    }

    public AtomicLong getClipCount() {
        return goodClipCount;
    }

    private record FilterTask(AnnotateFilterMT sample, List<Chromosome> chromosomes) {}

    /**
     * Pool thread that takes chromosome groups from any BAM. Keeps one reader and one
     * {@link AnnotationFiltering} (with its query executors) per BAM for its lifetime.
     */
    private static final class SharedFilterWorker implements Runnable {
        private final AbstractQueue<FilterTask> queue;
        private final Thread mainThread;

        SharedFilterWorker(AbstractQueue<FilterTask> queue, Thread mainThread) {
            this.queue = queue;
            this.mainThread = mainThread;
        }

        @Override
        public void run() {
            Map<AnnotateFilterMT, AnnotationFiltering> filters = new HashMap<>(4);
            try (SamReaderCache readers = new SamReaderCache()) {
                FilterTask task;
                while ((task = queue.poll()) != null) {
                    AnnotateFilterMT sample = task.sample();
                    try {
                        // once anything has failed, drain the queue without processing so that writers can finish
                        if (sample.exitStatus.intValue() == 0) {
                            AnnotationFiltering filter = filters.get(sample);
                            if (null == filter) {
                                filter = sample.new AnnotationFiltering(mainThread);
                                filters.put(sample, filter);
                            }
                            if ( ! filter.process(readers.get(sample.input), task.chromosomes())) {
                                sample.fail("records could not be passed to the writer");
                            }
                        }
                    } catch (final Exception e) {
                        sample.logger.error("Setting exit status in annotation thread to 1 as exception caught: " + QSVUtil.getStrackTrace(e));
                        sample.fail(e.getMessage());
                    } finally {
                        sample.filterLatch.countDown();
                    }
                }
            }
            staticLogger.info("Completed filtering thread: " + Thread.currentThread().getName());
        }
    }

    /**
     * Annotates and filters discordant pairs, clips and unmapped reads for this BAM.
     * One instance per (thread, BAM): the query executors are not shared between threads.
     */
    class AnnotationFiltering {

        private final Thread mainThread;
        private int countOutputSleep;
        QueryExecutor pairQueryEx;
        QueryExecutor lifescopeQueryEx;
        QueryExecutor clipQueryEx;


        public AnnotationFiltering(Thread mainThread) throws Exception {
            this.mainThread = mainThread;
            if (runPair && !StringUtils.isNullOrEmpty(query)) {
                pairQueryEx = new QueryExecutor(query);
            }
            if (runClip && !StringUtils.isNullOrEmpty(clipQuery)) {
                clipQueryEx = new QueryExecutor(clipQuery);
            }
            if (parameters.getPairingType().equals("lmp") && parameters.getMapper().equals("bioscope")) {
                lifescopeQueryEx = new QueryExecutor("and(Cigar_M > 35, MD_mismatch < 3, MAPQ > 0, flag_DuplicateRead == false)");
            }
        }

        /**
         * Filters each chromosome in the group.
         * @return false if records could not be passed to the writers
         */
        boolean process(SamReader reader, List<Chromosome> chromosomes) throws Exception {
            for (final Chromosome chromosome : chromosomes) {
                logger.info("Reading records from chromosome: " + chromosome.getName() + " in sample " + parameters.getFindType());

                boolean result;
                if (runClip) {
                    //set up writer to write clips to txt file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(SoftClipStaticMethods.getSoftClipFile(chromosome.getName(), parameters.getFindType(), softClipDir), true))) {
                        result = readAndWriteData(reader, writer, chromosome, chromosome.getStartPosition(), chromosome.getEndPosition());
                    }
                } else {
                    result = readAndWriteData(reader, null, chromosome, chromosome.getStartPosition(), chromosome.getEndPosition());
                }
                if ( ! result) {
                    return false;
                }
            }
            return true;
        }

        private boolean readAndWriteData(SamReader reader, BufferedWriter writer, Chromosome chromosome, int startPos, int endPos) throws Exception {

            boolean result = true;

            int queryStart = startPos - 100;
            if (queryStart <= 0) {
                queryStart = 1;
            }
            final int queryEnd = endPos + 100;
            //get reads from bam; the reader is reused, so the iterator must be closed
            try (SAMRecordIterator iter = reader.queryOverlapping(chromosome.getName(), queryStart, queryEnd)) {

                int count = 0;
                int shortReadCount = 0;

                while (iter.hasNext()) {
                    final SAMRecord record = iter.next();
                    count++;
                    boolean writersResult = true;
                    boolean pileupResult = true;

                    /*
                     * only proceed with records that have sequence > QSVAssemble_SEED_LENGTH
                     */
                    if (record.getReadLength() > QSVAssemble.SEED_LENGTH) {

                        final SAMReadGroupRecord srgr = record.getReadGroup();
                        if (null != srgr
                                && null != srgr.getId()
                                && readGroupIds.contains(srgr.getId())) {

                            //discordant pairs
                            if (runPair && record.getAlignmentStart() >= startPos && record.getAlignmentStart() <= endPos) {
                                writersResult = addToPairWriter(record, srgr.getId(), count);
                            }

                            if (runClip) {
                                //soft clips
                                if ((!record.getReadUnmappedFlag() && !record.getDuplicateReadFlag() && record.getCigar().containsOperator(CigarOperator.SOFT_CLIP))
                                        || (isSplitRead && record.getReadUnmappedFlag())) {

                                    pileupResult = pileupSoftClips(writer, record, srgr.getId(), startPos, endPos, chromosome, count);
                                }
                            }

                            //check to make sure there aren't any errors
                            if (!pileupResult || !writersResult) {
                                if (!pileupResult) {
                                    logger.error("Error finding soft clip records in " + chromosome);
                                }
                                if (!writersResult) {
                                    logger.error("Error finding discordant read records in " + chromosome);
                                }

                                if (exitStatus.intValue() == 0) {
                                    exitStatus.incrementAndGet();
                                }
                                result = false;
                            }
                        } else {
                            logger.warn("SAMReadGroupRecord : " + srgr + ":" + record.getSAMString());
                            logger.warn("SAMReadGroupRecord was null, or id was not in collection: " + (null != srgr ? srgr.getId() : null) + ":" + record.getSAMString());
                            throw new Exception("Null SAMReadGroupRecord");
                        }
                    } else {
                        shortReadCount++;
                    }
                }
                if (shortReadCount > 0) {
                    logger.warn("Ignored " + shortReadCount + " reads that had read length < " + QSVAssemble.SEED_LENGTH);
                }
            }
            return result;
        }

        /*
         * write soft clips and unmapped reads
         */
        private boolean pileupSoftClips(BufferedWriter writer, SAMRecord record, String rgId, int start, int end, Chromosome chromosome, int count) throws Exception {
            if (record.getReadUnmappedFlag()) {
                unmappedCount.incrementAndGet();
                QSVUtil.writeUnmappedRecord(writer, record, rgId, start, end);
                return add2queue(record, writeClipQueue, count);
            }


            //see if clips pass the filter
            if (clipQueryEx.Execute(record)) {
                goodClipCount.incrementAndGet();
                SoftClipStaticMethods.writeSoftClipRecord(writer, record, rgId, start, end);
                return add2queue(record, writeClipQueue, count);
            } else {
                return true;
            }
        }

        /*
         * Write discordant pairs to bam
         */
        private boolean addToPairWriter(SAMRecord record, String rgId, int count) throws Exception {

            if (!record.getReadUnmappedFlag()) {

                if (!translocationsOnly || QSVUtil.isTranslocationPair(record)) {

                    //annotate the read
                    parameters.getAnnotator().annotate(record, rgId);
                    final String zp = (String) record.getAttribute(QSVConstants.ZP_SHORT);

                    //make sure it is discordant
                    if (zp.contains("A") || zp.equals("C**") || zp.contains("B")) {
                        if (!zp.equals(QSVConstants.AAA)) {
                            //check if it passes the filter
                            if (pairQueryEx.Execute(record)) {
                                goodPairRecordCount.incrementAndGet();
                                return add2queue(record, writeQueue, count, writeLatch);
                            } else {
                                Object xc = record.getAttribute("XC");
                                if (lifescopeQueryEx != null && record.getAttribute("SM") == null && xc != null) {
                                    //try to filter lifescope reads
                                    if (lifescopeQueryEx.Execute(record)) {
                                        goodPairRecordCount.incrementAndGet();
                                        return add2queue(record, writeQueue, count, writeLatch);
                                    } else {
                                        record.setAttribute(QSVConstants.ZP, xc);
                                        if (lifescopeQueryEx.Execute(record)) {
                                            return add2queue(record, writeQueue, count, writeLatch);
                                        }
                                    }
                                    badRecordCount.incrementAndGet();
                                } else {
                                    badRecordCount.incrementAndGet();
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }

        private boolean add2queue(SAMRecord re, AbstractQueue<SAMRecord> queue, int count) {
            return add2queue(re, queue, count, null);
        }

        //add to write queue
        private boolean add2queue(SAMRecord re, AbstractQueue<SAMRecord> queue, int count, CountDownLatch latch) {
            // check queue size in each checkPoint number
            if (count % checkPoint == 0) {
                // check mainThread
                if (!mainThread.isAlive()) {
                    logger.error("mainThread died: " + mainThread.getName());
                    return false;
                }
                // check queue size
                while (queue.size() >= maxRecords) {
                    try {
                        Thread.sleep(sleepUnit);
                        countOutputSleep++;
                    } catch (final InterruptedException e) {
                        logger.info(Thread.currentThread().getName() + " "
                                + QSVUtil.getStrackTrace(e) + " (queue size full) ");
                    }
                    if (null != latch && latch.getCount() == 0) {
                        logger.error("output queue is not empty but writing thread are completed");
                        return false;
                    }
                } // end while
            }

            return queue.add(re);
        }
    }

    /**
     * Class to write discordant pairs, clips and unmapped reads to file
     */
    private class Writing implements Runnable {
        private final File file;
        private final AbstractQueue<SAMRecord> queue;
        private final Thread mainThread;
        private final CountDownLatch filterLatch;
        private final CountDownLatch writeLatch;
        private final SAMFileHeader header;

        public Writing(AbstractQueue<SAMRecord> q, File f, Thread mainThread,
                       CountDownLatch fLatch, CountDownLatch wLatch, SAMFileHeader header) {
            queue = q;
            file = f;
            this.mainThread = mainThread;
            this.filterLatch = fLatch;
            this.writeLatch = wLatch;
            this.header = header;
        }

        @Override
        public void run() {
            int countSleep = 0;
            boolean run = true;
            logger.info("Started Writing thread with output file: " + file);
            try {
                int count = 0;
                if (file != null) {
                    HeaderUtils.addProgramRecord(header, "qsv", Messages.getVersionMessage(), query);

                    /*
                    * try using the qpicard writer to see if that will handle CRAM files
                     */
                    SAMFileWriter writer = null;
                    try {
                        SAMWriterFactory factory = new SAMWriterFactory(header, false, file, file.getParentFile(), maxRecords, true, false, -1,
                                new File(parameters.getReference()), INTERMEDIATE_BAM_COMPRESSION_LEVEL);
                        writer = factory.getWriter();
                        logger.info("all tmp BAMs are located on " + file.getParentFile().getCanonicalPath());
                        logger.info("default maxRecordsInRam " + SAMFileWriterImpl.getDefaultMaxRecordsInRam() + ", compression level " + INTERMEDIATE_BAM_COMPRESSION_LEVEL);
                        SAMRecord record;
                        while (run) {
                            // when queue is empty,maybe filtering is done
                            if ((record = queue.poll()) == null) {

                                try {
                                    Thread.sleep(sleepUnit);
                                    countSleep++;
                                } catch (final Exception e) {
                                    logger.info(Thread.currentThread().getName() + " "
                                            + QSVUtil.getStrackTrace(e));
                                }

                                if ((count % checkPoint == 0) && (!mainThread.isAlive())) {
                                    throw new Exception("Writing thread failed since parent thread died.");
                                }

                                if (filterLatch.getCount() == 0) {
                                    /*
                                     * the queue could have been added to since our last check (we did have a quick sleep after all
                                     * Check size again before pulling the plug
                                     */
                                    if (queue.isEmpty()) {
                                        run = false;
                                    }
                                }

                            } else {

                                writer.addAlignment(record);
                                count++;

                                if (count % 1000000 == 0) {
                                    logger.info("have written " + count + " SAMRecords to file, queue size: " + queue.size());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception caught whilst writing SAMRecords to bam file: " + file.getAbsolutePath() + " : " + QSVUtil.getStrackTrace(e));
                        throw e;
                    } finally {
                        if (writer != null) {
                            logger.info("About to close writer for file: " + file.getAbsolutePath());
                            writer.close();
                        }

                    }
                }
                if (!mainThread.isAlive()) {
                    throw new Exception(
                            "Writing thread failed since parent thread died.");
                } else {
                    if (file != null) {
                        logger.info("Completed writing thread, added " + count
                                + " records to the output: "
                                + file.getAbsolutePath());
                        logger.info("Record count not passing filter for the "
                                + parameters.getFindType() + " sample is "
                                + badRecordCount.longValue());
                    }
                }
            } catch (final Exception e) {
                logger.error("Setting exit status to 1 as exception caught in writing thread: " + QSVUtil.getStrackTrace(e));
                if (exitStatus.intValue() == 0) {
                    exitStatus.incrementAndGet();
                }
                mainThread.interrupt();
            } finally {
                writeLatch.countDown();
                logger.info("Exit Writing thread, total " + countSleep
                        + " times get null from writing queue.");
            }
        }
    }
}
