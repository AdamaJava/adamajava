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
import au.edu.qimr.indel.pileup.IndelMT.ContigPileup;

public class ContigPileupTest {
	
	@Test
	public void resetPoolTest() throws IOException{			
		String inputBam = "input.sam";
		createSam(inputBam);
		
		//get pool
		List<SAMRecord> pool = new ArrayList<SAMRecord>();
		List<SAMRecord> nextpool = new ArrayList<SAMRecord>();
		SamReader inreader =  SAMFileReaderFactory.createSAMFileReader( new File(inputBam));	
				
		VcfRecord vcf = new VcfRecord(new String[] {"chr11", "500", null, "TAAAAAGGGGGTTTTTCCCCC", "T" });
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
        ContigPileup pileup = mt.new ContigPileup();      
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == IndelMT.MAXRAMREADS + 20);
        assertTrue(nextpool.size() == 1);
        
        vcf = new VcfRecord(new String[] {"chr11", "510", null, "TAAAAAGGGGGTTTTTCCCCC", "T" });
        topPos = new IndelPosition(vcf);
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == IndelMT.MAXRAMREADS + 20);
        assertTrue(nextpool.size() == 1);
       
        //deletion just start one base after first input read
        vcf = new VcfRecord(new String[] {"chr11", "551", null, "TAAAAAGGGGGTTTTTCCCCC", "T" });
        topPos = new IndelPosition(vcf);
        pileup.resetPool(topPos, pool, nextpool);
        assertTrue(pool.size() == 10);
        assertTrue(nextpool.size() == 1);

        vcf = new VcfRecord(new String[] {"chr11", "561", null, "TAAAAAGGGGGTTTTTCCCCC", "T" });
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
	   
}
