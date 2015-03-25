package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeader.FormattedRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
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
		if(options.isDIV())
			DivAnnotation(options.getDatabaseFileName());
		else	
			addAnnotation(options.getDatabaseFileName() );
		
		reheader(options.getCommandLine(),options.getInputFileName())	;
		writeVCF( new File(options.getOutputFileName()));	
	}
		
	//testing at momemnt
	void DivAnnotation(String dbSNPFile) throws Exception{
		//init remove all exsiting dbsnpid
		final Iterator<VcfRecord> it = positionRecordMap.values().iterator(); 
 	    while (it.hasNext()) {
	        final VcfRecord vcf = it.next();
	        vcf.setId(".");
	        vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_DB);
 	    }
		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header		
			List<Record>  metas = reader.getHeader().getMetaRecords(); 
			for(Record re: metas)
				if(re.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE))  
					header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
									VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
									VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, new VcfHeaderUtils.SplitMetaRecord(re).getValue()  ));  
			
			for (final VcfRecord dbSNPVcf : reader) {
//				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=DIV", false)  )
//					continue;
				
				final VcfInfoFieldRecord info = new VcfInfoFieldRecord(dbSNPVcf.getInfo());	
			//	final int start = Integer.parseInt(info.getField("RSPOS"));
				int start =  dbSNPVcf.getPosition();
				int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1;		
				
				VcfRecord inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf. getChromosome(), start, end ));	
				if (null == inputVcf){  
					start = Integer.parseInt(info.getField("RSPOS"));
						inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf. getChromosome(), start, end ));	
				}
				
				
				if (null == inputVcf) continue;
				
				inputVcf.setId(dbSNPVcf.getId());
				inputVcf.appendInfo("dbRef=" + dbSNPVcf.getRef());
				inputVcf.appendInfo("dbAlt=" + dbSNPVcf.getAlt());
				
			}

			
		
		}
	}
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception{
		//init remove all exsiting dbsnpid
		final Iterator<VcfRecord> it = positionRecordMap.values().iterator(); 
 	    while (it.hasNext()) {
	        final VcfRecord vcf = it.next();
	        vcf.setId(".");
	        vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_DB);
 	    }
		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header		
			List<Record>  metas = reader.getHeader().getMetaRecords(); 
			for(Record re: metas)
				if(re.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE))  
					header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
									VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
									VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, new VcfHeaderUtils.SplitMetaRecord(re).getValue()  ));  		
 			
					
			Map<String, FormattedRecord> snpInfoHeader = reader.getHeader().getInfoRecords();
			if(snpInfoHeader.get(VcfHeaderUtils.INFO_CAF) != null )
				header.parseHeaderLine( String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF  )	);
			
			if(snpInfoHeader.get(VcfHeaderUtils.INFO_VLD) != null )
			 	header.addInfo(snpInfoHeader.get(VcfHeaderUtils.INFO_VLD));		 						 
		 
				
			//below algorithm only work for SNP and compound SNP
			for (final VcfRecord dbSNPVcf : reader) {
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false) &&
						! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false))
					continue;
			
				// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
				final VcfInfoFieldRecord info = new VcfInfoFieldRecord(dbSNPVcf.getInfo());	
				final int start = Integer.parseInt(info.getField("RSPOS"));
				final int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1;		
				final VcfRecord inputVcf = positionRecordMap.get(new ChrPosition("chr" + dbSNPVcf. getChromosome(), start, end ));	
				if (null == inputVcf) continue;
				
				//reference base must be same
				final String dbRef = dbSNPVcf.getRef().substring(start - dbSNPVcf.getPosition());
				final String inputRef = inputVcf.getRef();
				if ( ! dbRef.equals( inputRef )){ 
					logger.warn(String.format( "dbSNP reference base (%s) are different to vcf Record (%s) for variant at position: %s", dbRef, inputRef, inputVcf.getPosition()));			 
					continue;
				}
				
				
				//*eg. dbSNP: "1 100 rs12334 A G,T,C ..." dbSNP may have multiple entries
				//*eg. input.vcf: "1 100 101 A G ..." , "1 100 101 A T,C ..." out snp vcf are single entries			  
				String [] alts = {}; 
				try{					
					alts = TabTokenizer.tokenize(dbSNPVcf.getAlt(), ','); //multi allels
				} catch (final IllegalArgumentException e){				
					alts = new String[] {dbSNPVcf.getAlt()};		//single allel	
				}
				
				int altOrder = 0;
				for (final String alt : alts) {
					altOrder ++;
					//if(dbSNPVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){
 					if(inputVcf.getAlt().equalsIgnoreCase(alt.substring(start-dbSNPVcf.getPosition()))) {
						inputVcf.appendInfo(getCAF(dbSNPVcf.getInfo(), altOrder));
						inputVcf.setId(dbSNPVcf.getId());	
						inputVcf.appendInfo(VcfHeaderUtils.INFO_DB);
						if(dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null) {
							inputVcf.appendInfo(VcfHeaderUtils.INFO_VLD);
						}
						break;
					} 
 					
				}
			}
		}
	}
	

	
	private String getCAF(String info, int order) throws Exception{
		final String caf =  new VcfInfoFieldRecord(info).getField(VcfHeaderUtils.INFO_CAF);
		if(caf != null) {
			String[] cafs = caf.replace("[", "").replace("]", "").split(Constants.COMMA_STRING);
			if(cafs.length > order)			
				return StringUtils.addToString(VcfHeaderUtils.INFO_VAF, cafs[order], EQ);			
		}
		
		return null;
	}
	
}

	
	
 
