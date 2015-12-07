package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader.FormattedRecord;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.options.DbsnpOptions;

public class DbsnpMode extends AbstractMode{
	QLogger logger = QLoggerFactory.getLogger(DbsnpMode.class);
//	private VcfRecord dbSNPVcf;
	
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
		
		removeExistingDbSnpIds();
		
		//debug
//		testDbSNP(options.getDatabaseFileName());
		
		if(options.isDIV())
			divAnnotation(options.getDatabaseFileName());
		else	
			addAnnotation(options.getDatabaseFileName() );
		
		reheader(options.getCommandLine(),options.getInputFileName())	;
		writeVCF( new File(options.getOutputFileName()));	
	}
	
	//debug
	void testDbSNP(String file) throws IOException{
		try (VCFFileReader reader= new VCFFileReader( file )) {
			for (final VcfRecord dbSNPVcf : reader) {
				if (! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false) &&
					 !StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false))
					continue; 
				
				int start;
				String rspos = dbSNPVcf.getInfoRecord().getField("RSPOS");
				if( !StringUtils.isNullOrEmpty(rspos)){ 		
					start = Integer.parseInt(rspos);
					if( dbSNPVcf.getPosition() != start   )
						System.out.println("debug:" + dbSNPVcf.toString());
				}
			}
			
			
		}
		
	}
		
	//testing at momemnt
	void divAnnotation(String dbSNPFile) throws Exception{
 	    		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header		
			List<Record>  metas = reader.getHeader().getMetaRecords(); 
			for(Record re: metas) {
				if(re.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE))  
					header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
									VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
									VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, new VcfHeaderUtils.SplitMetaRecord(re).getValue()  ));  
			}
			for (final VcfRecord dbSNPVcf : reader) {				
				final VcfInfoFieldRecord info = new VcfInfoFieldRecord(dbSNPVcf.getInfo());	
				int start =  dbSNPVcf.getPosition();
				int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1;		
				String ref = IndelUtils.getFullChromosome(dbSNPVcf.getChromosome());
				List<VcfRecord> inputVcfs = positionRecordMap.get(new ChrPosition(ref, start, end ));	
				//if not exists, move start position to RSPOS to see if we have a position there instead
				if (null == inputVcfs){  
					int rsposStart = Integer.parseInt(info.getField("RSPOS"));
					if (start != rsposStart)  
						inputVcfs = positionRecordMap.get(new ChrPosition(ref, rsposStart, end ));									 					 
				}
								
				if (null == inputVcfs) continue;
				
				for(VcfRecord inputVcf : inputVcfs ){
					inputVcf.setId(dbSNPVcf.getId());
					inputVcf.appendInfo("dbRef=" + dbSNPVcf.getRef());
					inputVcf.appendInfo("dbAlt=" + dbSNPVcf.getAlt());
				}
			}
		}
	}
	
	private void removeExistingDbSnpIds() {
		//init remove all exsiting dbsnpid
		for (List<VcfRecord> vcfs : positionRecordMap.values()) 
			for(VcfRecord vcf : vcfs ){
				vcf.setId(".");
				vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_DB);
			}
		
	}
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception{
		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header		
			List<Record>  metas = reader.getHeader().getMetaRecords(); 
			for (Record re: metas) {
				if (re.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE))  
					header.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
									VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
									VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, new VcfHeaderUtils.SplitMetaRecord(re).getValue()  ));  		
			}
					
			Map<String, FormattedRecord> snpInfoHeader = reader.getHeader().getInfoRecords();
			if (snpInfoHeader.get(VcfHeaderUtils.INFO_CAF) != null ) {
				header.parseHeaderLine( String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF  )	);
			}
			if (snpInfoHeader.get(VcfHeaderUtils.INFO_VLD) != null ) {
			 	header.addInfo(snpInfoHeader.get(VcfHeaderUtils.INFO_VLD));		 						 
			}
			
			int inputNo = 0;
			int dbSnpNo = 0;
			for (final VcfRecord dbSNPVcf : reader) {
				inputNo ++;
//				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false) &&
//						! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false))
//					continue;
							
				//each dbSNP check twice, since indel alleles followed by one reference base, eg. chr1 100 . TT T ...
				final int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1; 
				int start = dbSNPVcf.getPosition();
				final String chr = IndelUtils.getFullChromosome(dbSNPVcf.getChromosome());
				ChrPosition chrPos = new ChrPosition(chr, start, end );
				List<VcfRecord> inputVcfs = positionRecordMap.get(chrPos);
				if (null != inputVcfs && inputVcfs.size() != 0){
					for(VcfRecord re: inputVcfs)
						if(annotateDBsnp(re, dbSNPVcf ))
							dbSnpNo ++;						
				}
				
				//check again if RSPOS is not same with dbSNP start
				String rspos = dbSNPVcf.getInfoRecord().getField("RSPOS");
				if( !StringUtils.isNullOrEmpty(rspos)){ 		
					start = Integer.parseInt(rspos);
					if(start == chrPos.getPosition() || start > chrPos.getEndPosition()) continue; 
					
					chrPos = new ChrPosition(chr, start, end );	
					inputVcfs = positionRecordMap.get(chrPos);
					if (null != inputVcfs && inputVcfs.size() != 0) {
						for(VcfRecord re: inputVcfs)
							if(annotateDBsnp(re, dbSNPVcf ))
								dbSnpNo ++;					
					}
				}
			}
			 
			System.out.println(String.format("there are %d inputed variants and found %d variants mathch dbSNP", inputNo, dbSnpNo));
			logger.info(String.format("there are %d inputed variants and found %d variants mathch dbSNP", inputNo, dbSnpNo));
		}
	}
	
	/**
	 * annotate inputVcf with dbSNPVcf information: dbsnp id; allels frequency; VLD:if validated; mark as DB if one of input allele match dbSNP allele
	 * @param inputVcf: vcf record from input file
	 * @param dbSNPVcf: position matched vcf record from dbSNP file
	 */
	 boolean annotateDBsnp(VcfRecord  inputVcf, VcfRecord dbSNPVcf ){
		
		 ChrPosition chrPos = inputVcf.getChrPosition();
		 final String dbRef = dbSNPVcf.getRef().substring(chrPos.getPosition() - dbSNPVcf.getPosition());	
		 if(! inputVcf.getRef().equalsIgnoreCase(dbRef) ){
			 logger.warn(String.format( "dbSNP reference base (%s) are different to vcf Record (%s) for variant at position: %s~%s", 
					 dbRef, inputVcf.getRef(), chrPos.getPosition(), chrPos.getEndPosition()));			 
				return false; 			 
		 }
		 		 
		//trim the dbSNP alleles, since the first base maybe from reference if vcf second column different with "RSPOS" value 
		String[] db_alts = dbSNPVcf.getAlt().contains(Constants.COMMA_STRING) ? 
				TabTokenizer.tokenize(dbSNPVcf.getAlt(), Constants.COMMA) : new String[] {dbSNPVcf.getAlt()}; 				
		for(int i = 0; i < db_alts.length; i ++){
			int ll = chrPos.getPosition() - dbSNPVcf.getPosition();
			if(db_alts[i].length() > ll )
				db_alts[i] = db_alts[i].substring(ll);
			
		}		
												
		String[] input_alts = inputVcf.getAlt().contains(Constants.COMMA_STRING) ? 
				TabTokenizer.tokenize(inputVcf.getAlt(), Constants.COMMA) : new String[] {inputVcf.getAlt()}; 	
		
		//assign dbsnp id if one of allel matches
		boolean flag = false; 
		for(int i = 0; i < db_alts.length; i ++)
			for(int j = 0; j < input_alts.length; j ++) 
				if(db_alts[i].equalsIgnoreCase(input_alts[j])){
					inputVcf.setId(dbSNPVcf.getId());	
					inputVcf.appendInfo(VcfHeaderUtils.INFO_DB);
					if(dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null) 
						inputVcf.appendInfo(VcfHeaderUtils.INFO_VLD);					 
					flag = true; 
					break;
				}
		
		//set alleles frequency
		if(flag){
			final String caf =dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CAF);
			if(caf == null) return false;
			
			String[] cafs = TabTokenizer.tokenize(caf.substring(1,caf.length() -1), Constants.COMMA);
			String[] vafs = new String[input_alts.length];
			for(int j = 0; j < input_alts.length; j ++){
				vafs[j] = ".";
				//cafs[i+1] since element [0] is reference allele frequency
				for(int i = 0; i < db_alts.length; i ++)
					if(db_alts[i].equalsIgnoreCase(input_alts[j]))
						vafs[j] = cafs[i+1];
			} 
			if(vafs.length == 1)
				inputVcf.appendInfo(StringUtils.addToString(VcfHeaderUtils.INFO_VAF, vafs[0], EQ));
			else{
				String str = StringUtils.addToString(VcfHeaderUtils.INFO_VAF, "[" + vafs[0], EQ);
				for(int i = 1; i < vafs.length; i ++ )
					str +=   "," + vafs[i];				
				inputVcf.appendInfo( str + "]");
			}			
		}	
		
		return true; 
	}	
	
	
