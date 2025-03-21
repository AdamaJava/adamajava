/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.motif;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;


/**
 * 
 * @author christix
 * This class is used to Summarise the qmotif xml files. The function is different to the Motif class.
 * usage: java -cp qmotif.jar org.qcmg.motif.Summariser --ini <a list of xmls from Motif class>  --output-xml <xml output for Summarise class> --log <log file>
 */
public class Summariser {
	
	private static QLogger logger = null;
	private static final int exitStatus = 1;
	
	private String inputFile;
	private String outputFile;
	private String logFile;
	private String version;
	
	
	private int letsGo() throws IOException {
		/*
		 * get the qmotif xml files to be summarised
		 */
		List<String>inputs = loadInputsFromFile(inputFile);
		logger.info("Will attempt to summarise " + inputs.size() + " qmotif xml files");
		if (inputs.isEmpty()) {
			logger.warn("No qmotif xml files found in " + inputFile);
			return exitStatus;
		}
		
		/*
		 * get summary data
		 */
		List<String> results = getAllSummaryData(inputs);
		logger.info("Will attempt to write out " + results.size() + " summaries");
		
		writeOutput(outputFile, results);
			
		return 0;
	}
	
	public static void writeOutput(String out, List<String> data) throws IOException {
		if ( ! StringUtils.isNullOrEmpty(out)) {
			try (PrintWriter pw = new PrintWriter(out)) {
				data.forEach(pw::println);
			}
		}
	}
	
	public static String getSummaryData(String file) {
		
		if ( ! StringUtils.isNullOrEmpty(file)) {
			/*
			 * get pertinent bits of info from qmotif xml file
			 */
			try (Stream<String> lines = Files.lines(Paths.get(file), Charset.defaultCharset())) {
				String x = lines.filter(s -> s.startsWith("<summary") 
						|| s.startsWith("<totalReadCount")
						|| s.startsWith("<noOfMotifs")
						|| s.startsWith("<rawUnmapped")
						|| s.startsWith("<rawIncludes")
						|| s.startsWith("<rawGenomic")
						|| s.startsWith("<scaledUnmapped")
						|| s.startsWith("<scaledIncludes")
						|| s.startsWith("<scaledGenomic")
						).collect(Collectors.joining("\t"));
				x = x.replace("<", "");
				x = x.replace("/>", "");
				x = x.replace(">", "");
				x = x.replace("\"", "");
				x = x.replace("count=", "");
				x = x.replace("summary", "");
				return x;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static List<String> getAllSummaryData(List<String> inputFiles) {
		return inputFiles.stream().map(Summariser::getSummaryData).collect(Collectors.toList());
	}
	
	public static List<String> loadInputsFromFile(String inputFile) throws IOException {
		if (null != inputFile) {
			try (Stream<String> lines = Files.lines(Paths.get(inputFile), Charset.defaultCharset());) {
				return lines.collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}
	
	
	public static void main(final String[] args) throws Exception {
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
			inputFile = options.getIniFile();
			outputFile = options.getOutputXmlFileName();
		}
		return letsGo();
	}
}
