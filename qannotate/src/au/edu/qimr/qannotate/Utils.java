package au.edu.qimr.qannotate;

public class Utils {
	public static int parseIntSafe(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
}
