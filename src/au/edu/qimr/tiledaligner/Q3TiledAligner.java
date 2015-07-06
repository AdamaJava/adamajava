package au.edu.qimr.tiledaligner;

import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;

//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

public class Q3TiledAligner {
	
private static QLogger logger;
	
	
	private static String version;
	private String logFile;
	private String outputFile;
	private String inputFile;
	private FastaSequenceFile sequenceFile;
	
	// chr specific data
	private byte[] referenceBases;
	private int referenceBasesLength;
	private String currentChr;
	
	private final int tileSize = 13;
	
	private final Map<String, List<ChrPosition>> tilesAndPositions = new HashMap<>();
	
	private int exitStatus;
	
	protected int engage() throws Exception {
		
		logger.info("Lets go!");
				
		return exitStatus;
	}
	
	void performBinning() {
		
		while (true) {
			// load next contig
			loadNextReferenceSequence();
			
			if (null == referenceBases) {
				// nothing left - exit
				break;
			}
			
			for (int i = 0 ; i < referenceBasesLength - tileSize; i++) {
				String tile = new String(Arrays.copyOfRange(referenceBases, i, i+ tileSize));
				ChrPosition cp = new ChrPosition(currentChr, i + 1);
				updateMap(tile, cp);
			}
		}
		
		// print some stats
		
		logger.info("no of entries in tilesAndPositions: " +tilesAndPositions.size());
	}
	
	void updateMap(String tile, ChrPosition pos) {
		List<ChrPosition> positions = tilesAndPositions.get(tile);
		if (null == positions) {
			positions = new ArrayList<>();
			tilesAndPositions.put(tile, positions);
		}
		positions.add(pos);
	}

	
	void loadNextReferenceSequence() {
		if (null == sequenceFile) {
			sequenceFile = new FastaSequenceFile(new File(inputFile), true);
		}
		referenceBases = null;
		currentChr = null;
		referenceBasesLength = 0;
		ReferenceSequence refSeq = sequenceFile.nextSequence();
		
		// debugging code
//		while ( ! "chr1".equals(refSeq.getName()))
//			refSeq = sequenceFile.nextSequence();
//			 debugging code
		if (null == refSeq) {	// end of the line
			logger.info("No more chromosomes in reference file - shutting down");
			closeReferenceFile();
		} else {
			currentChr = refSeq.getName();
			referenceBases = refSeq.getBases();
			referenceBasesLength = refSeq.length();
			logger.info("Will process records from: " + currentChr + ", length: " + referenceBasesLength);
		}
	}
	
	void closeReferenceFile() {
		if (null != sequenceFile) sequenceFile.close();
	}
	
	public static void main(String[] args) throws Exception {
		// loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Q3TiledAligner.class);
		
		Q3TiledAligner qp = new Q3TiledAligner();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
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
//		} else if (options.getFastqs().length < 1) {
//			System.err.println(Messages.USAGE);
//		} else if ( ! options.hasLogOption()) {
//			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = Q3TiledAligner.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(Q3TiledAligner.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("q3tiledaligner", version, args);
			
			// get list of file names
			inputFile = options.getInput();
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputFile = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			
			return engage();
		}
		return returnStatus;
	}

}
