package au.edu.qimr.indel;

import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

import au.edu.qimr.indel.pileup.IndelMT;

public class Support {
	
	 
	public static void clear() throws IOException {
		File dir = new java.io.File( "." ).getCanonicalFile();
		File[] files =dir.listFiles();
		if (null != files) {
			for(File f : files) {
			    if(  f.getName().endsWith(".ini")  || f.getName().endsWith(".vcf")  ||  f.getName().endsWith(".bam") ||
			    		f.getName().endsWith(".sam") ||  f.getName().endsWith(".bai")  || f.getName().endsWith(".fai") ) {
			        f.delete();	
			    }
			}
		}
		
	}
	
	public static void createBam( List<String> data1, String output) {
        List<String> data = new ArrayList<>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chrY	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
        data.addAll(data1);		
        String tmp = "tmp.sam";
        try( BufferedWriter out = new BufferedWriter(new FileWriter(tmp))) {	           
           for (String line : data)   out.write(line + "\n");	
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }
		 	
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(tmp));){		
			SAMOrBAMWriterFactory factory = new  SAMOrBAMWriterFactory(reader.getFileHeader() ,false, new File(output));
			SAMFileWriter writer = factory.getWriter();
			for( SAMRecord record : reader) 
				writer.addAlignment(record);
			 
			factory.closeWriter();
		} catch (IOException e) {
			System.err.println(Q3IndelException.getStrackTrace(e));
			Assert.fail("Should not threw a Exception");
		}		
	}
	
	/**
	 * the file only contain four deletions, three on chr11 and one on chrY
	 * @param vcf: output vcf name
	 */
	public static void createGatkVcf(String vcf){	
		
        List<String> data = new ArrayList<>(6);
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1");
        data.add("chr11	2672739	.	ATT	A	123.86	.	.	GT	0/1"); 
        data.add("chrY	2672735	.	ATT	A	123.86	.	GATKINFO	GT	0/1"); 
        data.add("chr11	2672739	.	ATTC	A	123.86	.	.	GT	0/1"); 
        data.add("chr11	2672734	.	ATT	A	123.86	.	.	GT	0/1"); 
        
        createVcf(data, vcf);       
	}
	
	public static void createVcf( List<String> data1, String output){	
        List<String> data = new ArrayList<>(3);
        data.add("##fileformat=VCFv4.1");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"S1"); 
        
        createVcf(data, data1,output);		
	}
	
	public static void createVcf( List<String> header, List<String> records, String output){	
        try( BufferedWriter out = new BufferedWriter(new FileWriter(output ))) {
    		for (String line : header) {
                out.write(line + "\n");
    		}
    		for (String line : records) {
    			out.write(line + "\n");
    		}
         }catch(IOException e){
         	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
         	Assert.fail("Should not threw a Exception");
         }  
	}	
	
	/**
	 * run q3indel without run homopolymer for unit testing
	 * @param ini
	 */
	public static Options runQ3IndelNoHom( String ini){
		String[] args = {"-i", ini}; 
		try{
			Options options = new Options(args);	

			IndelMT mt = new IndelMT(options, options.getLogger());
			mt.process(2);
			
			return options; 
			
		} catch (Exception e) {
			System.err.println(Q3IndelException.getStrackTrace(e));
			Assert.fail("Should not throw an Exception");
		}		
		return null; 
	}
}
