/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2026.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * A {@link ConcurrentLinkedQueue} wrapper with a constant time {@link #size()}.
 * <p>
 * {@link ConcurrentLinkedQueue#size()} traverses every node in the queue, which is expensive when the queue holds many records.
 * This class keeps a running count instead. The count is incremented before the underlying offer and decremented after a successful
 * poll, so every element in the queue has always been counted: {@link #size()} never under-counts, but can briefly over-count by
 * the number of in-flight operations. That is the safe direction for back pressure and "wait until empty" checks.
 * Use {@link #isEmpty()} (which delegates to the underlying queue) for an exact emptiness check.
 * <p>
 * Elements can only be removed via {@link #poll()} - the iterator is read only so that the count can't drift.
 *
 * @param <E>
 */
public class CountingConcurrentQueue<E> extends AbstractQueue<E> {

	private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();
	/*
	 * LongAdder rather than AtomicInteger as the producer increments and the consumer decrements concurrently
	 */
	private final LongAdder count = new LongAdder();

	@Override
	public boolean offer(E e) {
		Objects.requireNonNull(e);
		/*
		 * count first, so that a consumer polling this element straight away can't take the count below the number of elements in the queue
		 */
		count.increment();
		queue.offer(e);		// always returns true for a non-null element
		return true;
	}

	@Override
	public E poll() {
		E e = queue.poll();
		if (null != e) {
			count.decrement();
		}
		return e;
	}

	@Override
	public E peek() {
		return queue.peek();
	}

	@Override
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	@Override
	public int size() {
		long size = count.sum();
		return size <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, size);
	}

	@Override
	public Iterator<E> iterator() {
		Iterator<E> iter = queue.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}
			@Override
			public E next() {
				return iter.next();
			}
		};
	}
}
