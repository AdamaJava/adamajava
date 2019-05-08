package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.qprofiler2.summarise.ReadIDSummary.RNPattern;

public class ReadIDSummaryTest {
		
	@Test
	public void patternTest() {
		ReadIDSummary idSummary = new ReadIDSummary();			
//		NoColon("<element>"),
		RNPattern pa =  idSummary.getPattern( "V100007022_3_001R018843237".split(Constants.COLON_STRING));
		assertTrue(pa.equals(RNPattern.NoColon ));
		
//		NoColon_NCBI("<run id>.<pos>"),//pattern 1 : no colon, short name [S|E][0-9]{6}.[0-9]+  eg.  SRR3083868.47411824 99 chr1 10015 9 100M = 10028 113 ...		      
		pa =  idSummary.getPattern( idSummary.splitElements("SRR3083868.47411824"));
		assertTrue(pa.equals(RNPattern.NoColon_NCBI  ));

//		NoColon_NUN("<pos>"), // pattern 2 : [no colon, NoColon name  0-9]+   eg. 322356 99 chr1 1115486 3 19M9S = 1115486 19
		pa =  idSummary.getPattern( "322356".split(Constants.COLON_STRING));
		assertTrue(pa.equals(RNPattern.NoColon_NUN  ));

//		NoColon_BGI("<flowcell ID><lane><tile><pos>"), //<flowcell ID><lane><tile><pos>   eg. bgiseq500 : FCL300002639L1C017R084_416735  
		pa =  idSummary.getPattern( idSummary.splitElements("CL100004581L2C004R050_89689"));
		assertTrue(pa.equals(RNPattern.NoColon_BGI  ));

//		OneColon("<element1>:<element2>"), //not sure
		pa =  idSummary.getPattern( idSummary.splitElements("FCL300002639L1:017R084_416735"));
		assertTrue(pa.equals(RNPattern.OneColon ));
		
//		TwoColon("<element1>:<element2>:<element3>"),   		
		pa =  idSummary.getPattern(idSummary.splitElements("V100007022L3C001::843237"));	
		assertTrue(pa.equals(RNPattern.TwoColon));	
		
//		TwoColon_Torrent("<Run_ID>:<x-pos>:<y-pos>"),  // pattern 4 : <Run> : <x-pos> : <y-pos>  eg. WR6H1:09838:13771 0ZT4V:02282:09455		
		pa =  idSummary.getPattern(idSummary.splitElements("WR6H1:09838:13771"));	
		assertTrue(pa.equals(RNPattern.TwoColon_Torrent));	
		
//		ThreeColon("<element1>:<element2>:<element3>:<element4>"), //not sure
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-ST797_0059:3:2205:20826"));
		assertTrue(pa.equals(RNPattern.ThreeColon ));
		
//		FourColon("<element1>:<element2>:<element3>:<element4>:<element5>"),// pattern 5 :  <instrument><_run id>:<lane>:<tile>:<x-pos>:<y-pos><#index>  eg. hiseq2000: HWI-ST797_0059:3:2205:20826:152489#CTTGTA		
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444x:1268y"));	
		assertTrue(pa.equals(RNPattern.FourColon));

//		FourColon_OlderIllumina("<instrument_id>:<lane>:<tile>:<x-pos>:<y-pos><#index></pair>"),
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-ST797_0059:3:2205:20826:152489#CTTGTA"));
		assertTrue(pa.equals(RNPattern.FourColon_OlderIllumina ));
		
//		FourColon_OlderIlluminaWithoutIndex("<instrument_id>:<lane>:<tile>:<x-pos>:<y-pos>"),
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444:1268"));	
		assertTrue(pa.equals(RNPattern.FourColon_OlderIlluminaWithoutIndex));
				
//		FiveColon("<element1>:<element2>:<element3>:<element4>:<element5>:<element6>"), //not sure
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444:1268:1"));	
		assertTrue(pa.equals(RNPattern.FiveColon));
				
//		SixColon("<element1>:<element2>:<element3>:<element4>:<element5>:<element6>:<element7>"), //pattern 6 :  <instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos> eg. MG00HS15:400:C4KC7ACXX:4:2104:11896:63394
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056#CAXXX"));	
		assertTrue(pa.equals(RNPattern.SixColon));

//		SixColon_Illumina("<instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos>"), //pattern 6 :  <instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos> eg. MG00HS15:400:C4KC7ACXX:4:2104:11896:63394
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056"));	
		assertTrue(pa.equals(RNPattern.SixColon_Illumina));

//		SevenColon_andMore("<element1>:<element2>:...:<elementn>");
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056:CAXXX"));	
		assertTrue(pa.equals(RNPattern.SevenColon_andMore));				
	}
	
