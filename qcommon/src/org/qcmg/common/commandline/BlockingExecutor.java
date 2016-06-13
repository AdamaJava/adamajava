/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.commandline;

public class BlockingExecutor {
	private final StreamConsumer outputStreamConsumer;
	private final StreamConsumer errorStreamConsumer;
	private final int errCode;

	public BlockingExecutor(String execCommand)
			throws Exception {
		Process process = Runtime.getRuntime().exec(execCommand);
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}

	public StreamConsumer getOutputStreamConsumer() {
		return outputStreamConsumer;
	}

	public StreamConsumer getErrorStreamConsumer() {
		return errorStreamConsumer;
	}

	public int getErrCode() {
		return errCode;
	}
	
	public boolean isSuccessful() {
		return 0 == errCode;
	}
	
	public boolean isFailure() {
		return 0 != errCode;
	}
	
}
