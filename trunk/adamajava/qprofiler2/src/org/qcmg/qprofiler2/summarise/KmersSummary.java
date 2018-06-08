package org.qcmg.qprofiler2.summarise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.w3c.dom.Element;

public class KmersSummary {
		
	private int cycleNo = 0 ; //init 0, eventially store the biggest cycle number
	
	//unpaired, firstOfPair, secondOfPair
	private final QCMGAtomicLongArray[] tally = new QCMGAtomicLongArray[3] ;
	private final AtomicLong[] parsedCount = new AtomicLong[3]; 
	
	private static final int iniCycleNo = 126;	
	public static final int maxKmers = 6; 
	public static final String atgc = "ATGC";  //make sure letter on same order
	public static final String atgcn = "ATGCN";  //make sure letter on same order
	public static final char[] atgcCharArray = new char[]{'A','T','G','C'};
	public static final char[] atgcnCharArray = new char[]{'A','T','G','C','N'};
	
	private final int merLength; 
	private final int[] mersIndex; 
	private final String[] mersStrList; 
	
	public KmersSummary( int length ) {	
		if(length > maxKmers ){ 			
			System.err.println("array size exceed Integer.MAX_VALUE/2! please reduce kmers length below 6. ");
			System.exit(1);
		}
		
 		//get max entry, that is the size of mersIndex array && init with -1
		byte[] maxMers = new byte[length];
		for(int i = 0; i < length; i++) maxMers[i] = 'N';
		int maxEntry = getEntry( maxMers);
		
		this.merLength = length; 		
		this.mersIndex = new int[maxEntry+1];
		for(int i = 0; i < mersIndex.length; i++) mersIndex[i] = -1; //init		
							
		// produce all possible kmers in String includeing N base
		mersStrList = getPossibleKmerString(merLength, true);	
				
		//get all possible kmers in byte[] and assign an index for each byte combination
		for(int i = 0; i < mersStrList.length; i ++ ){
			int entry = getEntry(mersStrList[i].getBytes());
			mersIndex[ entry ] = i;
		}	
		
		for(int i = 0; i < 3; i ++){
			tally[i] =  new QCMGAtomicLongArray( (int) ( iniCycleNo * mersStrList.length ) );	
			parsedCount[i] = new AtomicLong();
		}
		
		//for short mers on the tail of reads
		int index = 0; 
		for(int m = 1; m < merLength; m ++){
			String[] shortStrList = getPossibleKmerString(m, true);	
			for(int i = 0; i < shortStrList.length; i ++ ){
				int entry = getEntry( shortStrList[i].getBytes());
				mersIndex[ entry ] = index + i;
			}
			index += shortStrList.length; 
		}
	}
	
	public static String producer(final int k, final String mers , final boolean includeN){
		if(k == 0 )  return mers;			
		
		char[] cToUse = includeN ? atgcnCharArray : atgcCharArray;
		StringBuilder conStr = new StringBuilder();
		for(char c : cToUse) {
			StringUtils.updateStringBuilder(conStr, producer( k-1, mers + c,  includeN), Constants.COMMA);
		}
		return conStr.toString();	
	}
	
//	public static String producer(final int k, final String mers , final boolean includeN){ 		
//		if(k == 0 )  return mers;			
//		
//		List<Character> bases = atgc.chars().mapToObj(c ->(char) c).collect(Collectors.toList() ); 
//		if(includeN ) bases.add('N'); 
//				
//		String conStr = "";
//		for(char c : bases) 	 	 
//			conStr += "," + producer( k-1, mers + c,  includeN);			 
//		 		
//		return conStr;		
//	}
	
