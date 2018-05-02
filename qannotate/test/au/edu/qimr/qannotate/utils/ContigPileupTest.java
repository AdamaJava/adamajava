
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
import org.qcmg.picard.SAMFileReaderFactory;
import static org.junit.Assert.assertTrue;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import scala.actors.threadpool.Arrays;

public class ContigPileupTest {
	
	@BeforeClass
	public static void createInput() {	
		SnpPileupTest.createSam( makeReads4Pool() ); 
	}
	
	@AfterClass
	public static void deleteInput() {	
		SnpPileupTest.deleteInput();
	//	new File(SnpPileupTest.inputBam).delete();
	}		
		
	@Test
	public void resetPoolTest(){
	 	List<SAMRecord> current_pool = new ArrayList<SAMRecord>();
	 	List<SAMRecord> next_pool = new ArrayList<SAMRecord>(); 
	 	
	 	ChrPosition[] snps = new ChrPosition[]{ new ChrPointPosition( "chr11", 282753), new ChrPointPosition( "chr11", 282757) , new ChrPointPosition( "chr11", 282768) , new ChrPointPosition( "chr11", 282783 ) };
	 	initPool( snps[0], current_pool,   next_pool );
	 	
	 	assertTrue( current_pool.size() == 2 );
	 	assertTrue( next_pool.size() == 1 );	 

	 	ContigPileup pile = new ContigPileup(null,  null, null, null, null, null, null);
		pile.resetPool( snps[1], current_pool,   next_pool);	
	 	assertTrue( current_pool.size() == 1 );	 
	 	assertTrue( next_pool.size() == 1 );	
	 		 	
		pile.resetPool( snps[2], current_pool,   next_pool);	
	 	assertTrue( current_pool.size() == 2 );	 
	 	assertTrue( next_pool.size() == 0 );	
	 	
		pile.resetPool( snps[3], current_pool,   next_pool);	
	 	assertTrue( current_pool.size() == 0 );
	 	assertTrue( next_pool.size() == 0 );
	}
	
	@Test				
	public void runTest(){
	 	ChrPosition[] snps = new ChrPosition[]{ new ChrPointPosition( "chr11", 282753), new ChrPointPosition( "chr11", 282757) , new ChrPointPosition( "chr11", 282768) , new ChrPointPosition( "chr11", 282783 ) };
		AbstractQueue<ChrPosition> qIn = new ConcurrentLinkedQueue< ChrPosition >( Arrays.asList( snps ) ); 
		AbstractQueue< SnpPileup > queue = new ConcurrentLinkedQueue<>();
		SamReader reader =  SAMFileReaderFactory.createSAMFileReader( new File( SnpPileupTest.inputBam) ) ;				 
		 SAMSequenceRecord contig = reader.getFileHeader().getSequenceDictionary().getSequence(1); 		
		ContigPileup pile = new ContigPileup( contig, qIn, new File( SnpPileupTest.inputBam),null, queue, Thread.currentThread(), new CountDownLatch( 2));
		pile.run();
		
		
	}

	private void initPool( ChrPosition topPos, List<SAMRecord> current_pool, List<SAMRecord> next_pool ) {
		
		 try(SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File( SnpPileupTest.inputBam ));){  					 
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