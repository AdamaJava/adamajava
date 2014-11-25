/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;
import java.io.IOException;
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
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

public class BuildCommonSnpsVcf {
	private static QLogger logger;
	public static String version;
	
	public static final String DONOR_EQUALS = VcfHeaderUtils.INFO_DONOR + "=";
	
	// INPUTS
	private static String logFile;
	private String searchDirectory;
	private String outputVcfFile;
	private String searchString;
	private String [] additionalSearchStrings;
	private String dbSnpFile;
	
	private int exitStatus;
	
	private Map<ChrPosition, VCFRecord> snpPositions;
	private final Map<File, Integer> mapOfFilesAndIds = new HashMap<File, Integer>();
	
	private int engage() throws Exception {
		
		// get list of dcc1 files to process
		File[] dcc1Files = FileUtils.findFilesEndingWithFilter(searchDirectory, searchString, true);
		
		Arrays.sort(dcc1Files);
		
		logger.info("UNFILTERED LIST");
		for (File f : dcc1Files) logger.info(f.getAbsolutePath());
		logger.info("UNFILTERED LIST - END");
		
		if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
			int i = 1;
			// additional filtering of files
			for (File f : dcc1Files) {
				boolean passesFilter = true;
				for (String filter : additionalSearchStrings) {
					if ( ! f.getAbsolutePath().contains(filter)) {
						passesFilter = false;
						break;
					}
				}
				if (passesFilter) mapOfFilesAndIds.put(f, i++);
			}
			dcc1Files = mapOfFilesAndIds.keySet().toArray(dcc1Files);
		} else {
			int i = 1;
			// need to populate the map
			for (File f : dcc1Files) mapOfFilesAndIds.put(f, i++);
		}
		
		logger.info("Will create an output file based on the contents of " + dcc1Files.length + " files matching " + searchString);
		
		if ( ! mapOfFilesAndIds.isEmpty()) {
			
			// create map
			snpPositions = new HashMap<ChrPosition, VCFRecord>(1024 * 1024 * 4);
			
			List<File> files = new ArrayList<File>(mapOfFilesAndIds.keySet());
			Collections.sort(files);
			
			for (File f : files) {
				// quick check to see if file still exists before proceeding
				if (f.exists() && f.canRead()) {
					
					// use appropriate method depending on file suffix
					if (f.getName().endsWith(".dcc1")) {
						processDccFile(f, mapOfFilesAndIds.get(f));
					} else if (f.getName().endsWith("maf")) {
						processMafFile(f, mapOfFilesAndIds.get(f));
					} else {
						logger.info("Cant handle this type of file: " + f.getName());
					}
					
					logger.info("added : " + f.getAbsolutePath() + ", map size: " + snpPositions.size());
				} else {
					logger.info("File was removed before it could be processed: " + f.getAbsolutePath());
				}
			}
			
			// add in dbSnp data
			logger.info("adding dbSnp data");
			addDbSnpData(dbSnpFile);
			
			// write output
			writeVCF(outputVcfFile);
		}
		
