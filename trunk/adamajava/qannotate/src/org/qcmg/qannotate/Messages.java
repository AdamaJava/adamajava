package org.qcmg.qannotate;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/*
 * a collection of methods which return formated message string
 */
public class Messages {
    static final ResourceBundle messages =
        ResourceBundle.getBundle("org.qcmg.qannotate.messages");

    public static String USAGE = getMessage("USAGE");

    public static String getMessage(final String identifier)
    {
        return messages.getString(identifier);
    }

    static String getMessage(final String identifier,
                             final String argument)
    {
         String message = Messages.getMessage(identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }

    static String getMessage(final String identifier,
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
	static String getProgramName() {
		return Messages.class.getPackage().getImplementationTitle();
	}
	/**
	 * Reconstruct command line.
	 *
	 * @param args the args
	 * @return the string
	 */
	static String reconstructCommandLine(final String[] args) {
		String result = getProgramName() + " ";
		for (final String arg : args) {
			result += arg + " ";
		}
		return result;
	}


}
