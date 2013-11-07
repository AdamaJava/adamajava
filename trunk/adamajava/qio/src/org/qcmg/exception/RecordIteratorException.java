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
