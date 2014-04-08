/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.messages;

import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;

public final class Messages
{
    private final ResourceBundle messages;

    private static final Map<String, Messages> resourcesMap =
        new HashMap<String, Messages>();

    private static Messages getMessages(final String msgResourceLocation)
    {
        if (null == resourcesMap.get(msgResourceLocation)) {
            resourcesMap.put(msgResourceLocation,
                             new Messages(msgResourceLocation));
        }
        return resourcesMap.get(msgResourceLocation);
    }

    public static String getMessage(final String msgResourceLocation,
                                    final String identifier)
    {
        Messages messages = getMessages(msgResourceLocation);
        return messages.getMessage(identifier);
    }

    public static String getMessage(final String msgResourceLocation,
                                    final String identifier,
                                    final String argument)
    {
        String message = getMessage(msgResourceLocation, identifier);
        Object[] arguments = {argument};
        return MessageFormat.format(message, arguments);
    }

    public static String getMessage(final String msgResourceLocation,
                                    final String identifier,
                                    final String arg1,
                                    final String arg2)
    {
        String message = getMessage(msgResourceLocation, identifier);
        Object[] arguments = {arg1, arg2};
        return MessageFormat.format(message, arguments);
    }

    public static String getMessage(final String msgResourceLocation,
                                    final String identifier,
                                    final String arg1,
                                    final String arg2,
                                    final String arg3)
    {
        String message = getMessage(msgResourceLocation, identifier);
        Object[] arguments = {arg1, arg2, arg3};
        return MessageFormat.format(message, arguments);
    }

    public static String getMessage(final String msgResourceLocation,
                                    final String identifier,
                                    final Object[] arguments)
    {
        String message = getMessage(msgResourceLocation, identifier);
        return MessageFormat.format(message, arguments);
    }

    private Messages(final String msgResourceLocation)
    {
        messages = ResourceBundle.getBundle(msgResourceLocation);
    }

    private String getMessage(String identifier)
    {
        return messages.getString(identifier);
    }

}
