package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class GoldStandardGeneratorTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void checkNonSomatic() throws Exception {
		File vcf1 = testFolder.newFile("vcf1.vcf");
		File vcf2 = testFolder.newFile("vcf2.vcf");
		File out = testFolder.newFile();
		
		
	    final List<String> data = new ArrayList<String>(4);
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
       
        //germ position but not somatic
        data.add("chr1\t100\t.\tG\tA\t.\tPASS\tFS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(vcf1));) {          
            for (final String line : data)   bw.write(line + "\n");                  
         }
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(vcf2));) {          
        	for (final String line : data)   bw.write(line + "\n");                  
        }
        
        String cmd = " -vcf " + vcf1.getAbsolutePath() + " -vcf " + vcf2.getAbsolutePath() + " -out " + out.getAbsolutePath();
        Executor exec = execute(cmd);
        assertEquals(0, exec.getErrCode());
        
        /*
         * check for output file
         */
        List<String> output = Files.lines(Paths.get(out.getAbsolutePath())).collect(Collectors.toList());
        assertEquals(4, output.size());
	}
	
	private Executor execute(final String command) throws IOException, InterruptedException {
		return new Executor(command, "au.edu.qimr.vcftools.GoldStandardGenerator");
	}
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	static void createVcf(File f) throws IOException{
        List<String> data =Arrays.asList(
        		"##fileformat=VCFv4.2",
        		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,
        		"chr1	95813205	rs11165387	C	T	.	.	FLANK=CTTCGTGTCTT;BaseQRankSum=1.846;ClippingRankSum=-1.363;DP=35;FS=3.217;MQ=60.00;MQRankSum=-1.052;QD=18.76;ReadPosRankSum=-1.432;SOR=0.287;IN=1,2;DB;VAF=0.6175;HOM=0,TCCTTCTTCGtGTCTTTCCTT;EFF=downstream_gene_variant(MODIFIER||3602|||RP11-14O19.1|lincRNA|NON_CODING|ENST00000438093||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.	0/1:16,18:Germline:21:34:.:.:PASS:99:.:.:.:656.77	./.:.:HomozygousLoss:21:.:.:.:PASS:.:NCIG:.:.:.",
        		"chr1	60781981	rs5015226	T	G	.	.	FLANK=ACCAAGTGCCT;DP=53;FS=0.000;MQ=60.00;QD=31.16;SOR=0.730;IN=1,2;DB;HOM=0,CGAAAACCAAgTGCCTGCATT;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:.	1/1:0,53:Germline:34:53:.:.:PASS:99:.:.:.:2418.77	1/1:0,60:Germline:34:60:.:.:PASS:99:.:.:.:2749.77"
        		);
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(f));) {          
            for (String line : data)   out.write(line + "\n");                  
         }  
	}
	
	@Test
	public void getGT() {
		VcfRecord vcf = new VcfRecord(new String[]{"chr1","100",".","G","A",".","PASS","FS=GTGATATTCCC","GT:GD:AC:MR:NNS","0/0:G/A:A0[0],15[36.2],G11[36.82],9[33]","0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"});
		assertEquals("0/1", GoldStandardGenerator.getGT(vcf, true));
		assertEquals("0/0", GoldStandardGenerator.getGT(vcf, false));
		assertEquals("./.", GoldStandardGenerator.getGT(null, false));
		assertEquals("./.", GoldStandardGenerator.getGT(null, true));
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","100",".","G","A",".","PASS","FS=GTGATATTCCC","GD:AC:MR:NNS","G/A:A0[0],15[36.2],G11[36.82],9[33]","G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"});
		assertEquals("./.", GoldStandardGenerator.getGT(vcf1, false));
		assertEquals("./.", GoldStandardGenerator.getGT(vcf1, true));
	}
	
	@Test
	public void addToMap() {
		Map<ChrPositionRefAlt, boolean[]> map = new HashMap<>(1024 * 64);
//		Map<ChrPositionRefAlt, AtomicInteger> map = new HashMap<>(1024 * 64);
		GoldStandardGenerator.addToMap(map, null, 0, 0, null, null,0,2);
		assertEquals(0, map.size());
		GoldStandardGenerator.addToMap(map, "1", 0, 0, null, null,0,2);
		assertEquals(1, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		
		GoldStandardGenerator.addToMap(map, "1", 0, 0, null, null,1,2);
		assertEquals(1, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][1]);
	}
	@Test
	public void addToMapAgain() {
		Map<ChrPositionRefAlt, boolean[]> map = new HashMap<>(1024 * 64);
//		Map<ChrPositionRefAlt, AtomicInteger> map = new HashMap<>(1024 * 64);
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "C	0/1",0,6);
		assertEquals(1, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		assertEquals(false, map.values().toArray(new boolean[][]{})[0][1]);
		
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "T	0/1",0,6);
		assertEquals(2, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		assertEquals(false, map.values().toArray(new boolean[][]{})[0][1]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[1][0]);
		assertEquals(false, map.values().toArray(new boolean[][]{})[1][1]);
		
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "T	0/1",1,6);
		assertEquals(2, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[1][0]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[1][1]);
//		
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "C	0/1",2,6);
		assertEquals(2, map.size());
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][0]);
		assertEquals(false, map.values().toArray(new boolean[][]{})[0][1]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[0][2]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[1][0]);
		assertEquals(true, map.values().toArray(new boolean[][]{})[1][1]);
//		
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "G	1/1",3,6);
		assertEquals(3, map.size());
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "A", "G	0/1",4,6);
		assertEquals(4, map.size());
		GoldStandardGenerator.addToMap(map, "chr1", 100, 100, "T", "G	0/1",5,6);
		assertEquals(5, map.size());
	}
	
	@Test
	public void getStringFromArray() {
		String[] array = new String[]{"hello"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		try {
			Amalgamator.getStringFromArray(array, 1);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			Amalgamator.getStringFromArray(array, 10);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			Amalgamator.getStringFromArray(array, -1);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		array = new String[]{"hello","there","world"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		assertEquals("there", Amalgamator.getStringFromArray(array, 1));
		assertEquals("world", Amalgamator.getStringFromArray(array, 2));
	}
	@Test
	public void getStringFromArrayWithDelim() {
		String[] array = new String[]{"hello&there&world"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		array = new String[]{"why&hello","up&there","dear&world"};
		assertEquals("why", Amalgamator.getStringFromArray(array, 0));
		assertEquals("up", Amalgamator.getStringFromArray(array, 1));
		assertEquals("dear", Amalgamator.getStringFromArray(array, 2));
	}
	
	@Test
	public void getPositionFromHeader() {
		String[] array = new String[]{"hello"};
		assertEquals(0, Amalgamator.getPositionFromHeader(array, "hello"));
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "there"));
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "world"));
		array = new String[]{"why&hello","there","world"};
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "hello"));
		assertEquals(0, Amalgamator.getPositionFromHeader(array, "why&hello"));
		assertEquals(1, Amalgamator.getPositionFromHeader(array, "there"));
		assertEquals(2, Amalgamator.getPositionFromHeader(array, "world"));
	}

}
