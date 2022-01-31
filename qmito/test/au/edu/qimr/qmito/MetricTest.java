package au.edu.qimr.qmito;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import au.edu.qimr.qmito.lib.*;


public class MetricTest {
	private String input;
	private String ref;
	private String log;
	private String output;
 
	 @Rule
	 public TemporaryFolder testFolder = new TemporaryFolder();

	
	@Before
	public void createInput() throws IOException{	
		input = testFolder.newFile("input.bam").getAbsolutePath();
		TestFile.createBam(input);
		
		ref = testFolder.newFile("input.fa").getAbsolutePath();	
		TestFile.createRef(ref);
		
		log = testFolder.newFile("output.log").getAbsolutePath();
		output = testFolder.newFile("output.tsv").getAbsolutePath();
		
	}
	
	@Test
	public void pilelineTest(){
		//here we use fake reference file, since it won't be used only passing to option 
		String[] args = {  "--reference", ref, "--input", input, "--output", output, "--log",log, "--lowread-count","2", "--nonref-threshold" , "50" };
		try {
			MetricOptions options = new   MetricOptions(args);
			//here we can't call mito.report since we testing data don't provide reference index file		
			Metric mito = new Metric(options);
		    
			//Here we only test last three base of chrMT
			//int posStart = 16567;	
			int posStart = 567;		
			int[][] positionCounts = new int[][]{
					//position 16567: 1 read mapped with referece base 'A' ; total 1 reads < 2 lowreadcount is ture; nonreference Reads is 0 < 50%; so highNonreference is false
					{1,0,0,0,0,1,0},  
					//position 16568: 2 reads mapped with referece base 'T'; total 2 reads >= 2  lowreadcount is false; nonreference Reads is 0, ighNonreference is false
					{0,2,0,0,0,0,0}, 
					//position 16569: 1 read mapped with referece base 'G', 2 with non referenece base 'C'; 
					//total reads 3 >= 2 lowreadcount is false;  nonreference Reads is 2 > 50%, ighNonreference is true
					{0,0,1,2,1,0,2}    
			};
				
			Map<String, StrandElement> map;	 
			
			for (int i = 0; i < 3; i ++){
				int[] counts = new int[7];
				 
				 map  = mito.GetReverseStrandDS().getStrandElementMap(posStart + i - 1);
				 counts[0] = (int) map.get(StrandEnum.baseA.toString()).getStrandElementMember(0);
				 counts[1] = (int) map.get(StrandEnum.baseT.toString()).getStrandElementMember(0);
				 counts[2] = (int) map.get(StrandEnum.baseG.toString()).getStrandElementMember(0);
				 counts[3] = (int) map.get(StrandEnum.baseC.toString()).getStrandElementMember(0);
				 counts[4] = (int) map.get(StrandEnum.highNonreference.toString()).getStrandElementMember(0);
				 counts[5] = (int) map.get(StrandEnum.lowRead.toString()).getStrandElementMember(0);
				 counts[6] = (int) map.get(StrandEnum.nonreferenceNo.toString()).getStrandElementMember(0);
				 assertTrue(Arrays.equals(positionCounts[i], counts));				 	 
			}
				 
		} catch (Exception e) {
			e.printStackTrace();
			fail("testing failed");
		}
	}
	
	
	
 
}
