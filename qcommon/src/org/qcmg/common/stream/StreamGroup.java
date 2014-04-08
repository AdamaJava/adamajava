/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.stream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.qcmg.common.stream.Operation;
import org.qcmg.common.stream.Stream;

public class StreamGroup<RecordType> {
	private List<Operation<RecordType>> operations;
	private RecordType eosInstance;
	private Iterable<RecordType> iterable;
	private Vector<Stream<RecordType>> streams = new Vector<Stream<RecordType>>();
	private int numberThreads;
	private final Integer[] entries;

	public StreamGroup(int numberThreads,
			List<Operation<RecordType>> operations,
			RecordType endOfStreamInstance, Iterable<RecordType> iterable)
			throws Exception {
		if (0 >= numberThreads) {
			throw new Exception(
					"Number of threads must be a positive integer value.");
		}
		this.numberThreads = numberThreads;
		this.operations = operations;
		this.eosInstance = endOfStreamInstance;
		this.iterable = iterable;
		this.entries = null;
		startStreams();
		for (Thread thread : streams) {
			thread.join();
		}
	}

	public StreamGroup(int numberThreads, Operation<RecordType> operation,
			RecordType endOfStreamInstance, Iterable<RecordType> iterable)
			throws Exception {
		if (0 >= numberThreads) {
			throw new Exception(
					"Number of threads must be a positive integer value.");
		}
		this.numberThreads = numberThreads;
		this.operations = new Vector<Operation<RecordType>>();
		operations.add(operation);
		this.eosInstance = endOfStreamInstance;
		this.iterable = iterable;
		this.entries = null;
	}

	public StreamGroup(int numberThreads,
			List<Operation<RecordType>> operations,
			RecordType endOfStreamInstance, Iterable<RecordType> iterable,
			Integer[] entries) throws Exception {
		if (0 >= numberThreads) {
			throw new Exception(
					"Number of threads must be a positive integer value.");
		}
		this.numberThreads = numberThreads;
		this.operations = operations;
		this.eosInstance = endOfStreamInstance;
		this.iterable = iterable;
		this.entries = entries;
		startStreams();
		for (Thread thread : streams) {
			thread.join();
		}
	}

	public StreamGroup(int numberThreads, Operation<RecordType> operation,
			RecordType endOfStreamInstance, Iterable<RecordType> iterable,
			Integer[] entries) throws Exception {
		if (0 >= numberThreads) {
			throw new Exception(
					"Number of threads must be a positive integer value.");
		}
		this.numberThreads = numberThreads;
		this.operations = new Vector<Operation<RecordType>>();
		operations.add(operation);
		this.eosInstance = endOfStreamInstance;
		this.iterable = iterable;
		this.entries = entries;
	}

	public void startStreams() throws Exception {
		for (int i = 0; i < numberThreads; i++) {
			Stream<RecordType> stream = new Stream<RecordType>(operations, eosInstance);
			streams.add(stream);
			stream.start();
		}
		if (null == entries || 0 == entries.length) {
			int i = 0;
			for (RecordType record : iterable) {
				streams.get(i).put(record);
				i++;
				if (i == numberThreads) {
					i = 0;
				}
			}
		} else {
			List<Integer> entryList = Arrays.asList(entries);
			int i = 0;
			int index = 0;
			for (RecordType record : iterable) {
				if (entryList.contains(index)) {
					streams.get(i).put(record);
					i++;
					if (i == numberThreads) {
						i = 0;
					}
				}
				index++;
			}
		}
		for (int j = 0; j < numberThreads; j++) {
			streams.get(j).put(eosInstance);
		}
		for (Thread thread : streams) {
			thread.join();
		}
	}
}
