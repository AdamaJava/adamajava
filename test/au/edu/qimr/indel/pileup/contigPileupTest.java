package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.picard.SAMFileReaderFactory;

import au.edu.qimr.indel.Q3IndelException;
import au.edu.qimr.indel.pileup.IndelMT.contigPileup;

public class contigPileupTest {
	
	@Test
	public void resetPoolTest() throws IOException{			
		String inputBam = "input.sam";
		createSam(inputBam);
		
		//get pool
		List<SAMRecord> pool = new ArrayList<SAMRecord>();
		List<SAMRecord> nextpool = new ArrayList<SAMRecord>();
		SamReader inreader =  SAMFileReaderFactory.createSAMFileReader( new File(inputBam));	
				
		VcfRecord vcf = new VcfRecord("chr11", 500, 520, null, "TAAAAAGGGGGTTTTTCCCCC", "T" );
		IndelPosition topPos = new IndelPosition(vcf);
		
        for(SAMRecord record : inreader){ 
        	//two testing data overlap indel
        	if(record.getAlignmentStart() < topPos.getEnd())
        		pool.add(record);   
        	else
        		nextpool.add(record);
        }    
        inreader.close();
        
        //before resetPool
        assertTrue(pool.size() == IndelMT.MAXRAMREADS + 20);
        assertTrue(nextpool.size() == 1);
       
        //reset for first indel
        IndelMT mt = new IndelMT();
        contigPileup pileup = mt.new contigPileup();      
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == IndelMT.MAXRAMREADS + 20);
        assertTrue(nextpool.size() == 1);
        
        vcf = new VcfRecord("chr11", 510, 530, null, "TAAAAAGGGGGTTTTTCCCCC", "T" );
        topPos = new IndelPosition(vcf);
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == IndelMT.MAXRAMREADS + 20);
        assertTrue(nextpool.size() == 1);
       
        //deletion just start one base after first input read
        vcf = new VcfRecord("chr11", 551, 571, null, "TAAAAAGGGGGTTTTTCCCCC", "T" );
        topPos = new IndelPosition(vcf);
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == 10);
        assertTrue(nextpool.size() == 1);

        vcf = new VcfRecord("chr11", 561, 581, null, "TAAAAAGGGGGTTTTTCCCCC", "T" );
        topPos = new IndelPosition(vcf);
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == 0);
        assertTrue(nextpool.size() == 1);
      		
	}
	
	   public static void createSam(String inputBam){
	        List<String> data = new ArrayList<String>();
	        data.add("@HD	VN:1.0	SO:coordinate");
	        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
	        data.add("@PG	ID:qtest::Test	VN:0.2pre");
	        data.add("@SQ	SN:chrY	LN:249250621");
	        data.add("@SQ	SN:chr11	LN:243199373");
	        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
	        
	        for(int i = 0; i < IndelMT.MAXRAMREADS + 10; i ++ )
	        data.add(i+"997_1173_1256	99	chr11	401	60	100M20D31M	=	600	351	" + 
	        		"TATGTTTTTTAGTAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCTAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATTGGATTACAGGCATAAGCCACTGCCCCAAGCCC	" +
	        		"FFFFFFJFJJJJFJJFJFJJFAJJJJJJJJJJFJJJJJJJFJJJJJJJFJFJJJJJJJJJFJAJJJJJJJJJJJJJJJJFJFJJJJFJJJFFFJJJJAFFJFJJJJJFJFJFJJJFJJJFFJJJAFFJJJJ	"+ 
	        		"MD:Z:112^A39	PG:Z:MarkDuplicates.5	RG:Z:20140717025441134");
	        
	        for(int i = 0; i < 10; i ++ )
	        data.add(i+"997_1173_1257	99	chr11	411	60	100M20D31M	=	610	351	" + 
	        		"TATGTTTTTTAGTAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCTAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATTGGATTACAGGCATAAGCCACTGCCCCAAGCCC	" +
	        		"FFFFFFJFJJJJFJJFJFJJFAJJJJJJJJJJFJJJJJJJFJJJJJJJFJFJJJJJJJJJFJAJJJJJJJJJJJJJJJJFJFJJJJFJJJFFFJJJJAFFJFJJJJJFJFJFJJJFJJJFFJJJAFFJJJJ	" +	
	        		"RG:Z:20140717025441134");	
	        
	        data.add("1997_1173_1258	99	chr11	611	60	100M20D31M	=	800	351	" + 
	        		"TATGTTTTTTAGTAGAGACAGGGTCTCACTGTGTTGCCCAGGCTAGTCTCTAACTCCTGGGCTCAAATTATCCTCCCCACTTGGCCTCCCAAAAGGATTGGATTACAGGCATAAGCCACTGCCCCAAGCCC	" +
	        		"FFFFFFJFJJJJFJJFJFJJFAJJJJJJJJJJFJJJJJJJFJJJJJJJFJFJJJJJJJJJFJAJJJJJJJJJJJJJJJJFJFJJJJFJJJFFFJJJJAFFJFJJJJJFJFJFJJJFJJJFFJJJAFFJJJJ	" +	
	        		"RG:Z:20140717025441134");		        
	        
	        	      	     	    
	        try( BufferedWriter out = new BufferedWriter(new FileWriter(inputBam ))) {	           
	           for (String line : data)  
	                   out.write(line + "\n");	           	            
	        }catch(IOException e){
	        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
	        	assertTrue(false);
	        }
	        
	        
	    }	
	   
/*&7
 * 
 * ##FORMAT=<ID=ACINDEL,Number=1,Type=String,Description="counts of indels, follow formart:
 * novelStarts,TotalCoverage,InformativeReadCount,suportReadCount[forwardsuportReadCount,backwardsuportReadCount],particalReadCount,NearbyIndelCount,NearybySoftclipCount">
 * 
 * 
 * chrY    2672735 .       AT      A       123.86  PASS    AC=1;AF=0.500;AN=2;BaseQRankSum=-0.747;ClippingRankSum=-0.747;DP=10;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.747;QD=12.39;ReadPosRankSum=-0.747;SOR=0.693;HOMCNTXT=10,GTGTGTGTGA_TTTTTTTTTT;NIOC=0.105;SVTYPE=DEL;END=2672736   
 * GT:AD:DP:GQ:PL:ACINDEL  .:.:.:.:.:5,19,17,5[3,2],1,2,2  0/1:1,7:8:2:159,0,2:10,14,13,10[6,4],0,0,0


 *         data2.add("chrY	59033285	.	GGT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
        data2.add("chrY	59033286	.	GT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
        data2.add("chrY	59033423	.	T	A,TC,TCG	219.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.034;ClippingRankSum=0.278;DP=18;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=47.46;MQ0=0;MQRankSum=-2.520;QD=12.21;ReadPosRankSum=-1.769	GT:AD:DP:GQ:PL	0/1:7,4:11:99:257,0,348	0/1:17,2:19:72:72,0,702");            
	   
 */
	   

}
