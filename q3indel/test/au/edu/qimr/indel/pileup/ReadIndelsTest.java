package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.indel.Main;
import au.edu.qimr.indel.Q3IndelException;
import au.edu.qimr.indel.Support;

public class ReadIndelsTest {
	
	File input1;
	File input2;
	File input3;	
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();

	@Before 
	public void createInput() throws IOException {	
		input1 = testFolder.newFile("input1.vcf");
		input2 = testFolder.newFile("input2.vcf");
		input3 = testFolder.newFile("input3.vcf");		
		createVcf();		
	}
		
	@Test
	public void multiAltTest(){
		
		//only keep one variants since the allel CA is CTX
		//we only deal with indel which is single base on ref or allel
		//indel size can't be exceed 200
		try{
			ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));		
			indelload.loadIndels(input3);				
			Map<ChrRangePosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
            assertEquals(1, positionRecordMap.size());
			 
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
                assertEquals(1, indel.getIndelVcfs().size());
				assertEquals("C", indel.getIndelVcf(0).getAlt());
				
				//check format GT field
				List<String> format = indel.getIndelVcfs().getFirst().getFormatFields();
				VcfFormatFieldRecord record = new VcfFormatFieldRecord(format.get(0), format.get(1));
                assertEquals(".", record.getField("GT"));
				
