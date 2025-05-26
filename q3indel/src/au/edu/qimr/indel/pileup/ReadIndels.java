package au.edu.qimr.indel.pileup;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.vcf.VcfFileReader;

import au.edu.qimr.indel.Q3IndelException;

public class ReadIndels {
	static final String FILTER_SOMATIC = "SOMATIC";
	
	final QLogger logger; 
	private VcfHeader header; 
	
	private static final int errRecordLimit = 100;
	//counts from each input, {No of new indel, No of overlap indel, No of indels with multi Allele, No of inputs variants, No of input variants with multi Allele}
	private final int[] counts = {0, 0, 0, 0, 0}; 
 
	//here key will be uniq for indel: chr, start, end, allele
	private final  Map<VcfRecord, VcfRecord> positionRecordMap = new  ConcurrentHashMap<>();	
	
	public ReadIndels( QLogger logger) { 
		this.logger = logger; 
	}
	
	/**
	 * merge first sample column of input to existing variants which is stored on hash map
	 * @param f input of vcf file
	 * @throws IOException
	 */
	public void appendTestIndels(File f) throws IOException {
				
		//only keep the first sample column, and put "." to second column
		for (VcfRecord vcf : positionRecordMap.values()) {
			List<String> format = vcf.getFormatFields();
			if (format != null) {
				while (format.size() > 2) { 
					format.removeLast();
				}
				vcf.setFormatFields(format);			 
				VcfUtils.addMissingDataToFormatFields(vcf, 2);
			}			
		}	
		logger.info("only keep first sample column of tumour input vcf." );
		
    	//merge variants  
        try (VcfFileReader reader = new VcfFileReader(f)) {
        		header = VcfHeaderUtils.mergeHeaders(header, reader.getVcfHeader(), false);
			for (final VcfRecord re : reader) {
				
				resetGenotype(re);
    			String[] alleles = re.getAlt().split(",");
				for (String alt :alleles) {
					 
					SVTYPE type = IndelUtils.getVariantType(re.getRef(), alt);
					if (type.equals(SVTYPE.DEL) || type.equals(SVTYPE.INS) ) {
 	        			VcfRecord vcf1 = re; 
	 	        		//reset allele column
	 	        		if (alleles.length > 1) { 
	 	        			vcf1 = VcfUtils.resetAllele(re, alt);
	 	        		}
	 	        		mergeTestIndel(vcf1);
					}
				}  	    			
			}
		}        
	}	
	
	
	/**
	 * Add this vcf record if not exists on Map, mark as somatic on filter column,  move  this record first sample column to second column and mark missing data '.' on the first column; 
	 * Or merge this vcf record into existed variants: replace the second sample column of exist variants with first sample column of new one.
	 * @param secVcf a vcf record
	 * @return true if same variants exist and merge them; otherwise return false by adding this new variants 
	 */
	private boolean mergeTestIndel(VcfRecord secVcf) { 
	    //get vcf with same ChrPointPosition, ref and alt
		VcfRecord existingvcf = positionRecordMap.get( secVcf );
				
		//only keep first sample column of second vcf
		List<String> secformat = secVcf.getFormatFields();
		while (secformat != null && secformat.size() > 2 ) {
			secformat.removeLast();
		}
		
		//copy secVcf with sample column "FORMAT <missing data> oriSample" only 
		if (existingvcf == null) { 
			//the test only indel always set as somatic, even gatk Test runMode
			existingvcf = new VcfRecord.Builder(secVcf.getChrPosition(), secVcf.getRef())
			.id(secVcf.getId()).allele(secVcf.getAlt()).filter(FILTER_SOMATIC).build();
 			
			existingvcf.setInfo(secVcf.getInfo());	
			
			//insert . to first sample column and then shift original sample to second 
			if (secformat != null) {
				existingvcf.setFormatFields(secformat);				
				VcfUtils.addMissingDataToFormatFields(existingvcf, 1);
			}
			
			positionRecordMap.put(existingvcf, existingvcf);		
						
			return false; 
		} else {
			//gatk mode already set filter as "." (germline) ignore pileup, since they are also appear on control vcf			
			//only keep first sample column of existing vcf
			List<String> format1 = existingvcf.getFormatFields();
			
			if (format1 != null) {
				while (format1.size() > 2 ) {
					format1.removeLast();
				}
				existingvcf.setFormatFields(format1);
				//merge exiting and second vcf format, the existing vcf already inside map
				VcfUtils.addAdditionalSampleToFormatField(existingvcf,  secformat) ;				
			}
			return true; 			
		}		 
	}	
	
	/**
	 * load variants to hash map
	 * @param f vcf input file
	 * @param sampleCode: replace sample code inside the input vcf file 
	 * @throws IOException
	 */
	
