package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.summarise.ReadIdSummary.RnPattern;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class ReadIDSummaryTest { 
	
	@Test
	public void fastqIdTest() throws ParserConfigurationException { 
		ReadIdSummary idSummary = new ReadIdSummary();			
		 // accept empty string as id
		RnPattern pa =  idSummary.getPattern( "".split(Constants.COLON_STRING));
		assertTrue(pa.equals(RnPattern.NoColon ));
		
		 // id not null
		try { 
			idSummary.parseReadId(null);
			fail("can't accept null id");
		}catch(Exception e) { 
			 // expect to catch exception			
		}
		
		 // remove substring after space,tab newline etc from id
		idSummary.parseReadId(" NB551151:83:HWC2VBGX9:4:13602:8142:7462	1:N:0:GGGGGG");				
		Element root = XmlElementUtils.createRootElement( "root", null);				
		idSummary.toXml(root);		
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VALUE).get(0);
		assertEquals( ele.getAttribute(XmlUtils.NAME),"NB551151:83:HWC2VBGX9:4:13602:8142:7462" );
	}
	
	@Test
	public void toXmlTest() throws ParserConfigurationException { 
		ReadIdSummary idSummary = new ReadIdSummary();	
		
		 // NoColon("<Element>")
		idSummary.parseReadId("element");		
		 // NoColon_NCBI("<Run Id>.<Pos>") eg. SRR3083868.47411824
		idSummary.parseReadId("SRR3083868.47411824");				
		 // NoColon_NUN("<Pos>"), eg. 322356 ???belong to NoColon("<Element>")
		idSummary.parseReadId(" 322356");				
		 // NoColon_BGI("<Flow Cell Id><Flow Cell Lane><Tile Number><Pos>"), like bgiseq500
		idSummary.parseReadId(" FCL300002639L1C017R084_416735");
		 // OneColon("<Element1>:<Element2>"),  // not sure
		idSummary.parseReadId("element1:element2 ");
		 // TwoColon("<Element1>:<Element2>:<Element3>"),  not sure
		idSummary.parseReadId(" element1:element2:element3");
		 // TwoColon_Torrent("<Run Id>:<X Pos>:<Y Pos>")
		idSummary.parseReadId(" WR6H1:09838:13771");
		 // ThreeColon("<Element1>:<Element2>:<Element3>:<Element4>"), not sure
		idSummary.parseReadId("element1:element2:element3:element4");
		 // FourColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>"),
		idSummary.parseReadId(" element1:element2:element3:element4:element5");
		 // FourColon_OlderIllumina("<Instruments>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos><#Index></Pair>"), hiseq2000
		idSummary.parseReadId("HWI-ST797_0059:3:2205:20826:152489#CTTGTA");
		 // FourColon_OlderIlluminaWithoutIndex("<Instrument>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos>"), hiseq2000: 
		idSummary.parseReadId("HWI-ST797_0059:3:2205:20826:152489");
		 // FiveColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>:<Element6>"), not sure
		idSummary.parseReadId("element1:element2:element3:element4:element5:element6 ");
		 // SixColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>:<Element6>:<Element7>"), not sure
		idSummary.parseReadId("element1:element2:element3:element4:element5:element6:element7");
		 // SixColon_Illumina("<Instrument>:<Run Id>:<Flow Cell Id>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos>"),   
		idSummary.parseReadId("MG00HS15:400:C4KC7ACXX:4:2104:11896:63394");
		 // SevenColon_andMore("<Element1>:<Element2>:...:<Elementn>");   // not sure
		idSummary.parseReadId("element1:element2:element3:element4:element5:element6:element7:8");
		idSummary.parseReadId("element1:element2:element3:element4:element5:element6:element7:8:9");		
		
		Element root =  XmlElementUtils.createRootElement("root", null);
		idSummary.toXml(root);
		
		 // total 15 patterns
		assertEquals( idSummary.patterns.keySet().size(), 15 );
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP).stream().filter( k -> k.getAttribute(XmlUtils.NAME).equals("QNAME Format") ).findFirst().get() ;
		for(String pa : idSummary.patterns.keySet()) { 
			if(pa.equals(ReadIdSummary.RnPattern.SevenColon_andMore.toString())) { 
				 // only <tally count="2" value="<Element1>:<Element2>:...:<Elementn>"/>
				Element e = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY) .stream().filter(k -> k.getAttribute(XmlUtils.VALUE).equals(pa.toString())).findFirst().get();
				assertEquals( e.getAttribute(XmlUtils.COUNT), "2");
			}else { 
				 // others <tally count="1" value="..."/>
				Element e = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY) .stream().filter(k -> k.getAttribute(XmlUtils.VALUE).equals(pa.toString())).findFirst().get();
				assertEquals( e.getAttribute(XmlUtils.COUNT), "1");				
			}			
		}
		
		 // check <Element1..5>, last 2 element won't output, incase they are position and too many values
		int order = 1;
		for( int count : new int[] { 8,6,5,4,3} ) { 
			String name = "Element" + order;
			
			ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP).stream().filter( k -> k.getAttribute(XmlUtils.NAME).equals(name)).findFirst().get();
			assertEquals(ele.getAttribute(XmlUtils.COUNT), count + "");
			 // child element
			ele = XmlElementUtils.getChildElement(ele, XmlUtils.TALLY, 0);
			assertEquals(ele.getAttribute(XmlUtils.COUNT), count + "");
			assertEquals(ele.getAttribute(XmlUtils.VALUE), "element"+ order);			
			order ++;
		}
		
		 // 3 instrument 
		List<Pair<String, Integer>> valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("HWI-ST797_0059",2));
			add( new Pair<>("MG00HS15",1) );
		}};
		checkVariableGroup( root, "Instrument", valuePair  ) ;
		
		
		 // Flow Cell Id
		valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("FCL300002639",1));
			add( new Pair<>("C4KC7ACXX",1) );
		}};
		checkVariableGroup( root, "Flow Cell Id", valuePair  ) ;
		
		 // Run Id
		valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("SRR3083868",1));
			add( new Pair<>("WR6H1",1) );
			add( new Pair<>("400",1) );
		}};
		checkVariableGroup( root, "Run Id", valuePair  ) ;
		
		 //  Flow Cell Lane
		valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("L1",1) );
			add( new Pair<>("3",2) );
			add( new Pair<>("4",1) );
		}};
		checkVariableGroup( root, "Flow Cell Lane", valuePair  ) ;
		
		 //  Tile Number
		valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("C017R084",1) );
			add( new Pair<>("2205",2) );
			add( new Pair<>("2104",1) );
		}};
		checkVariableGroup( root, "Tile Number", valuePair  ) ;
		
		 //  Index
		valuePair = new ArrayList<Pair<String, Integer>>() { { 
			add( new Pair<>("#CTTGTA",1) );
		}};
		checkVariableGroup( root, "Index", valuePair  ) ;			
		
	}
	
	private void checkVariableGroup(Element root, String name, List<Pair<String, Integer>> valuePair) { 
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP).stream().filter( k -> k.getAttribute(XmlUtils.NAME).equals(name)).findFirst().get();
		int total = 0;
		for(Pair p : valuePair) { 
			Element e = XmlElementUtils.getChildElementByTagName( ele, XmlUtils.TALLY ).stream().filter(  k -> k.getAttribute(XmlUtils.VALUE ).equals(p.getLeft())).findFirst().get();
			assertEquals( e.getAttribute(XmlUtils.COUNT), p.getRight() + "");
			total += (Integer)p.getRight();
		}
				
		assertEquals(ele.getAttribute(XmlUtils.COUNT),  total+"");		
	}
	
		
	@Test
 	public void patternTest() { 
		ReadIdSummary idSummary = new ReadIdSummary();			
 // 		NoColon("<element>"),
		RnPattern pa =  idSummary.getPattern( "V100007022_3_001R018843237".split(Constants.COLON_STRING));
		assertTrue(pa.equals(RnPattern.NoColon ));
		
 // 		NoColon_NCBI("<run id>.<pos>"), // pattern 1 : no colon, short name [S|E][0-9] { 6}.[0-9]+  eg.  SRR3083868.47411824 99 chr1 10015 9 100M = 10028 113 ...		      
		pa =  idSummary.getPattern( idSummary.splitElements("SRR3083868.47411824"));
		assertTrue(pa.equals(RnPattern.NoColon_NCBI  ));

 // 		NoColon_NUN("<pos>"), //pattern 2 : [no colon, NoColon name  0-9]+   eg. 322356 99 chr1 1115486 3 19M9S = 1115486 19
		pa =  idSummary.getPattern( "322356".split(Constants.COLON_STRING));
		assertTrue(pa.equals(RnPattern.NoColon_NUN  ));

 // 		NoColon_BGI("<flowcell ID><lane><tile><pos>"),  // <flowcell ID><lane><tile><pos>   eg. bgiseq500 : FCL300002639L1C017R084_416735  
		pa =  idSummary.getPattern( idSummary.splitElements("CL100004581L2C004R050_89689"));
		assertTrue(pa.equals(RnPattern.NoColon_BGI  ));

 // 		OneColon("<element1>:<element2>"),  // not sure
		pa =  idSummary.getPattern( idSummary.splitElements("FCL300002639L1:017R084_416735"));
		assertTrue(pa.equals(RnPattern.OneColon ));
		
 // 		TwoColon("<element1>:<element2>:<element3>"),   		
		pa =  idSummary.getPattern(idSummary.splitElements("V100007022L3C001::843237"));	
		assertTrue(pa.equals(RnPattern.TwoColon));	
		
 // 		TwoColon_Torrent("<Run_ID>:<x-pos>:<y-pos>"),  //pattern 4 : <Run> : <x-pos> : <y-pos>  eg. WR6H1:09838:13771 0ZT4V:02282:09455		
		pa =  idSummary.getPattern(idSummary.splitElements("WR6H1:09838:13771"));	
		assertTrue(pa.equals(RnPattern.TwoColon_Torrent));	
		
 // 		ThreeColon("<element1>:<element2>:<element3>:<element4>"),  // not sure
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-ST797_0059:3:2205:20826"));
		assertTrue(pa.equals(RnPattern.ThreeColon ));
		
 // 		FourColon("<element1>:<element2>:<element3>:<element4>:<element5>"), //  pattern 5 :  <instrument><_run id>:<lane>:<tile>:<x-pos>:<y-pos><#index>  eg. hiseq2000: HWI-ST797_0059:3:2205:20826:152489#CTTGTA		
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444x:1268y"));	
		assertTrue(pa.equals(RnPattern.FourColon));

 // 		FourColon_OlderIllumina("<instrument_id>:<lane>:<tile>:<x-pos>:<y-pos><#index></pair>"),
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-ST797_0059:3:2205:20826:152489#CTTGTA"));
		assertTrue(pa.equals(RnPattern.FourColon_OlderIllumina ));
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-ST797_0059:3:2205:20826:152489#0/1"));
		assertTrue(pa.equals(RnPattern.FourColon_OlderIllumina ));
				
 // 		FourColon_OlderIlluminaWithoutIndex("<instrument_id>:<lane>:<tile>:<x-pos>:<y-pos>"),
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444:1268"));	
		assertTrue(pa.equals(RnPattern.FourColon_OlderIlluminaWithoutIndex));
				
 // 		FiveColon("<element1>:<element2>:<element3>:<element4>:<element5>:<element6>"),  // not sure
		pa =  idSummary.getPattern(idSummary.splitElements("HWI-EAS282-R_100802:2:14:7444:1268:1"));	
		assertTrue(pa.equals(RnPattern.FiveColon));
				
 // 		SixColon("<element1>:<element2>:<element3>:<element4>:<element5>:<element6>:<element7>"),  // pattern 6 :  <instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos> eg. MG00HS15:400:C4KC7ACXX:4:2104:11896:63394
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056#CAXXX"));	
		assertTrue(pa.equals(RnPattern.SixColon));

 // 		SixColon_Illumina("<instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos>"),  // pattern 6 :  <instrument>:<run id>:<flowcell id> :<lane>:<tile>:<x-pos>:<y-pos> eg. MG00HS15:400:C4KC7ACXX:4:2104:11896:63394
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056"));	
		assertTrue(pa.equals(RnPattern.SixColon_Illumina));

 // 		SevenColon_andMore("<element1>:<element2>:...:<elementn>");
		pa =  idSummary.getPattern(idSummary.splitElements("UNC9-SN296:432:C5CAJACXX:6:2113:17454:48056:CAXXX"));	
		assertTrue(pa.equals(RnPattern.SevenColon_andMore));				
	}
	
	@Test
	public void poolTest() { 
 			
		ReadIdSummary idSummary = new ReadIdSummary();
		
		String name = "SRR3083868.";
		for(int i = 1  ; i <= 10; i ++ ) idSummary.parseReadId(name + i);			
		 // only 9 of first 10 into random pool, another is inside uniq pool
		assertTrue( idSummary.pool_random.size() == 9 );
	
		for(int i = 11; i <= 1000; i ++)  idSummary.parseReadId(name + i);		 
		 // uniq pool always one which is the first read since same pattern and column0
		assertTrue( idSummary.pool_uniq.size() == 1 );					
		assertTrue( idSummary.patterns.keySet().size() == 1 );			
 		assertTrue( idSummary.pool_random.size() == 19 );
		 
		name = "FCL300002639L1C017R084_";		
		for(int i = 1001; i <= 1000000; i ++) idSummary.parseReadId(name + i);
		 // second pattern appears in the first 2M reads			
		assertTrue( idSummary.pool_uniq.size() == 2 );
		 // because they are selected randomly, each time number are slightly different
		 // assertTrue( idSummary.pool_random.size() == 119 );
	
		name = "FCL300002639L2C017R084_";  // new lane
		for(int i = 1000001; i <= 2000000; i ++) idSummary.parseReadId(name + i);		
		name = "CL300002639L2C017R084_";   // new flowcell
		for(int i = 2000001; i <= 3000000; i ++) idSummary.parseReadId(name + i);		
		name = "CL300002639L2C017R084";	  // new tile but not count into uniq
		for(int i = 3000001; i <= 5000000; i ++) idSummary.parseReadId(name + i);		
		
		assertTrue( idSummary.patterns.keySet().size() == 2 );
		assertTrue( idSummary.patterns.get( RnPattern.NoColon_NCBI.toString() ).get() == 1000 );
		assertTrue( idSummary.patterns.get( RnPattern.NoColon_BGI.toString() ).get() == 4999000 );
			
		
		 // stop record runid to column[0] for NoColon_NCBI("<Run Id>.<Pos>")
		assertTrue( idSummary.runIds.get("SRR3083868").get() == 1000 );
				
		assertTrue( idSummary.flowCellIds.get("FCL300002639").get() == 1999000 );
		assertTrue( idSummary.flowCellIds.get("CL300002639").get() == 3000000 );
		
		assertTrue( idSummary.flowCellLanes.get("L1").get() == 999000 );
		assertTrue( idSummary.flowCellLanes.get("L2").get() == 4000000 );
		assertTrue( idSummary.tileNumbers.get("C017R").get() == 2000000 );		
		assertTrue( idSummary.tileNumbers.get("C017R084").get() == 2999000 );
		 // since we ignore tile for uniq check
		assertTrue( idSummary.pool_uniq.size() == 4 );
		assertTrue( idSummary.pool_random.size() <= ReadIdSummary.MAX_POOL_SIZE  );						
	}
	
	@Test
	public void bigTileNumberTest() { 
		ReadIdSummary idSummary = new ReadIdSummary();
		
		String prefix = "FCL300002639L2C017R";		 
		for(int i = 0; i < 200; i++) { 
			idSummary.parseReadId(prefix + i + "_" +i);
		}
		
		assertTrue( idSummary.patterns.keySet().size() == 1 );
		assertTrue( idSummary.patterns.get( RnPattern.NoColon_BGI.toString() ).get() == 200 );
		assertTrue( idSummary.tileNumbers.get("C017R99").get() == 1 );
		assertTrue( idSummary.tileNumbers.get(XmlUtils.OTHER).get() == 100 );
		assertTrue( idSummary.tileNumbers.size() == 101 );
	}
	
	@Test
	public void indexTest() { 
		 
		String name = "HWI-ST567_0239:1:7:20610:49360#0";
		
		ReadIdSummary idSummary = new ReadIdSummary();
		idSummary.parseReadId(name);
		idSummary.parseReadId(name+"c");
				
		assertTrue( idSummary.indexes.size() == 2 );
		assertTrue( idSummary.indexes.get("#0").get()== 1 );
		assertTrue( idSummary.indexes.get("#0c").get()== 1 );
		
		idSummary.parseReadId(name+"/1");
		assertTrue( idSummary.indexes.size() == 2 );
		assertTrue( idSummary.pairs.size() == 1);
		assertTrue( idSummary.pairs.get("/1").get()== 1 );	
	}

}
