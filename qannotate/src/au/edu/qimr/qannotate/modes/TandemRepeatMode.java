/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.*;
import java.util.*;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.*;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.*;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;

//???? next day
public class TandemRepeatMode  extends AbstractMode{
	private static final int BLOCK_INDEX_GAP = 100; //insert index for big block
	private static final int MaxErrLine = 100; 
	
	private final QLogger logger = QLoggerFactory.getLogger(TandemRepeatMode.class);
	private final String input;
	private final String output;
	private final String commandLine;
	private final int buffer;
	private final boolean isStrict2chrName;
	
	//for unit test only
	@Deprecated
	TandemRepeatMode( String input, String output, int buffer) {	
		this.input = input;
		this.output = output;
		this.commandLine = null;
		this.buffer = buffer;
		this.isStrict2chrName = true;
	}	
	
	public TandemRepeatMode( Options options) throws IOException{	
		input = options.getInputFileName();
		output = options.getOutputFileName();
		commandLine = options.getCommandLine();
		buffer = options.getBufferSize();
		this.isStrict2chrName = options.isStrict2chrName();
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("mask File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("accept ambiguous chromosome name, eg. treat M and chrMT as same chromosome name: " + (!isStrict2chrName));
        
		addAnnotation( options.getDatabaseFileName() );				
	}	
	
	static class Repeat {
//		final String chr;
	    final int start;
		final int end;
		final int patternLength;
		final int patternNo; 
		
		Repeat(String chr,int start, int end, int length, int no){
//			this.chr = chr;
			this.start = start;
			this.end = end;
			this.patternLength = length;
			this.patternNo = no;			
		}
		
		//convert line from dbfile and return a Repeat
		//chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
//		Repeat(String str) throws NumberFormatException{	
//			new Repeat(str.split(Constants.TAB_STRING) );	 			
//		}
		
		//convert line from dbfile and return a Repeat
		//chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC		
		Repeat(String[] array) throws NumberFormatException{				
 //           String[] array = str.split(Constants.TAB_STRING);
//			this.chr =  array[0] ; //keep original first
			this.start = Integer.parseInt(array[1]);
			this.end = Integer.parseInt(array[2]);
			this.patternLength = Integer.parseInt(array[3]);
			this.patternNo = (int) (Float.parseFloat(array[4]) + 0.5);			 			
		}		
		
		String printMark(){		return patternLength + "_" + patternNo;	 }
	}
	
	class Block {
		private final int start;
		private final int end;
		private final List<Repeat> repeats; 
		
		//not allow create repeats from outside
		private Block(int start, int end, List<Repeat> repeats){
			this.start = start;
			this.end = end;		
			this.repeats = repeats; 
		}
		
		Block(int start, int end){ this(start, end, new ArrayList<>());  }	
		
		void addRepeat(Repeat re){repeats.add(re); }
		int getStart() { return start; }
		int getEnd() { return end; }
		
		@Override
		public String toString(){			
			return String.format("%d ~ %d :: %d",start, end, repeats.size());
		}
		
		@Override
		public int hashCode() {
			final int prime = 31; 			
			int result = prime + start;
			result = prime * result + end;			 
			return result;
		}
		
	}

	class BlockIndex {
		final int firstBlockStart;
		final int lastBlockEnd;
		final Map<Integer, Block> index;
		
		BlockIndex(int s, int e, Map<Integer, Block> in){
			this.firstBlockStart = s;
			this.lastBlockEnd = e;
			this.index = in;
		}
	}
	
	@Override
	public void addAnnotation(String dbfile) throws IOException {
		//eg. chrMT => BlockIndex
		Map<String, BlockIndex> indexedBlock = loadRepeat(dbfile );  //S1	
		if(indexedBlock.size() == 0) return ; 
				 
		long count = 0;
		long repeatCount = 0; 
		try (VCFFileReader reader = new VCFFileReader(input) ;
            VCFFileWriter writer = new VCFFileWriter(new File(output))  ) {
			//reheader
		    VcfHeader hd = 	reader.getHeader();
		    hd = reheader(hd, commandLine ,input);			    
		    hd.addInfo(VcfHeaderUtils.INFO_TRF, "1", "String", VcfHeaderUtils.INFO_TRF_DESC); 
		    hd.addFilter(VcfHeaderUtils.FILTER_TRF, VcfHeaderUtils.FILTER_TRF_DESC );
		    
		    for(final VcfHeaderRecord record: hd) {
		    		writer.addHeader(record.toString());			
		    }
			logger.info("annotating vcfs from inputs " );
			
	        for (final VcfRecord vcf : reader) { 
				String chr = isStrict2chrName? vcf.getChromosome(): ChrPositionUtils.ChrNameConveter(vcf.getChromosome());

		        	//String vcfchr =  IndelUtils.getFullChromosome(vcf.getChromosome());	         
	 	    		if(annotate(vcf, indexedBlock.get(chr)))
	 	    			repeatCount ++; 	    			
		    		count++;
		    		writer.add(vcf);
	        }
		}  
					
		logger.info(String.format("outputed %d VCF records, including %d marked as TRF.",  count , repeatCount ));		
	}
	

	boolean annotate(VcfRecord vcf, BlockIndex indexedBlock ){
 			
		if(indexedBlock == null) return false; //no repeat marked on this chr
		 		 
		int start = vcf.getPosition() - buffer; //use vcf start it is may one base ahead	
		if(start > indexedBlock.lastBlockEnd ) return false; //not in repeat region
						
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		int end = (type.equals(SVTYPE.INS))? buffer + vcf.getPosition() + vcf.getAlt().length()-1 : buffer + vcf.getChrPosition().getEndPosition();
		if(end < indexedBlock.firstBlockStart) return false; //not in repeat region
				
		List<Block> coveredBlocks = new ArrayList<>();	
		Map<Integer, Block> indexMap = indexedBlock.index;
		//find first block
		Block block = null;		
		
		//skip base before repeat start
		//here the end must be repeat region otherwise already return false
		if(start < indexedBlock.firstBlockStart )
			start = indexedBlock.firstBlockStart;
 		
		//seek each base in a block gap region
		for(int i = 0; i <= BLOCK_INDEX_GAP; i ++)  {
			if((block = indexMap.get(start - i)) != null){
				coveredBlocks.add(block);
				break; // stop backwards to gap				
			}
		}
		 
		if( block == null) {
			logger.warn("error on indexed blocks, can't find closest index for " + vcf.toString());
			throw new IllegalArgumentException("error on indexed blocks, can't find closest index for " + vcf.toString());
		}
		
		start = block.end + 1; 
		while(start <= end){
			if(start > indexedBlock.lastBlockEnd ) break;
		 
			block = indexMap.get(start);
			if(block == null) {
				logger.warn("error on indexed blocks, can't find closest index for " + vcf.toString());
				throw new IllegalArgumentException("error on indexed blocks, can't find closest index for " + vcf.toString());
			}
			
			coveredBlocks.add(block);
			start = block.end + 1; 
		}
		
		Set<Repeat>coveredRepeatsSet = new HashSet<>();
		for (Block blo: coveredBlocks) {
			coveredRepeatsSet.addAll(blo.repeats);
		}
				
		if(coveredRepeatsSet.isEmpty()) return false; //do nothing
		
		/*
		 * unique the list
		 */
		List<Repeat> coveredRepeats = new ArrayList<>(coveredRepeatsSet);
		
		/*
		 * sort list so results are reproducable (for our lovely regression tests)
		 */
		coveredRepeats.sort(Comparator.comparingInt(c -> ((Repeat) c).patternLength).thenComparingInt(c -> ((Repeat)c).patternNo));
		
		float rate = -1;
		try{
			 rate = Float.parseFloat(vcf.getInfoRecord().getField(IndelUtils.INFO_SSOI));
		}catch(NullPointerException | NumberFormatException  e){} //do nothing			
		 
		boolean tRF_filter = false;
		String tRF_info = ""; //"TRF=" + coveredRepeats.get(0).printMark();
		for(int i = 0; i < coveredRepeats.size(); i++){
			Repeat rep = coveredRepeats.get(i);
			tRF_info +="," + rep.printMark();	
			
			//once find one TRF satisfied, the filter will be marked as TRF
			if(tRF_filter) continue; 
			
			//discard TRF inside indel(DEL) region that is smaller than DEL size			
			if(rep.start >= vcf.getPosition() && rep.end <= vcf.getChrPosition().getEndPosition() && 
					(rep.end - rep.start) < (vcf.getRef().length()-1))
				tRF_filter = false;
			else if( rep.patternLength < 6 && rep.patternLength > 1 && rep.patternNo > 10)				
				tRF_filter = true; //high frequence short TRF
			else if(rep.patternLength == 1 && rep.patternNo > 6)
				tRF_filter = true; //homoplymers
			else if ( (rep.patternLength < 6 &&  rep.patternLength > 0) 
					&&  (rate < 0.2 && rate >= 0 )) // must check 0 value in case the repeat or ssoi value not exist
				tRF_filter = true;  //low confidence short TRF
		}
		
		vcf.appendInfo("TRF=" + tRF_info.substring(1));
		if(tRF_filter){
			VcfUtils.updateFilter(vcf, "TRF");
			return true;
		}
		return false;
	}
	
	
	/**
	 * the key of the map is converted referenct name, value is the realted repeats. eg. chrMT => set<Repeat>
	 * @param dbfile: TRF repeat file: eg chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
	 * @return a list a  repeat region, each region related to a line from dbfile
	 * @throws IOException
	 */
	//Map<String, HashSet<Repeat>> loadRepeat(String dbfile) throws IOException {
	Map<String, BlockIndex> loadRepeat(String dbfile) throws IOException {
 		
		logger.info("loading into RAM...");		
		//eg. chrMT => set<repeat>		
		Map<String, HashSet<Repeat>> allRepeats = new HashMap<>();
		int errLine = 0;
		//s1: read whole file into Repeat list
        try(BufferedReader reader = new BufferedReader(new FileReader(dbfile))){
            String line; //chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
            while (( line = reader.readLine()) != null)  {
            	/*
            	 * ignore header line
            	 */
            	if (line.startsWith("chrom\tstart")) continue;
            		
	           try {
	        	   String[] array = line.split(Constants.TAB_STRING);
        	   		Repeat rep = new Repeat(array);
        	   		String chr = isStrict2chrName? array[0]: ChrPositionUtils.ChrNameConveter(array[0]);
        	   		allRepeats.computeIfAbsent(chr, (v) -> new HashSet<>()).add(rep);
	            } catch (NumberFormatException e) {
	            	if (errLine ++ < MaxErrLine) {
	            		logger.warn("can't retrive information from TRF repeat file: " + line);
	            	}
	            }             
            }
        } 
        logger.info( "reference number inside TRF data file is " + allRepeats.size());
        
        Map<String, BlockIndex> indexedBlock  = new HashMap<>();
		if(allRepeats.size() == 0) return indexedBlock; 
		
		int totalRepeat = 0; 
		int totalBlock = 0;	
		//eg. chrMT => BlockIndex
		for(String chr: allRepeats.keySet()){
			 logger.debug("indexing blocks for " + chr);
			 //use converted chromosome name as key if requires
			 indexedBlock.put(chr,  makeIndexedBlock( allRepeats.get(chr)));
			 totalRepeat += allRepeats.get(chr).size();
			 totalBlock += indexedBlock.get(chr).index.size();	 
		}        
		logger.info("total repeats from dbfile is " + totalRepeat);
		logger.info("total blocks from dbfile is " + totalBlock );
		
		return indexedBlock;      
        
	}
	
	
	/**
	 * 
	 * @param repeats: a list of repeats from loadRepeat(String dbfile)
	 * @return an indexed block list for each chromosome reference
	 */
	BlockIndex makeIndexedBlock(final Set<Repeat> repeats){
		
		if(repeats == null || repeats.size() == 0) return null; 
		 
			//S1: get unique block edge 
			Set<Integer> starts = new HashSet<>(); //list will be very slow
			Set<Integer> ends = new HashSet<>(); //list will be very slow
			for(Repeat rep : repeats){				 
				starts.add(rep.start);				
				ends.add(rep.end);
			}			
			
			//sort and unique the elements
		   TreeSet<Integer> sortedStartEnd = new TreeSet<>();
		    sortedStartEnd.addAll(starts);
		    sortedStartEnd.addAll(ends);
		    
		    //S2:create block for each start end but the edge may require one base shift
			Map<Integer, Block> blockIndex = new HashMap<>(); 
			Iterator<Integer> it = sortedStartEnd.iterator(); 
			int start = it.next();
			int end = start; 
			
		    while(it.hasNext()){
		    	
		    	//do nothing if exists from starts disregards whether it is in ends or not
		    	if(!starts.contains(start))
		    		start += 1; 
		    	
		    	//trim if end is some repeat start
		    	end = it.next();	
		    	int end0 = end;  //store original boundary for next loop
		    	 
		    	if(starts.contains(end))		    		 
		    		end -= 1; 
		    	
		    	if(start <= end)
		    		blockIndex.put(start,  new Block(start, end));
		    	
		    	//next block must be one base forward
		    	start = end0; 
		    }
		    
		    //S3: filling repeat 
		    for(Repeat rep : repeats){	
				start = rep.start;	
				//repeat must be greater than one base; 
				//if end overlaop with next repeat start, chop the end
				while(start < rep.end){
					//all repeat start is never been chopped
					Block block = blockIndex.get(start);
					block.addRepeat(rep);
					//next block must be one base forward
					start = block.end+1;						
				}
			}
		    
		    
		    //S4: each gap insert a index
	 	    Map<Integer, Block> inserts = new HashMap<>();
		    for(Block block:  blockIndex.values() ){		    	
			    	start = block.start;
			    	//throw null exception since some block is null
			    	while (block.end > BLOCK_INDEX_GAP + start){	    		
			    		start += BLOCK_INDEX_GAP;
			    		inserts.put(start , block);	
			    	}	    	
		    }
		    blockIndex.putAll(inserts);
		    
		    
		    return new BlockIndex(sortedStartEnd.first(), sortedStartEnd.last(), blockIndex);
	}

}
