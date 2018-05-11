package org.qcmg.qprofiler2.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;

public class CycleSummaryUtils {
	public static final Pattern BAD_MD_PATTERN = Pattern.compile("([ACGTN])");	
	private static final Pattern badReadPattern = Pattern.compile("([.N])");	
	
	public static String tallyMDMismatches(final String mdData, Cigar cigar, final CycleSummary<Character> tagMDMismatchByCycle, 
			final byte[] readBases, final boolean reverse, QCMGAtomicLongArray mdRefAltLengthsForward, QCMGAtomicLongArray mdRefAltLengthsReverse) {
		
		if (null == mdData) return null; 
		
		if(cigar.getReadLength() != readBases.length)
			return cigar.toString() + " is invalid cigar, readlength from cigar is not same to length of readBases!";
		
		if (readBases.length == 0) return  "can't process mismatch for an empty readBase";	
				
		boolean deletion = false;
		int position = 1;
		for (int i = 0, size = mdData.length() ; i < size ; ) {
			if (Character.isDigit(mdData.charAt(i))) {								
				int numberLength = 1;
				while (++i < size && Character.isDigit(mdData.charAt(i))) {
					numberLength++;
				}
				position += Integer.parseInt(mdData.substring(i-numberLength, i));					
			} else if ( '^' == mdData.charAt(i) ) {
				deletion = true;
				i++;
			} else if (isInValidExtended(mdData.charAt(i))) {
				if(deletion ){
					while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) { }
					deletion = false; 				
					continue; 
				}
				// got a letter - update summary with positio					 					
				// check cigar to see if we need to adjust our offset due to insertions etc
				int additionalOffset = SAMUtils.getAdjustedReadOffset(cigar, position);	
				if(position + additionalOffset > readBases.length)
					return "invalid MD string, position outside read Base!";
				
				char readBase = (char)readBases[position-1 + additionalOffset];
				char refBase = mdData.charAt(i);					
				if(reverse){
					readBase = BaseUtils.getComplement( readBase );
					refBase = BaseUtils.getComplement( refBase );			 
				}
				
				if (refBase == readBase) { 
					return "Found refBase == altBase, md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse; 
					//System.out.println("Found refBase == altBase at ref position " + (i+1) + " ,refBase: " + refBase + ", md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse);
				}
//				int pos = (reverse)? readBases.length - position + 1 : position; 		
				
				int pos = position + additionalOffset;
				if(reverse) pos = readBases.length - pos + 1; 
				tagMDMismatchByCycle.increment(pos, readBase);
				
				int intFromChar = getIntFromChars(refBase, readBase);
				if( reverse && mdRefAltLengthsReverse != null )
					mdRefAltLengthsReverse.increment(intFromChar);					
				else if( !reverse && mdRefAltLengthsForward != null )
					mdRefAltLengthsForward.increment(intFromChar);
				i++;
				position++;
				 
			} else i++;	// need to increment this or could end up with infinite loop...			 
					
		}
		
