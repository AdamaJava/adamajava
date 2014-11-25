/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;

public class ReAnnotateDccWithDbSNP {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	private String header;
	
	private static QLogger logger;
	
	private Map<ChrPosition, String[]> dccs = new HashMap<ChrPosition, String[]>();
	
	
	public int engage() throws Exception {
		
		loadDccFile();
		
		updateDBSnpData();
		
		writeDCCOutput();
		
		
		return exitStatus;
	}
	
	private void writeDCCOutput() throws Exception {
		if ( ! StringUtils.isNullOrEmpty(cmdLineOutputFiles[0])) {
			FileWriter writer = new FileWriter(new File(cmdLineOutputFiles[0]));
			try {
				//sort 
				List<ChrPosition> data = new ArrayList<ChrPosition>(dccs.keySet());
				Collections.sort(data);
				
				
				writer.write(header + "\tdbSnpVer\n");
				
				for (ChrPosition cp : data) {
					String[] dcc = dccs.get(cp);
					StringBuilder sb = new StringBuilder();
					for (String s : dcc) {
						if (sb.length() > 0) sb.append('\t');
						sb.append(s);
					}
					writer.write(sb.toString() + '\n');
				}
				
			} finally {
				writer.close();
			}
		}
	}
	
	
	private void loadDccFile() throws Exception {
		logger.info("Attempting to load dcc data");
		TabbedFileReader reader = new TabbedFileReader(new File(cmdLineInputFiles[0]));
		int count = 0;
		try {
			for (TabbedRecord rec : reader) {
				if (++count == 1) {		// header line
					header = rec.getData();
					continue;
				}
				String[] params = TabTokenizer.tokenize(rec.getData());
				ChrPosition cp = new ChrPosition(params[4], Integer.parseInt(params[5]));
				
				// reset dbsnpid
				params[20] = null;
//				StringBuilder sb = new StringBuilder();
//				for (String s : params) {
//					if (sb.length() > 0) sb.append('\t');
//					sb.append(s);
//				}
//				rec.setData(sb.toString());
				dccs.put(cp, params);
			}
		} finally {
			reader.close();
		}
		logger.info("Attempting to load dcc data - DONE with " + dccs.size() + " entries");
	}
	
	private void updateDBSnpData() throws Exception {
		
		VCFFileReader reader = new VCFFileReader(new File(cmdLineInputFiles[1]));
		
		int count = 0, multipleVersions = 0;
		int pre30 = 0, thirty = 0, thirtyOne = 0, thirtyTwo = 0, thirtyThree = 0, thirtyFour = 0, thirtyFive = 0;
		try {
			for (VCFRecord dbSNPVcf : reader) {
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " dbsnp records");
				
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false)) continue;
				// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
				String[] params = dccs.get(new ChrPosition(dbSNPVcf.getChromosome(), dbSNPVcf.getPosition()));
				if (null == params) continue;
				
				// if no dbsnp data - continue
				String previousDBSnpValue = params[20];
				if ( ! StringUtils.isNullOrEmpty(previousDBSnpValue))  {
					multipleVersions++;
					continue;
				}
				
//				logger.info("Resetting previousDBSnpValue of: " + previousDBSnpValue + " to " + dbSNPVcf.getId());
				
				// only proceed if we have a SNP variant record
				int startIndex = dbSNPVcf.getInfo().indexOf("dbSNPBuildID=") + 13;
				int endIndex = dbSNPVcf.getInfo().indexOf(";" , startIndex);
				String dbSnpVersion = dbSNPVcf.getInfo().substring(startIndex, endIndex);
//				logger.info("dbsnp version = " + dbSnpVersion);
				
				int dbSnpVersionInt = Integer.parseInt(dbSnpVersion);
				if (dbSnpVersionInt < 130) pre30++;
				else if (dbSnpVersionInt == 130) thirty++;
				else if (dbSnpVersionInt == 131) thirtyOne++;
				else if (dbSnpVersionInt == 132) thirtyTwo++;
				else if (dbSnpVersionInt == 133) thirtyThree++;
				else if (dbSnpVersionInt == 134) thirtyFour++;
				else if (dbSnpVersionInt == 135) thirtyFive++;
				else if (dbSnpVersionInt > 135) logger.info("hmmm: " + dbSnpVersionInt);
				
