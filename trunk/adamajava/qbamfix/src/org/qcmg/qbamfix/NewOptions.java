/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import org.apache.commons.cli.*;
//import org.apache.commons.cli.CommandLine;

public class NewOptions {
     	
	private final CommandLineParser parser = new BasicParser();
	private final Options options = new Options();
	
	private String tmpdir = null;
	private String refFileName = null;
    private SAMFileHeader header = null;	
	private String sort = null;   
	private final HashMap<String, String> RG = new HashMap<String, String>();
	private final CommandLine cmd;
	private boolean checkCMD = false;

    
    /**
     * check command line and store arguments and option information
     */
    public NewOptions(final String[] args) throws Exception{ 
    	options.addOption("h", "help", false, OptLong.help.getDescription() );
    	options.addOption("v", "version", false,  OptLong.version.getDescription());
    	
    	options.addOption(OptionBuilder.hasArg(false).withArgName(OptLong.reFinalBAM.getArgName()).withLongOpt(OptLong.reFinalBAM.toString())
        		.withDescription(OptLong.reFinalBAM.getDescription()).create() );    
    	
    	options.addOption(OptionBuilder.hasArgs(1).withArgName(OptLong.input.getArgName()).withLongOpt(OptLong.input.toString())
        		.withDescription(OptLong.input.getDescription()).create('i'));    	
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.output.getArgName()).withLongOpt(OptLong.output.toString())
        		.withDescription(OptLong.output.getDescription()).create('o') );    	
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.log.getArgName()).withLongOpt(OptLong.log.toString())
        		.withDescription(OptLong.log.getDescription()).create() );
    	    	
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGID.getArgName()).withLongOpt(OptLong.RGID.toString())
        		.withDescription(OptLong.RGID.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGCN.getArgName()).withLongOpt(OptLong.RGCN.toString())
        		.withDescription(OptLong.RGCN.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGDS.getArgName()).withLongOpt(OptLong.RGDS.toString())
        		.withDescription(OptLong.RGDS.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGDT.getArgName()).withLongOpt(OptLong.RGDT.toString())
        		.withDescription(OptLong.RGDT.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGFO.getArgName()).withLongOpt(OptLong.RGFO.toString())
        		.withDescription(OptLong.RGFO.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGKS.getArgName()).withLongOpt(OptLong.RGKS.toString())
        		.withDescription(OptLong.RGKS.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGLB.getArgName()).withLongOpt(OptLong.RGLB.toString())
        		.withDescription(OptLong.RGLB.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGPG.getArgName()).withLongOpt(OptLong.RGPG.toString())
        		.withDescription(OptLong.RGPG.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGPI.getArgName()).withLongOpt(OptLong.RGPI.toString())
        		.withDescription(OptLong.RGPI.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGPL.getArgName()).withLongOpt(OptLong.RGPL.toString())
        		.withDescription(OptLong.RGPL.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGPU.getArgName()).withLongOpt(OptLong.RGPU.toString())
        		.withDescription(OptLong.RGPU.getDescription()).create() );
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.RGSM.getArgName()).withLongOpt(OptLong.RGSM.toString())
        		.withDescription(OptLong.RGSM.getDescription()).create() ); 
    	
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.CO.getArgName()).withLongOpt(OptLong.CO.toString())
        		.withDescription(OptLong.CO.getDescription()).create() ); 
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.SQ.getArgName()).withLongOpt(OptLong.SQ.toString())
        		.withDescription(OptLong.SQ.getDescription()).create() ); 
    	
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.loglevel.getArgName()).withLongOpt(OptLong.loglevel.toString())
        		.withDescription(OptLong.loglevel.getDescription()).create() ); 
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.tmpdir.getArgName()).withLongOpt(OptLong.tmpdir.toString())
        		.withDescription(OptLong.tmpdir.getDescription()).create() ); 
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.length.getArgName()).withLongOpt(OptLong.length.toString())
        		.withDescription(OptLong.length.getDescription()).create() ); 
    	options.addOption( OptionBuilder.hasArgs(1).withArgName(OptLong.validation.getArgName()).withLongOpt(OptLong.validation.toString())
        		.withDescription(OptLong.validation.getDescription()).create() );   	
  
    	cmd = parser.parse( options, args ); 
    }

    
    
   /**
    * 
    * @return true if has version option and show version information
    * @throws Exception
    */
   public boolean hasVersion() throws Exception{
 
    	if( cmd.hasOption(OptLong.version.toString())){
                System.out.println( Messages.getVersionMessage() );
                return true;
            }
    	
        return false;
    }
   /**
    *  
    * @return String of version information including svn version
    * @throws Exception
    */
   public String getVersion() throws Exception{
		return Messages.getVersionMessage();
   }
   /**
    * 
    * @return true if has help option and show all option usage
    * @throws IOException
    */
	public boolean hasHelp() throws IOException{  
		
    	if(cmd.hasOption(OptLong.help.toString())){
    		String header = "\nAvailable options:\n" +
    				"---------------------------------------------------------";
    		String footer = "---------------------------------------------------------\n" +
    				"For additional information, see http://qcmg-wiki.imb.uq.edu.au/index.php/Qbamfix\n";
			HelpFormatter helpFormatter = new HelpFormatter( );
			helpFormatter.setOptionComparator( new OptionComparator());
		    helpFormatter.printHelp(150, Messages.USAGE, header, options, footer );   
		    return true;
    	}
    	
    	return false; 
    }
	
	public boolean isFinalBAM() throws Exception{
		
		return cmd.hasOption(OptLong.reFinalBAM.toString());
				
				
	}

	/**
	 * 
	 * @return String of program by using java.lang.Package.getImplementationTitle()
	 */
	public String getPGName(){
        return Messages.getProgramName();   
    }
	
	/**
	 * 
	 * @return String of input name with path
	 * @throws Exception
	 */
	public String getInputFileName() throws Exception{
		if(!checkCMD) checkFiles();
		return cmd.getOptionValue(OptLong.input.toString());
	}
	
	/**
	 * 
	 * @return String of output name with path
	 * @throws Exception
	 */
	public String getOutputFileName() throws Exception{
		if(!checkCMD) checkFiles();
		return cmd.getOptionValue(OptLong.output.toString());
	}	
	
	/**
	 * 
	 * @return String of log file name with path
	 * @throws Exception
	 */
	public String getLogFileName() throws Exception{
		if(!checkCMD) checkFiles();
		return cmd.getOptionValue(OptLong.log.toString());
	}

	
	/**
	 * 
	 * @return String of reference file name with path
	 * @throws Exception
	 */
	public String getReferenceFile() throws Exception{
		if(refFileName == null && cmd.hasOption(OptLong.SQ.toString())){
			refFileName = cmd.getOptionValue(OptLong.SQ.toString()).toString();
			if(! new File( refFileName ).canRead())
				throw new Exception("specified SQ file is not readable: --" +
						OptLong.SQ.toString() + " " + refFileName);			 
		}
			
		return refFileName;
	}	
	
	/**
	 * 
	 * @return String of output directory for temporary file
	 * @throws Exception if the directory
	 */
	public String getTmpDir()throws Exception{
    	if(tmpdir == null )  
    		if( cmd.hasOption( OptLong.tmpdir.toString())){
    			tmpdir = cmd.getOptionValue(OptLong.tmpdir.toString());	    		 
	    		if(! new File( tmpdir ).canWrite() )
	    			throw new Exception("the specified output directory for temporary file are not writable: " + tmpdir);				 
    		}else
    			tmpdir = new File(getOutputFileName()).getParent();
    	
		return tmpdir;   	
    }
	
	/**
	 * 
	 * @return String of log level
	 */
	public String getLogLevel(){	
		if(cmd.hasOption( OptLong.loglevel.toString()))
			return cmd.getOptionValue( OptLong.loglevel.toString() );
		
		return  null;
	}
	
	/**
	 * 
	 * @return SAMFileReader.ValidationStringency if specified on command line; otherwise return LENIENT as default
	 * @throws Exception if specified invalid ValidationStringency 
	 */
	public SAMFileReader.ValidationStringency getValidation() throws Exception{	
		
		if( cmd.hasOption(OptLong.validation.toString())){
			if( cmd.getOptionValue( OptLong.validation.toString()).equalsIgnoreCase("LENIENT"))
				return SAMFileReader.ValidationStringency.LENIENT;
			else if( cmd.getOptionValue( OptLong.validation.toString() ).equalsIgnoreCase("SILENT"))
				return SAMFileReader.ValidationStringency.SILENT;
			else if( cmd.getOptionValue( OptLong.validation.toString() ).equalsIgnoreCase("STRICT"))
				return  SAMFileReader.ValidationStringency.STRICT;
			else
				throw new Exception("invalid validation option: " + cmd.getOptionValue( OptLong.validation.toString() ) );
		}

		return SAMFileReader.ValidationStringency.LENIENT;
	}
	
	/**
	 * 
	 * @return -1; there is no length checking of seq required
	 * @return 0 (default); make sure all read seq length should be equal to qual lenght 
	 * @return >0; all read base length should be same to this number
	 */
	public int getSeqLength() throws Exception{
		
		int length = 0;
		if(cmd.hasOption(OptLong.length.toString()))
			length = Integer.parseInt(cmd.getOptionValue(OptLong.length.toString()));
		
		if(length < -1)
			throw new Exception("invalid value set to option "+ OptLong.length.toString() + length);
		
		return length;
	}
	/**
	 * 
	 * @return String of comment which will be add to CO line of BAM header
	 * @throws Exception
	 */
	public String getComment() throws Exception{
		if(cmd.hasOption(OptLong.CO.toString())){
			String value =  cmd.getOptionValue( OptLong.CO.toString());
			//if(value.isEmpty()) throw new Exception("no value specified on CO option: --" + OptLong.CO);
			return (String) value;
		}
		return null;
	}
	/**
	 * 
	 * @return a HashMap of RG field and value based on command line; 
	 */
	public HashMap<String, String> getRGinfo() {
		if(RG.size() == 0){
			if(cmd.hasOption(OptLong.RGID.toString())) 
				RG.put("ID",cmd.getOptionValue(OptLong.RGID.toString()) );
			if(cmd.hasOption(OptLong.RGCN.toString())) 
				RG.put("CN",cmd.getOptionValue(OptLong.RGCN.toString()) );
			if(cmd.hasOption(OptLong.RGDS.toString())) 
				RG.put("DS",cmd.getOptionValue(OptLong.RGDS.toString()) );
			if(cmd.hasOption(OptLong.RGDT.toString())) 
				RG.put("DT",cmd.getOptionValue(OptLong.RGDT.toString()) );
			if(cmd.hasOption(OptLong.RGFO.toString())) 
				RG.put("FO",cmd.getOptionValue(OptLong.RGFO.toString()) );
			if(cmd.hasOption(OptLong.RGKS.toString())) 
				RG.put("KS",cmd.getOptionValue(OptLong.RGKS.toString()) );
			if(cmd.hasOption(OptLong.RGLB.toString())) 
				RG.put("LB",cmd.getOptionValue(OptLong.RGLB.toString()) );			
			if(cmd.hasOption(OptLong.RGPG.toString())) 
				RG.put("PG",cmd.getOptionValue(OptLong.RGPG.toString()) );
			if(cmd.hasOption(OptLong.RGPI.toString())) 
				RG.put("PI",cmd.getOptionValue(OptLong.RGPI.toString()) );
			if(cmd.hasOption(OptLong.RGPL.toString())) 
				RG.put("PL",cmd.getOptionValue(OptLong.RGPL.toString()) );
			if(cmd.hasOption(OptLong.RGPU.toString())) 
				RG.put("PU",cmd.getOptionValue(OptLong.RGPU.toString()) );
			if(cmd.hasOption(OptLong.RGSM.toString())) 
				RG.put("SM",cmd.getOptionValue(OptLong.RGSM.toString()) );			
		}	  
		
    	
		return RG;
	}
	
	 boolean checkFiles( ) throws Exception{
        File in = new File( cmd.getOptionValue( OptLong.input.toString()) );
        File out = new File( cmd.getOptionValue( OptLong.output.toString()) );
        File log = new File( cmd.getOptionValue( OptLong.log.toString()) );

        if(!in.exists()){
            String message = Messages.getMessage("NONEXIST_INPUT_FILE", in.getAbsolutePath());
            throw new Exception( message);            
        }

        if(!in.isFile()){       
            String message = Messages.getMessage("FILE_NOT_DIRECTORY", in.getName());
            throw new Exception( message);       
        }
        
        if(!in.canRead()){
        	throw new Exception(Messages.getMessage("UNREAD_INPUT_FILE"));           
        }

        if(in.getName().equals(out.getName()) && in.getPath().equals(out.getPath())  ){
            String message = Messages.getMessage("INPUT_SAME_OUTPUT",in.getAbsolutePath(), out.getAbsolutePath());
            throw new Exception( message);                      
        }

        if( !out.getParentFile().canWrite()){
        		String message = Messages.getMessage("UNWRITE_OUTPUT_PATH", out.getAbsolutePath());
        		throw new Exception( message);           
        }
        
        if( out.getName().endsWith("sam")){
        
        	String message = Messages.getMessage("NONSUPPORTED_SAM_OUTPUT", out.getName());   
    		throw new Exception( message);            	
        }

        if( ! log.getParentFile().canWrite()  ){
	    		String message = Messages.getMessage("UNWRITE_LOG_PATH", log.getAbsolutePath() );
	    		throw new Exception( message);	     
        }     
        	 	
        return true;
    }
	
	private enum OptLong {
		 help,version,
		 input, output,log,
		 loglevel,tmpdir,validation,length,SQ,CO,
		 RGID,RGCN,RGDS,RGDT, RGFO,RGKS,RGLB,RGPG,RGPI,RGPL,RGPU,RGSM,
		 reFinalBAM;

		 String getArgName(){
			 switch(this){
	             case input: return "SAM/BAM";
	             case output: return "SAM/BAM";
	             case log: return "log";
	             case loglevel: return "loglevel";
	             case tmpdir: return "dir";
	             case validation: return "validation";
	             case length: return "length";
	             case SQ: return "reference file";
	             case CO:return "comment";
	             case RGID: return "unique string";
	             case RGCN: return "string";
	             case RGDS: return "despcription";
	             case RGDT: return "date/time";
	             case RGFO: return "flow order";
	             case RGKS: return "nucleotide array";
	             case RGLB: return "libraryid";
	             case RGPG: return "program";
	             case RGPI: return "Integer";
	             case RGPL: return "platform";
	             case RGPU: return "barcode/lane";
	             case RGSM: return "sample";
	             case reFinalBAM: return "reheadfinalbam";
	            
	            		
			 }throw new AssertionError("no argument name for: " + this);
		 }
		 
		 String getDescription(){
			 switch(this){
	             case help: return Messages.getMessage("HELP_OPTION_DESCRIPTION");
	             case version: return Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	             case log: return Messages.getMessage("LOG_OPTION_DESCRIPTION");
	             case loglevel: return Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");	            
	             case validation: return Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	             case input: return Messages.getMessage("INPUT_DESCRIPTION");
	             case output: return Messages.getMessage("OUTPUT_DESCRIPTION");
	             case tmpdir: return Messages.getMessage("TMPDIR_DESCRIPTION");
	             case SQ: return Messages.getMessage("SQ_DESCRITPION");
	             case CO: return Messages.getMessage("CO_DESCRITPION");
	             case length: return Messages.getMessage("LENGTH_DESCRITPION");	             
	             case RGID: return Messages.getMessage("RGID_DESCRITPION");
	             case RGCN: return Messages.getMessage("RGCN_DESCRITPION");
	             case RGDS: return Messages.getMessage("RGDS_DESCRITPION");
	             case RGDT: return Messages.getMessage("RGDT_DESCRITPION");
	             case RGFO: return Messages.getMessage("RGFO_DESCRITPION");
	             case RGKS: return Messages.getMessage("RGKS_DESCRITPION");
	             case RGLB: return Messages.getMessage("RGLB_DESCRITPION");
	             case RGPG: return Messages.getMessage("RGPG_DESCRITPION");
	             case RGPI: return Messages.getMessage("RGPI_DESCRITPION");
	             case RGPL: return Messages.getMessage("RGPL_DESCRITPION");
	             case RGPU: return Messages.getMessage("RGPU_DESCRITPION");
	             case RGSM: return Messages.getMessage("RGSM_DESCRITPION");
	             case reFinalBAM: return Messages.getMessage("FinalBAM_DESCRIPTION");
			 }throw new AssertionError("Unknow option: " + this);
		 }
 
	 }
	 
	static class OptionComparator implements Comparator<Option>
	 {
	     public int compare(Option c1, Option c2)
	     {
	         return OptLong.valueOf(c1.getLongOpt()).compareTo(OptLong.valueOf(c2.getLongOpt()));
	     }
	 }  
}