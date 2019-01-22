package org.qcmg.qprofiler.summarise;

import java.util.Arrays;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KmersSummary {
		
	private int cycleNo = 0 ; //init	
	private final QCMGAtomicLongArray tally;
	private static final int INI_CYCLE_NUMBER = 126;	
	public static final int MAX_KMERS = 6; 
	public static final char[] ATGC_CHAR_ARRAY = new char[]{'A','T','G','C'};
	public static final char[] ATGCN_CHAR_ARRAY = new char[]{'A','T','G','C','N'};
	
	private final int merLength; 
	private final int[] mersIndex; 
	private final String[] mersStrList; 
	
	public KmersSummary( int length ) {	
		if(length > MAX_KMERS ){ 			
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
							
		// produce all possible kmers in String including N base
		mersStrList = getPossibleKmerString(merLength, true);	
				
		//get all possible kmers in byte[] and assign an index for each byte combination
		for(int i = 0; i < mersStrList.length; i ++ ){
			int entry = getEntry(mersStrList[i].getBytes());
			mersIndex[ entry ] = i;
		}											
		tally =  new QCMGAtomicLongArray( INI_CYCLE_NUMBER * mersStrList.length , 6 * 1024 * 1024);	
		
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
		
		char[] cToUse = includeN ? ATGCN_CHAR_ARRAY : ATGC_CHAR_ARRAY;
		StringBuilder conStr = new StringBuilder();
		for(char c : cToUse) {
			StringUtils.updateStringBuilder(conStr, producer( k-1, mers + c,  includeN), Constants.COMMA);
		}
		return conStr.toString();	
	}
	
	public String[] getPossibleKmerString(final int k, final boolean includeN){
		//if require initial mers combination 
		if( k == merLength && includeN && mersStrList != null )  
			return mersStrList; 
		
		//produce all possible kmers in String 
		String str1 = producer( k, "", includeN );		
		return TabTokenizer.tokenize(str1, Constants.COMMA);
	}
	
	public void parseKmers( byte[] readString, boolean reverse ){
		//set to false, it indicate we have to recalculate short kmer for last cycle since new read coming
		
		 //get the biggest cycle
		 int c = readString.length - merLength + 1;	
		 if(c > cycleNo) cycleNo = c; 
		 byte[] dataString =  readString.clone();  //can't point to same array
		  
		 if (reverse){  
			 byte[] dataReverse = readString.clone(); 
			 for (int i = dataReverse.length -1 , j = 0; i >= 0 ; i--, j++)  
				dataString[j] = ( byte ) BaseUtils.getComplement( (char) dataReverse[i] );
		 }
		 
		 //readString may have different length to other reads
		 for (int i = 0; i <= readString.length - merLength; i ++ ){
			 byte[] mers = new byte[ merLength ];
			 for (int j = 0; j <  merLength; j ++ ) {
				 mers[j] = dataString[ i + j ];
			 }
			 int pos = getPosition(i, mers );
			 tally.increment( pos );
		 }		 		 		 
	}
	
	//	can't be private since unit test
	int getPosition(int cycle, byte[] mers){
		int entry = getEntry(mers);
		int pos = mersIndex[ entry ];
		return cycle * mersStrList.length +  pos;
	}
	
	
	public static int getEntry(byte[] mers){
		int entry = 0;
		int no = 0;
		int j = mers.length-1;
		for (byte b : mers) {
			switch (b){
			case 'A' : no = 1; break;
			case 'C' : no = 2; break;
			case 'G' : no = 3; break;
			case 'T' : no = 4; break;
			default : no = 5; break;
			}
			entry += no << ( j-- * 3 );
		}
		return entry;
	}
		
	long[] getCounts(final int cycle, String[] possibleMers){		
		long[] counts = new long[possibleMers.length];		
		
		for(int i = 0; i< possibleMers.length; i ++)
			counts[i] = getCount( cycle,  possibleMers[i]);
		return counts; 					
	}
	
	long getCount(final int cycle, Object mers){		
		byte[] mer = null; 
		if(mers instanceof String) {
			mer = ((String) mers).getBytes();
		} else if(mers instanceof byte[]) {
			mer = (byte[]) mers; 
		}
		
		// can't cope with more than 6 kmers
		if(mer.length > merLength || cycle >= cycleNo )  return 0;	
				
		// full length kmers counts are stored in tally already		 	
		if(mer.length == merLength) {
			int pos = getPosition(cycle, mer);
			if (pos >= 0 && pos < tally.length()) {
				return  tally.get( pos );
			}
		}
						
		// accumulate all 6-mers start with the inputed short mer on the specified cycle position	
		byte[] small = new byte[ merLength ];
		byte[] big = new byte[ merLength ];
		for (int i = 0; i < mer.length; i ++ ) {
			small[i] = big[i] = mer[i];
		}
		for(int i = mer.length; i < merLength; i ++){ 
			small[i] = 'A'; big[i] = 'N'; 
		}
		
		long count = 0;	
		for(int i = getPosition(cycle, small), j = getPosition(cycle, big); i <=j ; i ++ ) {
			if (i >= 0 && i < tally.length()) {
				count +=  tally.get(i) ;
			}
		}
 	 			
		return count; 		
	}
	
	public void toXml( Element parent, int klength ) { 
		
		final String elementName = "mers" + klength; 				
		
		String[] possibleMers = getPopularKmerString(16,  klength, false) ;
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
		
		// adding another level to conform to DTD..
		Element cycleTallyElement = doc.createElement( "CycleTally" );
		element.appendChild(cycleTallyElement);
		cycleTallyElement.setAttribute( "possibleValues", StringUtils.parseArray2String( possibleMers  ) );
		
		for(int i = 0; i <  cycleNo; i++ ){ 
			Element cycleE = doc.createElement("Cycle");
			cycleTallyElement.appendChild(cycleE);			 
			cycleE.setAttribute("value", i+"");
			cycleE.setAttribute("counts", StringUtils.parseArray2String( Arrays.stream( getCounts(i, possibleMers)  ).boxed().toArray( Long[]::new )) );	
		}		
	}
	
	public String[] getPopularKmerString(final int popularNo, final int kLength, final boolean includeN){
		String[] possibleMers = getPossibleKmerString(kLength, false);
		
		if (possibleMers.length <= popularNo) {
			return possibleMers;
		}
		
		int midCycle = cycleNo / 2; 
		int bfMidCycle = (midCycle > 20 )? midCycle - 10 : (midCycle < kLength )? 0 : midCycle - kLength; 
		int afMidCycle = (midCycle > 20 )? midCycle + 10 : (midCycle < kLength )? cycleNo-1 : midCycle + kLength; 
		
		long[] bfMidCycleCounts = getCounts(bfMidCycle, possibleMers);
		long[] afMidCycleCounts = getCounts(afMidCycle, possibleMers);
		long[] midCycleCounts = getCounts(midCycle, possibleMers);
		
		for (int i = 0; i < midCycleCounts.length; i ++) {
			midCycleCounts[i] += bfMidCycleCounts[i] + afMidCycleCounts[i];
		}
		
		long[] sortCounts = midCycleCounts.clone(); 
		Arrays.sort(sortCounts);
		
		//get biggest popularNo
		String[] popularMers = new String[popularNo];
		for (int i = 0; i < popularNo; i ++ ){
			long bigValue = sortCounts[sortCounts.length - i-1];
			for (int j = 0; j < possibleMers.length; j ++) {
				if (bigValue == midCycleCounts[j]){
					popularMers[i] = possibleMers[j];
					break;
				}
			}
		}
		return popularMers; 
	}
}