package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.options.GeneralOptions;


public class TandemRepeatMode  extends AbstractMode{
	static final int BLOCK_INDEX_GAP = 100; //insert index for big block
	static final int MaxErrLine = 100; 
	
	private final QLogger logger = QLoggerFactory.getLogger(TandemRepeatMode.class);
	String input;
	String output;
	final String commandLine;
	
	private HashMap<String, HashMap<Integer, Block>> genomeRepeat = new HashMap<String, HashMap<Integer, Block>>();
	
//	@Deprecated
//	public TandemRepeatMode(){
//		input = null;
//		output = null;
//		commandLine = null;		
//	}
	
	public TandemRepeatMode( GeneralOptions options) throws Exception{	
		input = options.getInputFileName();
		output = options.getOutputFileName();
		commandLine = options.getCommandLine();
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("mask File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
          
		addAnnotation(options.getDatabaseFileName() );				
	}	
	
	

	
	private class Repeat {
		final String chr;
	    final int start;
		final int end;
		final int patternLength;
		final int patternNo; 
		
		Repeat(String chr, int start, int end, int length, int no){
			this.chr = chr;
			this.start = start;
			this.end = end;
			this.patternLength = length;
			this.patternNo = no;			
		}
		
		//convert line from dbfile and return a Repeat
		//chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
		Repeat(String str) throws NumberFormatException{				
            String[] array = str.split(Constants.TAB+"");
			this.chr = array[0].toLowerCase();
			this.start = Integer.parseInt(array[1]);
			this.end = Integer.parseInt(array[2]);
			this.patternLength = Integer.parseInt(array[3]);
			this.patternNo = (int) (Float.parseFloat(array[4]) + 0.5);
			 			
		} 	
		
		String printMark(){return patternLength + "_" + patternNo; }
		
	}
	
	private class Block {
		private final int start;
		private final int end;
		private List<Repeat> repeats; 
		
		//not allow create repeats from outside
		private Block(int start, int end, List<Repeat> repeats){
			this.start = start;
			this.end = end;		
			this.repeats = repeats; 
		}
		
		Block(int start, int end){
			this(start, end, new ArrayList<Repeat>()); 
		}	
		
		Block reset(int start, int end){			 							
			return new Block(start, end, this.repeats); 
		}
		
		void addRepeat(Repeat re){repeats.add(re); }
	
//		List<Repeat> getRepeats(){ return repeats; }
		
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

	private class BlockIndex {
		final int firstBlockStart;
		final int lastBlockEnd;
		final HashMap<Integer, Block> index;
		
		BlockIndex(int s, int e, HashMap<Integer, Block> in){
			this.firstBlockStart = s;
			this.lastBlockEnd = e;
			this.index = in;
		}
	}
	
	@Override
	public void addAnnotation(String dbfile) throws Exception {
		
		//System.out.println("loading repeat file...");
		List<Repeat> repeats = loadRepeat(dbfile);  //S1
		 
		 //System.out.println("creating block ...");
		 //here key are chr name with low case
		 HashMap<String, BlockIndex> indexedBlock = makeIndexedBlock( repeats);   //S2
		 
//		 HashMap<String, TreeSet<Integer>> blockStarts = new HashMap<String, TreeSet<Integer>>();   //S2 
//		 for(String chr : indexedBlock.keySet()){
//			//sort  ??? do it once outside this function
//			TreeSet<Integer> sortedStart = new TreeSet<Integer>();
//			sortedStart.addAll(indexedBlock.get(chr).keySet());		
//			blockStarts.put(chr, sortedStart);			 
//		 }
 
		long count = 0;
		long repeatCount = 0; 
	 
		try (VCFFileReader reader = new VCFFileReader(input) ;
            VCFFileWriter writer = new VCFFileWriter(new File(output ))  ) {
			    
			//reheader
		    VcfHeader hd = 	reader.getHeader();
		 //   hd.addFilterLine(FILTER_REPEAT, DESCRITPION_FILTER_REPEAT );       	  
		//    hd.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "1", "String", DESCRITPION_INFO_CONFIDENCE);		    
		    hd = reheader(hd, commandLine ,input);			    	  
	
		    for(final VcfHeader.Record record: hd)  
		    	writer.addHeader(record.toString());
		
	        for (final VcfRecord vcf : reader) {   
	        	String key = vcf.getChromosome().toLowerCase();
 	    		annotate(vcf, indexedBlock.get(key));
 	    		
	    		count++;
	    		writer.add(vcf);
	        }
		}  
	//	logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  count , posCheck.size()));
		logger.info("number of variants fallen into repeat region is " + repeatCount);	 
		
	}
	
	void annotate(VcfRecord vcf, BlockIndex indexedBlock ){
 			
		if(indexedBlock == null) return; //no repeat marked on this chr
		 		 
		int start = vcf.getPosition(); //use vcf start it is may one base ahead	
		if(start > indexedBlock.lastBlockEnd ) return; //not in repeat region
				
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());			
		int end = (type.equals(SVTYPE.INS))? start+vcf.getAlt().length()-1 : vcf.getChrPosition().getEndPosition();
		if(end < indexedBlock.firstBlockStart) return; //not in repeat region
		
		List<Block> coveredBlocks = new ArrayList<Block>();	
		HashMap<Integer, Block> indexMap = indexedBlock.index;
		//find first block
		Block block = null;
 		
		//exception: vcf start at 5757828, block 5757428~5757828; 
		//so it seek from indexMap.get(5757828) ~ indexMap.get(5757828 - BLOCK_INDEX_GAP + 1)
		//it missed indexMap.get(5757828 - BLOCK_INDEX_GAP)
		
		for(int i = 0; i <= BLOCK_INDEX_GAP; i ++) 
			if((block = indexMap.get(start - i)) != null){
				coveredBlocks.add(block);
				break; // stop backwards to gap				
			}	
		 
		if(block == null)
			logger.warn("error on indexed blocks, can't find closest index for " + vcf.toString());		
		start = block.end + 1; 
		while(start <= end){
			if(start > indexedBlock.lastBlockEnd ) break;
		 
			block = indexMap.get(start);
			if(block == null)
				logger.warn("error on indexed blocks, can't find closest index for " + vcf.toString());
		 
			coveredBlocks.add(block);
			start = block.end + 1; 
		}
		
		List<Repeat> coveredRepeats = new ArrayList<Repeat>();
		for(Block blo: coveredBlocks)
			if(!blo.repeats.isEmpty())
				for(Repeat rep : blo.repeats)
					if(!coveredRepeats.contains(rep))
						coveredRepeats.add(rep);
				
		if(coveredRepeats.isEmpty()) return; //do nothing
		
		boolean TRF_filter = false;
		String TRF_info = "TRF=" + coveredRepeats.get(0).printMark();
		for(int i = 1; i < coveredRepeats.size(); i++){
			TRF_info +=":" + coveredRepeats.get(i).printMark();
			
			if(coveredRepeats.get(i).patternLength > 1 && 
					coveredRepeats.get(i).patternLength < 6 &&
					coveredRepeats.get(i).patternNo > 10)
				TRF_filter = true;
		}
		
		vcf.appendInfo(TRF_info);
		if(TRF_filter)
			VcfUtils.updateFilter(vcf, "TRF");
	}
	
	
	/**
	 * 
	 * @param dbfile: TRF repeat file: eg chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
	 * @return a list a  repeat region, each region related to a line from dbfile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private List<Repeat> loadRepeat(String dbfile) throws FileNotFoundException, IOException {
		int errLine = 0;
		//s1: read whole file into Repeat list
		List<Repeat> gRepeats = new ArrayList<Repeat>();
        try(BufferedReader reader = new BufferedReader(new FileReader(dbfile))){
            String line; //chr1    11114   11123   5       2.0     5       100     0       20      0.97    GGCGC
            while (( line = reader.readLine()) != null)  
	           try{
	        	   gRepeats.add(new Repeat(line));
	            }catch(NumberFormatException e){
	            	if(errLine ++ < MaxErrLine)
	            		logger.warn("can't retrive information from TRF repeat file: " + line);
	            	continue;
	            }             
        } 
        
        return gRepeats; 		
	}
	
	
	/**
	 * 
	 * @param blockIndex: an initial indexed block list but the edge is not clear
	 * @return an indexed block list for a single chromosome reference
	 */
	private BlockIndex resetIndexedBlock(final int firstStart, final int lastEnd,  final HashMap<Integer, Block> blockIndex ){
		
		HashMap<Integer, Block> resetIndex = new HashMap<Integer, Block>();
	    
	   //S4: reset index by shifting block edge
	    Set<Block> blocks =  new HashSet<Block> (blockIndex.values()); //unique
	    for(Block block : blocks){
	    	
	    	//shift (+1) unless one of repeat region start is same with block start 
	    	// if no repeat must shift (+1)
	    	boolean shift = true;   //must shift for empty repeats		 
	    	for(Repeat rep : block.repeats)
	    		if(rep.start == block.start){
	    			shift = false;
	    			break;
	    		}
	    	 
	    	int start = (shift)? block.start + 1 : block.start;	    	
	    	
	    	//shift (-1) unless one of repeat region end is same with block end
	    	shift = true; 		    	
	    	for(Repeat rep : block.repeats)
	    		if(rep.end == block.end){
	    			shift = false;
	    			break;
	    		}
	    	int end = (shift)? block.end - 1 : block.end;	
	    	
	    	if(end < start ) continue; //skip this empty region
	    	
	    	block = block.reset(start, end);
	    	resetIndex.put(start, block);
	    } //end of S4
	    
	    logger.info("trimming block end if overlap with next block");
	    	    	    
	    //trim each block end if overlap with next block start
	    for(Block block:  resetIndex.values() ){    		   
	    	if(block.end > lastEnd)
	    		logger.warn("this block end is over repeat region: " + block.toString() );
	    	else if( block.end != lastEnd && resetIndex.get( block.end + 1) == null ){
		    	block = block.reset(block.start, block.end-1);
		    	resetIndex.replace(block.start, block);
		    	if(resetIndex.get(block.end + 1) == null)
		    		logger.warn("trimed one base still can't find next block: " + block.toString() );
		    }
	    	
	
	    }
	    logger.info("trimming block end done!");
	    logger.info("total hash map entry number: " + resetIndex.size());
	    logger.info("total block number: " + resetIndex.values().size());
	    logger.info("repeat frist start ~ last end: " + firstStart + " ~ " + lastEnd);
	    
	    
	    //S5: each gap insert a index
 	    HashMap<Integer, Block> inserts = new HashMap<Integer, Block>();
	    for(Block block:  resetIndex.values() ){
	    	int start = block.start;
	    	while (block.end > BLOCK_INDEX_GAP + start){	    		
	    		start += BLOCK_INDEX_GAP;
	    		inserts.put(start , block);	
	    	}	    	
	    }
 	    resetIndex.putAll(inserts);
	    
	    return new BlockIndex(firstStart, lastEnd, resetIndex);
		
	}
	
	/**
	 * 
	 * @param repeats: a list of repeats from loadRepeat(String dbfile)
	 * @return an indexed block list for each chromosome reference
	 */
	private HashMap<String, BlockIndex> makeIndexedBlock(final List<Repeat> repeats){
		HashMap<String, BlockIndex> genomeBlocks = new HashMap<String, BlockIndex>();
		
		//split to each reference
		List<String> chrs = new ArrayList<String>();
		for(Repeat rep : repeats)
			if(!chrs.contains(rep.chr))
				chrs.add(rep.chr);
		
		for(String chr : chrs){
			//S1: get unique block edge 
			HashSet<Integer> startEnd = new HashSet<Integer>(); //list will be very slow
			for(Repeat rep : repeats)
				if(rep.chr.equals(chr)){					
						startEnd.add(rep.start);				
						startEnd.add(rep.end);
				}			
			//sort
		    TreeSet<Integer> sortedStartEnd = new TreeSet<Integer>();
		    sortedStartEnd.addAll(startEnd);
		    
		    //S2:create block for each start end but the edge may require one base shift
			HashMap<Integer, Block> blockIndex = new HashMap<Integer, Block>(); 
			Iterator<Integer> it = sortedStartEnd.iterator();
			int start = sortedStartEnd.first();
			int end = sortedStartEnd.last();
		    while(it.hasNext()){
		    	end = (int) it.next();
		    	if(start < end)
		    		blockIndex.put(start,  new Block(start, end));
		    	start = end; 
		    }
		    
		    //S3: filling repeat 
		    for(Repeat rep : repeats)
				if(rep.chr.equals(chr)){	
					start = rep.start;					
					while(start < rep.end){
						Block block = blockIndex.get(start);
						block.addRepeat(rep);
						start = block.end;						
					}
				}
			BlockIndex resetB = resetIndexedBlock(sortedStartEnd.first(), sortedStartEnd.last(),  blockIndex);
					 
		    genomeBlocks.put( chr, resetB);
		} 
			
		return genomeBlocks;
		
	}
	
}
