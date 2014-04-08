/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.sf.samtools.SAMFileHeader;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.QLimsMetaFactory;

public class IndelDCCHeader {
	
	private String logFile;	
	private File somaticOutputFile;
	private File germlineOutputFile;
	private String mode;
	private File normalBam;
	private File tumourBam;
	private String uuid;
	private boolean qexecPresent  = false;
	private ArrayList<String> qexec = new ArrayList<String>();
	private boolean completeHeaderPresent = false;
	private File somaticFile;
	private File germlineFile;
	private String tumourSampleId;
	private String normalSampleId;
	private static QLogger logger;
	
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
			logger = QLoggerFactory.getLogger(IndelDCCHeader.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("IndelDCCHeader", IndelDCCHeader.class.getPackage().getImplementationVersion(), args);
			
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
			
			somaticFile = new File(cmdLineInputFiles[0]);
			germlineFile = new File(cmdLineInputFiles[1]);
			tumourBam = new File(options.getTumour());			
			normalBam = new File(options.getNormal());
			
			if ( ! FileUtils.canFileBeRead(tumourBam)) {
				throw new QMuleException("INPUT_FILE_READ_ERROR" , tumourBam.getAbsolutePath());
			}
			if ( ! FileUtils.canFileBeRead(normalBam)) {
				throw new QMuleException("INPUT_FILE_READ_ERROR" , tumourBam.getAbsolutePath());
			}
			
			String[] cmdLineOutputFiles = options.getOutputFileNames();
			
			somaticOutputFile = new File(cmdLineOutputFiles[0]);
			germlineOutputFile = new File(cmdLineOutputFiles[1]);	
			
			if (cmdLineOutputFiles.length != 2) {
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
			
			if (mode == null || (!mode.equals("pindel") && !mode.equals("gatk"))) {
				throw new QMuleException("MODE_ERROR", mode);
			}
			
			logger.info("Somatic input DCC: " + somaticFile.getAbsolutePath());
			logger.info("Germline input DCC: " + germlineFile.getAbsolutePath());
			logger.info("Output DCC: " + somaticOutputFile.getAbsolutePath());
			logger.info("Output DCC: " + germlineOutputFile.getAbsolutePath());
			logger.info("Tumour bam: " + tumourBam.getAbsolutePath());
			logger.info("Normal bam: " + normalBam.getAbsolutePath());
			logger.info("Mode: " + mode);	
			
		}
	}

	public int annotate() throws Exception {
		//double check to make sure that uuid isn't already present
		checkForUUid();		
		
		StringBuilder header = new StringBuilder();
		if (completeHeaderPresent) {
			logger.info("UUid already present in header. No annotation is taking place");			
		} else if (qexecPresent){
			StringBuilder sb = new StringBuilder();
			for (String s: qexec) {
				sb.append(s + "\n");
			}
			header.append(sb.toString());
			header.append(getDCCMeta());
			QLimsMeta tumour = QLimsMetaFactory.getLimsMeta("TEST", tumourBam.getAbsolutePath());
			tumourSampleId = tumour.getSample();
			header.append(tumour.getLimsMetaDataToString());
			QLimsMeta normal = QLimsMetaFactory.getLimsMeta("CONTROL", normalBam.getAbsolutePath());
			normalSampleId = normal.getSample();
			header.append(normal.getLimsMetaDataToString());
			//write somatic
			writeOutputFile(header.toString(), somaticFile, somaticOutputFile, false);
			//write germline
			writeOutputFile(header.toString(), germlineFile, germlineOutputFile, true);
		} 	
		
		return 0;
	}

	public File getSomaticOutputFile() {
		return somaticOutputFile;
	}

	public void setSomaticOutputFile(File somaticOutputFile) {
		this.somaticOutputFile = somaticOutputFile;
	}

	public File getGermlineOutputFile() {
		return germlineOutputFile;
	}

	public void setGermlineOutputFile(File germlineOutputFile) {
		this.germlineOutputFile = germlineOutputFile;
	}

	public File getSomaticFile() {
		return somaticFile;
	}

	public void setSomaticFile(File somaticFile) {
		this.somaticFile = somaticFile;
	}

	public File getGermlineFile() {
		return germlineFile;
	}

	public void setGermlineFile(File germlineFile) {
		this.germlineFile = germlineFile;
	}

	public boolean isQexecPresent() {
		return qexecPresent;
	}

	public void setQexecPresent(boolean qexecPresent) {
		this.qexecPresent = qexecPresent;
	}

	public ArrayList<String> getQexec() {
		return qexec;
	}

	public void setQexec(ArrayList<String> qexec) {
		this.qexec = qexec;
	}

	public boolean isCompleteHeaderPresent() {
		return completeHeaderPresent;
	}

	public void setCompleteHeaderPresent(boolean completeHeaderPresent) {
		this.completeHeaderPresent = completeHeaderPresent;
	}

