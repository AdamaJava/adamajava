/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.util.MafFilterUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qbamfilter.query.QueryExecutor;

public class MafPipelineSNV extends MafPipelineNew {
	
	private static final int END_OF_READ_LENGTH = 4;
	private static final int NO_OF_GOOD_READS = 5;
	
	private int engage() throws Exception {
		
		
		// need to have at least a single dcc file to work with
		if (StringUtils.isNullOrEmpty(dccqFile) || ! FileUtils.canFileBeRead(new File(dccqFile))) {
			logger.error("DCCQ file is null/empty or can't be read: " + dccqFile);
			return 1;
		}
		
		loadRequiredStaticFiles();
		
		loadDCCFiles();
		
		if (null != krasFile)
			loadKRASData();
			
			//TODO - add check to see what the maf records contain
			// will dictate what gets done downstream, and whether we can classify maf records
			
//			if ( ! containsNovelStarts()) {
//				if (null != patientBamFiles && patientBamFiles.length > 0) {
//					addNovelStartsMT(patientBamFiles[0]);
//				}
//			}
			if (mafType.isIndel()) {
//				checkNNSIndel();
			} else {
				checkAlleleFraction();
			}
			
			// remove min annotations that don't have sufficient evidence
			checkForMINAnnotation();
			
		// perform initial filter
		logger.info("about to run initial filter");
		performInitialFilter();
		logger.info("about to run initial filter - DONE");
			
			
		// post initial filter
//		logger.info("about to run end of read annotations");
//		getEndOfReadAnnotations();
//		logger.info("about to run end of read annotations - DONE");
			
		// for indels, move any records from H -> L where there is NNS
		if (mafType.isIndel()) {
			checkIndel();
		} 
			
		// re-filter
//		logger.info("about to run filter again");
//		performInitialFilter();
//		logger.info("about to run filter again - DONE");
			
		logger.info("about to get cpg info");
		addCpgAndGff();
		logger.info("about to get cpg info - DONE");
		
		if (mafType.isSomatic()) {
			logger.info("about to get cosmic info");
			loadCOSMICData();
			logger.info("about to get cosmic info - DONE");
		}
		
		// output
		writeOutput();
			
		return exitStatus;
	}
	
	private void getEndOfReadAnnotations() throws Exception {
		ReadEndCounter rec = new ReadEndCounter(new File(bamFile), mafs, "and (Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)");
		rec.run();
	}
	
	
	static class ReadEndCounter implements Runnable {
		private final File bamFile;
		private final List<MAFRecord> reMafs;
		private final QueryExecutor qbamFilter;
		
		ReadEndCounter(File bamFile, List<MAFRecord> mafs, String query) throws Exception{
			this.bamFile = bamFile;
			this.reMafs = mafs;
			qbamFilter = new QueryExecutor(query);
		}

