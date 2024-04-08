/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.string;

import java.util.*;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;

public class StringUtils {
    public static final char TAB = Constants.TAB;
    public static final String DOT = Constants.MISSING_DATA_STRING;
    public static final String RETURN = "\n";

    /**
     * Adds the given shift value to the ASCII value of each character in the existing string and returns the modified string.
     *
     * @param existingString the string to which the ASCII value will be added to each character
     * @param shift the value to be added to the ASCII value of each character
     * @return the modified string with each character having the ASCII value incremented by the given shift value
     */
    public static String addASCIIValueToChar(final String existingString, final int shift) {
        char[] charArray = new char[existingString.length()];
        int i = 0;
        for (char c : existingString.toCharArray()) {
            charArray[i++] = (char) (c + shift);
        }
        return String.valueOf(charArray);
    }

    /**
     * Update the given StringBuilder by appending the specified CharSequence with a delimiter.
     *
     * @param sb The StringBuilder to be updated. Can be null.
     * @param toAdd The CharSequence to be appended. Can be null.
     * @param delim The delimiter character to be added before the appended CharSequence.
     */
    public static void updateStringBuilder(StringBuilder sb, CharSequence toAdd, char delim) {
        if (null != sb) {
            if (!sb.isEmpty()) {
                sb.append(delim);
            }
            sb.append(toAdd);
        }
    }

    public static String parseArray2String(Object[] values) {
        if (null != values) {
            return Arrays.stream(values).map(Object::toString).collect(Collectors.joining(Constants.COMMA_STRING));
        } else {
            return null;
        }
    }


    public static int[][] getStartPositionsAndLengthOfSubStrings(String reference, String sequence) {
        List<int[]> results = new ArrayList<>();
        int previousRefStartPosition = 0;
        for (int i = 0, maxLength = sequence.length() - 13; i < maxLength; i++) {
            String nextSeqTile = sequence.substring(i, i + 13);
            int refNextStartPosition = reference.indexOf(nextSeqTile, previousRefStartPosition);
            if (refNextStartPosition > -1) {
                int nextBlockLength = getLengthOfSubStringMatch(reference.substring(refNextStartPosition), sequence.substring(i));
                results.add(new int[]{refNextStartPosition, i, nextBlockLength});
                i += (nextBlockLength - 1);
                previousRefStartPosition = refNextStartPosition + nextBlockLength;
            }
        }

        return results.toArray(new int[][]{});
    }

    public static int getLengthOfSubStringMatch(String reference, String sequence) {
        int currentTally = 0;
        for (int i = 0, refLength = reference.length(), seqLength = sequence.length(); i < refLength && i < seqLength; i++) {
            char refC = reference.charAt(i);
            char seqC = sequence.charAt(i);

            if (refC == seqC) {
                currentTally++;
            } else {
                /*
                 * no longer have a match - bail and return currentTally
                 */
                break;
            }
        }
        return currentTally;
    }

