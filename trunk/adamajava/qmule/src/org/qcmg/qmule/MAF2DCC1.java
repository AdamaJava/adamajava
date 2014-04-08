/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedFileWriter;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MAF2DCC1 {
	
	private String logFile;	
	private File mafFile;
	private List<File> dccFiles = new ArrayList<File>();
	private File outputDccFile;
	private static QLogger logger;
	private Map<ChrPosition, TabbedRecord> mafRecords = new HashMap<ChrPosition, TabbedRecord>();
	private int inputMafRecordCount;
	private int[] mafColumnIndexes;
	private int[] dccColumnIndexes;
	private String mode;
	

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public File getMafFile() {
		return mafFile;
	}

	public void setMafFile(File mafFile) {
		this.mafFile = mafFile;
	}


	public File getOutputDccFile() {
		return outputDccFile;
	}

	public void setOutputDccFile(File outputDccFile) {
		this.outputDccFile = outputDccFile;
	}

	public static QLogger getLogger() {
		return logger;
	}

	public static void setLogger(QLogger logger) {
		MAF2DCC1.logger = logger;
	}

	public Map<ChrPosition, TabbedRecord> getMafRecords() {
		return mafRecords;
	}

	public void setMafRecords(Map<ChrPosition, TabbedRecord> mafRecords) {
		this.mafRecords = mafRecords;
	}

	public int[] getMafColumnIndexes() {
		return mafColumnIndexes;
	}

	public void setMafColumnIndexes(int[] mafColumnIndexes) {
		this.mafColumnIndexes = mafColumnIndexes;
	}

	public int[] getDccColumnIndexes() {
		return dccColumnIndexes;
	}

	public void setDccColumnIndexes(int[] dccColumnIndexes) {
		this.dccColumnIndexes = dccColumnIndexes;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public int getInputMafRecordCount() {
		return inputMafRecordCount;
	}

	public void setInputMafRecordCount(int inputMafRecordCount) {
		this.inputMafRecordCount = inputMafRecordCount;
	}
	
	public List<File> getDccFiles() {
		return dccFiles;
	}

	public void setDccFiles(List<File> dccFiles) {
		this.dccFiles = dccFiles;
	}
	
	public void setup(String args[]) throws Exception{
		
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(MAF2DCC1.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MAF2DCC1", MAF2DCC1.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			String[] cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 2) {
				throw new QMuleException("INSUFFICIENT_INPUT_FILES");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {						
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			mafFile = new File(cmdLineInputFiles[0]);
			
			for (int i=1; i<cmdLineInputFiles.length; i++) {
				dccFiles.add(new File(cmdLineInputFiles[i]));
			}			
			
			String[] cmdLineOutputFiles = options.getOutputFileNames();
			
			outputDccFile = new File(cmdLineOutputFiles[0]);
			
			if (cmdLineOutputFiles.length != 1) {
				throw new QMuleException("TOO_MANY_OUTPUTFILE");
			}
			if ( ! FileUtils.canFileBeWrittenTo(cmdLineOutputFiles[0])) {
				throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", cmdLineOutputFiles[0]);
			}			
			for (String file :  cmdLineOutputFiles) {
				if (new File(file).exists()) {
					throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", file);
				}
			}				
			
			mode = options.getMode();
			if (mode == null || (!mode.equals("snp") && !mode.equals("indel"))) {
				throw new QMuleException("MODE_ERROR", mode);
			}
					
			logger.info("Input MAF file: " + mafFile.getAbsolutePath());
			
			for (File f: dccFiles) {
				logger.info("Input DCC file: " + f.getAbsolutePath());
			}
			logger.info("Output DCC file: " + outputDccFile.getAbsolutePath());
			logger.info("Mode: " + mode);
			
		}
	}

	public int annotate() throws Exception {
		readMafFile();
		
		int countInMaf = 0;
		
		TabbedFileWriter writer = new TabbedFileWriter(outputDccFile);
		for (int i=0; i<dccFiles.size(); i++) {
			countInMaf += compare(dccFiles.get(i), i+1, writer);
		}
		writer.close();
		
		//check to see if there are any which do not have a match
		if (mafRecords.size() > 0) {
			logger.warn("Could not find matches for the following records: ");
			for (Entry<ChrPosition, TabbedRecord> entry: mafRecords.entrySet()) {
				logger.info("Missing at positions: " + entry.getKey().toString());
			}
				throw new QMuleException("MISSING_DCC_RECORDS", Integer.toString(mafRecords.size()));
		}
		
		if (countInMaf != inputMafRecordCount || mafRecords.size() > 0) {
				throw new QMuleException("COUNT_ERROR", Integer.toString(countInMaf), Integer.toString(inputMafRecordCount));
		}
		
		logger.info("Added " + countInMaf + " records to the dcc1 output file");
		
		return 0;
	}

	private void readMafFile() throws Exception {
		TabbedFileReader reader = new TabbedFileReader(mafFile);	
		try {
			int count = 0;
			for (TabbedRecord rec : reader) {
				count++;
				//header
				if (rec.getData().startsWith("Hugo")) {
					mafColumnIndexes = findColumnIndexesFromHeader(rec);
				} else {					
					if (missingColumnIndex(mafColumnIndexes)) {
						throw new QMuleException("NO_COLUMN_INDEX", mafFile.getAbsolutePath());
					}
					addToMafRecordMap(rec, count);
					inputMafRecordCount++;
				}				
			}
			
			logger.info("Number of input maf records: " + inputMafRecordCount);
			
		} finally {
			reader.close();
		}
	}
	
	private int compare(File dccFile, int count, TabbedFileWriter writer) throws Exception {
		logger.info("Looking in dcc file: " + dccFile.getAbsolutePath());
		TabbedFileReader reader = new TabbedFileReader(dccFile);		
		if (count == 1) {
			TabbedHeader header = reader.getHeader();		
			writer.addHeader(header);
		}
		int countInMaf = 0;
		try {
			
			int total = 0;
			for (TabbedRecord rec : reader) {
				//header
				
				if (rec.getData().startsWith("analysis_id")) {
					//mutation id column
					dccColumnIndexes = findColumnIndexesFromHeader(rec);
					if (count == 1) {					
						writer.add(rec);
					}
				} else {
					total++;
					if (total % 10000 == 0) {
						logger.info("Processed: " + total + " dcc records" );
					}
					if (missingColumnIndex(mafColumnIndexes)) {
						throw new QMuleException("NO_MUTATION_ID", dccFile.getAbsolutePath());
					}
					String[] strArray = rec.getDataArray();
					String chr = strArray[dccColumnIndexes[0]].replace("chr", "");
					if (chr.equals("M")) {
						chr += "T";
					}
					ChrPosition chrPos = new ChrPosition(chr, new Integer(strArray[dccColumnIndexes[1]]), new Integer(strArray[dccColumnIndexes[2]]));
					if (recordInMaf(chrPos, rec)) {
						writer.add(rec);
						countInMaf++;
					}
				}
			}			
			
		} finally {
			reader.close();
		}
		logger.info("Finished looking in dcc file: " + dccFile.getAbsolutePath() + " found " + countInMaf + " maf record/s." );
		return countInMaf;
	}

	public void addToMafRecordMap(TabbedRecord rec, int count) throws QMuleException {
		String[] strArray = rec.getDataArray();
		
		//need to screw around with chr1 vs 1 vs chrMT vs chrM 
		String chr = strArray[mafColumnIndexes[0]].replace("chr", "");

		if (chr.equals("M")) {
			chr += "T";
		}
		ChrPosition chrPos = new ChrPosition(chr, new Integer(strArray[mafColumnIndexes[1]]), new Integer(strArray[mafColumnIndexes[2]]), Integer.toString(count));
		
		mafRecords.put(chrPos, rec);		
	}

	public boolean missingColumnIndex(int[] columnIndexes) throws QMuleException {
		for (int i =0; i< columnIndexes.length; i++) {
			if (columnIndexes[i] == -1) {
				throw new QMuleException("NO_COLUMN_INDEX");
			}
		}
		return false;
	}

	public int[] findColumnIndexesFromHeader(TabbedRecord rec) {
		int[] mutationColumns = {-1, -1, -1, -1, -1, -1};
		String[] strArray = rec.getDataArray();
		for (int i=0; i<strArray.length; i++) {
			String name = strArray[i];
			if (name.equalsIgnoreCase("chromosome")) {
				mutationColumns[0] = i;
			} else if (name.equalsIgnoreCase("Start_Position") || name.equalsIgnoreCase("chromosome_start")) {
				mutationColumns[1] = i;
			} else if (name.equalsIgnoreCase("End_Position") || name.equalsIgnoreCase("chromosome_end")) {
				mutationColumns[2] = i;
			} else if (name.equalsIgnoreCase("Variant_Type") || name.equalsIgnoreCase("mutation_type")) {
				mutationColumns[3] = i;
			} else if (name.equalsIgnoreCase("Reference_Allele") || name.equalsIgnoreCase("reference_genome_allele")) {
				mutationColumns[4] = i;
			} else if (name.equalsIgnoreCase("Tumor_Seq_Allele1") || name.equalsIgnoreCase("tumour_genotype")) {
				mutationColumns[5] = i;
			}
			
		}
		return mutationColumns;
	}

	public boolean recordInMaf(ChrPosition dccChrPos, TabbedRecord dccRec) throws QMuleException {	
		int matches = 0;
		Iterator<Entry<ChrPosition, TabbedRecord>> iterator = mafRecords.entrySet().iterator();
		boolean matchFound = false;
		while (iterator.hasNext()) {
			Entry<ChrPosition, TabbedRecord> mafEntry = iterator.next();
			if (match(mafEntry.getKey(), dccChrPos) && matchOtherColumns(mafEntry.getValue(), dccRec)) {
				matches++;
				if (matches > 1) {
					throw new QMuleException("T0O_MANY_MATCHES", dccChrPos.toString());
				}				
				iterator.remove();
				matchFound = true;
			}
		}
		
		return matchFound;
	}
	
	public boolean matchOtherColumns(TabbedRecord mafRec, TabbedRecord dccRec) {
		String[] mafValues = mafRec.getDataArray();
		String[] dccValues = dccRec.getDataArray();

		if (mode.equals("snp")) {
			if (matchingMutation(mafValues[mafColumnIndexes[3]], dccValues[dccColumnIndexes[3]])) {
				return true;
			}
		}
		if (mode.equals("indel")) {
			if (matchingMutation(mafValues[mafColumnIndexes[3]], dccValues[dccColumnIndexes[3]]) &&
					mafValues[mafColumnIndexes[4]].equals(dccValues[dccColumnIndexes[4]]) && 
					mafValues[mafColumnIndexes[5]].equals(dccValues[dccColumnIndexes[5]])) {
				return true;
			}
		}
		
				
		return false;	
	}

	public boolean matchingMutation(String mafMutation, String dccMutation) {
		if ((mafMutation.equals("SNP") && dccMutation.equals("1")) ||
		(mafMutation.equals("INS") && dccMutation.equals("2")) ||
		(mafMutation.equals("DEL") && dccMutation.equals("3"))) {
			return true;
		}
		return false;
	}

	public boolean match(ChrPosition mafChrPos, ChrPosition dccChrPos) {
		if (mafChrPos.getChromosome().equals(dccChrPos.getChromosome()) 
				&& mafChrPos.getPosition() == dccChrPos.getPosition() 
				&& mafChrPos.getEndPosition() == dccChrPos.getEndPosition()) {
			return true;
		}
		return false;
	}


	public static void main(String[] args) throws Exception {
		MAF2DCC1 sp = new MAF2DCC1();
		LoadReferencedClasses.loadClasses(MAF2DCC1.class);
		sp.setup(args);
		
		int exitStatus = sp.annotate();
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}

}
