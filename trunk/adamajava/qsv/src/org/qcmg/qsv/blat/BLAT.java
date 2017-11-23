/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.commandline.BlockingExecutor;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

/**
 * Class to launch BLAT
 *
 */
public class BLAT {
	private static final QLogger logger = QLoggerFactory.getLogger(BLAT.class);
	
	private final ArrayList<String> commands;
	private String cmdString = null;
	private int executeCount = 0;

	public BLAT(String server, String port, String path) {
		this.commands = new ArrayList<>(6);
		commands.add(path + QSVParameters.FILE_SEPERATOR +  "gfClient");
	    commands.add(server);
	    commands.add(port);
	    commands.add("/");
	    commands.add("-nohead");
	}
	
	public ArrayList<String> getCommands() {
		return commands;
	}
	
	private String getCommandsAsString() {
		if (null == cmdString) {
			StringBuilder sb = new StringBuilder();
			for (String c : commands) {
				sb.append(c).append(" ");
			}
			cmdString = sb.toString();
		} 
		return cmdString;
	}

	/**
	 * Align the provided fasta file path with blat and write to the given blat output file path 
	 * @param fastaFile
	 * @param blatOutputFile
	 * @return
	 * @throws QSVException 
	 * @throws IOException 
	 */
	public Map<String, BLATRecord> align(String fastaFile, String blatOutputFile) throws QSVException, IOException  {
		 //run blat
		 execute(fastaFile, blatOutputFile);

		 //parse the output
         return parseResults(blatOutputFile);		
	}

	/**
	 * Parse blat results
	 * @param blatOutputFile
	 * @return map with blat records
	 * @throws IOException 
	 */
	public Map<String, BLATRecord> parseResults(String blatOutputFile) throws IOException {
		Map<String, BLATRecord> records = new HashMap<>();
		File blatOutput = new File(blatOutputFile);
		
		try (TabbedFileReader reader = new TabbedFileReader(blatOutput);) {
			for (TabbedRecord tab: reader) {
				BLATRecord record = new BLATRecord(TabTokenizer.tokenize(tab.getData())); 
				if (record.isValid()) {
					BLATRecord previous = records.get(record.getName());
					if (null == previous || record.getScore() > previous.getScore()) {
						records.put(record.getName(), record);
					}
				}
			}
		}
		
		return records;
	}

	/**
	 * execute BLAT
	 * @param fastaFile
	 * @param blatOutputFile
	 * @throws QSVException
	 */
	public void execute(String fastaFile, String blatOutputFile) throws QSVException {
		executeCount++;
		StringBuilder sb = new StringBuilder(getCommandsAsString());
		sb.append(fastaFile).append(" ");
		sb.append(blatOutputFile);
		
		try {
			BlockingExecutor blex = new BlockingExecutor(sb.toString());
			
			if (blex.isFailure()) {
				
				logger.error("Error calling BLAT with command: " + sb.toString());
				logger.error("Contents of " + fastaFile + ": ");
				Files.lines(Paths.get(fastaFile))
					.forEach(line -> logger.error(line));
				
				String message = "Failed blat execution: "
						+ blex.getErrorStreamConsumer().toString();
				
				for (String line : blex.getErrorStreamConsumer().getLines()) {
					message += line;
				}
				throw new QSVException("BLAT_ERROR", message);
			}
		} catch (InterruptedException e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		} catch (IOException e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		} catch (Exception e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		}
	}

	/**
	 * Run blat for the given consensus sequence and make
	 * sure the results
	 * @param softclipDir
	 * @param name
	 * @param consensus
	 * @param leftReference
	 * @param rightReference
	 * @return
	 * @throws QSVException 
	 * @throws Exception
	 */
	public List<BLATRecord> alignConsensus(String softclipDir, String name, String consensus, String leftReference, String rightReference) throws IOException, QSVException {
		String base = softclipDir + QSVUtil.getFileSeparator() + name;
		File faFile = new File(base + ".fa");
		
		try (FileWriter fw = new FileWriter(faFile);
				BufferedWriter writer = new BufferedWriter(fw);) {
			
			writer.write(">" +name + QSVUtil.getNewLine());
			writer.write(consensus + QSVUtil.getNewLine());		
		}
		
		String outFile = base + ".psl";
		
		execute(faFile.getAbsolutePath(), outFile);
		
		// delete generated files
		Files.deleteIfExists(faFile.toPath());
		
		List<BLATRecord> records = new ArrayList<>();
		
		File out = new File(outFile);
		try (TabbedFileReader reader = new TabbedFileReader(out);) {

			for (TabbedRecord tab: reader) {
				BLATRecord record = new BLATRecord(TabTokenizer.tokenize(tab.getData()));
				if (record.isValid()) {
					if (leftReference != null && rightReference != null) {
						if (record.getReference().equals(leftReference) || record.getReference().equals(rightReference)) {
							records.add(record);				
						}
					} else {
						records.add(record);
					}
				}
			}
		}
		
		// delete generated files
		Files.deleteIfExists(out.toPath());
		
		/*
		 * java 8 List sort should be better than Collections.sort
		 */
		records.sort(null);
		return records;		
	}

	public List<BLATRecord> getBlatResults(String blatFile, String leftReference, String rightReference, String name) throws IOException {
		File outFile = new File(blatFile.replace(".fa", ".psl"));
		
		List<BLATRecord> records = new ArrayList<>();
		
		try (TabbedFileReader reader = new TabbedFileReader(outFile);) {

			for (TabbedRecord tab: reader) {
				BLATRecord record = new BLATRecord(TabTokenizer.tokenize(tab.getData())); 	
				
				if (record.isValid() && record.getName().equals(name)) {
					if (leftReference != null && rightReference != null) {
						if (record.getReference().equals(leftReference) || record.getReference().equals(rightReference)) {
							records.add(record);				
						}
					} else {
						records.add(record);
					}
				}
			}
		}
		
		/*
		 * java 8 List sort should be better than Collections.sort
		 */
		records.sort(null);
		return records;
	}

	public int getExecuteCount() {
		return executeCount;
	}
}
