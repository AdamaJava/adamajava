/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.common.model.MafType;

final class Options {
	private static final String INPUT_DESCRIPTION = Messages
			.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages
			.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String HELP_DESCRIPTION = Messages
			.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages
			.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String DIR_OUTPUT_DESCRIPTION = Messages
			.getMessage("DIR_OUTPUT_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputFileNames;
	private final String[] outputFileNames;
	private final String[] dirNames;
	private int minCoverage;
	private final String log;
	private final String logLevel;
	private final String entrez;
	private final String canonicalTranscripts;
	private final String verified;
	private final String dbSNP;
	private final String kras;
	private final String fasta;
	private final String gff;
	private final String[] lowCoveragePatients;
	private final String[] patients;
	private final String[] dccs;
	private final String[] bams;
	private int noOfBases;
	private final String chainFile;
	private final String mafMode;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
		parser.acceptsAll(asList("i", "input"), INPUT_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("infile");
		parser.accepts("output", OUTPUT_DESCRIPTION)
		.withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.acceptsAll(asList("d", "dir", "directory"),
				DIR_OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("outdir");
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(
				String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class);
		parser.accepts("minCoverage", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("minCoverage");
		parser.accepts("entrez", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("entrez");
		parser.accepts("canonicalTranscripts", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("canonicalTranscripts");
		parser.accepts("verified", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("verified");
		parser.accepts("dbSNP", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("dbSNP");
		parser.accepts("kras", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("kras");
		parser.accepts("fasta", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("fasta");
		parser.accepts("gff", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("gff");
		parser.accepts("chain", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("chain");
		parser.accepts("lowCoverage", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("lowCoverage");
		parser.accepts("patients", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("patients");
		parser.accepts("dcc", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("patient DCC");
		parser.accepts("bam", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("patient Bam");
		parser.accepts("noOfBases", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class).describedAs("noOfBases");
		parser.accepts("verifiedInvalid", INPUT_DESCRIPTION);
		parser.accepts("mafMode", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("mafMode");
		parser.accepts("alleleFraction", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("alleleFraction");
		parser.accepts("homopolymerCutoff", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("homopolymerCutoff");
		parser.accepts("snv", INPUT_DESCRIPTION);
		parser.accepts("indel", INPUT_DESCRIPTION);
		parser.accepts("somatic", INPUT_DESCRIPTION);
		parser.accepts("germline", INPUT_DESCRIPTION);
		parser.accepts("ensembl61", HELP_DESCRIPTION);
		parser.accepts("cosmic", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("cosmic");
		parser.accepts("donor", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("donor");
		options = parser.parse(args);

		List inputList = options.valuesOf("i");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);
		
		
		List outputList = options.valuesOf("output");
		outputFileNames = new String[outputList.size()];
		outputList.toArray(outputFileNames);

		List dirList = options.valuesOf("d");
		dirNames = new String[dirList.size()];
		dirList.toArray(dirNames);

		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		entrez = (String) options.valueOf("entrez");
		canonicalTranscripts = (String) options.valueOf("canonicalTranscripts");
		verified = (String) options.valueOf("verified");
		dbSNP = (String) options.valueOf("dbSNP");
		kras = (String) options.valueOf("kras");
		fasta = (String) options.valueOf("fasta");
		gff = (String) options.valueOf("gff");
		chainFile = (String) options.valueOf("chain");
		
		List lowCovList = options.valuesOf("lowCoverage");
		lowCoveragePatients = new String[lowCovList.size()];
		lowCovList.toArray(lowCoveragePatients);
		
		List patientsList = options.valuesOf("patients");
		patients = new String[patientsList.size()];
		patientsList.toArray(patients);
		
		List dccList = options.valuesOf("dcc");
		dccs = new String[dccList.size()];
		dccList.toArray(dccs);
		
		List bamList = options.valuesOf("bam");
		bams = new String[bamList.size()];
		bamList.toArray(bams);
		
		mafMode = (String) options.valueOf("mafMode");
		
		// qsignature
		if (null != options.valueOf("minCoverage"))
			minCoverage = (Integer) options.valueOf("minCoverage");
		if (null != options.valueOf("noOfBases"))
			noOfBases = (Integer) options.valueOf("noOfBases");
	}

	String getLog() {
		return log;
	}

	String getLogLevel() {
		return logLevel;
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasNumberedOption() {
		return options.has("n") || options.has("num")
				|| options.has("numbered");
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}
	
	boolean hasEnsembl61Option() {
		return options.has("ensembl61");
	}
	

//	boolean hasSamOption() {
//		return options.has("s") || options.has("sam");
//	}
//
//	boolean hasBamOption() {
//		return options.has("b") || options.has("bam");
//	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	String[] getInputFileNames() {
		return inputFileNames;
	}
	String[] getOutputFileNames() {
		return outputFileNames;
	}

	String[] getDirNames() {
		return this.dirNames;
	}
	String[] getLowCoveragePatients() {
		return this.lowCoveragePatients;
	}
	String[] getPatients() {
		return this.patients;
	}
	String[] getDccs() {
		return this.dccs;
	}
	String[] getBams() {
		return this.bams;
	}
	public int getMinCoverage() {
		return minCoverage;
	}
	public String getEntrezFile() {
		return entrez;
	}
	public String getDbSNPFile() {
		return dbSNP;
	}
	public String getKrasFile() {
		return kras;
	}
	public String getFastaFile() {
		return fasta;
	}
	public String getGffFile() {
		return gff;
	}
	public String getMafMode() {
		return mafMode;
	}
	void displayHelp() throws Exception {
		parser.printHelpOn(System.out);
	}

	void detectBadOptions() throws Exception {
		if (1 > inputFileNames.length) {
			throw new Exception("One input file must be specified");
		} else if (1 < inputFileNames.length) {
			throw new Exception("Only one input file permitted");
		} else if (1 > dirNames.length) {
			throw new Exception("One output directory must be specified");
		} else if (1 < dirNames.length) {
			throw new Exception("Only one output directory permitted");
		}
		if (!hasLogOption()) {
			throw new Exception("A log filename must be specified (using the --log option)");
		}
//		if (hasBamOption() && hasSamOption()) {
//			throw new Exception("Only one output file type can be specified");
//		}
	}

	public String getCanonicalTranscripts() {
		return canonicalTranscripts;
	}

	public String getVerified() {
		return verified;
	}
	
	public int getNoOfBases() {
		return noOfBases;
	}

	public String getChainFile() {
		return chainFile;
	}
	
	public boolean getIncludeInvalid() {
		return options.has("verifiedInvalid");
	}

	public boolean containsAlleleFraction() {
		return options.has("alleleFraction");
	}
	public int getAlleleFraction() {
		return (Integer) options.valueOf("alleleFraction");
	}
	
	/**
	 * defaults to SNV_SOMATIC if no options are supplied
	 * @return
	 */
	public MafType getMafType() {
		if (options.has("indel")) {
			if (options.has("germline")) {
				return MafType.INDEL_GERMLINE;
			} else {
				return MafType.INDEL_SOMATIC;
			}
		} else {
			if (options.has("germline")) {
				return MafType.SNV_GERMLINE;
			} else {
				return MafType.SNV_SOMATIC;
			}
		}
	}

	public String getCosmicFile() {
		return (String) options.valueOf("cosmic");
	}
	public String getDonor() {
		return (String) options.valueOf("donor");
	}
	
	public Integer getHomopolymerCutoff() {
		return (Integer) options.valueOf("homopolymnerCutoff");
	}
}
