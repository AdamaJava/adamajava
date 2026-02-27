/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.common.commandline;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Executor {
	
	private final StreamConsumer outputStreamConsumer;
	private final StreamConsumer errorStreamConsumer;
	private final int errCode;
	
	private static String getJavaPath() {
		return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
	}
	
	public Executor(String arguments, String qualifiedMainClassName) throws IOException, InterruptedException {
		this(getJavaPath(), "-classpath", System.getProperty("java.class.path"), qualifiedMainClassName, arguments);
	}
	
	public Executor(String [] arguments, String qualifiedMainClassName) throws IOException, InterruptedException {
		String javaPath = getJavaPath();
		String classPath = System.getProperty("java.class.path");
		List<String> commands = new ArrayList<>();
		commands.add(javaPath);
		commands.add("-classpath");
		commands.add(classPath);
		commands.add(qualifiedMainClassName);
		commands.addAll(Arrays.asList(arguments));
		
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		Process process = processBuilder.start();
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}

	public Executor(String jvmArgs, String arguments, String qualifiedMainClassName) throws IOException, InterruptedException {
		String javaPath = getJavaPath();
		String classPath = System.getProperty("java.class.path");
		List<String> commands = new ArrayList<>();
		commands.add(javaPath);
		commands.add("-classpath");
		commands.add(classPath);
		for (String arg : jvmArgs.split(" ")) {
			if (!arg.isEmpty()) {
				commands.add(arg);
			}
		}
		commands.add(qualifiedMainClassName);
		for (String arg : arguments.split(" ")) {
			if (!arg.isEmpty()) {
				commands.add(arg);
			}
		}
		
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		Process process = processBuilder.start();
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}

	// constructor for running a command line
	public Executor(String cmd1, String cmd2, String cmd3, String cmd4, String cmd5WithSpaces) throws IOException, InterruptedException {
		List<String> commands = new ArrayList<>(Arrays.asList(cmd1, cmd2, cmd3, cmd4));
		if (null != cmd5WithSpaces) {
			Collections.addAll(commands, cmd5WithSpaces.split(" "));
		}
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		Process process = processBuilder.start();
		outputStreamConsumer = new StreamConsumer(process.getInputStream());
		errorStreamConsumer = new StreamConsumer(process.getErrorStream());
		outputStreamConsumer.run();
		errorStreamConsumer.run();
		errCode = process.waitFor();
	}

	public Executor(String execCommand) throws IOException, InterruptedException {
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

	/**
	 * 
	 * @return 0 indicate succeed; others indicate failed
	 */
	public int getErrCode() {	
		return errCode;		
	}

}
