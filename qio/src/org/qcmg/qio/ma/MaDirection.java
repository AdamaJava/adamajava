/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.ma;

public enum MaDirection {
    F3, R3, F5,F5_BC;

    public static MaDirection getDirection(String directionStr) {
        MaDirection result = null;

        if (0 == directionStr.compareTo("F3")) {
            result = F3;
        } else if (0 == directionStr.compareTo("R3")) {
            result = R3;
        } else if (0 == directionStr.compareTo("F5")) {
            result = F5;
        } else if (0 == directionStr.compareTo("F5-BC")) {
            result = F5_BC;
        } else {
        	throw new IllegalArgumentException("Unknown direction type: " + directionStr);
        }
        return result;
    }
}
