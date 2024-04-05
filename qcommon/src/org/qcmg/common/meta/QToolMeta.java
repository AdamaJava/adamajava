/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QToolMeta {

    private final String toolName;
    private final List<KeyValue> toolMetaList;

    public QToolMeta(String toolName, KeyValue... toolSpecificMetaInformation) {
        this.toolName = toolName;

        if (toolSpecificMetaInformation.length > 0) {
            toolMetaList = new ArrayList<KeyValue>();
            Collections.addAll(toolMetaList, toolSpecificMetaInformation);
        } else {
            toolMetaList = Collections.emptyList();
        }
    }

    public String getToolMetaDataToString() {
        StringBuilder sb = new StringBuilder();

        for (KeyValue kv : toolMetaList) {
            sb.append(kv.toToolString(toolName));
        }
        return sb.toString();
    }

}
