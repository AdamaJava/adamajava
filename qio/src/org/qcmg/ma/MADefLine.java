/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.ma;

import java.util.Iterator;
import java.util.Vector;

public final class MADefLine {
    private final String readName;
    private final MADirection direction;
    private final Vector<MAMapping> mappings = new Vector<MAMapping>();

    public MADefLine(final String readName, final MADirection direction)
            throws Exception {
        this.readName = readName;
        this.direction = direction;
    }

    public MADefLine(final String readName, final MADirection direction,
            final Vector<MAMapping> mappings) throws Exception {
        this(readName, direction);
        for (final MAMapping mapping : mappings) {
            this.mappings.add(mapping);
        }
    }

    public String getReadName() {
        return readName;
    }

    public MADirection getDirection() {
        return direction;
    }

    public boolean hasMappings() {
        return 0 < mappings.size();
    }

    public int getNumberMappings() {
        return mappings.size();
    }

    public Iterator<MAMapping> iterator() {
        return getMappingIterator();
    }

    public Iterator<MAMapping> getMappingIterator() {
        return mappings.iterator();
    }
}
