package au.edu.qimr.qannotate;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.qcmg.common.log.QLogger;

import ca.mcgill.mcb.pcingola.snpEffect.commandLine.*;
 

public class SnpEffMode {
	
	//normally << 10,000,000 records in one vcf file
	QLogger logger;
	//public SnpEffMode(SnpEffOptions options ) throws Exception{}
	
	public  SnpEffMode(SnpEffOptions options,QLogger logger ) throws Exception{
		this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("snpEff database: " + options.getDatabaseFileName() );
        logger.tool("configure file: " + options.getConfigFileName());
        logger.tool("output stat file: " + options.getStatOutputFileName());
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());        
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		
		File fdata = new File(options.getDatabaseFileName());
		
		//constructure the command line for snpEff
		List<String> command = new ArrayList<String>();
		command.add(0, "eff");
		command.add(1, "-dataDir");
		command.add(2, fdata.getParent());
		command.add(3, "-config");
		command.add(4, options.getConfigFileName());
		command.add(5, "-stats");
		command.add(6, options.getStatOutputFileName());		
		command.add(7, fdata.getName());
		command.add(8, options.getInputFileName()); 		
		command.add(9, "-o");
		command.add(10, "VCF");	
		//command.add(11, "-interval");
		
		String[] args =  command.toArray(new String[0]) ;
//		System.out.println(Arrays.toString(args));
		SnpEff.main(args);
		
		// Parse
/*		SnpEff snpEff = new SnpEff();
		snpEff.parseArgs(args);
		
		System.out.println("start running...");

		// 		boolean ok = snpEff.run();
*/		 
	}
 
 
	
}
