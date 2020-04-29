package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;

public class HomopolymerTest {

	@Test
	public void testInsert(){
				
		VcfRecord vs = new VcfRecord( new String[] {"chr1", "21",null, "T", "TTAA" });
		IndelPosition indel = new IndelPosition (vs);		
		Homopolymer homo = new Homopolymer(indel, getReference(), 3, 3);
		assertTrue(homo.getPolymerSequence(0).equals("CCTtaaCCC"));
		assertTrue(homo.getCount(0) == 3);
	
		
		//check on edge of contig but report window still inside
		homo = new Homopolymer(indel, getReference(), 100, 10);		
//		assertTrue(homo.getUpBaseCount(0) .equals( homo.nullValue));		
//		assertTrue(homo.getDownBaseCount(0) .equals( "19C" ) );
		assertTrue(homo.getPolymerSequence(0).length() == 23);	
		assertTrue(homo.getCount(0) == 19);
		
		// report window also over edge
		homo = new Homopolymer(indel, getReference(), 100, 25);		
		assertTrue(homo.getPolymerSequence(0).length() == 43);	
		assertTrue(homo.getCount(0) == 19);		
	}
	
	@Test
	public void testEmbed(){
		VcfRecord vs = new VcfRecord(new String[] {"chr1", "23", null, "C", "CCAC" });
		Homopolymer homo = new Homopolymer(new IndelPosition (vs), getReference(), 3,3);		
		assertTrue(homo.getPolymerSequence(0).equals("TCCcacCCC"));
		assertTrue(homo.getCount(0) == 5);
				
		vs = new VcfRecord( new String[] {"chr1", "23", null, "C", "CCCC" });
		homo = new Homopolymer(new IndelPosition (vs), getReference(), 100,100);		
		assertTrue(homo.getPolymerSequence(0).equals("AATGCAATTGGATCGGACCCTCCcccCCCCCCCCCCCCCCCCC"));	
		
		assertTrue(homo.getCount(0) == 19);
		
		homo = new Homopolymer(new IndelPosition (vs), getReference(), 100,10);		
		assertTrue(homo.getPolymerSequence(0).equals("CGGACCCTCCcccCCCCCCCCCC"));	
		assertTrue(homo.getCount(0) == 19);		 
	}	
		
	@Test
	public void testDel(){
		VcfRecord vs = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "T" });
		IndelPosition indel = new IndelPosition (vs);
		Homopolymer homo = new Homopolymer(indel, getReference(), 3, 4);
		assertTrue(homo.getPolymerSequence(0).equals("CCT__CCC"));		
		assertTrue(homo.getCount(0) == 5);	
		
		vs = new VcfRecord(new String[] {  "chr1", "25", null, "CCC", "C" });
		indel = new IndelPosition (vs);
		homo = new Homopolymer(indel, getReference(), 100,10);
		assertTrue(homo.getPolymerSequence(0).equals("GACCCTCCCC__CCCCCCCCCC") );		
		assertTrue(homo.getCount(0) == 19);	
		
		//no repeats
		vs = new VcfRecord(new String[] {  "chr1", "12", null, "AT", "A" });
		indel = new IndelPosition (vs);
		homo = new Homopolymer(indel, getReference(), 100,10);	
		assertTrue(homo.getCount(0)  == 0 );
		assertTrue(homo.getPolymerSequence(0) == null);		
		
		//motif of DEL contains repeat base same to adjacant homopolymers 
		vs = new VcfRecord(new String[] {  "chr1", "18", null, "CCCTCC", "C" });
		indel = new IndelPosition (vs);
		homo = new Homopolymer(indel, getReference(), 100,10);	
		assertTrue(homo.getPolymerSequence(0).equals("TGGATCGGAC_____CCCCCCCCCC") );														
		assertTrue(homo.getCount(0) == 19);	 			
		
	}
	
	private byte[] getReference(){				
		
		/**
		1  AATGC
		6  AATTG
		11 GATCG
		16 GACCC
		21 TCCCC
		26 CCCCC
		31 CCCCC
		36 CCCCC
	*/			
					 	
		String ref = "AATGCAATTGGATCGGACCCTCCCCCCCCCCCCCCCCCCC"	;				  
		byte[] bytes = ref.getBytes();
		return bytes; 	
	}
}
