package org.qcmg.motif;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

public class Summariser {
	
	private static QLogger logger = null;
	private static final int exitStatus = 1;
	
	private String inputFile;
	private String logFile;
	private String version;
	
	
	private int letsGo() throws IOException {
		/*
		 * get the qmotif xml files to be summarised
		 */
		List<String>inputs = loadInputsFromFile(inputFile);
		logger.info("Will attempt to summarise " + inputs.size() + " qmotif xml files");
		
		
		
		return 0;
	}
	
	public static List<String> loadInputsFromFile(String inputFile) throws IOException {
		if (null != inputFile) {
			try (Stream<String> lines = Files.lines(Paths.get(inputFile), Charset.defaultCharset());) {
				return lines.map(s -> s.toString()).collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}
	
	
	public static void main(final String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Motif.class);
		Summariser operation = new Summariser();
		int exitStatus = operation.setup( args );
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		else
			System.err.println(Messages.USAGE);
		System.exit(exitStatus);
		
	}
	
	int setup(String [] args) throws Exception{
		Options options = new Options(args);
		if (options.hasHelpOption() || null == args || args.length == 0) {
//			System.out.println(Messages.USAGE);
			options.displayHelp();
			return exitStatus;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			return exitStatus;
		} else {
			options.detectBadOptions();
			// loop through supplied files - check they can be read
			// configure logging
			logFile = options.getLog();
			version = Summariser.class.getPackage().getImplementationVersion();
			if (null == version) {
				version = "test";
			}
			logger = QLoggerFactory.getLogger(Summariser.class, logFile, options.getLogLevel());
			// get input file
			inputFile = options.getInput();
		}
		return letsGo();
	}
}
