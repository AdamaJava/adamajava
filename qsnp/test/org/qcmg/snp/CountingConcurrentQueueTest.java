package org.qcmg.snp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class CountingConcurrentQueueTest {

	@Test
	public void emptyQueue() {
		CountingConcurrentQueue<String> q = new CountingConcurrentQueue<>();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		assertNull(q.poll());
		assertNull(q.peek());
		// polling an empty queue must not decrement the count
		assertEquals(0, q.size());
	}

	@Test
	public void offerPollPeek() {
		CountingConcurrentQueue<String> q = new CountingConcurrentQueue<>();
		assertTrue(q.offer("a"));
		assertTrue(q.add("b"));
		assertEquals(2, q.size());
		assertFalse(q.isEmpty());
		assertEquals("a", q.peek());
		assertEquals(2, q.size());
		assertEquals("a", q.poll());
		assertEquals(1, q.size());
		assertEquals("b", q.poll());
		assertEquals(0, q.size());
		assertTrue(q.isEmpty());
		assertNull(q.poll());
		assertEquals(0, q.size());
	}

	@Test(expected = NullPointerException.class)
	public void nullNotAllowed() {
		new CountingConcurrentQueue<String>().offer(null);
	}

	@Test
	public void nullDoesNotChangeTheCount() {
		CountingConcurrentQueue<String> q = new CountingConcurrentQueue<>();
		try {
			q.offer(null);
			fail("expected a NullPointerException");
		} catch (NullPointerException expected) {
			// expected
		}
		assertEquals(0, q.size());
		assertTrue(q.isEmpty());
	}

	/*
	 * With a single consumer, an element that the consumer can see must already have been counted, so size() must not be 0
	 * (the count is incremented before the element is added to the underlying queue)
	 */
	@Test
	public void sizeNeverUnderCounts() throws InterruptedException {
		final int n = 2_000_000;
		CountingConcurrentQueue<Integer> q = new CountingConcurrentQueue<>();
		Thread producer = new Thread(() -> {
			for (int i = 0; i < n; i++) {
				q.offer(i);
			}
		});
		producer.start();
		int polled = 0;
		int underCounts = 0;
		while (polled < n) {
			if ( ! q.isEmpty()) {
				if (q.size() < 1) {
					underCounts++;
				}
				assertNotNull(q.poll());
				polled++;
			}
		}
		producer.join();
		assertEquals(0, underCounts);
		assertEquals(0, q.size());
	}

	@Test
	public void iteratorIsReadOnly() {
		CountingConcurrentQueue<String> q = new CountingConcurrentQueue<>();
		q.offer("a");
		q.offer("b");
		Iterator<String> iter = q.iterator();
		assertEquals("a", iter.next());
		try {
			iter.remove();
			fail("Should have thrown an UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {}
		assertEquals(2, q.size());
	}

	@Test
	public void clearUsesPollSoCountStaysCorrect() {
		CountingConcurrentQueue<String> q = new CountingConcurrentQueue<>();
		q.offer("a");
		q.offer("b");
		q.clear();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
	}

	@Test
	public void concurrentProducerConsumer() throws InterruptedException {
		final CountingConcurrentQueue<Integer> q = new CountingConcurrentQueue<>();
		final int n = 1_000_000;
		final CountDownLatch producerDone = new CountDownLatch(1);
		final AtomicLong consumed = new AtomicLong();

		Thread producer = new Thread(() -> {
			for (int i = 0; i < n; i++) {
				q.offer(i);
			}
			producerDone.countDown();
		});
		Thread consumer = new Thread(() -> {
			long c = 0;
			int expected = 0;
			while (c < n) {
				Integer i = q.poll();
				if (null != i) {
					assertEquals(expected++, i.intValue());
					c++;
				}
			}
			consumed.set(c);
		});
		producer.start();
		consumer.start();
		producer.join();
		consumer.join();

		assertEquals(n, consumed.get());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
	}
}
