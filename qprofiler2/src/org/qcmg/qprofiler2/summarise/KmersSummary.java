package org.qcmg.qprofiler2.summarise;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.qprofiler2.bam.BamSummaryReport;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class KmersSummary {

	public static final int BITS_TO_SHIFT = 3;
		
	// init 0, eventually store the biggest cycle number
	private int cycleNo ; 

	// unpaired, firstOfPair, secondOfPair
	private final QCMGAtomicLongArray[] tally = new QCMGAtomicLongArray[3] ;
	private final AtomicLong[] parsedCount = new AtomicLong[3]; 
			
	// here we can only handle read length smaller than 1000
    static final int maxCycleNo = 1000;	
	private static final int iniCycleNo = 126;
	
	public static final int maxKmers = 6; 	
	// Array in java is mutable, so set to final is meaningless, here we use unmodifiableList
	public static final List<Character>  atgcCharArray = List.of('A', 'T', 'G', 'C');
	public static final List<Character>  atgcnCharArray = List.of('A', 'T', 'G', 'C', 'N');
	
	private final int merLength;
	public int[] mersIndex;
	private final String[] mersStrList;

	private final int mersStrListLength;

	public static final int BIT_MASK_VALUE = Integer.valueOf(StringUtils.padString("", maxKmers * BITS_TO_SHIFT, '1', false), 2);
	// Precompute nucleotide values in a lookup table
	public static final int[] nucleotideValues = new int[256];
	static {
		nucleotideValues['A'] = 1;
		nucleotideValues['C'] = 2;
		nucleotideValues['G'] = 3;
		nucleotideValues['T'] = 4;
		nucleotideValues['N'] = 5;
	}
	public KmersSummary( int length ) {	
		cycleNo = 0 ; // init 0, eventually store the biggest cycle number
		this.merLength = length; 
		
		if (length > maxKmers ) {			
			this.mersIndex = null;
			this.mersStrList = null;
			mersStrListLength = 0;
			System.err.println("array size exceed Integer.MAX_VALUE/2! please reduce kmers length below 6!");
			return;
		}
		
 		// get max entry, that is the size of mersIndex array && init with -1
		byte[] maxMers = new byte[length];
		Arrays.fill(maxMers, (byte) 'N');
		int maxEntry = getEntry( maxMers);
						
		this.mersIndex = new int[maxEntry + 1];
		// init
		Arrays.fill(mersIndex, -1);
							
		// produce all possible kmers in String including N base
		this.mersStrList = getPossibleKmerString(merLength, true);
		this.mersStrListLength = mersStrList.length;
				
		// get all possible kmers in byte[] and assign an index for each byte combination
		for (int i = 0; i < mersStrListLength; i ++ ) {
			// UTF-8 is a good choice because it is always supported and can encode any character.
			int entry = getEntry(mersStrList[i].getBytes(StandardCharsets.UTF_8));
			mersIndex[ entry ] = i;
		}	
		
		for (int i = 0; i < 3; i ++) {
			// array capacity for kmers. eg. kmer=6, base=1000mers ,  
			// maxCapacity = 4^6 * 1000 = 4096,000 in case of excludes 'N' 
			// maxCapacity = 5^6 * 1000 = 15625,000 in case of includes 'N'
			tally[i] =  new QCMGAtomicLongArray(iniCycleNo * mersStrListLength, maxCycleNo * mersStrListLength);
			parsedCount[i] = new AtomicLong();
		}
		
		// for short mers on the tail of reads
		int index = 0; 
		for (int m = 1; m < merLength; m ++) {
			String[] shortStrList = getPossibleKmerString(m, true);	
			for (int i = 0; i < shortStrList.length; i ++ ) {
				int entry = getEntry( shortStrList[i].getBytes(StandardCharsets.UTF_8));
				mersIndex[ entry ] = index + i;
			}
			index += shortStrList.length; 
		}
	}
	
	public static String producer(final int k, final String mers , final boolean includeN) {
		if (k == 0 ) {
			return mers;			
		}
		
		List<Character> cToUse = includeN ? atgcnCharArray : atgcCharArray;
		StringBuilder conStr = new StringBuilder();
		for (char c : cToUse) {
			StringUtils.updateStringBuilder(conStr, producer( k - 1, mers + c,  includeN), Constants.COMMA);
		}
		
		return conStr.toString();	
	}

	public String[] getPossibleKmerString(final int k, final boolean includeN) {
		// if require initial mers combination
		if ( k == merLength && includeN && mersStrList != null ) {
			return Arrays.copyOf( mersStrList, mersStrListLength);
		}
		
		// produce all possible kmers in String 
		String str1 = producer( k, "", includeN );	
		String[] mers = str1.split(Constants.COMMA_STRING);
		// arrays are always mutable, it returns the reference, then you lose control over the contents your variable, violating encapsulation.
		return Arrays.copyOf( mers, mers.length);
	}	

	public void parseKmers( byte[] readString, boolean reverse , int flagFirstOfPair) {
		int readLength = readString.length;
		if ( readLength > maxCycleNo ) {
			throw new IllegalArgumentException("Can't handle large read sequence, which length is greater than " + maxCycleNo + "!");
		}
		// get the biggest cycle
		int c = readLength - merLength + 1;
		if (c > cycleNo) {
			cycleNo = c;
		}

		byte[] dataString = reverse ? new byte[readLength] : readString;
		if (reverse) {
			for (int i = readLength - 1 , j = 0; i >= 0 ; i --, j ++) {
				dataString[j] = (byte) BaseUtils.getComplement( (char) readString[i] );
			}
		}
		// readString may have different length to other reads
		int kmerId = 0;
		int kmerIndex = 0;
		QCMGAtomicLongArray tallyLocal = tally[flagFirstOfPair]; // Cache in a local variable.
		for (int i = 0; i < readLength; i ++ ) {
			int nucleotide = nucleotideValues[dataString[i]];
			kmerId = (kmerId << BITS_TO_SHIFT) + (nucleotide > 0 ? nucleotide : 5); //nucleotideValues[dataString[i]]; //incrementInt(kmerId, dataString[i], BITS_TO_SHIFT);
			if (i >= merLength - 1) {
				/*
				get the relevant part of the kmerId int by bit masking
				 */
				tallyLocal.increment( (kmerIndex++ * mersStrListLength) + mersIndex[kmerId & BIT_MASK_VALUE]);
			}
		}
		parsedCount[flagFirstOfPair].incrementAndGet();
	}

	public static int incrementInt(int toIncrement, byte b, int leftShift) {
		// Perform the left shift and add the incrementer in one step
		return (toIncrement << leftShift) + nucleotideValues[b];
	}
	
	// 	can't be private since unit test
	int getPosition(int cycle, byte[] mers) {
		int entry = getEntry(mers);
		int pos = mersIndex[ entry ];
		return cycle * mersStrListLength +  pos;
	}
	
	// just for fast calculate, the return value is not position of mers in QCMGArray
	static int getEntry(byte[] mers) {
		int entry = 0;
		int shiftAmount = (mers.length - 1) * 3;

		for (byte mer : mers) {
			entry += nucleotideValues[mer] << shiftAmount;
			shiftAmount -= 3;
		}

		return entry;
	}
		
	long[] getCounts(final int cycle, String[] possibleMers, int flagFirstOfPair) {		
		long[] counts = new long[possibleMers.length];		
		
		for (int i = 0; i < possibleMers.length; i ++) {
			counts[i] = getCount( cycle,  possibleMers[i], flagFirstOfPair);
		}
		
		return counts; 					
	}
	/**
	 * 
	 * @param cycle number of cycle
	 * @param mers kmers string
	 * @param flagFirstOfPair 0: unPaired, 1: firstOfPair, 2: secondOfPair
	 * @return counts of specified mers string at this cycle
	 */
	long getCount(final int cycle, Object mers, int flagFirstOfPair) {	
		byte[] mer = null; 
		if (mers instanceof String) {
			mer = ((String) mers).getBytes(StandardCharsets.UTF_8);
		}
		if (mers instanceof byte[]) {
			mer = (byte[]) mers; 
		}
		
		// can't cope with more than 6 kmers
		if (mer.length > merLength || cycle >= cycleNo ) {
			return 0;	
		}
				
		// full length kmers counts are stored in tally already		 	
		if ( mer.length == merLength ) {
			int pos = getPosition(cycle, mer);
			return tally[flagFirstOfPair].get(pos);
		}
		
		// accumulate all 6-mers start with the inputted short mer on the specified cycle position
		byte[] small = new byte[ merLength ];
		byte[] big = new byte[ merLength ];
		for (int i = 0; i < mer.length; i ++ ) {
			small[i] = big[i] = mer[i];
		}
		
		for (int i = mer.length; i < merLength; i ++) {
			small[i] = 'A'; big[i] = 'N'; 
		}
		
		long count = 0;	
		for (int i = getPosition(cycle, small), j = getPosition(cycle, big); i <= j ; i ++ ) {
		  count +=  tally[flagFirstOfPair].get(i); 
		}
		return count; 		
	}
	
	 
	/**
	 * create xml 
	 * 
	 * @param parent  is the parent element
	 * @param klength is the length of kemers
	 * @param isFastq is true, the variable group name will be length+"kmers"; otherwise it will be pair type. 
	 */
	public void toXml( Element parent,  int klength , boolean isFastq) {
		long sum = 0;
		for (int pair = 0; pair < 3; pair ++) {
			sum += parsedCount[pair].get();	
		}
		Element merEle = XmlUtils.createMetricsNode( parent, klength + "mers", 
				new Pair<>(ReadGroupSummary.READ_COUNT, sum));
		
		final int maxNo = 16;				
		for (int pair = 0; pair < 3; pair ++) {
			if (parsedCount[pair].get() <= 0 ) {
				continue; 	
			}
			
			// avoid kmers_null or kmers_unPaired in case have no pair
			String name = BamSummaryReport.sourceName.get(pair);
			
			// read may have no pair information such as fastq
			if ( isFastq ) {
				name = klength + "mers";
			}
			Set<String> kmerStrs = getPopularKmerString(klength, pair);
			
			// "counts per mer string start on specified base cycle"	
			Element ele = XmlUtils.createGroupNode(merEle, name);
	 		for ( int i = 0; i < cycleNo; i ++ ) {	
	 			Map<String, AtomicLong> map = new HashMap<>();
	 			for (String mer :  kmerStrs) {
					long c = getCount( i,  mer, pair);
					if ( c > 0 ) {
						map.put(mer, new AtomicLong(c));
					}					
				}
				XmlUtils.outputCycleTallyGroup( ele, String.valueOf(i + 1), map, false );					
			}	
			if (	Math.pow(4, klength) > maxNo ) {
				XmlUtils.addCommentChild(ele, "here only list top " + maxNo + " most popular kmers sequence for each Base Cycle" );
			}
		}			
	}	
	
	// set to package access level due for unit test
	Set<String> getPopularKmerString(final int kLength, final int flagFirstRead) {
	 
		String[] possibleMers = getPossibleKmerString(kLength, false);
		// in case empty input bam
		if (cycleNo == 0 || possibleMers.length <= 16) {
			return new HashSet<>(Arrays.asList(possibleMers) );
		}
		
		// select three positions: middle, middle of first half, middle of second half
		int midCycle = cycleNo / 2; 
		int bfMidCycle = (midCycle > 20 ) ? midCycle - 10 : (midCycle < kLength ) ? 0 : midCycle - kLength; 
		int afMidCycle = (midCycle > 20 ) ? midCycle + 10 : (midCycle < kLength ) ? cycleNo - 1 : midCycle + kLength; 
						
		long[] bfMidCycleCounts = getCounts(bfMidCycle, possibleMers, flagFirstRead);
		long[] afMidCycleCounts = getCounts(afMidCycle, possibleMers, flagFirstRead);
		long[] midCycleCounts = getCounts(midCycle, possibleMers, flagFirstRead);
		for (int i = 0; i < midCycleCounts.length; i ++) {
			midCycleCounts[i] += bfMidCycleCounts[i] + afMidCycleCounts[i];
		}
		// sort three position sum
		long[] sortCounts = midCycleCounts.clone(); 
		Arrays.sort(sortCounts);
		
		// get biggest popularNo 
		Set<String> popularMers = new HashSet<>();
		for (int i = 0; i < 16; i ++ ) {
			long bigValue = sortCounts[sortCounts.length - i - 1];
			if ( bigValue == 0 ) {
				break; // find zero is meaningless
			}
			for (int j = 0; j < possibleMers.length; j ++) {
				if (bigValue == midCycleCounts[j] && !popularMers.contains(possibleMers[j])) {
					popularMers.add( possibleMers[j]);
					break;
				}
			}
		}
		return popularMers; 
	}
}
