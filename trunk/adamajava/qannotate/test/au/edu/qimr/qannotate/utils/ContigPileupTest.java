
package au.edu.qimr.qannotate.utils;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import static org.junit.Assert.assertTrue;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import scala.actors.threadpool.Arrays;

public class ContigPileupTest {
	
	@BeforeClass
	public static void createInput() {	
		VariantPileupTest.createSam( makeReads4Pool() );		 
	}
	
	@AfterClass
	public static void deleteInput() {	
		VariantPileupTest.deleteInput();
	}		
		
	@Test
	public void resetPoolTest(){
	 	List<SAMRecord> current_pool = new ArrayList<SAMRecord>();
	 	List<SAMRecord> next_pool = new ArrayList<SAMRecord>(); 
	 	
	 	ChrPosition[] snps = new ChrPosition[]{ new ChrPointPosition( "chr11", 282753), new ChrPointPosition( "chr11", 282757) , new ChrPointPosition( "chr11", 282768) , new ChrPointPosition( "chr11", 282783 ) };	 		 	
	 	initPool( snps[0], current_pool,   next_pool );	 	
	 	assertTrue( current_pool.size() == 2 );
	 	assertTrue( next_pool.size() == 1 );	 

	 	ContigPileup pile = new ContigPileup(null,  null, null, null, null,0, null, null);
		//pile.resetPool( snps[1], current_pool,   next_pool );	
	 	pile.resetPool( new VcfRecord.Builder( snps[1], "T").build(), current_pool,   next_pool );			
	 	assertTrue( current_pool.size() == 1 );	 
	 	assertTrue( next_pool.size() == 1 );	
	 		 	
		//pile.resetPool( snps[2], current_pool,   next_pool );	
		pile.resetPool( new VcfRecord.Builder( snps[2], "T").build(), current_pool,   next_pool );	
	 	assertTrue( current_pool.size() == 2 );	 
	 	assertTrue( next_pool.size() == 0 );	
	 	
		//pile.resetPool( snps[3], current_pool,   next_pool );	 	
	 	pile.resetPool( new VcfRecord.Builder( snps[3], "T").build(), current_pool,   next_pool );	
	 	assertTrue( current_pool.size() == 0 );
	 	assertTrue( next_pool.size() == 0 );
	}
	
	@Test				
	public void runTest(){
	 	ChrPosition[] poss = new ChrPosition[]{ new ChrPointPosition( "chr11", 282753), new ChrPointPosition( "chr11", 282757) , new ChrPointPosition( "chr11", 282768) , new ChrPointPosition( "chr11", 282783 ) };
	 	VcfRecord[] snps = new VcfRecord[4];
	 	for(int i = 0; i < 4; i++ )
	 		snps[i] = new VcfRecord.Builder(poss[i],	"T").allele("A").build();
	 	
	 	AbstractQueue<VcfRecord> qIn = new ConcurrentLinkedQueue< >( Arrays.asList( snps ) ); 
		AbstractQueue< VariantPileup > queue = new ConcurrentLinkedQueue<>();
		SamReader reader =  SAMFileReaderFactory.createSAMFileReader( new File( VariantPileupTest.inputBam) ) ;				 
		 SAMSequenceRecord contig = reader.getFileHeader().getSequenceDictionary().getSequence(1); 		
		ContigPileup pile = new ContigPileup( contig, qIn, new File( VariantPileupTest.inputBam), null, queue,0, Thread.currentThread(), new CountDownLatch( 2));
		pile.run();		
	}
	
	@Test
	public void mnpTest(){
		AbstractQueue< VariantPileup > qOut = new ConcurrentLinkedQueue<>();
		AbstractQueue<VcfRecord> qIn = new ConcurrentLinkedQueue<>();
		qIn.add( new VcfRecord.Builder( "chr11", 282754, "T").allele("C").build() );
		qIn.add( new VcfRecord.Builder( "chr11", 282757, "CA").allele("AC").build() );
		qIn.add( new VcfRecord.Builder( "chr11", 282758, "AG").allele("GA").build());
		qIn.add( new VcfRecord.Builder( "chr11", 282759, "GGCAA").allele("GCAAA").build() );
								 
  	 	ContigPileup pileup = new ContigPileup( new SAMSequenceRecord("chr11", 10000000), qIn, new File( VariantPileupTest.inputBam ), null, qOut, 1, Thread.currentThread(), new CountDownLatch(1) );  
  	 	pileup.run();	  	 	
  	 	assertTrue( qOut.size() == 4 );	
  	 	
  	 	for(VariantPileup vp : qOut){
  	 		//only one reference reads
  	 		if(vp.getVcf().getPosition() == 282754) 
  	 			assertTrue(vp.getAnnotation().equals("2[0,0,2,0,0]") ); //?
  	 		
  	 		//only aaaa2222	99	chr11	282751 
  	 		else  if(vp.getVcf().getPosition() == 282757) 
  	 			assertTrue(vp.getAnnotation().equals("1[0,0,1,0,0]") );
  	 		else  if(vp.getVcf().getPosition() == 282758) 
  	 			assertTrue(vp.getAnnotation().equals("3[1,0,2,0,0]") );
  	 		
  	 		//pair aaaa2222, and  aaaa1111	147	chr11	282758
  	 		else  if(vp.getVcf().getPosition() == 282759) 
  	 			assertTrue(vp.getAnnotation().equals("3[1,0,2,0,0]") );
  	 	}				
	}	
	
