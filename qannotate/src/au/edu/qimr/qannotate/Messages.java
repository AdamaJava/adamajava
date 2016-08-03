/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/*
 * a collection of methods which return formated message string
 */
public class Messages {
    static final ResourceBundle messages = ResourceBundle.getBundle("au.edu.qimr.qannotate.messages");
 
    public static String getMessage(final String identifier) {
        return messages.getString(identifier);
    }

    public static String getMessage(final String identifier, final String argument){
         String message = Messages.getMessage(identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }

    public static String getMessage(final String identifier,
                             final String arg1,
                             final String arg2)
    {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {arg1, arg2};
        return MessageFormat.format(message, arguments);
    }
    static String getMessage(final String identifier,
                             final String arg1,
                             final String arg2,
                             final String arg3)
    {
        final String message = Messages.getMessage(identifier);
        Object[] arguments = {arg1, arg2, arg3};
        return MessageFormat.format(message, arguments);
    }

    static String getMessage(final String identifier,
                             final Object[] arguments)
    {
        final String message = Messages.getMessage(identifier);
        return MessageFormat.format(message, arguments);
    }

    /**
	 * Gets the program name.
	 *
	 * @return the program name
	 */
	public static String getProgramName() {
		return Messages.class.getPackage().getImplementationTitle();
	}
	/**
	 * Reconstruct command line.
	 *
	 * @param args the args
	 * @return the string
	 */
	public static String reconstructCommandLine(final String[] args) {
		String result = getProgramName() + " ";
		for (final String arg : args) {
			result += arg + " ";
		}
		while( result.lastIndexOf(" ") == result.length() - 1 ) result = result.substring(0, result.length()-2);
		return result;
	}


}
