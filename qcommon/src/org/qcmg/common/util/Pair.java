/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

public class Pair<L,R> {
	  private final L left;
	  private final R right;

	  public Pair(L left, R right) {
	    this.left = left;
	    this.right = right;
	  }

    public L getLeft() {
	   return left;
	}
	  
	public R getRight() {
	   return right;
	}

}
