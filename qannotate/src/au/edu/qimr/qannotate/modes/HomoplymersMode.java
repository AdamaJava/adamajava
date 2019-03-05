/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;

public class HomoplymersMode extends AbstractMode{
	private final static QLogger logger = QLoggerFactory.getLogger(HomoplymersMode.class);
	private final String input;
	private final String output;
	private final int homopolymerWindow;
	private final String dbfile;
	private int reportWindow;
	public static final int defaultWindow = 100;
	public static final int defaultreport = 10;
	
	@Deprecated //for unit test
	HomoplymersMode( int homoWindow, int reportWindow){		
		this.input = null;
		this.output = null;
		this.dbfile = null;
		this.homopolymerWindow = homoWindow;
		this.reportWindow = reportWindow;
	}
		
	public HomoplymersMode(Options options) throws IOException {
		input = options.getInputFileName();
		output = options.getOutputFileName();
		dbfile = options.getDatabaseFileName();
		homopolymerWindow =  options.getHomoplymersWindow();
		reportWindow = options.getHomoplymersReportSize();
		reportWindow = options.getHomoplymersReportSize();
		logger.tool("input: " + options.getInputFileName());
        logger.tool("reference file: " + dbfile);
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        logger.tool("window size for homoplymers: " + homopolymerWindow);
        logger.tool("number of homoplymer bases on either side of variant to report: " + reportWindow);
        
        reheader(options.getCommandLine(),options.getInputFileName());
 		addAnnotation(dbfile);		
	}
	
	/*
	 * taken from htsjdk
	 * https://github.com/samtools/htsjdk/blob/master/src/main/java/htsjdk/samtools/reference/AbstractFastaSequenceFile.java
	 */
	protected static Path findSequenceDictionary(final Path path) {
        if (path == null) {
            return null;
        }
        // Try and locate the dictionary with the default method
        final Path dictionary = ReferenceSequenceFileFactory.getDefaultDictionaryForReferenceSequence(path); path.toAbsolutePath();
        if (Files.exists(dictionary)) {
            return dictionary;
        }
        // try without removing the file extension
        final Path dictionaryExt = path.resolveSibling(path.getFileName().toString() + IOUtil.DICT_FILE_EXTENSION);
        if (Files.exists(dictionaryExt)) {
            return dictionaryExt;
        }
        else return null;
    }
	
	
	@Override
	void addAnnotation(String dbfile) throws IOException {
		//load reference data
		Map<String, byte[]> referenceBase = getReferenceBase(new File(dbfile));
		
		try (VCFFileReader reader = new VCFFileReader(input);
	            VCFFileWriter writer = new VCFFileWriter(new File(output))) {
			header.addInfo(VcfHeaderUtils.INFO_HOM,  "2", "String",VcfHeaderUtils.INFO_HOM_DESC); 			
		    for (final VcfHeaderRecord record: header) {
		    	writer.addHeader(record.toString());
		    }
		    
		    int sum = 0;
			for (final VcfRecord re : reader) {	
				writer.add( annotate(re, referenceBase.get(IndelUtils.getFullChromosome(re.getChromosome()))));
				sum ++;
			}
			logger.info(sum + " records have been put through qannotate's homopolymer mode");
		}
	}
	
	VcfRecord annotate(VcfRecord re1, byte[] base){
		VcfRecord re = re1; 
		ChrRangePosition pos = new ChrRangePosition(  re.getChrPosition());
		SVTYPE variantType = IndelUtils.getVariantType(re.getRef(), re.getAlt());
		String motif = IndelUtils.getMotif(re.getRef(), re.getAlt(), variantType);
		byte[][] sideBases = getReferenceBase(base, pos, variantType, homopolymerWindow);
		
		String str = getHomopolymerData(motif, sideBases, variantType, reportWindow);
		re.appendInfo(VcfHeaderUtils.INFO_HOM + Constants.EQ + str);						 
		return re; 
	}
	
	/**
	 * use default of 
	 * @param motif
	 * @param sideBases
	 * @param variantType
	 * @return
	 */
	public static String getHomopolymerData(String motif, byte[][] sideBases, SVTYPE variantType) {
		return getHomopolymerData(motif, sideBases, variantType, defaultreport);
	}
	
