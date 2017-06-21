package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

import au.edu.qimr.qannotate.modes.TandemRepeatMode.*;

public class TandemRepeatModeTest {
	String repeatFileName = "repeat.txt";
	String inputVcfName = "input.vcf";
	String outputVcfName = "output.vcf";
	
	@Test
	public void checkRepeatTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);		
		Map<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );		
		assertTrue(repeats.get("chr1").size() == 9);
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));
		assertEquals(100, index.firstBlockStart);
		assertEquals(2000, index.lastBlockEnd);
		assertEquals(24, index.index.size());		// don't know where 24 comes from...
		
		assertTrue(index.firstBlockStart == 100);
		assertTrue(index.lastBlockEnd == 2000);
		
		Map<Integer, Block> blocks = index.index;
 		assertTrue(blocks.size() == 24); //some big gap will be divided to multi block, each block maximum size is 200		
		
		Set<Block> uniqBlocks = new HashSet<Block>(blocks.values());
		int areaLength = 0;
		for(Block blk: uniqBlocks) 
			areaLength += blk.getEnd() - blk.getStart() + 1;
 		assertTrue(areaLength == (index.lastBlockEnd - index.firstBlockStart + 1));	
 		
 		assertTrue(uniqBlocks.size() == 12);
		assertTrue(blocks.get(100).getEnd() == 114);
		assertTrue(blocks.get(115).getEnd() == 129);
		assertTrue(blocks.get(130).getEnd() == 299);
		assertTrue(blocks.get(300).getEnd() == 350);
		assertTrue(blocks.get(351).getEnd() == 499);
		assertTrue(blocks.get(500).getEnd() == 600);
		assertTrue(blocks.get(601).getEnd() == 1699);
		assertTrue(blocks.get(1700).getEnd() == 1799);
		assertTrue(blocks.get(1800).getEnd() == 1800);
		assertTrue(blocks.get(1801).getEnd() == 1802);
		assertTrue(blocks.get(1803).getEnd() == 1900);
		assertTrue(blocks.get(1901).getEnd() == 2000);
				 		
	    TreeSet<Integer> sortedStartEnd = new TreeSet<Integer>();
	    sortedStartEnd.addAll(blocks.keySet());	  
	    
	}
	
	@Test
	public void orderingOfTRF() {
		
		List<String> rawRepeatData = Arrays.asList("chr22\t36744042\t36744076\t12\t2.9\t12\t84\t12\t45\t1.32    TAAAAATATTTT",
"chr22\t36744085\t36744105\t3\t7.0\t3       80      20      26      0.92    TAA",
"chr22\t36744085\t36744114\t15\t2.0\t15      87      12      44      0.88    TAATAAATAAATAAA",
"chr22\t36744089\t36744114\t4\t6.5\t4       86      13      36      0.84    AATA",
"chr22\t36744104\t36744117\t5\t3.0\t5       90      10      21      0.75    AAATA",
"chr22\t36744111\t36744129\t9\t2.1\t9       100     0       38      1.00    TAAAATATT",
"chr22\t36744125\t36744136\t5\t2.4\t5       100     0       24      0.81    TATTT",
"chr22\t36744130\t36744176\t10\t4.7\t10      73      19      44      1.20    TATTTTAAGA",
"chr22\t36744118\t36744157\t12\t3.1\t12      82      10      44      1.11    TTTAAAATATTT",
"chr22\t36744113\t36744213\t44\t2.3\t45      81      6       118     1.16    AAATAATTAAAATATTTTATTTAAAAAAATTTTTAAAGATATTTT",
"chr22\t36744554\t36744568\t1\t15.0\t1       100     0       30      0.00    A",
"chr22\t36744566\t36744593\t5\t5.6\t5       100     0       56      0.94    AAATT");
		
		
		Map<String, HashSet<Repeat>> repeats = new HashMap<>();
		
		for (String s : rawRepeatData) {
			Repeat rep = new TandemRepeatMode.Repeat(s);
			repeats.computeIfAbsent(rep.chr, (v) -> new HashSet<>()).add(rep);
		}
		
		assertEquals(1, repeats.size());
		assertEquals(12, repeats.get("chr22").size());
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);	
		BlockIndex bi = trf.makeIndexedBlock(repeats.get("chr22"));
		assertEquals(36744042, bi.firstBlockStart);
		assertEquals(36744593, bi.lastBlockEnd);
		
		/*
		 * chr22	36744135	rs386395340	T	TAA	911.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=1.905;ClippingRankSum=1.154;DP=52;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQRankSum=1.264;QD=17.53;ReadPosRankSum=0.934;SOR=0.576;NIOC=0.019;SSOI=0.471;SVTYPE=INS;END=36744136;IN=1;DB;VAF=0.5014;TRF=44_2,10_5,12_3,5_2;CONF=HIGH	GT:GD:AD:DP:GQ:PL:ACINDEL	0/1:T/TAA:27,25:52:99:949,0,1035:22,52,51,24[14,10],24[22],0,1,10/1:T/TAA:57,65:122:99:2517,0,2197:52,124,120,58[27,31],60[54],0,1,1
		 */
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr22", 36744135, 36744136), "rs386395340","T", "TAA");
		 vcf.setFilter("PASS");
		 vcf.setInfo("AC=1;AF=0.500;AN=2;BaseQRankSum=1.905;ClippingRankSum=1.154;DP=52;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQRankSum=1.264;QD=17.53;ReadPosRankSum=0.934;SOR=0.576;NIOC=0.019;SSOI=0.471;SVTYPE=INS;END=36744136;IN=1;DB;VAF=0.5014;CONF=HIGH");
		 List<String> ff =  Arrays.asList("GT:GD:AD:DP:GQ:PL:ACINDEL","0/1:T/TAA:27,25:52:99:949,0,1035:22,52,51,24[14,10],24[22],0,1,1","0/1:T/TAA:57,65:122:99:2517,0,2197:52,124,120,58[27,31],60[54],0,1,1");
		 vcf.setFormatFields(ff);
		 
		 trf.annotate(vcf, bi);
		
		 
		 assertEquals("5_2,10_5,12_3,44_2", vcf.getInfoRecord().getField("TRF"));
	}
	
	
	
	@Test
	public void noBufferTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);		
		Map<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );	
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));
		assertEquals(100, index.firstBlockStart);
		assertEquals(2000, index.lastBlockEnd);
		assertEquals(24, index.index.size());		// don't know where 24 comes from...

		//before repeat region
		VcfRecord vcf = new VcfRecord.Builder("chr1", 98, "A").allele("AT").build();
		assertFalse(trf.annotate(vcf, index));
		assertTrue(StringUtils.isMissingDtaString( vcf.getFilter()));
		assertTrue(StringUtils.isMissingDtaString( vcf.getInfo()));
		
		//second base is the start of third repeat
		vcf = new VcfRecord.Builder("chr1", 99, "A").allele("AT").build();
		assertTrue( trf.annotate(vcf, index) );	 
		assertTrue(vcf.getFilter().equals("TRF"));		
		assertTrue( vcf.getInfoRecord().getField("TRF").equals(("3_14")));
 		
		//deletion should be same with insertion
		VcfRecord vcf2 = new VcfRecord.Builder("chr1", 99, "AT").allele("A").build();
		assertTrue( trf.annotate(vcf2, index) );	
		assertTrue( vcf.getInfoRecord().getField("TRF").equals(("3_14")));		
		
		//start at repeat start and another repeat end for SNP
		vcf = new VcfRecord.Builder("chr1", 500, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		assertTrue(vcf.getInfoRecord().getField("TRF").equals("5_3")  );
		
		//start one base region, in homoplymers but no strong supporting reads
		vcf = new VcfRecord.Builder("chr1", 1803, "A").allele("T").build();
		assertTrue( trf.annotate(vcf, index) );
		
		//adjacant to homoplymers
		vcf = new VcfRecord.Builder("chr1", 1800, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		
		//test new TRF rule by reading strong supporting read couts
		vcf = new VcfRecord.Builder("chr1", 1800, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		vcf.appendInfo(IndelUtils.INFO_SSOI + "=0.3");
		assertFalse( trf.annotate(vcf, index) );
		vcf.appendInfo(IndelUtils.INFO_SSOI + "=0.1");
		assertTrue( trf.annotate(vcf, index) );

		//just outside TRF region without strong supporting reads
		vcf = new VcfRecord.Builder("chr1", 2001, "A").allele("T").build();	
		assertFalse( trf.annotate(vcf, index) );
	}
		
	@Test
 	public void bufferTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 5);		
		Map<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );				
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));
		assertEquals(100, index.firstBlockStart);
		assertEquals(2000, index.lastBlockEnd);
		assertEquals(24, index.index.size());		// don't know where 24 comes from...

		//before repeat region
		VcfRecord vcf = new VcfRecord.Builder("chr1", 98, "A").allele("AT").build();
		assertTrue(trf.annotate(vcf, index));
		assertTrue( vcf.getFilter().equals("TRF"));
		assertTrue( vcf.getInfoRecord().getField("TRF").equals("3_14"));
				
		//start at repeat start and another repeat end for SNP
		vcf = new VcfRecord.Builder("chr1", 500, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		
		String[] trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		for(String str : trfs )
			assertTrue( str.equals("5_3") || str.equals("15_12") ); 
				
		//adjacant to homoplymers
		trf = new TandemRepeatMode( inputVcfName, outputVcfName, 3);			 
		vcf = new VcfRecord.Builder("chr1", 1800, "A").allele("T").build();
		assertTrue( trf.annotate(vcf, index) );
 		 
		trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		assertTrue(trfs.length == 4); // two repeats with same pattern different start
		for(String str : trfs )
			assertTrue(str.equals("4_6") || str.equals("6_2") || str.equals("1_200") );	
		 		 
		//just outside TRF region without strong supporting reads
		vcf = new VcfRecord.Builder("chr1", 2001, "A").allele("T").build();	
		assertTrue( trf.annotate(vcf, index) );
	}
	
	@Test
	public void embededTRFTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);		
		Map<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );				
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));
		assertEquals(100, index.firstBlockStart);
		assertEquals(2000, index.lastBlockEnd);
		assertEquals(24, index.index.size());		// don't know where 24 comes from...
		
		//DEL same length to homoplymers ref==16bp
		VcfRecord vcf = new VcfRecord.Builder("chr1", 115, "AAAAAAAAAAAAAAAA").allele("A").build();
		assertTrue(trf.annotate(vcf, index)); 			 
		for(String str : vcf.getInfoRecord().getField("TRF").split(",") )
			assertTrue(str.equals("1_12") || str.equals("16_2") );	

		//DEL bigger than homoplymers
		vcf = new VcfRecord.Builder("chr1", 115, "AAAAAAAAAAAAAAAAA").allele("A").build();
		assertFalse(trf.annotate(vcf, index)); 
		for(String str : vcf.getInfoRecord().getField("TRF").split(",") )
			assertTrue(str.equals("1_12") || str.equals("16_2") );	

		//DEL cross over adjacant TRF "chr1\t100\t115\t3\t13.7"
		vcf = new VcfRecord.Builder("chr1", 114, "AAAAAAAAAAAAAAAAA").allele("A").build();
		assertTrue(trf.annotate(vcf, index)); 
		assertTrue(vcf.getInfoRecord().getField("TRF").contains("1_12"));
		assertTrue(vcf.getInfoRecord().getField("TRF").contains("3_14"));
		
	}
	
	private void createRepeat() throws IOException{
		 final List<String> data = new ArrayList<String>();		 
		 data.add("chr1\t100\t115\t3\t13.7");
		 data.add("chr1\t115\t130\t1\t12.0");
//		 data.add("chr1\t115\t300\t8\t2.0");
		 data.add("chr1\t130\t350\t16\t2.0");
		 data.add("chr1\t300\t500\t15\t12.2\t5\t100\t0\t22\t0.68\tAAAAC");
		 data.add("chr1\t500\t600\t5\t3.2\t5\t81\t0\t23\t0.81\tCCGCC");		 
		 data.add("chr1\t1700\t1799\t6\t2.0\t12\t100\t0\t24\t1.79\tTGCAGG");
		 data.add("chr1\t1801\t1900\t4\t6.0\t4\t90\t0\t30\t1.19\tGCGG");		
		 data.add("chr1\t1700\t2000\t4\t6.0\t4\t90\t0\t30\t1.19\tGCGG");
		 data.add("chr1\t1803\t2000\t1\t200.0");
		  
        try(BufferedWriter out = new BufferedWriter(new FileWriter(repeatFileName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
		
	}

}