		return exitStatus;
	}
	
	void addDbSnpData(String fileName) throws Exception {
		
		int count = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(fileName));) {
			for (VCFRecord dbSNPVcf : reader) {
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " dbsnp records");
				
				// only proceed if we have a SNP variant record
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false)) continue;
				
				ChrPosition cp = new ChrPosition(dbSNPVcf.getChromosome(), dbSNPVcf.getPosition());
				VCFRecord commonSnpVcf = snpPositions.get(cp);
				if (null == commonSnpVcf) continue;
				
				// add the snp id to the the common vcf record
				if (null == commonSnpVcf.getId()) {
					commonSnpVcf.setId(dbSNPVcf.getId());
				} else {
//					logger.info("multiple dbSnp positions exist at : " + cp.toIGVString());
					commonSnpVcf.setId(commonSnpVcf.getId() + ";" + dbSNPVcf.getId());
				}
				
				//TODO should we check that the alt in dbSnp is the same as what the commonSnpVcf has?
				
			}
		}
	}
	
	void writeVCF(String outputFileName) throws IOException {
		if (StringUtils.isNullOrEmpty(outputFileName)) {
			logger.warn("No vcf output file scpecified so can't output vcf");
			return;
		}
		logger.info("Writing VCF output");
		
		String header = VcfUtils.getHeaderForCommonSnps(searchString, searchDirectory, additionalSearchStrings, mapOfFilesAndIds);
		
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(snpPositions.keySet());
		Collections.sort(orderedList);
		
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName));) {
			writer.addHeader(header);
			for (ChrPosition position : orderedList) {
				writer.add(snpPositions.get(position));
			}
		}
	}
	
	private void processDccFile(File f, Integer id) throws Exception {
		// read in data from file.
		try (TabbedFileReader reader = new TabbedFileReader(f);) {
			int i = 0;
			for (TabbedRecord rec : reader) {
				// ignore header line
				if (i++ == 0) continue;
				
				String [] params = TabTokenizer.tokenize(rec.getData());
				ChrPosition cp = new ChrPosition(params[4], Integer.parseInt(params[5]));
				String ref = params[10];
				String alt = getAltFromMutation(params, 13);		// can eventually change this to the last element in the file
				
				// check to see if this appears in the map already - if so, update patient (if not already there)
				if (snpPositions.containsKey(cp)) {
					VCFRecord vcfRec = snpPositions.get(cp);
					String existingDonors = vcfRec.getInfo();
					
					String existingRef = vcfRec.getRef();
					if ( ! ref.equals(existingRef)) {
						logger.warn("different references found at position : " + cp.toIGVString());
					}
					
					if ( ! StringUtils.isNullOrEmpty(alt) && ! ".".equals(alt)) {
						
						// check to see if existing alt is set to "." - if so replace (assuming its not being replaced by a dot).
						if (".".equals(vcfRec.getAlt())) {
							vcfRec.setAlt("");	// delete
						}
						
						for (char c : alt.toCharArray()) {
							if (vcfRec.getAlt().indexOf(c) == -1) {
								vcfRec.setAlt(vcfRec.getAlt().trim().length() > 0 ? (vcfRec.getAlt() + "," + c) : ""+c);
							}
						}
					}
					
	//				if ( ! existingDonors.contains(donor)) {
						vcfRec.setInfo(existingDonors + ";" + id);
	//				}	// else do nothing
				} else {
					// add
					snpPositions.put(cp, VcfUtils.createVcfRecord(cp, id.toString(), ref, alt));
				}
				
			}
		}
	}
	
	private void processMafFile(File f, Integer id) throws Exception {
		// read in data from file.
		try (TabbedFileReader reader = new TabbedFileReader(f);) {
			int i = 0;
			for (TabbedRecord rec : reader) {
				// ignore header line
				if (i++ == 0) continue;
				
				String [] params = TabTokenizer.tokenize(rec.getData());
				ChrPosition cp = new ChrPosition(params[4], Integer.parseInt(params[5]));
				String ref = params[10];
				final char alt1 = params[11].charAt(0);
				final char alt2 = params[12].charAt(0);
				String alt = ".";
				if (alt1 == ref.charAt(0)) alt = "" + alt2;
				else if (alt2 == ref.charAt(0)) alt = "" + alt1;
				else alt = alt1 + "," + alt2;
				
				// check to see if this appears in the map already - if so, update patient (if not already there)
				if (snpPositions.containsKey(cp)) {
					VCFRecord vcfRec = snpPositions.get(cp);
					String existingDonors = vcfRec.getInfo();
					
					String existingRef = vcfRec.getRef();
					if (ref != existingRef) {
						logger.warn("different references found at position : " + cp.toIGVString());
					}
					
					if ( ! StringUtils.isNullOrEmpty(alt) && ! ".".equals(alt)) {
						
						// check to see if existing alt is set to "." - if so replace (assuming its not being replaced by a dot).
						if (".".equals(vcfRec.getAlt())) {
							vcfRec.setAlt("");	// delete
						}
						
						for (char c : alt.toCharArray()) {
							if (vcfRec.getAlt().indexOf(c) == -1) {
								vcfRec.setAlt(vcfRec.getAlt().trim().length() > 0 ? (vcfRec.getAlt() + "," + c) : ""+c);
							}
						}
					}
					
					//				if ( ! existingDonors.contains(donor)) {
					vcfRec.setInfo(existingDonors + ";" + id);
					//				}	// else do nothing
				} else {
					// add
					snpPositions.put(cp, VcfUtils.createVcfRecord(cp, id.toString(), ref, alt));
				}
				
			}
		}
	}

	private String getAltFromMutation(String [] params, int position) {
		String mutation = params[position];
		String alt = ".";
		if ( ! StringUtils.isNullOrEmpty(mutation)) {
			if (mutation.length() == 3) {
				alt = ""+mutation.charAt(2);
			} else if (mutation.length() == 5){
				alt = mutation.charAt(2) + "," +mutation.charAt(4);
			} else if (mutation.length() == 7){
				alt = mutation.charAt(4) + "," +mutation.charAt(6);
			} else if ("-999".equals(mutation) && position != params.length - 1){
					return getAltFromMutation(params, params.length - 1);
			} else {
				logger.warn("need to deal with mutation: " + mutation);
			}
		}
		return alt;
	}
	
	String getDonorNameFromFilename(File f) {
		// big assumption here that the patient name comes directly before the search string
		String donorName = f.getName().substring(0, f.getName().indexOf(searchString));
		if (donorName.endsWith(".")) {
			donorName = donorName.substring(0, donorName.length() - 1);
		}
		return donorName;
	}
	
	/**
	 * Performs a single merge based on the supplied arguments. Errors will
	 * terminate the merge and display error and usage messages.
	 * 
	 * @param args
	 *            the command-line arguments.
	 * @throws Exception 
	 */
	public static void main(final String[] args) throws Exception {
		
		LoadReferencedClasses.loadClasses(BuildCommonSnpsVcf.class);
		
		BuildCommonSnpsVcf main = new BuildCommonSnpsVcf();
		int exitStatus = main.setup( args );
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
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
		} else {
			options.detectBadOptions();
			
			// configure logging
			logFile = options.getLogFile();
			version = BuildCommonSnpsVcf.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(BuildCommonSnpsVcf.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("qsnp", version, args);
			
			// get search directory 
			if (options.getInputFileNames().length == 1) {
				searchDirectory = options.getInputFileNames()[0];
			} else {
				throw new SnpException("MISSING_INI_FILE");
			}
			
			// get output vcf file
			if (options.hasOutputOption()) {
				outputVcfFile =  options.getOutput();
			} else {
				throw new SnpException("MISSING_INI_FILE");
			}
			
			// get search string
			if (options.hasSearchSuffixOption()) {
				searchString =  options.getSearchSuffix();
			} else {
				throw new SnpException("MISSING_INI_FILE");
			}
			
			// get dbSnp file
			if (options.hasDbSnpOption()) {
				dbSnpFile =  options.getDbSnp();
			} else {
				throw new SnpException("MISSING_INI_FILE");
			}
			
			// get additional search strings
			if (options.hasAdditionalSearchStringOption()) {
				additionalSearchStrings = options.getAdditionalSearchString();
			}
			
		}
		return engage();
	}
	
}
