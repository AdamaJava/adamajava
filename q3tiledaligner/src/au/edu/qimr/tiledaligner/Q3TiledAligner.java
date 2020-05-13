package au.edu.qimr.tiledaligner;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TLongProcedure;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;

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
	private byte currentChrIndex;
	
	private final int tileSize = 13;
	private final int positionsCutoff = 499;
	
	private final Map<String, TLongArrayList> tilesAndPositions = new HashMap<>(1024 * 1024 * 96);		// should prevent a resize - we are expecting 64Mish
//	private final Map<String, List<ChrPositionByteInt>> tilesAndPositions = new HashMap<>(1024 * 1024 * 32);
	private final Map<String, AtomicInteger> tilesAndCounts = new HashMap<>();
	private final List<String> chrLengthPosition = new ArrayList<>();
	
	private int exitStatus;
	
	protected int engage() throws Exception {
		
		logger.info("Lets go!");
		performBinning();
				
		return exitStatus;
	}
	
	void performBinning() throws IOException {
		long longPosition = 1;
		while (true) {
			// load next contig
			loadNextReferenceSequence();
			
			if (null == referenceBases) {
				// nothing left - exit
				break;
			}
			
			chrLengthPosition.add(currentChr + ":" + referenceBasesLength + ":" + longPosition); 
			
			int counter = 0;
			int millionCount = 0;
			int lastTilePosition = referenceBasesLength - tileSize;
			for (int i = 0 ; i < referenceBasesLength; i++) {
				if (i < lastTilePosition) {
				
					String tile = new String(Arrays.copyOfRange(referenceBases, i, i+ tileSize));
					
					AtomicInteger ai = tilesAndCounts.get(tile);
					if (null != ai) {
						ai.incrementAndGet();
					} else {
						
						TLongArrayList positions = tilesAndPositions.get(tile);
//						List<ChrPositionByteInt> positions = tilesAndPositions.get(tile);
						if (null == positions) {
							positions = new TLongArrayList();
//							positions = new ArrayList<>();
							tilesAndPositions.put(tile, positions);
						}
						
						if (positions.size() < positionsCutoff) {
//							ChrPositionByteInt cp = new ChrPositionByteInt(currentChrIndex, i + 1);
							positions.add(longPosition);
//							positions.add(cp);
						} else {
							// time to bump this into the counts map, and remove from positions map
							tilesAndCounts.put(tile,  new AtomicInteger(positions.size() + 1));
							tilesAndPositions.remove(tile);
						}
					}
					
					if (counter++ > 1000000) {
						logger.info("hit " + ++millionCount + "M kmers, positions map size: " + tilesAndPositions.size() + ", counts map size: " + tilesAndCounts.size());
						counter = 0;
					}
				}
				longPosition++;
			}
		}
		
		// print some stats
		
		logger.info("no of entries in tilesAndPositions: " +tilesAndPositions.size() + ", counts map size: " + tilesAndCounts.size());
		
		writeOutput();
	}
	
	private void writeOutput() throws IOException {
		List<String> orderedTiles = new ArrayList<>(tilesAndPositions.keySet());
		orderedTiles.addAll(tilesAndCounts.keySet());
		Collections.sort(orderedTiles);
		try (FileWriter writer = new FileWriter(new File(outputFile))) {
			
			/*
			 * Some header info
			 * 		tool, version, date, runBy, ref file used, column headers
			 */
			writer.write("##q3TiledAligner version: " + version + "\n");
			writer.write("##RunBy: " + System.getProperty("user.name") + "\n");
			writer.write("##RunOn: " + DateUtils.getCurrentDateAsString() + "\n");
			writer.write("##List of positions/Count cutoff: " + (positionsCutoff + 1) + "\n");
			writer.write("##Tile length: " + tileSize + "\n");
			writer.write("##Number of tiles: " +orderedTiles.size() + "\n");
			
			writer.write("##contig:contigLength:longPosition\n");
			for (String s : chrLengthPosition) {
				writer.write("##" + s + "\n");
			}
			
			
			writer.write("#Tile\tlist of positions OR count (C12345)\n");
			
			for (String tile : orderedTiles) {
				final StringBuilder sb = new StringBuilder();
				TLongArrayList positions = tilesAndPositions.get(tile);
//				List<ChrPositionByteInt> positions = tilesAndPositions.get(tile);
				if (null != positions) {
					positions.forEach(new TLongProcedure(){
						@Override
						public boolean execute(long l) {
							sb.append(l).append(",");
							return true;
						}});
					
//					for (ChrPositionByteInt position : positions) {
//						if (sb.length() > 0) {
//							sb.append(",");
//						}
//						sb.append(position.toString());
//					}
					// remove trailing comma
					sb.setLength(sb.length() - 1);
				} else {
					// get counts
					AtomicInteger ai = tilesAndCounts.get(tile);
					sb.append('C').append(ai.get());
				}
				sb.insert(0, tile + "\t");
				sb.append("\n");
				writer.write(sb.toString());
			}
		}
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
			currentChrIndex = (byte) refSeq.getContigIndex();
			logger.info("Will process records from: " + currentChr + ", length: " + referenceBasesLength + ", current map size: " + tilesAndPositions.size() + ", counts map size: " + tilesAndCounts.size());
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
