package au.edu.qimr.qlib.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionMessage {
	
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
