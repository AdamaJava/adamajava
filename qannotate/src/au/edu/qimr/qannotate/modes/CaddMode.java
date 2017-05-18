/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import htsjdk.tribble.readers.TabixReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;


public class  CaddMode extends AbstractMode{
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	private final static String CADD = "CADD";
	
	static long outputNo = 0;
	static long blockNo = 0;	
	static long inputNo = 0;
	private final QLogger logger= QLoggerFactory.getLogger(CaddMode.class);
	
	String description = "query CADD library for this variant. Format: (Ref>Alt|isDerived|Consequence|ConsScore|ConsDetail|scoreSegDup|priPhyloP|GerpRS|mirSVR-E|cHmmTssA|motifDist|ESP_AFR|ESP_EUR|TG_AMR|FeatureID|FeatureID|CCDS|relcDNApos)";	
		
	public CaddMode( Options options) throws Exception {		
		
		final String input = options.getInputFileName();
		final File output =  new File( options.getOutputFileName() );
		final int gap = options.getGapSize();
		
		final String[] database = options.getDatabaseFiles();
		final TabixReader[] tabixs = new TabixReader[database.length];
		for(int i = 0; i < database.length; i ++)
			tabixs[i] = new TabixReader( database[i] );
		
		
		String chr = null;
		int pos = 0; 
		int start = 1;
		
		try (VCFFileReader reader = new VCFFileReader(input);
				VCFFileWriter writer = new VCFFileWriter( output)){
			
			//reheader first
        	reheader(options.getCommandLine(),options.getInputFileName())	;
        	header.addInfo(VcfHeaderUtils.INFO_CADD, "1", "String", description);
        	for (final VcfHeaderRecord record: header)  {
        		writer.addHeader(record.toString());
        	}

			//read chrunk and annotateion	 
			for (final VcfRecord re : reader){ 
				//annotation
				if( !re.getChromosome().equals(chr) || (re.getPosition() - pos) > gap){
					//s1: annotate variants in hash map
					if(chr != null) 
						addAnnotation( chr, start-1, pos + 1, tabixs, writer );
						
					//s2: reset	hash map				
					positionRecordMap.clear();
					chr = re.getChromosome();
					start = re.getPosition();						
				}
				
				//add every variants into hashmap
				pos = re.getPosition();
				add2Map(re); 
			}
			
			//last block
			if(chr != null)
		    		addAnnotation( chr, start-1, pos+1 , tabixs, writer );			
		}//end try	

		logger.info("total input variants is  " + inputNo);		 
		logger.info("total annotated variants is  " + outputNo);
		logger.info("total query CADD library time is " + blockNo);
	}
	
	private void addAnnotation(String chr, int start, int end, TabixReader[] tabixs, VCFFileWriter writer) throws Exception {
		
	    	String line; 
	    	String[] eles;
	    	String last = null;
	
	    	int blockSize = 0; 
	    	int outputSize = 0;
	    	if(chr.startsWith("chr"))  chr = chr.substring(3);
	    	if(chr.equalsIgnoreCase("m")) chr = "MT"; 

    	
		for(TabixReader tabix : tabixs){			
			TabixReader.Iterator it = tabix.query(chr, start, end);
			while(( line = it.next())!= null){
				blockSize ++;  
				
				//only retrive the first annotation entry from CADD library
	    			eles = TabTokenizer.tokenize(line, '\t');	
	 			String entry =  eles[0] + ":" + eles[1] + ":" +eles[2]+ ":" +  eles[4]; //chr:pos:ref:allel
	 			if (entry.equals(last)) {
	 				continue;
	 			} else {
	 				last = entry;
	 			}
	 			
	
		    		int s = Integer.parseInt(eles[1]);  //start position = second column
		    		int e = s + eles[2].length() - 1;   //start position + length -1
		    		
		    		List<VcfRecord> inputVcfs = positionRecordMap.get(new ChrRangePosition(chr, s, e ));	    
				if ( (null == inputVcfs) || inputVcfs.size() == 0 ) {
					continue; 
				}
				for(VcfRecord inputVcf: inputVcfs ){
					if ( ! inputVcf.getRef().equalsIgnoreCase(eles[2]))  {
						continue;
					}
				
								
					String[] allels = {inputVcf.getAlt()};
			    		if(inputVcf.getAlt().contains(",")) {
			    			 allels = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
			    		}
		    		
			    		for(String al : allels) {
			    			if(al.equalsIgnoreCase(eles[4])){
			    				String cadd =	String.format("(%s>%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s)", eles[2],eles[4],eles[8],eles[10],eles[11],eles[12],eles[17],
			    						eles[21],eles[26],eles[35],eles[39],eles[72],eles[82],eles[83],eles[86],eles[92],eles[92],eles[93],eles[96]);  
			    				String info = inputVcf.getInfoRecord().getField(CADD);
			    				info = (info == null)? CADD + "=" + cadd : CADD + "=" + info + "," + cadd;
			    				inputVcf.appendInfo( info);
			    				outputSize ++;
			    			}
			    		}
		    		}
			}
		}
		
		//output
		final List<ChrPosition> orderedList = new ArrayList<>(positionRecordMap.keySet());
		orderedList.sort(COMPARATOR);
		for (final ChrPosition position : orderedList)  {
			for(VcfRecord re: positionRecordMap.get(position)) {
				writer.add( re );	
			}
		}
			 			
		//get stats   			
		logger.debug(String.format("%8d: query(%s, %8d, %8d) [ %8d,%8d,%8d ] ", blockNo++, chr, start, end, blockSize, positionRecordMap.size(), outputSize ));			
	    	inputNo += positionRecordMap.size();   	
	    	outputNo += outputSize;
	}
		
	/**
	 * it remove "chr" and change "chrM" to "MT"  for reference name 
	 * @param re input vcf record
	 */
	private void add2Map(VcfRecord re){
		ChrPosition pos = re.getChrPosition();
		String chr = pos.getChromosome();	
		boolean change = false; 
	    	if(chr.startsWith("chr"))  	{
	    		chr = chr.substring(3);
	    		change = true;
	    	}
	    	if(chr.equalsIgnoreCase("m")) {
	    		chr = "MT";
	    		change = true;
	    	}
		
		if(change) {
			pos =  new ChrRangePosition(chr, re.getChrPosition().getStartPosition(), re.getChrPosition().getEndPosition());
		}
		
		positionRecordMap.computeIfAbsent(pos, v -> new ArrayList<VcfRecord>()).add(re);
	}
		
		@Override
		void addAnnotation(String database) throws Exception { }
}