				record = new VcfFormatFieldRecord(format.get(0), format.get(2));
                assertEquals(".", record.getField("GT"));
			}

		}catch(Exception e){
            fail();
		}		
	}

	
	@Test
	public void appendIndelsTest(){
				 
		try{
			ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));		
			indelload.loadIndels(input1);
            assertEquals(7, getHeaderLineCounts(indelload.getVcfHeader()));
			//in case of GATK, take the second sample column
			indelload.appendTestIndels(input2);
            assertEquals(8, getHeaderLineCounts(indelload.getVcfHeader()));
			
			Map<ChrRangePosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
            assertEquals(3, positionRecordMap.size());
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
				if(indel.getStart() == 59033287){
					//merge only
					System.out.println("indel.getIndelVcf(0).getFormatFields().get(0): " + indel.getIndelVcf(0).getFormatFields().get(0));
					assertEquals("GT:AD:DP:GD:GQ:PL", indel.getIndelVcf(0).getFormatFields().get(0));
					assertEquals("0/0:0:0:GT/GT:0:0,0,0", indel.getIndelVcf(0).getFormatFields().get(1));
					assertEquals("0/1:131,31:162:GT/G:99:762,0,4864", indel.getIndelVcf(0).getFormatFields().get(2));
                    assertEquals("SOMATIC1", indel.getIndelVcf(0).getInfo()); //info column from first file
				}else if(indel.getStart() == 59033423){	
					//merge indels but split alleles
					assertEquals("0/1:7,4:11:T/TC:99:257,0,348", indel.getIndelVcf(0).getFormatFields().get(1));
 					assertEquals(".:7,5:.:T/A:.:.", indel.getIndelVcf(0).getFormatFields().get(2));
					assertEquals("./.:.:.", indel.getIndelVcf(1).getFormatFields().get(1));
					assertEquals(".:T/A:7,5", indel.getIndelVcf(1).getFormatFields().get(2));
                    assertEquals("SOMATIC1", indel.getIndelVcf(0).getInfo()); //info column from first file
                    assertEquals("SOMATIC", indel.getIndelVcf(1).getInfo());  //info column from second file
				}else if(indel.getStart() == 59033286){
					//indels only appear on second file,  
					assertEquals("0/1:GGT/G:131,31:162:99:762,0,4864", indel.getIndelVcf(0).getFormatFields().get(2)); 
					assertEquals("./.:.:.:.:.:.", indel.getIndelVcf(0).getFormatFields().get(1));
                    assertEquals("SOMATIC", indel.getIndelVcf(0).getInfo()); //info column from second file
				}						 
			}
		}catch(Exception e){
			System.err.println(Q3IndelException.getStrackTrace(e));
            fail();
		}		
	}
	
	@Test
	//test different records with same hashcode, but hashCode() can change from Java versions
	public void loadIndelsTest2()  {
		List<String> data = new ArrayList<>();
		data.add("chr2\t71697867\t.\tTTTCC\tT\t115.73\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.361;ClippingRankSum=-0.361;DP=11;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=1.881;QD=10.52;ReadPosRankSum=-0.922;SOR=1.609\tGT:AD:DP:GQ:PL\t0/1:5,4:9:99:153,0,145");
		data.add("chr2\t241133989\t.\tGAGGTGGAGCGTAGTGTTGAAATTGCATCCATTGTGGGGCAGTGTTGGA\tG\t457.73\t.\t AC=1;AF=0.500;AN=2;BaseQRankSum=2.462;ClippingRankSum=-0.359;DP=26;FS=3.628;MLEAC=1;MLEAF=0.500;MQ=59.33;MQ0=0;MQRankSum=-2.103;QD=17.61;ReadPosRankSum=-0.449;SOR=0.991\tGT:AD:DP:GQ:PL\t0/1:13,13:26:99:495,0,2645");
			
		Support.createVcf(data, input1);
		
		ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));				
		try{
			//load a single file
			indelload.loadIndels(input1);	
			Map<ChrRangePosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
            assertEquals(2, positionRecordMap.size());
			
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
	 
				if (indel.getStart() == 71697868 && indel.getFullChromosome().equals("chr2")){
					assertEquals("TTCC", indel.getMotif(0));
				} else if (indel.getStart() == 241133990 && indel.getFullChromosome().equals("chr2")){
					assertEquals("AGGTGGAGCGTAGTGTTGAAATTGCATCCATTGTGGGGCAGTGTTGGA", indel.getMotif(0));
				} else
                    fail();
				//can't run it for all environment 	 
 			}	
		}catch(Exception e){
			System.out.println(Q3IndelException.getStrackTrace(e));
            fail();
		}		
	}		
			
	@Test
	//load inputs only in case of pindel
	public void loadIndelsTest()  {
		
	//	createVcf();
		ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));				
		try{
			//load single file
			indelload.loadIndels(input1);
            assertEquals(7, getHeaderLineCounts(indelload.getVcfHeader()));
			
			//load second file, in case of pindel
			indelload.loadIndels(input2);
            assertEquals(8, getHeaderLineCounts(indelload.getVcfHeader()));

			Map<ChrRangePosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
            assertEquals(3, positionRecordMap.size());
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
                assertNotEquals(indel.getIndelVcf(0).getFormatFields().get(1), indel.getIndelVcf(0).getFormatFields().get(2));
				if(indel.getStart() == 59033423){
                    assertEquals("C", indel.getMotif(0));
                    assertEquals("CG", indel.getMotif(1));
 					 					
					//check GT:GD  from existing one,  no long overwrite existing one
                    assertEquals("GT:GD:AD:DP:GQ:PL", indel.getIndelVcf(0).getFormatFields().get(0));
                    assertEquals("0/1:T/TC:7,4:11:99:257,0,348", indel.getIndelVcf(0).getFormatFields().get(1));
                    assertEquals("0/1:T/TC:17,2:19:72:72,0,702", indel.getIndelVcf(0).getFormatFields().get(2));
                    assertEquals(".:T/A:7,5", indel.getIndelVcf(1).getFormatFields().get(1));
                    assertEquals(".:A/TC:9,9", indel.getIndelVcf(1).getFormatFields().get(2));
				}			
 			}
		 		
			//change inputs order
			indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));	
			indelload.loadIndels(input2);

            assertEquals(7, getHeaderLineCounts(indelload.getVcfHeader()));
			
			//load second file, in case of pindel
			indelload.loadIndels(input1);
            assertEquals(8, getHeaderLineCounts(indelload.getVcfHeader()));
			
		}catch(Exception e){
			System.out.println(Q3IndelException.getStrackTrace(e));
            fail();
		}
	}
	
	private int getHeaderLineCounts(VcfHeader header){
		int no = 0; 
		for(final VcfHeaderRecord record: header ) { 
			no ++;		
		}
		return no; 		
	}
	
	public void createVcf(){	
		
		List<String> head = new ArrayList<>();
		head.add("##fileformat=VCFv4.1");
		head.add("##FILTER=<ID=PASS,Description=\"test\">");
		head.add("##INFO=<ID=SOMATIC,Number=0,Type=Flag,Description=\"test\">");
		head.add("##contig=<ID=chrMT,length=16569>");     
		head.add("##contig=<ID=chrY,length=59373566>");	  
				    
		List<String> head1 = new ArrayList<>(head);
		head1.add("##INFO=<ID=SOMATIC1,Number=0,Type=Flag,Description=\"test1\">");       
		head1.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tS1\tS2");
		List<String> data1 = new ArrayList<>();
		data1.add("chrY	59033286	.	GT	G	724.73	PASS	SOMATIC1	GT:AD:DP:GQ:PL	0/0:0:0:0:0,0,0	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033423	.	T	TC	219.73	PASS	SOMATIC1	GT:AD:DP:GQ:PL	0/1:7,4:11:99:257,0,348	0/1:17,2:19:72:72,0,702"); 
		Support.createVcf(head1, data1, input1);
		       
		head1 = new ArrayList<>(head);
		head1.add("##PG=\"creating second file\"");
		head1.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tS1\tS2");      
		data1.clear();        
		data1.add("chrY	59033285	.	GGT	G	724.73	PASS	SOMATIC	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033286	.	GT	G	724.73	PASS	SOMATIC	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033423	.	T	A,TC,TCG	219.73	PASS	SOMATIC	GT:AD	0/1:7,5	1/2:9,9");            
		Support.createVcf(head1, data1, input2);
		
		data1.clear();
		data1.add("chrY	59033286	.	CAA	C,CA	724.73	PASS	.	GT:AD:DP:GQ:PL	1/2:14,38,25:77:99:1229,323,592,448,0,527	1/1:14,38,25:77:99:1229,323,592,448,0,527");
		data1.add("chrY	59033287	.	GTGTGTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT	G	724.73	PASS	.	GT:AD	1/2:14,38,25	1/1:14,38,25");
		data1.add("chrY	59033287	.	G	GTGTGTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT	724.73	PASS	.	GT:AD	1/2:14,38,25	1/1:14,38,25");

		Support.createVcf(head1, data1, input3);           
	}	
}
