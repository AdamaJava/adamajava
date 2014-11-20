/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfPositionComparator;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.vcf.VCFFileWriter;

public final class Coverage {
	private final Options options;
	private final Configuration invariants;
	private final JobQueue jobQueue;

	public Coverage(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;
		invariants = new Configuration(options);
		jobQueue = new JobQueue(invariants);
		saveCoverageReport();
	}

	private void writePerFeatureTabDelimitedCoverageReport(
			final QCoverageStats stats) throws IOException {
		File file = new File(options.getOutputFileNames()[0]);
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("#coveragetype\tnumberofbases\tcoverage\n");
		CoverageComparator comparator = new CoverageComparator();
		for (final CoverageReport report : stats.getCoverageReport()) {
			String type = report.getType().toString().toLowerCase();
			String feature = report.getFeature();
			out.write("#" + feature + StringUtils.RETURN);
			List<CoverageModel> coverages = report.getCoverage();
			Collections.sort(coverages, comparator);
			for (final CoverageModel coverage : coverages) {
				BigInteger bases = coverage.getBases();
				String atCoverage = coverage.getAt() + "x";
				out.write(type + StringUtils.TAB + bases + StringUtils.TAB
						+ atCoverage + StringUtils.RETURN);
			}
		}
		out.close();
	}

	private void writePerTypeTabDelimitedCoverageReport(
			final QCoverageStats stats) throws IOException {
		File file = new File(options.getOutputFileNames()[0]);
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("#coveragetype\tfeaturetype\tnumberofbases\tcoverage\n");
		CoverageComparator comparator = new CoverageComparator();
		for (final CoverageReport report : stats.getCoverageReport()) {
			String type = report.getType().toString().toLowerCase();
			String feature = report.getFeature();
			List<CoverageModel> coverages = report.getCoverage();
			Collections.sort(coverages, comparator);
			for (final CoverageModel coverage : coverages) {
				BigInteger bases = coverage.getBases();
				String atCoverage = coverage.getAt() + "x";
				out.write(type + StringUtils.TAB + feature + StringUtils.TAB
						+ bases + StringUtils.TAB + atCoverage
						+ StringUtils.RETURN);
			}
		}
		out.close();
	}

	private void writeXMLCoverageReport(final QCoverageStats report)
			throws Exception {
		JAXBContext context = JAXBContext.newInstance(QCoverageStats.class);
		Marshaller m = context.createMarshaller();
		StringWriter sw = new StringWriter();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(report, sw);
		File file = new File(options.getOutputFileNames()[0]);
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(sw.toString());
		fileWriter.close();
	}
	
	private void writeVCFReport(final QCoverageStats report) throws IOException {
		File file = new File(options.getOutputFileNames()[0] + ".vcf");
		List<VCFRecord> vcfs = new ArrayList<VCFRecord>();
		
		for (CoverageReport cr : report.getCoverageReport()) {
			if (cr.getFeature().contains("\t")) {
				VCFRecord vcf = convertCoverageToVCFRecord(cr);
				vcfs.add(vcf);
			}
		}
		
		if ( ! vcfs.isEmpty()) {
			Collections.sort(vcfs, new VcfPositionComparator());
				
			VCFFileWriter writer = new VCFFileWriter(file);
			try {
				writer.addHeader(VcfUtils.getHeaderForQCoverage(options.getBAMFileNames()[0], options.getInputGFF3FileNames()[0]));
				for (VCFRecord vcf : vcfs)
					writer.add(vcf);
				
			} finally {
				writer.close();
			}
		}
		
	}
	
	private static VCFRecord convertCoverageToVCFRecord(org.qcmg.coverage.CoverageReport covReport) {
		
		// tab delimited string containing loads of useful stuff 
		String feature = covReport.getFeature();
		// if there are no tabs in the string, the per-feature flag was not set
		String[] params = TabTokenizer.tokenize(feature);
		
		VCFRecord vcf = VcfUtils.createVcfRecord(params[0], Integer.parseInt(params[3]), null);
		
		// info field will contain coverage details
		int zeroCov = 0, nonZeroCov = 0, totalCov = 0;
		for (CoverageModel c : covReport.getCoverage()) {
			int coverage = Integer.parseInt(c.getAt());
			
			int countAtCoverage = c.getBases().intValue();
			if (coverage == 0) zeroCov += countAtCoverage;
			else {
				nonZeroCov += countAtCoverage;
				int cov = Integer.parseInt(c.getAt());
				totalCov += (cov * countAtCoverage);
			}
		}
		vcf.setInfo("B=" + params[2] + ";BE=" + params[4] + ";ZC=" + zeroCov + ";NZC=" + nonZeroCov + ";TOT=" + totalCov);
		
		return vcf;
	}

	private void saveCoverageReport() throws Exception {
		QCoverageStats stats = new QCoverageStats();
		for (CoverageReport report : jobQueue.getCoverageReport()) {
			stats.getCoverageReport().add(report);
		}
		if (options.hasVcfFlag() && options.hasPerFeatureOption()) {
			writeVCFReport(stats);
		}
		if (options.hasXmlFlag()) {
			writeXMLCoverageReport(stats);
//			if (options.hasPerFeatureOption())
//				writeVCFReport(stats);
		} else if (options.hasPerFeatureOption()) {
			writePerFeatureTabDelimitedCoverageReport(stats);
		} else {
			writePerTypeTabDelimitedCoverageReport(stats);
		}
	}

	private static Options moptions = null;
	private static int exitStatus = 1; // Defaults to FAILURE
	private static boolean performLogging = false; // Defaults to false
	private static QLogger mlogger = null;

	public static void main(final String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Coverage.class);
		try {
			moptions = new Options(args);
			if (moptions.hasHelpOption()) {
				displayHelpMessage();
			} else if (moptions.hasVersionOption()) {
				displayVersionMessage();
			} else {
				moptions.detectBadOptions();
				performLogging = true;
				mlogger = QLoggerFactory.getLogger(Coverage.class, moptions
						.getLog(), moptions.getLogLevel());
				mlogger.logInitialExecutionStats(getProgramName(),
						getProgramVersion(), args);
				Coverage operation = new Coverage(moptions);
				exitStatus = 0; // SUCCESS
			}
		} catch (Throwable e) {
			String errorMessage = chooseErrorMessage(e);
			logErrorMessage(errorMessage, e);
		}
		if (performLogging && null != mlogger) {
			mlogger.logFinalExecutionStats(exitStatus);
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
		throwable.printStackTrace();
		if (performLogging && null != mlogger) {
			mlogger.error(errorMessage, throwable);
			for (StackTraceElement elem : throwable.getStackTrace()) {
				mlogger.error(elem.toString());
			}
		}
	}

	private static void displayHelpMessage() throws Exception {
		System.out.println(Messages.USAGE);
		moptions.displayHelp();
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
