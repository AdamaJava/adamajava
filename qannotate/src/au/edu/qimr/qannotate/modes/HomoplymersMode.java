/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;

public class HomoplymersMode extends AbstractMode{
	private final static QLogger logger = QLoggerFactory.getLogger(HomoplymersMode.class);
	String input;
	String output;
	final String commandLine;
	private final int homopolymerWindow;
	private final int reportWindow;
//	private final int homBase;
	public static int defaultWindow = 100;
	public static int defaultreport = 10;
	public static int defaultHomoplymerBase = 5;
	
	@Deprecated
	HomoplymersMode( int homoWindow, int reportWindow){		
		this.input = null;
		this.output = null;
		this.commandLine = null;
		this.homopolymerWindow = homoWindow;
		this.reportWindow = reportWindow;
//		this.homBase = defaultHomoplymerBase; 
	}
		
	public HomoplymersMode(Options options) throws Exception{	
		input = options.getInputFileName();
		output = options.getOutputFileName();
		commandLine = options.getCommandLine();
		homopolymerWindow =  options.getHomoplymersWindow();
		reportWindow = options.getHomoplymersReportSize();
//		homBase = options.getHomoplymersReportSize();
		logger.tool("input: " + options.getInputFileName());
        logger.tool("reference file: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        logger.tool("window size for homoplymers: " + homopolymerWindow);
        logger.tool("report homoplymers base on both side of variants: " + reportWindow);
        reheader(options.getCommandLine(),options.getInputFileName());
 		addAnnotation(options.getDatabaseFileName() );		
		
		//writeVCF( new File(options.getOutputFileName()));	
	}	
	
	
	@Override
	void addAnnotation(String dbfile) throws IOException {
		//load reference data
		Map<String, byte[]> referenceBase = getReferenceBase(new File(dbfile));
		
		try (VCFFileReader reader = new VCFFileReader(input) ;
	            VCFFileWriter writer = new VCFFileWriter(new File(output))  ) {
			header.addInfoLine(VcfHeaderUtils.INFO_HOM,  "1", "String",VcfHeaderUtils.DESCRITPION_INFO_HOM); 			
		    for(final VcfHeader.Record record: header)	writer.addHeader(record.toString());
		    
		    int sum = 0;
			for (final VcfRecord re : reader) {	
				String chr = IndelUtils.getFullChromosome(re.getChromosome());
				byte[]  base =  referenceBase.get(chr);								 
				writer.add( annotate(re,  base));
				sum ++;
			}
			logger.info(sum + " record are outputed with homoplymers information");
		}
	}
	
	VcfRecord annotate(VcfRecord re1, byte[] base){
		VcfRecord re = re1; 
		ChrRangePosition pos = new ChrRangePosition(  re.getChrPosition());
		SVTYPE variantType = IndelUtils.getVariantType(re.getRef(), re.getAlt());
		String motif = IndelUtils.getMotif(re.getRef(), re.getAlt(), variantType);
		byte[][] sideBases = getReferenceBase(base, pos, variantType);		
		int homNo = findHomopolymer(sideBases,  motif,variantType);
		String var = (variantType.equals(SVTYPE.INS) || variantType.equals(SVTYPE.DEL))?
				motif : re.getAlt();
		
		String str = homNo + Constants.COMMA_STRING + getHomTxt(var, sideBases, variantType);
		re.appendInfo(VcfHeaderUtils.INFO_HOM + Constants.EQ + str);						 

		return re; 
	}
	
	private String getHomTxt(String variantStr, byte[][] updownReference, SVTYPE type ) {	
		
		//at the edge of reference, the report window maybe bigger than nearby base
		int baseNo1 = Math.min(updownReference[0].length, reportWindow) ;	
		int baseNo2 = Math.min(updownReference[1].length, reportWindow) ;		
		byte[] seq = new byte[baseNo1 + variantStr.length() + baseNo2]; 
				
		System.arraycopy(updownReference[0], (updownReference[0].length - baseNo1), seq, 0, baseNo1);  	
 		System.arraycopy(updownReference[1], 0, seq, baseNo1 + variantStr.length(), baseNo2); 
 				
		if (type.equals(SVTYPE.DEL)) {			 
			for (int i=0; i< variantStr.length(); i++) {
				seq[baseNo1 + i] = '_';
			}
		} else {  			
			//copy INS base  or SNPs reference base
			System.arraycopy( variantStr.toLowerCase().getBytes(), 0, seq, baseNo1 , variantStr.length());  
		}
		return new String(seq); 
	}
	
	
	private byte[][] getReferenceBase(byte[]  reference, ChrRangePosition pos, SVTYPE type) { 	
		//eg. INS: 21 T TC ,  DEL: 21  TCC T,  SNPs: TCC ATT
		//T from INS or DEL  position.getPosition() is 21 but should be  referenceBase[20] which is end of upStream
		//INS position.getEndPosition() is 21, downStream should start at referenceBase[21]
		//DEL position.getEndPosition() is 23, downStream should start at referenceBase[23]
	    //SNPs referenceBase[19] which is end of upStream and downStream should start at referenceBase[23]
		
	    //byet[] start with 0 but reference start with 1. 
		//INDELs contain leading base from reference but SNPs not
	    int indelStart = (type.equals(SVTYPE.INS) || type.equals(SVTYPE.DEL)) ? pos.getStartPosition() : pos.getStartPosition() - 1 ;	    
	    	int wstart =  Math.max( 0,indelStart-homopolymerWindow) ;   	
	    	byte[] upstreamReference = new byte[indelStart - wstart ]; 
	    System.arraycopy(reference, wstart, upstreamReference, 0, upstreamReference.length);
	    
	    int indelEnd = pos.getEndPosition(); 
	    int wend = Math.min(reference.length, indelEnd + homopolymerWindow);  
     	byte[] downstreamReference = new byte[wend - indelEnd ];     	 	
     	System.arraycopy(reference, indelEnd, downstreamReference, 0, downstreamReference.length);   
     	return new byte[][]{upstreamReference, downstreamReference};     	
	}
	
	private int findHomopolymer(byte[][] updownReference, String motif, SVTYPE indelType){
			
			int upBaseCount = 1;
			int downBaseCount = 1;
	 		//upstream - start from end since this is the side adjacent to the indel
			//decide if it is contiguous		
			int finalUpIndex = updownReference[0].length-1;	
			
			//count upstream homopolymer bases
			char nearBase = (char) updownReference[0][finalUpIndex];
			for (int i=finalUpIndex-1; i>=0; i--) {
				if (nearBase == updownReference[0][i]) {
					upBaseCount++;
				} else {
					break;
				}
			}
			
			//count downstream homopolymer
			nearBase = (char) updownReference[1][0];
			for (int i=1; i< updownReference[1].length; i++) {
				if (nearBase == updownReference[1][i]) {
					downBaseCount++;
				} else {
					break;
				}
			}
			
			int max  = 0;
			//reset up or down stream for deletion and SNPs reference base
			if(indelType.equals(SVTYPE.DEL) || indelType.equals(SVTYPE.SNP) || indelType.equals(SVTYPE.DNP)  
					|| indelType.equals(SVTYPE.ONP) || indelType.equals(SVTYPE.TNP) ){			
				byte[] mByte = motif.getBytes(); 	
				
				int left = 0;
				nearBase = (char) updownReference[0][finalUpIndex];			
				for(int i = 0; i < mByte.length; i ++ ) 
					if (nearBase == mByte[i])  left ++;
					else  break;				 
				upBaseCount += left; 
							
				int right = 0;
				nearBase = (char) updownReference[1][0];
				for(int i = mByte.length -1; i >=0; i--) 
					if (nearBase == mByte[i]) right++;
					else break;
				downBaseCount += right; 
				
				max = (left == right && left == mByte.length)? 
						(downBaseCount + upBaseCount - mByte.length) : Math.max(downBaseCount, upBaseCount);
							 			
			}else{
			    //INS don't have reference base
				max = (updownReference[0][finalUpIndex] == updownReference[1][0] )? 
						(downBaseCount + upBaseCount) : Math.max(downBaseCount, upBaseCount);
			}
						
			return (max == 1)? 0 : max;
		}
	
   public static Map<String, byte[]> getReferenceBase(File reference) throws IOException{
	   Map<String, byte[]> referenceBase = new HashMap<String, byte[]>();
	   File indexFile = new File(reference.getAbsolutePath() + ".fai");
	   FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
	   try (IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(reference, index);) {
		   for(SAMSequenceRecord re: indexedFasta.getSequenceDictionary().getSequences()  ){	
			   String contig = IndelUtils.getFullChromosome(re.getSequenceName());
			   referenceBase.put(contig, indexedFasta.getSequence(contig).getBases());
		   }
	   }
	   return referenceBase; 	   
   }
}
