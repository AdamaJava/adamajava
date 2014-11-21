package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;
import org.qcmg.common.vcf.VcfHeaderUtils;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.options.GermlineOptions;

public class GermlineMode extends AbstractMode{
 	public GermlineMode(GermlineOptions options, QLogger logger) throws Exception{
		
		//this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("germline database: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
 		
		inputRecord(new File( options.getInputFileName())   );
		addAnnotation(options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}
	
  	/*
	 * panfs/share/qsnp/icgc_germline_qsnp.vcf
		dbGermline exampleq
		   1       10002   .       A       C,T     .       .       5;11;17;23;29;30;31;46;55;56;57;60;61;62;71;80;81;83;84;91;125;126;127;128;175;176;179;180;181
		   1       10003   .       A       T       .       .       5;17;23;29;30;31;55;56;57;60;61;62;71;80;81;83;84;91;129;174;175;182	
		 input vcf example, replac end PASS to GERM or append for SOMATCI record only
		   chr1    10321   .       C       T       .       MIN;MR;SBIAS;GERM       SOMATIC;MR=4;NNS=4;FS=AACCCTAACCC       GT:GD:AC        0/0:C/C:C3[36],18[8.44],T0[0],1[7]      0/1:C/T:C1[40],16[15.25],T0[0],4[9.75]
			chr1    10327   rs112750067     T       C       .       SBIAS   MR=8;NNS=6;FS=AACCCCAACCC       GT:GD:AC        0/1:C/T:C0[0],8[8.25],T4[36.25],16[12.56]
       		0/1:C/T:C0[0],9[5],T2[20.5],12[17.92]
	 */
 	@Override	
	void addAnnotation(String dbGermlineFile) throws Exception{
 		try(VCFFileReader reader = new VCFFileReader(new File(dbGermlineFile))){
	 
		 String filter = null;
		 for (VCFRecord dbGermlineVcf : reader) {
			// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
			//eg.positionRecordMap (key, value) = (chr1.100, vcfRecord )
			VCFRecord inputVcf = positionRecordMap.get(new ChrPosition("chr"+ dbGermlineVcf.getChromosome(), dbGermlineVcf.getPosition()));
			if (null == inputVcf) continue;
			
		 		
			// only proceed if we have a SOMATIC variant record
			if ( ! StringUtils.doesStringContainSubString(inputVcf.getInfo(), "SOMATIC", false)) continue;
			
			//reference base must be same
			//?? maybe errif( dbGermlineVcf.getRef() != dbGermlineVcf.getRef() )
			if( dbGermlineVcf.getRef() != inputVcf.getRef() )
				throw new Exception("reference base are different ");
			 
 			String [] alts = null; 
			if(inputVcf.getAlt().length() > 1)	  				
				alts = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
		    else 
				alts = new String[] {inputVcf.getAlt()};	
			
			
			if (null == alts)  continue;			
			//annotation if at least one alts matches dbSNP alt
			for (String alt : alts)  
				if(dbGermlineVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){					
					filter = inputVcf.getFilter();
					if(filter.endsWith("PASS") ){
						if (filter.indexOf("PASS") != filter.length() - 4)
							throw new Exception("mutli \"PASS\" marked on the FILTER field for vcf record: " + inputVcf.toString());
						filter = filter.replace("PASS", "GERM");							
					}else
						filter +=  ";GERM";
					
					//inputVcf is a pointer, use set method to change real value
					inputVcf.setFilter(filter);
					break;
				}
		 	}
 		}
	}

	
}
	