	/**
	 * 
	 * @param f is vcf input file
	 * @throws IOException
	 */
	public void loadIndels(File f) throws IOException { 
		int indelNew = 0;
		int indelOverlap = 0;
		int indelMultiAltNo = 0;
	    int inLines = 0;
	    int inMultiAltNo = 0;
		
        try (VcfFileReader reader = new VcfFileReader(f)) {
        	if (header == null) {
        		header = reader.getVcfHeader();	
        	} else { 
        		header = VcfHeaderUtils.mergeHeaders(header, reader.getVcfHeader(), false);
        	}
	        	//no chr in front of position
			for (final VcfRecord re : reader) {
				inLines ++;
				resetGenotype(re);
    			String[] alleles = re.getAlt().split(",");
    			if (alleles.length > 1) { 
    				inMultiAltNo ++; //multi allele input variants
    			}
	    			
				for (String alt : alleles) {
					SVTYPE type = IndelUtils.getVariantType(re.getRef(), alt);
	 	        	if (type.equals(SVTYPE.DEL) || type.equals(SVTYPE.INS) ) {	        		
	 	        		VcfRecord vcf1 = re; 	 	        	
	 	        		if (alleles.length > 1) { 
	 	        			vcf1 = VcfUtils.resetAllele(re, alt); //reset allele column
	 	        			indelMultiAltNo ++; //mutli allele indel
	 	        		}
	 	        		
	 	        		//format data from control, set default as germline
	 	        		vcf1.setFilter(Constants.MISSING_DATA_STRING);
	 	        		
 					if (positionRecordMap.containsKey(vcf1) && (indelOverlap ++) < errRecordLimit) {						
 						logger.warn("same variants already exists, this one will be discard:\n" + positionRecordMap.get(vcf1).toString() );
 						continue; //no overwrite but just warning
 					}
 					positionRecordMap.put(vcf1, vcf1);  
 					indelNew ++;
	   	 	        	} //done for current indel allele
				}
	    	}
        }
	               
    	//counts from each input
    	counts[0] = indelNew;
    	counts[1] = indelOverlap;
    	counts[2] = indelMultiAltNo;
    	counts[3] = inLines;
    	counts[4] = inMultiAltNo;
	}
	
	/**
	 * change the input vcf by putting '.' on "GT" field if multi Alleles exists;
	 * do nothing if not multi Alleles or not GT field on format column
	 * @param vcf input vcf record
	 */
	public void resetGenotype(VcfRecord vcf) {
		List<String> format = vcf.getFormatFields();
		
		if (format == null || format.size() < 2) { 
			return; 
		}
		
		//add GD to second field 				
		VcfFormatFieldRecord[] frecords = new VcfFormatFieldRecord[format.size() - 1];		
		for (int i = 1; i < format.size(); i ++) { 
			VcfFormatFieldRecord re = new  VcfFormatFieldRecord(format.getFirst(), format.get(i));
			String gd = IndelUtils.getGenotypeDetails(re, vcf.getRef(), vcf.getAlt() );
			re.setField(1, VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, gd == null ? Constants.MISSING_DATA_STRING : gd);
			
			//put . since not sure GT value after split
			if (vcf.getAlt().contains(Constants.COMMA_STRING)) {
				re.setField(VcfHeaderUtils.FORMAT_GENOTYPE, Constants.MISSING_DATA_STRING);
			}
			
			frecords[i - 1] = re; 
		}
		
		format.clear();
		format.add(frecords[0].getFormatColumnString());
		format.add(frecords[0].getSampleColumnString());
		for (int i = 1; i < frecords.length; i++) {
			//the exception shouldn't happen
			if ( !frecords[i].getFormatColumnString().equals(frecords[0].getFormatColumnString())) { 
				throw new IllegalArgumentException("both sample column with different format column: \n"
						+ frecords[0].getFormatColumnString() + "\n" + frecords[i].getFormatColumnString());
			}
			format.add(frecords[i].getSampleColumnString());
		}
		
		vcf.setFormatFields(format);
	}
	
	/**
	 * 
	 * @return a map of, key is the indel position, value is the list a vcf record on that position. 
	 */
	public Map<ChrRangePosition, IndelPosition> getIndelMap() throws Q3IndelException {	
	
		Map<ChrRangePosition,IndelPosition> indelPositionMap = new  ConcurrentHashMap<>();
		for (VcfRecord vcf : positionRecordMap.values()) {
			ChrRangePosition indelPos = new ChrRangePosition(vcf.getChrPosition(), vcf.getChrPosition().getEndPosition());
			IndelPosition ip = indelPositionMap.get(indelPos);
			if (null == ip) {
				indelPositionMap.put(indelPos, new IndelPosition(vcf));
			} else {
				ip.addVcf(vcf);
			}
		}	
	
		return indelPositionMap;
	}
	
	public VcfHeader getVcfHeader() { 
		return header;	
	}
	
	int getCountsNewIndel() { 
		return counts[0];
	}
	
	int getCountsOverlapIndel() { 
		return counts[1];
	}
	
	int getCountsMultiIndel() {
		return counts[2];
	}
	
	int getCountsInputLine() { 
		return counts[3]; 
	}	
	
	int getCountsInputMultiAlt() { 
		return counts[4];
	}
	
	int getCountsTotalIndel() {
		return positionRecordMap.size();
	}
}
