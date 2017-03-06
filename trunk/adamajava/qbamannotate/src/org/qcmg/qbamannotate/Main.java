/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.io.File;
import java.io.FileWriter;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public final class Main {
	static final String ERROR_PREFIX = getProgramName() + ": ";
	static final String USAGE = Messages.getMessage("USAGE");
	private static QLogger logger;

	public static void main(final String[] args) {
		int exitStatus = 1;
		boolean performLogging = true;
		Options options = null;
		try {
			options = new Options(args);
			if (options.hasHelpOption()) {
				System.out.println(USAGE);
				options.displayHelp();
				performLogging = false;
			} else if (options.hasVersionOption()) {
				System.err.println(getVersionMessage());
				performLogging = false;
			} else {
				logger = QLoggerFactory.getLogger(Main.class, options.getLog(),
						options.getLogLevel());
				logger.logInitialExecutionStats(getProgramName(),
						getProgramVersion(), args);
				options.detectBadOptions();
				String commandLine = reconstructCommandLine(getProgramName(),
						args);
				AnnotatorType type = getType(options);
				if (options.hasSelfOption()) {
					if (options.hasInputMAFileNames()) {
						throw new Exception(
								"MA input files not required when using self annotation option. Remove MA file arguments");
					}
					new SelfAnnotator(options.getOutputFileName(), options.getInputBAMFileName(), type, getProgramName(), getProgramVersion(), commandLine, options.hasReportBamsOption());
					generateReport(type);
				} else if (options.hasInputMAFileNameA() && !options.hasInputMAFileNameB()) {
					new AdvancedAnnotator(options.getOutputFileName(), options.getInputBAMFileName(), options.getInputMAFileNameA(), type, getProgramName(), getProgramVersion(), commandLine);
					generateReport(type);
				} else if (options.hasInputMAFileNameA() && options.hasInputMAFileNameB()) {
					AdvancedAnnotator operation = new AdvancedAnnotator(options
							.getOutputFileName(),
							options.getInputBAMFileName(), options
									.getInputMAFileNameA(), options
									.getInputMAFileNameB(), type,
							getProgramName(), getProgramVersion(), commandLine);
					logger.info("Remaining Unmatched Records: "
							+ operation.getNumberOfUnmatchedRecords());
					generateReport(type);
				} else {
					// Execution should never reach this point
					assert false;
				}
			}
			exitStatus = 0;
		} catch (Exception e) {
			String message = chooseErrorMessage(e);
			displayErrorMessage(message);
			exitStatus = 1;
			if (performLogging && null != logger) {
				logger.error(message, e);
			}		
		}
		if (performLogging && null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}

	private static void displayErrorMessage(String message) {
		System.err.println(ERROR_PREFIX + message);
		System.err.println(USAGE);
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

	private static void generateReport(final AnnotatorType type)
			throws Exception {
		String report = type.generateReport();
		File file = new File("qbamannotatePairingStats.xml");
		FileWriter writer = new FileWriter(file);
		writer.write(report);
		writer.close();
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

	public static String reconstructCommandLine(final String programName, final String[] args) {
		String result = programName;
		for (final String arg : args) {
			result += " " + arg;
		}
		return result;
	}

	static AnnotatorType getType(Options options) throws Exception {
		AnnotatorType result = null;
		if (!options.hasTypeArgument()) {
			throw new BamAnnotateException("INSUFFICIENT_TYPE_ARGUMENTS");
		}
		if (options.hasTooManyTypeArguments()) {
			throw new BamAnnotateException("TOO_MANY_TYPES_SPECIFIED");
		}

		String type = (String) options.getTypes()[0];
		if (type.equals("frag")) {
			result = new Frag();
		} else if (type.equals("pebc")) {
			final int lsize;
			if (!options.hasLSizeLimit() && !options.hasTooManyLSizeLimits()) {
				throw new Exception(
						"PEBC type runs must specify a lower interval size limit using -l or -lsize");
			} else {
				lsize = options.getLowerISizeLimits()[0];
			}
			final int usize;
			if (!options.hasUSizeLimit() && !options.hasTooManyUSizeLimits()) {
				throw new Exception(
						"PEBC type runs must specify an upper interval size limit using -u or -usize");
			} else {
				usize = options.getUpperISizeLimits()[0];
			}
			result = new PairedEnd(lsize, usize);
		} else if (type.equals("lmp")) {
			final int lsize;
			if (!options.hasLSizeLimit() && !options.hasTooManyLSizeLimits()) {
				throw new Exception(
						"LMP type runs must specify a lower interval size limit using -l or -lsize");
			} else {
				lsize = options.getLowerISizeLimits()[0];
			}
			final int usize;
			if (!options.hasUSizeLimit() && !options.hasTooManyUSizeLimits()) {
				throw new Exception(
						"LMP type runs must specify an upper interval size limit using -u or -usize");
			} else {
				usize = options.getUpperISizeLimits()[0];
			}
			result = new LongMatePair(lsize, usize);
		} else {
			throw new Exception("Unknown run type \"" + type
					+ "\". Type must be \"frag\", \"lmp\", or \"pebc\"");
		}
		return result;
	}
}
