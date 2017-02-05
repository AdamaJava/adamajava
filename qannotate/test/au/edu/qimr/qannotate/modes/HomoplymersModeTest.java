package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class HomoplymersModeTest {
 	

	@Test
	public void testInsert(){				
		VcfRecord re = new VcfRecord( new String[] {"chr1", "21",null, "T", "TTAA" });
		
		//small insertion inside reference region
		HomoplymersMode  homo = new HomoplymersMode(3,3);		
		re = homo.annotate(re, getReference());
		assertEquals("3,CCTtaaCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));

		//check on edge of contig but report window still inside
		homo = new HomoplymersMode( 100, 10);	
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,ATCGGACCCTtaaCCCCCCCCCC"));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
 		
		// report window also over edge
		homo = new HomoplymersMode( 100, 25);
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,AATGCAATTGGATCGGACCCTtaaCCCCCCCCCCCCCCCCCCC"));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
		
		//embed case
		re = new VcfRecord(new String[] {"chr1", "23", null, "C", "CCAC" });
		homo = new HomoplymersMode(3,3);		
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,TCCcacCCC"));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
	}

	
	@Test
	public void testDel(){
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "T" });		
		HomoplymersMode  homo = new HomoplymersMode(3,4);	
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,CCT__CCC"));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
 		
		//no repeats
		re = new VcfRecord(new String[] {  "chr1", "12", null, "AT", "A" });
		homo = new HomoplymersMode(100,10);
		re = homo.annotate(re, getReference());
		assertTrue(  re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("0,TGCAATTGGA_CGGACCCTCC") );

		//motif of DEL contains repeat base same to adjacant homopolymers 
		re = new VcfRecord(new String[] {  "chr1", "18", null, "CCCTCC", "C" });
		homo = new HomoplymersMode(100,10);
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,TGGATCGGAC_____CCCCCCCCCC") );	
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
	}
	
	@Test
	public void testSNP(){
		//MNP
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "AGG" });		
		HomoplymersMode  homo = new HomoplymersMode(3,4);	
		re = homo.annotate(re, getReference());
		assertEquals("5,CCCaggCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP
		re = new VcfRecord(new String[] {  "chr1", "16", null, "G", "A" });		
		homo = new HomoplymersMode(10,5);	
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("2,GATCGaACCCT"));
		
		//SNP in non homoplyers region
		re = new VcfRecord(new String[] {  "chr1", "13", null, "T", "A" });	
		re = homo.annotate(re, getReference());	 
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("0,TTGGAaCGGAC"));
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
