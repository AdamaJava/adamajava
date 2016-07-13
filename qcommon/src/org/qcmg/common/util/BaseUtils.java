/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.Optional;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.Genotype;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;

public class BaseUtils {
	public enum Base{
		A('A'),
		T('T'),
		C('C'),
		G('G'),
		N('N'),
		DOT('.');
		
		private final char base;
		private Base(char base) {
			this.base = base;
		}
		public static final boolean validBase(char c) {
			for (Base base : values()) {
				if (base.base == c) return true;
			}
			return false;
		}
	}
	
	private static final QLogger logger = QLoggerFactory.getLogger(BaseUtils.class);

	public final static int MAX_UNSIGNED_SHORT_VALUE = 1 + (Short.MAX_VALUE * 2);
	public final static int[] EMPTY_DIST_ARRAY = new int[]{0,0,0,0};
	public final static char A = 'A';
	public final static char T = 'T';
	public final static char C = 'C';
	public final static char G = 'G';
	public final static char N = 'N';
	public final static char DOT = '.';
	public final static char M = 'M';
	public final static char R = 'R';
	public final static String BASE_SEPARATOR = "/";
	public final static char DEFAULT_CHAR = '\u0000';
	
	public static final boolean isAT(char c) {
		return c == A || c == T;
	}
	public static final boolean isCG(char c) {
		return c == C || c == G;
	}
	public static final boolean isACGT(char c) {
		return isAT(c) || isCG(c);
//		return c == A || c == C || c == G || c == T;
	}
	public static final boolean isACGTN(char c) {
		return isACGT(c) || c == N;
	}
	public static final boolean isACGTNDot(char c) {
		return isACGTN(c) || c == DOT;
	}
	public static final boolean isACGTNDotMR(char c) {
		return isACGTNDot(c) || c == M || c == R;
	}
	
	/**
	 * Returns a char representation of the complement of the supplied base
	 * <p>
	 * If the supplied char is an 'A' it will return 'T'<br>
	 * If the supplied char is an 'T' it will return 'A'<br>
	 * If the supplied char is an 'C' it will return 'G'<br>
	 * If the supplied char is an 'G' it will return 'C'
	 * 
	 * @param originalBase char representing the original base
	 * @return char representing complement of supplied char
	 * @throws IllegalArgumentExcpetion if supplied base is not A|C|G|T
	 */
	public static char getComplement(char originalBase) {
		if (A == originalBase)
			return T;
		if (T == originalBase)
			return A;
		if (C == originalBase)
			return G;
		if (G == originalBase)
			return C;
		if (N == originalBase)
			return N;
		throw new IllegalArgumentException("Supplied base was not complementable: " + originalBase);
	}
	
	
	/**
	 * Returns an ordered, complemented representation (including separator) of the supplied string
	 * <p>
	 * Gets the genotype characters from the supplied string (will ignore '[' and '/') and passes them to <code>getComplement</code> 
	 * to get the complement value, before passing both complemented characters to <code>getOrderedGenotype</code>, which
	 * creates an ordered string, including separator from them
	 * 
	 * @param bases String representing the original genotype
	 * @return String representing the complemented, ordered genotype, minus any enclosing braces
	 * @see BaseUtils#getComplement(char)
	 * @see BaseUtils#getGenotype(char, char)
	 */
	public static Genotype getComplementedGenotypeFromString(String bases) {
//		public static String getComplementFromString(String bases) {
		if (null == bases) return null;
		
		int i = 0;
		char firstBase = bases.charAt(i++);
		if ('[' == firstBase)
			firstBase = bases.charAt(i++);
			
		char secondBase = bases.charAt(i++);
		if ('/' == secondBase)
			secondBase = bases.charAt(i);
			
		return getGenotype(getComplement(firstBase),getComplement(secondBase)); 
	}
	
