/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

/**
 * The entry point for the command-line SAM/BAM merging tool.
 */
public final class Main {
	private static int exitStatus = 1; // Default to FAILURE
	private static boolean performLogging = true; // Default to true
	private static QLogger logger;

	public static void main(final String[] args) {
		try {
			LoadReferencedClasses.loadClasses(Main.class);
			
			
			Options options = new Options(args);
			if (options.hasHelpOption()) {
				displayHelpMessage(options);
				performLogging = false;
			} else if (options.hasVersionOption()) {
				displayVersionMessage();
				performLogging = false;
			} else {
				logger = QLoggerFactory.getLogger(Main.class, options.getLog(), options
						.getLogLevel());
				logger.logInitialExecutionStats(Messages.getProgramName(), Messages
						.getProgramVersion(), args);
				options.detectBadOptions();
				int numberRecords = -1;
				if (options.hasNumberRecordsOption()) {
					numberRecords = options.getNumberRecords()[0];
				}
				FileMerger operation = new FileMerger(
						options.getOutputFileNames()[0],
						options.getInputFileNames(),
						options.getGroupReplacementStrings(),
						reconstructCommandLine(Messages.getProgramName(), args),
						numberRecords, options.hasMergeOption(), 
						options.hasForceOption(), 
						options.hasCreateIndexOption(), 
						options.getTmpDir(),
						options.getValidation(),
						options.getComment(),
						options.getUUID());
 
				exitStatus = 0;// SUCCESS
			}
		} catch (Exception e) {
			String message = chooseErrorMessage(e);
			displayErrorMessage(message);
			if (performLogging && null != logger) {
				logger.error(message, e);
			}
		}
		if (performLogging && null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}

	private static void displayHelpMessage(Options options) throws Exception {
		System.out.println(Messages.USAGE);
		options.displayHelp();
	}

	private static void displayVersionMessage() throws Exception {
		System.err.println(Messages.getVersionMessage());
	}

	private static void displayErrorMessage(String message) {
		System.err.println(Messages.ERROR_PREFIX + message);
		System.err.println(Messages.USAGE);
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

	public static String reconstructCommandLine(final String programName,
			final String[] args) {
		String result = programName;

		for (final String arg : args) {
			result += " " + arg.replace("\t", " ");
		}

		return result.trim();
	}

}
