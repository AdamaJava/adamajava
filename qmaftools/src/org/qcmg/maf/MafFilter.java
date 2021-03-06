/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.qio.record.StringFileReader;
 
public class MafFilter {
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	private static QLogger logger;
	private String logFile;
	
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private String[] lowCoveragePatients;
	private int exitStatus;
	
	public final FilenameFilter mafFilenameFilter = new FilenameFilter(){
		@Override
		public boolean accept(File file, String name) {
			return name.endsWith(".maf") 
			&& ! cmdLineOutputFiles[0].endsWith(name) 
			&& ! cmdLineOutputFiles[1].endsWith(name);
		}
	};
	
	List<String> highConfidenceMafs = new ArrayList<String>(); 
	List<String> probableNoiseMafs = new ArrayList<String>(); 
	
	public int engage() throws Exception {
		// load mapping files
		logger.info("loading maf files from directory: " + cmdLineInputFiles[0]);
		loadMafFiles(cmdLineInputFiles[0]);
		logger.info("loading maf files from directory: " + cmdLineInputFiles[0] + " - DONE");
		
		logger.info("loading KRAS maf file: " + cmdLineInputFiles[1]);
		loadKRASFile(cmdLineInputFiles[1]);
		logger.info("loading KRAS maf file: " + cmdLineInputFiles[1] + " - DONE");
		
		logger.info("write output");
		writeMafOutput(cmdLineOutputFiles[0], highConfidenceMafs, MafUtils.HEADER_WITH_CONFIDENCE);
		writeMafOutput(cmdLineOutputFiles[1], probableNoiseMafs, MafUtils.HEADER);
		
		return exitStatus;
	}
	
	private void loadMafFiles(String directory) throws Exception {
		// get a list of all the maf files in the supplied directory
		File dir = new File(directory);
		if (! dir.isDirectory()) throw new IllegalArgumentException("Supplied directory is not a directory: " + directory);
		
		File[] mafFiles = dir.listFiles(mafFilenameFilter);
		
		for (File f : mafFiles) {
			logger.info("will operate on file: " + f.getAbsolutePath());
			loadFile(f);
		}
	}
	
	private void loadKRASFile(String krasFile) throws Exception {
		
		try (StringFileReader reader = new StringFileReader(new File(krasFile));) {
			int high = 0, noise = 0, fail = 0, count = 0;
			
			for (String rec : reader) {
				count++;
				String[] params = tabbedPattern.split(rec, -1);
				String chr = params[4];
				String position = params[5];
				String id = params[15];
				String verification = params[24];
				
				// if maf position verifies, put it straight away into high conf file
				if ("Valid".equals(verification)) {
					// should all be valid
					high++;
					
					// skip entries from low coverage patients
					// APGI_2270, APGI_2271, APGI_2285
					boolean lowCov = false;
					if (null != lowCoveragePatients) {
						for (String lowCovPatient : lowCoveragePatients) {
						// APGI_2270, APGI_2271, APGI_2285
							if (id.contains(lowCovPatient)) {
								lowCov = true;
								logger.info("Skipping KRAS record: " + rec + " - belongs to low coverage patient");
								break;
							}
						}
					}
					if (lowCov) continue;
					
					// check that we are not adding a duplicate into the highConfMaf list
					boolean recordAlreadyInList = false;
					for(int i = 0; i < highConfidenceMafs.size(); i++) {
						String tr = highConfidenceMafs.get(i);
						String [] p2 = tabbedPattern.split(tr, -1);
						String chr2 = p2[4];
						String position2 = p2[5];
						String id2 = p2[15];
						String verification2 = p2[24];
						
						if (chr.equals(chr2) && position.equals(position2) && id.equals(id2)) {
							recordAlreadyInList = true;
							if (verification.equals(verification2)) {
								logger.info("verification matches!");
							} else {
								logger.info("verification DOES NOT match! - updating");
								
								// update record with "Valid" validation status
								highConfidenceMafs.set(i, tr.replaceAll("Unknown", "Valid"));
							}
							break;
						}
					}
					
					if ( ! recordAlreadyInList) {
						
						boolean recordAlreadyInLowerConfList = false;
						// if record exists in low confidence file, remove, and put into high
						for (final String tr : probableNoiseMafs) {
							String [] p2 = tabbedPattern.split(tr, -1);
							String chr2 = p2[4];
							String position2 = p2[5];
							String id2 = p2[15];
							
							if (chr.equals(chr2) && position.equals(position2) && id.equals(id2)) {
								// remove from list
								recordAlreadyInLowerConfList = true;
								logger.info("moving record from low conf to high conf, and updating verification status to Valid: " 
										+ probableNoiseMafs.remove(tr));
								
								highConfidenceMafs.add(tr.replaceAll("Unknown", "Valid"));
								
								break;
							}
						}
						
						if ( ! recordAlreadyInLowerConfList) {
							// count no of fields in rec - beef up to the current number
							int diff = MafUtils.HEADER_WITH_CONFIDENCE_COLUMN_COUNT - tabbedPattern.split(rec, -1).length;
							
							for (int i = 0 ; i < diff ; i++) addColumn(rec, null);
							
							highConfidenceMafs.add(rec);
							logger.info("adding kras to high conf file");
						}
					}
				} else {
					logger.info("KRAS data that did not verify");
				}
			}
			logger.info("for file: " + krasFile + " stats (total, high, noise, fail): " + count + "," + high + "," + noise + "," + fail);
			
		} 
	}
	
