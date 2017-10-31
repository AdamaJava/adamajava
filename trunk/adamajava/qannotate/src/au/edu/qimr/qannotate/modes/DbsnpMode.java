/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.EQ;

import java.io.File;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Options;

public class DbsnpMode extends AbstractMode{
	private final static QLogger logger = QLoggerFactory.getLogger(DbsnpMode.class);
//	private VcfRecord dbSNPVcf;
	
	//for unit Test
	DbsnpMode(){}
	
	
	public DbsnpMode(Options options) throws Exception{	
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbSNP database: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
		
		inputRecord(new File( options.getInputFileName())   );		
		removeExistingDbSnpIds();		
 		addAnnotation(options.getDatabaseFileName() );		
		reheader(options.getCommandLine(),options.getInputFileName())	;
		writeVCF( new File(options.getOutputFileName()));	
	}
	
		
	//testing at momemnt
	@Deprecated
	void divAnnotation(String dbSNPFile) throws Exception{
 	    		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header	
			VcfHeaderRecord dbre = reader.getHeader().firstMatchedRecord(VcfHeaderUtils.STANDARD_DBSNP_LINE);
			if( dbre != null)	 {
//				header.addOrReplace(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
//						VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, dbre.getMetaValue()  ));
				
				header.addInfo(VcfHeaderUtils.INFO_DB,  "0", VcfInfoType.Flag.name(),VcfHeaderUtils.DESCRITPION_INFO_DB);
			}
		
			for (final VcfRecord dbSNPVcf : reader) {				
				final VcfInfoFieldRecord info = new VcfInfoFieldRecord(dbSNPVcf.getInfo());	
				int start =  dbSNPVcf.getPosition();
				int end =  dbSNPVcf.getRef().length() +  dbSNPVcf.getPosition() -1;		
				String ref = IndelUtils.getFullChromosome(dbSNPVcf.getChromosome());
				List<VcfRecord> inputVcfs = positionRecordMap.get(new ChrRangePosition(ref, start, end ));	
				//if not exists, move start position to RSPOS to see if we have a position there instead
				if (null == inputVcfs){
					int rsposStart = Integer.parseInt(info.getField("RSPOS"));
					if (start != rsposStart)  
						inputVcfs = positionRecordMap.get(new ChrRangePosition(ref, rsposStart, end ));									 					 
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
		for (List<VcfRecord> vcfs : positionRecordMap.values()) { 
			for (VcfRecord vcf : vcfs ) {
				vcf.setId(".");
				vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_DB);
			}
		}
	}
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception{
		 				 
		try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header		
			VcfHeaderRecord dbre = reader.getHeader().firstMatchedRecord(VcfHeaderUtils.STANDARD_DBSNP_LINE);
			 
			if (dbre != null)  
				header.addOrReplace(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
								VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
								VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, dbre.getMetaValue() )  );  		
		 
			if (reader.getHeader().getInfoRecord(VcfHeaderUtils.INFO_CAF) != null )	
				header.addOrReplace( String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF  )	);

			if (reader.getHeader().getInfoRecord(VcfHeaderUtils.INFO_VLD) != null )	
				header.addOrReplace( reader.getHeader().getInfoRecord(VcfHeaderUtils.INFO_VLD));
		 
