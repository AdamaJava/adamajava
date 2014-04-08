/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

public enum CoverageType {

    SEQUENCE("sequence"),
    PHYSICAL("physical");
    private final String value;

    CoverageType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CoverageType fromValue(String v) {
        for (CoverageType c: CoverageType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
