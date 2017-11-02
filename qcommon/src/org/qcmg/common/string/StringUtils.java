/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;

public class StringUtils {
	public static final char TAB = '\t';
	public static final String RETURN = "\n";
	public static final String DOT = ".";

	/**
	 * 
	 * @param existingString
	 * @param shift
	 * @return
	 */
	public static String addASCIIValueToChar(final String existingString, final int shift) {
		char[] charArray = new char[existingString.length()];
		int i = 0;
		for (char c : existingString.toCharArray()) {
			charArray[i++] = (char) (c + shift);
		}
		return String.valueOf(charArray);
	}
	
	public static void updateStringBuilder(StringBuilder sb, CharSequence toAdd, char delim) {
		if (null != sb) {
			if (sb.length() > 0) {
				sb.append(delim);
			}
			sb.append(toAdd);
		}
	}
	
	/**
	 * * Pads a String <code>s</code> to take up <code>n</code>
	 * characters, padding with char <code>c</code> on the
	 * left (<code>true</code>) or on the right (<code>false</code>).
	 * Returns <code>null</code> if passed a <code>null</code>
	 * String.
	 * @param s String to pad
	 * @param n desired length of string
	 * @param c character to use in the padding
	 * @param padLeft boolean indicating if the padding should be on the LHS of the string
	 * @return new string that has been padded accordingly
	 */
	public static String padString(final String s, final int n, final char c, final boolean padLeft) {
		if (s == null) {
			return s;
		}
		// may overflow int size... should not be a problem in real life
		int add = n - s.length(); 
		if (add <= 0) {
			return s;
		}
		StringBuilder str = new StringBuilder(s);
		char[] ch = new char[add];
		Arrays.fill(ch, c);
		if (padLeft) {
			str.insert(0, ch);
		} else {
			str.append(ch);
		}
		return str.toString();
	}
	
	/**
	 * Return the (first occurrence) position in the supplied array of the supplied token<br>
	 * -1 if the token does not appear in the array 
	 * 
	 * @param array String array to be searched
	 * @param token String containing token that is being sought
	 * @param ignoreCase boolean indicating if the match needs to take case into account
	 * @return int 0 based position reference is returned, or -1 if the string is not present in the array
	 * @throws IllegalArgumentException if the string array or token are null
	 */
	public static int getPositionOfStringInArray(final String [] array, final String token, final boolean ignoreCase) {
		if (null == array || null == token) 
			throw new IllegalArgumentException("null parameters passed to getPositionOfStringInArray");
		
		int position = -1;
		int count = 0;
		for (String arrayItem : array) {
			if (arrayItem.equals(token) || (ignoreCase && arrayItem.equalsIgnoreCase(token))) {
				position = count;
				break;
			}
			count++;
		}
		
		return position;
	}
	
	
	public static String parseArray2String(Object[] values){
		StringBuilder sb = new StringBuilder();	
		for (Object t : values)  
			if(t instanceof Number) sb.append(t + ",");
			else sb.append(t.toString() + ",");	 
		 
		if (sb.length() > 0)
			return sb.substring(0, sb.length()-1);
		return null;		
	}
	
	
	public static <T> List<T> getChildArrayFromParentArray(T[] parentArray, Integer[] childArrayPositions) {
		if (null == parentArray || null == childArrayPositions)
			throw new IllegalArgumentException("null parameters passed to getChildArrayFromParentArray");
			
		final int parentArrayLength = parentArray.length;
		if (parentArrayLength == 0) return new ArrayList<T>();
		
		List<T> subArrayList = new ArrayList<T>();
		for (int i : childArrayPositions) {
			if (i >= parentArrayLength)
				throw new IllegalArgumentException("attempt to retrieve element (" + i + ")" +
						" beyond end of parent array, parent array length: " + parentArrayLength + ", ");
				
			subArrayList.add(parentArray[i]);
		}
		return subArrayList;
	}
	
	/**
	 * Checks to see if the supplied string appears in the supplied string array.
	 * Returns true if it does, false otherwise.
	 * <p>
	 * Does this by calling {@link #getPositionOfStringInArray(String[], String, boolean)} 
	 * with <code>false</code> as the boolean argument for <code>ignoreCase</code>
	 * 
	 * @see #getPositionOfStringInArray(String[], String, boolean)
	 */
	public static boolean isStringInStringArray(final String token, final String [] array) {
		return getPositionOfStringInArray(array, token, false) > -1;
	}
	
	/**
	 * Calls {@link #doesStringContainSubString(String, String, boolean)} with <code>failIfNull</code> set to <code>true</code>
	 * 
	 * @see #doesStringContainSubString(String, String, boolean)
	 */
	public static boolean doesStringContainSubString(String outerString, String subString) {
		return doesStringContainSubString(outerString, subString, true);
	}
	
