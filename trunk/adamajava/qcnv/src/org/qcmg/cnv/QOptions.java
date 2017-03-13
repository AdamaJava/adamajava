/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.*;

public class QOptions {
 	private final String[] args;
	
	private final CommandLineParser parser = new BasicParser( );
	private final org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
	private  CommandLine cmd;	
    final Map<String, Integer> optMap = new HashMap<String, Integer>();

	public QOptions(String[] args){
		this.args = args;
	}
	
	//the main function to add options
	public void add(Option opt) throws Exception{
		
		String longname = opt.getLongOpt();
		
		if(optMap.get(longname) == null)
			optMap.put(longname, optMap.size());
		else 
			throw new Exception("this option already added, please rename it: " + longname);
		
		options.addOption(opt);

	}
	
	public void add(String shortname, String description) throws Exception{
		add(shortname, shortname, null, description,null, null); 
	}
	
	public void add(String shortname, String longname, String description) throws Exception{
		add(shortname, longname,null, description,null, null);		 
	}
	
	public void add(String shortname, String longname, String argname, String description) throws Exception{
		add(shortname, longname,argname, description, null, null);		 
	}
	
	//call add(String shortname, String longname, String argname, String description, Integer numArg, Object type) 
	//ignor argument type
	public void add(String shortname, String longname, String argname, String description, int numArg) throws Exception{
		add(shortname, longname,argname, description, numArg, null); 		
	}
	
	//created an option instance 
	public void add(String shortname, String longname, String argname, String description, Integer numArg, Object type) throws Exception{
		
		OptionBuilder oBuild = OptionBuilder.withLongOpt(longname);

		if(argname != null)
			oBuild = oBuild.withArgName(argname);
		
		if(description != null)
			oBuild = oBuild.withDescription(description);
		
		if( numArg != null)
			//only take the begin specified number of arguments. 
			oBuild = oBuild.hasArgs(numArg);
		else 
			//default take all arguments
			oBuild = oBuild.hasArgs();
		
		if(type != null )
			oBuild = oBuild.withType(type);
		
		if(shortname == null)
			add(oBuild.create());		
		else
			add(oBuild.create(shortname));	
	}
	
	public boolean has(String opt) throws ParseException{
		if(cmd == null) 
			cmd = parser.parse( options, args ); 
	 			
		return cmd.hasOption(opt);
	}
	
	//return an array of multi arguments
	public String[] valuesOf(String opt) throws ParseException{
		if(cmd == null) 
			cmd = parser.parse( options, args ); 
				
		//return Arrays.toString(cmd.getOptionValues(opt));
		return cmd.getOptionValues(opt);
	}
	
	//return the first argument if multi arguments exists
	public String valueOf(String opt) throws ParseException{
		if(cmd == null) 
			cmd = parser.parse( options, args ); 
		
		return cmd.getOptionValue(opt);
	}
	/**
	 * it print out the help information. The option order will follow the order when the program call the add methods. 
	 * @param usage : this string will be printed on the first line
	 * @throws ParseException
	 */
 	public void printHelp(String usage) throws ParseException{
		if(cmd == null) 
			cmd = parser.parse( options, args ); 
			
		if(cmd.hasOption("help")){
    		String header = "\nAvailable options:\n" +
    				"---------------------------------------------------------";
    		String footer = "---------------------------------------------------------\n" +
    				"For additional information, see http://qcmg-wiki.imb.uq.edu.au/index.php/<toolname>\n" ;
			HelpFormatter helpFormatter = new HelpFormatter( );
			helpFormatter.setOptionComparator( new OptionComparator(optMap));
		    helpFormatter.printHelp(150, usage, header, options, footer );   
		    
    	}	
	}
 	
 	public void printHelp(int wide, String usage, String header, 
			String footer, Comparator comp ) throws ParseException{
		if(cmd == null) 
			cmd = parser.parse( options, args ); 
		
		if(!cmd.hasOption("help")) return;
		
		HelpFormatter helpFormatter = new HelpFormatter( );
		
		if(header == null)
			header =   "\nAvailable options:\n" +
				"---------------------------------------------------------";
		
		if(footer == null)
			footer = "---------------------------------------------------------\n" +
    				"For additional information, see http://qcmg-wiki.imb.uq.edu.au/index.php/<toolname>\n" ;
		
		if(comp != null)
			helpFormatter.setOptionComparator( comp);
		else
			helpFormatter.setOptionComparator( new OptionComparator(optMap));
		
	    helpFormatter.printHelp(150, usage, header, options, footer );   
	}
 	
 	static class OptionComparator implements Comparator<Option>
	 {   
		 private final Map<String, Integer> optMap;
		
		 public OptionComparator(Map<String, Integer> optMap){
			 this.optMap = optMap;
		 }
		 
	     public int compare(Option c1, Option c2){
	         return optMap.get(c1.getLongOpt()).compareTo(optMap.get(c2.getLongOpt()));
	     }
	 }   
 	
 	//only for debug
	public static void main(final String[] args) throws Exception {
		
		System.out.println(Arrays.toString(args));
		QOptions myoptions = new QOptions(args);
		
		myoptions.add("h", "help", "","help info", 0);		
		myoptions.add(null, "v","", "mutiversion", 5);
		myoptions.add("in", "input","SAM/BAM", "input file with full path",null, String.class);
		
		System.out.println("value of (h) is " + myoptions.valueOf("h"));
		System.out.println("value of (v) is " + myoptions.valueOf("v"));
		System.out.println("value of (input) is " + myoptions.valueOf("input"));
		myoptions.printHelp("usage");

	}
	
	 
}
