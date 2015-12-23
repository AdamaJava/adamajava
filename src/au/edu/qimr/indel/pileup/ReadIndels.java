package au.edu.qimr.indel.pileup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.Main;


public class ReadIndels {
	QLogger logger; 
	private VcfHeader header; 
 
	//here key will be uniq for indel: chr, start, end, allel 
	private final  Map<ChrPosition,VcfRecord> positionRecordMap = new  ConcurrentHashMap<ChrPosition,VcfRecord>();

	public ReadIndels( QLogger logger){
		this.logger = logger; 		
	}
	
	/**
	 * merge first sample column to existing variants which is stored on hash map
	 * @param f: vcf file
	 * @throws IOException
	 */
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
    	//merge variants
       	int inLines = 0;
    	int inVariants = 0;   	
    	int indelnewCount = 0;
    	int overlapCount= 0;
	
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	header = VcfHeaderUtils.mergeHeaders(header, reader.getHeader(), false);
			for (final VcfRecord re : reader) {	
				inLines ++;
    			String StrAlt = re.getAlt(); 
    			
				for(String alt : StrAlt.split(",")){
					inVariants ++;
					SVTYPE type = IndelUtils.getVariantType(re.getRef(), alt);
	 	        	if(type.equals(SVTYPE.DEL) ||type.equals(SVTYPE.INS) ){	 	        		 
	 	        		VcfRecord vcf1 = new VcfRecord(re.toString().trim().split("\t"));
     					vcf1.setAlt(alt);
     					if(!mergeIndel(vcf1))
     						indelnewCount ++;
     					else overlapCount ++; 				 
	 	        	}
				}  	    			
			}
		}
		logger.info(String.format("Find %d indels from %d variants within file: %s", inVariants, inLines, f));
		logger.info(indelnewCount + " indel variants are only appeared in second vcf! ");
		logger.info(overlapCount + " indel variants are appeared in both input vcf! ");			
		logger.info(positionRecordMap.size() + " indel variants position are selected into output! ");
        
	}	
	
	/**
	 * @param indels a list of indels with same position
	 * @param pos: a new indel with same position
	 * @return true: if the input vcf merged into existing vcf which from first input vcf file. otherwise return false: add a new indel vcf record
	 */
	public boolean mergeIndel(  VcfRecord secondVcf){
		
 		ChrPosition pos = new ChrPosition(secondVcf.getChromosome(), secondVcf.getPosition(), secondVcf.getChrPosition().getEndPosition(), secondVcf.getAlt());     
		VcfRecord existingvcf = positionRecordMap.get(pos);
		//new indel
		
		List<String> informat = secondVcf.getFormatFields();
		List<String> outformat  = new ArrayList<String>();

		if(existingvcf == null){
			//insert missing data to first format column, shift original first column to second
			VcfUtils.addMissingDataToFormatFields(secondVcf, 1);	
			informat = secondVcf.getFormatFields(); //must do it again
			
 			outformat.add(0,informat.get(0));
 			outformat.add(1, informat.get(1));
 			outformat.add(2, informat.get(2));
 			secondVcf.setFormatFields(outformat);			
			positionRecordMap.put(pos, secondVcf);
			return false;
		}
		
		//add first sample column from secondVcf to exsitingvcf also check format
 		outformat.add(0,informat.get(0) );
		outformat.add(1, informat.get(1));
		VcfUtils.addAdditionalSampleToFormatField(existingvcf,  outformat) ;
 
		return true; 
	}	
	
	/**
	 * load variants to hash map
	 * @param f: vcf input file
	 * @param sampleCode: replace sample code inside the input vcf file 
	 * @throws IOException
	 */
	public void LoadIndels(File f) throws IOException{
    	int inVariants = 0;
    	int inLines = 0;
	
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	if(header == null)
        		header = reader.getHeader();	
        	else
        		header = VcfHeaderUtils.mergeHeaders(header, reader.getHeader(), false);
        	//no chr in front of position
			for (final VcfRecord re : reader) {	
				inLines ++;
    			String StrAlt = re.getAlt(); 
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
    		}
 	      }
		  logger.info(String.format("Find %d indels from %d variants (%d records lines) within file: %s",
						positionRecordMap.size(), inVariants, inLines, f.getAbsoluteFile()));
				 
			     
	}
	
	/**
	 * 
	 * @return a map of, key is the indel position, value is the list a vcf record on that position. 
	 * @throws Exception
	 */
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
