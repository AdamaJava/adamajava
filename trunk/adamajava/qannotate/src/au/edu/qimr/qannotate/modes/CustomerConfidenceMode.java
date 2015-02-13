package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.Iterator;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import au.edu.qimr.qannotate.modes.ConfidenceMode.Confidence;
import au.edu.qimr.qannotate.options.CustomerConfidenceOptions;

/**
 * @author christix
 *
 */
public final class CustomerConfidenceMode extends AbstractMode{
	
	String description = null;
	int min_read_counts = 50;
	int variants_rate = 10 ;
	boolean passOnly = false;
	
	private int test_column = -2; //can't be -1 since will "+1"
	private int control_column  = -2;
	
//	//unit test only
	CustomerConfidenceMode( ){	}
	
	public CustomerConfidenceMode(CustomerConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
        
        min_read_counts = options.get_min_read_count();
        variants_rate = options.get_min_mutant_rate();
        passOnly = options.isPassOnly();
        
		inputRecord(new File( options.getInputFileName())   );	
		
		//get control and test sample column; here use the header from inputRecord(...)
		SampleColumn column = new SampleColumn(options.getTestSample(), options.getControlSample(), this.header );
		test_column = column.getTestSampleColumn();
		control_column = column.getControlSampleColumn();
		
		addAnnotation();
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF( new File(options.getOutputFileName()) );	
	}
	
	public void setSampleColumn(int test, int control){
		test_column = test;
		control_column = control; 
	}
	
	/**
	 * add dbsnp version
	 * @throws Exception
	 */
	//inherited method from super
	void addAnnotation() {		
	    
		//add header line
		String description = "Set CONF to HIGH once the variants meet conditions otherwise set to ZERO : ";
		description += 	(passOnly) ? "passed filter," : "";
		description += "total read counts more than " +  Integer.toString(min_read_counts) + ", more than ";
		description += Integer.toString( variants_rate) + "% reads contains variants";		
		header.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "0", "Flag", description);
	      
		final Iterator<VcfRecord>  it =  positionRecordMap.values().iterator();
		while( it.hasNext() ){
			final VcfRecord re = it.next();
			
			//remove previous annotaion about CONF
			final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo());
			infoRecord.removeField(VcfHeaderUtils.INFO_CONFIDENT);
			re.setInfo(infoRecord.toString());	//must reset here
			
			//only annotate record passed filters
			if(passOnly && !re.getFilter().toUpperCase().contains(VcfHeaderUtils.FILTER_PASS)) 							
				continue;
			
			final VcfFormatFieldRecord allel = (re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) ? re.getSampleFormatRecord(test_column) :  re.getSampleFormatRecord(control_column);
			final int total =  VcfUtils.getAltFrequency(  allel, null );
			if( total <  min_read_counts) continue;
			
			try{
				final int mutants = Integer.parseInt( allel.getField(VcfHeaderUtils.FORMAT_MUTANT_READS));			  
				if( ((100 * mutants) / total) < variants_rate  ) continue;
			}catch(Exception e ){
				return; 
			}	
			
			infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());
			re.setInfo(infoRecord.toString());			
		}		
	}

	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}				
}	
