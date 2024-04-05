package org.qcmg.common.model;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class QCMGAtomicLongArrayTest {

    @Test
    public void testConstructor() {
        QCMGAtomicLongArray array = new QCMGAtomicLongArray(16);

        for (int i = 0; i < 17; i++) {
            array.increment(i);
        }
        for (int i = 0; i < 17; i++) {
            assertEquals(1, array.get(i));
        }

    }

    @Test
    public void testResize() throws InterruptedException {

        final int noOfLoops = 1000000;

        final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);
        // will create an array of length 20

        // create 2 threads, one that just increments values 0-20, and another that resizes
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int j = 0; j < 3; j++) {
            executor.execute(() -> {
                for (int i = 0; i < noOfLoops; i++)
                    array.increment(i % 20);
            });
        }
        executor.execute(() -> {
            int counter = 1;
            for (int i = 0; i < noOfLoops; i++)
                if (i % 20000 == 0)
                    array.increment(20 * counter++);
        });

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        for (int i = 0; i < 20; i++) {
            assertEquals((noOfLoops / 20) * 3, array.get(i));
        }
    }

    @Test
    public void isEmptyTest() {
        final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);
        assertTrue(array.isEmpty());

        array.increment(5);
        assertFalse(array.isEmpty());

    }

    @Test
    public void getSumTest() {
        final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);

        for (int i = 0; i < 5; i++) {
            array.increment(i * 2, 10);
        }
        assertEquals(50, array.getSum());

        array.increment(9, -10);
        assertEquals(40, array.getSum());

    }

}
