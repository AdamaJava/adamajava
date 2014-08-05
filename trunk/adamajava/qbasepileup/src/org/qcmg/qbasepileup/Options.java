/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.qbasepileup.snp.SnpOption;
public class Options {

	private static final String HELP_OPTION = Messages.getMessage("OPTION_HELP");	
	private static final String VERSION_OPTION = Messages.getMessage("OPTION_VERSION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String usage = Messages.USAGE;
	private String mode = null;
	private SnpOption  snpOpt = null; 
	private String log;
	

	public Options(final String[] args) throws Exception {
	 
		
		parser.allowsUnrecognizedOptions(); 
		 		
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	
 		parser.accepts("m", Messages.getMessage("OPTION_MODE")).withRequiredArg().ofType(String.class).describedAs("mode");				
 		options = parser.parse(args);
		
		if(  options.has("m")){	
			mode = ((String) options.valueOf("m")).toLowerCase();
			if(mode.equals(QBasePileupConstants.SNP_MODE) 
						|| mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) 
						|| mode.equals(QBasePileupConstants.SNP_CHECK_MODE)){
				snpOpt = new SnpOption(mode, args);
				log = snpOpt.getLog();				
			}
			
			else if ( mode.equals(QBasePileupConstants.INDEL_MODE)){}
					
			else if ( mode.equals(QBasePileupConstants.COVERAGE_MODE) ) {}
								
			else
				throw new QBasePileupException("MODE_ERROR",(String) options.valueOf("m"));
		}
		
		
	}
	
	public OptionSet getOptions() {
		return options;
	}
	
	public SnpOption getSnpOption(){
		return snpOpt;
	}
	
	public String getLog() {
		return log;
	}	
	
	public String getMode() {
		return mode;
	}
 
	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}	 
	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	void displayHelp() throws Exception {

		if(mode == null ){
			System.err.println(usage);
			parser.printHelpOn(System.err);
		}else if ( mode.equals(QBasePileupConstants.INDEL_MODE)){}
		
		else if ( mode.equals(QBasePileupConstants.COVERAGE_MODE) ) {}
					
		else
			snpOpt.displayHelp();
		
	}

	 

}