	public void checkForUUid() throws IOException, QMuleException {
		BufferedReader reader = new BufferedReader(new FileReader(somaticFile));
		
		String line;
		boolean ddcMeta = false;
		boolean uuidHere = false;
		boolean uuidInResults = false;
		qexec = new ArrayList<String>();
		while((line = reader.readLine()) != null) {			
			if (line.startsWith("#") || line.startsWith("analysis")) {
				if (line.contains("Uuid") || line.contains("uuid")) {					
					uuidHere = true;					
				}
				if (line.startsWith("#Q_EXEC")) {
					qexec.add(line);
				}
				if (line.startsWith("#Q_DCCMETA")) {
					ddcMeta = true;					
				}
			} else {
				String[] values = line.split("\t");
				if (isCorrectUuidFormat(values[0])) {
					uuidInResults = true;					
				}
			}
		}	
		reader.close();
		if (ddcMeta && uuidHere && uuidInResults) {
			logger.info("Complete header already present.");
			completeHeaderPresent = true;
		} else if (uuidHere && qexec.size() == 14) {
			qexecPresent = true;
			logger.info("QExec header and uuid present.");
			String q = "";
			for (String s: qexec) {
				if (s.contains("Uuid")) {
					q = s.replace("-", "_");					
					String potentialUuid = s.split("\t")[2].replace("-", "_");						
					if (isCorrectUuidFormat(potentialUuid)) {
						uuid = potentialUuid;						
					} else {
						logger.info("UUid was not correct format: " + potentialUuid);
						throw new QMuleException("UUID_ERROR");
					}
				}
			}
			qexec.remove(0);
			qexec.add(0, q);
		} else {
			logger.info("Could not determine if UUid and DCC header is present");
			throw new QMuleException("UUID_ERROR");
		}				
	}

	public boolean isCorrectUuidFormat(String potentialUuid) {			
		if (potentialUuid.length() == 36 && potentialUuid.split("_").length == 5) {
			return true;
		}
		return false;
	}

	public String getDCCMeta() throws Exception {		
		SAMFileHeader tHeader = SAMFileReaderFactory.createSAMFileReader(tumourBam).getFileHeader();
		SAMFileHeader nHeader = SAMFileReaderFactory.createSAMFileReader(normalBam).getFileHeader();		
		QDccMeta meta;
		
		meta = QDccMetaFactory.getDccMeta(uuid, nHeader, tHeader, mode);		
		return meta.getDCCMetaDataToString();			
	}

	public void writeOutputFile(String header, File inputFile, File outputFile, boolean isGermline) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		
		if (!completeHeaderPresent) {
			writer.write(header);
		}
		
		String line;
		while((line = reader.readLine()) != null) {
			if (!line.startsWith("#") && !line.startsWith("analysis") && !completeHeaderPresent) {
				writer.write(replaceIdsInLine(line, isGermline) + "\n");
			} else {				
				if (qexecPresent && !line.startsWith("#Q_EXEC")) {
					writer.write(line + "\n");
				}				
			}
		}		
		reader.close();
		writer.close();
	}

	public String getTumourSampleId() {
		return tumourSampleId;
	}

	public void setTumourSampleId(String tumourSampleId) {
		this.tumourSampleId = tumourSampleId;
	}

	public String getNormalSampleId() {
		return normalSampleId;
	}

	public void setNormalSampleId(String normalSampleId) {
		this.normalSampleId = normalSampleId;
	}

	public String replaceIdsInLine(String line, boolean isGermline) {
		String[] values = line.split("\t");
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i< values.length; i++) {
			if (i==0 && !completeHeaderPresent) {
				sb.append(uuid + "\t");
			} else if (i==1 && !completeHeaderPresent){
				if (isGermline) {
					sb.append(normalSampleId + "\t");
				} else {
					sb.append(tumourSampleId + "\t");
				}
			} else if (i==2 && !completeHeaderPresent) {
				String[] mutationStrs = values[i].split("_");
				String count = "_" + mutationStrs[mutationStrs.length-1];
				if (isGermline) {
					sb.append(uuid + "_" + normalSampleId + count + "\t");
				} else {
					sb.append(uuid + "_"+ tumourSampleId + count + "\t");
				}			
			} else {
				sb.append(values[i] + "\t");
			}
		}
		return sb.toString();
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public File getNormalBam() {
		return normalBam;
	}

	public void setNormalBam(File normalBam) {
		this.normalBam = normalBam;
	}

	public File getTumourBam() {
		return tumourBam;
	}

	public void setTumourBam(File tumourBam) {
		this.tumourBam = tumourBam;
	}

	public static void main(String[] args) throws Exception {
		IndelDCCHeader sp = new IndelDCCHeader();
		LoadReferencedClasses.loadClasses(IndelDCCHeader.class);
		sp.setup(args);
		int exitStatus = sp.annotate();
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}

}
