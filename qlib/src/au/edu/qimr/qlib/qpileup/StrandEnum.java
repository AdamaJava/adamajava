/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

import java.util.ArrayList;
import java.util.List;

public enum StrandEnum{
	
		baseA("A"),
		baseC("C"),
		baseG("G"),
		baseT("T"),
		baseN("N"),
		qualA("Aqual"),
		qualC("Cqual"),
		qualG("Gqual"),
		qualT("Tqual"),
		qualN("Nqual"),
		mapQual("MapQual"),
		referenceNo("ReferenceNo"),	
		nonreferenceNo("NonreferenceNo"),	
		highNonreference("HighNonreference"),	
		lowRead("LowReadCount"),		
		startAll("StartAll"),	
		startNondup("StartNondup"),	
		stopAll("StopAll"),	
		dupCount("DupCount"),	
		mateUnmapped("MateUnmapped"),	
		cigarI("CigarI"),	
		cigarD("CigarD"),
		cigarDStart("CigarD_start"),
		cigarS("CigarS"),	
		cigarSStart("CigarS_start"),
		cigarH("CigarH"),
		cigarHStart("CigarH_start"),
		cigarN("CigarN"),
		cigarNStart("CigarN_start");
		final static String DELIMITER = PileupConstants.DELIMITER;
		
		private String type;		
		public static int LONG_INDEX_START = 5;
		public static int LONG_INDEX_END = 10;
		
		private StrandEnum(final String type){
		        this.type = type;
	    }

	    public String getStrandEnum(){
	        return type;
	    } 
	    
	    public static String getHeader(){
	    	StringBuffer sb = new StringBuffer();
	    	StrandEnum[] enums = StrandEnum.values();
	    	
	    	for (int i=0; i<enums.length; i++){
	    		sb.append(enums[i].getStrandEnum()).append("_for").append(DELIMITER);
	    	}
	    	for (int i=0; i<enums.length; i++){
	    		sb.append(enums[i].getStrandEnum()).append("_rev");
	    		if (i != enums.length -1){
	    			sb.append(DELIMITER);
	    		}
	    	}
	    	return sb.toString();
	    }

		public static String getHeader(List<StrandEnum> viewElements){
			StringBuffer sb = new StringBuffer();
	    	StrandEnum[] enums = StrandEnum.values();

	    	for (int i=0; i<enums.length; i++){
	    		if (viewElements.contains(enums[i])){
	    			sb.append(enums[i].getStrandEnum()).append("_for").append(DELIMITER);
	    		}
	    	}
	    	int count = 0;
	    	for (int i=0; i<enums.length; i++){
	    		if (viewElements.contains(enums[i])){
	    			sb.append(enums[i].getStrandEnum()).append("_rev");	
	    			if (count != viewElements.size() -1){
		    			sb.append(DELIMITER);
		    		}
	    			count++;
	    		}
	    		
	    	}
	    	return sb.toString();
		}
		

		public static String getHeader(List<StrandEnum> groupElements, 
				boolean getForwardElements, boolean getReverseElements){
			StringBuffer sb = new StringBuffer();
	    	StrandEnum[] enums = StrandEnum.values();

	    	if (getForwardElements){
		    	for (int i=0; i<enums.length; i++){
		    		if (groupElements.contains(enums[i])){
		    			sb.append(enums[i].getStrandEnum()).append("_for").append(DELIMITER);
		    		}
		    	}
	    	}
	    	int count = 0;
	    	if (getReverseElements){
		    	for (int i=0; i<enums.length; i++){
		    		if (groupElements.contains(enums[i])){
		    			sb.append(enums[i].getStrandEnum()).append("_rev");	
		    			if (count != groupElements.size()-1){
			    			sb.append(DELIMITER);
			    		}
		    			count++;
		    		}		    		
		    	}
	    	}
	    	return sb.toString();
		}
		
		public static List<StrandEnum> getBases(){
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.add(baseA);
			list.add(baseC);
			list.add(baseG);
			list.add(baseT);
			list.add(baseN);
			list.add(referenceNo);
			list.add(nonreferenceNo);
			list.add(highNonreference);
			list.add(lowRead);			
			return list;
		}
		
		public static List<StrandEnum> getBaseCounts(){
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.add(baseA);
			list.add(baseC);
			list.add(baseG);
			list.add(baseT);
			list.add(baseN);			
			return list;
		}
		
		public static List<StrandEnum> getCigars(){
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.add(cigarI);
			list.add(cigarD);
			list.add(cigarDStart);
			list.add(cigarS);
			list.add(cigarSStart);
			list.add(cigarH);
			list.add(cigarHStart);
			list.add(cigarN);
			list.add(cigarNStart);
			return list;
		}
		
		public static List<StrandEnum> getQuals(){
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.add(qualA);
			list.add(qualC);
			list.add(qualG);
			list.add(qualT);
			list.add(qualN);
			list.add(mapQual);
			return list;
		}
		
		public static List<StrandEnum> getReadStats(){
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.add(startAll);
			list.add(startNondup);
			list.add(stopAll);
			list.add(dupCount);
			list.add(mateUnmapped);			
			return list;
		}

		public static List<StrandEnum> getMetrics() {
			List<StrandEnum> list = new ArrayList<StrandEnum>();
			list.addAll(getBases());	
			list.add(cigarI);
			list.add(cigarD);
			list.add(cigarDStart);
			list.add(cigarS);
			list.add(cigarSStart);
			list.add(cigarH);
			list.add(cigarHStart);
			list.add(mapQual);
			return list;
		}

		public static StrandEnum[] getMetricsElements() {
			List<StrandEnum> strandEnumList = new ArrayList<StrandEnum>();
			
			//total bases
			strandEnumList.add(StrandEnum.baseA);
			strandEnumList.add(StrandEnum.baseC);
			strandEnumList.add(StrandEnum.baseT);
			strandEnumList.add(StrandEnum.baseG);
			strandEnumList.add(StrandEnum.baseN);		
			strandEnumList.add(StrandEnum.lowRead);
			strandEnumList.add(StrandEnum.highNonreference);
			strandEnumList.add(StrandEnum.nonreferenceNo);
			strandEnumList.add(StrandEnum.referenceNo);
			strandEnumList.add(StrandEnum.qualA);
			strandEnumList.add(StrandEnum.qualC);
			strandEnumList.add(StrandEnum.qualT);
			strandEnumList.add(StrandEnum.qualG);
			strandEnumList.add(StrandEnum.qualN);		
			strandEnumList.add(StrandEnum.cigarI);
			strandEnumList.add(StrandEnum.cigarD);
			strandEnumList.add(StrandEnum.cigarDStart);		
			strandEnumList.add(StrandEnum.mapQual);
			strandEnumList.add(StrandEnum.cigarS);
			strandEnumList.add(StrandEnum.cigarH);
			strandEnumList.add(StrandEnum.cigarSStart);
			strandEnumList.add(StrandEnum.cigarHStart);
			strandEnumList.add(StrandEnum.mateUnmapped);	
			
			StrandEnum[] strandElements = new StrandEnum[strandEnumList.size()];
			for (int i=0; i<strandEnumList.size(); i++) {
				strandElements[i] = strandEnumList.get(i);
			}	
			return strandElements;
	}

}