    /**
     * * Pads a String <code>s</code> to take up <code>n</code>
     * characters, padding with char <code>c</code> on the
     * left (<code>true</code>) or on the right (<code>false</code>).
     * Returns <code>null</code> if passed a <code>null</code>
     * String.
     *
     * @param s       String to pad
     * @param n       desired length of string
     * @param c       character to use in the padding
     * @param padLeft boolean indicating if the padding should be on the LHS of the string
     * @return new string that has been padded accordingly
     */
    public static String padString(final String s, final int n, final char c, final boolean padLeft) {
        if (s == null) {
            return null;
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
     * @param array      String array to be searched
     * @param token      String containing token that is being sought
     * @param ignoreCase boolean indicating if the match needs to take case into account
     * @return int 0 based position reference is returned, or -1 if the string is not present in the array
     * @throws IllegalArgumentException if the string array or token are null
     */
    public static int getPositionOfStringInArray(final String[] array, final String token, final boolean ignoreCase) {
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

    /**
     * Returns a sublist of elements from the parent array at the specified positions.
     *
     * @param parentArray         the parent array from which the elements will be retrieved
     * @param childArrayPositions an array of positions indicating which elements to retrieve from the parent array
     * @param <T>                 the type of the elements in the arrays
     * @return a new list containing the elements from the parent array at the specified positions
     * @throws IllegalArgumentException if either the parentArray or the childArrayPositions are null
     *                                  or if any position in childArrayPositions is invalid
     */
    public static <T> List<T> getChildArrayFromParentArray(T[] parentArray, Integer[] childArrayPositions) {
        if (null == parentArray || null == childArrayPositions)
            throw new IllegalArgumentException("null parameters passed to getChildArrayFromParentArray");

        final int parentArrayLength = parentArray.length;
        if (parentArrayLength == 0) {
            return new ArrayList<>();
        }

        List<T> subArrayList = new ArrayList<>();
        for (int i : childArrayPositions) {
            if (i >= parentArrayLength) {
                throw new IllegalArgumentException("attempt to retrieve element (" + i + ")" +
                        " beyond end of parent array, parent array length: " + parentArrayLength + ", ");
            }
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
    public static boolean isStringInStringArray(final String token, final String[] array) {
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

    /**
     * Determines the sequence complexity of a given DNA sequence.
     *
     * @param sequence the DNA sequence to determine the complexity of
     * @return a BitSet representing the complexity of the sequence, where each bit is set if the corresponding base (A, C, G, T) is present in the sequence
     */
    public static BitSet determineSequenceComplexity(String sequence) {
        BitSet bs = new BitSet(4);
        for (int i = 0, len = sequence.length(); i < len; i++) {
            if (i > 4 && bs.cardinality() == 4) {
                /*
                 * no point continuing - all bases have been seen
                 */
                break;
            }
            switch (sequence.charAt(i)) {
                case 'A':
                    bs.set(0);
                    break;
                case 'C':
                    bs.set(1);
                    break;
                case 'G':
                    bs.set(2);
                    break;
                case 'T':
                    bs.set(3);
                    break;
                default:
                    break;
            }
        }
        return bs;
    }

    /**
     * Examines the supplied outer string to see if it contains the supplied inner string.<br>
     * If it does, return true, otherwise, return false.<br>
     * If <code>failIfNull</code> is set to true, and either of the strings are null, an IllegalArgumentException will be thrown.<br>
     * If it is set to false, and either of the strings are null, returns false
     *
     * @param outerString String outer string that is to be searched
     * @param subString   String inner string containing the search text
     * @param failIfNull  boolean indicating if an IllegalArgumentException should be thrown if either of the supplied strings are null
     * @return true if subString is contained within outerString, false otherwise
     * @throws IllegalArgumentException if either of the supplied strings are null, and if fialIfNull is set to true
     */
    public static boolean doesStringContainSubString(String outerString, String subString, boolean failIfNull) {
        return indexOfSubStringInString(outerString, subString, failIfNull) > -1;
    }

    public static int indexOfSubStringInString(String outerString, String subString) {
        return indexOfSubStringInString(outerString, subString, false);
    }

    /**
     * Examines the supplied outer string to see if it contains the supplied inner string.<br>
     * If it does, return the position in the outer string that the inner string appears, otherwise, return -1.<br>
     * If <code>failIfNull</code> is set to true, and either of the strings are null, an IllegalArgumentException will be thrown.<br>
     * If it is set to false, and either of the strings are null, returns -1
     *
     * @param outerString String outer string that is to be searched
     * @param subString   String inner string containing the search text
     * @param failIfNull  boolean indicating if an IllegalArgumentException should be thrown if either of the supplied strings are null
     * @throws IllegalArgumentException if either of the supplied strings are null, and if failIfNull is set to true
     */
    public static int indexOfSubStringInString(String outerString, String subString, boolean failIfNull) {
        if (isNullOrEmpty(outerString) || isNullOrEmpty(subString)) {
            if (failIfNull) {
                throw new IllegalArgumentException("Null or empty arguments supplied to doesStringContainSubString");
            } else {
                return -1;
            }
        }
        return outerString.indexOf(subString);
    }

    /**
     * Calls {@link #isNullOrEmpty(String, boolean)} passing in <code>true</code> as the boolean value for <code>ignoreWhiteSpace</code>
     *
     * @see #isNullOrEmpty(String, boolean)
     */
    public static boolean isNullOrEmpty(final String test) {
        return isNullOrEmpty(test, true);
    }

    /**
     * Tests to see if a String is null or empty.<br>
     * Returns true if it is null or empty, false otherwise.<p>
     * If <code>ignoreWhiteSpace</code> is set to <code>true</code>, and the string under test is not null,
     * it will be trimmed before the {@link String#isEmpty()} method is invoked.
     *
     * @param test             String that is being tested
     * @param ignoreWhiteSpace boolean indicating if whitespace should be considered when determining if the string is empty
     * @return true if supplied is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(final String test, boolean ignoreWhiteSpace) {
        return null == test || (ignoreWhiteSpace ? test.trim().isEmpty() : test.isEmpty());
    }

    public static boolean isNullOrEmptyOrMissingData(final String test, boolean ignoreWhiteSpace) {
        return null == test || (ignoreWhiteSpace ? test.trim().isEmpty() : test.isEmpty()) || Constants.MISSING_DATA_STRING.equals(test) || Constants.MISSING_GT.equals(test);
    }

    public static boolean isNullOrEmptyOrMissingData(final String test) {
        return isNullOrEmptyOrMissingData(test, true);
    }

    /**
     * Checks if the given input string is missing or contains an empty or null value.
     *
     * @param inputString the string to be checked
     * @return true if the input string is missing or contains an empty or null value, false otherwise
     */
    public static boolean isMissingDtaString(final String inputString) {
        return isNullOrEmpty(inputString) || inputString.equals(DOT);

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

        if (colonIndex == -1 || minusIndex == -1) {
            throw new IllegalArgumentException("malformed string passed to getChrPositionFromString (missing : or -): " + chrPosString);
        }
        // check that colonIndex and minusIndex aren't adjacent
        if (colonIndex == minusIndex - 1) {
            throw new IllegalArgumentException("malformed string passed to getChrPositionFromString - adjacent :- " + chrPosString);
        }
        int firstPosition = Integer.parseInt(chrPosString, colonIndex + 1, minusIndex, 10);
        int secondPosition = Integer.parseInt(chrPosString, minusIndex + 1, chrPosString.length(), 10);

        // if either position is -ve, throw exception
        if (firstPosition < 0 || secondPosition < 0) {
            throw new IllegalArgumentException("malformed string passed to getChrPositionFromString - negative positions: " + chrPosString);
        }
        return new ChrRangePosition(chrPosString.substring(0, colonIndex), firstPosition, secondPosition);
    }

    public static String getValueFromKey(String data, String key) {
        return getValueFromKey(data, key, '=');
    }

    /**
     * Retrieves the value associated with a given key from a string
     *
     * @param data      the string containing key-value pairs
     * @param key       the key to search for
     * @param separator the character that separates keys from values in the string
     * @return the value associated with the key, or null if the key is not found
     */
    public static String getValueFromKey(String data, String key, char separator) {
        if (!isNullOrEmpty(data)) {
            int index = data.indexOf(key + separator);
            if (index > -1) {
                int toIndex = data.indexOf("\t", index + key.length() + 1);
                if (-1 == toIndex) {
                    toIndex = data.length();
                }
                return data.substring(index + key.length() + 1, toIndex);
            }
        }
        return null;
    }

    public static String getJoinedString(String a, String b) {
        return getJoinedString(a, b, " ");
    }

    /**
     * Utility method to join 2 strings together using the supplied String
     * Null and empty checks are performed on the 2 operands, and if they are null or empty, the other operand is returned.
     * No checks are performed on the glue, and so if a null object is passed in, then this will be reflected in the output
     *
     */
    public static String getJoinedString(String a, String b, String glue) {
        if (null == a) {
            return b;
        }
        if (null == b) {
            return a;
        }
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;

        return a + glue + b;
    }

    /**
     * Utility method that returns a boolean indicating if the supplied mainString contains the supplied searchString
     * more than a certain number of times, which is dictated by the supplied cutoff parameter.
     * <p>
     * If either of the supplied string are null or empty, false is returned
     *
     */
    public static boolean passesOccurrenceCountCheck(String mainString, String searchString, int cutoff) {
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
     *
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

        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') return false;
        }

        return true;
    }

    public static String addToString(String existing, String addition, char delim) {
        if (StringUtils.isNullOrEmpty(existing)) {
            return addition;
        } else if (!existing.contains(addition)) {
            return existing + (delim + addition);
        } else {
            return existing;
        }
    }

    /**
     * convert string to number
     *
     * @param info:  a string of number
     * @param clazz: specify number type [Float or Integer]
     * @return an Integer or Float
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T string2Number(String str, Class<T> clazz) {
        T rate;
        try {
            if (clazz.equals(Integer.class)) {
                rate = (T) Integer.valueOf(str);
            } else {
                rate = (T) Float.valueOf(str);
            }
        } catch (NullPointerException | NumberFormatException e) {
            //otherwise set to 0
            rate = (clazz.equals(Integer.class)) ? (T) Integer.valueOf(0) : (T) Float.valueOf(0);
        }

        return rate;
    }

    public static int getCount(String s, char c) {
        int matchCount = 0;
        for (char c1 : s.toCharArray()) {
            if (c1 == c) {
                matchCount++;
            }
        }
        return matchCount;
    }

}
