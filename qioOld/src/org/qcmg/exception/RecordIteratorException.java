/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.exception;

public class RecordIteratorException extends RuntimeException {

	private static final long serialVersionUID = 7963940971937212428L;

	public RecordIteratorException() {}	// default constructor
	public RecordIteratorException(Exception e) {
		super(e.getMessage(), e);
	}
	public RecordIteratorException(String message, Exception e) {
		super(message, e);
	}
}
