package au.edu.qimr.qannotate.modes;



import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.options.FixOptions;



public class FixMode extends AbstractMode{
	
	private final QLogger logger;
	private final String sequencer;
	
	public FixMode(){
		logger = QLoggerFactory.getLogger(Main.class, null,  null);	   
		sequencer = null;
		
	}
	
	public FixMode(FixOptions options, QLogger logger) throws Exception {
		// TODO Auto-generated constructor stub
		 
		this.logger = logger;		
		this.sequencer = options.getSequencer();			
		
		try(VCFFileReader reader = new VCFFileReader(new File( options.getInputFileName()));
				PrintWriter out = new PrintWriter(options.getOutputFileName())){
				
			//add vcf header	 
			header = reader.getHeader();
			reheader(options.getCommandLine(), options.getInputFileName());
			for(final VcfHeader.Record record: header) 
				out.println(record.toString());
			
			//add fixed vcf
			for (final VcfRecord vcf : reader) 
				out.println( fixVcf(vcf));
				
			
	    }	

	}
	
	
	VcfRecord fixVcf(VcfRecord inputVcf){
		
		String str = inputVcf.toString().replaceAll(Constants.TAB+"", "(T)");
		System.out.println("vcf: " + str);
		str = inputVcf.getFormatFieldStrings().replaceAll(Constants.TAB+"", "(T)");
		System.out.println("format: " + str);
		
		final List<String> formats =  inputVcf.getFormatFields();
		for(int i = 0; i < formats.size(); i++)
			System.out.println( i + ": " + formats.get(i));
		
		
		VcfRecord vcf = inputVcf;
		
		
		return vcf; 
		
		
		
	}
	


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
