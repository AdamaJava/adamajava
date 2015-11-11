package au.edu.qimr.indel.pileup;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.header.VcfHeader;

import au.edu.qimr.indel.Main;
import au.edu.qimr.indel.Q3IndelException;

public class ReadIndelsTest {
	final static String input1 = "input1.vcf";
	final static String input2 = "input2.vcf";
	
	@Before  
	public void createInput() {	 
		createVcf();		
	}
	
	@After 
	public void clearInput() {	 		
		new File(input1).delete();
		new File(input2).delete();
	}
	
	@Test
	public void appendIndels(){
		 
		try{
			ReadIndels indelload = new ReadIndels(QLoggerFactory.getLogger(Main.class, null, null));		
			indelload.LoadIndels(new File(input1));	
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			indelload.appendIndels(new File(input2));
			assertTrue(getHeaderLineCounts(indelload.getVcfHeader()) == 7);
			
			Map<ChrPosition, IndelPosition> positionRecordMap = indelload.getIndelMap();
			
			assertTrue(positionRecordMap.size() == 3);		
			for( ChrPosition key : positionRecordMap.keySet()){
				IndelPosition indel = positionRecordMap.get(key);
				if(indel.getStart() == 59033286)
					assertFalse(indel.getIndelVcf(0).getFormatFields().get(1).equals(indel.getIndelVcf(0).getFormatFields().get(2)));
				else if(indel.getStart() == 59033423){
					assertTrue(indel.getIndelVcf(0).getFormatFields().get(1).equals(indel.getIndelVcf(0).getFormatFields().get(2)));
					assertTrue(indel.getIndelVcf(1).getFormatFields().get(2).equals(indel.getIndelVcf(0).getFormatFields().get(2)));
					assertTrue(indel.getIndelVcf(1).getFormatFields().get(1).equals(".:.:.:.:." ));
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
		
//		String[] tt = {"ok","ok1,ok2",",ok2","ok1,"};
//		for(int i = 0; i < tt.length; i ++)
//			for(String alt : tt[i].split(","))
//				System.out.println(tt[i] + " splitted to " + alt);
		
		createVcf();
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
	
	private static void createVcf(){						
        List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.1");
        data.add("##FILTER=<ID=PASS,Description=\"test\">");
        data.add("##INFO=<ID=SOMATIC,Number=0,Type=Flag,Description=\"test\">");
        data.add("##contig=<ID=chrMT,length=16569>");
        data.add("##contig=<ID=chrY,length=59373566>");	        
        
        List<String> data2 = new ArrayList<String>(data);
        data2.add("##PG:\"creating second file\"");
        data2.add("chrY	59033285	.	GGT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
        data2.add("chrY	59033286	.	GT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:131,31:162:99:762,0,4864	0/1:80,17:97:99:368,0,3028");
        data2.add("chrY	59033423	.	T	A,TC,TCG	219.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.034;ClippingRankSum=0.278;DP=18;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=47.46;MQ0=0;MQRankSum=-2.520;QD=12.21;ReadPosRankSum=-1.769	GT:AD:DP:GQ:PL	0/1:7,4:11:99:257,0,348	0/1:17,2:19:72:72,0,702");            
       //input2 with 6 head line but VcfReader will append CHROM line
        try( BufferedWriter out = new BufferedWriter(new FileWriter(input2 ))) {	           
            for (String line : data2)  
                    out.write(line + "\n");	           	            
         }catch(IOException e){
         	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
         	assertTrue(false);
         }       
        
        data.add("##INFO=<ID=SOMATIC1,Number=0,Type=Flag,Description=\"test1\">");       
        data.add("#CHROM	POS	ID      REF     ALT     QUAL	FILTER	INFO	FORMAT	S1	S2"); 
        data.add("chrY	59033286	.	GT	G	724.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=0.873;ClippingRankSum=0.277;DP=208;FS=1.926;MLEAC=1;MLEAF=0.500;MQ=57.66;MQ0=0;MQRankSum=1.328;QD=3.48;ReadPosRankSum=-0.302;END=59033287	GT:AD:DP:GQ:PL	0/1:0:0:0:0,0,0	0/1:80,17:97:99:368,0,3028");
        data.add("chrY	59033423	.	T	TC	219.73	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.034;ClippingRankSum=0.278;DP=18;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=47.46;MQ0=0;MQRankSum=-2.520;QD=12.21;ReadPosRankSum=-1.769	GT:AD:DP:GQ:PL	0/1:7,4:11:99:257,0,348	0/1:17,2:19:72:72,0,702"); 
        //input1 with 7 lines
        try( BufferedWriter out = new BufferedWriter(new FileWriter(input1 ))) {	           
           for (String line : data)  
                   out.write(line + "\n");	           	            
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }
        
	}
	
}
