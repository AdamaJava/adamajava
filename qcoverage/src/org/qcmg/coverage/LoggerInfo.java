/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

class LoggerInfo {
	private final String logFile;
	private final String logLevel;

	LoggerInfo(final Options options) {
		logFile = options.getLog();
		logLevel = options.getLogLevel();
	}

	String getLogFile() {
		return logFile;
	}

	String getLogLevel() {
		return logLevel;
	}
}