	/**
	 * Returns the IUPAC code that corresponds to the supplied character.
	 * 
	 * <p>
	 * See <a href="http://www.ncbi.nlm.nih.gov/SNP/iupac.html">ncbi website</a> for details<p>
	 * <b>Note</b> that if the supplied character is one if the IUPAC codes that returns only a single base,
	 * that value is returned for both alleles
	 * eg. 'A' will return "A/A"
	 * 
	 * @param code char representing the IUPAC code
	 * @return String IUPAC code meaning
	 * @throws IllegalArgumentException if the supplied char is not one of the IUPAc codes
	 */
	public static Genotype getGenotypeFromIUPACCode(char code) {
		if (DEFAULT_CHAR == code) return null;
		switch (code){
		case 'R':
			return new Genotype('A','G');
		case 'Y':
			return new Genotype('C','T');
		case 'S':
			return new Genotype('C','G');
		case 'W':
			return new Genotype('A','T');
		case 'K':
			return new Genotype('G','T');
		case 'M':
			return new Genotype('A','C');
		case 'V':
			return new Genotype('A','C','G');
		case 'H':
			return new Genotype('A','C','T');
		case 'D':
			return new Genotype('A','G','T');
		case 'B':
			return new Genotype('C','G','T');
		case 'N':
			return new Genotype('A','C','G','T');
		case 'A':
		case 'C':
		case 'G':
		case 'T':
			return new Genotype(code ,code);
		default:
			throw new IllegalArgumentException("Unrecognised IUPAC code: " + code);
		}
	}
//	public static String getGenotypeFromIUPACCode(char code) {
//		if (DEFAULT_CHAR == code) return null;
//		switch (code){
//		case 'R':
//			return "A/G";
//		case 'Y':
//			return "C/T";
//		case 'S':
//			return "C/G";
//		case 'W':
//			return "A/T";
//		case 'K':
//			return "G/T";
//		case 'M':
//			return "A/C";
//		case 'V':
//			return "A/C/G";
//		case 'H':
//			return "A/C/T";
//		case 'D':
//			return "A/G/T";
//		case 'B':
//			return "C/G/T";
//		case 'N':
//			return "A/C/G/T";
//		case 'A':
//		case 'C':
//		case 'G':
//		case 'T':
//			return code + BASE_SEPARATOR + code;
//		default:
//			throw new IllegalArgumentException("Unrecognised IUPAC code: " + code);
//		}
//	}
	
	/**
	 * Returns the (ordered) genotype based on the supplied vcf genotype string (which is in 0/0, 0/1, or 1/1 format) 
	 * and the reference and alt characters
	 * <p>
	 * More specifically, if the vcf genotype is 0/0, ref/ref is returned,<br>
	 * if the vcf genotype is 0/1, ref/alt is returned,<br>
	 * if the vcf genotype is 1/1, alt/alt is returned<br>
	 * 
	 * @param genotype String representation of the vcf genotype (0/0, 0/1, or 1/1)
	 * @param ref char reference base
	 * @param alt char alternative base
	 * @return String representing either ref/ref, ref/alt, or ref/alt (ordered)
	 * @see BaseUtils#getGenotype(char, char)
	 */
	public static Genotype getGenotypeFromVcf(String genotype, char ref, char alt) {
//		public static String getGenotypeFromVcf(String genotype, char ref, char alt) {
		if (null == genotype) return null;
		if (genotype.length() != 3) throw new IllegalArgumentException("Genotype string needs to be in '0/1' format");
		
		if ('1' == genotype.charAt(0) && '1' == genotype.charAt(2))
			return getGenotype(alt , alt);
		else if ('1' == genotype.charAt(2))
			return getGenotype(ref , alt);
		else
			return getGenotype(ref , ref);
	}
//	public static String getGenotypeFromVcf(String genotype, char ref, char alt) {
//		if (null == genotype) return null;
//		if (genotype.length() < 3) throw new IllegalArgumentException("Genotype string needs to be in '0/1' format");
//		
//		if ('1' == genotype.charAt(0) && '1' == genotype.charAt(2))
//			return getOrderedGenotype(alt , alt);
//		else if ('1' == genotype.charAt(2))
//			return getOrderedGenotype(ref , alt);
//		else
//			return getOrderedGenotype(ref , ref);
//	}
	
