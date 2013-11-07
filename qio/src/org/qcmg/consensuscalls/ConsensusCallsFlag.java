package org.qcmg.consensuscalls;


public enum ConsensusCallsFlag {

    H_1("h1"),
    H_2("h2"),
    H_3("h3"),
    H_4("h4"),
    H_5("h5"),
    H_6("h6"),
    H_7("h7"),
    H_8("h8"),
    H_9("h9"),
    H_10("h10"),
    H_11("h11"),
    H_12("h12"),
    H_13("h13"),
    H_14("h14"),
    H_15("h15"),
    H_16("h16"),
    H_17("h17"),
    H_18("h18"),
    H_19("h19"),
    H_20("h20"),
    H_21("h21"),
    H_22("h22"),
    M_1("m1"),
    M_2("m2"),
    M_3("m3"),
    M_4("m4"),
    M_5("m5"),
    M_6("m6"),
    M_7("m7"),
    M_8("m8"),
    M_9("m9"),
    M_10("m10"),
    M_11("m11"),
    M_12("m12"),
    M_13("m13");
    
    private final String value;

    ConsensusCallsFlag(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ConsensusCallsFlag fromValue(String v) {
        for (ConsensusCallsFlag c: ConsensusCallsFlag.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
