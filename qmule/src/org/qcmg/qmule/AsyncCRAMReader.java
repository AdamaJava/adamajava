package org.qcmg.qmule;

import htsjdk.samtools.*;
import htsjdk.samtools.util.CloseableIterator;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractQueue;
import java.util.concurrent.*;

public class AsyncCRAMReader {

    private static final QLogger logger = QLoggerFactory.getLogger(AsyncCRAMReader.class);

    public static final int CHECK_SIZE_INTERVAL = 2000000;

    private final ExecutorService executor;
    private final SamReader samReader;
    private final AbstractQueue<SAMRecord> recordQueue;
    private final int queueCapacity;
    private volatile boolean isIteratorEmpty = false;

    public AsyncCRAMReader(InputStream cramStream, File referenceFile, int queueCapacity) {
        this.executor = Executors.newSingleThreadExecutor();
        this.samReader = SamReaderFactory.makeDefault()
                .referenceSequence(referenceFile)
                .validationStringency(ValidationStringency.DEFAULT_STRINGENCY)
                .open(SamInputResource.of(cramStream));
        this.recordQueue = new ConcurrentLinkedQueue<>();
        this.queueCapacity = queueCapacity;
        startReading();
    }

    private void startReading() {
        logger.info("Start reading");
        executor.submit(() -> {
            try (CloseableIterator<SAMRecord> iterator = samReader.iterator()) {
                int i = 0;
                int j = 1;
                while (iterator.hasNext()) {
                    SAMRecord record = iterator.next();
                    recordQueue.add(record);
                    if (++i >= CHECK_SIZE_INTERVAL) {
                        logger.info("Processed " + (i * j) + " records");
                        if (recordQueue.size() >= queueCapacity) {
                            logger.info("Queue is full, waiting for it to be emptied");
                            while (recordQueue.size() >= queueCapacity) {
                                logger.info("Queue is full, waiting for it to be emptied: " + recordQueue.size());
                                Thread.sleep(100);
                            }
                        }
                        i = 0;
                        j++;
                    }
                }
                logger.info("iterator is empty, processed " + (j * CHECK_SIZE_INTERVAL + i) + " records");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
                isIteratorEmpty = true;
                logger.info("iterator is empty (in finally)");
            }
        });
    }

    public SAMRecord take() throws InterruptedException {
        return recordQueue.poll();
    }

    public boolean isIteratorEmpty() {
        return isIteratorEmpty && recordQueue.isEmpty();
    }

    public void close() throws IOException {
        samReader.close();
        executor.shutdownNow();
    }
}