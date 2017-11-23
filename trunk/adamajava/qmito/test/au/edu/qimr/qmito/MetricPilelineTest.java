package au.edu.qimr.qmito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import au.edu.qimr.qmito.lib.*;


//import au.edu.qimr.qlib.qpileup.PositionElement;
//import au.edu.qimr.qlib.qpileup.QPileupRecord;
//import au.edu.qimr.qlib.qpileup.StrandDS;
//import au.edu.qimr.qlib.qpileup.StrandElement;
//import au.edu.qimr.qlib.qpileup.StrandEnum;
//import au.edu.qimr.qlib.util.Reference;


public class MetricPilelineTest {
	private String input = "./input.bam";
	private String log = "./output.log";
	private String output = "./output.tsv";
 
	
	@Before
	public void createInput() throws IOException{	
		TestFile.createBam(input);
	}
	@After
	public void deleteInput(){	
		new File(input + ".bai").delete();
		new File(input).delete();
		new File(output).delete();
	}
	
	@Test
	public void pilelineTest(){
		//here we use fake reference file, since it won't be used only passing to option 
		String[] args = {"-m", "metric", "-r", input,"-i", input, "-o", output, "--log",log, "--lowreadcount","2", "--nonrefthreshold" , "50" };
		try {
			MetricOptions options = new Options(args).getMetricOption();
			//here we can't call mito.report since we testing data don't provide reference index file		
			MetricPileline mito = new MetricPileline(options);
		    
			//Here we only test last three base of chrMT
			int posStart = 16567;			
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
 				 Assert.assertTrue(Arrays.equals(positionCounts[i], counts));				 	 
			}
				 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
/*	
 	private void readReads(Options options ){
		SAMSequenceRecord ref =  options.getReferenceRecord();
	 
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(input,ValidationStringency.SILENT);
		SAMRecordIterator ite = reader.query(ref.getSequenceName(),0, ref.getSequenceLength(), false);
		while(ite.hasNext()){
			SAMRecord record = ite.next();	
			String Sbase = new String(record.getReadBases());
			 
			System.out.println(String.format("(%d~%d:) %s",  record.getAlignmentStart() , record.getAlignmentEnd(), Sbase));
			int start = record.getAlignmentEnd() - 3;
			int end = record.getAlignmentEnd(); 
		//	System.out.println(String.format("base (%d:) %s", start ,  new String(record.getReadBases()).substring(Sbase.length()-4)));
		}
		reader.close();
	}
*/
}
