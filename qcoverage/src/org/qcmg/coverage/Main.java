/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public final class Main {
	private static Options options = null;
	private static int exitStatus = 1; // Defaults to FAILURE
	private static boolean performLogging = false; // Defaults to false
	private static QLogger logger = null;

	public static void main(final String[] args) {
		try {
			options = new Options(args);
			if (options.hasHelpOption()) {
				displayHelpMessage();
			} else if (options.hasVersionOption()) {
				displayVersionMessage();
			} else {
				options.detectBadOptions();
				performLogging = true;
				logger = QLoggerFactory.getLogger(Main.class, options.getLog(),
						options.getLogLevel());
				logger.logInitialExecutionStats(getProgramName(),
						getProgramVersion(), args);
				
				if (options.hasSegementerOption()) {
					logger.info("Running segmenter");
					String cmdLine = QLogger.reconstructCommandLine("qcoverage", args);	
					Segmenter operation = new Segmenter(options, cmdLine);
				} else {
					logger.info("Running coverage");
					Coverage operation = new Coverage(options);					
				}
				exitStatus = 0; // SUCCESS
			}
		} catch (Throwable e) {
			String errorMessage = chooseErrorMessage(e);
			logErrorMessage(errorMessage, e);
		}
		if (performLogging && null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}

	private static String chooseErrorMessage(Throwable e) {
		String message = null;
		if (null == e.getMessage()) {
			message = "Unknown error";
		} else {
			message = e.getMessage();
		}
		return message;
	}

	private static void logErrorMessage(final String errorMessage,
			final Throwable throwable) {
		System.err.println(Messages.ERROR_PREFIX + errorMessage);
		System.err.println(Messages.USAGE);
		if (performLogging && null != logger) {
			logger.error(errorMessage, throwable);
			for (StackTraceElement elem : throwable.getStackTrace()) {
				logger.error(elem.toString());
			}
		}
	}

	private static void displayHelpMessage() throws Exception {
		System.out.println(Messages.USAGE);
		options.displayHelp();
	}

	private static void displayVersionMessage() throws Exception {
		System.err.println(Messages.getVersionMessage());
	}

	static String getProgramName() {
		return Main.class.getPackage().getImplementationTitle();
	}

	static String getProgramVersion() {
		return Main.class.getPackage().getImplementationVersion();
	}

	static String getVersionMessage() throws Exception {
		return getProgramName() + ", version " + getProgramVersion();
	}
}
