/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;


public class QExec {

	/**
	 * Maximum time to wait for the local host name to be resolved via {@link InetAddress}.
	 * Resolving the local host name can take several seconds if the DNS/hosts configuration
	 * of the machine is slow or misconfigured. As the host name is only used for logging
	 * purposes, the resolution is performed in the background and a faster alternative is
	 * used when it does not complete within this time frame.
	 */
	private static final long HOST_NAME_RESOLUTION_TIMEOUT_MS = 250;

	/**
	 * Maximum time to wait for the "hostname" command to complete.
	 */
	private static final long HOST_NAME_COMMAND_TIMEOUT_MS = 500;

	private static final String UNKNOWN_HOST_NAME = "unknown";

	private final KeyValue uuid;
	private final KeyValue startTime;
	private final KeyValue osName;
	private final KeyValue osArch;
	private final KeyValue osVersion;
	private final KeyValue runBy;
	private final KeyValue toolName;
	private final KeyValue toolVersion;
	private final KeyValue javaHome;
	private final KeyValue javaVendor;
	private final KeyValue javaVersion;
	private final KeyValue host;
	private final KeyValue commandLine;
	
	public QExec(String programName, String programVersion,  String[] cmd_args, String cmd, String uuid) {
		this.uuid = new KeyValue("Uuid", null != uuid ? uuid : createUUid());
		this.startTime = new KeyValue("StartTime", DateUtils.getCurrentDateAsString());
		this.osName = new KeyValue("OsName", System.getProperty("os.name"));
		this.osArch = new KeyValue("OsArch", System.getProperty("os.arch"));
		this.osVersion = new KeyValue("OsVersion", System.getProperty("os.version"));
		this.runBy = new KeyValue("RunBy", System.getProperty("user.name"));
		this.toolName = new KeyValue("ToolName", programName);
		this.toolVersion = new KeyValue("ToolVersion", programVersion);
		
		String cmdLine= "";
		if (null != cmd_args) {  
			cmdLine = QLogger.reconstructCommandLine(programName, cmd_args);			
		} else if (null != cmd) {
			cmdLine = cmd;
		}
		this.commandLine = new KeyValue("CommandLine", cmdLine);
		
		this.javaHome = new KeyValue("JavaHome",System.getProperty("java.home"));
		this.javaVendor = new KeyValue("JavaVendor",System.getProperty("java.vendor"));
		this.javaVersion = new KeyValue("JavaVersion",System.getProperty("java.version"));
		
		String h = determineHostName();
		this.host = new KeyValue("host", h);

	}
	
	public QExec(String programName, String programVersion, String[] args) {
		this(programName, programVersion, args, null);
	}	
	
  	
	public QExec(String programName, String programVersion, String[] args, String uuid) {
		this(programName, programVersion, args, null, uuid);		
	}
 
	
	public static String createUUid() {
		return UUID.randomUUID().toString();
 	}