	public String[] getPossibleKmerString(final int k, final boolean includeN){
		//if require inital mers combination 
		if( k == merLength && includeN && mersStrList != null )  
			return mersStrList; 
		
		//produce all possible kmers in String 
		String str1 = producer( k, "", includeN );		
		return str1.split(Constants.COMMA_STRING);
	}
	
//	public String[] getPossibleKmerString(final int k, final boolean includeN){
//		//if require inital mers combination 
//		if( k == merLength && includeN && mersStrList != null )  
//			return mersStrList; 
//		
//		//produce all possible kmers in String 
//		String str1 = producer( k, "", includeN );		
//		while(str1.contains(",,")) str1 = str1.replace(",,", ",");	
//		while(str1.startsWith(",")) str1 = str1.substring(1);			
//		return str1.split(",");			
//	}
	
	public void parseKmers( byte[] readString, boolean reverse , int flagFirstOfPair){
		 //get the biggest cycle
		 int c = readString.length - merLength + 1;	
		 if(c > cycleNo) cycleNo = c; 
		 byte[] dataString =  readString.clone();  //can't point to same array
		  
		 if (reverse){  
			 byte[] dataReverse = readString.clone(); 
			 for (int i = dataReverse.length -1 , j = 0; i >= 0 ; i--, j++)  
				dataString[j] = ( byte ) BaseUtils.getComplement( (char) dataReverse[i] );
		 }
		 
		 //readString may have differnt length to other reads
		 for(int i = 0; i <= readString.length - merLength; i ++ ){
			 byte[] mers = new byte[ merLength ];
			 for(int j = 0; j <  merLength; j ++ )  mers[j] = dataString[ i + j ];	
			 int pos = getPosition(i, mers );			 
			 tally[flagFirstOfPair].increment(pos);
		 }	
		 
		 parsedCount[flagFirstOfPair].incrementAndGet();
	}
	
	//	can't be private since unit test
	int getPosition(int cycle, byte[] mers){
		int entry = getEntry(mers);
		int pos = mersIndex[ entry ];
		return cycle * mersStrList.length +  pos;
	}
	
	
	private int getEntry(byte[] mers){
		int entry = 0; 
		for(int i = 0, j = mers.length-1; i < mers.length; i ++, j-- ){
			int no = 5; //default is 'N'
			switch (mers[i]){
				case 'A' : no = 1; break;
				case 'C' : no = 2; break;
				case 'G' : no = 3; break;
				case 'T' : no = 4; break;
			}
			entry += no << ( j * 3 ); 	
		}
		
		return entry;
	}
		
	long[] getCounts(final int cycle, String[] possibleMers, int flagFirstOfPair){		
		long[] counts = new long[possibleMers.length];		
		
		for(int i = 0; i< possibleMers.length; i ++)
			counts[i] = getCount( cycle,  possibleMers[i], flagFirstOfPair);
		return counts; 					
	}
	/**
	 * 
	 * @param cycle: number of cycle
	 * @param mers: kmers string
	 * @param flagFirstOfPair: 0: unPaired, 1: firstOfPair, 2: secondOfPair
	 * @return coutns of specified mers string at this cycle
	 */
	long getCount(final int cycle, Object mers, int flagFirstOfPair){  	
		byte[] mer = null; 
		if(mers instanceof String) mer = ((String) mers).getBytes();
		if(mers instanceof byte[]) mer = (byte[]) mers; 
		
		// can't cope with more than 6 kmers
		if(mer.length > merLength || cycle >= cycleNo )  return 0;	
				
		// full length kmers counts are stored in tally already		 	
		if( mer.length == merLength ){ 	
			int pos = getPosition(cycle, mer);
			return tally[flagFirstOfPair].get(pos);
		}
		
		// accumulate all 6-mers start with the inputed short mer on the specified cycle position	
		byte[] small = new byte[ merLength ];
		byte[] big = new byte[ merLength ];
		for(int i = 0; i < mer.length; i ++ ) small[i] = big[i] = mer[i];
		for(int i = mer.length; i < merLength; i ++){ small[i] = 'A'; big[i] = 'N'; }
		
		long count = 0;	
		for(int i = getPosition(cycle, small), j = getPosition(cycle, big); i <=j ; i ++ ) 
		  count +=  tally[flagFirstOfPair].get(i); 
 	 			
		return count; 		
	}
	
