package au.edu.qimr.indel.pileup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;


public class ReadIndels {
	QLogger logger; 
	private VcfHeader header; 
 
	//here key will be uniq for indel: chr, start, end, allel 
	private static final  Map<ChrPosition,VcfRecord> positionRecordMap = new  ConcurrentHashMap<ChrPosition,VcfRecord>();
	
 

	//for testing only
	ReadIndels(){}
	
	public ReadIndels( QLogger logger){
		this.logger = logger; 		
	}

	
	public void appendIndels(File f) throws IOException{
		
		//clear second sample column of tumour vcf input
		for(ChrPosition pos : positionRecordMap.keySet()){
			VcfRecord vcf = positionRecordMap.get(pos);
			List<String> format = vcf.getFormatFields();
			while(format != null && format.size() > 2)
					format.remove(format.size()-1);   
			vcf.setFormatFields(format);
			VcfUtils.addMissingDataToFormatFields(positionRecordMap.get(pos), 2);
			
		}	
			
		logger.info("only keep first sample column of tumour input vcf." );
		
        try (VCFFileReader reader = new VCFFileReader(f)) {

        	header = VcfHeaderUtils.mergeHeaders(header, reader.getHeader(), false);
        	//merge variants
           	int inLines = 0;
        	int indelCounts = 0;
        	int indelnewCount = 0;
        	 int overlapCount= 0;
			for (final VcfRecord re : reader) {	
				inLines ++;
    			String StrAlt = re.getAlt(); 
    			if( StrAlt.contains(",")){ 
    				for(String alt : StrAlt.split(",")){
    					SVTYPE type = IndelUtils.getVariantType(re.getRef(), alt);
		 	        	if(type.equals(SVTYPE.DEL) ||type.equals(SVTYPE.INS) ){
		 	        		indelCounts ++;
		 	        		VcfRecord vcf1 = new VcfRecord(re.toString().trim().split("\t"));
         					vcf1.setAlt(alt);
         					if(!mergeIndel(vcf1))
         						indelnewCount ++;
         					else overlapCount ++; 
		 	        		//merger tumour with normal		 	        	 
		 	        		break; // go to next variants
		 	        	}
    				}  		 
    			}else{
    				SVTYPE type = IndelUtils.getVariantType(re.getRef(), re.getAlt());
	 	        	if(type.equals(SVTYPE.DEL) ||type.equals(SVTYPE.INS) ){ 
	 	        		indelCounts ++;
     					if(!mergeIndel(re)) indelnewCount ++;
     					else overlapCount ++; 
	 	        		
 	 	        	}
    			}
			}
			
			logger.info(String.format("Find %d indels from %d variants within file: %s", indelCounts, inLines, f));
			logger.info(indelnewCount + " indel variants are only appeared in second vcf! ");
			logger.info(overlapCount + " indel variants are appeared in both input vcf! ");			
			logger.info(positionRecordMap.size() + " indel variants position are selected into output! ");
        }
	}	
	
	/**
	 * @param indels a list of indels with same position
	 * @param pos: a new indel with same position
	 * @return true: if the input vcf merged into existing vcf which from first input vcf file
	 */
	public boolean mergeIndel(  VcfRecord vcf){
		
 		ChrPosition pos = new ChrPosition(vcf.getChromosome(), vcf.getPosition(), vcf.getChrPosition().getEndPosition(), vcf.getAlt());     
		VcfRecord existingvcf = positionRecordMap.get(pos);
		//new indel
		
		if(existingvcf == null){
			//insert missing data to first format column, shift original first column to second
			VcfUtils.addMissingDataToFormatFields(vcf, 1);			
 			List<String> informat = vcf.getFormatFields();
 			List<String> outformat  = new ArrayList<String>();
 			outformat.add(0,informat.get(0));
 			outformat.add(1, informat.get(1));
 			outformat.add(2, informat.get(2));
 			vcf.setFormatFields(outformat);			
			positionRecordMap.put(pos, vcf);
			return false;
		}
		
		List<String> outformat  = new ArrayList<String>();;
		outformat.add(0, existingvcf.getFormatFields().get(0));
		outformat.add(1, existingvcf.getFormatFields().get(1));
		outformat.add(2, vcf.getFormatFields().get(1));
		existingvcf.setFormatFields(outformat); 
 
		return true; 
	}	
	/**
	 * if multi variants with same start position and ref allel, then only the last one will be kept
	 * @param f: vcf input file
	 * @param sampleCode: replace sample code inside the input vcf file 
	 * @throws IOException
	 */
	public void LoadSingleIndels(File f) throws IOException{
		
        //read indel record into RAM, 
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();	  	 
        	//no chr in front of position
        	int inVariants = 0;
        	int inLines = 0;
			for (final VcfRecord re : reader) {	
				inLines ++;
    			String StrAlt = re.getAlt(); 
    			if( StrAlt.contains(",")){ 
    				//here we don't split variants if ref same
    				for(String alt : StrAlt.split(",")){
    					inVariants ++;
    					SVTYPE type = IndelUtils.getVariantType(re.getRef(), alt);
		 	        	if(type.equals(SVTYPE.DEL) ||type.equals(SVTYPE.INS) ){
		 	        		VcfRecord vcf1 = new VcfRecord(re.toString().trim().split("\t"));
         					vcf1.setAlt(alt);
         					ChrPosition pos = new ChrPosition(vcf1.getChromosome(), vcf1.getPosition(), vcf1.getChrPosition().getEndPosition(), alt);         					 
         					positionRecordMap.put(pos, vcf1);					 
		 	        	}
    				}  		 
    			}else{
    				inVariants ++;
    				SVTYPE type = IndelUtils.getVariantType(re.getRef(), re.getAlt());
	 	        	if(type.equals(SVTYPE.DEL) ||type.equals(SVTYPE.INS) ){
     					ChrPosition pos = new ChrPosition(re.getChromosome(), re.getPosition(), re.getChrPosition().getEndPosition(), re.getAlt());         					 
     					positionRecordMap.put(pos, re);
	 	        	}  	 	        		 
    			}    			
 			}
			
			logger.info(String.format("Find %d indels from %d variants (%d records lines) within file: %s",
					positionRecordMap.size(), inVariants, inLines, f.getAbsoluteFile()));
		} 
	}
	
	
	public Map<ChrPosition, IndelPosition> getIndelMap() throws Exception{		
		Map<ChrPosition,IndelPosition> indelPositionMap = new  ConcurrentHashMap<ChrPosition,IndelPosition>();
		for(ChrPosition pos : positionRecordMap.keySet()){
			VcfRecord vcf =  positionRecordMap.get(pos);
			ChrPosition indelPos = new ChrPosition(pos.getChromosome(), pos.getPosition(), pos.getEndPosition());
			if(indelPositionMap.containsKey(indelPos)) 
				indelPositionMap.get(indelPos).addVcf( vcf );
 			  else 
				indelPositionMap.put(indelPos, new IndelPosition(vcf));
		}		
		return indelPositionMap;
	}
	
	public VcfHeader getVcfHeader(){ return header;	}
}
