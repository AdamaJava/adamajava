/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.ma;

import java.io.Serializable;
import java.util.Comparator;

public class MAComparator implements Comparator<String> , Serializable{
	
	private static final long serialVersionUID = 8463707706067473843L;
	private int offset;
	
	// default constructor
	public MAComparator() {}
	
	public MAComparator(int offset) {
		this.offset = offset;
	}

	@Override
	public int compare(String o1, String o2) {
		return Integer.valueOf(o1.substring(offset)).compareTo(Integer.valueOf(o2.substring(offset)));
	}

}
