package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;

public class HomopolymerTest {

	@Test
	public void testInsert(){
				
		VcfRecord vs = new VcfRecord("chr1", 21, 21, null, "T", "TTAA" );
		IndelPosition indel = new IndelPosition (vs);		
		Homopolymer homo = new Homopolymer(indel, getReference(), 3);
		assertTrue(homo.getUpBaseCount(0).equals(homo.nullValue));		
		
		assertTrue(homo.getDownBaseCount(0).equals("3C"  ));
		assertTrue(homo.getPolymerSequence(0).equals("GCTtaaCCC"));
		
		//check on edge of contig
		homo = new Homopolymer(indel, getReference(), 100);		
		assertTrue(homo.getUpBaseCount(0) .equals( homo.nullValue));		
		assertTrue(homo.getDownBaseCount(0) .equals( "19C" ) );
		assertTrue(homo.getPolymerSequence(0).length() == 43);	
		
	}
	
	@Test
	public void testEmbed(){
		VcfRecord vs = new VcfRecord("chr1", 23, 23, null, "C", "CCAC" );
		Homopolymer homo = new Homopolymer(new IndelPosition (vs), getReference(), 3);		
		assertTrue(homo.getUpBaseCount(0).equals("2C")); //   .getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getDownBaseCount(0).equals("3C"));  //.getHomopolymerCount(0)  == 3);
		assertTrue(homo.getPolymerSequence(0).equals("TCCcacCCC"));
				
		vs = new VcfRecord("chr1", 23, 23, null, "C", "CCCC" );
		homo = new Homopolymer(new IndelPosition (vs), getReference(), 100);		
		assertTrue(homo.getUpBaseCount(0).equals("2C")); //   .getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getDownBaseCount(0) .equals("17C" ));  //.getHomopolymerCount(0)  == 3);
		assertTrue(homo.getPolymerSequence(0).equals("AATGCAATTGGATCGGATGCTCCcccCCCCCCCCCCCCCCCCC"));	
	}	
	
	
	
	@Test
	public void testDel(){
		VcfRecord vs = new VcfRecord("chr1", 21, 23, null, "TCC", "T" );
		IndelPosition indel = new IndelPosition (vs);
		Homopolymer homo = new Homopolymer(indel, getReference(), 3);
		assertTrue(homo.getUpBaseCount(0) .equals( Homopolymer.nullValue)); //   .getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getDownBaseCount(0).equals( "3C" ));  //.getHomopolymerCount(0)  == 3);
		
		//debug
		System.out.println(homo.getPolymerSequence(0));
		assertTrue(homo.getPolymerSequence(0).equals("GCT__CCC"));
		
		homo = new Homopolymer(indel, getReference(), 100);
		assertTrue(homo.getUpBaseCount(0).equals( Homopolymer.nullValue));
		assertTrue(homo.getDownBaseCount(0).equals( "17C"));
		assertTrue(homo.getPolymerSequence(0).equals("AATGCAATTGGATCGGATGCT__CCCCCCCCCCCCCCCCC"));
	}
	
	private byte[] getReference(){
		/**
		1  AATGC
		6  AATTG
		11 GATCG
		16 GATGC
		21 TCCCC
		26 CCCCC
		31 CCCCC
		36 CCCCC
		 */
				
		String ref = "AATGCAATTGGATCGGATGCTCCCCCCCCCCCCCCCCCCC";				  
		byte[] bytes = ref.getBytes();
		return bytes; 	
	}
}
