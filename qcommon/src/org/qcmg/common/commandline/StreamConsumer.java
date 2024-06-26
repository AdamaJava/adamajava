/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.common.commandline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StreamConsumer {
    final InputStream is;
    String[] lines;

    public StreamConsumer(InputStream is) {
        this.is = is;
    }

    public void run() {
        List<String> results = new ArrayList<>();
        //specify a charset to avoid the application behaviour to vary between platforms
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        lines = new String[results.size()];
        results.toArray(lines);
    }

    public String[] getLines() {
        //return a copy of the object rather than a reference of mutable array
        return Arrays.copyOf(lines, lines.length);
    }
}
