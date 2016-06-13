/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.picard.QJumper;

public class GetBamRecords {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	
	private String position;
	
	private int exitStatus;
	private static QLogger logger;
	
	
	public int engage() throws Exception {
		
		logger.info("Setting up the QJumper");
		QJumper jumper = new QJumper();
		jumper.setupReader(cmdLineInputFiles[0]);
		
		String contig = position.substring(0, position.indexOf(":")); 
		int start = Integer.parseInt(position.substring(position.indexOf(":")+1)); 
		
		logger.info("config: " + contig);
		logger.info("start: " + start);
		
		List<SAMRecord> records = jumper.getOverlappingRecordsAtPosition(contig, start, start);
		
		logger.info("unfiltered read count: " + records.size()+ "");
		
		int filteredCount = 0, readsWithBaseAtPosition = 0, duplicateCount = 0, properlyPaired = 0,properlyPairedAll = 0, pairedAll = 0, paired = 0, notPrimaryAlignment = 0, unmapped = 0;
		String qualityString = "", qualityPhredString = "";
		String baseString = "";
		int unmappedSecondaryDuplicates = 0, unmappedSecondaryDuplicatesProperly = 0;
		
		char[] novelStartBases = new char[1024];	// hmmmmm
		Set<Integer> forwardStrand = new HashSet<Integer>();
		Set<Integer> reverseStrand = new HashSet<Integer>();
		int j = 0;
		
		for (SAMRecord rec : records) {
			int readPosition = org.qcmg.picard.util.SAMUtils.getIndexInReadFromPosition(rec, start);
			if (readPosition >= 0 && readPosition < rec.getReadLength()) {
				char c = rec.getReadString().charAt(readPosition);
				if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
					readsWithBaseAtPosition++;
					if (rec.getDuplicateReadFlag())  {
						duplicateCount++;
					} else {
						byte [] baseQuals = SAMUtils.fastqToPhred(rec.getBaseQualityString());
						qualityPhredString +=baseQuals[readPosition] + ",";
						baseString += (rec.getReadNegativeStrandFlag() ? Character.toLowerCase(c) : c) + "";
//						baseString += c + ",";
						qualityString +=rec.getBaseQualityString().charAt(readPosition) + "";
						
						if (rec.getMappingQuality() >= 10 && rec.getBaseQualities()[readPosition] >= 10) {
							if (rec.getReadNegativeStrandFlag()) {
								if (reverseStrand.add(rec.getAlignmentStart())) {
									novelStartBases[j++] = c;
								}
							} else {
								if (forwardStrand.add(rec.getAlignmentStart())) {
									novelStartBases[j++] = c;
								}
							}
						}
					}
				}
				
				if (rec.getReadPairedFlag()) {
					paired++;
					if ( rec.getProperPairFlag()) properlyPaired++;
					
				}
				if (rec.getReadUnmappedFlag()) unmapped++;
				if (rec.getReadUnmappedFlag()) unmapped++;
				if (rec.getNotPrimaryAlignmentFlag()) notPrimaryAlignment++;
				
				
				if ( ! rec.getDuplicateReadFlag() && ! rec.getNotPrimaryAlignmentFlag() && ! rec.getReadUnmappedFlag())
					unmappedSecondaryDuplicates++;
				if ( ! rec.getDuplicateReadFlag() && ! rec.getNotPrimaryAlignmentFlag() && ! rec.getReadUnmappedFlag() 
						&& (rec.getReadPairedFlag() ? rec.getProperPairFlag() : true))
//					&& (rec.getReadPairedFlag() && rec.getProperPairFlag()))
					unmappedSecondaryDuplicatesProperly++;
			}
			
			if (rec.getReadPairedFlag()) {
				pairedAll++;
				if (rec.getProperPairFlag()) properlyPairedAll++;
			}
			
			if (BAMPileupUtil.eligibleSamRecord(rec)) {
				++filteredCount;
				logger.info("***" + rec.getSAMString());
			} else logger.info(rec.getSAMString());
			
		}
		
		
		logger.info("SUMMARY: ");
		logger.info("Total no of records: " + records.size() );
		logger.info("No of records with a base at position: " + readsWithBaseAtPosition);
		logger.info("No of duplicate records (that have a base at position): " + duplicateCount);
		logger.info("No of unique records (that have a base at position): " + (readsWithBaseAtPosition-duplicateCount));
		logger.info("No of unique paired records (that have a base at position): " + paired);
		logger.info("No of unique properly paired records (that have a base at position): " + properlyPaired);
		logger.info("No of records not primary aligned (that have a base at position): " + notPrimaryAlignment);
		logger.info("No of records not mapped (that have a base at position): " + unmapped);
		logger.info("unmappedSecondaryDuplicates (that have a base at position): " + unmappedSecondaryDuplicates);
		logger.info("unmappedSecondaryDuplicatesProperly (that have a base at position): " + unmappedSecondaryDuplicatesProperly);
		logger.info("No of paired records (all): " + pairedAll);
		logger.info("No of properly paired records (all): " + properlyPairedAll);
		logger.info("Unique record bases: " + baseString.substring(0,baseString.length() > 0 ? baseString.length() : 0));
		logger.info("Unique record base qualities: " + qualityString.substring(0,qualityString.length() > 0 ? qualityString.length() : 0));
		logger.info("Unique record base qualities (phred): " + qualityPhredString.substring(0,qualityPhredString.length() > 0 ? qualityPhredString.length() : 0));
		logger.info("filtered read count: " + filteredCount + " out of " + records.size() );
		logger.info("Novel start bases: " + new String(novelStartBases));
		
		jumper.closeReader();
		
		return exitStatus;
	}
	
	
	
	public static void main(String[] args) throws Exception {
		GetBamRecords sp = new GetBamRecords();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = -1;
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
			logger = QLoggerFactory.getLogger(GetBamRecords.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("GetBamRecords", GetBamRecords.class.getPackage().getImplementationVersion(), args);
			
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
			
			position = options.getPosition();
			
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
