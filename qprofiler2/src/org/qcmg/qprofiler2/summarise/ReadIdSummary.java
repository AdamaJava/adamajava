package org.qcmg.qprofiler2.summarise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class ReadIdSummary {
static final int MAX_POOL_SIZE = 500;
	static final int TALLY_SIZE = MAX_POOL_SIZE / 5;
	
	// pattern predict
	public enum  RnPattern {
		NoColon("<Element>"),  // not sure
		NoColon_NCBI("<Run Id>.<Pos>"), // pattern 1 : no colon, short name [S|E][0-9] {6}.[0-9]+  eg.  SRR3083868.47411824 99 chr1 10015 9 100M = 10028 113 ...
		NoColon_NUN("<Pos>"), //pattern 2 : [no colon, NoColon name  0-9]+   eg. 322356 99 chr1 1115486 3 19M9S = 1115486 19
		NoColon_BGI("<Flow Cell Id><Flow Cell Lane><Tile Number><Pos>"),  // pattern 3: eg. bgiseq500 : FCL300002639L1C017R084_416735  
		OneColon("<Element1>:<Element2>"),  // not sure
		TwoColon("<Element1>:<Element2>:<Element3>"),  //not sure
		TwoColon_Torrent("<Run Id>:<X Pos>:<Y Pos>"),  //pattern 4 :  eg. WR6H1:09838:13771 0ZT4V:02282:09455
		ThreeColon("<Element1>:<Element2>:<Element3>:<Element4>"),  // not sure
		FourColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>"),
		FourColon_OlderIllumina("<Instrument>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos><#Index></Pair>"), // eg. hiseq2000: HWI-ST797_0059:3:2205:20826:152489#CTTGTA		
		FourColon_OlderIlluminaWithoutIndex("<Instrument>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos>"), // eg. hiseq2000: HWI-ST797_0059:3:2205:20826:152489
		FiveColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>:<Element6>"),  // not sure
		SixColon("<Element1>:<Element2>:<Element3>:<Element4>:<Element5>:<Element6>:<Element7>"),  // not sure
		SixColon_Illumina("<Instrument>:<Run Id>:<Flow Cell Id>:<Flow Cell Lane>:<Tile Number>:<X Pos>:<Y Pos>"),  // pattern 6 :  eg. MG00HS15:400:C4KC7ACXX:4:2104:11896:63394
		SevenColon_andMore("<Element1>:<Element2>:...:<Elementn>");  //not sure
				
		final String pattern ; 
		
		RnPattern(String str) {
			this.pattern = str; 
		}		
		
		public String toString() {
			return pattern; 
		}
	}
		
	// the max queue size is 100k refer to BamSummarizerMT2, so 500 qname is trival 
	List<String> poolRandom =  Collections.synchronizedList(new ArrayList<String>());  // collect random 500	
	List<String> poolUniq =  Collections.synchronizedList(new ArrayList<String>());  // QNAME with different Element value	

	// overall qname information
	ConcurrentMap<String, AtomicLong> patterns = new ConcurrentHashMap<>();	
	@SuppressWarnings("unchecked")
	ConcurrentMap<String, AtomicLong>[] columns = new ConcurrentMap[7];
	 
	{
		 for (int i = 0; i < 7; i++) {
			 columns[i] = new ConcurrentHashMap<>(); 
		 }
	}
	
	ConcurrentMap<String, AtomicLong> instruments = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> runIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellLanes = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> tileNumbers = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> pairs = new ConcurrentHashMap<>();	
	ConcurrentMap<String, AtomicLong> indexes = new ConcurrentHashMap<>();	
	private AtomicLong inputNo  = new AtomicLong();
	private Random rd = new Random();

	/**
	 * recognize known pattern, re-arrange split string
	 * 
	 * @param parts split string of read name
	 * @return RNPattern for that read name
	 */
	public   RnPattern getPattern(String[] parts) {
		switch (parts.length) {
						    
			case 1:
				if (StringUtils.isNumeric(parts[0])) {	
					return  RnPattern.NoColon_NUN;			
				}				
			case 0:
			    return RnPattern.NoColon;
			case 2:
				if (parts[1].startsWith(".")) {
					return RnPattern.NoColon_NCBI;
				}
				return RnPattern.OneColon;
			case 3:					
				if (StringUtils.isNumeric(parts[1]) && StringUtils.isNumeric(parts[2])) {	
					return RnPattern.TwoColon_Torrent;
				} else if ((parts[1] != null && parts[1].startsWith("L"))
						&& (parts[2] != null && parts[2].startsWith("C"))) {
					return  RnPattern.NoColon_BGI;
				}
				
				return RnPattern.TwoColon;
			case 4:
				return RnPattern.ThreeColon;     
			case 5:
				// <InstrumentS>:<lane>:<Tile Number>:<X Pos>:<Y Pos>
				if (StringUtils.isNumeric(parts[3]) && StringUtils.isNumeric(parts[4])) {
					return RnPattern.FourColon_OlderIlluminaWithoutIndex;
				}
				return RnPattern.FourColon;		
			case 6:			
				return RnPattern.FiveColon;  
			case 7:
				for (int i = 0; i < 5; i++) {
					 if (StringUtils.isNullOrEmpty(parts[i])) {
						 return RnPattern.SixColon;
					 }
				 }
				
				if (StringUtils.isNumeric(parts[5]) && StringUtils.isNumeric(parts[6])) {
					return RnPattern.SixColon_Illumina; 
				} 
				
				// last two Elements are not number, allow null
				boolean withIndex = (parts[5] == null || parts[5].startsWith("#"));
				boolean withPair = (parts[6] == null ||  parts[5].startsWith("/"));			
				if (withIndex || withPair) {
					if (StringUtils.isNumeric(parts[3]) && StringUtils.isNumeric(parts[4])) {
						if (parts[5] == null) {
							return RnPattern.FourColon_OlderIlluminaWithoutIndex;
						} else {
							return RnPattern.FourColon_OlderIllumina;
						}
					}
				}			
				// unknown pattern with 7 elements
				return RnPattern.SixColon; 
			  default:
					return RnPattern.SevenColon_andMore; 
		}	
	}	

    String[] splitElements(String readId) {
		String[] parts = readId.split(Constants.COLON_STRING);
		List<String> elements = new ArrayList<>();
				
		int pos = -1;				
		if (parts.length == 1) {
			// check NCBI 
			pos = parts[0].indexOf(".");	
			if (pos > 0) {
				elements.add(parts[0].substring(0, pos));
				elements.add(parts[0].substring(pos));		 
			} else if (StringUtils.isNumeric(parts[0])) {
				// NoColon_NUN("<Pos>")
				elements.add(parts[0]);
			} else if (!StringUtils.isNumeric(parts[0].substring(0,1))) {
				// if first char is not number then check BGI
				// BGI L?C must appear after 5th
				int  currentL = 5;
				// String str = parts[0].substring(currentL);
				while ((pos = parts[0].substring(currentL).indexOf("L")) != -1) {
					currentL += pos;
					if (parts[0].substring(currentL + 2).startsWith("C")) {
						pos = parts[0].substring(currentL + 2).indexOf("_");
						String tile = pos > 0 ? parts[0].substring(currentL + 2,currentL + 2 + pos) : null;
						if (pos < 0) {
							pos = parts[0].substring(currentL + 2).indexOf("R");
							tile = pos > 0 ? parts[0].substring(currentL + 2,currentL + 2 + pos + 1) : null;
						}
						if (tile != null) {
							elements.add(parts[0].substring(0, currentL));
							elements.add(parts[0].substring(currentL, currentL + 2));						
							elements.add(tile);
						}
						break;
					}		
				}				
			}
			
		} else if (parts.length == 5) {
			// check index and pair for five element name pattern
			String pair = null;
			String index = null;
			String yPos = parts[4]; 
		    pos = parts[4].lastIndexOf("/");
			if (pos > 0) {
				pair = parts[4].substring(pos); // put pair to 6th temporary
				yPos = parts[4].substring(0, pos);
			}
			pos = yPos.lastIndexOf("#");				
			if (pos > 0) {
				index = yPos.substring(pos); // put index	
				yPos = yPos.substring(0, pos);	 // then change yPos value
			}
			
			for (int i = 0; i < 4; i ++) {
				elements.add(parts[i]); 
			} 
			
			elements.add(yPos);
			// return five elements or 7 elements
			if (index != null || pair != null) {
				elements.add(index);
				elements.add(pair);		
			}	
		} else {
			for (int i = 0; i < parts.length; i ++) {
				elements.add(parts[i]);						
			}	
		}	
		
		return elements.toArray(new String[elements.size()]); 			
	}
	
	public void parseReadId(String id) {
		inputNo.incrementAndGet();
		String readId = id.trim(); 
		
		// read id line may contain extra informtion, such as below from fastq file
		// NB551151:83:HWC2VBGX9:4:13602:8142:7462 1:N:0:GGGGGG
		// we have to remove all string after space	
		try {
			StringTokenizer st = new StringTokenizer(readId," \n\r\t");	
			// only get first token as readId
			readId = st.nextToken();
		} catch (NoSuchElementException e) {
			// do nothing, just pass to next step. 
		}
		
		
		String[] elements = splitElements(readId);  
		RnPattern pattern =  getPattern(elements);	
							
		boolean isUpdated = false; 
			 
		switch (pattern) {
			case NoColon : // do nothing			
			case NoColon_NUN: // do nothing
				break;
			// don't output last two column(mostly position), it may cause too many value 
			case SevenColon_andMore:	
			case SixColon:  // record element 0~4	 
				XmlUtils.updateMapWithLimit(columns[4], elements[4],TALLY_SIZE);							
			case FiveColon:	
				XmlUtils.updateMapWithLimit(columns[3], elements[3],TALLY_SIZE);	
			case FourColon:  // record element 0~2
				XmlUtils.updateMapWithLimit(columns[2], elements[2],TALLY_SIZE);	
			case ThreeColon:
				XmlUtils.updateMapWithLimit(columns[1], elements[1],TALLY_SIZE);									
		    case TwoColon:  // record element 0			    	
		    case OneColon:  // record element 0	
		    	XmlUtils.updateMapWithLimit(columns[0], elements[0],TALLY_SIZE);
		    	break;
			case NoColon_NCBI:  // eg.  SRR3083868.47411824
				// XmlUtils.updateMapWithLimit(columns[0], elements[0],TALLY_SIZE);	
				XmlUtils.updateMapWithLimit(runIds, elements[0],TALLY_SIZE);	
				break;
							
			case NoColon_BGI:
				// "<Flow Cell Id><lane><tile><pos>"
				XmlUtils.updateMapWithLimit(tileNumbers,  elements[2] ,TALLY_SIZE);		  // too many tile number, so skip it for checking whether uniq			
				isUpdated = XmlUtils.updateMapWithLimit(flowCellIds, elements[0],TALLY_SIZE);	
				// updatedMap must before "|| isUpdated" if isUpdated == true, flowCellLanes will not be updated. 
				isUpdated = XmlUtils.updateMapWithLimit(flowCellLanes,  elements[1],TALLY_SIZE) || isUpdated;	
				break;
				
		   case  TwoColon_Torrent:
				// TwoColon_Torrent("<Run Id>:<X Pos>:<Y Pos>"),  //pattern 4 : <Run> : <X Pos> : <Y Pos>  eg. WR6H1:09838:13771 0ZT4V:02282:09455
				isUpdated = XmlUtils.updateMapWithLimit(runIds, elements[0],TALLY_SIZE);	
				break;
				
		   case FourColon_OlderIllumina:
				// FourColon_OlderIllumina("<InstrumentS>:<lane>:<tile>:<X Pos>:<Y Pos><#index></pair>"),
				XmlUtils.updateMapWithLimit(indexes, elements[5],TALLY_SIZE);		
				if (elements[6] != null) {
					XmlUtils.updateMapWithLimit(pairs, elements[6],TALLY_SIZE);	
				} 
				// no break here, below code work for both FourColon_OlderIllumina and FourColon_OlderIlluminaWithoutIndex
		   case FourColon_OlderIlluminaWithoutIndex:
				// FourColon_OlderIlluminaWithoutIndex("<InstrumentS>:<lane>:<tile>:<X Pos>:<Y Pos>"),
				XmlUtils.updateMapWithLimit(tileNumbers,  elements[2],TALLY_SIZE);		  // too many tile number, so skip it for checking whether uniq	
				
				// both instruments and lanes have to be update first, and then judge either of them updated
				boolean k1 = XmlUtils.updateMapWithLimit(instruments, elements[0],TALLY_SIZE);
				boolean k2 = XmlUtils.updateMapWithLimit(flowCellLanes,  elements[1] ,TALLY_SIZE);				
				isUpdated = k1 || k2;		
				break;
		    
		   case SixColon_Illumina:   //code block				
			   // "<instrument>:<run id>:<Flow Cell Id> :<lane>:<tile>:<X Pos>:<Y Pos>"
				XmlUtils.updateMapWithLimit(tileNumbers, elements[4],TALLY_SIZE);
				isUpdated = XmlUtils.updateMapWithLimit(instruments,  elements[0],TALLY_SIZE);	
				isUpdated = XmlUtils.updateMapWithLimit(runIds, elements[1],TALLY_SIZE) || isUpdated;	;
				isUpdated = XmlUtils.updateMapWithLimit(flowCellIds, elements[2],TALLY_SIZE) || isUpdated;	;
				isUpdated = XmlUtils.updateMapWithLimit(flowCellLanes, elements[3],TALLY_SIZE) || isUpdated;	;	  
				break;	   		   

		   default:
		}
		
		// record this qName
		isUpdated = XmlUtils.updateMapWithLimit(patterns, pattern.toString() ,TALLY_SIZE) || isUpdated;
		select2Queue(readId,   isUpdated);	 
	}
	
	/**
	 * selectively record input QNAME into pool
	 * 
	 * @param readId is the BAM or fastq read id
	 * @param isUpdated should set to true if the readId is first time appear
	 */
	private void select2Queue(String readId, boolean isUpdated) {
				
		// record the first Qname for iron
		if (isUpdated  &&  poolUniq.size() < MAX_POOL_SIZE / 2) {	
			poolUniq.add(readId);	
			return;
		} 	
		
		if (inputNo.get() <= 10) {
			// record first 10 QNAME
			poolRandom.add(readId);		
		} else if (inputNo.get() <= 1000) {
			// 1% of first 1000 QNAME, that is max of 10
			if (inputNo.get() % 100 == 11) {
				poolRandom.add(readId);
			}
		} else if (poolRandom.size() < MAX_POOL_SIZE) {	
			// 1/10000 of first 5M, that is max of 500
			if ((inputNo.get() % 1000 == 999) && (rd.nextInt(10) == 1)) {
				poolRandom.add(readId);			
			}
		} else  if ((inputNo.get() % 100000 == 99999) && (rd.nextInt(10) > 5)) {
			// after 5Mth random (5/M)replace to above pool with 	
			int pos = rd.nextInt(poolRandom.size() - 1);
			poolRandom.set(pos, readId);					
		}

	}
	
	public long getInputReadNumber() {
		return inputNo.get();
	}
	
	
	/**
	 * output information to xml element.
	 * 
	 * @param ele is the start element to store the children branch information
	 */
		
	public void toXml(Element ele) {
		
		Element element = XmlUtils.createMetricsNode(ele, "qnameInfo", new Pair<String, Number>(ReadGroupSummary.READ_COUNT, getInputReadNumber()));
		XmlUtils.outputTallyGroup(element,  "QNAME Format", patterns , false , true);
		for (int i = 0; i < columns.length; i ++) {
			if (columns[i].size() > 0) {
				XmlUtils.outputTallyGroup(element,  "Element" + (i + 1), columns[i] , false, true);
			}
 		}
				
		XmlUtils.outputTallyGroup(element,  "Instrument", instruments , false , true);
		XmlUtils.outputTallyGroup(element,  "Flow Cell Id", flowCellIds , false , true);
		XmlUtils.outputTallyGroup(element,  "Run Id", runIds , false , true);
		XmlUtils.outputTallyGroup(element,  "Flow Cell Lane", flowCellLanes , false , true);
		XmlUtils.outputTallyGroupWithSize(element,  "Tile Number", tileNumbers, TALLY_SIZE, true);
		XmlUtils.outputTallyGroupWithSize(element,  "Pair infomation", pairs, TALLY_SIZE, true);
		XmlUtils.outputTallyGroupWithSize(element,  "Index", indexes , TALLY_SIZE, true);
		// merge two pool together
		poolRandom.addAll(poolUniq);
		
		// output 20 qname randomly
		element = XmlUtils.createMetricsNode(ele, "qnameExample", null);
		int size = Math.min(20,poolRandom.size());
		element.appendChild(ele.getOwnerDocument().createComment(size + " QNAMEs are listed here, which are picked up randomly"));
		for (int i = 0; i < size ; i ++) {
			// in case there is only one element in the pool (unit test)
			int pos = poolRandom.size() > 1 ? rd.nextInt(poolRandom.size() - 1) : 0 ;
			XmlUtils.outputValueNode(element, poolRandom.get(pos), 1);
		}	
	}
}
