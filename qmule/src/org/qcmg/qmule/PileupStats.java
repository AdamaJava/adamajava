/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.picard.SAMFileReaderFactory;

public class PileupStats {
	
	private String logFile;
	private File inputFile;
	private File outputFile;
	private File bamFile;
	private static QLogger logger;
	
	public int engage() throws Exception {		

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		
		writer.write(getHeader());
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\t");			
			
			String result = pileup(values[0], new Integer(values[1]), new Integer(values[2]));
			
			writer.write(line + "\t" + result + "\n");
			//System.out.println(line + "\t " + result);
			if (count++ % 1000 == 0) {
				logger.info("Number processed: " + count);
			}
		}
		logger.info("Total processed: " + count);
		reader.close();		
		writer.close();
		
		return 0;
	}
	
	private String getHeader() {
		return "chr\tposition\tposition\tbed\tbed\tbed\ttotal reads\ttotal unmapped" +
				"\ttotal mates unmapped\ttotal indels\ttotal mismatch reads\ttotal soft clips" +
				"\ttotal hard clips\ttotal spliced reads\ttotal duplicates\tmismatch counts\tsplice lengths\n";
	}

	private String pileup(String chromosome, int start, int end) {
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
		
		SAMRecordIterator iterator = reader.queryOverlapping(chromosome, start, end);
		
		int totalReads = 0;
		int totalMatesUnmapped = 0;
		int totalUnmapped = 0;
		int totalDuplicates = 0;
		int totalMismatches = 0;
		int totalSpliced = 0;
		int totalSoftClips = 0;
		int totalHardClips = 0;
		int totalIndels = 0;
		TreeMap<Integer, Integer> spliceMap = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> mismatchMap = new TreeMap<Integer, Integer>();
		
		while (iterator.hasNext()) {
			SAMRecord record = iterator.next();
			if (record.getReadUnmappedFlag()) {
				totalUnmapped++;
			} else {
				totalReads++;
				if (record.getDuplicateReadFlag()) {
					totalDuplicates++;
				} else {
					
					if (record.getMateUnmappedFlag()) {
						totalMatesUnmapped++;
					}
					
					//cigars
					Cigar cigar = record.getCigar();
					
					for (CigarElement ce : cigar.getCigarElements()) {
						if (ce.getOperator().equals(CigarOperator.DELETION) || ce.getOperator().equals(CigarOperator.INSERTION)) {
							totalIndels++;
						}
						
						if (ce.getOperator().equals(CigarOperator.SOFT_CLIP)) {
							totalSoftClips++;
						}
						if (ce.getOperator().equals(CigarOperator.HARD_CLIP)) {
							totalHardClips++;
						}
						if (ce.getOperator().equals(CigarOperator.N)) {
							totalSpliced++;
							Integer length = new Integer(ce.getLength());
							int count = 1;							
							if (spliceMap.containsKey(length)) {
								count += spliceMap.get(length);								
							} 
							spliceMap.put(length, count);
						}
					}
					
					//MD tag
					String mdData = (String) record.getAttribute("MD");
					int matches = tallyMDMismatches(mdData);
					if (matches > 0) {
						totalMismatches++;						
					}
					int count = 1;							
					if (mismatchMap.containsKey(matches)) {
						count += mismatchMap.get(matches);								
					} 
					mismatchMap.put(matches, count);					
					
				}
			}
			
		}
		
		iterator.close();
		reader.close();
		
		String spliceCounts = getMapString(spliceMap);	
		String mismatchCounts = getMapString(mismatchMap);		
		
		String result = totalReads + "\t" + totalUnmapped + "\t" + totalMatesUnmapped + "\t" + totalIndels + "\t"
		+ totalMismatches + "\t" + totalSoftClips + "\t" + totalHardClips + "\t" + totalSpliced + "\t" + totalDuplicates 
		 + "\t" + mismatchCounts + "\t" + spliceCounts; 
		return result;
	}
	
    private String getMapString(TreeMap<Integer, Integer> map) {
    	StringBuilder sb = new StringBuilder();
    	
    	for (Entry<Integer, Integer> entry: map.entrySet()) {
    		sb.append(entry.getKey() + ":" + entry.getValue() + ";");
    	}
    	
    	return sb.toString();
	}

	public int tallyMDMismatches(String mdData) {
    	int count = 0;
		if (null != mdData) {
			for (int i = 0, size = mdData.length() ; i < size ; ) {
				char c = mdData.charAt(i);
				if (isValidMismatch(c)) {
					count++;
					i++;
				} else if ('^' == c) {
					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
		}
		return count;
	}
    
    private boolean isValidMismatch(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
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
			logger = QLoggerFactory.getLogger(PileupStats.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("PileupStats", PileupStats.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			String[] cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 2) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}		
			String[] cmdLineOutputFiles = options.getOutputFileNames();
			if ( ! FileUtils.canFileBeWrittenTo(cmdLineOutputFiles[0])) {
				throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", cmdLineOutputFiles[0]);
			}
			
			for (String file :  cmdLineOutputFiles) {
				if (new File(file).exists() && !new File(file).isDirectory()) {
					throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", file);
				}
			}			
			
			bamFile = new File(cmdLineInputFiles[0]);
			inputFile = new File(cmdLineInputFiles[1]);
			outputFile = new File(cmdLineOutputFiles[0]);
			logger.info("Bam file: " + bamFile);
			logger.info("Input file: " + inputFile);
			logger.info("Output file: " + outputFile);
			
		}

		return returnStatus;
	}

	public static void main(String[] args) throws Exception {
		PileupStats sp = new PileupStats();
		sp.setup(args);
		int exitStatus = sp.engage();
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
}