	@Test
	public void indelTest(){
		AbstractQueue< VariantPileup > qOut = new ConcurrentLinkedQueue<>();
		AbstractQueue<VcfRecord> qIn = new ConcurrentLinkedQueue<>();
		qIn.add( new VcfRecord.Builder( "chr11", 282754, "T").allele("TCC").build() );
		qIn.add( new VcfRecord.Builder( "chr11", 282758, "AG").allele("A").build() );
		qIn.add( new VcfRecord.Builder( "chr11", 282758, "AGGCAA").allele("A").build());
		qIn.add( new VcfRecord.Builder( "chr11", 282759, "GGCAA").allele("G").build() );
		
						 
  	 	ContigPileup pileup = new ContigPileup( new SAMSequenceRecord("chr11", 10000000), qIn, new File( VariantPileupTest.inputBam ), null, qOut, 1, Thread.currentThread(), new CountDownLatch(1) );  
  	 	pileup.run();	   	 	
  	 	assertTrue( qOut.size() == 4 );	
  	 	
  	 	for(VariantPileup vp : qOut){
  	 		//only one reference reads
  	 		if(vp.getVcf().getRef().equals("T")) 
  	 			assertTrue(vp.getAnnotation().equals("1[0,0,1,0,0]") ); //?
  	 		
  	 		//only aaaa2222	99	chr11	282751 
  	 		else  if(vp.getVcf().getRef().equals("AG")) 
  	 			assertTrue(vp.getAnnotation().equals("1[0,0,1,0,0]") );
  	 		else  if(vp.getVcf().getRef().equals("AGGCAA")) 
  	 			assertTrue(vp.getAnnotation().equals("1[0,0,1,0,0]") );
  	 		
  	 		//pair aaaa2222, and  aaaa1111	147	chr11	282758
  	 		else  if(vp.getVcf().getRef().equals("GGCAA")) 
  	 			assertTrue(vp.getAnnotation().equals("3[1,0,2,0,0]") );
  	 	}
	}	
	
	private void initPool( ChrPosition topPos, List<SAMRecord> current_pool, List<SAMRecord> next_pool ) {
		
		 try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File( VariantPileupTest.inputBam ));){  					 
			 for(SAMRecord re : inreader){		 		
		 		//bam file already sorted, skip non-indel region record
		 		//query take longer time so put to last condition
		 		if( re.getAlignmentEnd() < topPos.getStartPosition() ) continue; 

		 		//whether in current indel region
		 		if ( re.getAlignmentStart() <= topPos.getEndPosition() ) {
		 			current_pool.add(re) ;			 			
		 		} else {			 			
		 			next_pool.add(re);   		 			
		 			break; 
		 		}			 		
		 	}		  					 
		}  catch(IOException e ){
	 		System.err.println(e.getMessage());
	 	}
	}
		
    /**
     *  aaaa2222	99:						CT C TTT C AGGCAATGAC T GA
     * 	aaaa1111	99:	    CTTCCTTCTTCATCCACT A T
         ref         ***********CTTCTTCATCCACT C TTT C AGGCAATGAC T GA CC CACTGTGCCAT     CTG ***********************
                            |   |              |     |            |                 |
                               282739       282753 282757      282768              282783       
		aaaa1111	147:	    					   AGGCAATGAC A GA CC CACTG
		aaaa2222	147:							   AGGCAATGAC A GA CC CACTG
     */    
    private static List<String> makeReads4Pool(){
    	 List<String> data = new ArrayList<String>();   	 
         data.add("aaaa1111	99	chr11	282735	54	20M	=	282755	48	CTTCCTTCTTCATCCACTAT	AAAAA//AE/EAAAAA//AE	RG:Z:20140717025441134");         
         data.add("aaaa2222	99	chr11	282751	54	20M	=	282755	48	CTCTTTCAGGCAATGACTGA	AAAAA//AE/EAAAAA//AE	RG:Z:20140717025441134");  
         data.add("aaaa2222	147	chr11	282758	54	20M	=	282735	48	AGGCAATGACAGACCCACTG	AAAAA//AE/EAAAAA//AE	RG:Z:20140717025441134"); 
         data.add("aaaa1111	147	chr11	282758	54	20M	=	282735	48	AGGCAATGACAGACCCACTG	AAAAA//AE/EAAAAA//AE	RG:Z:20140717025441134");  
         return data; 
    }	
    
}