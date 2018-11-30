package au.edu.qimr.vcftools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class MergeSameSamplesTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void checkADisUpdated() throws IOException, InterruptedException {
		File vcf1 = testFolder.newFile("vcf1.vcf");
		File vcf2 = testFolder.newFile("vcf2.vcf");
		File out = testFolder.newFile();
		
		
	    final List<String> data = new ArrayList<>(4);
        data.add("##fileformat=VCFv4.0");
        data.add("##qControlBamUUID=CONTROL");
        data.add("##qTestBamUUID=TEST");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST");
       
        data.add(Arrays.stream(new String[]{"chr1","2587689",".","A","C,G",".",".","FLANK=AGCACCCACAC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","2/2:3,5,54:62:C1[]1[];G4[]1[]:A6;C26;G58:.:.:47:A2[26.5]1[41];C3[30]2[22];G37[32.59]17[33.06]","1/2:4,10,61:75:A1[]0[];C1[]0[];G0[]2[]:A13;C68;G87;T1:.:SOMATIC:10,50:A2[24.5]2[41];C4[28.25]6[24.33];G35[30.74]26[31.5]"}).collect(Collectors.joining(Constants.TAB_STRING)));
        data.add(Arrays.stream(new String[]{"chr2","91629178",".","C","A",".",".","FLANK=TCTGTATGTTC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:62,4:66:A1[]0[];C1[]6[]:A17;C91;T29:.:.:4:A3[38]1[12];C32[38.06]30[38]","0/0:123,6:130:A2[]0[];C7[]6[]:A61;C181;G2;T76:.:.:.:A6[38]0[0];C80[40.11]43[37.51];T0[0]1[41]"}).collect(Collectors.joining(Constants.TAB_STRING)));
        
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(vcf1));) {          
            for (final String line : data)   bw.write(line + "\n");                  
         }
        
        /*
         * remove last 2 entries and add 2 new ones
         */
        data.remove(data.size()-1);
        data.remove(data.size()-1);
        data.add(Arrays.stream(new String[]{"chr1","2587689",".","A","G",".",".","DP=60;ExcessHet=3.0103;FS=0.000;MQ=53.08;QD=34.17;SOR=1.919","GT:AD:DP:GQ:QL:FT:INF","1/1:0,56:56:99:1913.77:.:.","1/1:0,91:91:99:2811.77:.:."}).collect(Collectors.joining(Constants.TAB_STRING)));
        data.add(Arrays.stream(new String[]{"chr2","91629178",".","C","T",".",".","BaseQRankSum=-0.281;ClippingRankSum=0.000;DP=92;ExcessHet=3.0103;FS=0.954;MQ=33.50;MQRankSum=-1.018;QD=7.31;ReadPosRankSum=1.395;SOR=0.846","GT:AD:DP:GQ:QL:FT:INF","0/1:69,22:91:99:664.77:.:.","0/1:147,47:194:99:1551.77:.:."}).collect(Collectors.joining(Constants.TAB_STRING)));
        
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
        output.forEach(System.out::println);
        assertEquals(12, output.size());
        
        VcfRecord mergedRec = new VcfRecord(output.get(10).split("\t"));
        assertEquals("chr1", mergedRec.getChromosome());
        assertEquals(2587689, mergedRec.getPosition());
        assertEquals("A", mergedRec.getRef());
        assertEquals("C,G", mergedRec.getAlt());
        assertEquals(true, mergedRec.getInfo().contains("IN=1,2"));
        
        Map<String, String[]> ffs = mergedRec.getFormatFieldsAsMap();
        assertArrayEquals(new String[]{"2/2","1/2","2/2","2/2"}, ffs.get(VcfHeaderUtils.FORMAT_GENOTYPE));
        assertArrayEquals(new String[]{"62","75","56","91"}, ffs.get(VcfHeaderUtils.FORMAT_READ_DEPTH));
        assertArrayEquals(new String[]{"A6;C26;G58","A13;C68;G87;T1",".","."}, ffs.get(VcfHeaderUtils.FORMAT_FF));
        assertArrayEquals(new String[]{"47","10,50",".","."}, ffs.get(VcfHeaderUtils.FORMAT_NOVEL_STARTS));
        assertArrayEquals(new String[]{".","SOMATIC",".","."}, ffs.get(VcfHeaderUtils.FORMAT_INFO));
        assertArrayEquals(new String[]{".",".",".","."}, ffs.get(VcfHeaderUtils.FORMAT_FILTER));
        assertArrayEquals(new String[]{"3,5,54","4,10,61","0,0,56","0,0,91"}, ffs.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS));
        
        mergedRec = new VcfRecord(output.get(11).split("\t"));
        assertEquals("chr2", mergedRec.getChromosome());
        assertEquals(91629178, mergedRec.getPosition());
        assertEquals("C", mergedRec.getRef());
        assertEquals("A,T", mergedRec.getAlt());
        assertEquals(true, mergedRec.getInfo().contains("IN=1,2"));
        
        ffs = mergedRec.getFormatFieldsAsMap();
        assertArrayEquals(new String[]{"0/1","0/0","0/2","0/2"}, ffs.get(VcfHeaderUtils.FORMAT_GENOTYPE));
        assertArrayEquals(new String[]{"66","130","91","194"}, ffs.get(VcfHeaderUtils.FORMAT_READ_DEPTH));
//        assertArrayEquals(new String[]{"A6;C26;G58","A13;C68;G87;T1",".","."}, ffs.get(VcfHeaderUtils.FORMAT_FF));
        assertArrayEquals(new String[]{"4",".",".","."}, ffs.get(VcfHeaderUtils.FORMAT_NOVEL_STARTS));
        assertArrayEquals(new String[]{".",".",".","."}, ffs.get(VcfHeaderUtils.FORMAT_INFO));
        assertArrayEquals(new String[]{".",".",".","."}, ffs.get(VcfHeaderUtils.FORMAT_FILTER));
        assertArrayEquals(new String[]{"62,4,0","123,6,1","69,0,22","147,0,47"}, ffs.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS));
        
	}
	
	private Executor execute(final String command) throws IOException, InterruptedException {
		return new Executor(command, "au.edu.qimr.vcftools.MergeSameSamples");
	}
	

}
