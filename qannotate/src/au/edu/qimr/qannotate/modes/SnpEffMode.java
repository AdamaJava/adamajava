package au.edu.qimr.qannotate.modes;


import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.options.SnpEffOptions;
import ca.mcgill.mcb.pcingola.snpEffect.commandLine.SnpEff;


public class SnpEffMode extends AbstractMode{

//	private String cmd;
	private final  String tmpFile;
	private final QLogger logger;
	public SnpEffMode(SnpEffOptions options, QLogger logger) throws Exception{
		this.logger = logger;
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("snpeff database: " + options.getDatabaseFileName() );
        logger.tool("snpeff config file: " + options.getConfigFileName());
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("output for summary File: " + options.getSummaryFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		      
        tmpFile = options.getOutputFileName() + ".tmp";
        
    	logger.tool("running snpEFF, output to " + tmpFile);
    	boolean ok = addAnnotation( options , tmpFile );

    	logger.tool("exit snpEFF: " + ok);
		
		//reheader
        if(ok){ 	
         	reheader( options.getCommandLine());	
        	writeVCF(new File( options.getOutputFileName()) );
			logger.tool("reheader snpEFF output to " +   options.getOutputFileName());
		}else{
			logger.info("run SnpEff failed!");
			System.exit(1);
		}
	
	}
	@Override
	protected void writeVCF(File outputFile )  throws Exception{
		try(VCFFileReader reader = new VCFFileReader(new File( tmpFile));
				VCFFileWriter writer = new VCFFileWriter(outputFile )){
								
        	for(VcfHeaderRecord record: header)  writer.addHeader(record.toString());
        	for (VCFRecord qpr : reader) writer.add(qpr);
		} 
	}
		//throws Exception
	private boolean addAnnotation(SnpEffOptions options, String tmpFile ) throws Exception{	
		
		List<String> command = new ArrayList<String>();
		File fdata = new File(options.getDatabaseFileName());
		
		int i = 0;
		command.add(i, "eff");
		command.add(++i, "-o");
		command.add(++i, "VCF");
		//command.add(++i, "-c");
		command.add(++i, "-config");
		command.add(++i, options.getConfigFileName());
		command.add(++i, "-stats");
		command.add(++i, options.getSummaryFileName());				
		command.add(++i, "-dataDir");
		command.add(++i, fdata.getParent());	
		command.add(++i, fdata.getName());
		command.add(++i, options.getInputFileName());
		
		//run snpEff, store output to a tmp file
		String[] args =  command.toArray(new String[0]) ;	
    	logger.tool( command.toString());	 
		
		boolean ok = false;
		try(PrintStream original = new PrintStream(System.out);
				PrintStream ps =  new PrintStream(tmpFile)){
			
			System.setOut(ps);	
			SnpEff snpEff = new SnpEff(args);
			ok = snpEff.run();
						
	 	//	SnpEff.main(args); it will exit system once done
	 		System.setOut(original);	 		
		} 
		
		return ok; 		
	}
	
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception {
		// TODO Auto-generated method stub
		
	}
 
	
 
}
