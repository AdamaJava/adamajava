/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;


public class QExec {
	
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
		
		String h;
		try {
			h = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			h = e.getMessage();
		}
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
