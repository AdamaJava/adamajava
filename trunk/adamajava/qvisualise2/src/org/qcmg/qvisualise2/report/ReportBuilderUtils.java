package org.qcmg.qvisualise2.report;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map.Entry;

import org.qcmg.common.messages.QMessage;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.util.QProfilerCollectionsUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class ReportBuilderUtils {
	private static final QMessage messages = new QMessage( ReportBuilderUtils.class, ResourceBundle.getBundle("org.qcmg.qvisualise2.messages"));
	
	/**
	 * 
	 * @param summaryElement: xml element 
	 * @return a map with value and style for bam summary table
	 */
	public static Map<String, String>  createBamRgMap( Element summaryElement ) {
		
 		Map<String, String> duplicateMap = new LinkedHashMap<>();
		Map<String, String> maxLengthMap = new LinkedHashMap<>();
		Map<String, String> aveLengthMap = new LinkedHashMap<>();
		Map<String, String> totalReadsMap = new LinkedHashMap<>();
		Map<String, String> unmappedMap = new LinkedHashMap<>(); 	
		Map<String, String> nonCanonicalMap = new LinkedHashMap<>(); 	
		Map<String, String> isizeMap = new LinkedHashMap<>();
		Map<String, String> hardClipMap = new LinkedHashMap<>();
		Map<String, String> softClipMap = new LinkedHashMap<>();
		Map<String, String> overlapMap = new LinkedHashMap<>();
		Map<String, String> lostMap = new LinkedHashMap<>();
		Map<String, String> trimmedMap = new LinkedHashMap<>();
		
		//isize
 		List<Element> isizeNodes =   QprofilerXmlUtils.getChildElementByTagName( QprofilerXmlUtils.getChildElement(summaryElement, "readPairs" ,0 ),  "readGroup" ); 		 
		for (int i = 0 ; i < isizeNodes.size() ; i++){				 				 
			Element element =   isizeNodes.get(i);
			String rg = element.getAttributes().getNamedItem("id").getNodeValue();
			
			String modal = QprofilerXmlUtils.getChildElement ( element, "tlen", 0).getAttributes().getNamedItem("modalSize").getNodeValue();
			
			isizeMap.put( rg, modal);			   
		}	 
		
		//reads information
		NodeList readsChildren = ( QprofilerXmlUtils.getChildElement( summaryElement, "reads" ,0) ).getElementsByTagName("readGroup");  
		
		int rgNum = (null != readsChildren)?  readsChildren.getLength() : 0; 		
		for (int i = 0 ; i < rgNum ; i++){  			
			String rg = readsChildren.item(i).getAttributes().getNamedItem("id").getNodeValue();
			//a NodeList of all descendant Elements 
			NodeList rgNodes =  ((Element) readsChildren.item(i)).getElementsByTagName("*"); 
			
			//NodeList rgNodes = rgNodes.item(i).getChildNodes(); 
			for(int j = 0; j < rgNodes.getLength(); j ++){
				String nodeName =  rgNodes.item(j).getNodeName();
				NamedNodeMap nodeMap = rgNodes.item(j).getAttributes();
				String percentage  = (nodeMap.getNamedItem("basePercent") != null )?  nodeMap.getNamedItem("basePercent").getNodeValue() : null;
				switch (nodeName) {
					case "duplicateReads":  duplicateMap.put(rg, percentage); break;
					case "unmappedReads" : unmappedMap.put(rg,percentage); break; 
					case "nonCanonicalPair" : nonCanonicalMap.put(rg, percentage); ; break; 					
					case "softClippedBases" : softClipMap.put(rg, percentage); break; 
					case "hardClippedBases" : hardClipMap.put(rg, percentage); break; 
					case "overlapBases" : overlapMap.put(rg, percentage) ; break; 
					case "trimmedBase" : trimmedMap.put(rg, percentage) ; break; 						
					case "overall" : {
						maxLengthMap.put(rg, nodeMap.getNamedItem("maxLength").getNodeValue());				
						aveLengthMap.put(rg, nodeMap.getNamedItem("averageLength").getNodeValue());
						String value = String.format("%,.0f", Double.parseDouble( nodeMap.getNamedItem("readCount").getNodeValue().trim()));
						totalReadsMap.put(rg, value);
						lostMap.put(rg, nodeMap.getNamedItem("lostBasesPercent").getNodeValue());							
					}; break; 													
				}					
			}	
		 }
				
		Map<String, String> summaryMap = new LinkedHashMap<>();
		final String startVBlock = "{v: '";
        final String endVBlock = "', p: {style: 'text-align: right'}}" ;
        //final String finalVBlock = "]}";   
		String overallEle = null; 
		for (  String rg : totalReadsMap.keySet()) {
			String lostColor = endVBlock; 
			try{ 
				float lost =  Float.valueOf(lostMap.get(rg).replace("%", "").trim());
				String color = (lost > 40)? "tomato":"yellow"; 
				color = (lost < 20 ) ? "palegreen" : color; 
				lostColor = "', p: {style: 'text-align: right; background-color:" + color +";'}}" ;
			}catch(NumberFormatException e){ }	//do nothing
			
			StringBuilder ele  = new StringBuilder(startVBlock).append(totalReadsMap.get(rg)).append(endVBlock)  
                    .append(",").append(startVBlock).append(aveLengthMap.get(rg)).append(endVBlock )
                    .append(",").append(startVBlock).append(maxLengthMap.get(rg)).append(endVBlock )
                    .append(",").append(startVBlock).append((isizeMap.get(rg) == null ? "-" : isizeMap.get(rg) )).append(endVBlock)                                         
                    .append(",").append(startVBlock).append( unmappedMap.get(rg)).append( endVBlock  )
                    .append(",").append(startVBlock).append(nonCanonicalMap.get(rg)).append(endVBlock) 
                    .append(",").append(startVBlock).append(duplicateMap.get(rg)).append(endVBlock)  
                    .append(",").append(startVBlock).append(overlapMap.get(rg)).append( endVBlock  )
                    .append(",").append(startVBlock).append(softClipMap.get(rg)).append(endVBlock  )
                    .append(",").append(startVBlock).append(hardClipMap.get(rg)).append( endVBlock) 
                    .append(",").append(startVBlock).append(trimmedMap.get(rg)).append(endVBlock)
                    .append(",").append(startVBlock).append(lostMap.get(rg)).append(lostColor);			
						
			if( !rg.equals("overall") ) summaryMap.put( rg, ele.toString() );     
			else overallEle = ele.toString();         
		}
		summaryMap.put( "overall", overallEle );
		return summaryMap;
	}
	
	public static String[] createBamSummaryColumnName(String name){
				
		//add header line
		String[] column = new String[] { "Read Group", 
        				"Read Count",
        				"Average<BR>Read<BR>Length",
        				"Max<BR>Read<BR>Length",
        				"Mode<BR>TLEN",
        				"Unmapped<BR>Reads",
				"Non-canonical<BR>ReadPair",
				"Duplicate<BR>Reads",
				"Within<BR>ReadPair<BR>Overlap",
				"Soft<BR>Clipping<BR>(CIGAR)",
				"Hard<BR>Clipping<BR>(CIGAR)",
				"Adaptor<BR>Trimming",
				"Total<BR>Bases<BR>Lost"
		};
		 
		String[] description = new String[] {"", 
				messages.getMessage("COLUMN_readCount_DESCRIPTION"), 
				messages.getMessage("COLUMN_averageReadLength_DESCRIPTION"),
				messages.getMessage("COLUMN_maxReadLength_DESCRIPTION"),
				messages.getMessage("COLUMN_modeTlen_DESCRIPTION"),
				"",  //no description for unmapped reads
				messages.getMessage("COLUMN_NoncanonicalReadPair_DESCRIPTION"),
				messages.getMessage("COLUMN_DuplicateReads_DESCRIPTION"),
				messages.getMessage("COLUMN_WithinReadPairOverlap_DESCRIPTION"),
				messages.getMessage("COLUMN_SoftClipping_DESCRIPTION"),
				messages.getMessage("COLUMN_HardClipping_DESCRIPTION"),
				messages.getMessage("COLUMN_AdaptorTrimming_DESCRIPTION"),
				""	//no description for total lost
		};
		 		
		String[] arr = new String[column.length + 1];
		arr[0] = name; 
		
		for(int i = 0; i < column.length; i ++)	 			
				arr[i+1] = String.format("<span title=\"%s\" >%s</span>", description[i], column[i]);
				
		return arr;

	}
		
	public static Map<String, Map<String, AtomicLong>> createFastqSummaryMap(Element reportElement){
		
		Map<String, Map<String, AtomicLong>> summaryMap = new LinkedHashMap<>();		
		// instruments first
		addEntryToSummaryMap( reportElement, "INSTRUMENTS", "Instrument", summaryMap );
		addEntryToSummaryMap( reportElement, "RUN_IDS", "Run Id", summaryMap );
		addEntryToSummaryMap( reportElement, "FLOW_CELL_IDS", "Flow Cell Id", summaryMap );
		addEntryToSummaryMap( reportElement, "FLOW_CELL_LANES", "Flow Cell Lane", summaryMap );
		addEntryToSummaryMap( reportElement, "PAIR_INFO", "Pair", summaryMap );
		addEntryToSummaryMap( reportElement, "FILTER_INFO", "Filter", summaryMap );
		addEntryToSummaryMap( reportElement, "TILE_NUMBERS", "Tile Number", summaryMap );
		
		return summaryMap;
	}
	
	/**
	 * 
	 * @param reportElement: eg <ReadNameAnalysis>
	 * @param elementName: eg. <INSTRUMENTS>
	 * @param mapEntryName: eg. "Instrument"
	 * @param summaryMap
	 */
	private static void addEntryToSummaryMap( Element reportElement, String elementName, String mapEntryName, Map<String, Map<String, AtomicLong>> summaryMap ) {		
		 
		final Element element = QprofilerXmlUtils.getChildElement( reportElement, elementName, 0 ); // (Element) nodeList.item(0);
		if (null == element) {
			System.out.println("null " + elementName  + " element");
			return; 
		}
		 
		Map<String, AtomicLong> sourceMap = new HashMap<>();
		QProfilerCollectionsUtils.populateTallyItemMap(element, sourceMap, false, null);
		
		for (Entry<String, AtomicLong> entry : sourceMap.entrySet()) {
			// get map from summaryMap
			Map<String, AtomicLong> map = summaryMap.get(mapEntryName);
			if (null == map) {
				map = new HashMap<String, AtomicLong>();
				summaryMap.put(mapEntryName, map);
			}
			map.put(entry.getKey(), entry.getValue());
		}
				 
	}	

	
	//<TAG><ReadNameAnalysis><ReadGroup id="20150125163736341"><INSTRUMENTS>
	/**
	 * 
	 * @param summaryElement: eg. <ReadNameAnalysis>
	 * @return
	 */
	public static Map<String, String>  createReadNameSummaryMap( Element summaryElement ){
		
		//column name: 	"Read Group"  "INSTRUMENTS",   "RUN_IDS",   "FLOW_CELL_IDS",   "FLOW_CELL_LANES",  "TILE_NUMBERS"
		String[] tagNames = new String[]{"INSTRUMENTS", "RUN_IDS", "FLOW_CELL_IDS", "FLOW_CELL_LANES" }; //, "TILE_NUMBERS"};
		Map<String, String> summaryMap = new LinkedHashMap<>();	
		for(Element rgE : QprofilerXmlUtils.getChildElementByTagName(summaryElement,"ReadGroup")){
			StringBuilder sb = new StringBuilder();
			for(String tag :  tagNames){
				if(sb.length() > 0 ) sb.append(", "); //next column value				
				sb.append("'"); // start new column value
				Element ele = QprofilerXmlUtils.getChildElement(QprofilerXmlUtils.getChildElement(rgE, tag, 0), QprofilerXmlUtils.valueTally, 0);
				int k = 0;
				for(Element  itemElement :  QprofilerXmlUtils.getChildElementByTagName(ele, QprofilerXmlUtils.tallyItem) ){
					if((k++) > 0) sb.append(", ");
					sb.append(itemElement.getAttribute("value"));
				}
				sb.append("'");
			}
			//"TILE_NUMBERS"
			Element ele = QprofilerXmlUtils.getChildElement(QprofilerXmlUtils.getChildElement(rgE, "TILE_NUMBERS", 0), QprofilerXmlUtils.valueTally, 0);
			int no = QprofilerXmlUtils.getChildElementByTagName(ele, QprofilerXmlUtils.tallyItem).size() ;
			
			sb.append(", '").append(no).append("'"); // end new column value		
			summaryMap.put(rgE.getAttribute("id"), sb.toString());			
		}
		
		return summaryMap; 		
	}




}
