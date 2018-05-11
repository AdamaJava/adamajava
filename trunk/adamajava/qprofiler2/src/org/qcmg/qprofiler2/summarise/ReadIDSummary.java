package org.qcmg.qprofiler2.summarise;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class ReadIDSummary {

	// Header info
	ConcurrentMap<String, AtomicLong> instruments = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> runIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellLanes = new ConcurrentHashMap<>();
//	ConcurrentMap<Integer, AtomicLong> tileNumbers = new ConcurrentHashMap<>();
	//hiseq tile number is integer but bgi is string
	ConcurrentMap<String, AtomicLong> tileNumbers = new ConcurrentHashMap<>();
	AtomicLong firstInPair = new AtomicLong();
	AtomicLong secondInPair = new AtomicLong();
	AtomicLong filteredY = new AtomicLong();
	AtomicLong filteredN = new AtomicLong();
	AtomicLong invalidId = new AtomicLong();
	ConcurrentMap<String, AtomicLong> indexes = new ConcurrentHashMap<>();	
	
	/**
	 * analysis readId, record instrument, runId, flowCellId, flowCellLanes, titleNumber etc
	 * eg.	//@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2
			//@ERR091788 - machine id
			//3104 - read position in file
			// HSQ955 - flowcell
			// 155 - run_id
			// 2 - flowcell lane
			// 1101 - tile
			// 13051 - x
			// 2071 - y
			// 2 - 2nd in pair
	 * @param readId
	 * @throws Exception 
	 */
	public void parseReadId(String readId) throws Exception{
		
		if ( !readId.contains(":")) { parseBgiReads(readId); return; };	
		
		String [] headerDetails = TabTokenizer.tokenize( readId, ':' );
		if (null == headerDetails ||  headerDetails.length <= 0)  return;
				
		//if length is equal to 10, we have the classic Casava 1.8 format
		int headerLength = headerDetails.length;					
		if (headerLength == 5) {
			if (readId.contains(" "))  
				parseFiveElementHeaderWithSpaces(headerDetails);
			else  
				parseFiveElementHeaderNoSpaces(headerDetails);
			
			return;					 
		} 
								
		// if (headerLength != 5)					
		updateMap(instruments, headerDetails[0]);
		
		// run Id
		if (headerLength > 1)  			 
			updateMap(runIds, headerDetails[1]);
		 	
		// flow cell id
		if (headerLength > 2) 	
			updateMap(flowCellIds, headerDetails[2] );
			
		// flow cell lanes
		if (headerLength > 3)  		 
			updateMap(flowCellLanes, headerDetails[3]);
		 	
		// tile numbers within flow cell lane
		if (headerLength > 4) {
			updateMap(tileNumbers, headerDetails[4]);
			
			
			
//			try {
//				String key = headerDetails[4];
//				Integer intKey = Integer.valueOf(key);
//				updateMap(tileNumbers, intKey);				
//			} catch (NumberFormatException nfe) {				 
//				throw new Exception("Can't convert string to integer: " + key, nfe);
//			}
		}		
		// skip x, y coords for now
		if (headerLength > 6)	 
			// this may contain member of pair information
			getPairInfo(headerDetails[6]);
			
		// filtered
		if (headerLength > 7) {
			String key = headerDetails[7];
			if ("Y".equals(key)) {
				filteredY.incrementAndGet();
			} else if ("N".equals(key)) {
				filteredN.incrementAndGet();
			}			
			// skip control bit for now	 // thats it!!
		}
		// indexes
		if (headerLength > 9)  updateMap(indexes, headerDetails[9]);		
				
	}

	
	/**
	 * flow cell id: CL100013884, lane: L2 tile number: C004R071
	 * @param readId: eg CL100013884L2C004R071_323304
	 * @throws Exception
	 */
	public void parseBgiReads(String readId) throws Exception{
				
		if(readId.length() < 23  || !readId.contains("_")) return;
		
		String tile = readId.substring(13, readId.indexOf("_"));
//		tile = tile.substring(1, tile.indexOf("R"));
		
		updateMap(flowCellIds,readId.substring(0, 11) ) ;
		updateMap(flowCellLanes,  readId.substring(11,13));
		updateMap( tileNumbers,  tile );
		//updateMap( tileNumbers, Integer.valueOf(tile) );
	}
	
		
	public void toXml(Element element){
		// header breakdown
		SummaryReportUtils.lengthMapToXml( element, "INSTRUMENTS",  instruments );
		SummaryReportUtils.lengthMapToXml( element, "RUN_IDS",  runIds );
		SummaryReportUtils.lengthMapToXml( element, "FLOW_CELL_IDS",  flowCellIds );
		SummaryReportUtils.lengthMapToXml( element, "FLOW_CELL_LANES", flowCellLanes );
		SummaryReportUtils.lengthMapToXml( element, "TILE_NUMBERS", tileNumbers );
		SummaryReportUtils.lengthMapToXml( element, "PAIR_INFO",  getPairs() );
		SummaryReportUtils.lengthMapToXml( element, "FILTER_INFO",  getFiltered() );
		SummaryReportUtils.lengthMapToXml( element, "INDEXES",  indexes );		
	}
		
	public ConcurrentMap<String, AtomicLong> getInstrumentsMap(){ return instruments;	}	
	public ConcurrentMap<String, AtomicLong> getRunIdsMap(){ return runIds; }	
	public ConcurrentMap<String, AtomicLong> getFlowCellIdsMap(){ return flowCellIds; }
	public ConcurrentMap<String, AtomicLong> getFlowCellLanesMap(){ return flowCellLanes; }
//	public ConcurrentMap<Integer, AtomicLong> getTileNumbersMap(){	return tileNumbers; }
	public ConcurrentMap<String, AtomicLong> getTileNumbersMap(){	return tileNumbers; }
	public ConcurrentMap<String, AtomicLong> getIndexesMap(){ return indexes; }
	
	public Map<String, AtomicLong> getPairs(){
		Map<String, AtomicLong> pairs = new HashMap<>();
		pairs.put("1", firstInPair);
		pairs.put("2", secondInPair);
		return pairs; 
	}

	public Map<String, AtomicLong> getFiltered(){
		Map<String, AtomicLong> filtered = new HashMap<>();
		filtered.put( "Y", filteredY );
		filtered.put( "N", filteredN );
		return filtered; 
	}	
	
//	public String[] getInstruments(){ return instruments.keySet().stream().toArray(String[]::new);	}	
//	public String[] getRunIds(){ return runIds.keySet().stream().toArray(String[]::new); }	
//	public String[] getFlowCellIds(){ return flowCellIds.keySet().stream().toArray(String[]::new); }
//	public String[] getFlowCellLanes(){ return flowCellLanes.keySet().stream().toArray(String[]::new); }
//	public Integer[] getTileNumbers(){	return tileNumbers.keySet().stream().toArray(Integer[]::new); }	
	
	/**
	 * @HWUSI-EAS100R:6:73:941:1973#0/1
	 * HWUSI-EAS100R	the unique instrument name
	* 6	flowcell lane
	* 73	tile number within the flowcell lane
	* 941	'x'-coordinate of the cluster within the tile
	* 1973	'y'-coordinate of the cluster within the tile
	* #0	index number for a multiplexed sample (0 for no indexing)
	* /1	the member of a pair, /1 or /2 (paired-end or mate-pair reads only)
	* 
	* 
	* OR
	* 
	* @HS2000-1107_220:6:1115:6793:38143/1
	* HS2000-1107 	the unique instrument name
	* 220	runId 
	* 6	flowcell lane
	* 1115	tile number within the flowcell lane
	* 6793	'x'-coordinate of the cluster within the tile
	* 38143	'y'-coordinate of the cluster within the tile
	* /1	the member of a pair, /1 or /2 (paired-end or mate-pair reads only)
	*
	 * @param params
	 * @throws Exception 
	 */
	void parseFiveElementHeaderNoSpaces(String [] params) throws Exception {
		
		/*
		 * If instrument name contains an underscore, split on this and the RHS becomes the run id!
		 * If no underscore, no run_id and the 
		 */
		int underscoreIndex = params[0].indexOf('_');
		if (underscoreIndex > -1) {
			updateMap(runIds,  params[0].substring(underscoreIndex + 1));
			updateMap(instruments, params[0].substring(0, underscoreIndex));
		} else {
			updateMap(instruments, params[0]);
		}
		
		updateMap(flowCellLanes, params[1]);
//		updateMap(tileNumbers, Integer.parseInt(params[2]));
		updateMap(tileNumbers,  params[2] );
		// skip x, and y coords for now..
		getPairInfo(params[4]);
	}

	private <T> void updateMap(ConcurrentMap<T, AtomicLong> map , T key) {
		AtomicLong al = map.get(key);
		if (null == al) {
			al = new AtomicLong();
			AtomicLong existing = map.putIfAbsent(key, al);
			if (null != existing) {
				al = existing;
			}
		}
		al.incrementAndGet();
	}	

	/**
	 * 	//@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2
							//@ERR091788 - machine id
							//3104 - read position in file
							// HSQ955 - flowcell
							// 155 - run_id
							// 2 - flowcell lane
							// 1101 - tile
							// 13051 - x
							// 2071 - y
							// 2 - 2nd in pair
	 * @throws Exception 
	 */
	void parseFiveElementHeaderWithSpaces(String [] params) throws Exception {
		// split by space
		String [] firstElementParams = params[0].split(" ");
		if (firstElementParams.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		String [] machineAndReadPosition = firstElementParams[0].split("\\.");
		if (machineAndReadPosition.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		
		updateMap(instruments, machineAndReadPosition[0]);
		
		String [] flowCellAndRunId = firstElementParams[1].split("_");
		if (flowCellAndRunId.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		
		updateMap(flowCellIds, flowCellAndRunId[0]);
		updateMap(runIds, flowCellAndRunId[1]);
		
		updateMap(flowCellLanes, params[1]);
		updateMap(tileNumbers,  params[2] );
		//updateMap(tileNumbers, Integer.parseInt(params[2]));
		// skip x, and y coords for now..
		getPairInfo(params[4]);
	}
	
	private void getPairInfo(String key) throws Exception {
		int index = key.indexOf(" ");
		if (index == -1) {
			index = key.indexOf("/");
		}
		if (index != -1) {
			char c = key.charAt(index + 1);
			if (c == '1') {
				firstInPair.incrementAndGet();
			} else if (c == '2') {
				secondInPair.incrementAndGet();
			} else {
				throw new Exception("unexpected value for member of pair: " + c + " from " + key);
			}
		}
	}
}
