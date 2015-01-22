package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.util.Iterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.options.DbsnpOptions;

public class DbsnpMode extends AbstractMode{
	QLogger logger;
	
	//for unit Test
	DbsnpMode(){}
	
	public DbsnpMode(DbsnpOptions options, QLogger logger) throws Exception{	
		this.logger = logger;
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
		final Iterator<VcfRecord> it = positionRecordMap.values().iterator(); 
 	    while (it.hasNext()) {
	        final VcfRecord vcf = it.next();
	        vcf.setId(".");
 	    }
		 				 
		try(VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header	
			final VcfHeader snpHeader = reader.getHeader();
			for (final VcfHeaderRecord hr : snpHeader) 
				if(hr.getMetaType().equals(MetaType.META) &&  hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_DBSNP_LINE)) 
					header.replace(hr);  					 				
				else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_GMAF) ) 
					header.replace(hr);				 
				 else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_CAF) ) 
					header.replace(hr);
				 							 
			//below algorithm only work for SNP and compound SNP
			for (final VcfRecord dbSNPVcf : reader) {
				if ( !StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false) &&
						!StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false))
					continue;

			
				// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
				final VcfInfoFieldRecord info = new VcfInfoFieldRecord(dbSNPVcf.getInfo());	
				final int start = Integer.parseInt(info.getField("RSPOS"));
				final int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1;		
				final VcfRecord inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf. getChromosome(), start, end ));	
				if (null == inputVcf) continue;
			
				//debug untill find vcf inside
				
				/*VcfRecord inputVcf = null;
				while(inputVcf == null && end >= start){
					inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf. getChromosome(), start, end ));
					end --;
				}
				end ++;
				if (null == inputVcf) continue;*/
				
				//reference base must be same
				final String dbRef = dbSNPVcf.getRef().substring(start - dbSNPVcf.getPosition());
				final String inputRef = inputVcf.getRef();
				if( ! dbRef.equals( inputRef )){ 
					logger.warn(String.format( "dbSNP reference base (%s) are different to vcf Record (%s) for variant at position: %s", dbRef, inputRef, inputVcf.getPosition()));			 
					continue;
				}
				
				
				//*eg. dbSNP: "1 100 rs12334 A G,T,C ..." dbSNP may have multiple entries
				//*eg. input.vcf: "1 100 101 A G ..." , "1 100 101 A T,C ..." out snp vcf are single entries			  
				String [] alts = {}; 
				try{					
					alts = TabTokenizer.tokenize(dbSNPVcf.getAlt(), ','); //multi allels
				}catch(final IllegalArgumentException e){				
					alts = new String[] {dbSNPVcf.getAlt()};		//single allel	
				}
				
				for (final String alt : alts)  
					//if(dbSNPVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){
 					if(inputVcf.getAlt().equalsIgnoreCase(alt.substring(start-dbSNPVcf.getPosition())  )  ){
						inputVcf.appendInfo(getCAF(dbSNPVcf.getInfo()));
						inputVcf.setId(dbSNPVcf.getId());	
						break;
					} 
				}
		}
	}
	

	
	private String getCAF(String info) throws Exception{

		final String gmaf =  new VcfInfoFieldRecord(info).getField(VcfHeaderUtils.INFO_GMAF);
		final String caf =  new VcfInfoFieldRecord(info).getField(VcfHeaderUtils.INFO_CAF);
		 
		if(gmaf != null) return StringUtils.addToString(VcfHeaderUtils.INFO_GMAF, gmaf, EQ);
		if(caf != null) return StringUtils.addToString(VcfHeaderUtils.INFO_CAF, caf, EQ);
		
		return null;
	}
	
}


	 
	
	
 
