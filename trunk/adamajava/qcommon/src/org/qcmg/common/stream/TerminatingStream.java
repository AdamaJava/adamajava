/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.stream;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class TerminatingStream<DataType> implements Runnable {
	private final BlockingQueue<DataType> input;
	private final List<Operation<DataType>> operationSequence;
	private final DataType endOfStreamInstance;

	public TerminatingStream(final BlockingQueue<DataType> input,
			final BlockingQueue<DataType> output,
			final List<Operation<DataType>> operationSequence,
			final DataType endOfStreamInstance) {
		this.input = input;
		this.operationSequence = operationSequence;
		this.endOfStreamInstance = endOfStreamInstance;
	}

	@Override
	public void run() {
		while (true) {
			try {
				DataType data = input.take();
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
