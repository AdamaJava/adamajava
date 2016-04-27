package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;

import au.edu.qimr.qannotate.modes.TandemRepeatMode.*;

public class TandemRepeatModeTest {
	String repeatFileName = "repeat.txt";
	String inputVcfName = "input.vcf";
	String outputVcfName = "output.vcf";
	
	@Test
	public void checkRepeatTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);		
		HashMap<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );				
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));
		
		assertTrue(index.firstBlockStart == 100);
		assertTrue(index.lastBlockEnd == 2000);
		
		HashMap<Integer, Block> blocks = index.index;
		assertTrue(blocks.size() == 23);
		
		Set<Block> uniqBlocks = new HashSet<Block>(blocks.values());
		int areaLength = 0;
		for(Block blk: uniqBlocks) 
			areaLength += blk.getEnd() - blk.getStart() + 1;
 		assertTrue(areaLength == (index.lastBlockEnd - index.firstBlockStart + 1));	
 		
 		assertTrue(uniqBlocks.size() == 12);
		assertTrue(blocks.get(100).getEnd() == 149);
		assertTrue(blocks.get(150).getEnd() == 199);
		assertTrue(blocks.get(200).getEnd() == 299);
		assertTrue(blocks.get(300).getEnd() == 350);
		assertTrue(blocks.get(351).getEnd() == 499);
		assertTrue(blocks.get(500).getEnd() == 550);
		assertTrue(blocks.get(551).getEnd() == 600);
		assertTrue(blocks.get(601).getEnd() == 1699);
		assertTrue(blocks.get(1700).getEnd() == 1799);
		assertTrue(blocks.get(1800).getEnd() == 1800);
		assertTrue(blocks.get(1801).getEnd() == 1900);
		assertTrue(blocks.get(1901).getEnd() == 2000);
				 		
	    TreeSet<Integer> sortedStartEnd = new TreeSet<Integer>();
	    sortedStartEnd.addAll(blocks.keySet());	    
	}
	
	@Test
	public void noBufferTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 0);		
		HashMap<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );				
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));

		//before repeat region
		VcfRecord vcf = new VcfRecord.Builder("chr1", 98, "A").allele("AT").build();
		assertFalse(trf.annotate(vcf, index));
		assertTrue(StringUtils.isMissingDtaString( vcf.getFilter()));
		assertTrue(StringUtils.isMissingDtaString( vcf.getInfo()));
		
		//second base is the start of third repeat
		vcf = new VcfRecord.Builder("chr1", 199, "A").allele("AT").build();
		assertTrue( trf.annotate(vcf, index) );	 
		assertTrue(vcf.getFilter().equals("TRF"));
		
		String[] trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		assertTrue(trfs.length == 3);
		for(String str : trfs )
			assertTrue(str.equals("8_2") || str.equals("3_14") || str.equals("16_2"));
		
		//deletion should be same with insertion
		VcfRecord vcf2 = new VcfRecord.Builder("chr1", 199, "AT").allele("A").build();
		assertTrue( trf.annotate(vcf2, index) );	
		assertTrue(trfs.length == 3);
		for(String str : trfs )
			assertTrue(str.equals("8_2") || str.equals("3_14") || str.equals("16_2"));
		
		//start at repeat start and another repeat end for SNP
		vcf = new VcfRecord.Builder("chr1", 300, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		for(String str : trfs )
			assertTrue(str.equals("15_12") || str.equals("16_2") || str.equals("8_2"));
		
		//start one base region
		vcf = new VcfRecord.Builder("chr1", 1800, "A").allele("T").build();
		assertFalse( trf.annotate(vcf, index) );
		assertTrue(vcf.getInfoRecord().getField("TRF") .equals("4_6"));
	 
		
	}
	
	
	@Test
	public void bufferTest() throws Exception{
		createRepeat();
		TandemRepeatMode trf = new TandemRepeatMode( inputVcfName, outputVcfName, 5);		
		HashMap<String, HashSet<Repeat>> repeats = trf.loadRepeat(repeatFileName );				
		BlockIndex index = trf.makeIndexedBlock(  repeats.get("chr1"));

		//before repeat region
		VcfRecord vcf = new VcfRecord.Builder("chr1", 98, "A").allele("AT").build();
		assertTrue(trf.annotate(vcf, index));
		assertTrue( vcf.getFilter().equals("TRF"));
		assertTrue( vcf.getInfoRecord().getField("TRF").equals("3_14"));
		
 		
		//start at repeat start and another repeat end for SNP
		vcf = new VcfRecord.Builder("chr1", 300, "A").allele("T").build();
		assertTrue( trf.annotate(vcf, index) );
		String[] trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		for(String str : trfs )
			assertTrue(str.equals("15_12") || str.equals("16_2") || str.equals("8_2") || str.equals("3_14"));
		
		
		//fallen in single base repeat, extend one base both side, repeat pattern same
		 trf = new TandemRepeatMode( inputVcfName, outputVcfName, 1);			 
		 vcf = new VcfRecord.Builder("chr1", 1800, "A").allele("T").build();
		 assertFalse( trf.annotate(vcf, index) );
		 trfs =  vcf.getInfoRecord().getField("TRF").split(",");
		 assertTrue(trfs.length == 3); // two repeats with same pattern different start
		 for(String str : trfs )
			assertTrue(str.equals("4_6") || str.equals("6_2") );		
	}
	
	private void createRepeat() throws IOException{
		 final List<String> data = new ArrayList<String>();		 
		 data.add("chr1\t100\t300\t3\t13.7");
		 data.add("chr1\t150\t550\t8\t2.0");
		 data.add("chr1\t200\t350\t16\t2.0");
		 data.add("chr1\t300\t500\t15\t12.2\t5\t100\t0\t22\t0.68\tAAAAC");
		 data.add("chr1\t500\t600\t5\t3.2\t5\t81\t0\t23\t0.81\tCCGCC");
		 data.add("chr1\t1700\t1799\t6\t2.0\t12\t100\t0\t24\t1.79\tTGCAGG");
		 data.add("chr1\t1801\t1900\t4\t6.0\t4\t90\t0\t30\t1.19\tGCGG");
		 data.add("chr1\t1700\t2000\t4\t6.0\t4\t90\t0\t30\t1.19\tGCGG");
	     	     		 
        try(BufferedWriter out = new BufferedWriter(new FileWriter(repeatFileName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
		
	}

}
