package org.qcmg.sig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.qio.vcf.VcfFileReader;


/**
 * This class adds gene model information to the info field of a vcf file.
 * Only gene model records that have a feature type of exon or five_prime_utr or three_prime_utr will be used.
 * 
 * The intended use case is to annotate a snp positions vcf file with gene model information 
 * and as such it is not anticipated that this class will be often used.
 * 
 * @author oliverh
 *
 */
public class VcfAddGeneModel {
	
	private static QLogger logger;
	private int exitStatus;

    private String outputVcf;
	private String inputVcf;
	private String geneModelFile;
	
	private final Map<ChrPosition, String> geneModelPositionMap = new HashMap<>();
	
	private int engage() throws Exception {
		
		int geneModelRecordCount = 0;
		int geneModelExonRecordCount = 0;
		/*
		 * load gene model file into memory
		 */
		
		try (StringFileReader reader = new StringFileReader(new File(geneModelFile))) {
			for (String rec: reader) {
				geneModelRecordCount++;
				String [] recArray = TabTokenizer.tokenize(rec);
				
				/*
				 * only proceed if we are an exon, five_prime_utr or three_prime_utr, 
				 */
				
				if (recArray[2].equals("exon") || recArray[2].equals("five_prime_utr") || recArray[2].equals("three_prime_utr")) {
					geneModelExonRecordCount++;
					/*
					 * loop over the positions in this record and create an entry in the map for each one
					 * This should make the lookup quicker, at the expense of having a larger map
					 * 
					 * There will be some overlapping positions - stick with the first one encountered.
					 */
					int start = Integer.parseInt(recArray[3]);
					int stop = Integer.parseInt(recArray[4]);
					for (int i = start ; i <= stop ; i++) {
						ChrPosition cp = new ChrPointPosition(recArray[0], i);
						geneModelPositionMap.putIfAbsent(cp, recArray[8]);
					}
				}
			}
		}
		
		logger.info("Number of entries in geneModelPositionMap: " + geneModelPositionMap.size());
		logger.info("geneModelRecordCount: " + geneModelRecordCount);
		logger.info("geneModelExonRecordCount: " + geneModelExonRecordCount);
		
		/*
		 * now go through input vcf and look to see how many matches we get
		 */
		
		int matchingGeneModelAtPosition = 0;
		int vcfRecordCount = 0;
		try (VcfFileReader vReader = new VcfFileReader(inputVcf);
				RecordWriter<VcfRecord> writer = new RecordWriter<>(new File(outputVcf))) {
			VcfHeader header = vReader.getVcfHeader();
			for (final VcfHeaderRecord record: header) {
				writer.addHeader(record.toString());
			}
			for (VcfRecord vRec : vReader) {
				vcfRecordCount++;
				String info = geneModelPositionMap.get(vRec.getChrPosition());
				if (null != info) {
					/*
					 * perform a little manipulation on the info so that it is valid in the vcf info field
					 */
					vRec.setInfo(info.replace("; ", ";").replace(" ", "=").replace("\"", ""));
					matchingGeneModelAtPosition++;
				}
				writer.add(vRec);
			}
		}
		logger.info(matchingGeneModelAtPosition + " positions were found in gene model map from a total of: " + vcfRecordCount);
		
		return exitStatus;
	}
	
	public static void main(String[] args) throws Exception {
		VcfAddGeneModel sp = new VcfAddGeneModel();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger) {
				logger.error("Exception caught whilst running VcfAddGeneModel:", e);
			} else {
				System.err.println("Exception caught whilst running VcfAddGeneModel: " + e.getMessage());
				System.err.println(Messages.GENE_MODEL_USAGE);
			}
		}
		
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}
	
	protected int setup(String [] args) throws Exception {
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.GENE_MODEL_USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.GENE_MODEL_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.GENE_MODEL_USAGE);
		} else {
			// configure logging
            String logFile = options.getLog();
			logger = QLoggerFactory.getLogger(VcfAddGeneModel.class, logFile, options.getLogLevel());
			
			
			String [] cmdLineOutputFiles = options.getOutputFileNames();
			if (null != cmdLineOutputFiles && cmdLineOutputFiles.length > 0) {
				outputVcf = cmdLineOutputFiles[0];
			}
			
			options.getGeneModelFile().ifPresent(gm -> geneModelFile = gm);
			
			String [] cmdLineInputFiles = options.getInputFileNames();
			if (null != cmdLineInputFiles && cmdLineInputFiles.length > 0) {
				inputVcf = cmdLineInputFiles[0];
			}
			
			logger.logInitialExecutionStats("VcfAddGeneModel", VcfAddGeneModel.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