	public static String getHomopolymerData(String motif, byte[][] sideBases, SVTYPE variantType, int reportWindow ) {
		/*
		 * need to deal with multiple alts - find the one that gives the greatest HOM count and use that
		 */
		int homNo = 0;
		if (motif.contains(Constants.COMMA_STRING)) {
			String [] altAlleles = motif.split(Constants.COMMA_STRING);
			int maxHomValue = 0;
			String maxHomAlt = null;
			for (String alt : altAlleles) {
				if (maxHomAlt == null) {
					maxHomAlt = alt;
				}
				int hNo = findHomopolymer(sideBases, alt,variantType);
				if (hNo > maxHomValue) {
					maxHomValue = hNo;
					maxHomAlt = alt;
				}
			}
			motif = maxHomAlt;
			homNo = maxHomValue;
			
		} else {
			homNo = findHomopolymer(sideBases,  motif,variantType);
		}
		return homNo + Constants.COMMA_STRING + getHomTxt(motif, sideBases, variantType, reportWindow);
	}
	
	private static String getHomTxt(String variantStr, byte[][] updownReference, SVTYPE type, int reportWindow ) {	
		
		//at the edge of reference, the report window maybe bigger than nearby base
		int baseNo1 = Math.min(updownReference[0].length, reportWindow) ;	
		int baseNo2 = Math.min(updownReference[1].length, reportWindow) ;		
		byte[] seq = new byte[baseNo1 + variantStr.length() + baseNo2]; 
				
		System.arraycopy(updownReference[0], (updownReference[0].length - baseNo1), seq, 0, baseNo1);  	
 		System.arraycopy(updownReference[1], 0, seq, baseNo1 + variantStr.length(), baseNo2); 
 				
		if (type.equals(SVTYPE.DEL)) {			 
			for (int i = 0; i < variantStr.length(); i++) {
				seq[baseNo1 + i] = '_';
			}
		} else {  			
			//copy INS base  or SNPs reference base
			System.arraycopy( variantStr.toLowerCase().getBytes(), 0, seq, baseNo1 , variantStr.length());  
		}
		return new String(seq); 
	}
	
	static byte[][] getReferenceBase(byte[]  reference, ChrRangePosition pos, SVTYPE type, int homopolymerWindow) { 	
		//eg. INS: 21 T TC ,  DEL: 21  TCC T,  SNPs: TCC ATT
		//T from INS or DEL  position.getPosition() is 21 but should be  referenceBase[20] which is end of upStream
		//INS position.getEndPosition() is 21, downStream should start at referenceBase[21]
		//DEL position.getEndPosition() is 23, downStream should start at referenceBase[23]
	    //SNPs referenceBase[19] which is end of upStream and downStream should start at referenceBase[23]
		
	    //byte[] start with 0 but reference start with 1. 
		//INDELs contain leading base from reference but SNPs not
	    int indelStart = (type.equals(SVTYPE.INS) || type.equals(SVTYPE.DEL)) ? pos.getStartPosition() : pos.getStartPosition() - 1;
    	int wstart =  Math.max( 0,indelStart-homopolymerWindow);
    	if (indelStart - wstart < 0) {
  	    	throw new IllegalArgumentException("Start of variant is before (or equal to) start of contig! variant start (+homopolymerWindow): " + wstart);
  	    }
    	byte[] upstreamReference = new byte[indelStart - wstart];
	    System.arraycopy(reference, wstart, upstreamReference, 0, upstreamReference.length);
	    
	    int indelEnd = pos.getEndPosition();
	    int wend = Math.min(reference.length, indelEnd + homopolymerWindow);
	    if (wend - indelEnd < 0) {
	    	throw new IllegalArgumentException("End of variant is past (or equal to) end of contig! contig end: " + reference.length + ", variant end (+homopolymerWindow): " + wend);
	    }
     	byte[] downstreamReference = new byte[wend - indelEnd];
     	System.arraycopy(reference, indelEnd, downstreamReference, 0, downstreamReference.length);   
     	return new byte[][]{upstreamReference, downstreamReference};
	}
	