	/**
	 * Returns an ordered genotype string based on the supplied characters.
	 * <p>
	 *  If <code>includeSep</code> is true, will place "/" between the ordered characters<br>
	 * eg. if 'C' and 'A' and 'false' are passed in, output will be AC<br>
	 * eg. if 'T' and 'G' and 'true' are passed in, output will be G/T
	 *  
	 * @param a char genotype base a
	 * @param b char genotype base b
	 * @param includeSep boolean indicating whether the '/' separator should be included in the returned String 
	 * @return String containing the ordered genotype
	 */
//	public static Genotype getOrderedGenotype(char a, char b, boolean includeSep) {
//		if (a < b)
//			return a + (includeSep ? BASE_SEPARATOR : "") + b;
//		else
//			return b + (includeSep ? BASE_SEPARATOR : "") + a;
//	}
	/**
	 * Convenience method that calls {@link BaseUtils#getOrderedGenotype(char, char, boolean)}
	 * passing in <code>true</code> indicating that the separator should be included in the output
	 * @param a char genotype base a
	 * @param b char genotype base b
	 * @see BaseUtils#getOrderedGenotype(char, char, boolean)
	 */
	public static Genotype getGenotype(char a, char b) {
		return new Genotype(a,b);
	}
//	public static String getOrderedGenotype(char a, char b) {
//		return getOrderedGenotype(a,b,true);
//	}
	
	/**
	 * Returns ordered genotype based on the supplied String.
	 * <p>
	 * Genotypes can often contain extraneous characters such as '['<br>
	 * This method attempts to remove such characters, and return the genotype in an ordered fashion.
	 * Note that this method will only return a Genotype object consisting of 2 alleles. If your requirement
	 *  is for a tri-allelic, or indeed a quad-alleleic genotype, then please use the appropriate constructor
	 * 
	 * @param originalGenotype String representing original genotype, which possibly contains '[,],/'
	 * @return Genotype ordered genotype, without extraneous characters
	 * @see BaseUtils#getOrderedGenotype(char, char, boolean)
	 * @see Genotype#Genotype(char, char)
	 */
	public static GenotypeEnum getGenotypeEnum(String originalGenotype) {
		if (StringUtils.isNullOrEmpty(originalGenotype)) return null; 
		if (originalGenotype.length()  < 2) 
			throw new IllegalArgumentException("Supplied genotype was not of correct length: " + originalGenotype);
		
		int i = 0;
		char firstBase = originalGenotype.charAt(i++);
		if ('[' == firstBase)
			firstBase = originalGenotype.charAt(i++);
			
		char secondBase = originalGenotype.charAt(i++);
		if ('/' == secondBase)
			secondBase = originalGenotype.charAt(i);
		
		return GenotypeEnum.getGenotypeEnum(firstBase, secondBase);
	}
	
	public static Genotype getGenotype(String originalGenotype) {
		if (null == originalGenotype) return null; 
		if (originalGenotype.length()  < 2) 
			throw new IllegalArgumentException("Supplied genotype was not of correct length");
		
		int i = 0;
		char firstBase = originalGenotype.charAt(i++);
		if ('[' == firstBase)
			firstBase = originalGenotype.charAt(i++);
		
		char secondBase = originalGenotype.charAt(i++);
		if ('/' == secondBase)
			secondBase = originalGenotype.charAt(i);
		
		return getGenotype(firstBase, secondBase);
	}
//	public static String getOrderedGenotype(String originalGenotype, boolean includeSeperator) {
//		int i = 0;
//		char firstBase = originalGenotype.charAt(i++);
//		if ('[' == firstBase)
//			firstBase = originalGenotype.charAt(i++);
//		
//		char secondBase = originalGenotype.charAt(i++);
//		if ('/' == secondBase)
//			secondBase = originalGenotype.charAt(i);
//		
//		return getOrderedGenotype(firstBase, secondBase, includeSeperator);
//	}
	