		@Override
		public void run() {
			logger.info("thread starting");
			long start = System.nanoTime();
			long elapsedTime = 0;
			
			boolean debugEnabled = logger.isLevelEnabled(QLevel.DEBUG);
			
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
			// if we have a small no of positions, no need to cache
//			reader.enableIndexCaching(ncMafs.size() < 10);
			
			int  noOfPositionsRetrievedForPatient = 0;
			int noOf5BPAnnotationsAdded = 0;
			
			try {
				for (MAFRecord maf : reMafs) {
					
					if ( ! maf.getConfidence().isHighConfidence()) continue;
					
					noOfPositionsRetrievedForPatient++;
					String chr = MafUtils.getFullChromosome(maf);
					int positionOfInterest = maf.getStartPosition();
					SAMRecordIterator records = reader.queryOverlapping(chr, maf.getStartPosition(), maf.getStartPosition());
					
					List<SAMRecord> filteredRecords = new ArrayList<>();
					// put records through qbamfilter to remove unwanted reads
					while (records.hasNext()) {
						SAMRecord rec = records.next();
						
						if ( ! SAMUtils.isSAMRecordValidForVariantCalling(rec)) continue;
						
						boolean passesFilter = qbamFilter.Execute(rec);
						if (passesFilter) {
							filteredRecords.add(rec);
						}
					}
					
					if (filteredRecords.isEmpty()) {		// close iterator and carry on
						records.close();
						continue;
					} else if (debugEnabled){
						logger.debug("no of filteredRecords: " + filteredRecords.size());
					}
					
					int positionOfInterestANDEndOfReadLength = positionOfInterest + END_OF_READ_LENGTH;
					int positionOfInterestMINUSEndOfReadLength = positionOfInterest - END_OF_READ_LENGTH;
					
					int altsAtEndsOfRead = 0;
					int altsInMiddleOfRead = 0;
					
					int goodReadCutoff = NO_OF_GOOD_READS * 2;
					
					// get alt, and then get positions that have the alt at our POI
					 char alt = MafUtils.getVariant(maf).charAt(0);
					 boolean forwardStrand = false;
					 boolean reverseStrand = false;
					 
					 for (SAMRecord sam : filteredRecords) {
						 // get base at POI
						 int positionInRead = SAMUtils.getIndexInReadFromPosition(sam, positionOfInterest);
						 if (positionInRead > -1) {
							 char baseAtPosition = (char) sam.getReadBases()[positionInRead];
							 if (alt == baseAtPosition) {
								 if (sam.getAlignmentEnd() <= positionOfInterestANDEndOfReadLength 
										 || sam.getAlignmentStart() >= positionOfInterestMINUSEndOfReadLength) {
									 altsAtEndsOfRead++;
								 } else {
									 altsInMiddleOfRead++;
									 // break if we have more than 2x no of good reads
									 if (altsInMiddleOfRead >= goodReadCutoff && reverseStrand && forwardStrand) break;
									 
									 if (sam.getReadNegativeStrandFlag()) {
										 reverseStrand = true;
									 } else {
										 forwardStrand = true;
									 }
								 }
							 }
						 }
					 }
					
					 String stats = debugEnabled ? chr + ":" + positionOfInterest + ", altsInMiddleOfRead: " + altsInMiddleOfRead + ", altsAtEndsOfRead: " + altsAtEndsOfRead
							 + ", maf td: " + maf.getTd() + ", maf ta1: " + maf.getTumourAllele1() + ", ta2: " + maf.getTumourAllele2() + ", alt: " + alt + ", ref: " + maf.getRef() : null;
					 
					 // if we have 5 good alt reads and they are on both strands - we are fine, otherwise annotate
					 if (altsInMiddleOfRead >= NO_OF_GOOD_READS && forwardStrand &&  reverseStrand) {
						 if (debugEnabled)
							 logger.debug(stats + " - 5 or more good reads and they are on both strands - no need for further annotation");
					 } else if (altsAtEndsOfRead > 0) {
						 String annotation = SnpUtils.END_OF_READ + altsAtEndsOfRead;
						 if (debugEnabled)
							 logger.debug(stats + " - less than 5 good reads - annotating with " + annotation);
						 // add annotation
						 noOf5BPAnnotationsAdded++;
						 String existingAnnotation = maf.getFlag();
						 if (StringUtils.isNullOrEmpty(existingAnnotation) || SnpUtils.PASS.equals(existingAnnotation)) {
							 maf.setFlag(annotation);
						 } else {
							 maf.setFlag(existingAnnotation + ";" + annotation);
						 }
					 }
					
					records.close();
				}
				elapsedTime = (System.nanoTime() - start) / 1000000000;	// nanoseconds
				logger.info("bamfile: " + bamFile.getName() + ", positions queried: " + noOfPositionsRetrievedForPatient 
						+ " (" + ((double)noOfPositionsRetrievedForPatient / elapsedTime) 
						+ " per sec), noOf5BPAnnotationsAdded: " + noOf5BPAnnotationsAdded);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
//					latch.countDown();
					logger.info("thread finishing, elapsedTime: " + elapsedTime);
				}
//			}
			}
		}
	}
	
	private void performInitialFilter() {
		for (MAFRecord maf : mafs) {
			MafFilterUtils.classifyMAFRecord(maf, filterOptions);
		}
	}
	
	public static void main(String[] args) throws Exception {
		MafPipelineSNV sp = new MafPipelineSNV();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running MafPipelineSNV:", e);
			else System.err.println("Exception caught whilst running MafPipelineSNV");
			e.printStackTrace();
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
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			//give warning (at least) if these files don't exist
			
			entrezFile = options.getEntrezFile();
			canonicalTranscriptsFile = options.getCanonicalTranscripts();
			verificationFile = options.getVerified();
			dbSNPFile = options.getDbSNPFile();
			krasFile = options.getKrasFile();
			fastaFile = options.getFastaFile();
			gffFile = options.getGffFile();
			cosmicFile = options.getCosmicFile();
			
			mafType = options.getMafType();
			
			if (options.containsAlleleFraction()) {
				alleleFraction = options.getAlleleFraction();
			} else {
				// set default allele fraction based on maf type
				alleleFraction = mafType.isGermline() ? GERMLINE_ALLELE_FRACTION : SOMATIC_ALLELE_FRACTION;
			}
			
			if (null != options.getHomopolymerCutoff()) {
				filterOptions.setHomopolymerCutoff(options.getHomopolymerCutoff());
			}
			
			if (entrezFile == null || ! FileUtils.canFileBeRead(entrezFile))
				throw new QMafException("NO_ENTREZ_FILE_ERROR");
			
			if (null == options.getDirNames() || options.getDirNames().length < 1)
				throw new QMafException("MISSING_OUTPUT_DIRECTORY");
			
			outputDirectory = options.getDirNames()[0];
			lowCoveragePatients = options.getLowCoveragePatients();
			dccqFile = options.getDccs()[0];
			bamFile = options.getBams()[0];
			
			donor = options.getDonor();
			
			
			if (options.getNoOfBases() > 0)
				noOfBases = options.getNoOfBases();
			
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafPipelineSNV.class, logFile, options.getLogLevel());
			qExec = logger.logInitialExecutionStats("MafPipelineSNV", MafPipelineSNV.class.getPackage().getImplementationVersion(), args);
			
			logger.tool("will retrieve " + noOfBases + " on either side of position of interest from fasta file");
			logger.tool("dccqFile: " + dccqFile);
			logger.tool("bamFile: " + bamFile);
			if (alleleFraction > 0)	logger.tool("alleleFraction: " + alleleFraction);
			logger.tool("maf type: " + mafType);
			if (null != donor) logger.tool("donor: " + donor);
			
			return engage();
		}
		return returnStatus;
	}
}
