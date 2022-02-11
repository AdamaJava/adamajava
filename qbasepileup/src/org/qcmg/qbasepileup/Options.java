package org.qcmg.qbasepileup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.qcmg.pileup.Messages;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.model.StrandEnum;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Options {
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String log;
	private final String logLevel;
	private int threadNo = 1;
	private String filterQuery;
	private  List<InputBAM> inputBAMs;
	
	//private String iniFile;
	private String mode;
	private File output;
	 
	
	public Options(final String[] args) throws Exception {
			
			parser.accepts("help", Messages.getMessage("HELP_OPTION"));
			parser.accepts("version",Messages.getMessage("VERSION_OPTION"));
			parser.accepts("ini", Messages.getMessage("INI_OPTION")).withOptionalArg().ofType(String.class);
			parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
			parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);

		
			options = parser.parse(args);					
			log = (String) options.valueOf("log");
            logLevel = options.has("loglevel")? (String) options.valueOf("loglevel"): "INFO";
            
            if(options.has("ini")) {
	            String iniFile = (String) options.valueOf("ini");
	            parseIniFile(iniFile); 
	            detectBadOptions();
            }
            
            
			
	}
	
	
	public void parseIniFile(String iniFile) throws Exception {

		Ini ini = new Ini(new File(iniFile));
		
		//[general] common ini options
		Section generalSection = ini.get("general");		 		
	 
		//default mode = snp
		this.mode = generalSection.containsKey("mode")? generalSection.get("mode") : QBasePileupConstants.SNP_MODE;
		
		//default threadNo = 1
		if(generalSection.containsKey("thread_no")) {
			this.threadNo = new Integer(generalSection.get("thread_no")); 
		}
		
		//filter = Opt, a qbamfilter query to filter out BAM records. Def=null.
		if(generalSection.containsKey("filter")) {
			this.filterQuery =  generalSection.get("filter"); 
		}
		
		//
		if(generalSection.containsKey("output")) {
			this.output = new File( generalSection.get("output"));
		}
		 

 		//get input BAMs
		inputBAMs = new ArrayList<InputBAM>();
		if(generalSection.containsKey("input-bam")) {
			String input = generalSection.get("input-bam");    		 
			InputBAM i = new InputBAM(null, null, new File(input), "bam");
			inputBAMs.add(i);
		}
		
		if(generalSection.containsKey("input-bam-list")) {
			getBamList( generalSection.get("input-bam-list"));
		}
 
		if(generalSection.containsKey("input-hdf")) {
			getHDFBamList( generalSection.get("input-hdf"));			 
		}
 

		 

	 
				
		
	}
	
	public void detectBadOptions() throws QBasePileupException {
		
			if (QBasePileupConstants.SNP_MODE.equals(mode) || mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) 
					|| mode.equals(QBasePileupConstants.SNP_CHECK_MODE)) {}
	
	}
			
	private void getBamList(String list) throws IOException, QBasePileupException {
		
		String inputType = "list";		
		File bamList = new File (list);
		if (!bamList.exists()) {
			throw new QBasePileupException("BAMFILELIST_ERROR", list);
		}
		
		try (BufferedReader reader = new BufferedReader(new FileReader(bamList));) {
			String line;
			int count = 0;
			while((line = reader.readLine()) != null) {
				if ( ! line.startsWith("#") && ! line.startsWith("ID")) {
					if (line.split("\t").length >= 3) {
						count++;
						String[] values = line.split("\t");
						try {
							InputBAM i = new InputBAM(Integer.valueOf(values[0]), values[1], new File(values[2]), inputType);
							inputBAMs.add(i);
						} catch (NumberFormatException e) {
							reader.close();
							throw new QBasePileupException("INPUT_FORMAT_ERROR", line);
						}				
					} else {					
						InputBAM i = new InputBAM(count, "", new File(line), inputType);
						inputBAMs.add(i);
					}
				}
			}
		}
	}
			
	 
	
	private void getHDFBamList(String fhdf) throws Exception {
		String inputType = "hdf";
		File hdf = new File(fhdf);

		if (!hdf.exists()) {
			throw new QBasePileupException("NO_HDF", hdf.getAbsolutePath());
		}
		
		PileupHDF pileupHDF = new PileupHDF(hdf.getAbsolutePath(), false, false);
		pileupHDF.open();
		List<String> bamFiles = pileupHDF.getBamFilesInHeader();
		int count = 1;
		for (String bam: bamFiles) {
			InputBAM i = new InputBAM(count, "", new File(bam), inputType );
			inputBAMs.add(i);
			count++;
		}
		pileupHDF.close();
	}
	
	public OptionSet getOptions() {
		return options;
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasLogOption() {		
		return options.has("log");
	}

	//???
	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

}