			int dbSnpNo = 0;
			for (final VcfRecord dbSNPVcf : reader) {
				 
				//each dbSNP check twice, since indel alleles followed by one reference base, eg. chr1 100 . TT T ...
				ChrPosition dbSnpCP = dbSNPVcf.getChrPosition();
				final String chr = IndelUtils.getFullChromosome(dbSNPVcf.getChromosome());
				if ( ! chr.equals(dbSnpCP.getChromosome())) {
					dbSnpCP = ChrPositionUtils.cloneWithNewChromosomeName(dbSnpCP, chr);
				}
				List<VcfRecord> inputVcfs = positionRecordMap.get(dbSnpCP);
				if (null != inputVcfs && inputVcfs.size() != 0){
					for(VcfRecord re: inputVcfs) {
						if(annotateDBsnp(re, dbSNPVcf )) {
							dbSnpNo ++;						
						}
					}
				}
				
				//check RSPOS for MNV only
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false)) {
					continue;
				}
				
				//if RSPOS different to column 2
				String rspos = dbSNPVcf.getInfoRecord().getField("RSPOS");
				if( ! StringUtils.isNullOrEmpty(rspos)) {
					int start = Integer.parseInt(rspos);
					if(start == dbSnpCP.getStartPosition() || start > dbSnpCP.getEndPosition()) continue; 
					
					dbSnpCP = new ChrRangePosition(chr, start, dbSnpCP.getEndPosition() );	
					inputVcfs = positionRecordMap.get(dbSnpCP);
					if (null != inputVcfs && inputVcfs.size() != 0) {
						for(VcfRecord re: inputVcfs) {
							if(annotateDBsnp(re, dbSNPVcf )) {
								dbSnpNo ++;					
							}
						}
					}
				}
			}
			 
			logger.info(String.format("found %d matched dbSNP ", dbSnpNo));
		}
	}
	
	/**
	 * annotate inputVcf with dbSNPVcf information: dbsnp id; allels frequency; VLD:if validated; mark as DB if one of input allele match dbSNP allele
	 * @param inputVcf: vcf record from input file
	 * @param dbSNPVcf: position matched vcf record from dbSNP file
	 */
	 public static boolean annotateDBsnp(VcfRecord  inputVcf, VcfRecord dbSNPVcf ){
		 if (null == inputVcf || null == dbSNPVcf) {
			 throw new IllegalArgumentException("Null vcf record(s) passed to annotateDBsnp. inputVcf: " + inputVcf + ",  dbSNPVcf: " + dbSNPVcf);
		 }
		
		 ChrPosition chrPos = inputVcf.getChrPosition();
		 final String dbRef = dbSNPVcf.getRef().substring(chrPos.getStartPosition() - dbSNPVcf.getPosition());	
		 if ( ! inputVcf.getRef().equalsIgnoreCase(dbRef) ){
			 logger.warn(String.format( "dbSNP reference base (%s) are different to vcf Record (%s) for variant at position: %s~%s", 
					 dbRef, inputVcf.getRef(), chrPos.getStartPosition(), chrPos.getEndPosition()));			 
			return false; 			 
		 }
		 		 
		//trim the dbSNP alleles, since the first base maybe from reference if vcf second column different with "RSPOS" value 
		String[] db_alts = dbSNPVcf.getAlt().contains(Constants.COMMA_STRING) ? 
				TabTokenizer.tokenize(dbSNPVcf.getAlt(), Constants.COMMA) : new String[] {dbSNPVcf.getAlt()}; 				
		for(int i = 0; i < db_alts.length; i ++){
			int ll = chrPos.getStartPosition() - dbSNPVcf.getPosition();
			if(db_alts[i].length() > ll )
				db_alts[i] = db_alts[i].substring(ll);			
		}		
												
		String[] input_alts = inputVcf.getAlt().contains(Constants.COMMA_STRING) ? 
				TabTokenizer.tokenize(inputVcf.getAlt(), Constants.COMMA) : new String[] {inputVcf.getAlt()}; 	
		
		//assign dbsnp id if one of allel matches
		boolean validated = dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null;
		boolean flag = false; 
		for (int i = 0; i < db_alts.length; i ++) {
			for (int j = 0; j < input_alts.length; j ++) {
				if (db_alts[i].equalsIgnoreCase(input_alts[j])) {
					inputVcf.setId(dbSNPVcf.getId());
					inputVcf.appendInfo(VcfHeaderUtils.INFO_DB);
					if (validated) {
						inputVcf.appendInfo(VcfHeaderUtils.INFO_VLD);					 
					}
					flag = true; 
					break;
				}
			}
		}
		//set alleles frequency
		if (flag) {
			final String caf =dbSNPVcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CAF);
			if(caf == null) return false;
			
			String[] cafs = TabTokenizer.tokenize(caf.substring(1,caf.length() -1), Constants.COMMA);
			String[] vafs = new String[input_alts.length];
			for(int j = 0; j < input_alts.length; j ++){
				vafs[j] = ".";
				//cafs[i+1] since element [0] is reference allele frequency
				for(int i = 0; i < db_alts.length; i ++) {
					if(db_alts[i].equalsIgnoreCase(input_alts[j])) {
						vafs[j] = cafs[i+1];
					}
				}
			} 
			if (vafs.length == 1) {
				inputVcf.appendInfo(StringUtils.addToString(VcfHeaderUtils.INFO_VAF, vafs[0], EQ));
			} else {
				String str = StringUtils.addToString(VcfHeaderUtils.INFO_VAF, "[" + vafs[0], EQ);
				for(int i = 1; i < vafs.length; i ++ ) {
					str +=   "," + vafs[i];				
				}
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

	
	
 
