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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import htsjdk.samtools.*;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
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

import htsjdk.samtools.SAMFileHeader.SortOrder;

/**
 * Class to annotate discordant read pairs and filter
 * for high quality discordant pairs, clips and unmapped reads
 */
public class AnnotateFilterMT implements Runnable {

    private static final int noOfThreads = 2;
    private static final int maxRecords = 500000;
    private static final int checkPoint = 100000;

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


    public AnnotateFilterMT(Thread mainThread, CountDownLatch countDownLatch, QSVParameters parameters, AtomicInteger exitStatus, String softclipDir, Options options) {
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
     * Class entry point. Sets up read, write and annotating/filtering threads
     */
    @Override
    public void run() {

        // create sorted queue to get the chromosomes to reads
        final AbstractQueue<List<Chromosome>> readQueue = parameters.getChromosomes()
                .entrySet()
                .stream()
                .sorted((e1, e2) -> new ReferenceNameComparator().compare(e1.getKey(), e2.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        logger.info("readQueue setup with " + readQueue.size() + " lists of chromosomes");

        // create queue to store the satisfied BAM records for discordant pairs
        final AbstractQueue<SAMRecord> writeQueue = new ConcurrentLinkedQueue<>();

        // create queue to store the satisfied BAM records for clips and unmapped reads
        final AbstractQueue<SAMRecord> writeClipQueue = new ConcurrentLinkedQueue<>();

        final CountDownLatch filterLatch = new CountDownLatch(noOfThreads); // annotating filtering threads
        final CountDownLatch writeLatch = new CountDownLatch(2); // writing thread for satisfied records

        // set up executor services
        final ExecutorService filterThreads = new CustomThreadPoolExecutor(noOfThreads, exitStatus, logger);
        final ExecutorService writeThreads = new CustomThreadPoolExecutor(2, exitStatus, logger);

        try {


            // kick-off filtering thread
            for (int i = 0; i < noOfThreads; i++) {
                filterThreads.execute(new AnnotationFiltering(readQueue,
                        writeQueue, writeClipQueue, Thread.currentThread(),
                        filterLatch, writeLatch));
            }
            filterThreads.shutdown();

            //paired reads
            writeThreads.execute(new Writing(writeQueue, output, Thread.currentThread(), filterLatch, writeLatch, queryNameHeader));

            //soft clips
            writeThreads.execute(new Writing(writeClipQueue, clippedFile, Thread.currentThread(), filterLatch, writeLatch, coordinateHeader));
            writeThreads.shutdown();

            logger.info("waiting for  threads to finish (max wait will be 100 hours)");
            filterThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
            writeThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);

            if (!readQueue.isEmpty() || !writeQueue.isEmpty() || !writeClipQueue.isEmpty()) {
                throw new Exception(
                        " threads have completed but queue isn't empty  (readQueue, writeQueue, writeClipQueue ):  "
                                + readQueue.size() + ", " + writeQueue.size() + "," + writeClipQueue.size());
            }
            logger.info("All threads finished");

            if (runPair && goodPairRecordCount.longValue() == 0) {
                throw new QSVException("LOW_FILTER_COUNT", "discordant pair");
            }
            if (runClip && goodClipCount.longValue() == 0) {
                throw new QSVException("LOW_FILTER_COUNT", "clip");
            }
            logger.info("Finished filtering file: " + input.getAbsolutePath() + " .Discordant Pair records: " +
                    goodPairRecordCount.longValue() + "; Soft clip records: " + goodClipCount.longValue() + "; Unmapped records: " + unmappedCount.intValue());

        } catch (final Exception e) {

            logger.error(QSVUtil.getStrackTrace(e));
            if (exitStatus.intValue() == 0) {
                exitStatus.incrementAndGet();
            }
        } finally {
            // kill off any remaining threads
            writeThreads.shutdownNow();
            filterThreads.shutdownNow();
            mainLatch.countDown();
        }
    }

    public AtomicLong getClipCount() {
        return goodClipCount;
    }


    /**
     * Class to annotate and filter discordant pairs, clips and unmapped readsS
     */
    class AnnotationFiltering implements Runnable {

        private final AbstractQueue<List<Chromosome>> queueIn;
        private final AbstractQueue<SAMRecord> queueOutPair;
        private final Thread mainThread;
        private final CountDownLatch filterLatch;
        private final CountDownLatch writeLatch;
        private int countOutputSleep;
        private final AbstractQueue<SAMRecord> queueOutClip;
        QueryExecutor pairQueryEx;
        QueryExecutor lifescopeQueryEx;
        QueryExecutor clipQueryEx;


        public AnnotationFiltering(AbstractQueue<List<Chromosome>> readQueue,
                                   AbstractQueue<SAMRecord> writeQueue, AbstractQueue<SAMRecord> writeClipQueue, Thread mainThread,
                                   CountDownLatch fLatch, CountDownLatch wGoodLatch) throws Exception {
            this.queueIn = readQueue;
            this.queueOutPair = writeQueue;
            this.queueOutClip = writeClipQueue;
            this.mainThread = mainThread;
            this.filterLatch = fLatch;
            this.writeLatch = wGoodLatch;
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

        @Override
        public void run() {

            int sleepcount = 0;
            countOutputSleep = 0;
            boolean run = true;

            try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(input, "silent")){

                List<Chromosome> chromosomes;
                while (run) {
                    chromosomes = queueIn.poll();

                    if (chromosomes == null) {
                        run = false;
                    } else {
                        // pass the record to filter and then to output queue
                        for (final Chromosome chromosome : chromosomes) {
                            logger.info("Reading records from chromosome: " + chromosome.getName() + " in sample " + parameters.getFindType());

                            BufferedWriter writer = null;

                            //set up writer to write clips to txt file
                            if (runClip) {
                                writer = new BufferedWriter(new FileWriter(SoftClipStaticMethods.getSoftClipFile(chromosome.getName(), parameters.getFindType(), softClipDir), true));
                            }

                            //write reads
                            run = readAndWriteData(reader, writer, chromosome, chromosome.getStartPosition(), chromosome.getEndPosition());

                            if (runClip) {
                                writer.close();
                            }
                        }
                    } // end else
                }// end while
                logger.info("Completed filtering thread: " + Thread.currentThread().getName());

            } catch (final Exception e) {
                logger.error("Setting exit status in annotation thread to 1 as exception caught: " + QSVUtil.getStrackTrace(e));
                if (exitStatus.intValue() == 0) {
                    exitStatus.incrementAndGet();
                }
                mainThread.interrupt();
            } finally {

                logger.debug(String
                        .format(" total slept %d times since input queue is empty and %d time since either output queue is full. each sleep take %d mill-second. queue size for qIn, qOutGood and qOutBad are %d, %d",
                                sleepcount, countOutputSleep, sleepUnit,
                                queueIn.size(), queueOutPair.size()));
                filterLatch.countDown();
            }
        }

        private boolean readAndWriteData(SamReader reader, BufferedWriter writer, Chromosome chromosome, int startPos, int endPos) throws Exception {

            boolean result = true;

//            try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(input, "silent")) {
            SAMRecordIterator iter = null;
            try {
                int queryStart = startPos - 100;
                if (queryStart <= 0) {
                    queryStart = 1;
                }
                final int queryEnd = endPos + 100;
                //get reads from bam
                iter = reader.queryOverlapping(chromosome.getName(), queryStart, queryEnd);

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
                                if ((!record.getReadUnmappedFlag() && !record.getDuplicateReadFlag() && record.getCigarString().contains("S"))
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
            } finally {
                if (null != iter) {
                    iter.close();
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
                return add2queue(record, queueOutClip, count);
            }


            //see if clips pass the filter
            if (clipQueryEx.Execute(record)) {
                goodClipCount.incrementAndGet();
                SoftClipStaticMethods.writeSoftClipRecord(writer, record, rgId, start, end, chromosome.getName());
                return add2queue(record, queueOutClip, count);
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
                                return add2queue(record, queueOutPair, count, writeLatch);
                            } else {
                                Object xc = record.getAttribute("XC");
                                if (lifescopeQueryEx != null && record.getAttribute("SM") == null && xc != null) {
                                    //try to filter lifescope reads
                                    if (lifescopeQueryEx.Execute(record)) {
                                        goodPairRecordCount.incrementAndGet();
                                        return add2queue(record, queueOutPair, count, writeLatch);
                                    } else {
                                        record.setAttribute(QSVConstants.ZP, xc);
                                        if (lifescopeQueryEx.Execute(record)) {
                                            return add2queue(record, queueOutPair, count, writeLatch);
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
                    final SAMFileWriterFactory writeFactory = new SAMFileWriterFactory();
                    if (header.getSortOrder().equals(SortOrder.coordinate)) {
                        writeFactory.setCreateIndex(true);
                    }
                    // there is a bug at current version, cause an exception but we
                    // can use below static method
                    SAMFileWriterImpl.setDefaultMaxRecordsInRam(maxRecords);

                    /*
                     * We want the temp bam files to go to our temp folder rather than the default
                     * Set this at the factory level before creating writers
                     */
                    writeFactory.setTempDirectory(file.getParentFile());

                    /*
                    * try using the qpicard writer to see if that will handle CRAM files
                     */
//                    try (SAMFileWriter writer = writeFactory.makeBAMWriter(header, false, file, 1)) {
                    SAMFileWriter writer = null;
                    try {
                        SAMWriterFactory factory = new SAMWriterFactory(header, false, file, file.getParentFile(), maxRecords, true, false, -1, new File(parameters.getReference()));
                        writer = factory.getWriter();
                        logger.info("all tmp BAMs are located on " + file.getParentFile().getCanonicalPath());
                        logger.info("default maxRecordsInRam " + SAMFileWriterImpl.getDefaultMaxRecordsInRam());
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

                                while (queue.size() >= maxRecords) {
                                    try {
                                        Thread.sleep(sleepUnit);
                                        countSleep++;
                                    } catch (final Exception e) {
                                        logger.info(Thread.currentThread().getName() + " " + QSVUtil.getStrackTrace(e));
                                    }
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