		return null; 				
	}
	/**
	 * 
	 * @param mdCycles
	 * @param percent
	 * @param allReadsLineLengths
	 * @return the number of cycle with big mismatch rate after combining all strand reads mismatch information
	 */
	//public static int getBigMDCycleNo(CycleSummary<Character>[] mdCycles, float percent, long totalRecords ){
	public static int getBigMDCycleNo(CycleSummary<Character>[] mdCycles, float percent, QCMGAtomicLongArray[] allReadsLineLengths ){
		if(  mdCycles.length <= 0 ) return 0; 
		
		//get all cycle number
		TreeSet<Integer> cycles = (TreeSet<Integer>) mdCycles[0].cycles();
		for(int i = 1; i < mdCycles.length; i++) 
			cycles.addAll(mdCycles[i].cycles());
				
		int mismatchingCycles = 0;
		for(Integer cycle : cycles){
			long count = 0, allReadsCount = 0;
			for(int i = 0; i < mdCycles.length; i++){
				count += SummaryReportUtils.getCountOfMapValues(mdCycles[i].getValue(cycle));	
				allReadsCount += allReadsLineLengths[i].get(cycle);
			}					
			if ( (( (double) count / allReadsCount )) > percent) 	
				mismatchingCycles++;		 		
		}
		
		return mismatchingCycles; 
	}
	/**
	 * Adds an xml representation of the current object to the supplied parent element.
	 * 
	 * @param parent Element that the current objects xml representation will be added to
	 * @param elementName String representing the name to be used when creating the element
	 * @return 
	 */
	public static <T> void toXmlWithPercentage( CycleSummary<T> sumByCycle,  Element parent, String elementName ,
			String sourceName,  QCMGAtomicLongArray lengthArray) {
		
		Element element = (parent.getElementsByTagName(elementName).getLength() > 0)?
				(Element) parent.getElementsByTagName(elementName).item(0)  :QprofilerXmlUtils.createSubElement(parent, elementName);
		
		//do nothing if no base detected
		Set<T> possibles = sumByCycle.getPossibleValues();
		if(possibles.size() == 0 ) return; 
		
        // adding another level to conform to DTD..
        Element cycleTallyElement = QprofilerXmlUtils.createSubElement(element, "CycleTally");       
        cycleTallyElement.setAttribute("possibleValues", QprofilerXmlUtils.joinByComma(new ArrayList<T>(possibles)));
        
		if(sourceName != null)
			cycleTallyElement.setAttribute("source", sourceName);
        
        try {
            final NumberFormat nf = new DecimalFormat("0.0#%");
            	                
            for (Integer cycle : sumByCycle.cycles()) {
            	StringBuilder sb = new StringBuilder();
            	long mapTotal = 0; 
    			for(T t :  sumByCycle.getPossibleValues()){ 
    				long c = sumByCycle.count(cycle, t);
    				mapTotal += c; 
    				sb.append( c + QprofilerXmlUtils.COMMA );	
    			}
    			String counts =  (sb.length() > 0)? sb.substring(0, sb.length()-QprofilerXmlUtils.COMMA.length()) : "0";
    			long count = lengthArray.get(cycle);  
            	double percentage = (((double) mapTotal / count));                                            
                Element cycleE = QprofilerXmlUtils.createSubElement(cycleTallyElement, "Cycle");   
                cycleE.setAttribute("value", cycle.toString());
                cycleE.setAttribute("counts", counts);
                cycleE.setAttribute("percent", nf.format(percentage));
                cycleE.setAttribute("totalBase", count+"" );
                
		         //base number on that cycle is reducing since short reads chopped at current cycle
		         AtomicLong ml = new AtomicLong(lengthArray.get(cycle));
		         if (null != ml)   count -= ml.get();                 
            }
        } catch (DOMException e) {   e.printStackTrace(); }
    }		
	
	
	private static boolean isInValidExtended(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}
	
	private static boolean isInValidExtendedInDelete(char c) {
		if (! isInValidExtended(c))
			return c == 'M' || c =='R';
		else return true;
	}
	
	
	
	public static int getIntFromChars(final char ref, final char alt) {
		switch (ref) {
		case 'A':
			return 'A' == alt ? 1 : ('C' == alt ? 2 : ('G' == alt ? 3 : ('T' == alt ? 4 : 5)));
		case 'C':
			return 'A' == alt ? 6 : ('C' == alt ? 7 : ('G' == alt ? 8 : ('T' == alt ? 9 : 10)));
		case 'G':
			return 'A' == alt ? 11 : ('C' == alt ? 12 : ('G' == alt ? 13 : ('T' == alt ? 14 : 15)));
		case 'T':
			return 'A' == alt ? 16 : ('C' == alt ? 17 : ('G' == alt ? 18 : ('T' == alt ? 19 : 20)));
		case 'N':
			return 'A' == alt ? 21 : ('C' == alt ? 22 : ('G' == alt ? 23 : ('T' == alt ? 24 : 25)));
		}
		return -1;
	}
	
	public static String getStringFromInt(final int i) {
		switch (i) {
		// A's
		case 1: return "A>A";
		case 2: return "A>C";
		case 3: return "A>G";
		case 4: return "A>T";
		case 5: return "A>N";
		
		//C's
		case 6: return "C>A";
		case 7: return "C>C";
		case 8: return "C>G";
		case 9: return "C>T";
		case 10: return "C>N";
		
		//G's
		case 11: return "G>A";
		case 12: return "G>C";
		case 13: return "G>G";
		case 14: return "G>T";
		case 15: return "G>N";
		
		//T's
		case 16: return "T>A";
		case 17: return "T>C";
		case 18: return "T>G";
		case 19: return "T>T";
		case 20: return "T>N";
		
		//N's
		case 21: return "N>A";
		case 22: return "N>C";
		case 23: return "N>G";
		case 24: return "N>T";
		case 25: return "N>N";
		
		// hmmmm
		case -1: return "???";
		}
		return null;
	}
	
	
}
