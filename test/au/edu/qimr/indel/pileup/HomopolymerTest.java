package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;

import au.edu.qimr.indel.pileup.Homopolymer.HOMOTYPE;

 

public class HomopolymerTest {

	@Test
	public void testInsert(){
		VcfRecord vs = new VcfRecord("chr1", 21, 21, null, "T", "TTAA" );
		IndelPosition indel = new IndelPosition (vs);
		Homopolymer homo = new Homopolymer(indel, getReference(), 3);
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMADJ));
		assertTrue(homo.getHomopolymerCount(0)  == 3);
		assertTrue(homo.getPolymerSequence(0).equals("GCTtaaCCC"));
		
		//check on edge of contig
		homo = new Homopolymer(indel, getReference(), 100);
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMADJ));
		assertTrue(homo.getHomopolymerCount(0)  ==18);
		assertTrue(homo.getPolymerSequence(0).length() == (18 + 3 + 21));		
		
//		
// 		System.out.println(homo.getPolymerSequence(0).length());
//		System.out.println(homo.getPolymerSequence(0)  );
//		System.out.println(homo.getHomopolymerCount(0)  );

	}
	
	@Test
	public void testEmbed(){
		VcfRecord vs = new VcfRecord("chr1", 23, 23, null, "C", "CCAC" );
		IndelPosition indel = new IndelPosition (vs);
		Homopolymer homo = new Homopolymer(indel, getReference(), 3);
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getHomopolymerCount(0)  == 3);
		assertTrue(homo.getPolymerSequence(0).equals("TCCcacCCC"));
		
		homo = new Homopolymer(indel, getReference(), 100);		
		assertTrue(homo.getHomopolymerCount(0)  ==16);
//		System.out.println(homo.getPolymerSequence(0) );
//		System.out.println(homo.getPolymerSequence(0)  );
	
		
		vs = new VcfRecord("chr1", 23, 23, null, "C", "CCCC" );
		indel = new IndelPosition (vs);
		homo = new Homopolymer(indel, getReference(), 100);		
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMEMB));
		assertTrue(homo.getHomopolymerCount(0)  ==18);
		assertTrue(homo.getPolymerSequence(0).length() == (18 + 3 + 21));
		
	}	
	
	
	
	@Test
	public void testDel(){
		VcfRecord vs = new VcfRecord("chr1", 21, 23, null, "TCC", "T" );
		IndelPosition indel = new IndelPosition (vs);
		Homopolymer homo = new Homopolymer(indel, getReference(), 3);
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getHomopolymerCount(0)  == 3);
		assertTrue(homo.getPolymerSequence(0).equals("GCT__CCC"));
		
		homo = new Homopolymer(indel, getReference(), 100);
		assertTrue(homo.getType(0).equals(HOMOTYPE.HOMCON));
		assertTrue(homo.getHomopolymerCount(0)  ==16);
		assertTrue(homo.getPolymerSequence(0).length() == (16 + 2 + 21));
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
		
		
		   CCCCC
		   CCCCC
		   CCCCC
		   CCC
		 */
				
		String ref = "AATGCAATTGGATCGGATGCTCCCCCCCCCCCCCCCCCCC";				  
		byte[] bytes = ref.getBytes();
		return bytes; 
		
	}
	
}
