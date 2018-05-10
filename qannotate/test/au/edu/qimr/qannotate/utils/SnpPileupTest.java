package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import scala.actors.threadpool.Arrays;

public class SnpPileupTest {
	static final String inputBam = "input.bam"; 
 
	@BeforeClass
	public static void createInput() {	
		createSam( makeReads4Pair() ); 
	}
	
	@AfterClass
	public static void deleteInput() {			
		File dir = new File(".");
		if(!dir.isDirectory()) throw new IllegalStateException("wtf mate?");
		for(File file : dir.listFiles()) {
		    if(file.getName().startsWith("input."))
		       file.delete();
		}
	}	
	
	@Test 
	public void overlappedPairTest() throws IOException{
		List<SAMRecord> pool = createPool();
				
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);		 			
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation() ; 
			
			//discard pair with different base
			if( pos == 282753 )					
				 //assertEquals(anno, "2[0,1,0,0,0,0,0,0,0]"); 
				assertEquals(anno, "2[0,1,A0,T0,G0,C0,O0]");
			
			//count as one for pair with same base, here the snp base is [A]
			else if( pos == 282768 )	
				assertEquals(anno, "2[1,0,A1,T0,G0,C0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,T0,G0,C0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,T1,G0,C0,O0]");
		}
		 
	}
	
	@Test
	public void errMDTest() throws IOException{
		List<SAMRecord> pool = createPool();
		
		for(SAMRecord re: pool )
			re.setAttribute("MD", "");
		
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);		 			
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation() ; 
			
			//discard pair with different base
			if( pos == 282753 )					
				 assertEquals(anno, "2[0,1,A0,T0,G0,C0,O0]"); 
			
			//count as one for pair with same base, here the snp base is [A]
			else if( pos == 282768 )	
				assertEquals(anno, "2[1,0,A1,T0,G0,C0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,T0,G0,C0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,T1,G0,C0,O0]");  
		}
	}
	
	@Test
	/**
    30M2D13M3I3M25S             | -------------- 30M ------------ | 2D |---- 13M ---| 3I  3M  | ------- 25S --------- |
    (first of pair read base)   CTTCTTCATCCACT A TTT C AGGCAATGAC A    AA CACTGTGCCAT ATG CTG TATCTTATACACATCACCCAGCCCA
    									   282753  				282768			 282783
    (second of pair read base)  GCAGCGTCAGAGGT T TAT A AGTTACAGCT T	   CT TCATCCACTCT TTG AGG CAATGACACCCACTGTGCCATCTG

	 * @throws IOException
	 */
	public void errCigarTest() throws IOException{
		List<SAMRecord> pool = createPool();
		//pool.get(0).setCigarString("30M2D13M3I3M25S");
		//chage second read cigar to first one
		pool.get(1).setCigar(pool.get(0).getCigar());
	 		
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);		 			
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation() ; 
			
			//discard pair with different base
			if( pos == 282753 )					
				 assertEquals(anno, "2[0,1,A0,T0,G0,C0,O0]"); 
			
			//after ciagr changed, the base position also shift
			//base on second become [T]
			else if( pos == 282768 )	
				assertEquals(anno, "2[0,1,A0,T0,G0,C0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,T0,G0,C0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,T1,G0,C0,O0]");  
		}
	}
	
	private List<SAMRecord> createPool() throws IOException{			
		List<SAMRecord> pool = new ArrayList<SAMRecord>();				
		try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File(inputBam));){
	        for(SAMRecord re : inreader)  pool.add(re);   	
		}		
		return pool; 		
	}

    public static void createSam( List<String> reads ){
    	String ftmp = "input.sam";
    	
        List<String> data = new ArrayList<String> ();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile"); 
        data.addAll(reads);
                  
       
        try( BufferedWriter out =  new BufferedWriter( new FileWriter( ftmp )) ){
           for ( String line : data )  out.write( line + "\n" );          
           out.close();
        } catch (IOException e) {
            System.err.println( "IOException caught whilst attempting to write to SAM test file: " + ftmp  + e );
        } 
        
		try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File(ftmp));  ){
			SAMFileHeader he = inreader.getFileHeader();
			he.setSortOrder( SAMFileHeader.SortOrder.coordinate );
			SAMFileWriter writer = new SAMOrBAMWriterFactory(he , false, new File(inputBam), true).getWriter();	        
	        for(SAMRecord re : inreader){ writer.addAlignment(re); }
	        writer.close();
		} catch (IOException e) { e.printStackTrace(); }		
     
    }
       
	/**  
	 *    	                             snp  C->A              T->A  CC->AA
                                               |                  |    ||
    MD:Z:14C14T^GA0C0C14        |-----14-----| | |-------14-----| | del|| |--------14-------|
    30M2D13M3I3M25S             | -------------- 30M ------------ | 2D |---- 13M ---| 3I  3M  | ------- 25S --------- |
    ( first of pair read base)  CTTCTTCATCCACT A TTT C AGGCAATGAC A    AA CACTGTGCCAT ATG CTG TATCTTATACACATCACCCAGCCCA

    ref              ***********CTTCTTCATCCACT C TTT C AGGCAATGAC T GA CC CACTGTGCCAT     CTG ***********************
                                |              |     |            |                 |
                               282739       282753 282757      282768              282783       

    GCAGCGTCAGAGGTTTATAAGTTACAG CTTCTTCATCCACT C TTT G AGGCAATGAC A    CC CACTGTGCCAT     CTG    (second pair read base)
    | ----------- 27S ------- | | ------------ 30M -------------- | 2D | ------ 16M ------- |    27S30M2D16M 
                                |----------18------| | |----10--| |del |--------16----------|    MD:Z:18C11^GA16
                                                     |            |   
                                              snp: C->G         T->A 
	 */	    
    private static List<String> makeReads4Pair(){
    	 List<String> data = new ArrayList<String>();
    	 
         data.add("HVN7YBGXY:3:12503:8213:1979	99	chr11	282739	54	30M2D13M3I3M25S	=	282739	48	CTTCTTCATCCACTATTTCAGGCAATGACAAACACTGTGCCATATGCTGTATCTTATACACATCACCCAGCCCA	"
           		+ "AAAAA//AE/EE/E//EEEA/E//EEE////A///EE/E/EEE/A/EEEEEE/EE/AAEEE/A/////A/</AE	MD:Z:14C14T^GA0C0C14	RG:Z:20140717025441134");  

         data.add("HVN7YBGXY:3:12503:8213:1979	147	chr11	282739	60	27S30M2D16M	=	282739	-48	GCAGCGTCAGAGGTTTATAAGTTACAGCTTCTTCATCCACTCTTTGAGGCAATGACACCCACTGTGCCATCTG	"
             	+ "AAAAA//AE/EE/E//EEEA/E//EEE////A///EE/E/EEE/A/EEEEEE/EE/AAEEE/A/////A/</A	MD:Z:18C11^GA16	RG:Z:20140717025441134");  
   	    	 
    	 return data; 
    }

}