	/**
	 * at moment we count short mers on the last cycle from the longest reads
	 * these short mers will be discard on the tail of shorter reads
	 */
	void recaculatelastCycle(){		
		
		for(int i = 1; i < merLength; i ++){			
			//5mers from last cycle of 6mers; 5~1 mers stored after last cycle of tally
			int lastCycle = (i == 1)? cycleNo - 1: cycleNo;  	 
			String[] shortList = getPossibleKmerString(merLength-i, true);		
			for(String shortM : shortList){
				long[] sum = {0,0,0}; 
				for(byte base : atgcn.getBytes()){
					String preMers = (char) base + shortM; 
					int pos = getPosition(lastCycle, preMers.getBytes());
					for(int s = 0; s < 3; s++)
						sum[s] += tally[s].get(pos);
				}
				//put current short mers count to tally
				int pos = getPosition(cycleNo, shortM.getBytes() );
				for(int s = 0; s < 3; s++)
					tally[s].increment(pos, sum[s]);
			}
		}		
		
	}
	
	public void toXml( Element parent, int klength ) { 
		
		Element merElement = QprofilerXmlUtils.createSubElement(parent, "mers" + klength);				

		for(int pair = 0; pair < 3; pair ++){
			if (parsedCount[pair].get() <= 0 ) continue; 
			
			Element element = QprofilerXmlUtils.createSubElement(merElement, "CycleTally" );			
			Set<String> possibleMers = getPopularKmerString(16,  klength, false, pair) ;			
			
			element.setAttribute( "possibleValues", QprofilerXmlUtils.joinByComma(new ArrayList<String>( possibleMers)));
			element.setAttribute( "source",  BamSummaryReport2.sourceName[pair] );
			element.setAttribute( "parsedReads",  parsedCount[pair].get()+"" );
			for(int i = 0; i <  cycleNo; i++ ){ 		
				List<Long> counts = new ArrayList<Long>();
				for(String mer :  possibleMers)
					counts.add(getCount( i,  mer, pair));
				
				Element childElement = QprofilerXmlUtils.createSubElement(element, "Cycle" );
				childElement.setAttribute("value", i+"");
				childElement.setAttribute("counts", QprofilerXmlUtils.joinByComma(  counts));					
			}			
		}				
	}
	
	public Set<String> getPopularKmerString(final int popularNo, final int kLength, final boolean includeN, final int flagFristRead){
	 
		String[] possibleMers = getPossibleKmerString(kLength, false);
		//incase empty input bam
		if(cycleNo == 0 || possibleMers.length <= popularNo    ) 
			return new HashSet<String>(Arrays.asList(possibleMers) );
	 
		
		int midCycle = cycleNo / 2; 
		int bfMidCycle = (midCycle > 20 )? midCycle - 10 : (midCycle < kLength )? 0 : midCycle - kLength; 
		int afMidCycle = (midCycle > 20 )? midCycle + 10 : (midCycle < kLength )? cycleNo-1 : midCycle + kLength; 
				
		long[] bfMidCycleCounts = getCounts(bfMidCycle, possibleMers,flagFristRead);
		long[] afMidCycleCounts = getCounts(afMidCycle, possibleMers,flagFristRead);
		long[] midCycleCounts = getCounts(midCycle, possibleMers,flagFristRead);
		
		for(int i = 0; i < midCycleCounts.length; i ++)
			midCycleCounts[i] += bfMidCycleCounts[i] + afMidCycleCounts[i];
		
		long[] sortCounts = midCycleCounts.clone(); 
		Arrays.sort(sortCounts);
		
		//get biggest popularNo
	//	String[] popularMers = new String[popularNo];
		Set<String> popularMers = new HashSet<>();
		for(int i = 0; i < popularNo; i ++ ){
			long bigValue = sortCounts[sortCounts.length - i-1];
			for(int j = 0; j < possibleMers.length; j ++) {
				if(bigValue == midCycleCounts[j]){
					popularMers.add( possibleMers[j]);
					break;
				}
			}
		}
		return popularMers; 
	}
}