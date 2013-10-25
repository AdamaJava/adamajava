package org.qcmg.common.log;

import java.util.logging.Level;

/**
 * Subclass of the Level class, allowing qcmg tools to use a number of qcmg specific logging levels as required.
 * In particular, <CODE>EXEC</CODE> and <CODE>TOOL</CODE> levels have been created to log key statistics from each of the QCMG tools.
 * These levels exist above the standard <CODE>Level.SEVERE</CODE> to ensure that they will always b enabled, unless the user explicitly selects <CODE>Level.OFF</CODE>
 * 
 * @see Level
 * @author oholmes
 * @version 0.1, 21/03/2011 
 *
 */
public class QLevel extends Level {
	/**
	 * default generated serialVersionUID
	 */
	private static final long serialVersionUID = 1594415737845021597L;
	
	 /**
      	* EXEC is a message level for general execution messages.
      	* <p>
      	* Typically EXEC messages will be written to the console
      	* or its equivalent. So the EXEC level should only be 
      	* used for reasonably significant messages that will
      	* make sense to end users and system admins.
      	* This level is initialized to <CODE>1060</CODE>.
      	*/
	public static final QLevel EXEC = new QLevel("EXEC", 1060);
	
	 /**
  	* TOOL is a message level for tool specific messages.
  	* <p>
  	* Typically TOOL messages will be written to the console
  	* or its equivalent. So the TOOL level should only be 
  	* used for reasonably significant messages that will
  	* make sense to end users and system admins.
  	* This level is initialized to <CODE>1040</CODE>.
  	*/
	public static final QLevel TOOL = new QLevel("TOOL", 1040);
	
	/**
  	* DEBUG is a message level for relatively low level messages.
  	* <p>
  	* In general the DEBUG level should be used for information
      	* that will be broadly interesting to developers who do not have
      	* a specialized interest in the specific subsystem.
  	* This level is initialized to <CODE>450</CODE>.
  	*/
	public static final QLevel DEBUG = new QLevel("DEBUG", 450);
	
/**
 * 
 * @param name String representing the QLevel to be instantiated
 * @param value int representing where in the Level hierarchy this QLevel instance should exist
 */
	QLevel(String name, int value) {
		super(name, value);
	}
}
