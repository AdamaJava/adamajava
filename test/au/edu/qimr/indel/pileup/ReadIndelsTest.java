package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.header.VcfHeader;

import au.edu.qimr.indel.Main;
import au.edu.qimr.indel.Q3IndelException;
import au.edu.qimr.indel.Support;

public class ReadIndelsTest {
	public final static String input1 = "input1.vcf";
	public final static String input2 = "input2.vcf";
	public final static String input3 = "input3.vcf";
	
	@BeforeClass  
	public static void createInput() {	 
		createVcf();		
	}
	
	@AfterClass
	public static void clearInput() {	 		
		new File(input1).delete();
		new File(input2).delete();
		new File(input3).delete();
	}
	
	@Test
	public void multiDeletionTest(){
		
		//only keep one variants since the allel CA is CTX
		//we only deal with indel which is single base on ref or allel
		try{
			ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));		
			indelload.LoadIndels(new File(input3));				
			Map<ChrPosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
			assertTrue(positionRecordMap.size() == 1);
			 
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
				assertTrue(indel.getIndelVcfs().size() == 1);
				indel.getIndelVcf(0).equals("C");
				
				//check format GT field
				List<String> format = indel.getIndelVcfs().get(0).getFormatFields();
				VcfFormatFieldRecord record = new VcfFormatFieldRecord(format.get(0), format.get(1));
				assertTrue(record.getField("GT").equals("."));
				
				record = new VcfFormatFieldRecord(format.get(0), format.get(2));
				assertTrue(record.getField("GT").equals("."));
			}

		}catch(Exception e){
			assertFalse(true);
		}		
	}

	
	@Test
	public void appendIndelsTest(){
		 
		try{
			ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));		
			indelload.LoadIndels(new File(input1));	
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			//in case of GATK, take the second sample column
			indelload.appendIndels(new File(input2));
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			
			Map<ChrPosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
			assertTrue(positionRecordMap.size() == 3);		
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
				if(indel.getStart() == 59033286)
					assertFalse(indel.getIndelVcf(0).getFormatFields().get(1).equals(indel.getIndelVcf(0).getFormatFields().get(2)));
				else if(indel.getStart() == 59033423){	
 					assertTrue(indel.getIndelVcf(0).getFormatFields().get(1).equals("0/1:7,4:11:99:257,0,348"));
 					assertTrue(indel.getIndelVcf(0).getFormatFields().get(2).equals(".:7,5:.:.:."));
					
					assertTrue(indel.getIndelVcf(1).getFormatFields().get(1).equals(".:." ));
					assertTrue(indel.getIndelVcf(1).getFormatFields().get(2).equals(".:7,5"));
				}else if(indel.getStart() == 59033285){
					assertFalse(indel.getIndelVcf(0).getFormatFields().get(1).equals(indel.getIndelVcf(0).getFormatFields().get(2))); 
					assertTrue(indel.getIndelVcf(0).getFormatFields().get(1).equals(".:.:.:.:." ));						 
				}						 
			}

		}catch(Exception e){
			assertFalse(true);
		}
		
	}
	 
	@Test
	public void LoadIndelsTest()  {
		
	//	createVcf();
		ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));				
		try{
			//load single file
			indelload.LoadIndels(new File(input1));	
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			
			//load second file, in case of pindel
			indelload.LoadIndels(new File(input2));		
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);

			Map<ChrPosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
			assertTrue(positionRecordMap.size() == 3);		
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
				assertFalse(indel.getIndelVcf(0).getFormatFields().get(1).equals(indel.getIndelVcf(0).getFormatFields().get(2)));
				if(indel.getStart() == 59033423){
					assertTrue( indel.getMotif(0).equals("C"));
					assertTrue( indel.getMotif(1).equals("CG"));
					assertTrue(indel.getIndelVcf(0).getFormatFieldStrings().equals(indel.getIndelVcf(1).getFormatFieldStrings()  ));				 
				}			
 			}
		 		
			//change inputs order
			indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));	
			indelload.LoadIndels(new File(input2));			
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			
			//load second file, in case of pindel
			indelload.LoadIndels(new File(input1));
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 8);		
			
		}catch(Exception e){
			assertFalse(true);
		}
	}
	
	
	
	
	private static int getHeaderLineCounts(VcfHeader header){
		int no = 0; 
		for(final VcfHeader.Record record: header )  
			no ++;		
		return no; 		
	}
	
	public static void createVcf(){	
		
		List<String> head = new ArrayList<String>();
		head.add("##fileformat=VCFv4.1");
		head.add("##FILTER=<ID=PASS,Description=\"test\">");
		head.add("##INFO=<ID=SOMATIC,Number=0,Type=Flag,Description=\"test\">");
		head.add("##contig=<ID=chrMT,length=16569>");     
		head.add("##contig=<ID=chrY,length=59373566>");	  
		
		    
		List<String> head1 = new ArrayList<String>(head);
		head1.add("##INFO=<ID=SOMATIC1,Number=0,Type=Flag,Description=\"test1\">");       
		head1.add("#CHROM	POS	ID      REF     ALT     QUAL	FILTER	INFO	FORMAT	S1	S2"); 		
		List<String> data1 = new ArrayList<String>();
		data1.add("chrY	59033286	.	GT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:0:0:0:0,0,0	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033423	.	T	TC	219.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.034;ClippingRankSum=0.278;DP=18;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=47.46;MQ0=0;MQRankSum=-2.520;QD=12.21;ReadPosRankSum=-1.769	GT:AD:DP:GQ:PL	0/1:7,4:11:99:257,0,348	0/1:17,2:19:72:72,0,702"); 
		Support.createVcf(head1, data1, input1);
		       
		head1 = new ArrayList<String>(head);
		head1.add("##PG:\"creating second file\"");
		head1.add("#CHROM	POS	ID      REF     ALT     QUAL	FILTER	INFO	FORMAT	S1	S2");        
		data1.clear();        
		data1.add("chrY	59033285	.	GGT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033286	.	GT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
		data1.add("chrY	59033423	.	T	A,TC,TCG	219.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.034;ClippingRankSum=0.278;DP=18;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=47.46;MQ0=0;MQRankSum=-2.520;QD=12.21;ReadPosRankSum=-1.769	GT:AD	0/1:7,5	1/2:9,9");            
		Support.createVcf(head1, data1, input2);
		
		data1.clear();
		data1.add("chrY	59033286	.	CAA	C,CA	724.73	PASS	AC=1;END=59033287	GT:AD:DP:GQ:PL	1/2:14,38,25:77:99:1229,323,592,448,0,527	1/1:14,38,25:77:99:1229,323,592,448,0,527");
		Support.createVcf(head1, data1, input3);
            
	}
	

	
}
