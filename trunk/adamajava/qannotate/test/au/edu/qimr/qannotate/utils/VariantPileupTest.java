package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class VariantPileupTest {
	static final String inputBam = "tumor.bam"; 
	
	@BeforeClass
	public static void createInput() { 
		List<String> reads = makeReads4Indel();
		reads.addAll(makeReads4Pair());
		createSam(reads);		
	}
	
	@AfterClass
	public static void deleteInput() {
		String USER_DIR = System.getProperty("user.dir");	
		String bam = "tumor";
		String log = ".log";
		
		File[] files = (new File(USER_DIR)).listFiles(f->f.getName().contains(bam) | f.getName().contains(log) );
		for(File f : files) f.delete();		
}	
		
	@Test
	public void mnpTest() throws Exception{
		VcfRecord vcf = new VcfRecord.Builder("chr11",	282768,	"T").allele("A").build();
		List<SAMRecord> pool = makePool(vcf);
		
		//pair exactly match snp
		VariantPileup pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[1,0,0,1,0]")); 
				
		//mnp match alt
		vcf = new VcfRecord.Builder("chr11",282781,	"GG").allele("CA").build();
		pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[1,0,0,1,0]")); 
		//mnp others
		vcf = new VcfRecord.Builder("chr11",282781,	"GG").allele("AC").build();
		pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[1,0,0,0,1]")); 		
		//mnp match ref
		vcf = new VcfRecord.Builder("chr11",282781,	"CA").allele("AC").build();
		pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[1,0,1,0,0]")); 				
	}
	
	@Test
	public void pairTest() throws Exception{
		VcfRecord vcf =  new VcfRecord.Builder("chr11", 282771, "CC").allele("AA").build();
		List<SAMRecord> pool = makePool(vcf);			 
		VariantPileup pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[0,1,0,0,0]")); 
		
		//one have adjacent ins, other not
		vcf = new VcfRecord.Builder("chr11",282782,	"GG").allele("AT").build();
		pileup = new VariantPileup(vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[0,1,0,0,0]"));
	}
	
	@Test
	public void insertTest() throws Exception{		 
 
		VcfRecord vcf = new VcfRecord.Builder( "chr1",	183014,	"G" ).allele( "GTT" ).build();
		List<SAMRecord> pool = makePool( vcf );
				
		//no pair with same del
		VariantPileup pileup = new VariantPileup( vcf, pool,0);
		assertTrue(pileup.getAnnotation().equals("3[0,0,1,1,1]"));
		
		//shift one base
		pileup = new VariantPileup(new VcfRecord.Builder("chr1",	183013, "G").allele("GT").build(), pool,0);
	    //there is no ins bwt 183013~183014
		assertTrue(pileup.getAnnotation().equals("3[0,0,3,0,0]")); 
	}	
	
	@Test
	public void deleteTest() throws Exception{
 		//get delete indel
		VcfRecord vs = new VcfRecord.Builder("chr1", 197, "CAG").allele("C").build();				 				
		List<SAMRecord> pool = makePool(vs);		
		//no pair with same del
		VariantPileup pileup = new VariantPileup(vs, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[0,0,0,2,0]"));
		
		//shift one of deletion to far away
		pool.get(0).setCigarString("31M2D120M");
		pileup = new VariantPileup(vs, pool,0);
		assertTrue(pileup.getAnnotation().equals("2[0,0,1,1,0]"));
		
		//shift deletion one base away
		if(pool.get(0).getReadName().endsWith("1267"))
				pool.get(0).setCigarString("125M1I10M2D15M");
		else pool.get(1).setCigarString("125M1I10M2D15M");
		pileup = new VariantPileup(vs, pool,0);
		//deletion happened bwt 197~200. 199 is last base of vcf dels; if del happen on this base belong to partial(others)
		assertTrue(pileup.getAnnotation().equals("2[0,0,0,1,1]"));
		
		//shift deletion two base away, it is adjacant/nearby deltion still belong to partial/others 
		if(pool.get(0).getReadName().endsWith("1267"))
				pool.get(0).setCigarString("125M1I11M2D14M");
		else pool.get(1).setCigarString("125M1I11M2D14M");
		pileup = new VariantPileup(vs, pool,0);
		//deletion happened bwt 197~200. 200 is one base after vcf dels; if del happen on this base belong to partial(others)
		assertTrue(pileup.getAnnotation().equals("2[0,0,0,1,1]"));
		
		//shift deletion three base away, it is byond deltion region belong to  referece 
		if(pool.get(0).getReadName().endsWith("1267"))
			pool.get(0).setCigarString("125M1I12M2D13M");
		else pool.get(1).setCigarString("125M1I12M2D13M");
		  pileup = new VariantPileup(vs, pool,0);		 
		assertTrue(pileup.getAnnotation().equals("2[0,0,1,1,0]"));
	}	
	
	 // @return a SAMRecord pool overlap the indel position
	private List<SAMRecord> makePool(VcfRecord vs) throws IOException{
		//make pool
		List<SAMRecord> pool = new ArrayList<SAMRecord>();				
		try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File(inputBam));){
	        for(SAMRecord record : inreader){
	        	if(record.getAlignmentStart() <= vs.getPosition() && record.getAlignmentEnd() > vs.getChrPosition().getEndPosition())
	        	pool.add(record);
	        }
		}	
		
		return pool; 
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

    GCAGCGTCAGAGGTTTATAAGTTACAG CTTCTTCATCCACT C TTT G AGGCAATGAC A    AC CACTGTGCCAT     CTG    (second pair read base)
    | ----------- 27S ------- | | ------------ 30M -------------- | 2D | ------ 16M ------- |    27S30M2D16M 
                                |----------18------| | |----10--| |del |--------16----------|    MD:Z:18C11^GA16
                                                     |            |   
                                              snp: C->G         T->A 
	 */	    
    private static List<String> makeReads4Pair(){
    	 List<String> data = new ArrayList<String>();
    	 
         data.add("HVN7YBGXY:3:12503:8213:1979	99	chr11	282739	54	30M2D13M3I3M25S	=	282739	48	CTTCTTCATCCACTATTTCAGGCAATGACAAACACTGTGCCATATGCTGTATCTTATACACATCACCCAGCCCA	"
           		+ "AAAAA//AE/EE/E//EEEA/E//EEE////A///EE/E/EEE/A/EEEEEE/EE/AAEEE/A/////A/</AE	MD:Z:14C14T^GA0C0C14	RG:Z:qtest::Test");  
  
         data.add("HVN7YBGXY:3:12503:8213:1979	147	chr11	282739	60	27S30M2D16M	=	282739	-48	GCAGCGTCAGAGGTTTATAAGTTACAGCTTCTTCATCCACTCTTTGAGGCAATGACAACCACTGTGCCATCTG	"
             	+ "AAAAA//AE/EE/E//EEEA/E//EEE////A///EE/E/EEE/A/EEEEEE/EE/AAEEE/A/////A/</A	MD:Z:18C11^GA16	RG:Z:20140717025441134");  
   	    	 
    	 return data; 
    }   

    
    private static List<String> makeReads4Indel(){
    	
        List<String> data = new ArrayList<String>();
        data.add("1997_1173_1256	99	chr1	183011	60	112M1D39M	=	183351	491	" + 
        		"TATGTTTTTTAGTAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCTAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATTGGATTACAGGCATAAGCCACTGCCCCAAGCCTAAAATTTTTTAAAGTACCAT	FFFFFFJFJJJJFJJFJFJJFAJJJJJJJJJJFJJJJJJJFJJJJJJJFJFJJJJJJJJJFJAJJJJJJJJJJJJJJJJFJFJJJJFJJJFFFJJJJAFFJFJJJJJFJFJFJJJFJJJFFJJJAFFJJJJFAFJFFFJJAFFAJJJJFJ7	ZC:i:4	MD:Z:112^A39	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");	                                 
         
        //INS
        data.add("1997_1173_1257	163	chr1	182999	60	16M1I105M29S	=	183397	540	" + 
        		"TACATTTAAAAATATGTTTTTTTAATAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCAAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATTGGATTACAGGNANNNNNNNNNGNCNNNNNGCTAAAANTT	AAFFFJJJJJJJJJJJAJJJFJJJJJJ<J7FFJF-JFAFJJ<AJAFAJFF-FJJ-FJJAAFFJAFA-FFAFF<FFAFJAFJ<JJA7F-<-AJ<<J<F<FFJ-J<A7J-F-FJA7-<F<7J<<#-#########A#A#####AAFFFJA#--	ZC:i:4	"
        		+ "MD:Z:23G38T0C57	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");// MD:Z:23G38T58 before
        data.add("1997_1173_1258	163	chr1	183001	9	14M2I106M29S	=	183287	440	" + 
        		"CATTTAAAAATATGTTTTTTTTAATAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCAAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATGGGATTACAGGCNTNNNNNNNNNCNCNNNNNCTAAAATNTT	AFFFFJJJJJJJJJJJJJJJJJJJJJFJJJJJJJJJJJJJJJJJJJJJJFJJJJJJJJJAJJJJJJJJJJAFJJJFJJJJJJJJJJJJJJJJJJJFJFFFAJJJJJJJJJJJJJJJJJJJJJ#A#########J#A#####JFJJJJF#JF	ZC:i:4	MD:Z:21G38T47T11	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");

       //deletion
        data.add("1997_1173_1267	113	chr1	64	60	125M1I9M2D16M	chr1	72846	0	" +
              "GAAAATACTAAACCACACCAGGTGTGGTGTCACATGCCTGTGGTCTCAGGTACTTGGGAGGCTGAGGTGGGAGGATCGCTTGAACCCAGGAAGTTGAGGCTGCAGTGAGTTGTGATTACACCAGCCTGGGTGACAGTGTCACCCTGTCTCA	JF7-<7--7-77-JJFFFJFJF<JJ<<JAFJAJJAAF<JJ<AFJ-JJAJJJAJAJJAAAJF-JFF7FFJFJAFAFAFJA<JF--FJA-F--JJAJJFJJ<FJJJJ<JJJJJJJJJJJJJJ<FAFJ<AA-JJJ<JJJJJJJJJFAFJAFFAA	ZC:i:5	"
        + "MD:Z:11G0T121^AG17	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");
        
       data.add("1997_1173_1268	177	chr1	67	57	131M2D20M	chr1	72680	0	" +
    		   "AATACTAAAACACACCAGGTGTGGTGTCACATGCCTGTGGTCTCAGGNANTNGNGANGNTNAGGTGGGAGGATCGCTTGAACCCAGGAAGTTGAGGCTGCAGTGAGTTGTGATTACACCAGCCTGGGTGACAGTGTCACCCTGTCTCAAAA	JJJJFJJFFFJJFJJJFFJJJJJJJJJJFJJJJJJJJJJJJJJJJJJ#J#J#J#JJ#J#F#JJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJFJJJJJJJJFFFAA	ZC:i:4	"
       + "MD:Z:47A1G1A1C2C1G1C70^AG20	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");    
       
       return data;    	
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
     
		
		new File(ftmp).delete();
    }
     
//     @Test
//     public void xuTest(){
//    	 List<String> original = new ArrayList<>();
//    	 original.add("one");
//    	 original.add("two");
//    	 
//    	 List<String> second = new ArrayList<>(original);
//    	 second.add("three");
//    	 System.out.println("original.size is "+ original.size());
//    	 System.out.println("second.size is "+ second.size());
//    	 
//    	 
//    	 List<String> third = ops(second);
//       	 System.out.println("third.size is "+ third.size());
//    	 System.out.println("second.size is "+ second.size());
//     	 
//     }
//     
//     private  List<String> ops(List<String>  in ){
//    	 List<String> third = in;
//    	 third.add("four");
//    	 return third; 
//     }
}
