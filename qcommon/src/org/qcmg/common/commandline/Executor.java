/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.commandline;

import java.io.IOException;
import java.util.Arrays;

public class Executor {
	private final String execCommand; 
	
	private  StreamConsumer outputStreamConsumer;
	private  StreamConsumer errorStreamConsumer;
	private  int errCode;	
	boolean alreadyExecuted = false;	
	
	public Executor(String arguments, String qualifiedMainClassName) throws IOException, InterruptedException {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " "
				+ qualifiedMainClassName + " ";
		this.execCommand = javaCommand + arguments;
	}
	
	public Executor(String [] arguments, String qualifiedMainClassName) throws Exception {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " "
		+ qualifiedMainClassName + " ";
		this.execCommand = javaCommand + Arrays.toString(arguments);
	}

	public Executor(String jvmArgs, String arguments, String qualifiedMainClassName) throws Exception {
		String classpath = System.getProperty("java.class.path");
		String javaCommand = "java -classpath " + classpath + " " + jvmArgs + " "
				+ qualifiedMainClassName + " ";
		this.execCommand = javaCommand + arguments;
	}

	public Executor(String execCommand) throws Exception {
		this.execCommand = execCommand;
	}

	public StreamConsumer getOutputStreamConsumer() {
		return outputStreamConsumer;
	}

	public StreamConsumer getErrorStreamConsumer() {
		return errorStreamConsumer;
	}

	public int getErrCode() throws IOException, InterruptedException {
		if( alreadyExecuted) { return errCode; }
		
		Process process = Runtime.getRuntime().exec(execCommand);
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.start();;
		errorStreamConsumer.start();;
		errCode = process.waitFor();		
		alreadyExecuted =  true;
		
		return errCode;		
	}


//	public boolean isSuccessful() {
//		return 0 == errCode;
//	}
//
//
//	public boolean isFailure() {
//		return 0 != errCode;
//	}
}
