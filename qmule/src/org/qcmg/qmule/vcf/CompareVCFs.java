/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.vcf;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.picard.QJumper;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.vcf.VCFFileReader;

public class CompareVCFs {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private static QLogger logger;
	
	private final ConcurrentMap<ChrPosition, VCFRecord> normalVCFMap = new ConcurrentHashMap<ChrPosition, VCFRecord>(12500); //not expecting more than 100000
	private final ConcurrentMap<ChrPosition, VCFRecord> tumourVCFMap = new ConcurrentHashMap<ChrPosition, VCFRecord>(12500);
	private final ConcurrentMap<ChrPosition, VCFRecord> uniqueTumourVCFMap = new ConcurrentHashMap<ChrPosition, VCFRecord>(40000);
	
	public int engage() throws Exception {
		
		logger.info("loading normal vcf data");
		loadVCFData(cmdLineInputFiles[0], normalVCFMap);
		logger.info("loading normal vcf data - DONE [" + normalVCFMap.size() + "]");
		
		logger.info("loading tumour vcf data");
		loadVCFData(cmdLineInputFiles[1], tumourVCFMap);
		logger.info("loading tumour vcf data - DONE [" + tumourVCFMap.size() + "]");
		
		examine();
		
		addPileupFromNormalBam();
			
		return exitStatus;
	}
	
	private void addPileupFromNormalBam() throws Exception {
		// loop through each position in the unique map and get the entries in the normal GATK cleaned BAM file.
		int notEnoughCoverage = 0, mutationFoundInNormal = 0;
		StringBuilder sb = new StringBuilder();
		QJumper qj = new QJumper();
		qj.setupReader(cmdLineInputFiles[2]);
		
		for (Entry<ChrPosition, VCFRecord> entry : uniqueTumourVCFMap.entrySet()) {
			int position = entry.getKey().getPosition();
			boolean foundInNormal = false;
			List<SAMRecord> sams = qj.getOverlappingRecordsAtPosition(entry.getKey().getChromosome(), position, position);
			
			for (SAMRecord sam : sams) {
				int offset = SAMUtils.getIndexInReadFromPosition(sam, position);
				if (offset > -1 && offset < sam.getReadLength()) {
					char c = sam.getReadString().charAt(offset);
					if (c == entry.getValue().getAlt().charAt(0)) {
						foundInNormal = true;
						mutationFoundInNormal++;
						break;
					}
				}
			}
			
			if ( ! foundInNormal && sams.size() < 8) 
				notEnoughCoverage++;
			else if ( ! foundInNormal) 
				sb.append(entry.getKey().getChromosome() + ":" + position + "\n");
		}
		
		logger.info("total positions examined: " + uniqueTumourVCFMap.size());
		logger.info("positions where mutation was also found in normal (class C): " + mutationFoundInNormal);
		logger.info("positions where coverage in normal was less than 8 (class B): " + notEnoughCoverage);
		logger.info("Potential class A positions: ");
		logger.info(sb.toString());
	}
	
//	public static int getReadPosition(SAMRecord sam, int position) {
//		int offset = -1;
//		int readStart = sam.getAlignmentStart();
//		int readEnd = sam.getAlignmentEnd();
//		int readLength = sam.getReadLength();
//		
//		if (position == readStart) offset = 0;
//		if (position == readEnd) offset = readLength-1;
//		
//		if (-1 == offset) {
//			if (CoordMath.getLength(readStart, readEnd) == readLength) {
//				//woohoo - no deletions
//				offset = position - readStart;
////				readLengthMatchCounter++;
//			} else {
////				// we have a deletion - use AlignmentBlocks to determine the position within the string
//				
//				int blockLengthTally = 0;
//				for (AlignmentBlock block : sam.getAlignmentBlocks()) {
//					if (block.getReferenceStart() <= position && (CoordMath.getEnd(block.getReferenceStart(),block.getLength()) >= position)) {
//						offset = blockLengthTally + ( position -  block.getReferenceStart() );
//						break;
//					} else blockLengthTally += block.getLength();
//				}
//				
////				if (-1 == offset) {
////					posiitonInDeletionCounter++;
////				}
//			}
//		}	
//		return offset;
//	}
	