	/**
	 * Determines the name of the host on which this JVM is running.
	 * <p>
	 * The host name is resolved using {@link InetAddress#getLocalHost()} as this gives the
	 * canonical answer for the machine. Unfortunately this resolution can be very slow (e.g.
	 * several seconds) when the DNS/hosts configuration of the machine is not working
	 * properly, and the host name is only used for logging purposes. The resolution is
	 * therefore performed on a background thread and, if it does not complete promptly, a
	 * quicker alternative is used instead.
	 *
	 * @return the name of the host, never null
	 */
	static String determineHostName() {
		final FutureTask<String> task = new FutureTask<>(QExec::resolveHostName);
		final Thread thread = new Thread(task, "qcmg-host-name-resolver");
		thread.setDaemon(true);
		thread.start();

		String hostName = null;
		String resolutionError = null;
		try {
			hostName = task.get(HOST_NAME_RESOLUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			// the resolution is taking too long, fall back to the quicker alternatives
			// the background thread is left to complete of its own accord
		} catch (ExecutionException e) {
			if (e.getCause() instanceof UnknownHostException) {
				resolutionError = e.getCause().getMessage();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (null != hostName && ! hostName.isEmpty()) {
			return hostName;
		}
		return determineHostNameFallback(resolutionError);
	}

	/**
	 * Resolves the name of the local host.
	 */
	private static String resolveHostName() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName();
	}

	/**
	 * Determines the name of the host without using {@link InetAddress#getLocalHost()}.
	 * <p>
	 * The alternatives are tried in order of preference, ending with the address of the
	 * machine and finally a generic placeholder.
	 *
	 * @param resolutionError the message from the failed {@link InetAddress} resolution, may be null
	 * @return the name of the host, never null
	 */
	private static String determineHostNameFallback(final String resolutionError) {
		final String fromCommand = runHostNameCommand();
		if (null != fromCommand) {
			return fromCommand;
		}

		for (final String variable : Arrays.asList("HOSTNAME", "COMPUTERNAME")) {
			final String value = System.getenv(variable);
			if (null != value && ! value.trim().isEmpty()) {
				return value.trim();
			}
		}

		if (null != resolutionError && ! resolutionError.isEmpty()) {
			return resolutionError;
		}

		final String address = getLocalIpAddress();
		if (null != address) {
			return address;
		}

		return UNKNOWN_HOST_NAME;
	}

	/**
	 * Uses the "hostname" command to determine the name of the host.
	 * <p>
	 * On unix-like systems the "-f" option returns the fully qualified name, which matches
	 * the name that {@link InetAddress#getLocalHost()} would return. Windows does not
	 * support this option, so the short name is used there instead.
	 *
	 * @return the name of the host or null if it could not be determined
	 */
	private static String runHostNameCommand() {
		final boolean windows = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
		final List<String> command = windows ? Arrays.asList("hostname") : Arrays.asList("hostname", "-f");

		final FutureTask<String> task = new FutureTask<>(() -> {
			final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			process.waitFor();
			return output;
		});
		final Thread thread = new Thread(task, "qcmg-host-name-command");
		thread.setDaemon(true);
		thread.start();

		try {
			final String output = task.get(HOST_NAME_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			return output.isEmpty() ? null : output;
		} catch (TimeoutException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	/**
	 * Determines the IP address of this machine, without performing any name resolution.
	 *
	 * @return the IP address or null if it could not be determined
	 */
	private static String getLocalIpAddress() {
		try {
			final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (null == interfaces) {
				return null;
			}
			for (final NetworkInterface networkInterface : Collections.list(interfaces)) {
				if (networkInterface.isLoopback() || ! networkInterface.isUp()) {
					continue;
				}
				for (final InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
					if (! address.isLoopbackAddress() && ! address.isAnyLocalAddress()) {
						return address.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			// ignore and fall through to the unknown host name
		}
		return null;
	}
	
	public KeyValue getUuid() {
		return uuid;
	}

	public KeyValue getStartTime() {
		return startTime;
	}

	public KeyValue getOsName() {
		return osName;
	}

	public KeyValue getOsArch() {
		return osArch;
	}

	public KeyValue getOsVersion() {
		return osVersion;
	}

	public KeyValue getRunBy() {
		return runBy;
	}

	public KeyValue getToolName() {
		return toolName;
	}

	public KeyValue getToolVersion() {
		return toolVersion;
	}

	public KeyValue getJavaHome() {
		return javaHome;
	}

	public KeyValue getJavaVendor() {
		return javaVendor;
	}

	public KeyValue getJavaVersion() {
		return javaVersion;
	}

	public KeyValue getHost() {
		return host;
	}

	public KeyValue getCommandLine() {
		return commandLine;
	}

	public String getExecMetaDataToString() {
        return uuid.toExecString() +
                startTime.toExecString() +
                osName.toExecString() +
                osArch.toExecString() +
                osVersion.toExecString() +
                runBy.toExecString() +
                toolName.toExecString() +
                toolVersion.toExecString() +
                commandLine.toExecString() +
                javaHome.toExecString() +
                javaVendor.toExecString() +
                javaVersion.toExecString() +
                host.toExecString();
	}
	
}
