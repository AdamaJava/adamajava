/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

/**
 * The class <code>BamMergeException</code> is a form of throwable
 * <code>Exception</code> that indicates conditions raised by classes within the
 * <code>org.qcmg.bammerge</code> package. This class obtains error messages
 * from a message dictionary, with additionally supplied parameters substituted
 * into these messages.
 * <p>
 * For example, the dictionary message defined in the message.properties file
 * as:
 * <pre>
 * BAD_GROUP_REPLACEMENT_FILENAME = Group replacement {0} specifies unknown input file {1}
 * </pre>
 * would be accessed by instantiating <code>BamMergeException</code> in
 * application code as:
 * <pre>
 * BamMergeException ex = new BamMergeException("BAD_GROUP_REPLACEMENT_FILENAME", "file.sam:A:X" , "file.sam");
 * </pre>
 * The resulting <code>BamMergeException</code> object would have an
 * encapsulated error message of:
 * <pre>
 * Group replacement file.sam:A:X specifies unknown input file file.sam
 * </pre>
 * <p>
 * This message is accessible programmatically with the
 * <code>Exception.getMessage()</code> method inherited by
 * <code>BamMergeException</code>.
 * <p>
 * <pre>
 * String message = ex.getMessage();
 * </pre>
 */
@SuppressWarnings("serial")
public final class BamMergeException extends Exception {
	/**
	 * Instantiates a new <code>BamMergeException</code> object for messages
	 * that have no substitutable fields.
	 * 
	 * @param identifier
	 *            the key identifying the message to use from the message
	 *            dictionary
	 */
	public BamMergeException(final String identifier) {
		super(Messages.getMessage(identifier));
	}

	/**
	 * Instantiates a new <code>BamMergeException</code> object for messages
	 * that have one substitutable field.
	 * 
	 * @param identifier
	 *            the key identifying the message to use from the message
	 *            dictionary
	 * @param param
	 *            the <code>String</code> to substitute into the {0} field of
	 *            message.
	 */
	public BamMergeException(final String identifier, final String param) {
		super(Messages.getMessage(identifier, param));
	}

	/**
	 * Instantiates a new <code>BamMergeException</code> object for messages
	 * that have two substitutable fields.
	 * 
	 * @param identifier
	 *            the key identifying the message to use from the message
	 *            dictionary
	 * @param param0
	 *            the <code>String</code> to substitute into the {0} field of
	 *            message.
	 * @param param1
	 *            the <code>String</code> to substitute into the {1} field of
	 *            message.
	 */
	public BamMergeException(final String identifier, final String param0,
			final String param1) {
		super(Messages.getMessage(identifier, param0, param1));
	}

	/**
	 * Instantiates a new <code>BamMergeException</code> object for messages
	 * that have three substitutable fields.
	 * 
	 * @param identifier
	 *            the key identifying the message to use from the message
	 *            dictionary
	 * @param param0
	 *            the <code>String</code> to substitute into the {0} field of
	 *            message.
	 * @param param1
	 *            the <code>String</code> to substitute into the {1} field of
	 *            message.
	 * @param param2
	 *            the <code>String</code> to substitute into the {2} field of
	 *            message.
	 */
	public BamMergeException(final String identifier, final String param0,
			final String param1, final String param2) {
		super(Messages.getMessage(identifier, param0, param1, param2));
	}

	/**
	 * Instantiates a new <code>BamMergeException</code> object for dictionary
	 * messages with any number of substitutable fields. The number of elements
	 * in the <code>arguments</code> array must equate to the number of
	 * substitutable fields in the message being obtained from the dictionary
	 * for the supplied key.
	 * 
	 * @param identifier
	 *            the key identifying the message to use from the message
	 *            dictionary
	 * @param params
	 *            the array of parameters to substitute into the fields of
	 *            message according to index of array elements.
	 */
	public BamMergeException(final String identifier, final Object[] params) {
		super(Messages.getMessage(identifier, params));
	}
}