				params[20] = dbSNPVcf.getId();
				params = Arrays.copyOf(params, params.length + 1);
				params[params.length -1] = dbSnpVersion;
				dccs.put(new ChrPosition(dbSNPVcf.getChromosome(), dbSNPVcf.getPosition()), params);
				

//				GenotypeEnum tumour = snpRecord.getTumourGenotype();
//				//TODO should we continue if the tumour Genotype is null??
//				if (null == tumour) continue;
//				
//				// multiple dbSNP entries can exist for a position.
//				// if we already have dbSNP info for this snp, check to see if the dbSNP alt is shorter than the existing dbSNP record
//				// if so, proceed, and re-write dbSNP details (if applicable).
//				int dbSNPAltLengh = dbSNPVcf.getAlt().length(); 
//				if (snpRecord.getDbSnpAltLength() > 0 && dbSNPAltLengh > snpRecord.getDbSnpAltLength()) {
//					continue;
//				}
//				
//				// deal with multiple alt bases
//				String [] alts = null;
//				if (dbSNPAltLengh == 1) {
//					alts = new String[] {dbSNPVcf.getAlt()};
//				} else if (dbSNPAltLengh > 1){
//					alts = TabTokenizer.tokenize(dbSNPVcf.getAlt(), ',');
//				}
//				
//				if (null != alts) {
//					for (String alt : alts) {
//						
//						GenotypeEnum dbSnpGenotype = BaseUtils.getGenotypeEnum(dbSNPVcf.getRef() +  alt);
//						if (null == dbSnpGenotype) {
//							logger.warn("Couldn't get Genotype from dbSNP position with variant: " + alt);
//							continue;
//						}
////				// no longer flip the genotype as dbSNP is reporting on the +ve strand
//////				if (reverseStrand) {
//////					dbSnpGenotype = dbSnpGenotype.getComplement();
//////				}
//						if (tumour == dbSnpGenotype || (tumour.isHomozygous() && dbSnpGenotype.containsAllele(tumour.getFirstAllele()))) {
//							boolean reverseStrand = StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "RV", false);
////							boolean reverseStrand = VcfUtils.isDbSNPVcfRecordOnReverseStrand(dbSNPVcf.getInfo());
//							snpRecord.setDbSnpStrand(reverseStrand ? '-' : '+');
//							snpRecord.setDbSnpId(dbSNPVcf.getId());
//							snpRecord.setDbSnpGenotype(dbSnpGenotype);
//							snpRecord.setDbSnpAltLength(dbSNPAltLengh);
//							break;
//						}
//					}
//				}
			}
		} finally {
			reader.close();
		}
		logger.info("STATS:");
		logger.info("No of dcc records with dbSNP version of pre 130: " + pre30);
		logger.info("No of dcc records with dbSNP version of 130: " + thirty);
		logger.info("No of dcc records with dbSNP version of 131: " + thirtyOne);
		logger.info("No of dcc records with dbSNP version of 132: " + thirtyTwo);
		logger.info("No of dcc records with dbSNP version of 133: " + thirtyThree);
		logger.info("No of dcc records with dbSNP version of 134: " + thirtyFour);
		logger.info("No of dcc records with dbSNP version of 135: " + thirtyFive);
		logger.info("No of dcc records with duplicate dbSNP versions : " + multipleVersions);
		logger.info("Total no of  dcc records with dbSNP data : " + (pre30 + thirty + thirtyOne + thirtyTwo + thirtyThree + thirtyFour + thirtyFive));
	}
	
	public static void main(String[] args) throws Exception {
		ReAnnotateDccWithDbSNP sp = new ReAnnotateDccWithDbSNP();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
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
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(ReAnnotateDccWithDbSNP.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("ReAnnotateDccWithDbSNP", ReAnnotateDccWithDbSNP.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			return engage();
		}
		return returnStatus;
	}

}
