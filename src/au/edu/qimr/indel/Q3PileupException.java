/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Q3PileupException extends Exception {
	
	private static final long serialVersionUID = -7765139841201954800L;

    public Q3PileupException(final String identifier) {
        super(Messages.getMessage(identifier));
    }
    public Q3PileupException(final String identifier, final String argument) {
        super(Messages.getMessage(identifier, argument));
    }
    public Q3PileupException(final String identifier, final String argument1, final String argument2) {
        super(Messages.getMessage(identifier, argument1, argument2));
    }
    
	public static String getStrackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	} 
	
	public static String printStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	}

}
