/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfPositionComparator;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.record.RecordWriter;

import java.io.*;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Coverage {
	private final Options options;
	private final JobQueue jobQueue;

	public Coverage(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;
		Configuration invariants = new Configuration(options);
		jobQueue = new JobQueue(invariants);
		saveCoverageReport(invariants.getCoverageType());
	}
	
	/**
	 * check output file extension whether match format
	 * @param fname is the input file name
	 * @param format is the input file required format, e.g. txt, xml and vcf
	 * @return return corrected output file name which extension match the format
	 */
	private String fileNameCorrection(String fname, String format) {
		String extension = format.startsWith(".")? 
				format.toLowerCase() : "." + format.toLowerCase();
				
		return fname.toLowerCase().endsWith(extension) ? 
				fname : fname + extension;
	}

	private void writePerFeatureTabDelimitedCoverageReport( final QCoverageStats stats) throws IOException {
		String foutput = fileNameCorrection(options.getOutputFileNames()[0], "txt");
		final File file = new File(foutput);
		try (final BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
			out.write("#coveragetype\tnumberofbases\tcoverage\n");
			final CoverageComparator comparator = new CoverageComparator();
			for (final CoverageReport report : stats.getCoverageReport()) {
				final String type = report.getType().toString().toLowerCase();
				final String feature = report.getFeature();
				out.write("#" + feature + StringUtils.RETURN);
				final List<CoverageModel> coverages = report.getCoverage();
				coverages.sort(comparator);
				for (final CoverageModel coverage : coverages) {
					final BigInteger bases = coverage.getBases();
					final String atCoverage = coverage.getAt() + "x";
					out.write(type + StringUtils.TAB + bases + StringUtils.TAB
							+ atCoverage + StringUtils.RETURN);
				}
			}
		}
	}

	private void writePerTypeTabDelimitedCoverageReport(final QCoverageStats stats) throws IOException {
		String foutput = fileNameCorrection(options.getOutputFileNames()[0], "txt");
		final File file = new File(foutput);
		try (final BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
			out.write("#coveragetype\tfeaturetype\tnumberofbases\tcoverage\n");
			final CoverageComparator comparator = new CoverageComparator();
			for (final CoverageReport report : stats.getCoverageReport()) {
				final String type = report.getType().toString().toLowerCase();
				final String feature = report.getFeature();
				final List<CoverageModel> coverages = report.getCoverage();
				coverages.sort(comparator);
				for (final CoverageModel coverage : coverages) {
					final BigInteger bases = coverage.getBases();
					final String atCoverage = coverage.getAt() + "x";
					out.write(type + StringUtils.TAB + feature + StringUtils.TAB
							+ bases + StringUtils.TAB + atCoverage
							+ StringUtils.RETURN);
				}
			}
		}
	}

	private void writeXMLCoverageReport(final QCoverageStats report) throws JAXBException, IOException {
		writeXMLCoverageReport(report, fileNameCorrection(options.getOutputFileNames()[0], "xml"));
	}

	public static void writeXMLCoverageReport(final QCoverageStats report, String outputFile) throws IOException, jakarta.xml.bind.JAXBException {
		jakarta.xml.bind.JAXBContext context = JAXBContextFactory
				.createContext(new Class[] {CoverageReport.class, CoverageModel.class, QCoverageStats.class}, null);
		final Marshaller m = context.createMarshaller();
		final StringWriter sw = new StringWriter();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(report, sw);
		final File file = new File(outputFile);
		try (final FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(sw.toString());
		}
	}

	private void writeVCFReport(final QCoverageStats report) throws Exception {
		String foutput = fileNameCorrection(options.getOutputFileNames()[0], "vcf");
		final File file = new File(foutput);

		final List<VcfRecord> vcfs = new ArrayList<>();
		
		for (final CoverageReport cr : report.getCoverageReport()) {
			if (cr.getFeature().contains("\t")) {
				final VcfRecord vcf = convertCoverageToVCFRecord(cr);
				vcfs.add(vcf);
			}
		}
		
		if ( ! vcfs.isEmpty()) {
			vcfs.sort(new VcfPositionComparator());
			try(final RecordWriter<VcfRecord> writer = new RecordWriter<>(file)) {
				final VcfHeader header = getHeaderForQCoverage(options.getBAMFileNames()[0], options.getInputGFF3FileNames()[0]);
				for(final VcfHeaderRecord record: header) {
					writer.addHeader(record.toString() + "\n");
				}
				for (final VcfRecord vcf : vcfs) {
					writer.add(vcf);				
				}
			}  
		}
		
	}
	
	//create vcf output header
	private  VcfHeader getHeaderForQCoverage(final String bamFileName, final String gffFile) {
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		final String version = Coverage.class.getPackage().getImplementationVersion();
		final String pg = Messages.getProgramName();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();

		//move input uuid into preuuid
		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid );
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);
		header.addOrReplace( "##bam_file=" + bamFileName);
		header.addOrReplace("##gff_file=" + gffFile);
		header.addFilter(VcfHeaderUtils.FILTER_LOW_QUAL,"REQUIRED: QUAL < 50.0");
		header.addInfo("B", "-1", "String", "Bait end position");
		header.addInfo("BE", "-1", "String", "Bait end position");
		header.addInfo("ZC",  "-1", "String", "bases with Zero Coverage");
		header.addInfo("NZC","-1", "String", "bases with Non Zero Coverage");
		header.addInfo("TOT", "-1", "String", "Total number of sequenced bases");
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);		
 		
		return  header;
	}
	
	
	private static VcfRecord convertCoverageToVCFRecord(org.qcmg.coverage.CoverageReport covReport) {
		
		// tab delimited string containing loads of useful stuff 
		final String feature = covReport.getFeature();
		// if there are no tabs in the string, the per-feature flag was not set
		final String[] params = TabTokenizer.tokenize(feature);
		
		final VcfRecord vcf = new VcfRecord.Builder(params[0], Integer.parseInt(params[3])).build();

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

	private void saveCoverageReport(CoverageType coverageType) throws Exception {

		//save low coverage bed file
		if (coverageType.equals(CoverageType.LOW_COVERAGE)) {
			final HashMap<String, List<LowCoverageRegion>> lowCoverageList = jobQueue.lowCoverageResultsFinalList();

			for (HashMap.Entry<String, List<LowCoverageRegion>> entry : lowCoverageList.entrySet()) {
				String key = entry.getKey();
				List<LowCoverageRegion> lowCoverageValues = entry.getValue();
				LinkedHashSet<String> refNameOrder = jobQueue.getRefNamesOrdered();

				//Sort vy refName, start and end so that bed is in correct order
				lowCoverageValues.sort(new LowCoverageRegionComparator(refNameOrder));

				String outfile = options.getOutputFileNames()[0];
				//rename .bed extension if present to add the mnin coverage value
				if (outfile.endsWith(".bed")) {
					outfile = outfile.substring(0, outfile.length() - 4);
				}
				outfile = String.format("%s.lowcov.%s.bed", outfile, key);

				try (final BufferedWriter out = new BufferedWriter(new FileWriter(outfile))) {
					for (final LowCoverageRegion region : lowCoverageValues) {
						out.write(region.toBedString() + StringUtils.RETURN);
					}
				}
			}
		} else {
			//save coverage report
			final QCoverageStats stats = new QCoverageStats();
			for (final CoverageReport report : jobQueue.getCoverageReport()) {
				stats.getCoverageReport().add(report);
			}
			if (options.hasVcfFlag() && options.hasPerFeatureOption()) {
				writeVCFReport(stats);
			}

			if (options.hasXmlFlag()) {
				writeXMLCoverageReport(stats);
			}

			if (options.hasTxtFlag()) {
				if( options.hasPerFeatureOption()) writePerFeatureTabDelimitedCoverageReport(stats);
				else writePerTypeTabDelimitedCoverageReport(stats);
			}
		}
		



		
	}

	private static Options moptions = null;
	private static int exitStatus = 1; // Defaults to FAILURE
	private static boolean performLogging = false; // Defaults to false
	private static QLogger mlogger = null;
	
	
	public static void main(final String[] args) throws Exception {
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
				new Coverage(moptions);
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
		return null == e.getMessage() ? "Unknown error" : e.getMessage();
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
		return Coverage.class.getPackage().getImplementationTitle();
	}

	static String getProgramVersion() {
		return Coverage.class.getPackage().getImplementationVersion();
	}

}
