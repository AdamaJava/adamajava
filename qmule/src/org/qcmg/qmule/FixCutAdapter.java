package org.qcmg.qmule;

import picard.cmdline.CommandLineProgram;
import org.broadinstitute.barclay.argparser.Argument;

import picard.cmdline.StandardOptionDefinitions;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.StringUtil;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.fastq.FastqConstants;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;

public class FixCutAdapter extends CommandLineProgram {
	static final String USAGE = "When cutadapt finds adapter sequence at the start of a read the default action of adapter sequence trimming results in empty sequence in the output. This tool replaces any empty sequence with a single base N with 0 base quality value.";
    @Argument(shortName = StandardOptionDefinitions.INPUT_SHORT_NAME, doc = "Input FASTQ file to be fixed. Default is STDIN.", optional=true)
    public File INPUT = null;

    @Argument(shortName = StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc = "Where to write fixed fastq file. Default is STDOUT.", optional=true)
    public File OUTPUT = null;
       
    
    private int line=1;
    private int fixes = 0;

    public static void main(final String[] argv) {
    	
     	
     	int exitStatus = new FixCutAdapter().instanceMain(argv);
     	
        System.exit(exitStatus);
    }
	@Override
	protected int doWork() {
		 			        
        try( BufferedReader reader = INPUT == null ? 
<<<<<<< HEAD
				new BufferedReader(new InputStreamReader(System.in)) : IOUtil.openFileForBufferedReading(INPUT);        		
        		BufferedWriter writer = OUTPUT == null ? 
        				new BufferedWriter(new OutputStreamWriter(System.out)) : IOUtil.openFileForBufferedWriting(OUTPUT)) {       	
=======
				new BufferedReader(new InputStreamReader(System.in)) : IOUtil.openFileForBufferedReading(INPUT);
        	FastqWriter writer =  OUTPUT == null ?
				null : (new FastqWriterFactory()).newWriter(OUTPUT);) {
        	
>>>>>>> 64d8da3e6993d1ba20f14fbf41f7dbf0107229a3
        	do {        		
        		
            	final String seqHeader = reader.readLine(); // read header             
                String seqLine = reader.readLine(); // Read sequence line                          
                final String qualHeader = reader.readLine(); // Read quality header                
                String qualLine = reader.readLine(); // Read quality line
        	     
                //end of file                
                if(seqHeader == null) break;
                           	
                if (!seqHeader.startsWith(FastqConstants.SEQUENCE_HEADER)) {
                    throw new SAMException(error("Sequence header must start with " + FastqConstants.SEQUENCE_HEADER + ": " + seqHeader));
                }          
                if (!qualHeader.startsWith(FastqConstants.QUALITY_HEADER)) {
                    throw new SAMException(error("Quality header must start with " + FastqConstants.QUALITY_HEADER + ": "+ qualHeader));
                }
                if (StringUtil.isBlank(seqLine) || StringUtil.isBlank(qualLine)) { 
                	seqLine = "N";
                	qualLine = "!";
                	fixes ++;
                } 
               
                //output updated fastq record
                final FastqRecord frec = new FastqRecord(seqHeader.substring(1, seqHeader.length()), seqLine,
                        qualHeader.substring(1, qualHeader.length()), qualLine);
                
                writer.write(frec.toFastQString());
                writer.newLine();                
        		
                line += 4 ;
        	} while( true );
        	        	
        } catch (IOException | SAMException e) {
			e.printStackTrace();
			return 1;
		}   
        
        System.err.println("[" + new Date() + "] Total " + (line / 4) + " fastq records are processed, " + fixes + " of them with empty sequence are fixed to single base N");
		return 0;
	}
	
	
    /** Generates an error message with line number information. */
    protected String error(final String msg) {
        return msg + " at line " + line + " in fastq " + INPUT.getAbsolutePath();
    }

}
