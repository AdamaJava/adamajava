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
import java.io.File;
import java.io.IOException;

public class FixCutAdapter extends CommandLineProgram {
    static final String USAGE = "A Fastq file may contains empty seqence and base quality after cutAdapter,  Here A base N is set to empty sequence with 0 base quality value";
    
    @Argument(shortName = StandardOptionDefinitions.INPUT_SHORT_NAME, doc = "Input Fastq file to be fixed.")
    public File INPUT;

    @Argument(shortName = StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc = "Where to write fixed fastq file.")
    public File OUTPUT;
    
    private int line=1;

    public static void main(final String[] argv) {
    	
     	
     	int exitStatus = new FixCutAdapter().instanceMain(argv);
     	
        System.exit(exitStatus);
    }
	@Override
	protected int doWork() {
        IOUtil.assertFileIsReadable(INPUT);
        IOUtil.assertFileIsWritable(OUTPUT);
        
        try( BufferedReader reader = IOUtil.openFileForBufferedReading(INPUT);
        		FastqWriter writer =  (new FastqWriterFactory()).newWriter(OUTPUT);){
        	
        	 
        	do {
        		
            	final String seqHeader = reader.readLine(); // read header             
                String seqLine = reader.readLine(); // Read sequence line                          
                final String qualHeader = reader.readLine(); // Read quality header                
                String qualLine = reader.readLine(); // Read quality line
        	     
                //end of file   
            	if ( seqHeader == null || seqLine == null || qualHeader == null || qualLine == null ) {
            		break ;          	
            	}
            	
                if (!seqHeader.startsWith(FastqConstants.SEQUENCE_HEADER)) {
                    throw new SAMException(error("Sequence header must start with " + FastqConstants.SEQUENCE_HEADER + ": " + seqHeader));
                }          
                if (!qualHeader.startsWith(FastqConstants.QUALITY_HEADER)) {
                    throw new SAMException(error("Quality header must start with " + FastqConstants.QUALITY_HEADER + ": "+ qualHeader));
                }
                if (StringUtil.isBlank(seqLine)) seqLine = "N";
                if (StringUtil.isBlank(qualLine)) qualLine = "!";

                //output updated fastq record
                FastqRecord frec = new FastqRecord(seqHeader, seqLine, qualHeader, qualLine);
                writer.write(frec); 
        		
                line += 4 ;
        	} while( true );
        	        	
        } catch (IOException e) {
			e.printStackTrace();
			return 1;
		}        
		return 0;
	}
	
	
    /** Generates an error message with line number information. */
    protected String error(final String msg) {
        return msg + " at line " + line + " in fastq " + INPUT.getAbsolutePath();
    }

}
