package au.edu.qimr.qannotate;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;
import org.qcmg.vcf.VCFHeader;

import ca.mcgill.mcb.pcingola.snpEffect.commandLine.SnpEff;


public class SnpEffMode {
	
	 
	QLogger logger;

		public SnpEffMode(SnpEffOptions options, QLogger logger) throws Exception{
			
			
//			JarFile jf = new JarFile("/Users/christix/Documents/Eclipse/adamajava/lib/snpEff-3.5g.jar");
//			ZipEntry manifest = jf.getEntry("META-INF/MANIFEST.MF");
//			long manifestTime = manifest.getTime();  //in standard millis
//			 Date date = new Date(manifestTime);
//			System.out.println(date);
			
		this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbSNP file: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		
      
        String tmpFile = options.getOutputFileName() + ".tmp";
        
        	logger.tool("running snpEFF, output to " + tmpFile);
        	boolean ok = addAnnotation( options , tmpFile );
        	logger.tool("exit snpEFF: " + ok);
		
		//reheader
        if(ok){ 				
			try(VCFFileReader reader = new VCFFileReader(new File( tmpFile));
					VCFFileWriter writer = new VCFFileWriter(new File( options.getOutputFileName()))){
	        	VCFHeader header = reheader(reader.getHeader());	
	        	for(String record: header)  writer.addHeader(record);
	        	for (VCFRecord qpr : reader) writer.add(qpr);
			}  
			logger.tool("reheader snpEFF output to " +   options.getOutputFileName());
		}
	
	}
		//throws Exception
	public boolean addAnnotation(SnpEffOptions options, String tmpFile ) throws Exception{	
		
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
	
	
	private VCFHeader reheader(VCFHeader header){
		final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
		final String STANDARD_FILE_DATE = "##fileDate=";
		final String STANDARD_SOURCE_LINE = "##source=";
		final String STANDARD_UUID_LINE = "##uuid=";
		
		String version = Main.class.getPackage().getImplementationVersion();

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		Vector<String> headerLines = new Vector<String>();
		for(String record: header){ 
			if(record.startsWith(STANDARD_FILE_DATE)){
				//replace date
				headerLines.add(STANDARD_FILE_DATE + df.format(Calendar.getInstance().getTime()) + "\n");
				//add uuid
				headerLines.add(STANDARD_UUID_LINE + QExec.createUUid() + "\n");
				headerLines.add(STANDARD_SOURCE_LINE + "qannotate " + version + "\n");
				continue;
			}else if(record.startsWith(STANDARD_SOURCE_LINE) || record.startsWith(STANDARD_UUID_LINE)){
				continue;
			
			}else if( record.startsWith(STANDARD_FINAL_HEADER_LINE) ){
		//		headerLines.add("##" + cmd + "\n"); //add cmd
				headerLines.add(record + "\n");
				break;
			} 
				
			headerLines.add(record + "\n");
		} 
				
		return new VCFHeader(headerLines);
	}	
	
	
	
/*	
	public  SnpEffMode(SnpEffOptions options,QLogger logger, String[] args ) throws Exception{
		inputFile = options.getInputFileName(); // variant input file
		outputFile  = options.getOutputFileName();
		File fdata = new File(options.getDatabaseFileName());

		//parent class variable
		String configFile = options.getConfigFileName();
		String dataDir = fdata.getParent();
		String genomeVer = fdata.getName();		
		config = new Config(genomeVer, configFile, dataDir);		
		
		summaryFile =  options.getSummaryFileName();
		summaryGenesFile = options.getGenesFileName();
		
//		iterateVcf(String inputFile, OutputFormatter outputFormatter)  

		config.loadSnpEffectPredictor(); // Read snpEffect predictor

		boolean tapc = !config.getGenome().hasCodingInfo();
		config.setTreatAllAsProteinCoding(tapc);
	 

 
	// Set upstream-downstream interval length
	config.getSnpEffectPredictor().setUpDownStreamLength(upDownStreamLength);

	// Set splice site size
	config.getSnpEffectPredictor().setSpliceSiteSize(spliceSiteSize);


	// Load NextProt database
	 loadNextProt();

	// Load Motif databases
	 loadMotif();

	// Build tree	if (verbose) Timer.showStdErr("Building interval forest");
	config.getSnpEffectPredictor().buildForest();


//	genome = config.getSnpEffectPredictor().getGenome();

		// Set upstream-downstream interval length
		SnpEffectPredictor snpEffectPredictor = config.getSnpEffectPredictor();
		snpEffectPredictor.setUpDownStreamLength(upDownStreamLength);

		
	}
*/
}
