package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class SnpPileupTest {
    @org.junit.Rule
    public  TemporaryFolder testFolder = new TemporaryFolder();

//	static final String inputBam = "input.bam"; 
	File input;
 
	@Before 
	public void createInput() throws IOException {	
		input = testFolder.newFile("input.bam");
		createSam( makeReads4Pair(), input ); 
	}
	
//	@AfterClass
//	public static void deleteInput() {			
//		File dir = new File(".");
//		if(!dir.isDirectory()) throw new IllegalStateException("wtf mate?");
//		for(File file : dir.listFiles()) {
//		    if(file.getName().startsWith("input."))
//		       file.delete();
//		}
//	}
	
 
	
	@Test 
	public void overlappedPairTest() throws IOException{
		List<SAMRecord> pool = createPool(input);
				
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);		 			
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation() ; 
			
			//discard pair with different base
			if( pos == 282753 )					
				 //assertEquals(anno, "2[0,1,0,0,0,0,0,0,0]"); 
				assertEquals(anno, "2[0,1,A0,C0,G0,T0,O0]");
			
			//count as one for pair with same base, here the snp base is [A]
			else if( pos == 282768 )	
				assertEquals(anno, "2[1,0,A1,C0,G0,T0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,C0,G0,T0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,C0,G0,T1,O0]");
		}
		 
	}
	
	@Test
	public void errMDTest() throws IOException{
		List<SAMRecord> pool = createPool(input);
		
		for(SAMRecord re: pool )
			re.setAttribute("MD", "");
		
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);		 			
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation() ; 
			
			//discard pair with different base
			if( pos == 282753 )					
				 assertEquals(anno, "2[0,1,A0,C0,G0,T0,O0]"); 
			
			//count as one for pair with same base, here the snp base is [A]
			else if( pos == 282768 )	
				assertEquals(anno, "2[1,0,A1,C0,G0,T0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,C0,G0,T0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,C0,G0,T1,O0]");
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
		List<SAMRecord> pool = createPool(input);
		//pool.get(0).setCigarString("30M2D13M3I3M25S");
		//chage second read cigar to first one
		pool.get(1).setCigar(pool.get(0).getCigar());
	 		
		for(int pos : new int[]{ 282753, 282768, 282769, 282783 }){
			ChrPointPosition chrP = new ChrPointPosition("chr11", pos);
			SnpPileup pileup= new SnpPileup( chrP, pool );
			String anno = pileup.getAnnotation();
			
			//discard pair with different base
			if( pos == 282753 )					
				 assertEquals(anno, "2[0,1,A0,C0,G0,T0,O0]"); 
			
			//after ciagr changed, the base position also shift
			//base on second become [T]
			else if( pos == 282768 )	
				assertEquals(anno, "2[0,1,A0,C0,G0,T0,O0]");
			
			//count as one for pair with same base, here the snp base is others(del)
			else if( pos == 282769 )	
				assertEquals(anno, "2[1,0,A0,C0,G0,T0,O1]"); 
			
			//snp on adjacent to insertion location since inertion don't take reference space
			else
				assertEquals(anno, "2[1,0,A0,C0,G0,T1,O0]");  
		}
	}
	
	private List<SAMRecord> createPool(File fbam) throws IOException{			
		List<SAMRecord> pool = new ArrayList<SAMRecord>();				
		try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(fbam);){
	        for(SAMRecord re : inreader)  pool.add(re);   	
		}		
		return pool; 		
	}

    public static void createSam( List<String> reads, File fsam ) throws IOException{
    	//String ftmp = "input.sam";
    	
        List<String> data = new ArrayList<String> ();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile"); 
        data.addAll(reads);
                  
       
        try( BufferedWriter out =  new BufferedWriter( new FileWriter( fsam )) ){
           for ( String line : data )  out.write( line + "\n" );                     
        }  
        
		try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(fsam);  ){
			SAMFileHeader he = inreader.getFileHeader();
			he.setSortOrder( SAMFileHeader.SortOrder.coordinate );
			SAMFileWriter writer = new SAMOrBAMWriterFactory(he , false, fsam, true).getWriter();	        
	        for(SAMRecord re : inreader){ writer.addAlignment(re); }
		}  	
     
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
