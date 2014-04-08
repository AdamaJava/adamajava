/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.commandline.BlockingExecutor;
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
	
	private ArrayList<String> commands;

	public BLAT(String server, String port, String path) {
		this.commands = new ArrayList<String>();
		commands.add(path + QSVParameters.FILE_SEPERATOR +  "gfClient");
	    commands.add(server);
	    commands.add(port);
	    commands.add("/");
	    commands.add("-nohead");
	}
	
	public ArrayList<String> getCommands() {
		return commands;
	}

	public void setCommands(ArrayList<String> commands) {
		this.commands = commands;
	}

	/**
	 * Align the provided fasta file path with blat and write to the given blat output file path 
	 * @param fastaFile
	 * @param blatOutputFile
	 * @return
	 * @throws Exception
	 */
	public Map<String, BLATRecord> align(String fastaFile, String blatOutputFile) throws Exception {
		 //run blat
		 execute(fastaFile, blatOutputFile);

		 //parse the output
         return parseResults(blatOutputFile);		
	}

	/**
	 * Parse blat results
	 * @param blatOutputFile
	 * @return map with blat records
	 * @throws Exception
	 */
	public Map<String, BLATRecord> parseResults(String blatOutputFile) throws Exception {
		Map<String, BLATRecord> records = new HashMap<String, BLATRecord>();
		TabbedFileReader reader = new TabbedFileReader(new File(blatOutputFile));
		for (TabbedRecord tab: reader) {
			BLATRecord record = new BLATRecord(tab.getData().split("\t")); 
			if (record.isValid()) {
					if (records.containsKey(record.getName())) {
						BLATRecord previous = records.get(record.getName());
						if (record.getScore() > previous.getScore()) {
							records.put(record.getName(), record);
						}
					} else {
						records.put(record.getName(), record);
					}								
			}
		}
		
		reader.close();
		
		return records;
	}

	/**
	 * execute BLAT
	 * @param fastaFile
	 * @param blatOutputFile
	 * @throws QSVException
	 */
	public void execute(String fastaFile, String blatOutputFile) throws QSVException {
		List<String> coms = new ArrayList<String>();
		coms.addAll(commands);
		coms.add(fastaFile);
		coms.add(blatOutputFile);
		String cmd = "";
		for (String c: coms) {
			cmd += c + " ";
		}
		try {
			BlockingExecutor blex = new BlockingExecutor(cmd);
			
			if (blex.isFailure()) {
				String message = "Failed blat execution: "
						+ blex.getErrorStreamConsumer().toString();
				
				for (String line : blex.getErrorStreamConsumer().getLines()) {
					message += line;
				}
				throw new QSVException("BLAT_ERROR", message);
			}
		
//			ProcessBuilder pb = new ProcessBuilder(coms);
//		    Process process = pb.start();
//	
//		     
//	        // any error message?
//	        ProcessStreamHandler error = new 
//	            ProcessStreamHandler(process.getErrorStream(), "ERROR");            
//	        
//	        // any output?
//	        ProcessStreamHandler output = new 
//	            ProcessStreamHandler(process.getInputStream(), "OUTPUT");
//	            
//	        // kick them off
//	        error.start();
//	        output.start();       
//	       
//	                                
//	        // any error???
//	        int exitVal;
//		
//			exitVal = process.waitFor();
//			if (exitVal > 0) {
//		      	  throw new QSVException("BLAT_ERROR", error.toString());
//	        }			
//			process.getInputStream().close();
//			process.getOutputStream().close();
//			process.getErrorStream().close(); 
	        
	       
		} catch (InterruptedException e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		} catch (IOException e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		} catch (Exception e) {
			 throw new QSVException("BLAT_ERROR", QSVUtil.getStrackTrace(e));
		}
        
        coms = null;
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
	 * @throws Exception
	 */
	public List<BLATRecord> alignConsensus(String softclipDir, String name, String consensus, String leftReference, String rightReference) throws Exception {
		String base = softclipDir + QSVUtil.getFileSeparator() + name;
		String fa = base + ".fa";
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fa)));		
			
		writer.write(">" +name + QSVUtil.getNewLine());
		writer.write(consensus + QSVUtil.getNewLine());		
		writer.close();
		
		String outFile = base + ".psl";
		
		execute(fa, outFile);
		
		List<BLATRecord> records = new ArrayList<BLATRecord>();
		
		TabbedFileReader reader = new TabbedFileReader(new File(outFile));

		for (TabbedRecord tab: reader) {
			BLATRecord record = new BLATRecord(tab.getData().split("\t")); 
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
		
		reader.close();
		new File(fa).delete();
		new File(outFile).delete();
		
		Collections.sort(records);		
		
		return records;		
	}

	public List<BLATRecord> getBlatResults(String blatFile, String leftReference, String rightReference, String name) throws Exception {
		String outFile = blatFile.replace(".fa", ".psl");
		
		List<BLATRecord> records = new ArrayList<BLATRecord>();
		
		TabbedFileReader reader = new TabbedFileReader(new File(outFile));

		for (TabbedRecord tab: reader) {
			BLATRecord record = new BLATRecord(tab.getData().split("\t")); 	
			
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
		
		reader.close();
		Collections.sort(records);	
		return records;
	}
}
