/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfInfoFieldRecord {


    private final StringBuilder line;

    /**
     * parse line into <key, value> pairs. eg. "CONF;NNS=5" will be parsed to <CONF,NULL>, <NNS,5>
     */
    public VcfInfoFieldRecord(String line) {
        if (StringUtils.isNullOrEmpty(line)) {
            throw new IllegalArgumentException("Null or empty string passed to VcfInfoFieldRecord ctor");
        }

        /*
         * if line is equal to the missing data value, construct an empty string builder
         * Avoids having to deal with it when toStringing
         */

        this.line = new StringBuilder(line.equals(Constants.MISSING_DATA_STRING) ? "" : line);
        this.line.trimToSize();
    }

    /**
     * Replace any entry with this key in the info field
     */
    public void setField(String key, String value) {
        int existingKeyIndex = line.indexOf(key);
        if (existingKeyIndex == -1) {
            // nothing to replace - have at it
            addField(key, value);

        } else {
            // replace existing key (and potentially value)
            // if there is no value, don't need to do anything...
            removeField(key);
            addField(key, value);
        }
    }

    /**
     * Update an entry with this key in the info field, appending the supplied value to the existing value, separated by a comma
     */
    public void appendField(String key, String value) {
        String existingValue = getField(key);
        if (StringUtils.isNullOrEmpty(existingValue)) {
            removeField(key);
            addField(key, value);
        } else if (!existingValue.equals(value)) {
            removeField(key);
            addField(key, existingValue + Constants.COMMA + value);
        }
    }

    /**
     * This method will add the supplied key and value to the underlying string buffer with an '=' as the separator.
     * No check is performed to ensure that an existing entry with the same key is present.
     * Please use {@link #setField(String, String)} if this is the behaviour you want.
     *
     * @param key
     * @param value
     */
    public void addField(String key, String value) {
        if (!line.isEmpty() && line.lastIndexOf(Constants.SEMI_COLON_STRING) != line.length() - 1) {
            line.append(Constants.SEMI_COLON);
        }

        line.append(key);

        if (null != value) {
            line.append(Constants.EQ).append(value);
        }
        line.trimToSize();
    }

    /**
     *
     * @param key
     * @return null if the key field do not exist
     *  @return empty String if the key field exists but no value
     */
    public String getField(String key) {
        //must be looped, in case multi keys/subString exists
        int pos = 0; //line.indexOf(key);

        while ((pos = line.indexOf(key, pos)) > -1) {
            //position of first ; after key
            int scIndex = getSemiColonIndex(pos + 1);
            //position of last ; before key
            int preIndex = line.substring(0, pos).lastIndexOf(Constants.SEMI_COLON_STRING);

            //whether the matching is subString or real key
            String kv = line.substring(preIndex + 1, scIndex);
            int keyend = kv.indexOf(Constants.EQ_STRING);
            String keyStr = (keyend > -1 ? kv.substring(0, keyend) : kv);

            //if the key is just subString of existing key
            //eg. existing INFO: EFF=(....BEND1...); but key is END
            //so END is subString of existing INFO and same length to EFF
            if (!keyStr.equals(key)) {
                pos = scIndex;
                continue;
            }

            String value = StringUtils.getValueFromKey(kv, key, Constants.EQ);
            return value != null ? value : Constants.EMPTY_STRING;
        }
        return null;

    }

    private int getSemiColonIndex(int position) {
        int semiColonIndex = line.indexOf(Constants.SEMI_COLON_STRING, position + 1);
        return (semiColonIndex == -1 ? line.length() : semiColonIndex);
    }

    /**
     * remove existing key value from vcf info column string
     * @param key: will be removed if exists
     */

    public void removeField(String key) {

        //must be looped, in case multi keys/subString exists
        int pos = 0; //line.indexOf(key);
        while ((pos = line.indexOf(key, pos)) > -1) {
            //position of first ; after key
            int scIndex = getSemiColonIndex(pos + 1);
            //position of last ; before key
            int preIndex = line.substring(0, pos).lastIndexOf(Constants.SEMI_COLON_STRING);

            //whether the matching is subString or real key
            String kv = line.substring(preIndex + 1, scIndex);
            int keyend = kv.indexOf(Constants.EQ_STRING);
            String keyStr = (keyend > -1 ? kv.substring(0, keyend) : kv);

            //if the key is just subString of existing key
            //eg. existing INFO: EFF=(....BEND1...); but key is END
            //so END is subString of existing INFO and same length to EFF
            //if(keyStr.length() != key.length()){
            if (!keyStr.equals(key)) {
                pos = scIndex;
                continue;
            }

            //there are four case of the key position, eg. ND field in below info column
            //ND=1,2,3  : subString(0,scIndex), pos = 0 ,scIndex = line.length();
            //ND=1,2,3;OK : subString(0,scIndex) , pos = 0 ,scIndex = 8
            //END=100;ND=1,2,3 : subString(7,scindex), pos = 8 ,scIndex = line.length();
            //END=100;ND=1,2,3;OK : subString(8,scIndex) pos = 8,scIndex = 16

            // check to see if there was a semi colon preceding the key
            if (pos > 0 && scIndex == line.length()) {
                line.delete(pos - 1, scIndex + 1);
            } else {
                line.delete(pos, scIndex + 1);
            }
            break;
        }
        line.trimToSize();
    }

    /**
     * If the string builder is not empty, return its contents, otherwise return the missing data value
     */
    @Override
    public String toString() {
        return !line.isEmpty() ? line.toString() : Constants.MISSING_DATA_STRING;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((line == null) ? 0 : line.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VcfInfoFieldRecord other = (VcfInfoFieldRecord) obj;
        if (line == null) {
            return other.line == null;
        } else return line.toString().contentEquals(other.line);
    }

}
