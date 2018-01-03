/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.stream;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Stream<DataType> extends Thread {
	private final List<Operation<DataType>> operationSequence;
	private final DataType endOfStreamInstance;
	private final BlockingQueue<DataType> inputQueue = new LinkedBlockingQueue<>(
			100);

	public Stream(final List<Operation<DataType>> operationSequence,
			final DataType endOfStreamInstance) {
		this.operationSequence = operationSequence;
		this.endOfStreamInstance = endOfStreamInstance;
		setDaemon(true);
	}

	public void put(final DataType record) throws InterruptedException {
		inputQueue.put(record);
	}

	@Override
	public void run() {
		while (true) {
			try {
				DataType data = inputQueue.take();
				if (endOfStreamInstance == data) {
					return; // ends the thread
				}
				boolean drop = false;
				for (final Operation<DataType> operation : operationSequence) {
					drop = operation.applyTo(data);
					if (drop) {
						break;
					}
				}
			} catch (InterruptedException e) {
				// Fall through for run() to return which ends the thread
			}
		}
	}
}
