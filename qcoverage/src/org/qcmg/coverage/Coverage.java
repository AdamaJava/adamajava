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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfPositionComparator;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderFilter;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
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
		final File file = new File(options.getOutputFileNames()[0]);
		final BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("#coveragetype\tnumberofbases\tcoverage\n");
		final CoverageComparator comparator = new CoverageComparator();
		for (final CoverageReport report : stats.getCoverageReport()) {
			final String type = report.getType().toString().toLowerCase();
			final String feature = report.getFeature();
			out.write("#" + feature + StringUtils.RETURN);
			final List<CoverageModel> coverages = report.getCoverage();
			Collections.sort(coverages, comparator);
			for (final CoverageModel coverage : coverages) {
				final BigInteger bases = coverage.getBases();
				final String atCoverage = coverage.getAt() + "x";
				out.write(type + StringUtils.TAB + bases + StringUtils.TAB
						+ atCoverage + StringUtils.RETURN);
			}
		}
		out.close();
	}

	private void writePerTypeTabDelimitedCoverageReport(
			final QCoverageStats stats) throws IOException {
		final File file = new File(options.getOutputFileNames()[0]);
		final BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("#coveragetype\tfeaturetype\tnumberofbases\tcoverage\n");
		final CoverageComparator comparator = new CoverageComparator();
		for (final CoverageReport report : stats.getCoverageReport()) {
			final String type = report.getType().toString().toLowerCase();
			final String feature = report.getFeature();
			final List<CoverageModel> coverages = report.getCoverage();
			Collections.sort(coverages, comparator);
			for (final CoverageModel coverage : coverages) {
				final BigInteger bases = coverage.getBases();
				final String atCoverage = coverage.getAt() + "x";
				out.write(type + StringUtils.TAB + feature + StringUtils.TAB
						+ bases + StringUtils.TAB + atCoverage
						+ StringUtils.RETURN);
			}
		}
		out.close();
	}

	private void writeXMLCoverageReport(final QCoverageStats report)
			throws Exception {
		final JAXBContext context = JAXBContext.newInstance(QCoverageStats.class);
		final Marshaller m = context.createMarshaller();
		final StringWriter sw = new StringWriter();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(report, sw);
		final File file = new File(options.getOutputFileNames()[0]);
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(sw.toString());
		fileWriter.close();
	}
	
	private void writeVCFReport(final QCoverageStats report) throws Exception {
		final File file = new File(options.getOutputFileNames()[0] + ".vcf");
		final List<VcfRecord> vcfs = new ArrayList<VcfRecord>();
		
		for (final CoverageReport cr : report.getCoverageReport()) {
			if (cr.getFeature().contains("\t")) {
				final VcfRecord vcf = convertCoverageToVCFRecord(cr);
				vcfs.add(vcf);
			}
		}
		
		if ( ! vcfs.isEmpty()) {
			Collections.sort(vcfs, new VcfPositionComparator());
			try(final VCFFileWriter writer = new VCFFileWriter(file)) {
				final VcfHeader header = getHeaderForQCoverage(options.getBAMFileNames()[0], options.getInputGFF3FileNames()[0]);
				for(final VcfHeaderRecord record: header)  writer.addHeader(record.toString()+"\n");
				for (final VcfRecord vcf : vcfs)
					writer.add(vcf);				
			}  
		}
		
	}
	
	//create vcf output header
	private  VcfHeader getHeaderForQCoverage(final String bamFileName, final String gffFile) throws Exception {
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		final String version = Main.class.getPackage().getImplementationVersion();
		final String pg = Messages.getProgramName();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();

		//move input uuid into preuuid
		header.add(new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_VERSION));		
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate ));
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid ));
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version));
		header.add(new VcfHeaderRecord( "##bam_file=" + bamFileName ) );
		header.add( new VcfHeaderRecord("##gff_file=" + gffFile  ));
		header.add( new VcfHeaderFilter("LowQual","REQUIRED: QUAL < 50.0") );
		header.add( new VcfHeaderInfo("B", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "Bait end position", null, null) );
		header.add( new VcfHeaderInfo("BE", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "Bait end position", null, null)  );
		header.add( new VcfHeaderInfo("ZC", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "bases with Zero Coverage", null, null)  );
		header.add( new VcfHeaderInfo("NZC", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "bases with Non Zero Coverage", null, null)  );
		header.add( new VcfHeaderInfo("TOT", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "Total number of sequenced bases", null, null)  );
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE));		
 		
		return  header;
	}
	
	
	private static VcfRecord convertCoverageToVCFRecord(org.qcmg.coverage.CoverageReport covReport) throws Exception {
		
		// tab delimited string containing loads of useful stuff 
		final String feature = covReport.getFeature();
		// if there are no tabs in the string, the per-feature flag was not set
		final String[] params = TabTokenizer.tokenize(feature);
		
		final VcfRecord vcf = VcfUtils.createVcfRecord(params[0], Integer.parseInt(params[3]), null);
		
		// info field will contain coverage details
		int zeroCov = 0, nonZeroCov = 0, totalCov = 0;
		for (final CoverageModel c : covReport.getCoverage()) {
			final int coverage = Integer.parseInt(c.getAt());
			
			final int countAtCoverage = c.getBases().intValue();
			if (coverage == 0) zeroCov += countAtCoverage;
			else {
				nonZeroCov += countAtCoverage;
				final int cov = Integer.parseInt(c.getAt());
				totalCov += (cov * countAtCoverage);
			}
		}
		vcf.setInfo("B=" + params[2] + ";BE=" + params[4] + ";ZC=" + zeroCov + ";NZC=" + nonZeroCov + ";TOT=" + totalCov);
		
		return vcf;
	}

	private void saveCoverageReport() throws Exception {
		final QCoverageStats stats = new QCoverageStats();
		for (final CoverageReport report : jobQueue.getCoverageReport()) {
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
				final Coverage operation = new Coverage(moptions);
				exitStatus = 0; // SUCCESS
			}
		} catch (final Throwable e) {
			final String errorMessage = chooseErrorMessage(e);
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
			for (final StackTraceElement elem : throwable.getStackTrace()) {
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
