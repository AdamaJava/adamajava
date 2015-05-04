package org.qcmg.qmule;
 

import htsjdk.tribble.readers.TabixReader;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.vcf.VCFFileReader;


public class QueryCADDLib {
//	protected final static ArrayList<Integer> libBlocks = new ArrayList<>();
//	protected final static ArrayList<Integer> inputBlocks = new ArrayList<>();
//	protected final static ArrayList<Integer> outputBlocks = new ArrayList<>();

	protected final static Map<ChrPosition,VcfRecord> positionRecordMap = new HashMap<ChrPosition,VcfRecord>();
	protected static long outputNo = 0;
	protected static long blockNo = 0;	
	protected static long inputNo = 0;
	
	public QueryCADDLib(final String input_gzip_file,  final String vcf, final String output, final int gap) throws IOException{
		
		TabixReader tabix = new TabixReader( input_gzip_file);
		String chr = null;
		int pos = 0; 
		int start = -1;
		
		System.out.println("Below is the stats for each queried block, follow the format \norder: query(ref,start,end) [CADDLibBlockSize, inputVariantNo, outputVariantNo, runtime]");
		
		try (VCFFileReader reader = new VCFFileReader(vcf);
				FileWriter writer = new FileWriter(new File(output))) {
			for (final VcfRecord re : reader){ 
				if(re.getChromosome().equals(chr) &&
						(re.getPosition() - pos) < gap ){
						pos = re.getPosition();
						add2Map(re);
				}else{							
					//s1: query(chr:start:pos), and output
					if(chr != null){
				    	if(chr.startsWith("chr"))  	chr = chr.substring(3);
						TabixReader.Iterator it = tabix.query(chr, start-1, pos);
						//debug	
						System.out.print(String.format("%8d: query(%s, %8d, %8d) ", blockNo++, chr, start, pos));						
						query( it, writer );

					}
					//s2: reset
					
					//debug bf clear
					for( Entry<ChrPosition, VcfRecord> entry: positionRecordMap.entrySet()){
						if(entry.getValue().getFilter() == null)
							System.out.println(entry.getValue().toString());
						
					}
					
					positionRecordMap.clear();
					chr = re.getChromosome();
					start = re.getPosition();
					pos = re.getPosition();
					add2Map(re);
				}
			}
			//last block
			if(chr != null){
		    	if(chr.startsWith("chr"))  	chr = chr.substring(3);
				TabixReader.Iterator it = tabix.query(chr, start, pos);
				query( it, writer );

			}
			
			
		}//end try	
		
//		int totalInput = 0;
//		int totalOutput = 0;
//		
//		for(int i = 0; i < libBlocks.size(); i ++){
//			totalOutput += outputBlocks.get(i);
//			totalInput += inputBlocks.get(i);
//			System.out.print(String.format(", [ %d: %d,%d,%d]", i, libBlocks.get(i), inputBlocks.get(i), outputBlocks.get(i)));
//			
//		}
		
		
		 System.out.println("total input variants is  " + inputNo);
		 System.out.println("total outputed and annotated variants is  " + outputNo);
		 System.out.println("total query CADD library time is " + blockNo);
		 
	}
	
