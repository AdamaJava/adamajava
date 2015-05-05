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
	final String CADD = "CADD";
	
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
//					//debug bf clear
//					for( Entry<ChrPosition, VcfRecord> entry: positionRecordMap.entrySet()){
//						if(entry.getValue().getFilter() == null)
//							System.out.println(entry.getValue().toString());						
//					}
					
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
    		
    		if ( (null == inputVcf) || !inputVcf.getRef().equalsIgnoreCase(eles[2])) continue; 
    		
    		String[] allels = {inputVcf.getAlt()};
    		 if(inputVcf.getAlt().contains(","))
    			 allels = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
    			   		 
    		String cadd = "";
    		
    		//it will exit loop once find the matched allele
    		for(String al : allels)
    			if(al.equalsIgnoreCase(eles[4])){
    				cadd =	String.format("(%s=>%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s)", eles[2],eles[4],eles[8],eles[10],eles[11],eles[12],eles[17],
    						eles[21],eles[26],eles[35],eles[39],eles[72],eles[82],eles[83],eles[86],eles[92],eles[92],eles[93],eles[96]);  
    				String info = inputVcf.getInfoRecord().getField(CADD);
    				info = (info == null)? CADD + "=" + cadd : CADD + "=" + info + "," + cadd;
    				inputVcf.appendInfo( info);
    				
    				writer.append(inputVcf.toString() + "\n");
    				outputSize ++;
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

