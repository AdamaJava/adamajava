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
	
	private final StreamConsumer outputStreamConsumer;
	private final StreamConsumer errorStreamConsumer;
	private final int errCode;	
	
	public Executor(String arguments, String qualifiedMainClassName) throws IOException, InterruptedException {			
		this("java -classpath " + System.getProperty("java.class.path") + " " + qualifiedMainClassName + " " + arguments);
	}
	
	public Executor(String [] arguments, String qualifiedMainClassName) throws IOException, InterruptedException {				
		this("java -classpath " + System.getProperty("java.class.path") + " " + qualifiedMainClassName + " " + Arrays.toString(arguments) );		
	}

	public Executor(String jvmArgs, String arguments, String qualifiedMainClassName) throws IOException, InterruptedException {
		this("java -classpath " + System.getProperty("java.class.path") + " " + jvmArgs + " " + qualifiedMainClassName + " " + arguments);				
	}

	public Executor(String execCommand) throws IOException, InterruptedException {
		Process process = Runtime.getRuntime().exec(execCommand);
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		//has to call run() rather than start() inside the construtor
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

	/**
	 * 
	 * @return 0 indicate succeed; others indicate failed
	 */
	public int getErrCode() {	
		return errCode;		
	}

}