	/**
	 * it remove "chr" string from reference name if exists
	 * @param re input vcf record
	 */
	private void add2Map(VcfRecord re){
		ChrPosition chr = re.getChrPosition();
		if(chr.getChromosome().startsWith("chr"))
			chr =  new ChrPosition(re.getChromosome().substring(3), re.getChrPosition().getPosition(), re.getChrPosition().getEndPosition());   // orig.getChromosome().substring(3);
				
		
		re.setFilter(null); //for debug
		positionRecordMap.put(chr, re);			
	}
	
	
    private  void query(TabixReader.Iterator it,FileWriter writer ) throws IOException{
    	long startTime = System.currentTimeMillis();
    	
    	String line; 
    	String[] eles;
    	String last = null;

    	int blockSize = 0; 
    	int outputSize = 0;
    	
    	while(( line = it.next())!= null){
    		blockSize ++;  
    		eles = TabTokenizer.tokenize(line, '\t');
    		int s = Integer.parseInt(eles[1]);  //start position = second column
    		int e = s + eles[2].length() - 1;   //start position + length -1

    		//only retrive the first annotation entry from CADD library
 			String entry =  eles[0] + ":" + eles[1] + ":" +eles[2]+ ":" +  eles[4];
 			if(entry.equals(last)) continue;
 			else last = entry;

    		VcfRecord inputVcf = positionRecordMap.get(new ChrPosition(eles[0], s, e ));	     		
    		//walk through queried chrunk until find matched variants
//			if ( (null == inputVcf) || !inputVcf.getRef().equalsIgnoreCase(eles[2]) ||  
//					!inputVcf.getAlt().equalsIgnoreCase(eles[4]))  
//				continue;
    		
    		if ( (null == inputVcf) || !inputVcf.getRef().equalsIgnoreCase(eles[2])) continue; 
    		
    		String[] allels = {inputVcf.getAlt()};
    		 if(inputVcf.getAlt().contains(","))
    			 allels = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
    			
    		for(String al : allels)
    			if(al.equalsIgnoreCase(eles[4])){
    				//output found variant, since CADD only output found variant too
    				writer.append(line + "\n");
    				outputSize ++;
    				inputVcf.setFilter("CADD");
    			}
    	}
    	    	
       	//get stats   	
		long endTime = System.currentTimeMillis();
		String time = QLogger.getRunTime(startTime, endTime);	  
		System.out.println(String.format("[ %8d,%8d,%8d, %s ] ",  blockSize, positionRecordMap.size(), outputSize, time));
    	inputNo += positionRecordMap.size();   	
    	outputNo += outputSize;
    	
    }
	
	 
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		try{
			String gzlib = args[0];
			String input = args[1];
			String output = args[2];
			int gap = 1000;
			if(args.length > 3)
				gap = Integer.parseInt(args[3]);
			
			new QueryCADDLib(gzlib, input, output, gap);
			
		}catch(Exception e){
			e.printStackTrace();		 
			System.err.println("Usage: java -cp qmule-0.1pre.jar QueryCADDLib <CADD GZ lib>  <input vcf> <output> <chrunk gap (integer)>");			
		}		
		
		long endTime = System.currentTimeMillis();
		 String time = QLogger.getRunTime(startTime, endTime);	  
		 System.out.println("run Time is " + time);
	}
}


//	static void getUniqPosition(String input_gzip_file,   String output_file) throws IOException{
//
//	TabixReader tabix = new TabixReader( input_gzip_file);
//	Set<String> chrs = tabix.getChromosomes();
////	HashSet<String> uniqVariant = new HashSet<String>();
//	long total_uniq = 0; 
//	long total_number = 0; 
//	
//	String line;
//	System.out.println("total reference number is " + chrs.size() + " from " + input_gzip_file);
//	
//	try( FileWriter writer = new FileWriter(new File(output_file)) ){  		
//		//get header line
//		while( (line = tabix.readLine()) != null) 
//			 if(line.startsWith("#"))  writer.append(line+"\n");
//			 else break;
//     		 
//		//query each chrome to avoid out off memory
//		for(String str : chrs){
////			uniqVariant.clear();
//			String entry = null;
//			long num = 0;	
//			TabixReader.Iterator it = tabix.query(str);			 
//			while(( line = it.next())!= null){
//				total_number ++;
//				String[] eles = TabTokenizer.tokenize(line, '\t');
//				String pos =  eles[1]+ ":" +eles[2]+ ":" +  eles[4] ;
//				if(!pos.equalsIgnoreCase(entry) ){
//					entry = pos;
//					writer.append(line+"\n");
//					num ++;
//				} 
//			} 
// 			total_uniq += num; 
//			System.out.println("There are " + num + " uniq variants recorded in reference " + str);		
//		} 
//	}	
//	System.out.println("Total uniq variants recorded in all reference is " + total_uniq);
//	System.out.println("Total records in whole file is " +total_number);
//	
//}
