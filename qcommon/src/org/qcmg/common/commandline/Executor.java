/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.commandline;

import java.io.IOException;
import java.util.Arrays;

public class Executor {
	private final StreamConsumer outputStreamConsumer;
	private final StreamConsumer errorStreamConsumer;
	private final int errCode;

	public Executor(String arguments, String qualifiedMainClassName)
			throws IOException, InterruptedException {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " "
				+ qualifiedMainClassName + " ";
		String execCommand = javaCommand + arguments;
//		System.out.println("execCommand: " + execCommand);
		
		Process process = Runtime.getRuntime().exec(execCommand);
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}
	
	public Executor(String [] arguments, String qualifiedMainClassName)
	throws Exception {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " "
		+ qualifiedMainClassName + " ";
		String execCommand = javaCommand + Arrays.toString(arguments);
		Process process = Runtime.getRuntime().exec(execCommand);
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}

	public Executor(String jvmArgs, String arguments, String qualifiedMainClassName)
			throws Exception {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " " + jvmArgs + " "
				+ qualifiedMainClassName + " ";
		String execCommand = javaCommand + arguments;
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
}