	static int findHomopolymer(byte[][] updownReference, String motif, SVTYPE indelType){
		if (null == motif || motif.contains(Constants.COMMA_STRING)) {
			throw new IllegalArgumentException("motif supplied to findHomopolymer is invalid: " + motif);
		}
		
		int upBaseCount = 1;
		int downBaseCount = 1;
 		//upstream - start from end since this is the side adjacent to the indel
		//decide if it is contiguous		
		final int finalUpIndex = updownReference[0].length - 1;	
		
		//count upstream homopolymer bases
		if (finalUpIndex >= 0) {
			char nearBase = (char) updownReference[0][finalUpIndex];
			for (int i = finalUpIndex - 1; i >= 0; i--) {
				if (nearBase == updownReference[0][i]) {
					upBaseCount++;
				} else {
					break;
				}
			}
		}
		
		//count downstream homopolymer
		if (null != updownReference[1] && updownReference[1].length > 0) {
			char nearBase = (char) updownReference[1][0];
			for (int i = 1; i < updownReference[1].length; i++) {
				if (nearBase == updownReference[1][i]) {
					downBaseCount++;
				} else {
					break;
				}
			}
		}
		
		int max;
		//reset up or down stream for deletion and SNPs reference base
		if(indelType.equals(SVTYPE.DEL) || indelType.equals(SVTYPE.SNP) || indelType.equals(SVTYPE.DNP)  
				|| indelType.equals(SVTYPE.ONP) || indelType.equals(SVTYPE.TNP) ){
			byte[] mByte = motif.getBytes();
			
			int left = 0;
			if (finalUpIndex >= 0) {
				char nearBase = (char) updownReference[0][finalUpIndex];
				for(int i = 0; i < mByte.length; i++ ) {
					if (nearBase == mByte[i]) {
						left ++;
					} else {
						break;				 
					}
				}
			}
			upBaseCount += left; 
						
			int right = 0;
			if (null != updownReference[1] && updownReference[1].length > 0) {
				char nearBase = (char) updownReference[1][0];
				for(int i = mByte.length - 1; i >= 0; i--) { 
					if (nearBase == mByte[i]) {
						right++;
					} else  {
						break;
					}
				}
			}
			downBaseCount += right; 
			
			max = (left == right && left == mByte.length) ? 
					(downBaseCount + upBaseCount - mByte.length) : Math.max(downBaseCount, upBaseCount);
		} else {
		    //INS don't have reference base
			max = (updownReference[0][finalUpIndex] == updownReference[1][0]) ? 
					(downBaseCount + upBaseCount) : Math.max(downBaseCount, upBaseCount);
		}
					
		return max == 1 ? 0 : max;
	}
	
   static Map<String, byte[]> getReferenceBase(File reference) throws IOException {
	   
	   /*
        * check to see if the index and dict file exist for the reference
        */
       Path dictPath = findSequenceDictionary(reference.toPath());
       if (null == dictPath) {
       	throw new IllegalArgumentException("No dict file found for reference file: " + reference);
       }
       logger.tool("reference dictionary file: " + dictPath.toString());
       Path indexPath = ReferenceSequenceFileFactory.getFastaIndexFileName(reference.toPath());
       if (null == indexPath || ! indexPath.toFile().exists()) {
       	throw new IllegalArgumentException("No index file found for reference file: " + reference);
       }
       logger.tool("reference index file: " + indexPath.toString());
	   
	   Map<String, byte[]> referenceBase = new HashMap<>();
	   
	   FastaSequenceIndex index = new FastaSequenceIndex(indexPath);
	   
	   try (IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(reference, index);) {
		   SAMSequenceDictionary dict = indexedFasta.getSequenceDictionary();
		   for (SAMSequenceRecord re: dict.getSequences()) {
			   String contig = IndelUtils.getFullChromosome(re.getSequenceName());
			   referenceBase.put(contig, indexedFasta.getSequence(contig).getBases());
		   }
	   }
	   return referenceBase;
   }
}
