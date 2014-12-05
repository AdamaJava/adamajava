package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.util.Iterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.options.DbsnpOptions;

public class DbsnpMode extends AbstractMode{
	
	//for unit Test
	DbsnpMode(){}
	
	public DbsnpMode(DbsnpOptions options, QLogger logger) throws Exception{	
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbSNP database: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		
		inputRecord(new File( options.getInputFileName())   );
		
		addAnnotation(options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;
		logger.info("Writing VCF output");	
		writeVCF(new File(options.getOutputFileName()));	
	}
		
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception{
		//init remove all exsiting dbsnpid
		final Iterator<VCFRecord> it = positionRecordMap.values().iterator(); 
 	    while (it.hasNext()) {
	        final VCFRecord vcf = it.next();
	        vcf.setId(".");
 	    }
		 				 
		try(VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header	
			final VcfHeader snpHeader = reader.getHeader();
			for (final VcfHeaderRecord hr : snpHeader){ 
				if(hr.getMetaType().equals(MetaType.META) &&  hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_DBSNP_LINE)) 
					header.replace(hr);  
					 				
				else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_GMAF) ){
					header.replace(hr);
				}				
			}
			
			
			//check dbsnp record
			for (final VCFRecord dbSNPVcf : reader) {
				// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
				//eg.positionRecordMap (key, value) = (chr1.100-101, vcfRecord )
				//put end position in case of compound snp
				final VCFRecord inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf.getChromosome(), dbSNPVcf.getPosition(), dbSNPVcf.getPosition() + dbSNPVcf.getRef().length() - 1 ));
 
				
				// , dbSNPVcf.getPosition() + dbSNPVcf.getRef().length()));
				if (null == inputVcf) continue;
	 			 		
				// only proceed if we have a SNP, MNP variant record
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false) &&
						! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false)) continue;

				//reference base must be same	 
				if( ! dbSNPVcf.getRef().equals( inputVcf.getRef() )) 
					throw new Exception("reference base are different ");			 
			 
				//*eg. dbSNP: "1 100 rs12334 A G,T,C ..." dbSNP may have multiple entries
				//*eg. input.vcf: "1 100 101 A G ..." , "1 100 101 A T,C ..." out snp vcf are single entries			  
				String [] alts = null; 
				try{
					//multi allels
					alts = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
				}catch(final IllegalArgumentException e){
					//single allel
					alts = new String[] {inputVcf.getAlt()};		
				}
				
				if (null == alts)  continue;			
				//annotation if at least one alts matches dbSNP alt
				for (final String alt : alts)  
					if(dbSNPVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){
						inputVcf.addInfo(getGMAF(dbSNPVcf.getInfo()));
						inputVcf.setId(dbSNPVcf.getId());						
						break;
					} 
			}
		}

	}
	
	private String getGMAF(String info){

		final String gmaf =  new VcfInfoFieldRecord(info).getfield(VcfHeaderUtils.INFO_GMAF);
		 
		if(gmaf != null) return StringUtils.addToString(VcfHeaderUtils.INFO_GMAF, gmaf, EQ);
		
		return null;
	}
	
}


	 
	
	
 