	private void examine() {
		
		final Map<RefAndMultiGenotype, AtomicLong> diffGenotypes = new HashMap<RefAndMultiGenotype, AtomicLong>();
		
		// we want to know the following...
		// number unique to normal
		// number unique to tumour
		// no of  common positions
		int normalUnique = 0, tumourUnique = 0, normalAndTumour = 0;
		
		// for the common positions...
		// no that have the same mutation
		// no that have a different  mutation
		// no of those that have the same genotype
		
		int sameMutation = 0, sameMutationSameGenotype = 0;
		int diffMutation = 0, diffMutationSameGenotype = 0;
		
		// here we go
		
		for (Entry <ChrPosition, VCFRecord> entry : normalVCFMap.entrySet()) {
			
			VCFRecord normalVCF = entry.getValue();
			VCFRecord tumourVCF = tumourVCFMap.get(entry.getKey());
			
			if (null == tumourVCF) {
				normalUnique++;
			} else {
				++normalAndTumour;
				
				// sanity check - compare ref - if not the same - oh dear...
				assert normalVCF.getRef() == tumourVCF.getRef();
				
				// compare mutations
				char normalMut = normalVCF.getAlt().charAt(0);
				char tumourMut = tumourVCF.getAlt().charAt(0);
				
				// need to get the genotype from the VCFRecord
				
				GenotypeEnum normalGenotype = VcfUtils.calculateGenotypeEnum(
						normalVCF.getInfo().substring(0, 3), normalVCF.getRefChar(), normalVCF.getAlt().charAt(0));
				GenotypeEnum tumourGenotype = VcfUtils.calculateGenotypeEnum(
						tumourVCF.getInfo().substring(0, 3), tumourVCF.getRefChar(), tumourVCF.getAlt().charAt(0));
//				GenotypeEnum normalGenotype = normalVCF.getGenotypeEnum();
//				GenotypeEnum tumourGenotype = tumourVCF.getGenotypeEnum();
				
				if (normalMut == tumourMut) {
					sameMutation++;
					if (normalGenotype == tumourGenotype)
						++sameMutationSameGenotype;
					else {
						RefAndMultiGenotype ramg = new RefAndMultiGenotype(normalVCF.getRefChar(), normalGenotype, tumourGenotype);
						AtomicLong al = diffGenotypes.get(ramg);
						if (null == al) {
							al = new AtomicLong();
							diffGenotypes.put(ramg, al);
						}
						al.incrementAndGet();
//						logger.info(normalVCF.getRef() + " : " + normalGenotype + " : " + tumourGenotype);
					}
				} else {
					diffMutation++;
					if (normalGenotype == tumourGenotype)
						++diffMutationSameGenotype;
				}
			}
		}
		
		for (ChrPosition position : tumourVCFMap.keySet()) {
			if (null ==  normalVCFMap.get(position)) {
				tumourUnique++;
				uniqueTumourVCFMap.put(position, tumourVCFMap.get(position));
			}
		}
		
		// now print out some stats
		StringBuilder sb = new StringBuilder("\nSTATS\n");
		sb.append("No of positions in normal map: " + normalVCFMap.size());
		sb.append("\nNo of unique positions in normal map: " + normalUnique);
		sb.append("\nNo of positions in tumour map: " + tumourVCFMap.size());
		sb.append("\nNo of unique positions in tumour map: " + tumourUnique);
		sb.append("\nNo of shared positions: " + normalAndTumour);
		sb.append("\n");
		sb.append("\nNo of positions with same mutation: " + sameMutation);
		sb.append("\nNo of positions with same mutation and same genotype: " + sameMutationSameGenotype);
		
		sb.append("\npositions with same mutation and diff genotype: ");
		
		for (Entry<RefAndMultiGenotype, AtomicLong> entry : diffGenotypes.entrySet()) {
			sb.append("\n" + entry.getKey().toString() + " count: " + entry.getValue().get());
		}
		sb.append("\nNo of positions with diff mutation: " + diffMutation);
		sb.append("\nNo of positions with diff mutation and same genotype: " + diffMutationSameGenotype);
		
		logger.info(sb.toString());
		
		
	}
	
	private void loadVCFData(String vcfFile, Map<ChrPosition,VCFRecord> map) throws Exception {
		if (FileUtils.canFileBeRead(vcfFile)) {
			
			VCFFileReader reader  = new VCFFileReader(new File(vcfFile));
			try {
				for (VCFRecord qpr : reader) {
					map.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
				}
			} finally {
				reader.close();
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		CompareVCFs sp = new CompareVCFs();
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
			logger = QLoggerFactory.getLogger(CompareVCFs.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareVCFs", CompareVCFs.class.getPackage().getImplementationVersion(), args);
			
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