	/**
	 * Returns true if the supplied strings are equal, once they have been ordered, and any extraneous info has been removed
	 * 
	 * @param firstGen String first genotype
	 * @param secondGen String second genotype
	 * @return boolean indicating if the supplied genotypes are equal, once they have been ordered, and extraneous info have been removed
	 * @see BaseUtils#getGenotype(String, boolean)
	 */
	public static boolean areGenotypesEqual(String firstGen, String secondGen) {
		if (null == firstGen || null == secondGen) return false;
		return getGenotype(firstGen).equals(getGenotype(secondGen));
	}
	
	
	public static Optional<int[]> decodeDistribution(long code) {
		if (code == 0) {
			return Optional.of(EMPTY_DIST_ARRAY);
		}
		if (code == Long.MIN_VALUE) {
			logger.warn("Distribution overflow!");
			return Optional.empty();
		} else {
			int as = (int) ((code >>> 48) & MAX_UNSIGNED_SHORT_VALUE);
//			logger.info("decode as: " + as);
			int cs =  (int) ((code >>> 32) & MAX_UNSIGNED_SHORT_VALUE);
//			logger.info("decode cs: " + cs);
			int gs =  (int) ((code >>> 16) & MAX_UNSIGNED_SHORT_VALUE);
//			logger.info("decode gs: " + gs);
			int ts =  (int) (code & MAX_UNSIGNED_SHORT_VALUE);
//			logger.info("decode ts: " + ts);
			return Optional.of(new int[]{as,cs,gs,ts});
		}
		
		
	}
	
	
	public static long encodeDistribution(int as, int cs, int gs, int ts) {
		long l = 0;
		if (as > 0 && as <= MAX_UNSIGNED_SHORT_VALUE) {
			System.out.println("as: " + as);
			l += ((long)as << 48);
		} else if (as > MAX_UNSIGNED_SHORT_VALUE) {
			logger.warn("A count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + as);
			return Long.MIN_VALUE;
		}
		
		if (cs > 0 && cs <= MAX_UNSIGNED_SHORT_VALUE) {
			System.out.println("cs: " + cs);
			l += (long)cs << 32;
		} else if (cs > MAX_UNSIGNED_SHORT_VALUE) {
			logger.warn("C count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + cs);
			return Long.MIN_VALUE;
		}
		
		if (gs > 0 && gs <= MAX_UNSIGNED_SHORT_VALUE) {
			System.out.println("gs: " + gs);
			l += ((long)gs << 16);
		} else if (gs > MAX_UNSIGNED_SHORT_VALUE) {
			logger.warn("G count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + gs);
			return Long.MIN_VALUE;
		}
		
		if (ts > 0 && ts <= MAX_UNSIGNED_SHORT_VALUE) {
			System.out.println("ts: " + ts);
			l += ts;
		} else if (ts > MAX_UNSIGNED_SHORT_VALUE) {
			logger.warn("T count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + ts);
			return Long.MIN_VALUE;
		}
		
		return l;
		
//		int acs = 0;
//		long gts = 0;
//		long l = 0;
//		if (as > 0 && as <= MAX_UNSIGNED_SHORT_VALUE) {
//			System.out.println("as: " + as);
//			acs += (as << 16);
//		} else if (as > MAX_UNSIGNED_SHORT_VALUE) {
//			logger.warn("A count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + as);
//			return Long.MIN_VALUE;
//		}
//		
//		if (cs > 0 && cs <= MAX_UNSIGNED_SHORT_VALUE) {
//			System.out.println("cs: " + cs);
//			acs += cs;
//		} else if (cs > MAX_UNSIGNED_SHORT_VALUE) {
//			logger.warn("C count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + cs);
//			return Long.MIN_VALUE;
//		}
//		
//		l = ((long)acs << 32);
////		System.out.println("l binary: " + Long.toBinaryString(l));
//		
//		
//		
//		if (gs > 0 && gs <= MAX_UNSIGNED_SHORT_VALUE) {
//			System.out.println("gs: " + gs);
//			gts += ((long)gs << 16);
//		} else if (gs > MAX_UNSIGNED_SHORT_VALUE) {
//			logger.warn("G count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + gs);
//			return Long.MIN_VALUE;
//		}
//		
//		if (ts > 0 && ts <= MAX_UNSIGNED_SHORT_VALUE) {
//			System.out.println("ts: " + ts);
//			gts += ts;
//		} else if (ts > MAX_UNSIGNED_SHORT_VALUE) {
//			logger.warn("T count is greater than " + MAX_UNSIGNED_SHORT_VALUE + ": " + ts);
//			return Long.MIN_VALUE;
//		}
//		
//		l += gts;
////		System.out.println("acs binary: " + Integer.toBinaryString(acs));
////		System.out.println("gts binary: " + Long.toBinaryString(gts));
////		System.out.println("l binary: " + Long.toBinaryString(l));
//		return l;
	}
	
}
