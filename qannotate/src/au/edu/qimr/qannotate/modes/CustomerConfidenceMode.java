/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.SampleColumn;

/**
 * @author christix
 *
 */
@Deprecated
public final class CustomerConfidenceMode extends AbstractMode{
	
	final String PASS = "PASS";
	final String BP = "5BP"; 
	final String SBIASCOV = "SBIASCOV"; 
	
//	String description = null;
	int min_read_counts = 50;
	int variants_rate = 10 ;
//	boolean passOnly = false;
	
	private int test_column = -2; //can't be -1 since will "+1"
	private int control_column  = -2;
	
	private final QLogger logger  = QLoggerFactory.getLogger(CustomerConfidenceMode.class); 
	
 	
//	//unit test only
	CustomerConfidenceMode( ){	}
	
	public CustomerConfidenceMode(Options options) throws Exception{	
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        
//        min_read_counts = options.get_min_read_count();
//        variants_rate = options.get_min_mutant_rate();
//        passOnly = options.isPassOnly();
        
		inputRecord(new File( options.getInputFileName())   );	
		
		//get control and test sample column; here use the header from inputRecord(...)
		SampleColumn column = SampleColumn.getSampleColumn(options.getTestSample(), options.getControlSample(), this.header );
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
		String description = "Set CONF to HIGH if total read counts more than " +  Integer.toString(min_read_counts) + "; and more than  "
				+ Integer.toString( variants_rate) + "% reads contains variants; plus filter column is , \"5BP\" or \"SBIASCOV\". Set ZERO to remaining mutations ";	
 
		header.addInfoLine(VcfHeaderUtils.INFO_CONFIDENCE, "1", "String", description);
	      
//		final Iterator<VcfRecord>  it =  positionRecordMap.values().iterator();
//		while( it.hasNext() ){
// final VcfRecord re = it.next();			
		for (List<VcfRecord> vcfs : positionRecordMap.values()) 
			for(VcfRecord re : vcfs){					
				boolean  flag = false;
				String filter = re.getFilter().toUpperCase();
				if(!filter.contains(Constants.SEMI_COLON_STRING) && 
						(  filter.contains(BP) || filter.contains(PASS) ||  filter.contains(SBIASCOV))  ){
				 
					final VcfFormatFieldRecord format = (re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) ? re.getSampleFormatRecord(test_column) :  re.getSampleFormatRecord(control_column);
					final int total =  VcfUtils.getAltFrequency(  format, null );
					if( total >=  min_read_counts) 
						try{
							//final int mutants = Integer.parseInt( allel.getField(VcfHeaderUtils.FORMAT_MUTANT_READS));	
							final int mutants =  VcfUtils.getAltFrequency(  format, re.getAlt() );
							if( ((100 * mutants) / total) >= variants_rate  ) flag = true;  
							
						}catch(Exception e ){
							logger.error("err during caculating mutants rate for variants: " + re.toString() + "\n" + e.getMessage());
						}
				} 							
					
				if(flag)
					re.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.HIGH.toString());				 
				else	
					re.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.ZERO.toString());
		}		
	}
	
	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}				
}	
