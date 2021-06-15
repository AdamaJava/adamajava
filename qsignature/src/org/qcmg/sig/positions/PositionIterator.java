package org.qcmg.sig.positions;

import java.util.Iterator;
import java.util.List;

public abstract class PositionIterator<ChrPosition> implements Iterator<ChrPosition> {
	
	public abstract List<String> order();
	public abstract void sort(List<String> contigOrder);

}
