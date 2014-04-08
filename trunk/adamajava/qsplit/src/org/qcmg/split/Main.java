/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

import java.io.IOException;
import java.net.URISyntaxException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

public final class Main {
	private static int exitStatus = 1; // Default to FAILURE
	private static boolean performLogging = true; // Default to true
	private static QLogger logger;

	public static void main(final String[] args) throws URISyntaxException, IOException, ClassNotFoundException {
		LoadReferencedClasses.loadClasses(Main.class);
		try {
			Options options = new Options(args);
			if (options.hasHelpOption()) {
				displayHelpMessage(options);
				performLogging = false;
			} else if (options.hasVersionOption()) {
				displayVersionMessage();
				performLogging = false;
			} else if ( ! options.hasLogOption()) {
				displayHelpMessage(options);
				performLogging = false;
			} else {
				logger = QLoggerFactory.getLogger(Main.class, options.getLog(), options.getLogLevel());
				logger.logInitialExecutionStats(Messages.getProgramName(), Messages
						.getProgramVersion(), args);
				options.detectBadOptions();
				if (options.hasSingleLevel()) {
					SingleLevelSplit op = new SingleLevelSplit(options);
				} else {
					Split operation = new Split(options);
				}
				exitStatus = 0; // SUCCESS
			}
		} catch (Exception e) {
			String message = chooseErrorMessage(e);
			displayErrorMessage(message);
			logErrorMessage(message, e);
			exitStatus = 1;
		}
		if (performLogging && null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}

	private static void displayErrorMessage(String message) {
		System.err.println(Messages.ERROR_PREFIX + message);
		System.err.println(Messages.USAGE);
	}

	private static void logErrorMessage(final String errorMessage,
			final Throwable throwable) {
		System.err.println(Messages.ERROR_PREFIX + errorMessage);
		System.err.println(Messages.USAGE);
		if (performLogging && null != logger) {
			logger.error(errorMessage, throwable);
//			for (StackTraceElement elem : throwable.getStackTrace()) {
//				logger.error(elem.toString());
//			}
		}
	}
	
	private static void displayHelpMessage(Options options) throws Exception {
		System.out.println(Messages.USAGE);
		options.displayHelp();
	}

	private static void displayVersionMessage() throws Exception {
		System.err.println(Messages.getVersionMessage());
	}

	private static String chooseErrorMessage(Exception e) {
		String message = null;
		if (null == e.getMessage()) {
			message = "Unknown error";
		} else {
			message = e.getMessage();
		}
		return message;
	}
}
