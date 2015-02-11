package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
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
	        vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_DB);
 	    }
		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header	
			final VcfHeader snpHeader = reader.getHeader();
			for (final VcfHeader.Record hr : snpHeader.getMetaRecords()) { 
				if (hr.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE)) {
				//if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_DB) ) 
					header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
									VcfHeaderUtils.INFO_DB, "Flag",VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, 
									StringUtils.getValueFromKey(hr.getData(), VcfHeaderUtils.STANDARD_DBSNP_LINE)));
				}
			}
			
			Map<String, VcfHeader.FormattedRecord> infoRecords = snpHeader.getInfoRecords();
			
			if (infoRecords.containsKey(VcfHeaderUtils.INFO_GMAF)) {
				header.addInfo(infoRecords.get(VcfHeaderUtils.INFO_GMAF));
			}
			if (infoRecords.containsKey(VcfHeaderUtils.INFO_CAF)) {
				header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF));
			}
			if (infoRecords.containsKey(VcfHeaderUtils.INFO_VLD)) {
				header.addInfo(infoRecords.get(VcfHeaderUtils.INFO_VLD));
			}
			
//			for (final VcfHeader.Record hr : snpHeader.get()) { 
//			
//				} else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_GMAF) ) { 
//					header.replace(hr);				 
//				} else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_CAF) ) {
//					header.replace(VcfHeaderUtils.parseHeaderLine(
//							String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF  )	));
//							 
//				} else if( hr.getMetaType().equals(MetaType.INFO) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.INFO_VLD)) {
//									header.replace(hr);
//				}
//			}
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

//		final String gmaf =  new VcfInfoFieldRecord(info).getfield(VcfHeaderUtils.INFO_GMAF);
		final String caf =  new VcfInfoFieldRecord(info).getField(VcfHeaderUtils.INFO_CAF);
		if(caf != null) {
			String[] cafs = caf.replace("[", "").replace("]", "").split(Constants.COMMA_STRING);
			if(cafs.length > order)			
				return StringUtils.addToString(VcfHeaderUtils.INFO_VAF, cafs[order], EQ);			
		}
		
		return null;
	}
	
}

/*error
chrY	9930004	rs74440257	C	A	35.63	NCIT	.	GT:AD:DP:GQ:PL:GD:AC:MR:NNS	1/1:0,2:2:6.02:67,6,0:A/A:A2[35],0[0]:2:2	.:.:.:.:.:.:A3[27.33],0[0]:3:3
[christix@QIMR13479:~/Documents/Eclipse/data]$grep 9930004 output.vcf 
chrY	9930004	rs74440257	C	A	35.63	NCIT	.;DB	GT:AD:DP:GQ:PL:GD:AC:MR:NNS	1/1:0,2:2:6.02:67,6,0:A/A:A2[35],0[0]:2:2	.:.:.:.:.:.:A3[27.33],0[0]:3:3

 * 
 * 
 */


	 
	
	
 