//	 void annotateDBsnp(VcfRecord  inputVcf, VcfRecord dbSNPVcf ){
//		//trim the dbSNP alleles, since the first base maybe from reference if vcf second column different with "RSPOS" value 
//		String[] db_alts = dbSNPVcf.getAlt().contains(Constants.COMMA_STRING) ? 
//				TabTokenizer.tokenize(dbSNPVcf.getAlt(), Constants.COMMA) : new String[] {dbSNPVcf.getAlt()}; 
//		int start = dbSNPVcf.getPosition();
//		String rspos = dbSNPVcf.getInfoRecord().getField("RSPOS");
//		if( !StringUtils.isNullOrEmpty(rspos)) 		
//			start = Integer.parseInt(rspos);		
//		for(int i = 0; i < db_alts.length; i ++)
//			db_alts[i] = db_alts[i].substring(start-dbSNPVcf.getPosition());
//				
//		String[] input_alts = inputVcf.getAlt().contains(Constants.COMMA_STRING) ? 
//				TabTokenizer.tokenize(inputVcf.getAlt(), Constants.COMMA) : new String[] {inputVcf.getAlt()}; 	
//		
//		//assign dbsnp id if one of allel matches
//		boolean flag = false; 
//		
//		for(int i = 0; i < db_alts.length; i ++)
//			for(int j = 0; j < input_alts.length; j ++) 
//				if(db_alts[i].equalsIgnoreCase(input_alts[j])){
//					inputVcf.setId(dbSNPVcf.getId());	
//					inputVcf.appendInfo(VcfHeaderUtils.INFO_DB);
//					if(dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null) 
//						inputVcf.appendInfo(VcfHeaderUtils.INFO_VLD);
//					 
//					flag = true; 
//					break;
//				}
//		
//		//set alleles frequency
//		if(flag){
//			final String caf =dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CAF);
//			if(caf == null) return;
//			String[] cafs = TabTokenizer.tokenize(caf.substring(1,caf.length() -1), Constants.COMMA);
//			String[] vafs = new String[input_alts.length];
//			for(int j = 0; j < input_alts.length; j ++){
//				vafs[j] = ".";
//				//cafs[i+1] since element [0] is reference allele frequency
//				for(int i = 0; i < db_alts.length; i ++)
//					if(db_alts[i].equalsIgnoreCase(input_alts[j]))
//						vafs[j] = cafs[i+1];
//			} 
//			if(vafs.length == 1)
//				inputVcf.appendInfo(StringUtils.addToString(VcfHeaderUtils.INFO_VAF, vafs[0], EQ));
//			else{
//				String str = StringUtils.addToString(VcfHeaderUtils.INFO_VAF, "[" + vafs[0], EQ);
//				for(int i = 1; i < vafs.length; i ++ )
//					str +=   "," + vafs[i];				
//				inputVcf.appendInfo( str + "]");
//			}			
//		}		
//	}
	
	
	
	
//	public static String getCAF(VcfInfoFieldRecord info, int order) {
//		final String caf =info.getField(VcfHeaderUtils.INFO_CAF);
//		if (caf != null) {
//			 
//				String[] cafs = TabTokenizer.tokenize(caf.substring(1,caf.length() -1), Constants.COMMA);
//				if(cafs.length > order)			
//					return StringUtils.addToString(VcfHeaderUtils.INFO_VAF, cafs[order], EQ);	
// 		}
//		
//		return null;
//	}
	
}

	
	
 
