package au.edu.qimr.tiledaligner;

import au.edu.qimr.tiledaligner.util.TiledAlignerUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.string.StringFileReader;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.util.SequenceUtil;

public class TiledAligner {
	
	private static QLogger logger;
	public final static int TILE_LENGTH = 13;
	
	private static String version;
	private String logFile;
	private String outputFile;
	private String inputFile;
	private String reference;
	private String sequence;
	private String name;
	private FastaSequenceFile sequenceFile;
	
	protected int engage() throws Exception {
		
		logger.info("Lets go!");
		
		
		/*
		 * split sequence into tiles so that we can build up a map containing just the tiles of interest from the 
		 */
		Set<String> seqTiles = tileSequence(sequence);
		TIntObjectMap<int[]> map = new TIntObjectHashMap<>(68 * 1024, 1f);
		
		getTiledDataInMap(inputFile, 256, seqTiles, map);
		
		/*
		 * create map of sequence to name
		 */
		Map<String, String> sequenceToNameMap = new HashMap<>();
		sequenceToNameMap.put(sequence, name);
		
		boolean recordsMustComeFromChrInName = null != name && name.contains("splitcon");
		
		Map<String, List<BLATRecord>> results = TiledAlignerUtil.runTiledAlignerCache(reference, map, sequenceToNameMap, TILE_LENGTH, "TiledAligner", true, recordsMustComeFromChrInName);
		
		for (Entry<String, List<BLATRecord>> result : results.entrySet()) {
			logger.info("name: " + result.getKey());
			for (BLATRecord br : result.getValue()) {
				logger.info("rec: " + br.toString());
			}
		}
		return 0;
	}
	
	public static void getTiledDataInMap(String tiledAlignerFile, int bufferSize, Set<String> tilesToMatch, TIntObjectMap<int[]> map) throws IOException {
		
		try (StringFileReader reader = new StringFileReader(new File(tiledAlignerFile), bufferSize * 1024)) {
			
			int i = 0;
			int invalidCount = 0;
			long start = System.currentTimeMillis();
			for (String rec : reader) {
				/*
				 * just get the tile
				 */
				if (tilesToMatch.contains(rec.substring(0, TILE_LENGTH))) {
					String tile = rec.substring(0, TILE_LENGTH);
					int tileLong = NumberUtils.convertTileToInt(tile);
					if (tileLong == -1) {
						invalidCount++;
					} else {
						int [] existingArray = map.put(tileLong, TiledAlignerUtil.convertStringToIntArray(rec.substring(TILE_LENGTH + 1)));
						if (null != existingArray) {
							logger.warn("already have an entry for tile: " + tile);
						}
						if (map.size() == tilesToMatch.size()) {
							/*
							 * we're done!
							 */
							break;
						}
					}
				}
				if (++i % 1000000 == 0) {
					logger.info("hit " + (i / 1000000) + "M, map size: " + map.size());
				}
			}
			logger.info("Time taken with buffer size: " + bufferSize + "kb is: " + (System.currentTimeMillis() - start) + ", invalidCount: " + invalidCount);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			logger.info("finished reading from file - setting hasReaderFinished to true");
		}
	}
	
	public static Set<String> tileSequence(String sequence) {
		return tileSequence(sequence, TILE_LENGTH);
	}
	public static Set<String> tileSequence(String sequence, int tileLength) {
		Set<String> results = new HashSet<>();
		int length = sequence.length();
		int arraySize = (length - tileLength) + 1;
		String rcSequence = SequenceUtil.reverseComplement(sequence);
		for (int i = 0 ; i < arraySize ; i++) {
			String tile = sequence.substring(i, i + tileLength);
			String rcTile = rcSequence.substring(i, i + tileLength);
			results.add(tile);
			results.add(rcTile);
		}
		logger.info("Added " + results.size() + " tiles to map");
		return results;
	}
	
	public static void main(String[] args) throws Exception {
		
		TiledAligner qp = new TiledAligner();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception {
		int returnStatus = 1;
		Options options = new Options(args);
		if (null == args || args.length == 0) {
			System.err.println(Messages.TILED_ALIGNER_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasHelpOption()) {
			System.err.println(Messages.TILED_ALIGNER_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.getReference().isPresent()) {
			System.err.println("Please supply a reference option for the fasta file");
			System.err.println(Messages.TILED_ALIGNER_USAGE);
			returnStatus = 0;
		} else {
			// configure logging
			logFile = options.getLog();
			version = TiledAligner.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(TiledAligner.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("TiledAligner", version, args);
			
			// get list of file names
			inputFile = options.getInput();
			options.getReference().ifPresent(r -> reference = r);
			options.getSequence().ifPresent(r -> sequence = r);
			options.getName().ifPresent(r -> name = r);
			
			return engage();
		}
		return returnStatus;
	}
}