	@Test
	public void poolTest() {
 			
		ReadIDSummary idSummary = new ReadIDSummary();
		
		String name = "SRR3083868.";
		for(int i = 1  ; i <= 10; i ++ ) idSummary.parseReadId(name + i);			
		//only 9 of first 10 into random pool, another is inside uniq pool
		assertTrue( idSummary.pool_random.size() == 9 );
	
		for(int i = 11; i <= 1000; i ++)  idSummary.parseReadId(name + i);		 
		//uniq pool always one which is the first read since same pattern and column0
		assertTrue( idSummary.pool_uniq.size() == 1 );					
		assertTrue( idSummary.patterns.keySet().size() == 1 );			
 		assertTrue( idSummary.pool_random.size() == 19 );
		 
		name = "FCL300002639L1C017R084_";		
		for(int i = 1001; i <= 1000000; i ++) idSummary.parseReadId(name + i);
		//second pattern appears in the first 2M reads			
		assertTrue( idSummary.pool_uniq.size() == 2 );
		//because they are selected randomly, each time number are slightly different
		//assertTrue( idSummary.pool_random.size() == 119 );
	
		name = "FCL300002639L2C017R084_"; //new lane
		for(int i = 1000001; i <= 2000000; i ++) idSummary.parseReadId(name + i);		
		name = "CL300002639L2C017R084_";  //new flowcell
		for(int i = 2000001; i <= 3000000; i ++) idSummary.parseReadId(name + i);		
		name = "CL300002639L2C017R084";	 //new tile but not count into uniq
		for(int i = 3000001; i <= 5000000; i ++) idSummary.parseReadId(name + i);		
		
		assertTrue( idSummary.patterns.keySet().size() == 2 );
		assertTrue( idSummary.patterns.get( RNPattern.NoColon_NCBI.toString() ).get() == 1000 );
		assertTrue( idSummary.patterns.get( RNPattern.NoColon_BGI.toString() ).get() == 4999000 );
			
		assertTrue( idSummary.columns[0].keySet().size() == 1 );
		assertTrue( idSummary.columns[0].get("SRR3083868").get() == 1000 );
				
		assertTrue( idSummary.flowCellIds.get("FCL300002639").get() == 1999000 );
		assertTrue( idSummary.flowCellIds.get("CL300002639").get() == 3000000 );
		
		assertTrue( idSummary.flowCellLanes.get("L1").get() == 999000 );
		assertTrue( idSummary.flowCellLanes.get("L2").get() == 4000000 );
		assertTrue( idSummary.tileNumbers.get("C017R").get() == 2000000 );		
		assertTrue( idSummary.tileNumbers.get("C017R084").get() == 2999000 );
		//since we ignore tile for uniq check
		assertTrue( idSummary.pool_uniq.size() == 4 );
		assertTrue( idSummary.pool_random.size() <= ReadIDSummary.maxPoolSize  );	
		
	
				
	}
	@Test
	public void bigTileNumberTest() {
		ReadIDSummary idSummary = new ReadIDSummary();
		
		String prefix = "FCL300002639L2C017R";		 
		for(int i = 0; i < 200; i++) {
			idSummary.parseReadId(prefix + i + "_" +i);
		}
		
		assertTrue( idSummary.patterns.keySet().size() == 1 );
		assertTrue( idSummary.patterns.get( RNPattern.NoColon_BGI.toString() ).get() == 200 );
		assertTrue( idSummary.tileNumbers.get("C017R99").get() == 1 );
		assertTrue( idSummary.tileNumbers.get(ReadIDSummary.other).get() == 100 );
		assertTrue( idSummary.tileNumbers.size() == 101 );
	}

}