	public static String getStringFromArray(String [] array, String string) {
		for (String arr : array) {
			if (arr.startsWith(string)) return arr;
		}
		return null;
	}
	public static String getStringFromArray(String [] array, String string, boolean ignoreCase) {
		for (String arr : array) {
			if (arr.startsWith(string)) return arr;
		}
		return null;
	}
	
	/**
	 * Examines the supplied outer string to see if it contains the supplied inner string.<br>
	 * If it does, return true, otherwise, return false.<br>
	 * If <code>failIfNull</code> is set to true, and either of the strings are null, an IllegalArgumentException will be thrown.<br> 
	 * If it is set to false, and either of the strings are null, returns false 
	 * 
	 * @param outerString String outer string that is to be searched
	 * @param subString String inner string containing the search text
	 * @param failIfNull boolean indicating if an IllegalArgumentException should be thrown if either of the supplied strings are null
	 * @return true if subString is contained within outerString, false otherwise
	 * @throws IllegalArgumentException if either of the supplied strings are null, and if fialIfNull is set to true
	 */
	public static boolean doesStringContainSubString(String outerString, String subString, boolean failIfNull) {
		
		if (isNullOrEmpty(outerString) || isNullOrEmpty(subString)) {
			if (failIfNull)
				throw new IllegalArgumentException("Null or empty arguments suppleid to doesStringContainSubString");
			else return false;
		}
		
		return outerString.contains(subString);
	}
	
	/**
	 * Calls {@link #isNullOrEmpty(String, boolean)} passing in <code>true</code> as the boolean value for <code>ignoreWhiteSpace</code>
	 * 
	 * @see #isNullOrEmpty(String, boolean)
	 * 
	 */
	public static boolean isNullOrEmpty(final String test) {
		return isNullOrEmpty(test, true);
	}
	
	/**
	 * Tests to see if a String is null or empty.<br>
	 * Returns true if it is null or empty, false otherwise.<p>
	 * If <code>ignoreWhiteSpace</code> is set to <code>true</code>, and the string under test is not null,
	 *  it will be trimmed before the {@link String#isEmpty()} method is invoked.
	 * 
	 * @param test String that is being tested
	 * @param ignoreWhiteSpace boolean indicating if whitespace should be considered when determining if the string is empty
	 * @return true if supplied is null or empty, false otherwise
	 */
	public static boolean isNullOrEmpty(final String test, boolean ignoreWhiteSpace) {
		return null == test || (ignoreWhiteSpace ? test.trim().isEmpty() : test.isEmpty());
	}
	
	public static boolean isNullOrEmptyOrMissingData(final String test, boolean ignoreWhiteSpace) {
		return null == test || (ignoreWhiteSpace ? test.trim().isEmpty() : test.isEmpty()) || Constants.MISSING_DATA_STRING.equals(test);
	}
	public static boolean isNullOrEmptyOrMissingData(final String test) {
		return isNullOrEmptyOrMissingData(test, true);
	}
	
	/**
	 * 
	 * @param test
	 * @return true if supplied is null, empty or ".", false otherwise
	 */
	public static boolean isMissingDtaString(final String test){
		
		boolean flag = isNullOrEmpty(test);
		
		if(!flag) 
			flag = test.equals(DOT);
		 		
		return flag;
	}
	
	/**
	 * Returns a ChrPosition object based on a string of the following format: chr1:123456-123456
	 * <p>
	 * Positions must not be -ve or an IllegalArgumentException will be thrown
	 * 
	 * @param chrPosString String representation of a ChrPosition object
	 * @return ChrPosition object corresponding to the chromosome and positions supplied in the input string
	 * @throws IllegalArgumentException if the supplied string is null, empty, or malformed
	 */
	public static ChrRangePosition getChrPositionFromString(final String chrPosString) {
		if (isNullOrEmpty(chrPosString))
			throw new IllegalArgumentException("null or empty string passed to getChrPositionFromString");
		
		int colonIndex = chrPosString.indexOf(":");
		int minusIndex = chrPosString.indexOf("-");
		
		if (colonIndex == -1 || minusIndex == -1) 
			throw new IllegalArgumentException("malformed string passed to getChrPositionFromString (missing : or -): " + chrPosString);
		
		// check that colonIndex and minusIndex aren't adjacent
		if (colonIndex == minusIndex -1)
			throw new IllegalArgumentException("malformed string passed to getChrPositionFromString - adjacent :- " + chrPosString);
		
		int firstPosition =  Integer.parseInt(chrPosString.substring(colonIndex + 1, minusIndex));
		int secondPosition =  Integer.parseInt(chrPosString.substring(minusIndex+1));
		
		// if either postion is -ve, throw exception 
		if (firstPosition < 0 || secondPosition < 0)
			throw new IllegalArgumentException("malformed string passed to getChrPositionFromString - negative positions: " + chrPosString);
		
		return new ChrRangePosition(chrPosString.substring(0, colonIndex), firstPosition, secondPosition);
	}

