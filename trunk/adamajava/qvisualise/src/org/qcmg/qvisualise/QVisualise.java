/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.qvisualise.report.HTMLReportGenerator;
import org.qcmg.qvisualise.report.Report;
import org.qcmg.qvisualise.report.ReportBuilder;
import org.qcmg.qvisualise.report.XmlReportReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class QVisualise {
	
	private static QLogger logger;
	
	private static final String XML_EXTENTION = "xml";
	private static final String HTML_EXTENTION = "html";
	private static final String REPORT = "Report";
	
	private String inputFile;
	private String outputFile;
	
	private int exitStatus;
	
	protected int engage() throws Exception {
		Document doc = XmlReportReader.createDocumentFromFile(new File(inputFile));		
		
		if (null != doc) {
			final List<ProfileType> reportElements = new ArrayList<ProfileType>();
			final List<Report> reports = new ArrayList<Report>();
			
			for (ProfileType type : ProfileType.values()) {
				if (null != doc.getElementsByTagName(type.getReportName() + REPORT).item(0))
					reportElements.add(type);
			}
			 
			for (ProfileType type : reportElements) {				
				String reportName = type.getReportName();				
				// we could have more than 1 report of a particular type
				NodeList reportNL = doc.getElementsByTagName(reportName + REPORT);
				for (int i = 0 ; i < reportNL.getLength() ; i++) {
//					Report report = ReportBuilder.buildReport(type, (Element) reportNL.item(i), i+1);
					Report report = ReportBuilder.buildReport(type, (Element) reportNL.item(i), i+1,  (Element) doc.getElementsByTagName("qProfiler").item(0) );
					if (null != report && ! report.getTabs().isEmpty())
						reports.add(report);
					else
						logger.info( "no HTML output generated by ReportBuilder.buildReport " + reportName + REPORT );
				}
			}
			
			if (reports.isEmpty()) {
				logger.error("no qvisualise output has been generated");
				exitStatus = 1;
			} else {
				HTMLReportGenerator reportGenerator = new HTMLReportGenerator(reports);
				String html = reportGenerator.generate();
				
				BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
				try {
					out.write(html);
				} finally {
					out.close();
				}
			}
		} else {
			logger.error("unable to create Document object from file: "+ inputFile);
			exitStatus = 1;
		}

		// not in the main method as this is called from qprofiler which invokes setup() directly
		logger.logFinalExecutionStats(exitStatus);
		return exitStatus;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(QVisualise.class);
		QVisualise qp = new QVisualise();
		int exitStatus = qp.setup(args);
		System.exit(exitStatus);
	}
	
	public int setup(String args[]) throws Exception{
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
		} else if ( ! options.hasInputOption()) {
			System.err.println(Messages.INSUFFICIENT_ARGUMENTS);
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogFileOption()) {
			System.err.println(Messages.USAGE);
		} else {
			logger = QLoggerFactory.getLogger(QVisualise.class, options.getLog(), options.getLogLevel());
			logger.logInitialExecutionStats("qvisualise", QVisualise.class.getPackage().getImplementationVersion(), args);
			
//			logger.tool("QVisualise called with following arguments: " + Arrays.deepToString(args));
			
			// get list of file names
			inputFile = options.getInputFile();
			outputFile = options.hasOutputOption() ? options.getOutputFile() :  inputFile + ".html";
			
			// check that input and output files are of the right type
			if ( ! FileUtils.isFileTypeValid(inputFile, XML_EXTENTION))
				throw new QVisualiseException("UNSUPPORTED_FILE_TYPE", inputFile);
			if ( ! FileUtils.isFileTypeValid(outputFile, HTML_EXTENTION))
				throw new QVisualiseException("UNSUPPORTED_FILE_TYPE", outputFile);
			
			// now check that we can read the input and write to the output
			if ( ! FileUtils.canFileBeRead(inputFile))
				throw new QVisualiseException("CANT_READ_INPUT_FILE", inputFile);
			if ( ! FileUtils.canFileBeWrittenTo(outputFile))
				throw new QVisualiseException("CANT_WRITE_TO_OUTPUT_FILE", outputFile);
			
			// don't like empty input files
			if (FileUtils.isFileEmpty(inputFile))
				throw new QVisualiseException("EMPTY_INPUT_FILE", inputFile);
			
			// setup proxy if running on qcmg-clustermk2
			try {
				String host = InetAddress.getLocalHost().getHostName();
				if (host.startsWith("minion")) {
					logger.info("setting proxy...");
					System.setProperty("http.proxyHost", "proxy.imb.uq.edu.au");
					System.setProperty("http.proxyPort", "3128");
				}
			} catch (UnknownHostException e) {
				logger.warn("could not set proxy");
			}
			
			logger.tool("running qVisualise with input file: " + inputFile + " and outputFile: " + outputFile);
			return engage();
		}
		return 1;
	}
}
