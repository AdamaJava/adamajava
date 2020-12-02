/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.ma;

import java.util.Iterator;
import java.util.Vector;

public final class MaDefLine {
    private final String readName;
    private final MaDirection direction;
    private final Vector<MaMapping> mappings = new Vector<MaMapping>();

    public MaDefLine(final String readName, final MaDirection direction) {
        this.readName = readName;
        this.direction = direction;
    }

    public MaDefLine(final String readName, final MaDirection direction, final Vector<MaMapping> mappings) {
        this(readName, direction);
        for (final MaMapping mapping : mappings) {
            this.mappings.add(mapping);
        }
    }

    public String getReadName() {
        return readName;
    }

    public MaDirection getDirection() {
        return direction;
    }

    public boolean hasMappings() {
        return 0 < mappings.size();
    }

    public int getNumberMappings() {
        return mappings.size();
    }

    public Iterator<MaMapping> iterator() {
        return getMappingIterator();
    }

    public Iterator<MaMapping> getMappingIterator() {
        return mappings.iterator();
    }
}