	public static String getValueFromKey(String data, String key) {
		return getValueFromKey(data, key, '=');
	}

	public static String getValueFromKey(String data, String key, char separator) {
		if ( ! isNullOrEmpty(data)) {
			if (data.contains(key + separator)) {
				int index = data.indexOf(key + separator) + key.length() + 1;		// take separator into account
				int toIndex = data.indexOf("\t", index);
				if (-1== toIndex) toIndex = data.length();
				return data.substring(index, toIndex);
			}
		}
		return null;
	}
	
	public static String getJoinedString(String a, String b) {
		return getJoinedString(a,b," ");
	}
	
	/**
	 * Utility method to join 2 strings together using the supplied String
	 * Null and empty checks are performed on the 2 operands, and if they are null or empty, the other operand is returned.
	 * No checks are performed on the glue, and so if a null object is passed in, then this will be reflected in the output
	 * 
	 * @param a
	 * @param b
	 * @param glue
	 * @return
	 */
	public static String getJoinedString(String a, String b, String glue) {
		if (null == a) {
			if (null == b) return null;
			return b;
		}
		if (null == b) {
			return a;
		}
		if (a.length() == 0) return b;
		if (b.length() == 0) return a;
		
		return a + glue + b;
	}
		
	public static String toTitleCase (String input) {
		StringBuilder titleCase = new StringBuilder();
	    boolean nextTitleCase = true;

	    for (char c : input.toCharArray()) {
	        if (Character.isSpaceChar(c)) {
	            nextTitleCase = true;
	        } else if (nextTitleCase) {
	            c = Character.toTitleCase(c);
	            nextTitleCase = false;
	        }

	        titleCase.append(c);
	    }

	    return titleCase.toString();
	}
	
	/**
	 * Utility method that returns a boolean indicating if the supplied mainString contains the supplied searchString 
	 * more than a certain number of times, which is dictated by the supplied cutoff parameter.
	 * 
	 * If either of the supplied string are null or empty, false is returned
	 * 
	 * 
	 * 
	 * @param mainString
	 * @param searchString
	 * @param cutoff
	 * @return
	 */
	public static boolean passesOccurenceCountCheck(String mainString, String searchString, int cutoff) {
		if (isNullOrEmpty(mainString)) return false;
		if (isNullOrEmpty(searchString)) return false;
		
		int fromIndex = 0;
		int count = 0;
		while (fromIndex >= 0) {
			fromIndex = mainString.indexOf(searchString, fromIndex + 1);
			if (count++ > cutoff) return false;
		}
		return true;
	}
	
	public static String getTabAndString(String s) {
		return TAB + s;
	}
	
	/**
	 * Returns true if string contains only numbers, false otherwise.
	 * NOTE that this will return true if there are a large number of numbers in the string which could lead to overflow issues when parsing to integer (or smaller)
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		if (isNullOrEmpty(str)) return false;
		
		int length = str.length();
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) return false;
			i++;
		}
		
		for ( ; i < length ; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') return false;
		}
		
		return true;
	}
	
	public static String addToString(String existing, String addition, char delim) {
		if (StringUtils.isNullOrEmpty(existing)) {
			return addition;
		} else if ( ! existing.contains(addition)) {
			return existing += delim + addition;
		} else {
			return existing;
		}
	}
	
	public static String removeFromString(String existing, String addition, char delim) {
		if (null != existing && null != addition && existing.contains(addition)) {
			if (existing.equals(addition)) {
				return null;
			} else if (existing.startsWith(addition)) {	// need to remove semi-colon along with annotation
				return  existing.replace(addition + delim, "");
			} else {
				return  existing.replace(delim + addition, "");
			}
		} else {
			return existing;
		}
	}

	/**
	 * Examines the string and returns true if the character is in it!
	 * 
	 * @param string String that we are examining
	 * @param c upper case char value
	 * @return boolean if the supplied character exists in the supplied string, in either upper case, or lower case
	 */
	public static boolean isCharPresentInString(String string, char c) {
		return null != string && (string.contains("" + c) 
				|| string.contains("" + Character.toLowerCase(c))); 
	}
	
	/**
	 * convert string to number
	 * @param info: a string of number
	 * @param clazz: specify number type [Float or Integer]
	 * @return an Integer or Float
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T string2Number(String str, Class<T> clazz){
		T rate = (clazz.equals(Integer.class))? (T) new Integer(0): (T) new Float(0);		
		try{
			if(clazz.equals(Integer.class)){ 
				rate = (T)  Integer.valueOf(str); 
			}else
				rate = (T)  Float.valueOf(str); 
		}catch(NullPointerException | NumberFormatException  e){} //do nothing

		return  rate;
	}

	public static String getStringFromCharSet(Set<Character> set) {
		final StringBuilder sb = new StringBuilder();
		if (null != set) {
			for (final Character c : set) sb.append(c);
		}
		return sb.toString();
	}

}
