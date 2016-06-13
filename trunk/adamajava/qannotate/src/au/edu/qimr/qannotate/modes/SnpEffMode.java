/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;
import ca.mcgill.mcb.pcingola.snpEffect.commandLine.SnpEff;

public class SnpEffMode extends AbstractMode{

	private final  String tmpFile;
	private final QLogger logger = QLoggerFactory.getLogger(SnpEffMode.class);
	public SnpEffMode( Options options) throws IOException {
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("snpeff database: " + options.getDatabaseFileName() );
        logger.tool("snpeff config file: " + options.getConfigFileName());
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("output for summary File: " + options.getSummaryFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
		      
        tmpFile = options.getOutputFileName() + ".tmp";
        
	    	logger.tool("running snpEFF, output to " + tmpFile);
	    	final boolean ok = addAnnotation( options , tmpFile );
	
	    	logger.tool("exit snpEFF: " + ok);
		
		//reheader
        if (ok) { 
	        	header = new VCFFileReader(tmpFile).getHeader();
	        	reheader(options.getCommandLine(),options.getInputFileName())	;	
	        	writeVCF(new File( options.getOutputFileName()) );
			logger.tool("reheader snpEFF output to " +   options.getOutputFileName());
		} else {
			logger.info("run SnpEff failed!");
			System.exit(1);
		}
	}
	
	@Override
	protected void writeVCF(File outputFile )  throws IOException{
		long counts = 0;
		HashSet<ChrPosition> posCheck = new HashSet<ChrPosition>();
		try(VCFFileReader reader = new VCFFileReader(new File( tmpFile));
				VCFFileWriter writer = new VCFFileWriter(outputFile )){
								
	        	for(final VcfHeader.Record record: header) {
	        		writer.addHeader(record.toString());
	        	}
	        	for (final VcfRecord record : reader) {
	        		counts ++;
	        		posCheck.add(record.getChrPosition());
	        		writer.add(record);
	        	}
		}
		logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  counts , posCheck.size()));
	}
		//throws Exception
	private boolean addAnnotation(Options options, String tmpFile ) throws FileNotFoundException{	
		
		final List<String> command = new ArrayList<String>();
		final File fdata = new File(options.getDatabaseFileName());
		
		int i = 0;
		command.add(i, "eff");
		command.add(++i, "-o");
		command.add(++i, "VCF");
		command.add(++i, "-v"); //verbose
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
		final String[] args =  command.toArray(new String[0]) ;	
    	logger.tool( command.toString());	 
		
		boolean ok = false;
		try(PrintStream original = new PrintStream(System.out);
				PrintStream ps =  new PrintStream(tmpFile)){
			
			System.setOut(ps);	
			final SnpEff snpEff = new SnpEff(args);
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