	private void loadFile(File file) throws Exception {
		StringFileReader reader = new StringFileReader(file);
		try {
			
			int high = 0, noise = 0, fail = 0, count = 0;
			
			for (String rec : reader) {
				if (count++ == 0 && rec.startsWith("Hugo_Symbol")) continue;
				
				String[] params = tabbedPattern.split(rec, -1);
				String flag = params[32];
				String type = params[9];		//eg. SNP, INS or DEL
				String td = params[34];		//eg. A:5[40],3[35],T:1[25],19[35.43]
				String dbSNP = params[13];
				String ref = params[10];
				String tumour1 = params[11];
				String tumour2 = params[12];
				String variant = ref.equals(tumour1) ? tumour2 : tumour1;
				String consequence = params[8];
				String verification = params[24];
								
				// if maf position verifies, put it straight away into high conf file
				if ("Valid".equals(verification)) {
					high++;
					String confidence = "other";
					if (DccConsequence.passesMafNameFilter(consequence)) {
						if (passesHighConfidenceFilter(flag, type, td, dbSNP, variant)) {
							confidence = "high";
						} else if (passesProbableNoiseFilter(flag, type, td, dbSNP, variant)) {
							confidence = "low";
						}
					}
					logger.info("setting confidence to be: " + confidence);
					highConfidenceMafs.add(addColumn(rec, confidence));
					
				} else if (DccConsequence.passesMafNameFilter(consequence)) {
					if (passesHighConfidenceFilter(flag, type, td, dbSNP, variant)) {
						high++;
						highConfidenceMafs.add(addColumn(rec, null));
					} else if (passesProbableNoiseFilter(flag, type, td, dbSNP, variant)) {
						noise++;
						probableNoiseMafs.add(addColumn(rec, null));
					} else fail++;
				} else fail++;
				
			}
			logger.info("for file: " + file.getAbsoluteFile() + " stats (total, high, noise, fail): " + count + "," + high + "," + noise + "," + fail);
			
		} finally {
			reader.close();
		}
	}
	
	private String addColumn(String tabbedRec, String data) {
		return tabbedRec + "\t" + data;
	}
	
	private void writeMafOutput(String fileName, List<String> mafs, String header) throws IOException {
		if (mafs.isEmpty()) return;
				
		try (FileWriter writer = new FileWriter(new File(fileName), false);) {
			writer.write(header);
			for (String record : mafs) {
				writer.write(record + "\n");
			}
		} 
	}
	
	protected static boolean passesHighConfidenceFilter(String flag, String type, String td, String dbSNP, String variant) {
		MutationType mt = MutationType.getMutationType(type);
		// must be class A 
		return SnpUtils.isClassA(flag) 
		&& ((MutationType.isIndel(mt) && "novel".equals(dbSNP))
				|| (MutationType.isSubstitution(mt) && passesCountCheck(td, 5, variant)));
	}
	protected static boolean passesProbableNoiseFilter(String flag, String type, String td, String dbSNP, String variant) {
		MutationType mt = MutationType.getMutationType(type);
		// must be class A or B 
		return SnpUtils.isClassAorB(flag)
		&& (MutationType.isIndel(mt) || MutationType.isSubstitution(mt) && passesCountCheck(td, 4, variant));
	}
	
	protected static boolean passesCountCheck(String td, int count, String variant) {
		if (null != variant) {
			List<PileupElement> pileups = PileupElementUtil.createPileupElementsFromString(td);
			char var = variant.charAt(0);
			for (PileupElement pe : pileups) {
				if (pe.getBase() == var && pe.getTotalCount() >= count) return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		MafFilter sp = new MafFilter();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running MafFilter:", e);
			else System.err.println("Exception caught whilst running MafFilter");
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafFilter.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafFilter", MafFilter.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMafException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMafException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			lowCoveragePatients = options.getLowCoveragePatients();
			logger.tool("Will handle the following low coverage patients: " + Arrays.deepToString(lowCoveragePatients));
						
			return engage();
		}
		return returnStatus;
	}
}
